package io.filepicker.manager.fragments;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import butterknife.ButterKnife;
import butterknife.InjectView;
import io.filepicker.manager.R;
import io.filepicker.manager.data.FileUtils;
import io.filepicker.manager.models.File;
import io.filepicker.manager.utils.Utils;

/**
 * Created by maciejwitowski on 11/25/14.
 */
public class FileFragment extends Fragment {

    private static final String KEY_FILE = "file";
    private File file = null;

    private static final String GOOGLE_DRIVE_PATH =
            "https://docs.google.com/gview?embedded=true&url=";

    @InjectView(R.id.progressBar) ProgressBar mProgressBar;
    @InjectView(R.id.imagePicture) ImageView mImageView;
    @InjectView(R.id.webViewFile) WebView mWebView;

    public static FileFragment newInstance(File file) {
        FileFragment frag = new FileFragment();
        Bundle args = new Bundle();
        args.putParcelable(KEY_FILE, file);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        file = args.getParcelable(KEY_FILE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_file, container, false);

        initToolbar(view);

        ButterKnife.inject(this, view);

        return view;
    }


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        if (Utils.isImage(file.type))
            loadImage();
        else
            loadDocument();
    }


    private void loadImage() {
        mImageView.setVisibility(View.VISIBLE);

        Picasso.with(getActivity())
            .load(FileUtils.getFileDownloadUrl(getActivity(), file))
            .into(mImageView, new Callback() {
                @Override
                public void onSuccess() {
                    Utils.updateProgressBar(mProgressBar, false);
                }

                @Override
                public void onError() {
                    Context mContext = getActivity();
                    if(mContext == null) return;

                    Utils.showQuickToast(getActivity(), R.string.no_internet);
                    Utils.updateProgressBar(mProgressBar, false);
                    ((ActionBarActivity)mContext).getSupportFragmentManager()
                            .beginTransaction()
                            .setCustomAnimations(R.anim.right_slide_out_back, R.anim.right_slide_in_back)
                            .remove(FileFragment.this).commit();
                }
            });
    }


    private void loadDocument() {
        mWebView.setVisibility(View.VISIBLE);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.loadUrl(GOOGLE_DRIVE_PATH + file.url);
        Utils.updateProgressBar(mProgressBar, false);
    }


    private void initToolbar(View view) {
        Toolbar mToolbar = (Toolbar) view.findViewById(R.id.toolbar);
        ((ActionBarActivity) getActivity()).setSupportActionBar(mToolbar);
        ((ActionBarActivity) getActivity()).getSupportActionBar().setTitle(file.filename);

        mToolbar.setLogo(R.drawable.ic_action_picture_file);
        mToolbar.setNavigationIcon(R.drawable.ic_action_back);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().onBackPressed();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.reset(this);
    }
}
