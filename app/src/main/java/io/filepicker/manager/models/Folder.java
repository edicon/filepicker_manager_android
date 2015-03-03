package io.filepicker.manager.models;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by maciejwitowski on 11/12/14.
 */

/** Final class cannot be extended */
public final class Folder implements Parcelable {

    public long id;
    public final String name;
    public final long parentId;
    public String createdAt;

    public Folder(String name, long parentId) {
        this.name = name;
        this.parentId = parentId;
    }

    /** Saved file */
    public static Folder getSavedInstance(long id, String name, long parentId, String createdAt) {
        Folder folder = new Folder(name, parentId);
        folder.id = id;
        folder.createdAt = createdAt;

        return folder;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.id);
        dest.writeString(this.name);
        dest.writeLong(this.parentId);
        dest.writeString(this.createdAt);
    }

    private Folder(Parcel in) {
        this.id = in.readLong();
        this.name = in.readString();
        this.parentId = in.readLong();
        this.createdAt = in.readString();
    }

    public static final Parcelable.Creator<Folder> CREATOR = new Parcelable.Creator<Folder>() {
        public Folder createFromParcel(Parcel source) {
            return new Folder(source);
        }

        public Folder[] newArray(int size) {
            return new Folder[size];
        }
    };
}
