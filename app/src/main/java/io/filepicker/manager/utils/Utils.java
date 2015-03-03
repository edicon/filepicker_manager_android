package io.filepicker.manager.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.common.base.Optional;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import io.filepicker.Filepicker;
import io.filepicker.manager.R;
import io.filepicker.manager.models.Folder;

/**
 * Created by maciejwitowski on 11/5/14.
 */
public final class Utils {

    private static final DateFormat formatLong = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final DateFormat formatShort = new SimpleDateFormat("MMM d, h:mm a");

    private static File downloadsDir;

    private static File getDownloadsDir(Context context) {
        if(downloadsDir == null)
            downloadsDir =  new java.io.File(context.getFilesDir(), "downloads");

        return downloadsDir;
    }


    // Enforcing noninstantiability
    private Utils(){
        // Throw the exception if the constructor is invoked from within the class
        throw new AssertionError();
    }

    // Show Toast
    public static void showQuickToast(Context context, int resId ){
        if(context != null)
            Toast.makeText(context, resId, Toast.LENGTH_SHORT).show();
    }

    public static void showQuickToast(Context context, String message) {
        if(context != null)
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    public static void showLongToast(Context context, String message ){
        if(context != null)
            Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }


    // Returns downloads path (create if doesn't exists)
    public static File getDownloadsPath(Context context) {
        File downloadsDir = getDownloadsDir(context);

        if(!downloadsDir.exists() || !downloadsDir.isDirectory()) {
            downloadsDir.mkdir();
        }

        return downloadsDir;
    }

    public static void getFileFromLibrary(Context context) {
        Filepicker.setKey(Constants.API_KEY);
        Intent intent = new Intent(context, Filepicker.class);

        ((Activity)context).startActivityForResult(intent, Filepicker.REQUEST_CODE_GETFILE);
    }

    public static long getFolderIdOrRoot(Folder folder) {
        if(folder != null) {
            return folder.id;
        } else {
            return -1;
        }
    }

    public static String getReadableTimestamp(String timestamp) {
        try {
            Date date = formatLong.parse(timestamp);
            return formatShort.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return timestamp;
    }

    public static boolean isRootFolder(Folder folder){
        return !Optional.fromNullable(folder).isPresent();
    }

    public static void disableButton(Context context, Button button) {
        button.setEnabled(false);
        button.setTextColor(context.getResources().getColor(R.color.cool_gray));
    }

    public static void enableButton(Context context, Button button) {
        button.setEnabled(true);
        button.setTextColor(context.getResources().getColor(R.color.fp_blue));
    }

    public static String getShortName(String name){
        return (name.length() > 20) ? name.substring(0, 20) + "..." : name;
    }

    public static boolean isImage(String type) {
        return type.equals(Constants.IMAGE_JPEG) ||
               type.equals(Constants.IMAGE_JPG)  ||
               type.equals(Constants.IMAGE_PNG);
    }

    public static String getFileExtension(String filename) {
        int i = filename.lastIndexOf(".");
        if(i > 0) {
            return filename.substring(i+1);
        }

        return "unknown";
    }

    /** Returns list of activities which can handle specific intent with type */
    public static List<ResolveInfo> getIntentResolvers(Context context, String action, String type) {
        Intent basicShareIntent = new Intent()
                .setAction(action)
                .setType(type);

        return context.getPackageManager()
                .queryIntentActivities(basicShareIntent, 0);
    }

    public static boolean isBeforeLollipop() {
        return Build.VERSION.SDK_INT < 21;
    }

    public static void updateProgressBar(ProgressBar progressBar, boolean isLoading) {
        if(progressBar != null) {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
    }

}
