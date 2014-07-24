package com.danny_oh.reddit.util;

import android.os.Parcel;
import android.os.Parcelable;

import com.github.jreddit.entity.Submission;

import org.json.simple.JSONObject;

/**
 * Created by danny on 7/23/14.
 */
public class ExtendedSubmission extends Submission implements Parcelable {

    public ExtendedSubmission(String name) {
        super(name);
    }

    public ExtendedSubmission(JSONObject object) {
        super(object);
    }

    public ExtendedSubmission(Submission submission) {
        super(submission.getFullName());

        setUrl(submission.getUrl());
        setPermalink(submission.getPermalink());
        setAuthor(submission.getAuthor());
        setTitle(submission.getTitle());
        setSubreddit(submission.getSubreddit());
        setSubredditId(submission.getSubredditId());
        setThumbnail(submission.getThumbnail());

        setSelftext(submission.getSelftext());
        setSelftextHTML(submission.getSelftextHTML());
        setDomain(submission.getDomain());
        setBannedBy(submission.getBannedBy());
        setApprovedBy(submission.getApprovedBy());

        setGilded(submission.getGilded());
        setCommentCount(submission.getCommentCount());
        setReportCount(submission.getReportCount());
        setScore(submission.getScore());
        setUpVotes(submission.getUpVotes());
        setDownVotes(submission.getDownVotes());

        setCreated(submission.getCreated());
        setCreatedUTC(submission.getCreatedUTC());
        setVisited(submission.isVisited());
        setSelf(submission.isSelf());
        setSaved(submission.isSaved());
        setEdited(submission.isEdited());
        setStickied(submission.isStickied());
        setNSFW(submission.isNSFW());
        setHidden(submission.isHidden());
        setClicked(submission.isClicked());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(getFullName());
        parcel.writeString(getURL());
        parcel.writeString(getPermalink());
        parcel.writeString(getAuthor());
        parcel.writeString(getTitle());
        parcel.writeString(getSubreddit());
        parcel.writeString(getSubredditId());
        parcel.writeString(getThumbnail());

        parcel.writeString(getSelftext());
        parcel.writeString(getSelftextHTML());
        parcel.writeString(getDomain());
        parcel.writeString(getBannedBy());
        parcel.writeString(getApprovedBy());

        parcel.writeLong(getGilded());
        parcel.writeLong(getCommentCount());
        parcel.writeLong(getReportCount());
        parcel.writeLong(getScore());
        parcel.writeLong(getUpVotes());
        parcel.writeLong(getDownVotes());

        parcel.writeLong(getCreated());
        parcel.writeLong(getCreatedUTC());
        parcel.writeBooleanArray(new boolean[]{isVisited(), isSelf(), isSaved(), isEdited(), isStickied(), isNSFW(), isHidden(), isClicked()});

    }

    public static final Parcelable.Creator<ExtendedSubmission> CREATOR
            = new Parcelable.Creator<ExtendedSubmission>() {
        public ExtendedSubmission createFromParcel(Parcel parcel) {

            ExtendedSubmission submission = new ExtendedSubmission(parcel.readString());
            submission.setURL(parcel.readString());
            submission.setPermalink(parcel.readString());
            submission.setAuthor(parcel.readString());
            submission.setTitle(parcel.readString());
            submission.setSubreddit(parcel.readString());
            submission.setSubredditId(parcel.readString());
            submission.setThumbnail(parcel.readString());

            submission.setSelftext(parcel.readString());
            submission.setSelftextHTML(parcel.readString());
            submission.setDomain(parcel.readString());
            submission.setBannedBy(parcel.readString());
            submission.setApprovedBy(parcel.readString());

            submission.setGilded(parcel.readLong());
            submission.setCommentCount(parcel.readLong());
            submission.setReportCount(parcel.readLong());
            submission.setScore(parcel.readLong());
            submission.setUpVotes(parcel.readLong());
            submission.setDownVotes(parcel.readLong());

            submission.setCreated(parcel.readLong());
            submission.setCreatedUTC(parcel.readLong());

            boolean[] boolArray = null;
            parcel.readBooleanArray(boolArray);

            if (boolArray != null) {
                submission.setVisited(boolArray[0]);
                submission.setSelf(boolArray[1]);
                submission.setSaved(boolArray[2]);
                submission.setEdited(boolArray[3]);
                submission.setStickied(boolArray[4]);
                submission.setNSFW(boolArray[5]);
                submission.setHidden(boolArray[6]);
                submission.setClicked(boolArray[7]);
            }

            return submission;
        }

        public ExtendedSubmission[] newArray(int size) {
            return new ExtendedSubmission[size];
        }
    };
}
