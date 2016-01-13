package com.antwish.povi.familyconnect;

import android.os.Parcel;
import android.os.Parcelable;

import com.antwish.povi.server.ParentingTip;
import com.antwish.povi.server.SampleAnswerArray;

/**
 * Created by fabio on 04/09/2015.
 */
public class Beat implements Parcelable {
    public int resourceId;
    public int tipId;
    public int commentCount;
    public int likeCount;
    public String category;
    public String tipText;

    public Beat(ParentingTip tip){
        resourceId = tip.getTipId().getTipResourceId();
        tipId = tip.getTipId().getTipSequenceId();
        commentCount = tip.getCommentCount();
        likeCount = tip.getLikeCount();
        category = tip.getTipCategory().toString();
        tipText = tip.getTipDetail();
    }


    public Beat(Parcel in){
        resourceId = in.readInt();
        tipId = in.readInt();
        commentCount = in.readInt();
        likeCount = in.readInt();
        category = in.readString();
        tipText = in.readString();
    }


    public static final Creator<Beat> CREATOR = new Creator<Beat>() {
        @Override
        public Beat createFromParcel(Parcel in) {
            return new Beat(in);
        }

        @Override
        public Beat[] newArray(int size) {
            return new Beat[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(resourceId);
        dest.writeInt(tipId);
        dest.writeInt(commentCount);
        dest.writeInt(likeCount);
        dest.writeString(category);
        dest.writeString(tipText);
    }
}
