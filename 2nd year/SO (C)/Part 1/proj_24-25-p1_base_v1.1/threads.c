#include <limits.h>
#include <stdio.h>
#include <stdlib.h>
#include <fcntl.h>
#include <unistd.h>
#include <dirent.h>
#include <string.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/wait.h> 
#include <pthread.h>

#include "constants.h"
#include "parser.h"
#include "operations.h"
#include "threads.h"

pthread_mutex_t backups_lock = PTHREAD_MUTEX_INITIALIZER;
pthread_mutex_t thread_counter_lock = PTHREAD_MUTEX_INITIALIZER;
int CURRENT_BACKUPS = 0; // for all files

// function used by the threads
void* processFile (void* arg){
    thread_args_t *args = (thread_args_t*)arg;
    int fd = args->fd;
    int fd_out = args->fd_out;
    char FileName[MAX_JOB_FILE_NAME_SIZE];
    strcpy(FileName, args->FileName);
    int MAX_BACKUPS = args->MAX_BACKUPS;
    int* thread_counter = args->thread_counter;

    int CREATED_BACKUPS = 0; // for this file

    char keys[MAX_WRITE_SIZE][MAX_STRING_SIZE] = {0};
    char values[MAX_WRITE_SIZE][MAX_STRING_SIZE] = {0};
    unsigned int delay;
    size_t num_pairs;

    while (1) {
        switch (get_next(fd)) {
            case CMD_WRITE:
                num_pairs = parse_write(fd, keys, values, MAX_WRITE_SIZE, MAX_STRING_SIZE);
                if (num_pairs == 0) {
                    fprintf(stderr, "Invalid command. See HELP for usage\n");
                    continue;
                }

                if (kvs_write(num_pairs, keys, values)) {
                    fprintf(stderr, "Failed to write pair\n");
                }

                break;

            case CMD_READ:
                num_pairs = parse_read_delete(fd, keys, MAX_WRITE_SIZE, MAX_STRING_SIZE);
                if (num_pairs == 0) {
                    fprintf(stderr, "Invalid command. See HELP for usage\n");
                    continue;
                }
                if (kvs_read(num_pairs, keys, fd_out)) {
                    fprintf(stderr, "Failed to read pair\n");
                }
                break;

            case CMD_DELETE:
                num_pairs = parse_read_delete(fd, keys, MAX_WRITE_SIZE, MAX_STRING_SIZE);

                if (num_pairs == 0) {
                    fprintf(stderr, "Invalid command. See HELP for usage\n");
                    continue;
                }

                if (kvs_delete(num_pairs, keys,fd_out)) {
                    fprintf(stderr, "Failed to delete pair\n");
                }
                break;

            case CMD_SHOW:
                kvs_show_aux(fd_out);
                break;

            case CMD_WAIT:
                if (parse_wait(fd, &delay, NULL) == -1) {
                    fprintf(stderr, "Invalid command. See HELP for usage\n");
                    continue;
                }

                if (delay > 0) {
                    const char* message = "Waiting...\n";
                    write(fd_out, message, strlen(message));
                    kvs_wait(delay);
                }
                break;

            case CMD_BACKUP:
                // só queremos uma thread a alterar CURRENT_BACKUPS de cada vez
                pthread_mutex_lock(&backups_lock);
                if (CURRENT_BACKUPS >= MAX_BACKUPS) {
                    if (wait(NULL) > 0) // Aguarda e reduz o número de backups ativos
                        CURRENT_BACKUPS--;
                }
                pthread_mutex_unlock(&backups_lock);

                if (kvs_backup(FileName, ++CREATED_BACKUPS)) {
                    fprintf(stderr, "Failed to perform backup.\n");
                }

                // mesma coisa que em cima
                pthread_mutex_lock(&backups_lock);
                CURRENT_BACKUPS++;
                pthread_mutex_unlock(&backups_lock);
                break;

            case CMD_INVALID:
                fprintf(stderr, "Invalid command. See HELP for usage\n");
                break;

            case CMD_HELP:
                printf( 
                    "Available commands:\n"
                    "  WRITE [(key,value)(key2,value2),...]\n"
                    "  READ [key,key2,...]\n"
                    "  DELETE [key,key2,...]\n"
                    "  SHOW\n"
                    "  WAIT <delay_ms>\n"
                    "  BACKUP\n"
                    "  HELP\n"
                );

                break;
            
            case CMD_EMPTY:
                break;

            case EOC: // End Of Commands for the current file
                close(fd);
                close(fd_out);
                // wait for any unfinished backups
                kvs_wait_backup(&CURRENT_BACKUPS);

                pthread_mutex_lock(&thread_counter_lock);
                (*thread_counter)--; // estamos diretamente a decrementar a variável thread_counter da main.c
                pthread_mutex_unlock(&thread_counter_lock);
                return NULL;
        }
    }
}