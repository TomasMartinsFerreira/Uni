#include "packet-format.h"
#include <arpa/inet.h>
#include <limits.h>
#include <netinet/in.h>
#include <stdbool.h>
#include <stddef.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/socket.h>
#include <unistd.h>
#include <sys/time.h>

#define SET_BIT(x, n) ((x) |= (1U << (n))) // set bit to one 
#define CHECK_BIT(x, n) (((x) >> (n)) & 1U) // check if bit is one

int main(int argc, char *argv[]) {
  char *file_name = argv[1];
  int port = atoi(argv[2]);
  int receiver_window = atoi(argv[3]);

  FILE *file = fopen(file_name, "w");
  if (!file) {
    perror("fopen");
    exit(EXIT_FAILURE);
  }

  // Prepare server socket.
  int sockfd = socket(AF_INET, SOCK_DGRAM, 0);
  if (sockfd == -1) {
    perror("socket");
    exit(EXIT_FAILURE);
  }

  // Allow address reuse so we can rebind to the same port,
  // after restarting the server.
  if (setsockopt(sockfd, SOL_SOCKET, SO_REUSEADDR, &(int){1}, sizeof(int)) <
      0) {
    perror("setsockopt");
    exit(EXIT_FAILURE);
  }

  struct sockaddr_in srv_addr = {
      .sin_family = AF_INET,
      .sin_addr.s_addr = htonl(INADDR_ANY),
      .sin_port = htons(port),
  };
  if (bind(sockfd, (struct sockaddr *)&srv_addr, sizeof(srv_addr))) {
    perror("bind");
    exit(EXIT_FAILURE);
  }
  fprintf(stderr, "Receiving on port: %d\n", port);

  struct timeval tv;  // Set timeout for receiving ACKs.
  tv.tv_sec = 4;        // timeout in seconds
  tv.tv_usec = 0;       // microseconds
  if (setsockopt(sockfd, SOL_SOCKET, SO_RCVTIMEO, &tv, sizeof(tv)) < 0) {
    perror("setsockopt");
    exit(EXIT_FAILURE);
  }

  int final_packet_received = 0;
  ack_pkt_t ack_pkt;
  ack_pkt.selective_acks = 0;
  uint32_t seq_num = 0;
  ssize_t len;
  
  do { // Iterate over segments, until last the segment is detected.
    // Receive segment.
    struct sockaddr_in src_addr;
    data_pkt_t data_pkt;
    ack_pkt.seq_num = htonl(++seq_num);
    len =
        recvfrom(sockfd, &data_pkt, sizeof(data_pkt), 0,
                 (struct sockaddr *)&src_addr, &(socklen_t){sizeof(src_addr)});
    if (len < 0){
      if (final_packet_received == ntohl(ack_pkt.seq_num) - 2) {
        close(sockfd);
        fclose(file);
        return EXIT_SUCCESS; // Timeout
      }
      close(sockfd);
      fclose(file);
      file = fopen(file_name, "w"); // clears the file
      fclose(file);
      return EXIT_FAILURE; // Timeout
    }
    
    printf("Received segment %d, size %ld.\n", ntohl(data_pkt.seq_num), len);

    // remember final packet seq num 
    if (len < sizeof(data_pkt.data)) {
      final_packet_received = ntohl(data_pkt.seq_num);
    }
    
    if (ntohl(ack_pkt.seq_num) + receiver_window - 2 >= ntohl(data_pkt.seq_num) &&
        ntohl(data_pkt.seq_num) > ntohl(ack_pkt.seq_num) - 1) {

      int ack_seq_num = ntohl(ack_pkt.seq_num) - 1;
      int data_seq_num = ntohl(data_pkt.seq_num);
      int chunks_to_skip = MAX_CHUNK_SIZE*(data_seq_num - ack_seq_num);
      int chunks_to_return = -MAX_CHUNK_SIZE*( data_seq_num - ack_seq_num) -
                          (len - offsetof(data_pkt_t, data));
      fseek(file, chunks_to_skip, SEEK_CUR);
      fwrite(data_pkt.data, 1, len - offsetof(data_pkt_t, data), file);
      fseek(file, chunks_to_return, SEEK_CUR);
      
      SET_BIT(ack_pkt.selective_acks,
              ntohl(data_pkt.seq_num)-ntohl(ack_pkt.seq_num));

      ack_pkt.seq_num = htonl(--seq_num);
      ack_pkt.selective_acks = htonl(ack_pkt.selective_acks);
      sendto(sockfd, &ack_pkt, sizeof(ack_pkt), 0, 
              (struct sockaddr *)&src_addr, sizeof(src_addr));
      ack_pkt.selective_acks = ntohl(ack_pkt.selective_acks);
      continue; // Inside window.
    } else if (ntohl(data_pkt.seq_num) > ntohl(ack_pkt.seq_num) - 1) {
      ack_pkt.seq_num = htonl(--seq_num);
      ack_pkt.selective_acks = htonl(ack_pkt.selective_acks);
      sendto(sockfd, &ack_pkt, sizeof(ack_pkt), 0, 
              (struct sockaddr *)&src_addr, sizeof(src_addr));
      ack_pkt.selective_acks = ntohl(ack_pkt.selective_acks);
      continue; // Out of window, ignore.
    }

    if (ntohl(ack_pkt.seq_num) - 1 > ntohl(data_pkt.seq_num)) {
      // Send segment.
      ack_pkt.seq_num = htonl(--seq_num);
      ack_pkt.selective_acks = htonl(ack_pkt.selective_acks);
      sendto(sockfd, &ack_pkt, sizeof(ack_pkt), 0, 
              (struct sockaddr *)&src_addr, sizeof(src_addr));
      ack_pkt.selective_acks = ntohl(ack_pkt.selective_acks);
      continue; // Duplicate packet, ignore.
    }


    // Write data to file.
    fwrite(data_pkt.data, 1, len - offsetof(data_pkt_t, data), file);
    // Update ACK packet. and check for contiguous acks
    for (int i = 1; i < receiver_window; i++) {
      if (CHECK_BIT(ack_pkt.selective_acks,0) == 1) {
        ack_pkt.seq_num = htonl(++seq_num);
        fseek(file, MAX_CHUNK_SIZE, SEEK_CUR);
        ack_pkt.selective_acks >>= 1;
      } else {
        break;
      }
    }
    
    // Send segment.
    ack_pkt.selective_acks = htonl(ack_pkt.selective_acks);
    sendto(sockfd, &ack_pkt, sizeof(ack_pkt), 0, 
          (struct sockaddr *)&src_addr, sizeof(src_addr));
    ack_pkt.selective_acks = ntohl(ack_pkt.selective_acks);



  } while (true);

  // Clean up and exit.
  close(sockfd);
  fclose(file);

  return EXIT_SUCCESS;
}
