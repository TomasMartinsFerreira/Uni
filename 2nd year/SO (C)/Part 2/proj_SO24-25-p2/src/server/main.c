#include <dirent.h>
#include <fcntl.h>
#include <limits.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/wait.h>
#include <unistd.h>
#include <semaphore.h>
#include <signal.h>
#include <errno.h>

#include "constants.h"
#include "src/common/constants.h"
#include "src/common/io.h"
#include "src/common/protocol.h"
#include "io.h"
#include "operations.h"
#include "parser.h"
#include "pthread.h"

#define CONNECTION_REQUEST_SIZE sizeof(char) + 3 * MAX_PIPE_PATH_LENGTH + 1

struct SharedData {
  DIR *dir;
  char *dir_name;
  pthread_mutex_t directory_mutex;
};

//  Buffer Produtor-Consumidor

size_t in_idx = 0;             // Índice para o próximo item a ser produzido
size_t out_idx = 0;            // Índice para o próximo item a ser consumido

sem_t empty;            // Contagem de espaços vazios
sem_t full;             // Contagem de itens disponíveis

char produtor_consumidor[MAX_SESSION_COUNT * (CONNECTION_REQUEST_SIZE)]; // buffer para as conexões

pthread_mutex_t lock = PTHREAD_MUTEX_INITIALIZER;
pthread_mutex_t n_current_backups_lock = PTHREAD_MUTEX_INITIALIZER;

size_t active_backups = 0; // Number of active backups
size_t max_backups;        // Maximum allowed simultaneous backups
size_t max_threads;        // Maximum allowed simultaneous threads
char reg_fifo[MAX_PIPE_PATH_LENGTH];
char *jobs_directory = NULL;

int req_pipe[MAX_SESSION_COUNT], resp_pipe[MAX_SESSION_COUNT], notif_pipe[MAX_SESSION_COUNT];

int filter_job_files(const struct dirent *entry) {
  const char *dot = strrchr(entry->d_name, '.');
  if (dot != NULL && strcmp(dot, ".job") == 0) {
    return 1; // Keep this file (it has the .job extension)
  }
  return 0;
}

static int entry_files(const char *dir, struct dirent *entry, char *in_path,
                       char *out_path) {
  const char *dot = strrchr(entry->d_name, '.');
  if (dot == NULL || dot == entry->d_name || strlen(dot) != 4 ||
      strcmp(dot, ".job")) {
    return 1;
  }

  if (strlen(entry->d_name) + strlen(dir) + 2 > MAX_JOB_FILE_NAME_SIZE) {
    fprintf(stderr, "%s/%s\n", dir, entry->d_name);
    return 1;
  }

  strcpy(in_path, dir);
  strcat(in_path, "/");
  strcat(in_path, entry->d_name);

  strcpy(out_path, in_path);
  strcpy(strrchr(out_path, '.'), ".out");

  return 0;
}

