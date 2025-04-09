#ifndef THREADS_H
#define THREADS_H

typedef struct {
    int fd;
    int fd_out;
    char FileName[MAX_JOB_FILE_NAME_SIZE];
    int MAX_BACKUPS;
    int* thread_counter;
} thread_args_t;

/// function used by the threads
/// processes commands from a '.job' file
/// @param args pointer to arguments {fd, fd_out, fileName, maxBackups}
void* processFile (void* args);

extern pthread_mutex_t thread_counter_lock;

#endif 