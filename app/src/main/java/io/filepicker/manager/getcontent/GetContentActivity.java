package io.filepicker.manager.getcontent;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.google.common.base.Optional;

import java.util.ArrayList;

import de.greenrobot.event.EventBus;
import io.filepicker.Filepicker;
import io.filepicker.manager.data.FileUtils;
import io.filepicker.manager.data.FolderUtils;
import io.filepicker.manager.data.ManagerContract;
import io.filepicker.manager.enums.Operation;
import io.filepicker.manager.events.FileSavedEvent;
import io.filepicker.manager.models.File;
import io.filepicker.manager.services.ManagerService;
import io.filepicker.manager.utils.Utils;
import io.filepicker.models.FPFile;

public class GetContentActivity extends FragmentActivity implements GetContentFragment.Contract {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getSupportFragmentManager().findFragmentById(android.R.id.content) == null) {
            getSupportFragmentManager().beginTransaction().add(android.R.id.content,
                    GetContentFragment.newInstance())
                    .commit();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        EventBus.getDefault().register(this);
    }

    @Override
    protected void onPause() {
        EventBus.getDefault().unregister(this);

        super.onPause();
    }

    @Override
    public void returnContent(Uri fileUri) {
        final Optional<File> optFile = FileUtils.get(this, fileUri);
        Utils.showQuickToast(this, "Saving file");

        if(optFile.isPresent()) {
            File file = optFile.get();
            // If saved return path to local file
            if (FileUtils.isSaved(this, file)) {
                returnFilePath(file);
            }
            // Firstly save the file and then return (onEvent)
            else {
                ManagerService.saveFile(this, fileUri, Operation.SAVE);
            }
        }
    }

    @Override
    public void useFilepickerLibrary() {
        Utils.getFileFromLibrary(this);
    }

    public void onEvent(FileSavedEvent event) {
        File file = event.file;
        returnFilePath(file);
    }

    // Return content:// path to the file
    private void returnFilePath(File file) {
        Utils.showQuickToast(this, "File saved, returning...");

        Optional<Uri> contentUri = FileUtils.getContentUri(this, file);

        if(contentUri.isPresent()) {
            Intent result = new Intent(Intent.ACTION_SEND);
            result.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            result.setData(contentUri.get());

            setResult(RESULT_OK, result);
        }

        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == Filepicker.REQUEST_CODE_GETFILE) {
            if(resultCode != RESULT_OK)
                return;

            ArrayList<FPFile> fpFiles = data.getParcelableArrayListExtra(Filepicker.FPFILES_EXTRA);
            FPFile fpFile = fpFiles.get(0);

            File newItem = new File(fpFile.getUrl(),
                                    fpFile.getType(),
                                    fpFile.getFilename(),
                                    fpFile.getKey(),
                                    fpFile.getSize(),
                                    FolderUtils.ROOT_ID);

            long fileId = FileUtils.insert(this, newItem);

            returnContent(ManagerContract.File.buildUri(fileId));
        }
    }




}