static int run_job(int in_fd, int out_fd, char *filename) {
  size_t file_backups = 0;
  while (1) {
    char keys[MAX_WRITE_SIZE][MAX_STRING_SIZE] = {0};
    char values[MAX_WRITE_SIZE][MAX_STRING_SIZE] = {0};
    unsigned int delay;
    size_t num_pairs;

    switch (get_next(in_fd)) {
    case CMD_WRITE:
      num_pairs =
          parse_write(in_fd, keys, values, MAX_WRITE_SIZE, MAX_STRING_SIZE);
      if (num_pairs == 0) {
        write_str(STDERR_FILENO, "Invalid command. See HELP for usage\n");
        continue;
      }

      if (kvs_write(num_pairs, keys, values)) {
        write_str(STDERR_FILENO, "Failed to write pair\n");
      }
      break;

    case CMD_READ:
      num_pairs =
          parse_read_delete(in_fd, keys, MAX_WRITE_SIZE, MAX_STRING_SIZE);

      if (num_pairs == 0) {
        write_str(STDERR_FILENO, "Invalid command. See HELP for usage\n");
        continue;
      }

      if (kvs_read(num_pairs, keys, out_fd)) {
        write_str(STDERR_FILENO, "Failed to read pair\n");
      }
      break;

    case CMD_DELETE:
      num_pairs =
          parse_read_delete(in_fd, keys, MAX_WRITE_SIZE, MAX_STRING_SIZE);

      if (num_pairs == 0) {
        write_str(STDERR_FILENO, "Invalid command. See HELP for usage\n");
        continue;
      }

      if (kvs_delete(num_pairs, keys, out_fd)) {
        write_str(STDERR_FILENO, "Failed to delete pair\n");
      }
      break;

    case CMD_SHOW:
      kvs_show(out_fd);
      break;

    case CMD_WAIT:
      if (parse_wait(in_fd, &delay, NULL) == -1) {
        write_str(STDERR_FILENO, "Invalid command. See HELP for usage\n");
        continue;
      }

      if (delay > 0) {
        printf("Waiting %d seconds\n", delay / 1000);
        kvs_wait(delay);
      }
      break;

    case CMD_BACKUP:
      pthread_mutex_lock(&n_current_backups_lock);
      if (active_backups >= max_backups) {
        wait(NULL);
      } else {
        active_backups++;
      }
      pthread_mutex_unlock(&n_current_backups_lock);
      int aux = kvs_backup(++file_backups, filename, jobs_directory);

      if (aux < 0) {
        write_str(STDERR_FILENO, "Failed to do backup\n");
      } else if (aux == 1) {
        return 1;
      }
      break;

    case CMD_INVALID:
      write_str(STDERR_FILENO, "Invalid command. See HELP for usage\n");
      break;

    case CMD_HELP:
      write_str(STDOUT_FILENO,
                "Available commands:\n"
                "  WRITE [(key,value)(key2,value2),...]\n"
                "  READ [key,key2,...]\n"
                "  DELETE [key,key2,...]\n"
                "  SHOW\n"
                "  WAIT <delay_ms>\n"
                "  BACKUP\n" // Not implemented
                "  HELP\n");

      break;

    case CMD_EMPTY:
      break;

    case EOC:
      printf("EOF\n");
      return 0;
    }
  }
}

// frees arguments
static void *get_file(void *arguments) {
  struct SharedData *thread_data = (struct SharedData *)arguments;
  DIR *dir = thread_data->dir;
  char *dir_name = thread_data->dir_name;

  // ignora SIGUSR1 nestas tarefas
  sigset_t sig_set;
  sigemptyset(&sig_set);
  sigaddset(&sig_set, SIGUSR1);
  pthread_sigmask(SIG_BLOCK, &sig_set, NULL);

  if (pthread_mutex_lock(&thread_data->directory_mutex) != 0) {
    fprintf(stderr, "Thread failed to lock directory_mutex\n");
    return NULL;
  }

  struct dirent *entry;
  char in_path[MAX_JOB_FILE_NAME_SIZE], out_path[MAX_JOB_FILE_NAME_SIZE];
  while ((entry = readdir(dir)) != NULL) {
    if (entry_files(dir_name, entry, in_path, out_path)) {
      continue;
    }

    if (pthread_mutex_unlock(&thread_data->directory_mutex) != 0) {
      fprintf(stderr, "Thread failed to unlock directory_mutex\n");
      return NULL;
    }

    int in_fd = open(in_path, O_RDONLY);
    if (in_fd == -1) {
      write_str(STDERR_FILENO, "Failed to open input file: ");
      write_str(STDERR_FILENO, in_path);
      write_str(STDERR_FILENO, "\n");
      pthread_exit(NULL);
    }

    int out_fd = open(out_path, O_WRONLY | O_CREAT | O_TRUNC, 0666);
    if (out_fd == -1) {
      write_str(STDERR_FILENO, "Failed to open output file: ");
      write_str(STDERR_FILENO, out_path);
      write_str(STDERR_FILENO, "\n");
      pthread_exit(NULL);
    }

    int out = run_job(in_fd, out_fd, entry->d_name);

    close(in_fd);
    close(out_fd);

    if (out) {
      if (closedir(dir) == -1) {
        fprintf(stderr, "Failed to close directory\n");
        return 0;
      }

      exit(0);
    }

    if (pthread_mutex_lock(&thread_data->directory_mutex) != 0) {
      fprintf(stderr, "Thread failed to lock directory_mutex\n");
      return NULL;
    }
  }

  if (pthread_mutex_unlock(&thread_data->directory_mutex) != 0) {
    fprintf(stderr, "Thread failed to unlock directory_mutex\n");
    return NULL;
  }

  pthread_exit(NULL);
}

