package io.filepicker.manager.models;

import android.os.Parcelable;

import io.filepicker.manager.utils.Constants;
import io.filepicker.manager.utils.Utils;

/**
 * Created by maciejwitowski on 11/3/14.
 */

public final class File implements Parcelable {

    public long id;
    public final String url;
    public final String type;
    public final String filename;
    public final String key;
    public final long size;
    public long folderId;
    public String createdAt;

    /* Required for Parceler */
    public File() {
        key = null;
        filename = null;
        size = 0;
        type = null;
        url = null;
        folderId = -1;
    }

    /* For building file from library */
    public File(String url, String type, String filename, String key, long size, long folderId) {
        this.url        = url;
        this.type   = type;
        this.filename   = filename;
        this.key        = key;
        this.size       = size;
        this.folderId   = folderId;
    }

    /** For saved file - with id and created at */
    public static File getSavedInstance(long id, String url, String type, String filename, String key, long size, long folderId, String createdAt) {
        File file = new File(url, type, filename, key, size, folderId);
        file.id = id;
        file.createdAt = createdAt;

        return file;
    }

    public String getUrlId() {
        return url.substring(url.lastIndexOf('/') + 1);
    }

    public String getReadableType() {
        if (Utils.isImage(type))
            return "Image";
        else if (isDocument()) {
            return "PDF";
        } else {
            return "Unknown";
        }
    }

    public boolean isDocument() {
        return type.equals(Constants.DOCUMENT_PDF);
    }

    public String getReadableSize() {
        int unit = 1024;
        if (size < unit) return size + " B";
        int exp = (int) (Math.log(size) / Math.log(unit));
        String pre = ("kMGTPE").charAt(exp-1) + "";

        return String.format("%.1f %sB", size / Math.pow(unit, exp), pre);
    }

    public void setFolderId(long folderId) {
        this.folderId = folderId;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(android.os.Parcel dest, int flags) {
        dest.writeString(this.url);
        dest.writeString(this.type);
        dest.writeString(this.filename);
        dest.writeString(this.key);
        dest.writeLong(this.size);
        dest.writeLong(this.folderId);
        dest.writeString(this.createdAt);
    }

    private File(android.os.Parcel in) {
        this.url = in.readString();
        this.type = in.readString();
        this.filename = in.readString();
        this.key = in.readString();
        this.size = in.readLong();
        this.folderId = in.readLong();
        this.createdAt = in.readString();
    }

    public static final Creator<File> CREATOR = new Creator<File>() {
        public File createFromParcel(android.os.Parcel source) {
            return new File(source);
        }

        public File[] newArray(int size) {
            return new File[size];
        }
    };
}
