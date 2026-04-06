<template>
  <v-dialog v-model="dialog" persistent width="1300">
    <v-card>
      <v-card-title>
        <span class="headline">
          {{
            createShift && createShift.id === null
              ? 'New Shift'
              : 'Edit Shift'
          }}
        </span>
      </v-card-title>
      <v-card-text>
        <v-form ref="form" lazy-validation>
          <v-row>
            <v-col cols="12">
              <v-text-field
                label="*Location"
                :rules="[
                  (v) => !!v || 'Location is required',
                  (v) => isBetween20and200(v) || 'Location must be between 20 and 200 characters'
                ]"
                required
                v-model="createShift.location"
                data-cy="LocationInput"
              ></v-text-field>
            </v-col>
            <v-col cols="12" sm="6" md="4">
              <v-text-field
                label="*Number of ParticipantsLimit"
                v-model="createShift.participantsLimit"
                data-cy="participantsNumberInput"
              ></v-text-field>
            </v-col>
            <v-col>
              <VueCtkDateTimePicker
                id="startTimeInput"
                v-model="createShift.startTime"
                format="YYYY-MM-DDTHH:mm:ssZ"
                label="*Starting Date"
                position="top"
              ></VueCtkDateTimePicker>
            </v-col>
            <v-col>
              <VueCtkDateTimePicker
                id="endDateInput"
                v-model="createShift.endTime"
                format="YYYY-MM-DDTHH:mm:ssZ"
                label="*Ending Date"
                position="top"
              ></VueCtkDateTimePicker>
            </v-col>
          </v-row>
        </v-form>
      </v-card-text>
      <v-card-actions>
        <v-spacer></v-spacer>
        <v-btn
          color="blue-darken-1"
          variant="text"
          @click="$emit('close-shift-dialog')"
        >
          Close
        </v-btn>
        <v-btn
          :disabled="!canSave"
          color="blue-darken-1"
          variant="text"
          @click="createShifts"
          data-cy="saveShift"
        >
          Save
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>
<script lang="ts">
import { Vue, Component, Prop, Model } from 'vue-property-decorator';
import Shift from '@/models/shift/Shift';
import RemoteServices from '@/services/RemoteServices';
import VueCtkDateTimePicker from 'vue-ctk-date-time-picker';
import 'vue-ctk-date-time-picker/dist/vue-ctk-date-time-picker.css';
import { ISOtoString } from '@/services/ConvertDateService';

Vue.component('VueCtkDateTimePicker', VueCtkDateTimePicker);
@Component({
  methods: { ISOtoString },
})
export default class ShiftDialog extends Vue {
  @Model('dialog', Boolean) dialog!: boolean;
  @Prop({ type: Shift, required: true }) readonly shift!: Shift;
    
  createShift: Shift = new Shift();
  activityId!: number;

  async created() {
    this.createShift = new Shift(this.shift);
    this.activityId = Number(this.$route.params.activityId);
  }

  get canSave(): boolean {
    return (
      !!this.createShift.participantsLimit &&
      this.isBetween20and200(this.createShift.location || '') &&
      !!this.createShift.startTime &&
      !!this.createShift.endTime
    );
  }

  isBetween20and200(Location: String) {
    const length = Location.length;
    return length >= 20 && length <= 200;
  }

  async createShifts() {
  const form = this.$refs.form as Vue & { validate: () => boolean };
  
  if (form.validate()) {
    try {
      const result = await RemoteServices.createShift(this.activityId, this.createShift);      
      this.$emit('save-shift', result);
    } catch (error) {
      await this.$store.dispatch('error', error);
    }
  }
}
}
</script>

<style scoped lang="scss">
::v-deep .v-dialog, 
::v-deep .v-overlay__content,
::v-deep .v-card {
  overflow: visible !important;
}

::v-deep .v-card-text {
  overflow: visible !important;
  padding-top: 40px !important; 
}

::v-deep .datetimepicker {
  top: auto !important;
  bottom: 100% !important;
  margin-bottom: 8px;
  
  z-index: 9999 !important;
}

::v-deep .datetimepicker.is-top {
  top: auto !important;
  bottom: 100% !important;
}
</style>