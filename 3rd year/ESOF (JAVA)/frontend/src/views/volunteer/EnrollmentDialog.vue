<template>
  <v-dialog v-model="dialog" persistent width="800">
    <v-card>
      <v-card-title>
        <span class="headline">
          {{
            editEnrollment && editEnrollment.id === null
              ? 'New Application'
              : 'Edit Application'
          }}
        </span>
      </v-card-title>
      <v-card-text>
        <v-form ref="form" lazy-validation>
          <v-row>
            <v-col cols="12">
              <v-textarea
                label="*Motivation"
                :rules="[(v) => !!v || 'Motivation is required']"
                required
                v-model="editEnrollment.motivation"
                data-cy="motivationInput"
                auto-grow
                rows="1"
              ></v-textarea>
            </v-col>
             <v-col cols="12">
              <v-select
                label="*Shifts"
                v-model="editEnrollment.shiftIds"
                :items="shifts"
                multiple
                item-value="id"
                required
                data-cy="shiftSelect"
              >
                <template v-slot:item="{ item }">
                  {{ item.location }} ({{ item.formattedStartTime }} - {{ item.formattedEndTime }})
                </template>

                <template v-slot:selection="{ item }">
                  <v-chip small>
                    {{ item.formattedStartTime }} - {{ item.formattedEndTime }}
                  </v-chip>
                </template>
              </v-select>
            </v-col>
          </v-row>
        </v-form>
      </v-card-text>
      <v-card-actions>
        <v-spacer></v-spacer>
        <v-btn
          color="blue-darken-1"
          variant="text"
          @click="$emit('close-enrollment-dialog')"
        >
          Close
        </v-btn>
        <v-btn
          v-if="canSave"
          color="blue-darken-1"
          variant="text"
          @click="updateEnrollment"
          data-cy="saveEnrollment"
        >
          Save
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>
<script lang="ts">
import { Vue, Component, Prop, Model } from 'vue-property-decorator';
import RemoteServices from '@/services/RemoteServices';
import { ISOtoString } from '@/services/ConvertDateService';
import Enrollment from '@/models/enrollment/Enrollment';
import Shift from '@/models/shift/Shift';

@Component({
  methods: { ISOtoString },
})
export default class EnrollmentDialog extends Vue {
  @Model('dialog', Boolean) dialog!: boolean;
  @Prop({ type: Enrollment, required: true }) readonly enrollment!: Enrollment;
  @Prop({ type: Array, required: true }) readonly shifts!: Shift[];


  editEnrollment: Enrollment = new Enrollment();

  async created() {
    this.editEnrollment = new Enrollment(this.enrollment);
  }

  get canSave(): boolean {
    return (
      !!this.editEnrollment.motivation &&
      this.editEnrollment.motivation.length >= 10 &&
      this.editEnrollment.shiftIds &&          
      this.editEnrollment.shiftIds.length > 0 &&
      !this.hasOverlap
    );
  }
  
  get hasOverlap(): boolean {
    if (!this.editEnrollment.shiftIds || this.editEnrollment.shiftIds.length < 2) {
      return false;
    }

    const selected = this.shifts.filter((s) =>
      this.editEnrollment.shiftIds.includes(s.id!)
    );

    for (let i = 0; i < selected.length; i++) {
      for (let j = i + 1; j < selected.length; j++) {
        const shift1 = selected[i];
        const shift2 = selected[j];

        if (this.overlaps(shift1, shift2)) {
          return true;
        }
      }
    }
    return false;
  }

  private overlaps(shift1: Shift, shift2: Shift): boolean {
    const s1Start = new Date(shift1.startTime).getTime();
    const s1End = new Date(shift1.endTime).getTime();
    const s2Start = new Date(shift2.startTime).getTime();
    const s2End = new Date(shift2.endTime).getTime();

    // Edge case: All times are exactly the same (e.g., 10:00-10:00 and 10:00-10:00)
    if (s1Start === s1End && s1Start === s2Start && s2Start === s2End) {
      return true;
    }

    // Standard overlap logic: 
    // A shift overlaps another if it starts before the other ends 
    // AND ends after the other starts.
    return s1Start < s2End && s2Start < s1End;
  }

  async updateEnrollment() {
    //editar
    if (
      this.editEnrollment.id !== null &&
      (this.$refs.form as Vue & { validate: () => boolean }).validate()
    ) {
      try {
        const result = await RemoteServices.editEnrollment(
          this.editEnrollment.id,
          this.editEnrollment,
        );
        this.$emit('update-enrollment', result);
      } catch (error) {
        await this.$store.dispatch('error', error);
      }
    }
    //criar
    else if (
      this.editEnrollment.activityId !== null &&
      (this.$refs.form as Vue & { validate: () => boolean }).validate()
    ) {
      try {
        console.log('Payload being sent:', JSON.stringify(this.editEnrollment));
        const result = await RemoteServices.createEnrollment(
          this.editEnrollment,
        );
        this.$emit('save-enrollment', result);
      } catch (error) {
        await this.$store.dispatch('error', error);
      }
    }
  }
}
</script>

<style scoped lang="scss"></style>
