package io.filepicker.manager.fragments;

import android.app.Dialog;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.google.common.base.Optional;

import java.util.ArrayList;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import butterknife.OnItemClick;
import de.greenrobot.event.EventBus;
import io.filepicker.manager.R;
import io.filepicker.manager.adapters.FilesAdapter;
import io.filepicker.manager.adapters.FoldersAdapter;
import io.filepicker.manager.adapters.SectionedAdapter;
import io.filepicker.manager.data.FileUtils;
import io.filepicker.manager.data.FolderUtils;
import io.filepicker.manager.data.ManagerContract;
import io.filepicker.manager.enums.ListUpdatedEvent;
import io.filepicker.manager.events.FileUpdatedEvent;
import io.filepicker.manager.models.File;
import io.filepicker.manager.models.Folder;
import io.filepicker.manager.utils.Utils;

/**
 * Created by maciejwitowski on 11/26/14.
 */
public class MoveDialogFragment extends DialogFragment
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String KEY_FILES = "files";

    private ArrayList<File> files = null;
    private Folder currentFolder = null;
    private static final String CURRENT_FOLDER_STATE = "currentFolderState";

    private static final int LOADER_FOLDERS = 0;
    private static final int LOADER_FILES = 1;

    private FoldersAdapter mFoldersAdapter = null;
    private FilesAdapter mFilesAdapter = null;

    private SectionedAdapter mSectionedAdapter = null;

    private static final String TAG_FOLDERS_SECTION= "foldersSection";
    private static final String TAG_FILES_SECTION = "filesSection";

    @InjectView(R.id.lvFolders) ListView mList;
    @InjectView(R.id.layout_empty_list) LinearLayout mLayoutEmptyList;

    @InjectView(R.id.tvFolderName) TextView mTvFolderName;
    @InjectView(R.id.btnBack) ImageButton mBtnBack;
    @InjectView(R.id.btnMove) Button mBtnMove;


    public interface Contract {
        public void showNewFolderDialog(long parentFolderId);
    }

    public static MoveDialogFragment newInstance(ArrayList<File> files) {
        MoveDialogFragment frag = new MoveDialogFragment();

        Bundle args = new Bundle();
        args.putParcelableArrayList(KEY_FILES, files);

        frag.setArguments(args);

        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        files = getArguments().getParcelableArrayList(KEY_FILES);

        if(savedInstanceState != null) {
            Folder currentFolderState = savedInstanceState.getParcelable(CURRENT_FOLDER_STATE);
            if(currentFolderState != null) {
                currentFolder = currentFolderState;
            }
        } else {
            // Take the folder of the 1st element (all of them should have the same folder)
            long folderId = files.get(0).folderId;
            if(!FolderUtils.isRootFolder(folderId)) {
                Optional<Folder> optFolder = FolderUtils.getById(getActivity(), folderId);
                currentFolder = optFolder.get();
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_move, container, false);

        Dialog dialog = getDialog();
        if(dialog != null) {
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        }

        ButterKnife.inject(this, view);

        return view;
    }

    // This makes the dialog fill the screen
    @Override
    public void onStart()
    {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null)
        {
            int width = ViewGroup.LayoutParams.MATCH_PARENT;
            int height = ViewGroup.LayoutParams.MATCH_PARENT;
            dialog.getWindow().setLayout(width, height);
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        mFoldersAdapter = new FoldersAdapter(getActivity(), null, 0);
        mFilesAdapter = new FilesAdapter(getActivity(), null, 0, null);
        mFilesAdapter.markItemsEnabled(false);
        mFilesAdapter.setInfoBtnEnabled(false);
        mFilesAdapter.setMultipleSelectionEnabled(false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);

        LoaderManager manager = getLoaderManager();
        manager.initLoader(LOADER_FOLDERS, null, this);
        manager.initLoader(LOADER_FILES, null, this);
    }

    @Override
    public void onResume(){
        super.onResume();
        refreshList();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.reset(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(CURRENT_FOLDER_STATE, currentFolder);
        super.onSaveInstanceState(outState);
    }

    @OnItemClick(R.id.lvFolders)
    void onItemClick(int positionInList) {
        if(mSectionedAdapter.belongsToSection(positionInList, TAG_FOLDERS_SECTION)) {
            int position = mSectionedAdapter.getPositionInSection(positionInList, TAG_FOLDERS_SECTION);

            Optional<Cursor> optCursor =
                    Optional.fromNullable((Cursor) mFoldersAdapter.getItem(position));

            if (optCursor.isPresent()) {
                Cursor cursor = optCursor.get();

                long intentFolderId = cursor.getLong(FolderUtils.COLUMN_ID);

                Optional<Folder> intentFolder = FolderUtils.getById(getActivity(), intentFolderId);

                if (intentFolder.isPresent())
                    currentFolder = intentFolder.get();
                refreshList();
            }
        }
    }

    @OnClick(R.id.btnBack)
    public void goBack() {
        if(currentFolder != null) {

            long parentFolderId = currentFolder.parentId;
            // Parent is root
            if(FolderUtils.isRootFolder(parentFolderId)) {
                currentFolder = null;
            } else {
                Optional<Folder> optParent =
                        FolderUtils.getById(getActivity(), parentFolderId);

                if(optParent.isPresent()) {
                    currentFolder = optParent.get();
                }
            }

            refreshList();
        }
    }

    private void refreshList() {
        LoaderManager manager = getLoaderManager();
        manager.restartLoader(LOADER_FOLDERS, null, this);
        manager.restartLoader(LOADER_FILES, null, this);
    }

    @OnClick(R.id.btnCancel)
    public void hideDialog() {
        dismiss();
    }

    @OnClick(R.id.btnMove)
    public void moveFile() {
        // Set folder as root
        if(Utils.isRootFolder(currentFolder)) {
            updateFileFolder(-1);
        } else {
            updateFileFolder(currentFolder.id);
        }
    }

    @OnClick(R.id.btnNewFolder)
    public void showNewFolderDialog() {
        getContract().showNewFolderDialog(Utils.getFolderIdOrRoot(currentFolder));
    }

    private void updateFileFolder(long folderId) {
        int counter = 0;
        for(File file : files) {
            file.setFolderId(folderId);
            if(FileUtils.update(getActivity(), file) > 0) {
                EventBus.getDefault().post(new FileUpdatedEvent(file));
                counter++;
            }
        }

        if(counter > 0) EventBus.getDefault().post(new ListUpdatedEvent());

        final String folderName = ((currentFolder == null) ? " root folder" : currentFolder.name);

        if(files.size() == 1) {
            Utils.showQuickToast(getActivity(), files.get(0).filename + " was moved to " + folderName);
        } else {
            Utils.showQuickToast(getActivity(), counter + " files were moved to " + folderName);
        }

        dismiss();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {

        Uri uri;
        String[] columns;
        String selection;
        String[] selectionArgs = new String[]{String.valueOf(Utils.getFolderIdOrRoot(currentFolder))};
        String sortOrder;

        if(id == LOADER_FOLDERS) {
            uri = ManagerContract.Folder.CONTENT_URI;
            columns = FoldersAdapter.FoldersListQuery.PROJECTION;
            selection = ManagerContract.Folder.COLUMN_PARENT_ID + " = ?";
            sortOrder = ManagerContract.Folder.COLUMN_NAME;

        } else {
            uri = ManagerContract.File.CONTENT_URI;
            selection = ManagerContract.File.COLUMN_FOLDER_ID + " = ?";
            columns = FileUtils.COLUMNS;
            sortOrder = ManagerContract.File.COLUMN_FILENAME;
        }

        return new CursorLoader(getActivity(), uri, columns, selection, selectionArgs, sortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if(cursorLoader.getId() == LOADER_FOLDERS) {
            mFoldersAdapter.swapCursor(cursor);
        } else {
            mFilesAdapter.swapCursor(cursor);
        }

        mSectionedAdapter = new SectionedAdapter.Builder()
                .add(new SectionedAdapter.Section(getHeader("Folders"), mFoldersAdapter, TAG_FOLDERS_SECTION))
                .add(new SectionedAdapter.Section(getHeader("Files"), mFilesAdapter, TAG_FILES_SECTION))
                .build();

        mList.setAdapter(mSectionedAdapter.getAdapter());
        mList.setEmptyView(mLayoutEmptyList);

        if(Utils.isRootFolder(currentFolder)) {
            mBtnBack.setVisibility(View.INVISIBLE);
            mTvFolderName.setText("");
        } else {
            mTvFolderName.setText(currentFolder.name);
            mBtnBack.setVisibility(View.VISIBLE);
        }


        // Disable move button if the selected folder is file's folder (no moving needed)
        if(isSameFolder()) {
            Utils.disableButton(getActivity().getApplicationContext(), mBtnMove);
        } else {
            Utils.enableButton(getActivity().getApplicationContext(), mBtnMove);
        }
    }

    private View getHeader(String text) {
        View view = getActivity().getLayoutInflater().inflate(R.layout.list_item_subheader,null);
        ((TextView)view.findViewById(R.id.tvSubheader)).setText(text);
        return view;
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        if(cursorLoader.getId() == LOADER_FOLDERS) {
            mFoldersAdapter.swapCursor(null);
        } else {
            mFilesAdapter.swapCursor(null);
        }
    }

    public Contract getContract() {
        return (Contract) getActivity();
    }

    // Check if the chosen folder is the folder the file is already in
    public boolean isSameFolder() {
        if (Utils.isRootFolder(currentFolder))
            return files.get(0).folderId == -1;
        else
            return files.get(0).folderId == currentFolder.id;
    }
}
