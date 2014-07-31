package com.danny_oh.reddit.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.danny_oh.reddit.R;
import com.danny_oh.reddit.util.CommentsListHelper;
import com.github.jreddit.entity.Comment;
import com.github.jreddit.entity.Submission;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by danny on 7/23/14.
 */
public class CommentMapAdapter extends BaseAdapter {

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
    private HashMap<Integer, CommentsListHelper.CommentContainer> mComments;

    private Submission mSubmission;

    private List<Integer> mDepthColors;

    public CommentMapAdapter(Context context, HashMap<Integer, CommentsListHelper.CommentContainer> comments, Submission submission) {
        mContext = context;
        mComments = comments;
        mSubmission = submission;

        mDepthColors = new ArrayList<Integer>();
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
        Comment comment = mComments.get(position).comment;
        int depth = mComments.get(position).depth;

        if (mDepthColors.size() < depth + 1) {
            mDepthColors.add(Color.argb(255, (int)(255 * Math.random()), (int)(255 * Math.random()), (int)(255 * Math.random())));
        }

        if (comment != null) {
            // time since comment posted calculation
            long timeNow = System.currentTimeMillis() / 1000;
            long timePosted = comment.getCreatedUTC();
            int hoursElapsed = (int)((timeNow - timePosted) / 60 / 60);
            String timeElapsed = (hoursElapsed > 0) ? hoursElapsed + " hrs ago" : ((timeNow - timePosted) / 60) + " mins ago";

            ViewHolder viewHolder = (ViewHolder)view.getTag();

            viewHolder.username.setText(comment.getAuthor());
            viewHolder.score.setText(comment.getScore().toString() + " points");
            viewHolder.timeCreated.setText(timeElapsed);
            viewHolder.body.setText(comment.getBody());

            viewHolder.depthIndicator.setBackgroundColor(mDepthColors.get(depth));
            ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams)viewHolder.depthIndicator.getLayoutParams();
            layoutParams.setMargins(depth * 10, layoutParams.topMargin, layoutParams.rightMargin, layoutParams.bottomMargin);

            if (comment.getAuthor().equals(mSubmission.getAuthor())) {
                viewHolder.username.setTextColor(mContext.getResources().getColor(R.color.comment_author_font_color));
            }

            // TODO: onClickListener for upvote and downvote
        }

        return view;
    }

    @Override
    public Object getItem(int i) {
        return mComments.get(i).comment;
    }

    @Override
    public long getItemId(int i) {
        return i;
    }
}
