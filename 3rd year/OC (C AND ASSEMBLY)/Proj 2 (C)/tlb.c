#include "tlb.h"

#include <stdbool.h>
#include <stdlib.h>
#include <string.h>

#include "clock.h"
#include "constants.h"
#include "log.h"
#include "memory.h"
#include "page_table.h"

typedef struct {
  bool valid;
  bool dirty;
  uint64_t last_access;
  va_t virtual_page_number;
  pa_dram_t physical_page_number;
} tlb_entry_t;

tlb_entry_t tlb_l1[TLB_L1_SIZE];
tlb_entry_t tlb_l2[TLB_L2_SIZE];

uint64_t tlb_l1_hits = 0;
uint64_t tlb_l1_misses = 0;
uint64_t tlb_l1_invalidations = 0;

uint64_t tlb_l2_hits = 0;
uint64_t tlb_l2_misses = 0;
uint64_t tlb_l2_invalidations = 0;

uint64_t get_total_tlb_l1_hits() { return tlb_l1_hits; }
uint64_t get_total_tlb_l1_misses() { return tlb_l1_misses; }
uint64_t get_total_tlb_l1_invalidations() { return tlb_l1_invalidations; }

uint64_t get_total_tlb_l2_hits() { return tlb_l2_hits; }
uint64_t get_total_tlb_l2_misses() { return tlb_l2_misses; }
uint64_t get_total_tlb_l2_invalidations() { return tlb_l2_invalidations; }

void tlb_init() {
  memset(tlb_l1, 0, sizeof(tlb_l1));
  memset(tlb_l2, 0, sizeof(tlb_l2));
  tlb_l1_hits = 0;
  tlb_l1_misses = 0;
  tlb_l1_invalidations = 0;
  tlb_l2_hits = 0;
  tlb_l2_misses = 0;
  tlb_l2_invalidations = 0;
}

void tlb_invalidate(va_t virtual_page_number) {
  
  increment_time(TLB_L1_LATENCY_NS);
  for (int i = 0; i < TLB_L1_SIZE; i++) {
    if (tlb_l1[i].valid && tlb_l1[i].virtual_page_number == virtual_page_number) {
      tlb_l1[i].valid = 0;
      tlb_l1_invalidations++;
    }
  }

  increment_time(TLB_L2_LATENCY_NS);
  for (int i = 0; i < TLB_L2_SIZE; i++) {
    if (tlb_l2[i].valid && tlb_l2[i].virtual_page_number == virtual_page_number) {
      tlb_l2[i].valid = 0;
      tlb_l2_invalidations++;
    }
  }
  
}


