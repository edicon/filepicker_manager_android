package io.filepicker.manager.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.content.FileProvider;

import com.google.common.base.Optional;

import io.filepicker.manager.models.File;
import io.filepicker.manager.utils.Constants;
import io.filepicker.manager.utils.Utils;

/**
 * Created by maciejwitowski on 11/4/14.
 */
public class FileUtils {

    public static final String[] COLUMNS = {
            ManagerContract.File._ID,
            ManagerContract.File.COLUMN_URL,
            ManagerContract.File.COLUMN_MIMETYPE,
            ManagerContract.File.COLUMN_FILENAME,
            ManagerContract.File.COLUMN_KEY,
            ManagerContract.File.COLUMN_SIZE,
            ManagerContract.File.COLUMN_FOLDER_ID,
            ManagerContract.File.COLUMN_CREATED_AT
    };

    public static final int COLUMN_ID           = 0;
    public static final int COLUMN_URL          = 1;
    public static final int COLUMN_MIMETYPE     = 2;
    public static final int COLUMN_FILENAME     = 3;
    public static final int COLUMN_KEY          = 4;
    public static final int COLUMN_SIZE         = 5;
    public static final int COLUMN_FOLDER_ID    = 6;
    public static final int COLUMN_CREATED_AT    = 7;


    /* Returns FilepickerItem object by id */
    public static Optional<File> get(Context context, Uri uri) {
        File file = null;

        Cursor cursor = context.getContentResolver().query(
                uri,
                COLUMNS,
                null,
                null,
                null
        );

        if(cursor != null && cursor.moveToFirst()) {
            file =  File.getSavedInstance(
                    cursor.getLong(COLUMN_ID),
                    cursor.getString(COLUMN_URL),
                    cursor.getString(COLUMN_MIMETYPE),
                    cursor.getString(COLUMN_FILENAME),
                    cursor.getString(COLUMN_KEY),
                    cursor.getLong(COLUMN_SIZE),
                    cursor.getInt(COLUMN_FOLDER_ID),
                    cursor.getString(COLUMN_CREATED_AT)
            );
            cursor.close();
        }

        return Optional.fromNullable(file);
    }

    public static Optional<File> getById(Context context, long id) {
        Uri uri = ManagerContract.File.buildUri(id);

        return get(context, uri);
    }

    public static long insert(Context context, File file) {
        Uri resultUri = context.getContentResolver().insert(ManagerContract.File.CONTENT_URI,
                getContentValues(file));

        return ManagerContract.File.getIdFromUri(resultUri);
    }

    public static int update(Context context, File file) {
        return context.getContentResolver().update(ManagerContract.File.CONTENT_URI,
                                                    getContentValues(file),
                                                    ManagerContract.File._ID + " = ?",
                                                    new String[]{String.valueOf(file.id)});
    }

    public static ContentValues getContentValues(File fpfile) {
        ContentValues values = new ContentValues();
        values.put(ManagerContract.File.COLUMN_URL, fpfile.url);
        values.put(ManagerContract.File.COLUMN_MIMETYPE, fpfile.type);
        values.put(ManagerContract.File.COLUMN_FILENAME, fpfile.filename);
        values.put(ManagerContract.File.COLUMN_KEY, fpfile.key);
        values.put(ManagerContract.File.COLUMN_SIZE, fpfile.size);
        values.put(ManagerContract.File.COLUMN_FOLDER_ID, fpfile.folderId);

        return values;
    }

    public static boolean isSaved(Context context, File file) {
        return getContentUri(context, file).isPresent();
    }

    public static Optional<Uri> getContentUri(Context context, File file) {
        java.io.File javaFile = new java.io.File(Utils.getDownloadsPath(context), file.key);

        if (javaFile.exists()) {
            Uri fileUri = FileProvider.getUriForFile(context,
                    Constants.CONTENT_FILES_AUTHORITY, javaFile);

            return Optional.fromNullable(fileUri);
        } else {
            return Optional.absent();
        }
    }

    /** Returns local url if file is saved and remote url if not */
    public static String getFileDownloadUrl(Context context, File file) {
        Optional<Uri> contentUri = FileUtils.getContentUri(context, file);

        if(contentUri.isPresent()) {
            return contentUri.get().toString();
        } else {
            return file.url;
        }
    }
}
