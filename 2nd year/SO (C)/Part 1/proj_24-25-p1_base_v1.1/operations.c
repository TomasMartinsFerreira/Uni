#include <stdio.h>
#include <stdlib.h>
#include <fcntl.h>
#include <unistd.h>
#include <string.h>
#include <time.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/wait.h>
#include <pthread.h>

#include "kvs.h"
#include "constants.h"

static struct HashTable* kvs_table = NULL;


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
  return 0;
}

// Função de comparação para o qsort
int compare_ints(const void *a, const void *b) {
  return (*(int *)a - *(int *)b);
}

int contains(int *array, size_t size, int value) {
  for (size_t i = 0; i < size; i++) {
    if (array[i] == value)
      return 1;
  }
  return 0;
}

int kvs_write(size_t num_pairs, char keys[][MAX_STRING_SIZE], char values[][MAX_STRING_SIZE]) {
  if (kvs_table == NULL) {
    fprintf(stderr, "KVS state must be initialized\n");
    return 1;
  }

  // para tornar estas instruções atómicas é preciso que cada instrução adquira todos os locks de que precisa de uma vez
  // e para evitar que isto gere deadlocks, criamos uma ordem pela qual os locks são adquiridos
  // exemplo: file1.jobs: WRITE [(k1,v1)(k2,v2)(k3,v3)]   key: k1 k2 k3 
  //          file2.jobs: WRITE [(k2,v2)(k1,v1)]          hash: 1  2  5
  // se file1 perder o processador após adquirir o lock para k1, file2 não irá tentar adquirir o lock de k2
  // mas sim o de k1 pois agora há uma ordem estabelecida e assim não há deadlock (file2 simplesmente espera por file1)

  int hashes[num_pairs];
  size_t j = 0;

  // calcula a hash de cada chave
  for (size_t i = 0; i < num_pairs; i++) {
    int h = hash(keys[i]);
    // evitar hashes duplicadas
    if (!contains(hashes, j, h))
      hashes[j++] = h;
  }

  // ordena as hashes
  qsort(hashes, j, sizeof(int), compare_ints);

  // adquire os locks por ordem das hashes
  for (size_t i = 0; i < j; i++) {
    pthread_rwlock_wrlock(&locks[hashes[i]]);
  }

  // escreve na tabela
  for (size_t i = 0; i < num_pairs; i++) {
    if (write_pair(kvs_table, keys[i], values[i]) != 0) {
      fprintf(stderr, "Failed to write keypair (%s,%s)\n", keys[i], values[i]);
    }
  }

  // solta os locks pela ordem inversa
  for (size_t i = j; i > 0; i--) {
    pthread_rwlock_unlock(&locks[hashes[i - 1]]);
  }

  return 0;
}

int kvs_read(size_t num_pairs, char keys[][MAX_STRING_SIZE],int fd_out) {
  if (kvs_table == NULL) {
    fprintf(stderr, "KVS state must be initialized\n");
    return 1;
  }

  int hashes[num_pairs];
  size_t j = 0; // unique hashes

  qsort(keys, num_pairs, MAX_STRING_SIZE, (int (*)(const void*, const void*))strcmp);
  
  // calcula a hash de cada chave
  for (size_t i = 0; i < num_pairs; i++) {
    int h = hash(keys[i]);
    // evitar hashes duplicadas
    if (!contains(hashes, j, h))
      hashes[j++] = h;
  }

  // ordena as hashes
  qsort(hashes, j, sizeof(int), compare_ints);

  // adquire os locks por ordem das hashes
  for (size_t i = 0; i < j; i++) {
    pthread_rwlock_rdlock(&locks[hashes[i]]);
  }

  // escreve na tabela
  if (write(fd_out, "[", 1) < 0){
    fprintf(stderr, "Failed to write to a file.\n");
    return 1;
  }

  for (size_t i = 0; i < num_pairs; i++) {
    char* result = read_pair(kvs_table, keys[i]);
    char pair[MAX_STRING_SIZE];
    if (result == NULL) {
      sprintf(pair, "(%s,KVSERROR)", keys[i]);
    } else {
      sprintf(pair, "(%s,%s)", keys[i], result);
    }

    if (write(fd_out, pair, strlen(pair)) < 0)
    {
      fprintf(stderr, "Failed to write to a file.\n");
      return 1;
    }
    

    free(result);
  }

  if (write(fd_out,"]\n", 2) < 0){
    fprintf(stderr, "Failed to write to a file.\n");
    return 1;
  }
  
  // solta os locks pela ordem inversa
  for (size_t i = j; i > 0; i--) {
    pthread_rwlock_unlock(&locks[hashes[i - 1]]);
  }

  return 0;
}

