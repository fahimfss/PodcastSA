package com.example.fahim.podcastsa;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by fahim on 03-Jul-17.
 */

public class PodcastItem implements Parcelable {
    String title, link, secondaryTitle, lenght;
    int status, position, playing;

    public PodcastItem(String title, String link, String secondaryTitle, String lenght, int status, int no, int playing) {
        this.title = title;
        this.link = link;
        this.secondaryTitle = secondaryTitle;
        this.lenght = lenght;
        this.status = status;
        this.position = no;
        this.playing = playing;
    }

    public PodcastItem() {
        this.title = "";
        this.link = null;
        this.secondaryTitle = "";
        this.lenght = "";
        this.status = -1;
        this.position = -1;
        this.playing = -1;
    }

    public String getLenght() {
        return lenght;
    }

    public void setLenght(String lenght) {
        this.lenght = lenght;
    }

    public int getPlaying() {
        return playing;
    }

    public void setPlaying(int playing) {
        this.playing = playing;
    }

    public String getSecondaryTitle() {
        return secondaryTitle;
    }

    public void setSecondaryTitle(String secondaryTitle) {
        this.secondaryTitle = secondaryTitle;
    }

    public String getTitle() {
        return title;
    }

    public String getTitleWithPositon() {
        return String.valueOf(position)+ ". " + title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public void setStatus(boolean status) {
        if(status)this.status = 1;
        else this.status = 0;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(title);
        dest.writeString(link);
        dest.writeString(secondaryTitle);
        dest.writeString(lenght);
        dest.writeInt(status);
        dest.writeInt(position);
        dest.writeInt(playing);
    }

    private PodcastItem(Parcel in){
        title = in.readString();
        link = in.readString();
        secondaryTitle = in.readString();
        lenght = in.readString();
        status = in.readInt();
        position = in.readInt();
        playing = in.readInt();
    }

    public static final Parcelable.Creator<PodcastItem> CREATOR = new Parcelable.Creator<PodcastItem>() {
        public PodcastItem createFromParcel(Parcel in) {
            return new PodcastItem(in);
        }

        public PodcastItem[] newArray(int size) {
            return new PodcastItem[size];
        }
    };
}
