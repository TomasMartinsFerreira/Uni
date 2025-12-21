#include "packet-format.h"
#include <limits.h>
#include <netdb.h>
#include <stdbool.h>
#include <stddef.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/time.h>

#define SET_BIT(x, n) ((x) |= (1U << (n))) // set bit to one 
#define CHECK_BIT(x, n) (((x) >> (n)) & 1U) // check if bit is one

int main(int argc, char *argv[]) {
  char *file_name = argv[1];
  char *host = argv[2];
  int port = atoi(argv[3]);
  int sender_window = atoi(argv[4]);

  FILE *file = fopen(file_name, "r");
  if (!file) {
    perror("fopen");
    exit(EXIT_FAILURE);
  }

  // Prepare server host address.
  struct hostent *he;
  if (!(he = gethostbyname(host))) {
    perror("gethostbyname");
    exit(EXIT_FAILURE);
  }

  struct sockaddr_in srv_addr = {
      .sin_family = AF_INET,
      .sin_port = htons(port),
      .sin_addr = *((struct in_addr *)he->h_addr),
  };

  int sockfd = socket(AF_INET, SOCK_DGRAM, 0);
  if (sockfd == -1) {
    perror("socket");
    exit(EXIT_FAILURE);
  }
  bool window_sent = false;
  struct timeval tv;  // Set timeout for receiving ACKs.
  tv.tv_sec = 1;        // timeout in seconds
  tv.tv_usec = 0;       // microseconds
  if (setsockopt(sockfd, SOL_SOCKET, SO_RCVTIMEO, &tv, sizeof(tv)) < 0) {
    perror("setsockopt");
    exit(EXIT_FAILURE);
  }
  bool eof_reached = false;
  int last_data_sent = 0;
  int total_timeouts = 0;
  int current_ack_num = 0;
  int last_ack_num = 0;
  int dup_ack_count = 0;
  uint32_t seq_num = 0;
  data_pkt_t data_pkt;
  ack_pkt_t ack_pkt;
  ack_pkt.seq_num = 0;
  ack_pkt.selective_acks = 0;
  size_t data_len;

  do { // Generate segments from file, until the the end of the file.
    // Prepare data segment.
    if (window_sent == false) {
      for (int i = 0; i < sender_window; i++) {
        data_pkt.seq_num = htonl(seq_num++);
        // Load data from file.
        data_len = fread(data_pkt.data, 1, sizeof(data_pkt.data), file);

        // if last packet already acked, break
        if (data_len < sizeof(data_pkt.data) && CHECK_BIT(ntohl(ack_pkt.selective_acks),ntohl(data_pkt.seq_num)-ntohl(ack_pkt.seq_num) - 1) == 1 && ntohl(data_pkt.seq_num)!= ntohl(ack_pkt.seq_num)){
          break;
        }

        // if packet already acked, continue
        if (CHECK_BIT(ntohl(ack_pkt.selective_acks),
            ntohl(data_pkt.seq_num)-ntohl(ack_pkt.seq_num) - 1) == 1 && ntohl(data_pkt.seq_num)!= ntohl(ack_pkt.seq_num)){
          continue;
        }
        
        // Send segment.
        ssize_t sent_len =
          sendto(sockfd, &data_pkt, offsetof(data_pkt_t, data) + data_len, 0,
                (struct sockaddr *)&srv_addr, sizeof(srv_addr));

        printf("Sending segment %d, size %ld.\n", ntohl(data_pkt.seq_num),
            offsetof(data_pkt_t, data) + data_len);

        if (sent_len != offsetof(data_pkt_t, data) + data_len) {
          fprintf(stderr, "Truncated packet.\n");
          exit(EXIT_FAILURE);
        }
        
        if (data_len < sizeof(data_pkt.data)) {
          last_data_sent = ntohl(data_pkt.seq_num);
          eof_reached = true;
          break;
        }
        window_sent = true;
      }
    }
    
    // Wait for ACK.
    ssize_t len =
        recvfrom(sockfd, &ack_pkt, sizeof(ack_pkt), 0,
                (struct sockaddr *)&srv_addr, &(socklen_t){sizeof(srv_addr)});
  
    // if timeout send last ack received
    if (len < 0) {
      total_timeouts++;
      // after max retries, exit failure
      if (total_timeouts >= MAX_RETRIES) {
        close(sockfd);
        fclose(file);
        return EXIT_FAILURE;
      }
      
      int ack_seq_num = ntohl(ack_pkt.seq_num);
      int data_seq_num = seq_num;
      int chunks_to_seek = -MAX_CHUNK_SIZE*( data_seq_num - ack_seq_num - 
                          (data_len < sizeof(data_pkt.data) ? 1 : 0));
      fseek(file, chunks_to_seek, SEEK_CUR);
      seq_num = ntohl(ack_pkt.seq_num);
      window_sent = false;
      continue;
    }

    if (last_ack_num == 0) {
      last_ack_num = ntohl(ack_pkt.seq_num);
    }
    current_ack_num = ntohl(ack_pkt.seq_num);

    // Check for termination condition.
    if (last_data_sent == current_ack_num - 1 && last_data_sent != 0) {
      close(sockfd);
      fclose(file);
      return EXIT_SUCCESS;
    }
    total_timeouts = 0;
    if (eof_reached == false && sender_window + ntohl(ack_pkt.seq_num) - 1 > ntohl(data_pkt.seq_num)) {
        data_pkt.seq_num = htonl(seq_num++);

        data_len = fread(data_pkt.data, 1, sizeof(data_pkt.data), file);
        
        // Send segment.
        ssize_t sent_len =
          sendto(sockfd, &data_pkt, offsetof(data_pkt_t, data) + data_len, 0,
                (struct sockaddr *)&srv_addr, sizeof(srv_addr));

        printf("Sending segment %d, size %ld.\n", ntohl(data_pkt.seq_num),
            offsetof(data_pkt_t, data) + data_len);

        if (sent_len != offsetof(data_pkt_t, data) + data_len) {
          fprintf(stderr, "Truncated packet.\n");
          exit(EXIT_FAILURE);
        }
        
        if (data_len < sizeof(data_pkt.data)) {
          last_data_sent = ntohl(data_pkt.seq_num);
          eof_reached = true;
        }
    }

    // for dup ack
    if (last_ack_num == current_ack_num) {
      dup_ack_count++;
      // if 2 dup acks, resend last unacked packet
      if (dup_ack_count == MAX_RETRIES) {
        ssize_t len =
        recvfrom(sockfd, &ack_pkt, sizeof(ack_pkt), 0,
                (struct sockaddr *)&srv_addr, &(socklen_t){sizeof(srv_addr)});
        if (len < 0) { // 2 dup acks then timeout
              total_timeouts++;
              int ack_seq_num = ntohl(ack_pkt.seq_num);
              int data_seq_num = seq_num;
              int chunks_to_seek = -MAX_CHUNK_SIZE*( data_seq_num - ack_seq_num - 
                              (data_len < sizeof(data_pkt.data) ? 1 : 0));
              fseek(file, chunks_to_seek, SEEK_CUR);
              seq_num = ntohl(ack_pkt.seq_num);
              dup_ack_count = 0;
              window_sent = false;
              eof_reached = false;
            } else { // 3 dup acks
              int current_data_len = data_len;
              int ack_seq_num = ntohl(ack_pkt.seq_num);
              int data_seq_num = seq_num;
              int chunks_to_seek = -MAX_CHUNK_SIZE*( data_seq_num - ack_seq_num - 
                              (data_len < sizeof(data_pkt.data) ? 1 : 0));
              fseek(file, chunks_to_seek, SEEK_CUR);
              seq_num = ntohl(ack_pkt.seq_num);
              dup_ack_count = 0;
              data_pkt.seq_num = htonl(seq_num++);

              data_len = fread(data_pkt.data, 1, sizeof(data_pkt.data), file);
       
              sendto(sockfd, &data_pkt, offsetof(data_pkt_t, data) + data_len, 0,
                    (struct sockaddr *)&srv_addr, sizeof(srv_addr));

              printf("Sending segment %d, size %ld.\n", ntohl(data_pkt.seq_num),
              offsetof(data_pkt_t, data) + data_len);
              seq_num = data_seq_num;
              fseek(file, -chunks_to_seek - MAX_CHUNK_SIZE, SEEK_CUR);
              data_len = current_data_len;
            }
        }
      
    } else {
      last_ack_num = 0;
      dup_ack_count = 0;
    }
    

  } while (!(feof(file) && data_len < sizeof(data_pkt.data)) || ntohl(ack_pkt.seq_num) - 1 !=  ntohl(data_pkt.seq_num));
  // Clean up and exit.
  close(sockfd);
  fclose(file);

  return EXIT_SUCCESS;
}