int kvs_delete(size_t num_pairs, char keys[][MAX_STRING_SIZE],int fd_out) {
  if (kvs_table == NULL) {
    fprintf(stderr, "KVS state must be initialized\n");
    return 1;
  }
  int aux = 0;
  int hashes[num_pairs];
  size_t j = 0; // unique hashes

  // calcula a hash de cada chave
  for (size_t i = 0; i < num_pairs; i++) {
    int h = hash(keys[i]);
    // evitar hashes duplicadas
    if (!contains(hashes, j, h))
      hashes[j++] = h;
  }

  // ordena as hashes
  qsort(hashes, j, sizeof(int), compare_ints);

  // adquire os locks por ordem das hashes
  for (size_t i = 0; i < j; i++) {
    pthread_rwlock_wrlock(&locks[hashes[i]]);
  }

  // modifica a tabela
  for (size_t i = 0; i < num_pairs; i++) {
    if (delete_pair(kvs_table, keys[i]) != 0) {
      if (!aux) {
        if (write(fd_out, "[", 1) < 0){
          fprintf(stderr, "Failed to write to a file.\n");
          return 1;
        }
        aux = 1;
      }
      char pair[MAX_STRING_SIZE];
      sprintf(pair, "(%s,KVSMISSING)", keys[i]);
      
      if (write(fd_out, pair, strlen(pair)) < 0) {
        fprintf(stderr, "Failed to write to a file.\n");
        return 1;
        }
    }
  }
  if (aux) {
    if (write(fd_out,"]\n", 2) < 0){
      fprintf(stderr, "Failed to write to a file.\n");
      return 1;
    }
  }

  // solta os locks pela ordem inversa
  for (size_t i = j; i > 0; i--) {
      pthread_rwlock_unlock(&locks[hashes[i - 1]]);
  }

  return 0;
}

void kvs_show(int fd_out) {
  for (int i = 0; i < TABLE_SIZE; i++) {
    KeyNode *keyNode = kvs_table->table[i];
    while (keyNode != NULL) {
      char pair[MAX_STRING_SIZE];
      sprintf(pair, "(%s, %s)\n", keyNode->key, keyNode->value);

      if (write(fd_out, pair, strlen(pair)) < 0){
        fprintf(stderr, "Failed to write to a file.\n");
        return;
      }
      keyNode = keyNode->next; // Move to the next node
    }
  }
}

void kvs_show_aux(int fd_out){
  int hashes[TABLE_SIZE];
  size_t num_hashes = 0; // Contador de hashes válidas

  // Calcula a hash de cada chave e armazena apenas as válidas
  for (int i = 0; i < TABLE_SIZE; i++) {
    KeyNode *keyNode = kvs_table->table[i];
    if (keyNode != NULL)
      hashes[num_hashes++] = hash(keyNode->key);
  }

  // ordena as hashes
  qsort(hashes, num_hashes, sizeof(int), compare_ints);

  // adquirir locks
  for (size_t i = 0; i < num_hashes; i++){
    pthread_rwlock_rdlock(&locks[hashes[i]]);
  }

  // ler tabela
  kvs_show(fd_out);

  // solta os locks pela ordem inversa
  for (size_t i = num_hashes; i > 0; i--) {
    pthread_rwlock_unlock(&locks[hashes[i - 1]]);
  }
}

int kvs_backup(char FileName[MAX_JOB_FILE_NAME_SIZE],int backup_number) {
  
  if (kvs_table == NULL) {
    fprintf(stderr, "KVS state must be initialized\n");
    return 1;
  }

  int hashes[TABLE_SIZE];
  size_t num_hashes = 0; // Contador de hashes válidas

  // Calcula a hash de cada chave e armazena apenas as válidas
  for (int i = 0; i < TABLE_SIZE; i++) {
    KeyNode *keyNode = kvs_table->table[i];
    if (keyNode != NULL)
      hashes[num_hashes++] = hash(keyNode->key);
  }

  // ordena as hashes
  qsort(hashes, num_hashes, sizeof(int), compare_ints);

  // adquirir locks
  for (size_t i = 0; i < num_hashes; i++){
    pthread_rwlock_rdlock(&locks[hashes[i]]);
  }

  pid_t pid = fork();
  if (pid < 0)
  {
    fprintf(stderr, "Failed to create a new process\n");
    return 1;
  }

  // o processo filho copia o espaço de memória do pai - os locks serão copiados mas não vão corresponder
  // aos mesmos objetos, logo se o processo filho tentar mexer em locks vai levar a unexpected behavior
  // daí a criação do kvs_show_aux (que o filho não usa)
  // deve haver uma maneira melhor de fazer isto mas por enquanto foi o que consegui

  if (pid == 0) { // processo filho
    char FileName_backup[MAX_JOB_FILE_NAME_SIZE];
    strcpy(FileName_backup,FileName);
    FileName_backup[strlen(FileName_backup) - 4] = '\0';
    strcat(FileName_backup,"-");

    char buffer[sizeof(int)]; 
    sprintf(buffer, "%d", backup_number); 
    strcat(FileName_backup, buffer); 
    strcat(FileName_backup,".bck");
    int fd_bck = open(FileName_backup, O_CREAT | O_TRUNC | O_WRONLY, S_IRUSR | S_IWUSR);
    
    if (fd_bck < 0){
      fprintf(stderr, "Failed to open the file\n");
      return 1;
    }

    kvs_show(fd_bck);

    close(fd_bck);
    exit(0);
  } else { // processo pai
    // solta os locks pela ordem inversa
    for (size_t i = num_hashes; i > 0; i--) {
      pthread_rwlock_unlock(&locks[hashes[i - 1]]);
    }
  }
  
  return 0;
}

// recebe um ponteiro para CURRENT_BACKUPS para poder alterar o seu valor
void kvs_wait_backup(int* current_backups){
  // esperar que todos os processos acabem 
  while (*current_backups > 0){
    // se wait der <= 0, então esta thread não tem processos filho
    if (wait(NULL) > 0)
      (*current_backups)--;
  }
}

void kvs_wait(unsigned int delay_ms) {
  struct timespec delay = delay_to_timespec(delay_ms);
  nanosleep(&delay, NULL);
}