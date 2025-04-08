#ifndef KEY_VALUE_STORE_H
#define KEY_VALUE_STORE_H
#define TABLE_SIZE 26

#include <pthread.h>
#include <stddef.h>

typedef struct ClientNode {
  int notif_fifo;
  struct ClientNode* next;
} ClientNode;

typedef struct KeyNode {
  char *key;
  char *value;
  ClientNode* clients;
  struct KeyNode *next;
} KeyNode;

typedef struct HashTable {
  KeyNode *table[TABLE_SIZE];
  pthread_rwlock_t tablelock;
} HashTable;

/// Creates a new KVS hash table.
/// @return Newly created hash table, NULL on failure
struct HashTable *create_hash_table();

int hash(const char *key);

// Writes a key value pair in the hash table.
// @param ht The hash table.
// @param key The key.
// @param value The value.
// @return 0 if successful.
int write_pair(HashTable *ht, const char *key, const char *value);

// Reads the value of a given key.
// @param ht The hash table.
// @param key The key.
// return the value if found, NULL otherwise.
char *read_pair(HashTable *ht, const char *key);

/// Deletes a pair from the table.
/// @param ht Hash table to read from.
/// @param key Key of the pair to be deleted.
/// @return 0 if the node was deleted successfully, 1 otherwise.
int delete_pair(HashTable *ht, const char *key);

/// Returns the fifos of all clients associated with a key.
/// @param ht The hash table.
/// @param key The key.
/// @param count (will be written to) Number of fifos found 
/// @return the list of clients
int* get_client_fifos(HashTable *ht, const char *key, size_t* count);

/// Adds a client to a key. Used when a client subscribes to that key.
/// @param ht The hash table.
/// @param key The key.
/// @param notif_fifo The client's notifications fifo.
/// @return 0 if the key exists, 1 otherwise.
int add_client_to_key(HashTable *ht, const char *key, int notif_fifo);

/// Removes a client from a key. Used when a client unsubscribes that key.
/// @param ht The hash table.
/// @param key The key.
/// @param notif_fifo The client's notifications fifo.
/// @return 0 if the client is subscribed to the key, 1 otherwise.
int remove_client_from_key(HashTable *ht, const char *key, int notif_fifo);

/// Deletes all subscriptions of a client. Used to disconnect.
/// @param ht The hash table.
/// @param notif_fifo The client's notifications fifo.
void delete_client_subscriptions(HashTable *ht, int notif_fifo);

/// Frees the hashtable.
/// @param ht Hash table to be deleted.
void free_table(HashTable *ht);

#endif // KVS_H
