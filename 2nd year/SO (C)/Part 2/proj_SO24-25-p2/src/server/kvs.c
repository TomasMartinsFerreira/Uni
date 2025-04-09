#include "kvs.h"

#include <ctype.h>
#include <stdlib.h>

#include "string.h"

// Hash function based on key initial.
// @param key Lowercase alphabetical string.
// @return hash.
// NOTE: This is not an ideal hash function, but is useful for test purposes of
// the project
int hash(const char *key) {
  int firstLetter = tolower(key[0]);
  if (firstLetter >= 'a' && firstLetter <= 'z') {
    return firstLetter - 'a';
  } else if (firstLetter >= '0' && firstLetter <= '9') {
    return firstLetter - '0';
  }
  return -1; // Invalid index for non-alphabetic or number strings
}

struct HashTable *create_hash_table() {
  HashTable *ht = malloc(sizeof(HashTable));
  if (!ht)
    return NULL;
  for (int i = 0; i < TABLE_SIZE; i++) {
    ht->table[i] = NULL;
  }
  pthread_rwlock_init(&ht->tablelock, NULL);
  return ht;
}

int write_pair(HashTable *ht, const char *key, const char *value) {
  int index = hash(key);

  // Search for the key node
  KeyNode *keyNode = ht->table[index];
  KeyNode *previousNode;

  while (keyNode != NULL) {
    if (strcmp(keyNode->key, key) == 0) {
      // overwrite value
      free(keyNode->value);
      keyNode->value = strdup(value);
      return 0;
    }
    previousNode = keyNode;
    keyNode = previousNode->next; // Move to the next node
  }
  // Key not found, create a new key node
  keyNode = malloc(sizeof(KeyNode));
  keyNode->key = strdup(key);       // Allocate memory for the key
  keyNode->value = strdup(value);   // Allocate memory for the value
  keyNode->clients = NULL;
  keyNode->next = ht->table[index]; // Link to existing nodes
  ht->table[index] = keyNode; // Place new key node at the start of the list
  return 0;
}

char *read_pair(HashTable *ht, const char *key) {
  int index = hash(key);

  KeyNode *keyNode = ht->table[index];
  KeyNode *previousNode;
  char *value;

  while (keyNode != NULL) {
    if (strcmp(keyNode->key, key) == 0) {
      value = strdup(keyNode->value);
      return value; // Return the value if found
    }
    previousNode = keyNode;
    keyNode = previousNode->next; // Move to the next node
  }

  return NULL; // Key not found
}

int delete_pair(HashTable *ht, const char *key) {
  int index = hash(key);

  // Search for the key node
  KeyNode *keyNode = ht->table[index];
  KeyNode *prevNode = NULL;

  while (keyNode != NULL) {
    if (strcmp(keyNode->key, key) == 0) {
      // Key found; delete this node
      if (prevNode == NULL) {
        // Node to delete is the first node in the list
        ht->table[index] =
            keyNode->next; // Update the table to point to the next node
      } else {
        // Node to delete is not the first; bypass it
        prevNode->next =
            keyNode->next; // Link the previous node to the next node
      }
      while (keyNode->clients != NULL){
        ClientNode *temp = keyNode->clients;
        keyNode->clients = keyNode->clients->next;
        free(temp);
      }
      // Free the memory allocated for the key and value
      free(keyNode->key);
      free(keyNode->value);
      free(keyNode); // Free the key node itself
      return 0;      // Exit the function
    }
    prevNode = keyNode;      // Move prevNode to current node
    keyNode = keyNode->next; // Move to the next node
  }

  return 1;
}

int* get_client_fifos(HashTable *ht, const char *key, size_t* count) {
  int index = hash(key);
  KeyNode *keyNode = ht->table[index];

  // procura pela chave
  while (keyNode != NULL){
    if (strcmp(keyNode->key, key) == 0){
      break;
    }
    keyNode = keyNode->next;
  }

  if (keyNode == NULL){
    *count = 0;
    return NULL;
  }

  size_t num_nodes = 0;
  ClientNode *current = keyNode->clients;
  while (current != NULL) {
      num_nodes++;
      current = current->next;
  }

  int *fifos = malloc(num_nodes * sizeof(int));
  size_t i = 0;
  current = keyNode->clients;
  while (current != NULL) {
    fifos[i++] = current->notif_fifo;
    current = current->next;
  }

  *count = num_nodes;
  return fifos;
}

int add_client_to_key(HashTable *ht, const char *key, int notif_fifo) {
  int index = hash(key);
  KeyNode *keyNode = ht->table[index];

  // procura pela chave
  while (keyNode != NULL){
    if (strcmp(keyNode->key, key) == 0){
      break;
    }
    keyNode = keyNode->next;
  }

  if (keyNode == NULL) return 1;
  
  // adiciona o cliente à lista de clientes
  ClientNode* new_client = malloc(sizeof(ClientNode));
  new_client->notif_fifo = notif_fifo;
  new_client->next = keyNode->clients;
  keyNode->clients = new_client;

  return 0;
}

int remove_client_from_key(HashTable *ht, const char *key, int notif_fifo) {
  int index = hash(key);
  KeyNode *keyNode = ht->table[index];

  // procura pela chave
  while (keyNode != NULL){
    if (strcmp(keyNode->key, key) == 0){
      break;
    }
    keyNode = keyNode->next;
  }

  if (keyNode == NULL) return 1;
  
  ClientNode *prev = NULL;
  ClientNode *curr = keyNode->clients;

  while (curr != NULL){
    if (curr->notif_fifo == notif_fifo){
      // remove o cliente da lista
      if (prev == NULL){
        keyNode->clients = curr->next; // se for o primeiro node
      } else {
        prev->next = curr->next; // se for um node no meio ou fim
      }
      free(curr);
      return 0;
    }
    prev = curr;
    curr = curr->next;
  }

  return 1; // cliente não encontrado
}

void delete_client_subscriptions(HashTable *ht, int notif_fifo) {
  for (int i = 0; i < TABLE_SIZE; i++){
    KeyNode *keyNode = ht->table[i];
    while (keyNode != NULL){
      ClientNode *prev = NULL;
      ClientNode *curr = keyNode->clients;

      // remove o cliente de todas as listas associadas à chave
      while (curr != NULL){
        if (curr->notif_fifo == notif_fifo){
          if (prev == NULL){
            keyNode->clients = curr->next;
          } else {
            prev->next = curr->next;
          }
          ClientNode *temp = curr;
          curr = curr->next;
          free(temp);
        } else {
          prev = curr;
          curr = curr->next;
        }
      }
      keyNode = keyNode->next;
    }
  }
}

void free_table(HashTable *ht) {
  for (int i = 0; i < TABLE_SIZE; i++) {
    KeyNode *keyNode = ht->table[i];
    while (keyNode != NULL) {
      while (keyNode->clients != NULL){
        ClientNode *cTemp = keyNode->clients;
        keyNode->clients = keyNode->clients->next;
        free(cTemp);
      }
      KeyNode *temp = keyNode;
      keyNode = keyNode->next;
      free(temp->key);
      free(temp->value);
      free(temp);
    }
  }
  pthread_rwlock_destroy(&ht->tablelock);
  free(ht);
}
