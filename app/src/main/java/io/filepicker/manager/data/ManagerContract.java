package io.filepicker.manager.data;

import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;

import io.filepicker.manager.utils.Constants;

/**
 * Created by maciejwitowski on 11/4/14.
 */
public class ManagerContract {

    private static final Uri BASE_CONTENT_URI = Uri.parse("content://" + Constants.CONTENT_DATA_AUTHORITY);

    public static final String PATH_FILES = "files";
    public static final String PATH_FOLDERS = "folders";

    public static final class File implements BaseColumns {

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_FILES).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/" + Constants.CONTENT_DATA_AUTHORITY + "/" + PATH_FILES;
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/" + Constants.CONTENT_DATA_AUTHORITY + "/" + PATH_FILES;

        // Table name
        public static final String TABLE_NAME = "files";

        public static final String COLUMN_ID_TYPE = "INTEGER PRIMARY KEY";

        public static final String COLUMN_URL = "url";
        public static final String COLUMN_URL_TYPE = "TEXT";

        public static final String COLUMN_MIMETYPE = "mimetype";
        public static final String COLUMN_MIMETYPE_TYPE = "TEXT";

        public static final String COLUMN_FILENAME = "filename";
        public static final String COLUMN_FILENAME_TYPE = "TEXT";

        public static final String COLUMN_KEY = "key";
        public static final String COLUMN_KEY_TYPE = "TEXT";

        public static final String COLUMN_SIZE = "size";
        public static final String COLUMN_SIZE_TYPE = "INTEGER";

        public static final String COLUMN_FOLDER_ID = "folder_id";
        public static final String COLUMN_FOLDER_ID_TYPE = "INTEGER";

        public static final String COLUMN_CREATED_AT = "created_at";
        public static final String COLUMN_CREATED_AT_TYPE = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP";

        public static Uri buildUri( long id ) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }

        public static long getIdFromUri(Uri uri) {
            return Long.parseLong(uri.getPathSegments().get(1));
        }
    }

    public static final class Folder implements BaseColumns {

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_FOLDERS).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/" + Constants.CONTENT_DATA_AUTHORITY + "/" + PATH_FOLDERS;
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/" + Constants.CONTENT_DATA_AUTHORITY + "/" + PATH_FOLDERS;

        // Table name
        public static final String TABLE_NAME = "folders";

        public static final String COLUMN_ID_TYPE = "INTEGER PRIMARY KEY";

        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_NAME_TYPE = "TEXT";

        public static final String COLUMN_PARENT_ID = "parent_id";
        public static final String COLUMN_PARENT_ID_TYPE = "INTEGER";

        public static final String COLUMN_CREATED_AT = "created_at";
        public static final String COLUMN_CREATED_AT_TYPE = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP";

        public static Uri buildUri( long id ) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }

        public static long getFolderId(Uri uri) {
            return Long.parseLong(uri.getPathSegments().get(1));
        }
    }
}
