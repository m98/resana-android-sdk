package io.resana.player;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Request;
import com.squareup.picasso.RequestHandler;

import java.io.File;
import java.io.IOException;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import io.resana.player.FileManager.Dir;
import io.resana.player.FileManager.Media;
import jp.wasabeef.picasso.transformations.RoundedCornersTransformation;

public class AdapterExpandbleList extends BaseExpandableListAdapter {
    private Dir dir;
    private List<Dir> subDirs;
    private List<Media> medias;
    private Context appContext;
    private LayoutInflater inflater;
    Picasso picasso;
    RoundedCornersTransformation transformation;

    enum Group {
        Folders, Videos
    }

    public AdapterExpandbleList(Activity activity) {
        this.appContext = activity.getApplicationContext();
        inflater = (LayoutInflater) appContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        ThumbnailRequestHandler rh = new ThumbnailRequestHandler();
        picasso = new Picasso.Builder(appContext)
                .addRequestHandler(rh)
                .build();
        transformation = new RoundedCornersTransformation(ImageHelper.dpToPx(appContext, 6), 0);
    }


    public void setData(Dir dir) {
        this.dir = dir;
        this.subDirs = dir.subDirs;
        this.medias = dir.medias;
    }

    @Override
    public int getGroupCount() {
        int res = 0;
        if (dir != null) {
            res += medias.size() > 0 ? 1 : 0;
            res += subDirs.size() > 0 ? 1 : 0;
        }
        return res;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        Group group = (Group) getGroup(groupPosition);
        return (group == Group.Folders ? subDirs : medias).size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        int groupCount = getGroupCount();
        if (groupCount == 2) {
            if (groupPosition == 0)
                return Group.Folders;
            else
                return Group.Videos;
        } else if (groupCount == 1) {
            if (subDirs.size() > 0)
                return Group.Folders;
            else
                return Group.Videos;
        } else {
            //will not come here
            return null;
        }
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        Group group = (Group) getGroup(groupPosition);
        return (group == Group.Folders ? subDirs : medias).get(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        Group group = (Group) getGroup(groupPosition);
        return groupPosition * (group == Group.Folders ? subDirs : medias).size() + childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.adapter_expandble_list_group_item, null);
            GroupViewHolder vh = new GroupViewHolder(convertView);
            convertView.setTag(vh);
        }
        GroupViewHolder viewHolder = (GroupViewHolder) convertView.getTag();
        viewHolder.textView.setText("" + getGroup(groupPosition));
        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        Group group = (Group) getGroup(groupPosition);
        if (group.equals(Group.Folders))
            return getFolderChildView(childPosition, isLastChild, convertView, parent);
        else
            return getMediaChildView(childPosition, isLastChild, convertView, parent);
    }

    private View getFolderChildView(int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.adapter_expandble_list_child_item, null);
            ChildViewHolder vh = new ChildViewHolder(convertView);
            convertView.setTag(vh);
        }
        ChildViewHolder viewHolder = (ChildViewHolder) convertView.getTag();
        Dir dir = subDirs.get(childPosition);
        viewHolder.textView.setText(dir.name);
        viewHolder.mediaIcon.setVisibility(View.GONE);
        viewHolder.folderIcon.setVisibility(View.VISIBLE);
        viewHolder.folderState.setVisibility(View.VISIBLE);
        viewHolder.numOfSubfolders.setText("(" + dir.subDirs.size() + ")");
        viewHolder.numOfMedias.setText("(" + dir.medias.size() + ")");
        return convertView;
    }

    private View getMediaChildView(int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        final Media media = medias.get(childPosition);
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.adapter_expandble_list_child_item, null);
            ChildViewHolder vh = new ChildViewHolder(convertView);
            convertView.setTag(vh);
        }
        final ChildViewHolder viewHolder = (ChildViewHolder) convertView.getTag();
        viewHolder.textView.setText(media.name);
        viewHolder.mediaIcon.setVisibility(View.VISIBLE);
        viewHolder.folderIcon.setVisibility(View.GONE);
        viewHolder.folderState.setVisibility(View.GONE);
        picasso.load(Uri.fromFile(new File(media.getPath())))
                .fit()
                .centerCrop()
                .transform(transformation)
                .placeholder(R.drawable.video)
                .into(viewHolder.mediaIcon);
        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    class GroupViewHolder {

        @Bind(R.id.textView)
        TextView textView;

        public GroupViewHolder(View v) {
            ButterKnife.bind(this, v);
        }
    }

    class ChildViewHolder {
        @Bind(R.id.textView)
        TextView textView;
        @Bind(R.id.folder_icon)
        ImageView folderIcon;
        @Bind(R.id.media_icon)
        ImageView mediaIcon;
        @Bind(R.id.folderState)
        View folderState;
        @Bind(R.id.numOfSubfolders)
        TextView numOfSubfolders;
        @Bind(R.id.numOfMedias)
        TextView numOfMedias;

        public ChildViewHolder(View v) {
            ButterKnife.bind(this, v);
        }
    }

    class ThumbnailRequestHandler extends RequestHandler {
        int roundInPx;

        public ThumbnailRequestHandler() {
            roundInPx = ImageHelper.dpToPx(appContext, 5);
        }

        @Override
        public boolean canHandleRequest(Request data) {
            return true;
        }

        @Override
        public Result load(Request request, int networkPolicy) throws IOException {
            Bitmap bm = ThumbnailUtils.createVideoThumbnail(request.uri.getPath(), MediaStore.Images.Thumbnails.MINI_KIND);
//            Bitmap roundedBm = ImageHelper.getRoundedCornerBitmap(bm, roundInPx);
            return new Result(bm, Picasso.LoadedFrom.DISK);

        }
    }

}
