package io.filepicker.manager.getcontent;

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
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.google.common.base.Optional;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import butterknife.OnItemClick;
import io.filepicker.manager.R;
import io.filepicker.manager.adapters.FilesAdapter;
import io.filepicker.manager.adapters.FoldersAdapter;
import io.filepicker.manager.adapters.SectionedAdapter;
import io.filepicker.manager.data.FileUtils;
import io.filepicker.manager.data.FolderUtils;
import io.filepicker.manager.data.ManagerContract;
import io.filepicker.manager.models.Folder;
import io.filepicker.manager.utils.Utils;

/**
 * Created by maciejwitowski on 11/6/14.
 */
public class GetContentFragment extends DialogFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    @InjectView(R.id.lvFolders) ListView mList;
    @InjectView(R.id.layout_empty_list) LinearLayout mLayoutEmptyList;

    @InjectView(R.id.tvFolderName) TextView mTvFolderName;
    @InjectView(R.id.btnBack) ImageButton mBtnBack;

    public interface Contract {
        public void returnContent(Uri uri);
        public void useFilepickerLibrary();
    }


    private Folder currentFolder = null;
    private static final String CURRENT_FOLDER_STATE = "currentFolderState";

    private static final int LOADER_FOLDERS = 0;
    private static final int LOADER_FILES = 1;

    private FoldersAdapter mFoldersAdapter = null;
    private FilesAdapter mFilesAdapter = null;

    private SectionedAdapter mSectionedAdapter = null;

    private static final String TAG_FOLDERS_SECTION= "foldersSection";
    private static final String TAG_FILES_SECTION = "filesSection";

    public static GetContentFragment newInstance() {
        return new GetContentFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(savedInstanceState != null) {
            Folder currentFolderState = savedInstanceState.getParcelable(CURRENT_FOLDER_STATE);
            if(currentFolderState != null) {
                currentFolder = currentFolderState;
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_get_content, container, false);

        ButterKnife.inject(this, view);
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.reset(this);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        mFoldersAdapter = new FoldersAdapter(getActivity(), null, 0);
        mFilesAdapter = new FilesAdapter(getActivity(), null, 0, null);
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
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(CURRENT_FOLDER_STATE, currentFolder);
        super.onSaveInstanceState(outState);
    }

    private void refreshList() {
        LoaderManager manager = getLoaderManager();
        manager.restartLoader(LOADER_FOLDERS, null, this);
        manager.restartLoader(LOADER_FILES, null, this);
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
            mBtnBack.setVisibility(View.GONE);
            mTvFolderName.setText("");
        } else {
            mTvFolderName.setText(currentFolder.name);
            mBtnBack.setVisibility(View.VISIBLE);
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
        } else {
            int position = mSectionedAdapter.getPositionInSection(positionInList, TAG_FILES_SECTION);
            Optional<Cursor> optCursor =
                    Optional.fromNullable((Cursor) mFilesAdapter.getItem(position));

            if (optCursor.isPresent()) {
                Cursor cursor = optCursor.get();
                long fileId = cursor.getLong(FileUtils.COLUMN_ID);
                Uri uri = ManagerContract.File.buildUri(fileId);

                getContract().returnContent(uri);
            }
        }
    }


    @OnClick(R.id.btnAdd)
    public void useFilepickerLibrary() {
        getContract().useFilepickerLibrary();
    }

    @OnClick(R.id.btnBack)
    public void goBack() {
        if(currentFolder != null) {

            // Parent is root
            if(currentFolder.parentId == -1) {
                currentFolder = null;
            } else {
                Optional<Folder> optParent =
                        FolderUtils.getById(getActivity(), currentFolder.parentId);

                if(optParent.isPresent()) {
                    currentFolder = optParent.get();
                }
            }

            refreshList();
        }
    }

    public Contract getContract() {
        return ((Contract) getActivity());
    }
}