// sends a response to a client indicating the success or failure of their request
int send_response(char op_code, char result, int j){
  size_t BUFFER_SIZE = 2 * sizeof(char);
  char buffer[BUFFER_SIZE];

  // build buffer
  buffer[0] = op_code + '0';
  buffer[1] = result + '0';
  // send response
  return write_all(resp_pipe[j], buffer, BUFFER_SIZE);
}

// tarefas gestoras
void *handle_client(void *arg){
  int j = *(int *)arg;
  char op_buffer;
  char result;

  char req_pipe_path[MAX_PIPE_PATH_LENGTH];
  char resp_pipe_path[MAX_PIPE_PATH_LENGTH];
  char notif_pipe_path[MAX_PIPE_PATH_LENGTH];

  // ignora SIGUSR1 nestas tarefas
  sigset_t sig_set;
  sigemptyset(&sig_set);
  sigaddset(&sig_set, SIGUSR1);
  pthread_sigmask(SIG_BLOCK, &sig_set, NULL);

  // quando um cliente se desconecta não terminamos a thread. Em vez disso,
  // voltamos aqui e reutilizamos esta thread para comunicar com o próximo cliente
  while (1){
    int handle_new_client = 0;
    sem_wait(&full); // aguarda uma mensagem disponível no buffer

    pthread_mutex_lock(&lock);

    // ler do buffer produtor_consumidor
    size_t index = out_idx * CONNECTION_REQUEST_SIZE;
    strncpy(req_pipe_path, produtor_consumidor + index + 1, MAX_PIPE_PATH_LENGTH);
    strncpy(resp_pipe_path, produtor_consumidor + index + 1 + MAX_PIPE_PATH_LENGTH, MAX_PIPE_PATH_LENGTH);
    strncpy(notif_pipe_path, produtor_consumidor + index + 1 + 2 * MAX_PIPE_PATH_LENGTH, MAX_PIPE_PATH_LENGTH);
    
    out_idx = (out_idx + 1) % MAX_SESSION_COUNT; // incrementar indice de leitura

    pthread_mutex_unlock(&lock);
    sem_post(&empty); // indica que há mais um espaço disponível

    // open the pipes
    if ((req_pipe[j] = open(req_pipe_path, O_RDONLY)) < 0){
      write_str(STDERR_FILENO, "Failed to open FIFO\n");
      send_response(OP_CODE_CONNECT, 1, j);
      return NULL;
    }
    if ((resp_pipe[j] = open(resp_pipe_path, O_WRONLY)) < 0){
      write_str(STDERR_FILENO, "Failed to open FIFO\n");
      send_response(OP_CODE_CONNECT, 1, j);
      return NULL;
    }
    if ((notif_pipe[j] = open(notif_pipe_path, O_WRONLY)) < 0){
      write_str(STDERR_FILENO, "Failed to open FIFO\n");
      send_response(OP_CODE_CONNECT, 1, j);    
      return NULL;
    }

    if (send_response(OP_CODE_CONNECT, 0, j) < 0){
      // cliente foi desconectado por um SIGUSR1 e por isso os pipes foram fechados
      if (errno == EBADF) {
        continue;
      } else {
        write_str(STDERR_FILENO, "Failed to write to response pipe\n");
        return NULL;
      }
    }
    // read messages from client until they ask to disconnect
    while (1){
      if (read(req_pipe[j], &op_buffer, 1) < 0){
        if (errno == EBADF) {
          break;
        } else {
          write_str(STDERR_FILENO, "Failed to read from request pipe\n");
          return NULL;
        }
      }

      // Convert from ASCII to integer
      int operation = op_buffer - '0';
      switch (operation) {
      case OP_CODE_DISCONNECT:
        result = (char) client_disconnect(req_pipe[j], notif_pipe[j]);
        if (send_response(OP_CODE_DISCONNECT, result, j) < 0){
          if (errno == EBADF) {
            handle_new_client = 1;
            break;
          } else {
            write_str(STDERR_FILENO, "Failed to writo to response pipe\n");
            return NULL;
          }
        }
        // resp_pipe tem que ser fechado depois de enviar a mensagem
        close(resp_pipe[j]);
        handle_new_client = 1;
        break;
      case OP_CODE_SUBSCRIBE:
        result = (char) client_subscribe(req_pipe[j], notif_pipe[j]);
        break;
      case OP_CODE_UNSUBSCRIBE:
        result = (char) client_unsubscribe(req_pipe[j], notif_pipe[j]);
        break; 
      }

      if (handle_new_client) break;
      if (send_response((char) operation, result, j) < 0){
        if (errno == EBADF) {
          break;
        } else {
          write_str(STDERR_FILENO, "Failed to write to response pipe\n");
          return NULL;
        }
      }
    }
  }
}

