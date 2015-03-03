package io.filepicker.manager.fragments;

import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.google.common.base.Optional;

import java.util.ArrayList;
import java.util.HashSet;

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
import io.filepicker.manager.enums.Operation;
import io.filepicker.manager.events.FileInfoShown;
import io.filepicker.manager.events.FileThumbnailClickedEvent;
import io.filepicker.manager.events.ListBackPressedEvent;
import io.filepicker.manager.models.File;
import io.filepicker.manager.models.Folder;
import io.filepicker.manager.utils.Utils;

/**
 * Created by maciejwitowski on 11/4/14.
 */
public class ListFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<Cursor> {

    public interface Contract {
        public void showFile(File file);
        public void saveFile(File file, Operation operation);
        public void exportFile(File file);
        public void showNewFolderDialog(long parentFolderId);
        public void showNewFileView();
        public void showMoveDialog(ArrayList<File> file);
        public void shareFile(File file);
    }

    @InjectView(R.id.lvItems) ListView mList;
    @InjectView(R.id.floating_add_menu) FloatingActionsMenu mFloatingAddMenu;
    @InjectView(R.id.layout_empty_list) LinearLayout mLayoutEmptyList;
    @InjectView(R.id.toolbar) Toolbar mToolbar;
    private TextView mActionModeTitle;

    // For selecting multiple files
    private HashSet<Integer> selectedFiles;

    private static final int LOADER_FOLDER = 0;
    private static final int LOADER_FILES = 1;

    private static final String FOLDER_STATE = "folder";
    private static final String SELECTED_FILES_STATE = "selectedFilesState";

    private static final String TAG_FOLDERS_SECTION= "foldersSection";
    private static final String TAG_FILES_SECTION = "filesSection";

    private Folder folder = null;
    public Folder getCurrentFolder() {
        return folder;
    }

    private FoldersAdapter mFoldersAdapter = null;
    private FilesAdapter mFilesAdapter = null;
    private SectionedAdapter mSectionedAdapter = null;

    private ActionMode mActionMode;
    boolean isActionMode = false;

    private boolean mFoldersReady = false;
    private boolean mFilesReady = false;

    private enum HeaderType {
        FILES, FOLDERS
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(savedInstanceState != null) {
            ArrayList<Integer> filesArray =
                    savedInstanceState.getIntegerArrayList(SELECTED_FILES_STATE);

            if(filesArray != null)
                selectedFiles = new HashSet<>(filesArray);

            Folder savedFolder = savedInstanceState.getParcelable(FOLDER_STATE);

            if(savedFolder != null)
                folder = savedFolder;
        } else {
            selectedFiles = new HashSet<>();
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_folder, container, false);

        ButterKnife.inject(this, view);

        initToolbar(view);

        if(selectedFiles.size() > 0)
            startActionMode();

        registerForContextMenu(mList);

        return view;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;

