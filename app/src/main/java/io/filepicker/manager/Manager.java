package io.filepicker.manager;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.widget.Toast;

import com.google.common.base.Optional;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import de.greenrobot.event.EventBus;
import io.filepicker.Filepicker;
import io.filepicker.manager.adapters.FilesAdapter;
import io.filepicker.manager.data.FileUtils;
import io.filepicker.manager.data.FolderUtils;
import io.filepicker.manager.data.ManagerContract;
import io.filepicker.manager.enums.ListUpdatedEvent;
import io.filepicker.manager.enums.Operation;
import io.filepicker.manager.events.FileSavedEvent;
import io.filepicker.manager.events.ListBackPressedEvent;
import io.filepicker.manager.fragments.FileFragment;
import io.filepicker.manager.fragments.FileInfoFragment;
import io.filepicker.manager.fragments.ListFragment;
import io.filepicker.manager.fragments.MoveDialogFragment;
import io.filepicker.manager.fragments.NewFolderDialogFragment;
import io.filepicker.manager.models.File;
import io.filepicker.manager.models.Folder;
import io.filepicker.manager.services.ManagerService;
import io.filepicker.manager.utils.Constants;
import io.filepicker.manager.utils.Utils;
import io.filepicker.models.FPFile;


public class Manager extends ActionBarActivity
        implements NewFolderDialogFragment.Contract,
                   FilesAdapter.Contract,
                   FileInfoFragment.Contract,
                   MoveDialogFragment.Contract,
                   ListFragment.Contract {

    private static final String TAG_NEW_FOLDER_DIALOG = "new_folder";
    private static final String TAG_FILE_FRAGMENT = "file_fragment";
    private static final String TAG_FILE_INFO_FRAGMENT = "file_info_fragment";
    private static final String TAG_MOVE_FRAGMENT = "move_fragment";
    private static final String TAG_LIST_FRAGMENT = "list_fragment";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(Utils.isBeforeLollipop()) {
            setContentView(R.layout.manager_activity);
        }

        ButterKnife.inject(this);

        if (getSupportFragmentManager().findFragmentById(getContentView()) == null) {
            addFolderFragment();
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
    public void saveFile(File file, Operation operation) {
        Optional<Uri> contentUri = FileUtils.getContentUri(this, file);

        if (contentUri.isPresent()) {
            Utils.showQuickToast(this, R.string.file_already_saved);
            return;
        }

        Toast.makeText(this, R.string.saving_file, Toast.LENGTH_LONG).show();

        ManagerService.saveFile(this, ManagerContract.File.buildUri(file.id), operation);
    }

    @Override
    public void exportFile(File file) {
        Optional<Uri> contentUri = FileUtils.getContentUri(this, file);

        if (contentUri.isPresent()) {
            Filepicker.setKey(Constants.API_KEY);

            Intent intent = new Intent()
                    .setAction(Filepicker.ACTION_EXPORT_FILE)
                    .setClass(this, Filepicker.class)
                    .putExtra(Intent.EXTRA_STREAM, contentUri.get())
                    .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    .setData(contentUri.get());

            startActivityForResult(intent, Filepicker.REQUEST_CODE_EXPORT_FILE);
        } else {
            showSaveNeededDialog(file, Operation.EXPORT);
        }
    }

    private void showSaveNeededDialog(final File file, final Operation operation) {
        new AlertDialog.Builder(this)
            .setTitle(R.string.question_save)
            .setMessage(R.string.save_required)
            .setPositiveButton(R.string.question_continue, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    saveFile(file, operation);
                }
            })
            .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    // do nothing
                }
            })
            .show();
    }

    public void onEvent(FileSavedEvent event) {
        Utils.showLongToast(this, "File " + event.file.filename + " has been saved");
        Operation operation = Operation.fromString(event.operationType);

        switch(operation) {
            case EXPORT:
                exportFile(event.file);
                break;
            case SHARE:
                shareFile(event.file);
                break;
        }
    }

    @Override
    public void showNewFolderDialog(long parentFolderId) {
        NewFolderDialogFragment frag = NewFolderDialogFragment.newInstance(parentFolderId);
        frag.show(getSupportFragmentManager(), TAG_NEW_FOLDER_DIALOG);
    }

    @Override
    public void showNewFileView() {
        Utils.getFileFromLibrary(this);
    }

    // Checks if user is in any folder.
    // If so then current folder is set as parent of the new folder.
    // if not, then new folder is created in root folder.
    @Override
    public void createNewFolder(String name, long parentFolderId) {
        long id = FolderUtils.insert(this, new Folder(name, parentFolderId));

        if(id != -1) {
            EventBus.getDefault().post(new ListUpdatedEvent());
        }

        Utils.showQuickToast(this, "Folder " + name + " was created");
    }

    @Override
    public void showFile(File file) {
        slideInFragment(FileFragment.newInstance(file), TAG_FILE_FRAGMENT);
    }

    @Override
    public void showFileInfo(File file) {
        slideInFragment(FileInfoFragment.newInstance(file), TAG_FILE_INFO_FRAGMENT);
    }

    @Override
    public void showMoveDialog(ArrayList<File> files) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Fragment prev = getSupportFragmentManager().findFragmentByTag(TAG_MOVE_FRAGMENT);

        if(prev != null)
            ft.remove(prev);

        ft.addToBackStack(null);

        DialogFragment moveFragment = MoveDialogFragment.newInstance(files);
        moveFragment.show(ft, TAG_MOVE_FRAGMENT);
    }

    @Override
    public void shareFile(File file) {
        Optional<Uri> contentUri = FileUtils.getContentUri(this, file);
        if (contentUri.isPresent()) {
            List<ResolveInfo> shareResolvers =
                    Utils.getIntentResolvers(this, Intent.ACTION_SEND, file.type);

            if(!shareResolvers.isEmpty()) {
                Intent shareIntent = getFilteredShareIntent(shareResolvers, contentUri.get());
                startActivity(shareIntent);
            }
        } else {
            showSaveNeededDialog(file, Operation.SHARE);
        }
    }

    private void addFolderFragment() {
        getSupportFragmentManager().beginTransaction()
                .add(getContentView(), new ListFragment(), TAG_LIST_FRAGMENT)
                .commit();
    }

    @Override
    public void onBackPressed() {
        // Not in root - go up
        if(getSupportFragmentManager().findFragmentByTag(TAG_FILE_FRAGMENT) != null) {
            Fragment fileFragment =
                    getSupportFragmentManager().findFragmentByTag(TAG_FILE_FRAGMENT);

            slideOutFragment(fileFragment);

        } else if(getSupportFragmentManager().findFragmentByTag(TAG_FILE_INFO_FRAGMENT) != null) {
            Fragment fileInfoFragment =
                    getSupportFragmentManager().findFragmentByTag(TAG_FILE_INFO_FRAGMENT);

            slideOutFragment(fileInfoFragment);

        } else if(getSupportFragmentManager().findFragmentByTag(TAG_LIST_FRAGMENT) != null &&
                ((ListFragment)getSupportFragmentManager()
                        .findFragmentByTag(TAG_LIST_FRAGMENT))
                        .getCurrentFolder() != null) {

            EventBus.getDefault().post(new ListBackPressedEvent());
        } else {
            super.onBackPressed();
        }
    }

    private void slideOutFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.right_slide_out_back, R.anim.right_slide_in_back)
                .remove(fragment)
                .commit();
    }

    private void slideInFragment(Fragment fragment, String tag) {
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.right_slide_in, R.anim.right_slide_out)
                .add(getContentView(), fragment, tag)
                .commit();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == Filepicker.REQUEST_CODE_GETFILE) {
            if(resultCode != RESULT_OK)
                return;

            ArrayList<FPFile> fpFiles = data.getParcelableArrayListExtra(Filepicker.FPFILES_EXTRA);
            FPFile fpFile = fpFiles.get(0);

            // Get currently displayed folder from the fragment
            ListFragment listFragment =
                    (ListFragment) getSupportFragmentManager().findFragmentByTag(TAG_LIST_FRAGMENT);

            Folder currentFolder = null;
            if(listFragment != null) {
                currentFolder = listFragment.getCurrentFolder();
            }

            File newItem = new File(fpFile.getUrl(), fpFile.getType(),
                    fpFile.getFilename(), fpFile.getKey(),
                    fpFile.getSize(), Utils.getFolderIdOrRoot(currentFolder));

            FileUtils.insert(this, newItem);
            Toast.makeText(this, R.string.new_item_added, Toast.LENGTH_SHORT).show();
        }
    }

    private int getContentView() {
        return android.R.id.content;
    }

    /** Returns share intent for selected resolvers with current package filtered out
     * (so this app does not share with itself)
     */
    private Intent getFilteredShareIntent(List<ResolveInfo> intentResolvers, Uri uri) {
        List<Intent> targetedShareIntents = new ArrayList<>();

        for(ResolveInfo resInfo : intentResolvers) {
            String packageName = resInfo.activityInfo.packageName;
            if(!packageName.contains(getPackageName())) {
                Intent targetedShareIntent = new Intent(Intent.ACTION_SEND)
                        .putExtra(Intent.EXTRA_STREAM, uri)
                        .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        .setData(uri)
                        .setPackage(packageName);

                targetedShareIntents.add(targetedShareIntent);
            }
        }

        Intent chooserIntent = Intent.createChooser(targetedShareIntents.remove(0),
                getResources().getString(R.string.share_file_via));

        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS,
                targetedShareIntents.toArray(new Parcelable[] {}));

        return chooserIntent;
    }
}
