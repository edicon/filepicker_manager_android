package io.filepicker.manager;

import android.app.Activity;
import android.content.ClipData;
import android.os.Build;
import android.os.Bundle;

import de.greenrobot.event.EventBus;
import io.filepicker.Filepicker;
import io.filepicker.events.FpFilesReceivedEvent;
import io.filepicker.manager.data.FileUtils;
import io.filepicker.manager.data.FolderUtils;
import io.filepicker.manager.models.File;
import io.filepicker.manager.services.ManagerService;
import io.filepicker.manager.utils.Constants;
import io.filepicker.manager.utils.Utils;
import io.filepicker.models.FPFile;
import io.filepicker.services.ContentService;

public class ShareActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Utils.showQuickToast(this, R.string.sending_file);

        EventBus.getDefault().register(this);

        if(isLocalShare()) {
            Filepicker.setKey(Constants.API_KEY);
            ContentService.uploadFile(this, getIntent().getClipData().getItemAt(0).getUri());
        } else {
            ManagerService.shareContent(this, getUrl());
        }

        finish();
    }

    public void onEvent(FpFilesReceivedEvent event) {
        FPFile fpFile = event.fpFiles.get(0);

        File file = new File(fpFile.getUrl(), fpFile.getType(), fpFile.getFilename(),
                fpFile.getKey(), fpFile.getSize(), FolderUtils.ROOT_ID);

        FileUtils.insert(this, file);
        Utils.showQuickToast(this, R.string.file_added_to_filepicker);
    }

    private String getUrl() {
        String url = null;

        // For JellyBean and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            ClipData clip = getIntent().getClipData();

            if(clip != null) {
                url = clip.getItemAt(0).getText().toString();
            }
        }
        // For Ice Cream Sandwich
        else {
            url = getIntent().getDataString();
        }
        return url;
    }

    @Override
    protected void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    //
    // True if we share document from local provider
    private boolean isLocalShare() {
        boolean isLocalShare = false;

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            if(getIntent().getClipData().getItemAt(0).getText() == null)
                isLocalShare = true;
        } else {
            if(getIntent().getDataString() == null)
                isLocalShare = true;
        }

        return isLocalShare;
    }

}
