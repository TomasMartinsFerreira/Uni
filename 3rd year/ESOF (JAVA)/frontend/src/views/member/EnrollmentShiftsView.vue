<template>
  <v-card class="table">
  <div class="text-h3">Select Shift</div>
    <v-data-table
      :headers="headers"
      :items="shifts"
      :search="search"
      disable-pagination
      :hide-default-footer="true"
      :mobile-breakpoint="0"
      data-cy="enrollmentShiftsTable"
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
          <v-btn @click="goBack"
            >Back</v-btn
          >
        </v-card-title>
      </template>
      <template v-slot:[`item.action`]="{ item }">
        <v-tooltip 
          v-if="hasVacancies(item)"
          bottom>
          <template v-slot:activator="{ on }">
            <v-icon
              class="mr-2 action-button"
              @click="selectShift(item)"
              v-on="on"
              data-cy="selectShiftButton"
              >check
            </v-icon>
          </template>
          <span>Select Shift</span>
        </v-tooltip>
      </template>
    </v-data-table>
     <participation-selection-dialog
      v-if="currentParticipation && editParticipationSelectionDialog"
      v-model="editParticipationSelectionDialog"
      :participation="currentParticipation"
      :enrollment="enrollment"
      v-on:save-participation="onSaveParticipation"
      v-on:close-participation-dialog="onCloseParticipationDialog"
    />
  </v-card>
</template>

<script lang="ts">
import { Component, Vue } from 'vue-property-decorator';
import RemoteServices from '@/services/RemoteServices';
import Shift from '@/models/shift/Shift';
import ParticipationSelectionDialog from '@/views/member/ParticipationSelectionDialog.vue';
import Participation from '@/models/participation/Participation';
import Enrollment from '@/models/enrollment/Enrollment';
import Activity from '@/models/activity/Activity';

@Component({
  components: {
    'participation-selection-dialog': ParticipationSelectionDialog,
  },
})
export default class EnrollmentShiftsView extends Vue {
  enrollment!: Enrollment;
  activity!: Activity;
  shifts: Shift[] = [];
  participations: Participation[] = [];
  search: string = '';
  currentParticipation: Participation | null = null;
  editParticipationSelectionDialog: boolean = false;

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
    {
      text: 'Actions',
      value: 'action',
      align: 'left',
      sortable: false,
      width: '5%',
    },
  ];

  async created() {
    this.enrollment = this.$store.getters.getEnrollment;
    this.activity = this.$store.getters.getActivity;

    if (this.enrollment !== null && this.enrollment.activityId !== null) {
      await this.$store.dispatch('loading');
      try {
        this.shifts = await RemoteServices.getShiftsByActivity(this.enrollment.activityId);
        this.participations = await RemoteServices.getActivityParticipations(this.enrollment.activityId);
        this.filterEnrollmentShifts();
      } catch (error) {
        await this.$store.dispatch('error', error);
      }
      await this.$store.dispatch('clearLoading');
    }
  }

  async selectShift(shift: Shift) {
    this.currentParticipation = new Participation();
    this.currentParticipation.shiftId = shift.id;

    if (this.checkIfActivityHasEnded()) {
      this.editParticipationSelectionDialog = true;
    } else {
      await RemoteServices.createParticipation(
                this.currentParticipation!.shiftId!,
                this.enrollment.id!,
                this.currentParticipation!,
              );
      this.goBack();
    }
  }

  checkIfActivityHasEnded() {
    let endingDate = new Date(this.activity.endingDate);
    let now = new Date();
    return endingDate < now;
  }

  onCloseParticipationDialog() {
    this.editParticipationSelectionDialog = false;
    this.currentParticipation = null;
  }

  async onSaveParticipation(participation: Participation) {
    this.editParticipationSelectionDialog = false;
    this.currentParticipation = null;
    this.enrollment.participating = true;

    this.shifts = await RemoteServices.getShiftsByActivity(participation.activityId!);
    this.filterEnrollmentShifts();

    this.goBack();
  }

  filterEnrollmentShifts() {
    if (!this.enrollment || !this.enrollment.shiftIds) {
      this.shifts = [];
      return;
    }

    this.shifts = this.shifts.filter(shift =>
      this.enrollment.shiftIds.includes(shift.id!)
    );
  }

  hasVacancies(shift: Shift) {
    const participantsCount = this.participations.filter(
      (p) => p.shiftId === shift.id
    ).length;
    return participantsCount < shift.participantsLimit;
  }

  goBack() {
    this.$router.push({ name: 'activity-enrollments' });
  }
}
</script>

<style scoped></style>
