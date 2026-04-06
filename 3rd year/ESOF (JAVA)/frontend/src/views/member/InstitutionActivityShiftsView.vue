<template>
  <v-card class="table">
    <v-data-table
      :headers="headers"
      :items="shifts"
      :search="search"
      disable-pagination
      :hide-default-footer="true"
      :mobile-breakpoint="0"
      data-cy="memberShiftsTable"
    >
      <template v-slot:top>
         <v-card-title>
          <v-text-field
            v-model="search"
            append-icon="search"
            label="Search"
            class="mx-2"
          />
          <v-spacer />
          <v-btn 
            :disabled="!canCreate"
            color="primary" dark
            @click="newShift"
            data-cy="newShift"
            >New Shift</v-btn
          >
          <v-btn @click="goBack" data-cy="newActivity"
            >Back</v-btn
          >
        </v-card-title>
      </template>
    </v-data-table>
     <shift-dialog
      v-if="currentShift && editShiftDialog"
      v-model="editShiftDialog"
      :shift="currentShift"
      v-on:save-shift="onSaveShift"
      v-on:close-shift-dialog="onCloseShiftDialog"
    />
  </v-card>
</template>

<script lang="ts">
import { Component, Vue } from 'vue-property-decorator';
import RemoteServices from '@/services/RemoteServices';
import Shift from '@/models/shift/Shift';
import ShiftDialog from '@/views/member/ShiftDialog.vue';
import Activity from '@/models/activity/Activity';

@Component({
  components: {
    'shift-dialog': ShiftDialog,
  },
})
export default class ActivityShiftsView extends Vue {
  shifts: Shift[] = [];
  search: string = '';
  currentShift: Shift | null = null;
  editShiftDialog: boolean = false;
  activity: Activity = new Activity();


  headers: object = [
    {
      text: 'Location',
      value: 'location',
      align: 'left',
      width: '5%',
    },
    {
      text: 'Participants Limit',
      value: 'participantsLimit',
      align: 'left',
      width: '5%',
    },
    {
      text: 'Start Time',
      value: 'formattedStartTime',
      align: 'left',
      width: '5%',
    },
    {
      text: 'End Time',
      value: 'formattedEndTime',
      align: 'left',
      width: '5%',
    },
  ];

  async created() {
    await this.$store.dispatch('loading');
    try {
      this.activity = this.$store.getters.getActivity;
      if (this.activity.id != null) {
        this.shifts = await RemoteServices.getShiftsByActivity(this.activity.id);
      }
    } catch (error) {
      await this.$store.dispatch('error', error);
    }
    await this.$store.dispatch('clearLoading');
  }

  get canCreate(): boolean {
    return this.activity.state == 'APPROVED'
  }

  newShift() {
    this.currentShift = new Shift();
    this.editShiftDialog = true;
  }

  onSaveShift(shift: Shift) {
    this.shifts = this.shifts.filter(
      (s) => s.id !== shift.id,
    );
    this.shifts.unshift(shift);
    this.editShiftDialog = false;
    this.currentShift = null;
  }

  onCloseShiftDialog() {
    this.currentShift = null;
    this.editShiftDialog = false;
  }


  goBack() {
    this.$router.push({ name: 'institution-activities' });
  }
}
</script>

<style scoped></style>