// waits for a client to request a connection (through reg_fifo) and establishes it
void await_connection(){
  int reg_fd;

  // este loop é para ao receber SIGUSR1, o servidor voltar a tentar open()
  while (1) {
    // waits for a client to open the other end of reg_fifo
    reg_fd = open(reg_fifo, O_RDONLY);
    
    // Se não houver erro ou o erro não for EINTR, sai do loop
    if (reg_fd >= 0 || errno != EINTR) break;
  }

  if (reg_fd < 0){
    write_str(STDERR_FILENO, "Failed to open FIFO\n.");
    // vvv   isto não seria possivel já que os pipes não foram abertos
    // send_response(OP_CODE_CONNECT, 1, active_sessions);
    return;
  }

  int intr = 0;

  sem_wait(&empty); // aguarda espaço disponível no buffer
  pthread_mutex_lock(&lock);

  // ler request e colocar no buffer produtor_consumidor
  read_all(reg_fd, produtor_consumidor + (in_idx * CONNECTION_REQUEST_SIZE), CONNECTION_REQUEST_SIZE, &intr);
  close(reg_fd);
  in_idx = (in_idx + 1) % MAX_SESSION_COUNT; // incrementar indice de escrita

  pthread_mutex_unlock(&lock);
  sem_post(&full); // indica que há mais uma mensagem disponível
  return;
}

