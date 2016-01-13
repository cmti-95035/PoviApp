package com.antwish.povi.familyconnect;

import android.os.Parcel;
import android.os.Parcelable;

import com.antwish.povi.server.ParentingTip;
import com.antwish.povi.server.ParentingTipId;
import com.antwish.povi.server.SampleAnswerArray;

/**
 * Created by fabio on 04/09/2015.
 */
public class BeatId implements Parcelable {
    public int resourceId;
    public int sequenceId;

    public BeatId(ParentingTipId id){
        resourceId = id.getTipResourceId();
        sequenceId = id.getTipSequenceId();
    }


    public BeatId(Parcel in){
        resourceId = in.readInt();
        sequenceId = in.readInt();
    }


    public static final Creator<BeatId> CREATOR = new Creator<BeatId>() {
        @Override
        public BeatId createFromParcel(Parcel in) {
            return new BeatId(in);
        }

        @Override
        public BeatId[] newArray(int size) {
            return new BeatId[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(resourceId);
        dest.writeInt(sequenceId);
    }
}
