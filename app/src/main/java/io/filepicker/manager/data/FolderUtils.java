package io.filepicker.manager.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.google.common.base.Optional;

import io.filepicker.manager.models.Folder;

/**
 * Created by maciejwitowski on 11/12/14.
 */
public class FolderUtils {

    public static final int ROOT_ID = -1;

    public static final String[] COLUMNS = {
            ManagerContract.Folder._ID,
            ManagerContract.Folder.COLUMN_NAME,
            ManagerContract.Folder.COLUMN_PARENT_ID,
            ManagerContract.Folder.COLUMN_CREATED_AT
    };

    public static final int COLUMN_ID   = 0;
    public static final int COLUMN_NAME = 1;
    public static final int COLUMN_PARENT_ID = 2;
    public static final int COLUMN_CREATED_AT = 3;

    /* Returns FilepickerItem object by id */
    public static Optional<Folder> get(Context context, Uri uri) {
        Folder folder = null;

        Cursor cursor = context.getContentResolver().query(uri, COLUMNS, null, null, null);

        if(cursor != null && cursor.moveToFirst()) {
            folder = Folder.getSavedInstance(cursor.getLong(COLUMN_ID), cursor.getString(COLUMN_NAME),
                    cursor.getLong(COLUMN_PARENT_ID), cursor.getString(COLUMN_CREATED_AT));

            cursor.close();
        }

        return Optional.fromNullable(folder);
    }

    public static Optional<Folder> getById(Context context, long id) {
        Uri uri = ManagerContract.Folder.buildUri(id);

        return get(context, uri);
    }

    public static long insert(Context context, Folder folder) {
        Uri resultUri = context.getContentResolver().insert(ManagerContract.Folder.CONTENT_URI,
                getContentValues(folder));

        return ManagerContract.Folder.getFolderId(resultUri);
    }

    public static ContentValues getContentValues(Folder folder) {
        ContentValues values = new ContentValues();
        values.put(ManagerContract.Folder.COLUMN_NAME, folder.name);
        values.put(ManagerContract.Folder.COLUMN_PARENT_ID, folder.parentId);
        return values;
    }

    public static boolean isRootFolder(long folderId) {
        return folderId == ROOT_ID;
    }
}
