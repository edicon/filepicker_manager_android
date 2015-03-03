package io.filepicker.manager.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import io.filepicker.manager.utils.Constants;

/**
 * Created by maciejwitowski on 11/4/14.
 */
public class ManagerProvider extends ContentProvider {

    private static final UriMatcher sUriMatcher = buildUriMatcher();
    private ManagerDbHelper mManagerHelper;

    private static final int FOLDERS = 100;
    private static final int FOLDER = 101;
    private static final int FOLDER_FILES = 102;
    private static final int FILES = 103;
    private static final int FILE = 104;

    private static UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = Constants.CONTENT_DATA_AUTHORITY;

        // folders/
        matcher.addURI(authority, ManagerContract.PATH_FOLDERS, FOLDERS);

        // folders/:id
        matcher.addURI(authority, ManagerContract.PATH_FOLDERS + "/#", FOLDER);

        // folders/:id/files
        matcher.addURI(authority, ManagerContract.PATH_FOLDERS + "/#/" +
                ManagerContract.PATH_FILES, FOLDER_FILES);

        // files
        matcher.addURI(authority, ManagerContract.PATH_FILES, FILES);

        // files/:id
        matcher.addURI(authority, ManagerContract.PATH_FILES + "/#", FILE);

        return matcher;
    }

    @Override
    public boolean onCreate() {
        mManagerHelper = new ManagerDbHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Cursor cursor;
        switch(sUriMatcher.match(uri)){
            case FOLDERS:
                cursor = mManagerHelper.getReadableDatabase().query(
                        ManagerContract.Folder.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;

            case FOLDER:
                cursor = mManagerHelper.getReadableDatabase().query(
                        ManagerContract.Folder.TABLE_NAME,
                        projection,
                        ManagerContract.Folder._ID + " = ?",
                        new String[] {String.valueOf(ContentUris.parseId(uri))},
                        null,
                        null,
                        sortOrder
                );
                break;

            case FILES:
                cursor = mManagerHelper.getReadableDatabase().query(
                        ManagerContract.File.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;

            case FILE:
                cursor = mManagerHelper.getReadableDatabase().query(
                        ManagerContract.File.TABLE_NAME,
                        projection,
                        ManagerContract.File._ID + " = ?",
                        new String[] {String.valueOf(ContentUris.parseId(uri))},
                        null,
                        null,
                        sortOrder
                );
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);

        switch(match) {
            case FOLDERS:
                return ManagerContract.Folder.CONTENT_TYPE;

            case FOLDER:
                return ManagerContract.Folder.CONTENT_ITEM_TYPE;

            case FOLDER_FILES:
                return ManagerContract.File.CONTENT_TYPE;

            case FILES:
                return ManagerContract.File.CONTENT_TYPE;

            case FILE:
                return ManagerContract.File.CONTENT_ITEM_TYPE;
        }

        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final SQLiteDatabase db = mManagerHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        Uri returnUri;

        switch(match) {
            // categories
            case FILES: {
                long _id = db.insert(ManagerContract.File.TABLE_NAME, null, values);

                returnUri = ManagerContract.File.buildUri(_id);

                break;
            }

            case FOLDERS: {
                long _id = db.insert(ManagerContract.Folder.TABLE_NAME, null, values);

                returnUri = ManagerContract.Folder.buildUri(_id);

                break;
            }

            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return returnUri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mManagerHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);

        int rowsUpdated;

        switch(match) {
            case FILES:
                rowsUpdated = db.update(ManagerContract.File.TABLE_NAME, values, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsUpdated;
    }
}
