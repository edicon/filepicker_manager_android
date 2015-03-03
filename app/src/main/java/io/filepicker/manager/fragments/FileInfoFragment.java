package io.filepicker.manager.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import de.greenrobot.event.EventBus;
import io.filepicker.manager.R;
import io.filepicker.manager.data.FileUtils;
import io.filepicker.manager.data.FolderUtils;
import io.filepicker.manager.data.ManagerContract;
import io.filepicker.manager.enums.Operation;
import io.filepicker.manager.events.FileUpdatedEvent;
import io.filepicker.manager.models.File;
import io.filepicker.manager.models.Folder;
import io.filepicker.manager.utils.Utils;

/**
 * Created by maciejwitowski on 11/25/14.
 */
public class  FileInfoFragment extends Fragment {

    public interface Contract {
        public void showFile(File file);
        public void exportFile(File file);
        public void saveFile(File file, Operation operation);
        public void showMoveDialog(ArrayList<File> file);
        public void shareFile(File file);
    }

    private static final String KEY_FILE = "file";
    private File file = null;

    @InjectView(R.id.imageView) ImageButton mImageView;
    @InjectView(R.id.tvName) TextView mTvName;
    @InjectView(R.id.tvKind) TextView mTvKind;
    @InjectView(R.id.tvSize) TextView mTvSize;
    @InjectView(R.id.tvLocation) TextView mTvLocation;
    @InjectView(R.id.tvCreated) TextView mTvCreated;
    @InjectView(R.id.progressBar) ProgressBar mProgressBar;
    @InjectView(R.id.fileIcon) ImageView mFileIcon;
    @InjectView(R.id.tvExtension) TextView mTvExtension;
    @InjectView(R.id.layoutFileIcon) RelativeLayout mLayoutFileIcon;

    public static FileInfoFragment newInstance(File file) {
        FileInfoFragment frag = new FileInfoFragment();
        Bundle args = new Bundle();
        args.putParcelable(KEY_FILE, file);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        file = getArguments().getParcelable(KEY_FILE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_file_info, container, false);

        ButterKnife.inject(this, view);

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        if(Utils.isImage(file.type))
            loadImageView();
        else
            loadFileView();

        // Setting details
        mTvName.setText(file.filename);
        mTvKind.setText(file.getReadableType());
        mTvSize.setText(file.getReadableSize());
        mTvLocation.setText(getFileFolderName());
        mTvCreated.setText(Utils.getReadableTimestamp(file.createdAt));
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if(!(activity instanceof Contract)) {
            throw new ClassCastException("Activity must implement fragment's contract");
        }
    }

    private void loadImageView() {
        mImageView.setVisibility(View.VISIBLE);
        mFileIcon.setImageResource(R.drawable.ic_action_picture_file);
        Picasso.with(getActivity())
                .load(FileUtils.getFileDownloadUrl(getActivity(), file))
                .fit().centerCrop()
                .into(mImageView, new Callback() {
                    @Override
                    public void onSuccess() {
                        Utils.updateProgressBar(mProgressBar, false);
                    }

                    @Override
                    public void onError() {
                        Utils.updateProgressBar(mProgressBar, false);
                    }
                });
    }

    // For non-images
    private void loadFileView() {
        mImageView.setImageResource(R.drawable.ic_action_other_file);
        mLayoutFileIcon.setVisibility(View.VISIBLE);
        mTvExtension.setText(Utils.getFileExtension(file.filename));
        mFileIcon.setImageResource(R.drawable.ic_action_add_file);
        Utils.updateProgressBar(mProgressBar, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        // In case the parent was changed in dialog
        EventBus.getDefault().register(this);
    }

    @Override
    public void onPause() {
        // In case the parent was changed in dialog
        EventBus.getDefault().unregister(this);
        super.onPause();
    }

    public void onEvent(FileUpdatedEvent event) {
        file = event.file;

        // For now only location can be updated. May need change in the future
        mTvLocation.setText(getFileFolderName());
    }

    @OnClick(R.id.btn_export_file)
    public void exportFile() {
        getContract().exportFile(file);
    }

    @OnClick(R.id.btn_save_file)
    public void saveFile() {
        getContract().saveFile(file, Operation.SAVE);
    }

    @OnClick(R.id.btn_move_file)
    public void showMoveDialog() {
        ArrayList<File> fileToMove = new ArrayList<>();
        fileToMove.add(file);
        getContract().showMoveDialog(fileToMove);
    }

    @OnClick(R.id.btn_share_file)
    public void shareFile() {
        getContract().shareFile(file);
    }

    @OnClick({R.id.imageView, R.id.layoutFileIcon})
    public void showFile() {
        getContract().showFile(file);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.reset(this);
    }

    private Contract getContract() {
        return (Contract) getActivity();
    }

    private String getFileFolderName() {
        if(FolderUtils.isRootFolder(file.folderId)) {
           return getResources().getString(R.string.root_folder);
        } else {
            Folder folder = FolderUtils.get(getActivity(),
                    ManagerContract.Folder.buildUri(file.folderId)).get();

            return folder.name;
        }
    }
}