        if(mSectionedAdapter.belongsToSection(info.position, TAG_FILES_SECTION)) {
            MenuInflater inflater = getActivity().getMenuInflater();
            inflater.inflate(R.menu.files_context, menu);

            Optional<File> optFile = getFile(info.position);

            if(optFile.isPresent()) {
                String title = Utils.getShortName(optFile.get().filename);
                menu.setHeaderView(getContextMenuHeader(title));
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo menuInfo
                = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

        Optional<File> optFile = getFile(menuInfo.position);

        if(!optFile.isPresent())
            return true;

        switch(item.getItemId()) {
            case R.id.action_export:
                getContract().exportFile(optFile.get());
                return true;

            case R.id.action_move_to_folder:
                ArrayList<File> fileToMove = new ArrayList<>();
                fileToMove.add(optFile.get());
                getContract().showMoveDialog(fileToMove);
                return true;

            case R.id.action_share:
                getContract().shareFile(optFile.get());
                return true;

            case R.id.action_save:
                getContract().saveFile(optFile.get(), Operation.SAVE);
                return true;

            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if(!(activity instanceof Contract)) {
            throw new ClassCastException("Activity must implement fragment's contract");
        }
    }

    @Override
    public void onPause() {
        EventBus.getDefault().unregister(this);
        super.onPause();
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    private void startActionMode() {
        mActionMode = getActivity().startActionMode(mActionModeCallback);

        View view = View.inflate(getActivity(), R.layout.action_mode_layout, null);
        mActionModeTitle = (TextView) view.findViewById(R.id.action_mode_title);
        mActionModeTitle.setText(String.valueOf(selectedFiles.size()));
        mActionMode.setCustomView(view);
        isActionMode = true;
        updateToolbarVisibility();
    }

    public void onEvent(FileThumbnailClickedEvent event) {
        int position = event.position;

        if(selectedFiles.contains(position))
            selectedFiles.remove(position);
        else
            selectedFiles.add(position);

        updateActionMode(selectedFiles.size());
    }

    public void onEvent(ListBackPressedEvent event) {
        refreshToParentFolder();
    }

    public void onEvent(ListUpdatedEvent event) {
        restartLoaders();
    }

    private void updateActionMode(int size) {
        // Last element was removed
        if(size == 0)
            finishActionMode();
        // First element was added
        else if(!isActionMode && size == 1 )
            startActionMode();
        else
            mActionModeTitle.setText(String.valueOf(size));
    }

    private void finishActionMode() {
        selectedFiles.clear();

        if(mActionMode != null)
            mActionMode.finish();

        isActionMode = false;

        getLoaderManager().restartLoader(LOADER_FILES, null, this);

        updateToolbarVisibility();
    }

    public void onEvent(FileInfoShown event) {
        collapseFloatingMenu();
        finishActionMode();
    }

    private void collapseFloatingMenu() {
        if(mFloatingAddMenu != null)
            mFloatingAddMenu.collapse();
    }

    @OnClick(R.id.add_folder)
    public void showNewFolderDialog() {
        collapseFloatingMenu();
        getContract().showNewFolderDialog(Utils.getFolderIdOrRoot(folder));
    }

    @OnClick(R.id.add_file)
    public void showNewFileDialog() {
        collapseFloatingMenu();
        getContract().showNewFileView();
    }

    private void initToolbar(View view) {
        drawToolbar();

        ((ActionBarActivity) getActivity()).setSupportActionBar(mToolbar);

        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshToParentFolder();
            }
        });
    }

    private void refreshToParentFolder() {
        if(folder != null) {
            // Parent is root
            if(FolderUtils.isRootFolder(folder.parentId)) {
                folder = null;
            } else {
                Optional<Folder> optParent =
                        FolderUtils.getById(getActivity(), folder.parentId);

                if(optParent.isPresent()) {
                    folder = optParent.get();
                }
            }

            restartLoaders();
            drawToolbar();
        }
    }

    private void updateToolbarVisibility() {
        if(mToolbar == null) return;

        mToolbar.setVisibility(
                isActionMode ? View.GONE : View.VISIBLE
        );
    }


    private void drawToolbar() {
        if(mToolbar == null) return;

        if(Utils.isRootFolder(folder)) {
            mToolbar.setLogo(R.drawable.ic_action_logo_appbar);
            mToolbar.setNavigationIcon(null);
            setToolbarTitle("");

        // Set Toolbar for subfolders
        } else {
            mToolbar.setLogo(null);
            mToolbar.setNavigationIcon(R.drawable.ic_action_back);
            setToolbarTitle(folder.name);
        }

        updateToolbarVisibility();
    }

    private void setToolbarTitle(String title) {
        if(mToolbar != null) {
            mToolbar.setTitle(title);
        }
    }

    // If folder was clicked, show it's content (Files kind items are handled in adapter)
    @OnItemClick(R.id.lvItems)
    void onItemClick(int position) {
        collapseFloatingMenu();
        finishActionMode();

        if(mSectionedAdapter.belongsToSection(position, TAG_FOLDERS_SECTION))
            showFolder(position);
        else
            showFile(position);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putIntegerArrayList(SELECTED_FILES_STATE, new ArrayList<>(selectedFiles));
        outState.putParcelable(FOLDER_STATE, folder);
        super.onSaveInstanceState(outState);
    }

    private void showFolder(int positionInList) {
        int position = mSectionedAdapter.getPositionInSection(positionInList, TAG_FOLDERS_SECTION);

        Optional<Cursor> optCursor =
                Optional.fromNullable((Cursor) mFoldersAdapter.getItem(position));

        if (optCursor.isPresent()) {
            long intentFolderId = optCursor.get().getLong(FolderUtils.COLUMN_ID);

            Optional<Folder> intentFolder = FolderUtils.getById(getActivity(), intentFolderId);

            if (intentFolder.isPresent()) {
                folder = intentFolder.get();
                setToolbarTitle(folder.name);
                drawToolbar();

                restartLoaders();
            }
        }
    }

    private void showFile(int positionInList) {
        Optional<File> optFile = getFile(positionInList);

        if(optFile.isPresent()) {
            getContract().showFile(optFile.get());
        }
    }