static void dispatch_threads(DIR *dir) {
  pthread_t *threads = malloc(max_threads * sizeof(pthread_t));

  if (threads == NULL) {
    fprintf(stderr, "Failed to allocate memory for threads\n");
    return;
  }

  struct SharedData thread_data = {dir, jobs_directory,
                                   PTHREAD_MUTEX_INITIALIZER};

  for (size_t i = 0; i < max_threads; i++) {
    if (pthread_create(&threads[i], NULL, get_file, (void *)&thread_data) !=
        0) {
      fprintf(stderr, "Failed to create thread %zu\n", i);
      pthread_mutex_destroy(&thread_data.directory_mutex);
      free(threads);
      return;
    }
  }

  // inicializa os semáforos
  sem_init(&empty, 0, MAX_SESSION_COUNT); // começa com MAX_SESSION_COUNT espaços para escrever 
  sem_init(&full, 0, 0); // começa com 0 mensagens para ler

  // criar threads gestoras
  pthread_t client_handler[MAX_SESSION_COUNT];
  for (size_t i = 0; i < MAX_SESSION_COUNT; i++) {
    size_t *arg = malloc(sizeof(size_t));
    *arg = i;
    if (pthread_create(&client_handler[i], NULL, handle_client, arg) != 0) {
      fprintf(stderr, "Failed to create thread\n");
      return;
    }
  }

  while (1){
    await_connection();
  }

  for (unsigned int i = 0; i < MAX_SESSION_COUNT; i++) {
    if (pthread_join(client_handler[i], NULL) != 0) {
      fprintf(stderr, "Failed to join thread %u\n", i);
      return;
    }
  }

  // destroi os semáforos e o mutex
  sem_destroy(&empty);
  sem_destroy(&full);
  pthread_mutex_destroy(&lock);

  for (unsigned int i = 0; i < max_threads; i++) {
    if (pthread_join(threads[i], NULL) != 0) {
      fprintf(stderr, "Failed to join thread %u\n", i);
      pthread_mutex_destroy(&thread_data.directory_mutex);
      free(threads);
      return;
    }
  }

  if (pthread_mutex_destroy(&thread_data.directory_mutex) != 0) {
    fprintf(stderr, "Failed to destroy directory_mutex\n");
  }

  free(threads);
}

void sigusr_handler(int s){
  fprintf(stderr, "Server received signal %d (SIGUSR1). Disconnecting all clients...\n", s);

  for (int i = 0; i < MAX_SESSION_COUNT; i++){
    client_disconnect(req_pipe[i], notif_pipe[i]);
    close(resp_pipe[i]); // este não é fechado pelo client_disconnect
  }
}

int main(int argc, char **argv) {
  signal(SIGUSR1, sigusr_handler);

  if (argc < 5) {
    write_str(STDERR_FILENO, "Usage: ");
    write_str(STDERR_FILENO, argv[0]);
    write_str(STDERR_FILENO, " <jobs_dir>");
    write_str(STDERR_FILENO, " <max_threads>");
    write_str(STDERR_FILENO, " <max_backups> \n");
    write_str(STDERR_FILENO, " <register_fifo_name> \n");
    return 1;
  }

  jobs_directory = argv[1];

  char *endptr;
  max_backups = strtoul(argv[3], &endptr, 10);

  if (*endptr != '\0') {
    fprintf(stderr, "Invalid max_proc value\n");
    return 1;
  }

  max_threads = strtoul(argv[2], &endptr, 10);

  if (*endptr != '\0') {
    fprintf(stderr, "Invalid max_threads value\n");
    return 1;
  }

  if (max_backups <= 0) {
    write_str(STDERR_FILENO, "Invalid number of backups\n");
    return 0;
  }

  if (max_threads <= 0) {
    write_str(STDERR_FILENO, "Invalid number of threads\n");
    return 0;
  }

  strcpy(reg_fifo, argv[4]);
  unlink(reg_fifo);
  if (mkfifo(reg_fifo, 0600)< 0){ // 0600: permissions for the owner only
    write_str(STDERR_FILENO, "Failed to create FIFO\n");
    return 1;
  }

  if (kvs_init()) {
    write_str(STDERR_FILENO, "Failed to initialize KVS\n");
    return 1;
  }

  DIR *dir = opendir(argv[1]);
  if (dir == NULL) {
    fprintf(stderr, "Failed to open directory: %s\n", argv[1]);
    return 0;
  }

  dispatch_threads(dir);

  if (closedir(dir) == -1) {
    fprintf(stderr, "Failed to close directory\n");
    return 0;
  }

  while (active_backups > 0) {
    wait(NULL);
    active_backups--;
  }

  unlink(reg_fifo);
  kvs_terminate();

  return 0;
}
