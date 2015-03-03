package io.filepicker.manager.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by maciejwitowski on 11/4/14.
 */
public class ManagerDbHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "manager.db";

    private static final String COMMA_SEP = ", ";

    // SQLiteOpenHelper provides a life cycle framework for creating/upgrading/downgrading database
    public ManagerDbHelper(Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        final String SQL_CREATE_FILES_TABLE =
                "CREATE TABLE " + ManagerContract.File.TABLE_NAME + " (" +
                        ManagerContract.File._ID + " " + ManagerContract.File.COLUMN_ID_TYPE + COMMA_SEP +
                        ManagerContract.File.COLUMN_URL + " " + ManagerContract.File.COLUMN_URL_TYPE + COMMA_SEP +
                        ManagerContract.File.COLUMN_FILENAME + " " + ManagerContract.File.COLUMN_FILENAME_TYPE + COMMA_SEP +
                        ManagerContract.File.COLUMN_MIMETYPE + " " + ManagerContract.File.COLUMN_MIMETYPE_TYPE + COMMA_SEP +
                        ManagerContract.File.COLUMN_KEY + " " + ManagerContract.File.COLUMN_KEY_TYPE + COMMA_SEP +
                        ManagerContract.File.COLUMN_SIZE + " " + ManagerContract.File.COLUMN_SIZE_TYPE + COMMA_SEP +
                        ManagerContract.File.COLUMN_FOLDER_ID + " " + ManagerContract.File.COLUMN_FOLDER_ID_TYPE + COMMA_SEP +
                        ManagerContract.File.COLUMN_CREATED_AT + " " + ManagerContract.File.COLUMN_CREATED_AT_TYPE +
                        ");";

        final String SQL_CREATE_FOLDER_TABLE =
                "CREATE TABLE " + ManagerContract.Folder.TABLE_NAME + " (" +
                        ManagerContract.Folder._ID + " " + ManagerContract.Folder.COLUMN_ID_TYPE + COMMA_SEP +
                        ManagerContract.Folder.COLUMN_NAME + " " + ManagerContract.Folder.COLUMN_NAME_TYPE + COMMA_SEP +
                        ManagerContract.Folder.COLUMN_PARENT_ID + " " + ManagerContract.Folder.COLUMN_PARENT_ID_TYPE + COMMA_SEP +
                        ManagerContract.Folder.COLUMN_CREATED_AT + " " + ManagerContract.Folder.COLUMN_CREATED_AT_TYPE +
                        ");";

        database.execSQL(SQL_CREATE_FILES_TABLE);
        database.execSQL(SQL_CREATE_FOLDER_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        database.execSQL("DROP TABLE IF EXISTS " + ManagerContract.File.TABLE_NAME);
        database.execSQL("DROP TABLE IF EXISTS " + ManagerContract.Folder.TABLE_NAME);
    }
}
