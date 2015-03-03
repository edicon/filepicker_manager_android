package io.filepicker.manager.fragments;

import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import java.util.ArrayList;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import io.filepicker.manager.R;
import io.filepicker.manager.data.ManagerContract;
import io.filepicker.manager.utils.Utils;

/**
 * Created by maciejwitowski on 11/12/14.
 */
public class NewFolderDialogFragment extends DialogFragment {


    private static final String KEY_PARENT_FOLDER_ID = "parent_folder_id";
    private long parentFolderId;

    public interface Contract {
        public void createNewFolder(String name, long parentFolderId);
    }

    @InjectView(R.id.etName) EditText mEtName;
    @InjectView(R.id.btnOk) Button mBtnOk;

    public static NewFolderDialogFragment newInstance(long parentFolderId) {
        NewFolderDialogFragment frag = new NewFolderDialogFragment();
        Bundle args = new Bundle();
        args.putLong(KEY_PARENT_FOLDER_ID, parentFolderId);
        frag.setArguments(args);

        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(android.app.DialogFragment.STYLE_NO_TITLE, 0);

        parentFolderId = getArguments().getLong(KEY_PARENT_FOLDER_ID);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){

        View view = inflater.inflate(R.layout.new_folder_dialog, container, false);
        ButterKnife.inject(this, view);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getDialog().setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                InputMethodManager imm = (InputMethodManager)
                        getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);

                imm.showSoftInput(mEtName, InputMethodManager.SHOW_IMPLICIT);
            }
        });

        mEtName.addTextChangedListener(new AllowedFolderNameWatcher());
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.reset(this);
    }

    @OnClick(R.id.btnCancel)
    public void hideDialog() {
        dismiss();
    }

    @OnClick(R.id.btnOk)
    public void addFolder() {
        String name = mEtName.getText().toString();

        if (name.length() > 0){
            getContract().createNewFolder(name, parentFolderId);
            dismiss();
        }
    }

    private Contract getContract() {
        return (Contract) getActivity();
    }

    // Class listening if the new folder name is allowed
    private class AllowedFolderNameWatcher implements TextWatcher {

        ArrayList<String> siblingsNames = null;

        @Override
        public void afterTextChanged(Editable text) {
            if(text.length() == 0 || getSiblingsNames().contains(text.toString())) {
                Utils.disableButton(getActivity(), mBtnOk);
            } else {
                Utils.enableButton(getActivity(), mBtnOk);
            }
        }

        // Not used
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        // Not used
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        private ArrayList<String> getSiblingsNames() {
            if(siblingsNames == null) {
                siblingsNames = new ArrayList<>();
                Cursor cursor = getActivity().getContentResolver()
                        .query(ManagerContract.Folder.CONTENT_URI,
                                new String[]{ManagerContract.Folder.COLUMN_NAME},
                                ManagerContract.Folder.COLUMN_PARENT_ID + " = ?",
                                new String[]{String.valueOf(parentFolderId)}, null);

                if(cursor.moveToFirst()) {
                    do {
                        siblingsNames.add(cursor.getString(cursor.getColumnIndex(ManagerContract.Folder.COLUMN_NAME)));
                    } while (cursor.moveToNext());
                }
            }

            return siblingsNames;
        }
    }
}
