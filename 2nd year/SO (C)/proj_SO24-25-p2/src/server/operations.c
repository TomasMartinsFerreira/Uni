#include "operations.h"

#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/wait.h>
#include <time.h>
#include <unistd.h>
#include <errno.h>

#include "constants.h"
#include "io.h"
#include "src/common/io.h"
#include "kvs.h"

static struct HashTable *kvs_table = NULL;

/// Calculates a timespec from a delay in milliseconds.
/// @param delay_ms Delay in milliseconds.
/// @return Timespec with the given delay.
static struct timespec delay_to_timespec(unsigned int delay_ms) {
  return (struct timespec){delay_ms / 1000, (delay_ms % 1000) * 1000000};
}

int kvs_init() {
  if (kvs_table != NULL) {
    fprintf(stderr, "KVS state has already been initialized\n");
    return 1;
  }

  kvs_table = create_hash_table();
  return kvs_table == NULL;
}

int kvs_terminate() {
  if (kvs_table == NULL) {
    fprintf(stderr, "KVS state must be initialized\n");
    return 1;
  }

  free_table(kvs_table);
  kvs_table = NULL;
  return 0;
}

int kvs_write(size_t num_pairs, char keys[][MAX_STRING_SIZE],
              char values[][MAX_STRING_SIZE]) {
  if (kvs_table == NULL) {
    fprintf(stderr, "KVS state must be initialized\n");
    return 1;
  }

  pthread_rwlock_wrlock(&kvs_table->tablelock);

  for (size_t i = 0; i < num_pairs; i++) {
    if (write_pair(kvs_table, keys[i], values[i]) != 0) {
      fprintf(stderr, "Failed to write key pair (%s,%s)\n", keys[i], values[i]);
    } else {
      size_t cf_size = 0;
      int* client_fifos = get_client_fifos(kvs_table, keys[i], &cf_size);
      notify_clients(kvs_table, keys[i], client_fifos, cf_size);
    }
  }

  pthread_rwlock_unlock(&kvs_table->tablelock);
  return 0;
}

int kvs_read(size_t num_pairs, char keys[][MAX_STRING_SIZE], int fd) {
  if (kvs_table == NULL) {
    fprintf(stderr, "KVS state must be initialized\n");
    return 1;
  }

  pthread_rwlock_rdlock(&kvs_table->tablelock);

  write_str(fd, "[");
  for (size_t i = 0; i < num_pairs; i++) {
    char *result = read_pair(kvs_table, keys[i]);
    char aux[MAX_STRING_SIZE];
    if (result == NULL) {
      snprintf(aux, MAX_STRING_SIZE, "(%s,KVSERROR)", keys[i]);
    } else {
      snprintf(aux, MAX_STRING_SIZE, "(%s,%s)", keys[i], result);
    }
    write_str(fd, aux);
    free(result);
  }
  write_str(fd, "]\n");

  pthread_rwlock_unlock(&kvs_table->tablelock);
  return 0;
}

int kvs_delete(size_t num_pairs, char keys[][MAX_STRING_SIZE], int fd) {
  if (kvs_table == NULL) {
    fprintf(stderr, "KVS state must be initialized\n");
    return 1;
  }

  pthread_rwlock_wrlock(&kvs_table->tablelock);

  int aux = 0;
  for (size_t i = 0; i < num_pairs; i++) {
    // precisamos dos fifos dos clientes antes de eliminar a chave
    size_t cf_size = 0;
    int* client_fifos = get_client_fifos(kvs_table, keys[i], &cf_size);
    if (delete_pair(kvs_table, keys[i]) != 0) {
      if (!aux) {
        write_str(fd, "[");
        aux = 1;
      }
      char str[MAX_STRING_SIZE];
      snprintf(str, MAX_STRING_SIZE, "(%s,KVSMISSING)", keys[i]);
      write_str(fd, str);
    } else {
      notify_clients(kvs_table, keys[i], client_fifos, cf_size);
    }
  }
  if (aux) {
    write_str(fd, "]\n");
  }

  pthread_rwlock_unlock(&kvs_table->tablelock);
  return 0;
}

void kvs_show(int fd) {
  if (kvs_table == NULL) {
    fprintf(stderr, "KVS state must be initialized\n");
    return;
  }

  pthread_rwlock_rdlock(&kvs_table->tablelock);
  char aux[MAX_STRING_SIZE];

  for (int i = 0; i < TABLE_SIZE; i++) {
    KeyNode *keyNode = kvs_table->table[i]; // Get the next list head
    while (keyNode != NULL) {
      snprintf(aux, MAX_STRING_SIZE, "(%s, %s)\n", keyNode->key,
               keyNode->value);
      write_str(fd, aux);
      keyNode = keyNode->next; // Move to the next node of the list
    }
  }

  pthread_rwlock_unlock(&kvs_table->tablelock);
}

