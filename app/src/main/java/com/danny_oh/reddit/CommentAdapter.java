package com.danny_oh.reddit;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.jreddit.entity.Comment;

import java.util.List;

/**
 * Created by danny on 7/21/14.
 */
public class CommentAdapter extends BaseAdapter {

    static class ViewHolder {
        private TextView username;
        private TextView score;
        private TextView timeCreated;
        private TextView body;

        private ImageView upvote;
        private ImageView downvote;

        private View depthIndicator;
    }

    private Context mContext;
    private List<Comment> mComments;

    public CommentAdapter(Context context, List<Comment> comments) {
        mContext = context;
        mComments = comments;
    }

    @Override
    public int getCount() {
        return mComments.size();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;

        if (view == null) {
            ViewHolder viewHolder = new ViewHolder();
            view = LayoutInflater.from(mContext).inflate(R.layout.list_item_comment, parent, false);

            viewHolder.username = (TextView)view.findViewById(R.id.comment_username);
            viewHolder.score = (TextView)view.findViewById(R.id.comment_score);
            viewHolder.timeCreated = (TextView)view.findViewById(R.id.comment_time_created);
            viewHolder.body = (TextView)view.findViewById(R.id.comment_body);
            viewHolder.upvote = (ImageView)view.findViewById(R.id.comment_up_vote);
            viewHolder.downvote = (ImageView)view.findViewById(R.id.comment_down_vote);
            viewHolder.depthIndicator = (View)view.findViewById(R.id.comment_depth_indicator);

            view.setTag(viewHolder);
        }

        // get comment at position
        Comment comment = mComments.get(position);

        if (comment != null) {
            // time since comment posted calculation
            long timeNow = System.currentTimeMillis() / 1000;
            long timePosted = comment.getCreatedUTC();
            int hoursElapsed = (int)((timeNow - timePosted) / 60 / 60);

            ViewHolder viewHolder = (ViewHolder)view.getTag();

            viewHolder.username.setText(comment.getAuthor());
            viewHolder.score.setText(comment.getScore().toString());
            viewHolder.timeCreated.setText(hoursElapsed + " hrs ago");
            viewHolder.body.setText(comment.getBody());

            viewHolder.depthIndicator.setBackgroundColor(Color.argb(255, (int)(255 * Math.random()), (int)(255 * Math.random()), (int)(255 * Math.random())));

            // TODO: onClickListener for upvote and downvote
        }

        return view;
    }

    @Override
    public Object getItem(int i) {
        return mComments.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }
}
