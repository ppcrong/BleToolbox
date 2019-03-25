package com.ppcrong.bletoolbox.cgms;

import android.os.Parcel;
import android.os.Parcelable;

public class CgmsRecord implements Parcelable {
    /**
     * Record sequence number.
     */
    protected int sequenceNumber;
    /**
     * The base time of the measurement (start time + sequenceNumber of minutes).
     */
    protected long timestamp;
    /**
     * The glucose concentration in mg/dL.
     */
    protected float glucoseConcentration;

    protected CgmsRecord(int sequenceNumber, float glucoseConcentration, long timestamp) {
        this.sequenceNumber = sequenceNumber;
        this.glucoseConcentration = glucoseConcentration;
        this.timestamp = timestamp;
    }

    protected CgmsRecord(Parcel in) {
        this.sequenceNumber = in.readInt();
        this.glucoseConcentration = in.readFloat();
        this.timestamp = in.readLong();
    }

    public static final Creator<CgmsRecord> CREATOR = new Creator<CgmsRecord>() {
        @Override
        public CgmsRecord createFromParcel(Parcel in) {
            return new CgmsRecord(in);
        }

        @Override
        public CgmsRecord[] newArray(int size) {
            return new CgmsRecord[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(sequenceNumber);
        parcel.writeFloat(glucoseConcentration);
        parcel.writeLong(timestamp);
    }
}