int kvs_backup(size_t num_backup, char *job_filename, char *directory) {
  pid_t pid;
  char bck_name[50];
  snprintf(bck_name, sizeof(bck_name), "%s/%s-%ld.bck", directory,
           strtok(job_filename, "."), num_backup);

  pthread_rwlock_rdlock(&kvs_table->tablelock);
  pid = fork();
  pthread_rwlock_unlock(&kvs_table->tablelock);
  if (pid == 0) {
    // functions used here have to be async signal safe, since this
    // fork happens in a multi thread context (see man fork)
    int fd = open(bck_name, O_WRONLY | O_CREAT | O_TRUNC, 0666);
    for (int i = 0; i < TABLE_SIZE; i++) {
      KeyNode *keyNode = kvs_table->table[i]; // Get the next list head
      while (keyNode != NULL) {
        char aux[MAX_STRING_SIZE];
        aux[0] = '(';
        size_t num_bytes_copied = 1; // the "("
        // the - 1 are all to leave space for the '/0'
        num_bytes_copied += strn_memcpy(aux + num_bytes_copied, keyNode->key,
                                        MAX_STRING_SIZE - num_bytes_copied - 1);
        num_bytes_copied += strn_memcpy(aux + num_bytes_copied, ", ",
                                        MAX_STRING_SIZE - num_bytes_copied - 1);
        num_bytes_copied += strn_memcpy(aux + num_bytes_copied, keyNode->value,
                                        MAX_STRING_SIZE - num_bytes_copied - 1);
        num_bytes_copied += strn_memcpy(aux + num_bytes_copied, ")\n",
                                        MAX_STRING_SIZE - num_bytes_copied - 1);
        aux[num_bytes_copied] = '\0';
        write_str(fd, aux);
        keyNode = keyNode->next; // Move to the next node of the list
      }
    }
    exit(1);
  } else if (pid < 0) {
    return -1;
  }
  return 0;
}

void kvs_wait(unsigned int delay_ms) {
  struct timespec delay = delay_to_timespec(delay_ms);
  nanosleep(&delay, NULL);
}

int client_subscribe(int req_pipe, int notif_pipe){
  size_t REQUEST_SIZE = MAX_STRING_SIZE + 1;
  char req_buffer[REQUEST_SIZE];
  int intr = 0;

  // read request
  if (read_all(req_pipe, req_buffer, REQUEST_SIZE, &intr) < 0){
    if (errno != EBADF) {
      fprintf(stderr, "Failed to read from request pipe\n");
    }
    return 1;
  }

  char key[MAX_STRING_SIZE];
  strncpy(key, req_buffer, MAX_STRING_SIZE);

  pthread_rwlock_wrlock(&kvs_table->tablelock);
  int result = add_client_to_key(kvs_table, key, notif_pipe);
  pthread_rwlock_unlock(&kvs_table->tablelock);
  return result;
}

int client_unsubscribe(int req_pipe, int notif_pipe){
  size_t REQUEST_SIZE = MAX_STRING_SIZE + 1;
  char req_buffer[REQUEST_SIZE];
  int intr = 0;

  // read request
  if (read_all(req_pipe, req_buffer, REQUEST_SIZE, &intr) < 0){
    if (errno != EBADF) {
      fprintf(stderr, "Failed to write to response pipe\n");
    }
    return 1;
  }

  char key[MAX_STRING_SIZE];
  strncpy(key, req_buffer, MAX_STRING_SIZE);

  pthread_rwlock_wrlock(&kvs_table->tablelock);
  int result = remove_client_from_key(kvs_table, key, notif_pipe);
  pthread_rwlock_unlock(&kvs_table->tablelock);
  return result;
}

int client_disconnect(int req_pipe, int notif_pipe){
  pthread_rwlock_wrlock(&kvs_table->tablelock);
  delete_client_subscriptions(kvs_table, notif_pipe);
  pthread_rwlock_unlock(&kvs_table->tablelock);
  close(req_pipe);
  close(notif_pipe);
  return 0; // não sei em que caso retornaria 1
}

void notify_clients(HashTable *ht, const char* key, int* fifos, size_t fifos_size){
  int index = hash(key);
  KeyNode *keyNode = ht->table[index];

  // procura pela chave
  while (keyNode != NULL){
    if (strcmp(keyNode->key, key) == 0){
      break;
    }
    keyNode = keyNode->next;
  }

  size_t BUFFER_SIZE = 2 * (MAX_STRING_SIZE + 1);
  char buffer[BUFFER_SIZE];

  // build buffer
  memset(buffer, '\0', BUFFER_SIZE);
  strncpy(buffer, key, MAX_STRING_SIZE);

  // chave não existe (foi apagada com kvs_delete)
  if (keyNode == NULL){
    // isto é só uma forma de informar o cliente que a chave foi eliminada
    // pois esta posição do buffer teria sempre o valor '\0' caso contrário
    buffer[BUFFER_SIZE - 1] = 'D'; // podia ser outro caracter qualquer (exceto o '\0')
  } else {
    strncpy(buffer + MAX_STRING_SIZE + 1, keyNode->value, MAX_STRING_SIZE);
  }

  // percorrer clientes e escrever para os seus fifos de notificações
  for (size_t i = 0; i < fifos_size; i++){
    write_all(fifos[i], buffer, BUFFER_SIZE);
  }
}
