package io.filepicker.manager.adapters;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.HashSet;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.greenrobot.event.EventBus;
import io.filepicker.manager.R;
import io.filepicker.manager.data.FileUtils;
import io.filepicker.manager.events.FileInfoShown;
import io.filepicker.manager.events.FileThumbnailClickedEvent;
import io.filepicker.manager.models.File;
import io.filepicker.manager.utils.Utils;

/**
 * Created by maciejwitowski on 11/4/14.
 */
public class FilesAdapter extends CursorAdapter {

    private Context context;

    private HashSet<Integer> pickedFiles;

    private boolean mItemsEnabled = true;
    private boolean mInfoBtnEnabled = true;
    private boolean mMultipleSelectionEnabled = true;


    public interface Contract {
        public void showFileInfo(File file);
    }

    public FilesAdapter(Context context, Cursor cursor, int flags, HashSet<Integer> pickedFiles){
        super(context, cursor, flags);
        this.context = context;
        this.pickedFiles = pickedFiles;
    }

    public void markItemsEnabled(boolean enabled) {
        mItemsEnabled = enabled;
    }

    public void setInfoBtnEnabled(boolean enabled) {
        mInfoBtnEnabled = enabled;
    }

    public void setMultipleSelectionEnabled(boolean enabled) {
        mMultipleSelectionEnabled = enabled;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = LayoutInflater.from(context).inflate(R.layout.main_list_item, parent, false);

        ViewHolder viewHolder = new ViewHolder(view);
        view.setTag(viewHolder);

        return view;
    }

    @Override
    public void bindView(final View view, final Context context, final Cursor cursor) {
        final ViewHolder viewHolder = (ViewHolder) view.getTag();

        final File file = FileUtils.getById(context, cursor.getLong(FileUtils.COLUMN_ID)).get();
        final int position = cursor.getPosition();

        viewHolder.name.setText(Utils.getShortName(file.filename));
        if (isItemSelected(position))
            setSelectedView(view, viewHolder.btnThumbnail);
        else
            setUnselectedView(view, viewHolder.btnThumbnail, file);

        final String info = "Created " + Utils.getReadableTimestamp(file.createdAt);
        viewHolder.info.setText(info);

        if (mInfoBtnEnabled) {
            viewHolder.btnInfo.setVisibility(View.VISIBLE);
            // Info button onClick
            viewHolder.btnInfo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    EventBus.getDefault().post(new FileInfoShown());
                    getContract().showFileInfo(file);
                }
            });
        }

        if (mMultipleSelectionEnabled){
            // File thumbnail on click
            viewHolder.btnThumbnail.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Unselect if is currently selected
                    if (isItemSelected(position)) {
                        setUnselectedView(view, viewHolder.btnThumbnail, file);
                    } else {
                        setSelectedView(view, viewHolder.btnThumbnail);
                    }
                    EventBus.getDefault().post(new FileThumbnailClickedEvent(position));
                }
            });
        }

        if(!mItemsEnabled){
            view.setEnabled(false);
            view.setAlpha(0.4f);
        }
    }



    private boolean isItemSelected(int position) {
        return pickedFiles != null && pickedFiles.contains(position);
    }

    private void setSelectedView(View view, ImageButton button) {
        button.setImageResource(R.drawable.ic_action_check_circle);
        view.setBackgroundColor(context.getResources().getColor(R.color.light_gray));
    }

    private void setUnselectedView(View view, ImageButton button, File file) {
        view.setBackgroundColor(context.getResources().getColor(R.color.white));
        int mFileIcon = R.drawable.ic_action_file;

        if (Utils.isImage(file.type)){
            Picasso.with(context)
                    .load(FileUtils.getFileDownloadUrl(context, file))
                    .resize(112, 112)
                    .centerCrop()
                    .placeholder(mFileIcon)
                    .into(button);
        } else {
            button.setImageResource(mFileIcon);
        }
    }

    static class ViewHolder {
        @InjectView(R.id.tvName) TextView name;

        @InjectView(R.id.imageThumbnail)
        ImageButton btnThumbnail;

        @InjectView(R.id.tvInfo)
        TextView info;

        @InjectView(R.id.img_btn_info)
        ImageButton btnInfo;

        public ViewHolder(View view) {
            ButterKnife.inject(this, view);
        }
    }

    private Contract getContract() {
        return (Contract) context;
    }
}
