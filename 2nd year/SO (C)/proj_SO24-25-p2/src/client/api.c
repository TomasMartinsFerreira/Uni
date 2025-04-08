#include <fcntl.h>
#include <stdio.h>
#include <string.h>
#include <sys/stat.h>
#include <unistd.h>
#include <stdlib.h>
#include "api.h"

#include "src/common/constants.h"
#include "src/common/protocol.h"
#include "src/common/io.h"

char req_pipe_pathname[MAX_PIPE_PATH_LENGTH];
char resp_pipe_pathname[MAX_PIPE_PATH_LENGTH];
char notif_pipe_pathname[MAX_PIPE_PATH_LENGTH];
int req_pipe;
int resp_pipe;

void print_operation_result(char result, char op_code){
  char* operation;
  switch (op_code){
  case OP_CODE_CONNECT:
    operation = "connect";
    break;
  case OP_CODE_DISCONNECT:
    operation = "disconnect";
    break;
  case OP_CODE_SUBSCRIBE:
    operation = "subscribe";
    break;
  case OP_CODE_UNSUBSCRIBE:
    operation = "unsubscribe";
    break;
  }
  printf("Server returned %c for operation: %s\n", result, operation);
}

int kvs_connect(char const *req_pipe_path, char const *resp_pipe_path,
                char const *server_pipe_path, char const *notif_pipe_path,
                int *notif_pipe) {
  // store pathnames to then use in kvs_disconnect
  strcpy(req_pipe_pathname, req_pipe_path);
  strcpy(resp_pipe_pathname, resp_pipe_path);
  strcpy(notif_pipe_pathname, notif_pipe_path);
  
  // create pipes
  unlink(req_pipe_path);
  unlink(resp_pipe_path);
  unlink(notif_pipe_path);
  if (mkfifo(req_pipe_path, 0600) < 0 || mkfifo(resp_pipe_path, 0600) < 0 ||
        mkfifo(notif_pipe_path, 0600)< 0){
    return 1;
  }

  size_t REQUEST_SIZE = sizeof(char) + 3 * MAX_PIPE_PATH_LENGTH + 1;
  char buffer[REQUEST_SIZE];

  // build buffer
  memset(buffer, '\0', REQUEST_SIZE);
  buffer[0] = OP_CODE_CONNECT;

  // Copy the pipe names into their sections
  strncpy(buffer + 1, req_pipe_path, MAX_PIPE_PATH_LENGTH);
  strncpy(buffer + 1 + MAX_PIPE_PATH_LENGTH, resp_pipe_path, MAX_PIPE_PATH_LENGTH);
  strncpy(buffer + 1 + 2 * MAX_PIPE_PATH_LENGTH, notif_pipe_path, MAX_PIPE_PATH_LENGTH);

  // send pipe names to the server
  int server_fd = open(server_pipe_path, O_WRONLY);
  if (server_fd < 0){
    fprintf(stderr, "Failed to open FIFO\n.");
    return 1;
  }

  write_all(server_fd, buffer, REQUEST_SIZE);
  close(server_fd);

  // open pipes
  if ((req_pipe = open(req_pipe_path, O_WRONLY)) < 0){
    fprintf(stderr, "Failed to open FIFO\n");
    return 1;
  }
  if ((resp_pipe = open(resp_pipe_path, O_RDONLY)) < 0){
    fprintf(stderr, "Failed to open FIFO\n");
    return 1;
  }
  if ((*notif_pipe = open(notif_pipe_path, O_RDONLY)) < 0){
    fprintf(stderr, "Failed to open FIFO\n");
    return 1;
  }

  // read response
  int intr = 0;
  read_all(resp_pipe, buffer, 2, &intr);
  print_operation_result(buffer[1], OP_CODE_CONNECT);
  if (buffer[1] != '0'){
    return 1;
  }
  return 0;
}

int kvs_disconnect(int notif_pipe) {
  size_t REQUEST_SIZE = sizeof(char);
  char buffer[REQUEST_SIZE + 1];
  int intr = 0;

  // build buffer
  sprintf(buffer, "%d", OP_CODE_DISCONNECT);

  // send request
  write_all(req_pipe, buffer, REQUEST_SIZE);

  // read response
  read_all(resp_pipe, buffer, 2, &intr);
  print_operation_result(buffer[1], OP_CODE_DISCONNECT);
  if (buffer[1] != '0'){
    return 1;
  }

  // close and unlink pipes
  close(req_pipe);
  close(resp_pipe);
  close(notif_pipe);
  unlink(req_pipe_pathname);
  unlink(resp_pipe_pathname);
  unlink(notif_pipe_pathname);
  return 0;
}

int kvs_subscribe(const char *key) {
  size_t REQUEST_SIZE = sizeof(char) + MAX_STRING_SIZE + 1;
  char buffer[REQUEST_SIZE];
  int intr = 0;

  // build buffer
  sprintf(buffer, "%d%s", OP_CODE_SUBSCRIBE, key);

  // send request
  write_all(req_pipe, buffer, REQUEST_SIZE);

  // read response
  if (read_all(resp_pipe, buffer, 2, &intr) == 0){
    exit(1); // terminar processo (cliente) caso o servidor tenha fechado os pipes
  }
  print_operation_result(buffer[1], OP_CODE_SUBSCRIBE);
  if (buffer[1] != '0'){
    return 1;
  }

  return 0;
}

int kvs_unsubscribe(const char *key) {
  size_t REQUEST_SIZE = sizeof(char) + MAX_STRING_SIZE + 1;
  char buffer[REQUEST_SIZE];
  int intr = 0;

  // build buffer
  sprintf(buffer, "%d%s", OP_CODE_UNSUBSCRIBE, key);

  // send request
  write_all(req_pipe, buffer, REQUEST_SIZE);

  // read response
  if (read_all(resp_pipe, buffer, 2, &intr) == 0){
    exit(1); // terminar processo (cliente) caso o servidor tenha fechado os pipes
  };
  print_operation_result(buffer[1], OP_CODE_UNSUBSCRIBE);
  if (buffer[1] != '0'){
    return 1;
  }

  return 0;
}
