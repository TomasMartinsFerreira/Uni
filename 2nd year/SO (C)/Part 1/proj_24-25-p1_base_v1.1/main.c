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

int main(int argc, char const *argv[]) {

    if (argc != 4){
        fprintf(stderr, "Usage: %s <jobs_directory> <max_simultaneous_backups> <max_threads>\n"
                    "Arguments:\n"
                    "<jobs_directory>: path to the directory containing the '.job' files\n"
                    "<max_simultaneous_backups>: number of maximum simultaneous backups (integer)\n"
                    "<max_threads>: maximum number of threads that can proccess '.job' files.\n", argv[0]);
        return 1;
    }

    if (kvs_init()) {
        fprintf(stderr, "Failed to initialize KVS\n");
        return 1;
    }

    // parse argv[1] (folder_path)

    const char *folder_path = argv[1];
    DIR *dir = opendir(folder_path);

    if (dir == NULL){
        fprintf(stderr, "Error: Unable to open directory '%s'.\n"
                    "Make sure this is a valid directory path.\n", argv[1]);
        return 1;
    }

    // parse argv[2] (MAX_BACKUPS)

    char *endptr;
    const int MAX_BACKUPS = (int)strtol(argv[2], &endptr, 10);

    if (*endptr != '\0' || MAX_BACKUPS <= 0) {
        fprintf(stderr, "Error: <max_simultaneous_backups> must be a positive integer.\n");
        closedir(dir);
        return 1;
    }

    // parse argv[3] (MAX_THREADS)

    const int MAX_THREADS = (int)strtol(argv[3], &endptr, 10);

    if (*endptr != '\0' || MAX_THREADS <= 0) {
        fprintf(stderr, "Error: <max_threads> must be a positive integer.\n");
        closedir(dir);
        return 1;
    }
    
    struct dirent *entry;
    pthread_t tid[MAX_THREADS];
    int thread_counter = 0;
    thread_args_t args[MAX_THREADS];

    // loop through every file in the directory
    while ((entry = readdir(dir)) != NULL) {
        if (strstr(entry->d_name,".job")) {
            char fullPath[MAX_JOB_FILE_NAME_SIZE + sizeof(folder_path) + 1];
            snprintf(fullPath, sizeof(fullPath), "%s/%s", folder_path, entry->d_name);
            int fd = open(fullPath, O_RDONLY);

            if (fd < 0){
                fprintf(stderr, "Failed to open the file\n");
                closedir(dir);
                return 1;
            }

            char FileName[MAX_JOB_FILE_NAME_SIZE];
            strcpy(FileName,fullPath);            
            strcpy(&FileName[strlen(FileName) - 3], "out");

            int fd_out = open(FileName, O_CREAT | O_TRUNC | O_WRONLY, S_IRUSR | S_IWUSR);
            
            if (fd_out < 0){
                fprintf(stderr, "Failed to open the file\n");
                close(fd);
                closedir(dir);
                return 1;
            }

            // wait until one of the active threads finishes execution
            // 'processFile' directly decrements thread_counter
            while (thread_counter == MAX_THREADS);

            // para garantir que thread_counter não é alterado antes de chamar pthread_create
            // isso levaria a indexar uma posição errada do array args
            pthread_mutex_lock(&thread_counter_lock);

            // preparing arguments for the new thread
            args[thread_counter] = (thread_args_t){.fd = fd, .fd_out = fd_out, 
                                .MAX_BACKUPS = MAX_BACKUPS, .thread_counter = &thread_counter};

            strcpy(args[thread_counter].FileName, FileName);

            // create a new thread that will process the current file
            if (pthread_create(&tid[thread_counter], NULL, processFile, &args[thread_counter]) != 0){
                fprintf(stderr, "Failed to create thread\n");
                close(fd);
                close(fd_out);
                closedir(dir);
                return 1;
            }
            thread_counter++;
            pthread_mutex_unlock(&thread_counter_lock);
        }
    }
    // esperar que as threads acabem todas
    for (int i = 0; i < thread_counter; i++){
        pthread_join(tid[i], NULL);
    }

    kvs_terminate();
    closedir(dir);
    return 0;
}