package com.antwish.povi.familyconnect;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Parcel;
import android.os.Parcelable;

import com.antwish.povi.server.Comment;
import com.linkedin.data.ByteString;
import com.linkedin.data.template.GetMode;

/**
 * Created by fabio on 04/09/2015.
 */
public class Content implements Parcelable {
    public int commentCount;
    public int likeCount;
    public int commentGroup;
    public long timestamp;
    public long commentId;
    public String commentText;
    public String description;
    public String userId;
    public String displayedName;
    public String mediaFileName;
    public byte[] thumbnail;

    public Content(Comment comment){
        commentCount = comment.getCommentCount();
        likeCount = comment.getLikeCount();
        commentGroup = comment.getContentGroups();
        timestamp = comment.getTimestamp();
        commentId = comment.getCommentId();
        commentText = comment.getCommentText();
        description = comment.getTextDescription(GetMode.NULL);
        userId = comment.getUserId();
        displayedName = comment.getAuthorNickName(GetMode.NULL);
        mediaFileName = comment.getRemoteFileName();
        if (comment.getThumbnailImage(GetMode.NULL) != null) {
            thumbnail = comment.getThumbnailImage().copyBytes();
        }
    }

    public Content(Parcel in){
        commentCount = in.readInt();
        likeCount = in.readInt();
        commentGroup = in.readInt();
        timestamp = in.readLong();
        commentId = in.readLong();
        commentText = in.readString();
        description = in.readString();
        userId = in.readString();
        displayedName = in.readString();
        mediaFileName = in.readString();
        int len = in.readInt();
        if (len > 0) {
            thumbnail = new byte[len];
            in.readByteArray(thumbnail);
        }
    }


    public static final Creator<Content> CREATOR = new Creator<Content>() {
        @Override
        public Content createFromParcel(Parcel in) {
            return new Content(in);
        }

        @Override
        public Content[] newArray(int size) {
            return new Content[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(commentCount);
        dest.writeInt(likeCount);
        dest.writeInt(commentGroup);
        dest.writeLong(timestamp);
        dest.writeLong(commentId);
        dest.writeString(commentText);
        dest.writeString(description);
        dest.writeString(userId);
        dest.writeString(displayedName);
        dest.writeString(mediaFileName);
        if (thumbnail != null) {
            dest.writeInt(thumbnail.length);
            dest.writeByteArray(thumbnail);
        }
        else
            dest.writeInt(0);
    }
}
