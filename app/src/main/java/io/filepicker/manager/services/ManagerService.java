package io.filepicker.manager.services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.google.common.base.Optional;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import de.greenrobot.event.EventBus;
import io.filepicker.manager.R;
import io.filepicker.manager.api.ApiClient;
import io.filepicker.manager.data.FileUtils;
import io.filepicker.manager.enums.Operation;
import io.filepicker.manager.events.FileSavedEvent;
import io.filepicker.manager.models.File;
import io.filepicker.manager.utils.Constants;
import io.filepicker.manager.utils.Utils;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.mime.TypedByteArray;


public class ManagerService extends IntentService {

    private static final String LOG_TAG = ManagerService.class.getSimpleName();

    private static final String ACTION_SHARE = "io.filepicker.manager.services.action.share";

    private static final String EXTRA_FILE_URI = "io.filepicker.services.extra.fileUri";

    private static final String ACTION_SAVE_FILE = "io.filepicker.manager.services.action.saveFile";

    /** File can be saved for different reasons - normal save, to be exported, to be shared */
    public static final String EXTRA_OPERATION_TYPE = "io.filepicker.services.extra.operationType";

    public ManagerService() {
        super("ShareService");
    }

    public static void shareContent(Context context, String url) {
        Intent intent = new Intent(context, ManagerService.class);
        intent.setAction(ACTION_SHARE);
        intent.putExtra(EXTRA_FILE_URI, url);
        context.startService(intent);
    }

    public static void saveFile(Context context, Uri uri, Operation operation) {
        Intent intent = new Intent(context, ManagerService.class);
        intent.setAction(ACTION_SAVE_FILE);
        intent.putExtra(EXTRA_OPERATION_TYPE, operation.name());
        intent.setData(uri);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_SHARE.equals(action)) {
                String url = intent.getStringExtra(EXTRA_FILE_URI);
                handleActionShare(url);
            } else if (ACTION_SAVE_FILE.equals(action)) {
                String operationType = intent.getStringExtra(EXTRA_OPERATION_TYPE);
                handleActionSaveFile(intent.getData(), operationType);
            }
        }
    }

    private void handleActionShare(String url) {
        Log.d(LOG_TAG, "handleActionShare");
        ApiClient.getApiClient()
            .storeFile(Constants.API_KEY, url,
                new Callback<File>() {
                    @Override
                    public void success(File fpfile, retrofit.client.Response response) {
                        FileUtils.insert(ManagerService.this, fpfile);
                    }

                    @Override
                    public void failure(RetrofitError error) {

                    }
                });
    }

    private void handleActionSaveFile(Uri uri, final String operationType) {
        final Optional<File> optFile = FileUtils.get(this, uri);

        if(optFile.isPresent()) {
            final File file = optFile.get();

            ApiClient.getApiClient()
                .getFilelinkContent(file.getUrlId(),
                    new Callback<Response>() {
                        @Override
                        public void success(Response responseObject, Response response) {
                            saveResponseObject(file, responseObject, operationType);
                        }

                        @Override
                        public void failure(RetrofitError error) {
                            // No internet error
                            if(error.getResponse() == null) {
                               Utils.showQuickToast(ManagerService.this, R.string.save_no_internet);
                            }
                        }
                    });
        }
    }

    private void saveResponseObject(File file, Response response, String operationType) {
        // Get the content of file returned from request
        byte[] responseBody = ((TypedByteArray) response.getBody()).getBytes();

        java.io.File javaFile = new java.io.File(Utils.getDownloadsPath(ManagerService.this), file.key);

        try {
            FileOutputStream output = new FileOutputStream(javaFile);
            output.write(responseBody);
            output.close();

            EventBus.getDefault().post(new FileSavedEvent(file, operationType));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
