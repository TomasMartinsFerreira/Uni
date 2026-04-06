import { ISOtoString } from '@/services/ConvertDateService';

export default class Shift {
    id: number | null = null;
    location!: string;
    startTime!: string;
    formattedStartTime!: string;
    endTime!: string;
    formattedEndTime!: string;
    participantsLimit!: number;
    activityId!: number;

    constructor(jsonObj?: Shift) {
        if (jsonObj) {
            this.id = jsonObj.id;
            this.location = jsonObj.location;
            this.startTime = jsonObj.startTime;
            if (jsonObj.startTime)
                this.formattedStartTime = ISOtoString(jsonObj.startTime)
            this.endTime = jsonObj.endTime;
            if (jsonObj.endTime)
                this.formattedEndTime = ISOtoString(jsonObj.endTime)
            this.participantsLimit = jsonObj.participantsLimit;
            this.activityId = jsonObj.activityId;
        }
    }
}