pa_dram_t tlb_translate(va_t virtual_address, op_t op) {
  
  tlb_entry_t tlb_entry;
  bool hit_l1 = false; 
  bool hit_l2 = false;

  virtual_address &= VIRTUAL_ADDRESS_MASK;

  va_t virtual_page_number =
      (virtual_address >> PAGE_SIZE_BITS) & PAGE_INDEX_MASK;

  va_t offset = virtual_address & PAGE_OFFSET_MASK;
 
  // checkar se esta no l1
  increment_time(TLB_L1_LATENCY_NS);

  for (int i = 0; i < TLB_L1_SIZE; i++) {
    if (tlb_l1[i].valid && tlb_l1[i].virtual_page_number == virtual_page_number) {
      hit_l1 = true;
      tlb_l1_hits++;
      tlb_l1[i].last_access = 0;

      if (op == OP_WRITE) {
        tlb_l1[i].dirty = 1;
      }
      tlb_entry = tlb_l1[i];

    } else if (tlb_l1[i].valid) {
      tlb_l1[i].last_access++;
    }
  }

  if (hit_l1) {
    return (tlb_entry.physical_page_number << PAGE_SIZE_BITS) | offset;
  } 
  
  tlb_l1_misses++;

  // checkar se esta no l2
  increment_time(TLB_L2_LATENCY_NS);

  for (int i = 0; i < TLB_L2_SIZE; i++) {
    if (tlb_l2[i].valid && tlb_l2[i].virtual_page_number == virtual_page_number) {
      hit_l2 = true;
      tlb_l2_hits++;
      tlb_l2[i].last_access = 0;
      
      if (op == OP_WRITE) {
        tlb_l2[i].dirty = 1;
      }
      tlb_entry = tlb_l2[i];

    } else if (tlb_l2[i].valid) {
      tlb_l2[i].last_access++;
    }
  }


  if (!hit_l2) {
    tlb_l2_misses++;

    // incializacao da struct do address para adicionar na tlb
    //---------------------------------------------------------------------------------------------------------------
    tlb_entry.last_access = 0;
    tlb_entry.virtual_page_number = virtual_page_number;
    tlb_entry.physical_page_number = (page_table_translate(virtual_address, op) >> PAGE_SIZE_BITS) & PAGE_INDEX_MASK;
    tlb_entry.valid = 1;
    tlb_entry.dirty = (op == OP_WRITE) ? 1 : 0;
    //---------------------------------------------------------------------------------------------------------------
  }

  // como saber em que cache escrever o address em caso de este nao existir na tlb? a resposta e dar fill da l1 para a l2
  int insert_l1 = -1;
  int insert_l2 = -1;

  // procurar espaco vazio na l1
  for (int i = 0; i < TLB_L1_SIZE; i++) {
    if (!tlb_l1[i].valid) { 
      insert_l1 = i;
      break;
    }
  }

  // se nao houver espaco na tlb
  if (insert_l1 == -1) {
    insert_l1 = 0;
    uint64_t oldest_time_l1 = 0;
    for (int i = 0; i < TLB_L1_SIZE; i++) {
      if (tlb_l1[i].last_access > oldest_time_l1) {
        oldest_time_l1 = tlb_l1[i].last_access;  
        insert_l1 = i;    
      }
    }
  }
  
  // procurar espaco vazio na l2
  if (!hit_l2) {
    for (int i = 0; i < TLB_L2_SIZE; i++) {
      if (!tlb_l2[i].valid) { 
        insert_l2 = i;
        break;
      }
    }

  if (insert_l2 == -1) {
    insert_l2 = 0;
    uint64_t oldest_time_l2 = 0;
    for (int i = 0; i < TLB_L2_SIZE; i++) {
      if (tlb_l2[i].last_access > oldest_time_l2) {
        oldest_time_l2 = tlb_l2[i].last_access;
        insert_l2 = i;
      }
    }
  }
}


  // se for uma entrada dirty em l1 temos de propagar o dirty bit para l2
  if (tlb_l1[insert_l1].dirty) {
    for (int i = 0; i < TLB_L2_SIZE; i++) {
      if (tlb_l2[i].valid && tlb_l2[i].virtual_page_number == tlb_l1[insert_l1].virtual_page_number) {
        tlb_l2[i].dirty = 1;
        break;
      }
    }
  } 
  
  // se for uma entrada dirty em L2 temos de fazer write back
  if (!hit_l2 && tlb_l2[insert_l2].dirty && tlb_l2[insert_l2].virtual_page_number != tlb_l1[insert_l1].virtual_page_number) {
    write_back_tlb_entry(tlb_l2[insert_l2].physical_page_number << PAGE_SIZE_BITS);
  }

  // -----------------------------------------------------------------------------------------------------------------------------------------
  // adicionar as posicoes respetivas(tlb_l1[insert_l1], tlb_l2[insert_l2]) o novo address
  tlb_l1[insert_l1] = tlb_entry;

  if (!hit_l2) {
    tlb_l2[insert_l2] = tlb_entry;
  }
  //-------------------------------------------------------------------------------------------- --------------------------------------------- 

  return (tlb_entry.physical_page_number << PAGE_SIZE_BITS) | offset;
}
