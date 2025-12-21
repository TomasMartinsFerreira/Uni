// shell cmd: cc spark.c -o spark; ./spark

#include <errno.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h> // exit()
#include <string.h> // memset()
#include <time.h>
#include <math.h>

#define CACHE_MIN (4 * 1024)        // 4 KB
#define CACHE_MAX (4 * 1024 * 1024) // 4 MB
#define N_REPETITIONS (100)

// returns elapsed time in seconds
double get_elapsed(struct timespec const *start) {
    struct timespec end;
    clock_gettime(CLOCK_PROCESS_CPUTIME_ID, &end);

    double nanoseconds = ((end.tv_sec - start->tv_sec) * 1000000000) +
                         (end.tv_nsec - start->tv_nsec);
    return nanoseconds / 1000000000.0;
}

int main() {
    uint8_t *array = calloc(CACHE_MAX, sizeof(uint8_t));

    fputs("size\tstride\telapsed(s)\tcycles\n", stdout);

    for (size_t cache_size = CACHE_MIN; cache_size <= CACHE_MAX;
        cache_size = cache_size * 2) {
        fprintf(stderr, "\n[LOG]: running with array of size %zu KiB\n",
                cache_size >> 10);
        fflush(stderr);

        double total_time = 0;
        size_t accesses = 0;

        for (size_t stride = 1; stride <= cache_size / 2; stride = 2 * stride) {
            size_t limit = cache_size - stride + 1;

            /* warm up the cache */
            for (size_t index = 0; index < limit; index += stride) {
                array[index] = array[index] + 1;
            }
            
            clock_t const start_cycles = clock();

            struct timespec start_time;
            clock_gettime(CLOCK_PROCESS_CPUTIME_ID, &start_time);

            size_t n_iterations = 0;
            /* ************************************************************** */
            for (size_t repeat = 0; repeat < N_REPETITIONS * stride; repeat++) {
                for (size_t index = 0; index < limit;
                     index += stride, n_iterations++) {
                    array[index] = array[index] + 1;
                }
            }
            /* ************************************************************** */

            clock_t const cycle_count = clock() - start_cycles;
            double const time_diff = get_elapsed(&start_time);

            /******************************************************************
             * Note: You can change the code bellow to calculate more measures
             * as needed.
             *****************************************************************/

            /* Output to stdout */
            total_time += time_diff;
            //mean_time = time_diff / n_iterations;
            accesses += n_iterations;
            fprintf(stdout, "%zu\t%zu\t%lf\t%zu\t%lf\n", cache_size, stride,
                    time_diff, cycle_count, time_diff*1e9 / n_iterations);
        }

        fprintf(stdout, "--\n Array Size : %d\n", cache_size);
        fprintf(stdout, "--\n t2-t1 : %lf\n", total_time);
        fprintf(stdout, "--\n access a[i] : %zu\n", accesses);
        fprintf(stdout, "--\n mean access time : %lf ns\n", (total_time * 1e9)/accesses);

    }

    return 0;
}