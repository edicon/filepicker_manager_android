package io.filepicker.manager.adapters;

import android.content.Context;
import android.database.Cursor;
import android.provider.BaseColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import butterknife.ButterKnife;
import butterknife.InjectView;
import io.filepicker.manager.R;
import io.filepicker.manager.data.ManagerContract;
import io.filepicker.manager.utils.Utils;

/**
 * Created by maciejwitowski on 11/12/14.
 */
public class FoldersAdapter  extends CursorAdapter {

    public FoldersAdapter(Context context, Cursor cursor, int flags){
        super(context, cursor, flags);
    }


    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = LayoutInflater.from(context).inflate(R.layout.main_list_item, parent, false);

        ViewHolder viewHolder = new ViewHolder(view);
        view.setTag(viewHolder);

        return view;
    }

    @Override
    public void bindView(View view, final Context context, final Cursor cursor) {
        ViewHolder viewHolder = (ViewHolder) view.getTag();

        final String name = cursor.getString(FoldersListQuery.NAME);

        final String createdAt = Utils.getReadableTimestamp(
                cursor.getString(FoldersListQuery.CREATED_AT));

        viewHolder.name.setText(name);
        viewHolder.info.setText("Created " + createdAt);
        viewHolder.btnThumbnail.setImageResource(R.drawable.ic_action_folder);
    }


    public static class ViewHolder {
        @InjectView(R.id.tvName)
        TextView name;

        @InjectView(R.id.imageThumbnail)
        ImageButton btnThumbnail;

        @InjectView(R.id.tvInfo)
        TextView info;

        public ViewHolder(View view) {
            ButterKnife.inject(this, view);
        }
    }

    public static class FoldersListQuery {
        private FoldersListQuery(){}

        public static final String[] PROJECTION = {
                BaseColumns._ID,
                ManagerContract.Folder.COLUMN_NAME,
                ManagerContract.Folder.COLUMN_CREATED_AT
        };

        final static int ID = 0;
        final static int NAME = 1;
        final static int CREATED_AT = 2;
    }
}