    // Returns file from position in the whole list (headers+files+folders)
    private Optional<File> getFile(int positionInList) {
        int filePosition = mSectionedAdapter.getPositionInSection(positionInList, TAG_FILES_SECTION);

        if(filePosition != SectionedAdapter.INVALID_POSITION)
            return getFileFromAdapter(filePosition);
        else
            return Optional.absent();

    }

    // Returns file from position in files adapter
    private Optional<File> getFileFromAdapter(int position) {
        Cursor cursor = (Cursor) mFilesAdapter.getItem(position);
        long fileId = cursor.getLong(FileUtils.COLUMN_ID);

        return FileUtils.getById(getActivity(), fileId);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);

        checkIfAdaptersInitialized();

        LoaderManager manager = getLoaderManager();
        manager.initLoader(LOADER_FOLDER, null, this);
        manager.initLoader(LOADER_FILES, null, this);
    }

    @Override
    public void onResume(){
        super.onResume();
        restartLoaders();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
        Uri uri;
        String[] columns;
        String selection;
        String[] selectionArgs = new String[]{String.valueOf(Utils.getFolderIdOrRoot(folder))};
        String sortOrder;

        if(id == LOADER_FOLDER) {
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
        if(cursorLoader.getId() == LOADER_FOLDER) {
            mFoldersAdapter.swapCursor(cursor);
            mFoldersReady = true;
        } else if(cursorLoader.getId() == LOADER_FILES) {
            mFilesAdapter.swapCursor(cursor);
            mFilesReady = true;
        }

        if(loadersReady()) {
            mSectionedAdapter = new SectionedAdapter.Builder()
                    .add(new SectionedAdapter.Section(getHeader(HeaderType.FOLDERS), mFoldersAdapter, TAG_FOLDERS_SECTION))
                    .add(new SectionedAdapter.Section(getHeader(HeaderType.FILES), mFilesAdapter, TAG_FILES_SECTION))
                    .build();

            mList.setAdapter(mSectionedAdapter.getAdapter());
            mList.setEmptyView(mLayoutEmptyList);
            mFoldersReady = false;
            mFilesReady = false;
        }
    }

    private boolean loadersReady() {
        return mFoldersReady && mFilesReady;
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        if(cursorLoader.getId() == LOADER_FOLDER) {
            mFoldersAdapter.swapCursor(null);
        } else if (cursorLoader.getId() == LOADER_FILES) {
            mFilesAdapter.swapCursor(null);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.reset(this);
    }

    private void checkIfAdaptersInitialized() {
        if(mFoldersAdapter == null || mFilesAdapter != null) {
            initAdapters();
        }
    }

    private void initAdapters() {
        mFoldersAdapter = new FoldersAdapter(getActivity(), null, 0);
        mFilesAdapter = new FilesAdapter(getActivity(), null, 0, selectedFiles);
    }

    private void restartLoaders() {
        initAdapters();

        LoaderManager manager = getLoaderManager();
        manager.restartLoader(LOADER_FILES, null, this);
        manager.restartLoader(LOADER_FOLDER, null, this);
    }

    private Contract getContract() {
        return (Contract) getActivity();
    }

    private View getHeader(HeaderType type) {
        int resId = type.equals(HeaderType.FOLDERS) ? R.string.header_folders : R.string.header_files;

        View view = getActivity().getLayoutInflater().inflate(R.layout.list_item_subheader,null);
        ((TextView)view.findViewById(R.id.tvSubheader)).setText(getText(resId));
        return view;
    }

    private View getContextMenuHeader(String text) {
        View view = getActivity().getLayoutInflater().inflate(R.layout.context_menu_header, null);
        ((TextView)view.findViewById(R.id.tvMenuTitle)).setText(text);
        return view;
    }

    private void processMultipleMove() {
        ArrayList<File> pickedFiles = new ArrayList<>();

        for(Integer position : selectedFiles) {
            Optional<File> optFile = getFileFromAdapter(position);

            if(optFile.isPresent()) {
                pickedFiles.add(optFile.get());
            }
        }

        if(!pickedFiles.isEmpty())
            getContract().showMoveDialog(pickedFiles);
    }

    private final ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            MenuInflater inflater = actionMode.getMenuInflater();
            inflater.inflate(R.menu.action_mode_menu, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode actionMode, MenuItem item) {
            switch(item.getItemId()) {
                case R.id.action_move_to_folder:
                    processMultipleMove();
                    finishActionMode();
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            finishActionMode();
        }
    };
}