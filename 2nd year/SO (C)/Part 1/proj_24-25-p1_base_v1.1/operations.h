#ifndef KVS_OPERATIONS_H
#define KVS_OPERATIONS_H

#include <stddef.h>

#include "constants.h"

/// Initializes the KVS state.
/// @return 0 if the KVS state was initialized successfully, 1 otherwise.
int kvs_init();

/// Destroys the KVS state.
/// @return 0 if the KVS state was terminated successfully, 1 otherwise.
int kvs_terminate();

/// Writes a key value pair to the KVS. If key already exists it is updated.
/// @param num_pairs Number of pairs being written.
/// @param keys Array of keys' strings.
/// @param values Array of values' strings.
/// @return 0 if the pairs were written successfully, 1 otherwise.
int kvs_write(size_t num_pairs, char keys[][MAX_STRING_SIZE], char values[][MAX_STRING_SIZE]);

/// Reads values from the KVS.
/// @param num_pairs Number of pairs to read.
/// @param keys Array of keys' strings.
/// @param fd_out File descriptor to write the (successful) output.
/// @return 0 if the key reading, 1 otherwise.
int kvs_read(size_t num_pairs, char keys[][MAX_STRING_SIZE],int fd_out);

/// Deletes key value pairs from the KVS.
/// @param num_pairs Number of pairs to read.
/// @param keys Array of keys' strings.
/// @param fd_out File descriptor to write the output.
/// @return 0 if the pairs were deleted successfully, 1 otherwise.
int kvs_delete(size_t num_pairs, char keys[][MAX_STRING_SIZE],int fd_out);

/// Writes the state of the KVS.
/// @param fd_out File descriptor to write the output.
void kvs_show(int fd_out);

void kvs_show_aux(int fd_out);

/// Creates a backup of the KVS state and stores it in the correspondent
/// backup file
/// @param FileName name of the file being read
/// @param backup_number to be used on the backup-file's name
/// @return 0 if the backup was successful, 1 otherwise.
int kvs_backup(char FileName[MAX_JOB_FILE_NAME_SIZE],int backup_number);

/// Waits for the last backup to be called.
void kvs_wait_backup(int* current_backups);

/// Waits for a given amount of time.
/// @param delay_us Delay in milliseconds.
void kvs_wait(unsigned int delay_ms);

#endif  // KVS_OPERATIONS_H
