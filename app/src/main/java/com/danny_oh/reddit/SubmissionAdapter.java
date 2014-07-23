package com.danny_oh.reddit;

import android.content.Context;
import android.text.format.Time;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.jreddit.entity.Submission;
import com.github.jreddit.entity.Subreddit;
import com.squareup.picasso.Picasso;

import java.util.List;

/**
 * Created by danny on 7/21/14.
 */
public class SubmissionAdapter extends BaseAdapter {
    private Context mContext;
    private List<Submission> mSubmissions;

    static class ViewHolder {
        private ImageView upvote;
        private ImageView downvote;
        private TextView score;
        private ImageView thumbnail;
        private TextView title;
        private TextView subtitle;
        private TextView numComments;
    }

    public SubmissionAdapter(Context context, List<Submission> submissions) {
        mContext = context;
        mSubmissions = submissions;
    }

    @Override
    public int getCount() {
        return mSubmissions.size();
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public Object getItem(int i) {
        return mSubmissions.get(i);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = LayoutInflater.from(mContext).inflate(R.layout.list_item_submission, parent, false);
            ViewHolder viewHolder = new ViewHolder();
            viewHolder.upvote = (ImageView)view.findViewById(R.id.submission_up_vote);
            viewHolder.downvote = (ImageView)view.findViewById(R.id.submission_down_vote);
            viewHolder.score = (TextView)view.findViewById(R.id.submission_score);
            viewHolder.thumbnail = (ImageView)view.findViewById(R.id.submission_thumbnail);
            viewHolder.title = (TextView)view.findViewById(R.id.submission_title);
            viewHolder.subtitle = (TextView)view.findViewById(R.id.submission_subtitle);
            viewHolder.numComments = (TextView)view.findViewById(R.id.submission_num_comments);
            view.setTag(viewHolder);
        }

        Submission submission = mSubmissions.get(position);

        if (submission != null) {
            ViewHolder viewHolder = (ViewHolder) view.getTag();
            viewHolder.score.setText(submission.getScore().toString());
            viewHolder.title.setText(submission.getTitle().toString());

            Time time = new Time("UTC");
            long timeNow = System.currentTimeMillis() / 1000;
            long created = submission.getCreatedUTC();
            int hoursSinceCreation = (int)((timeNow - created) / 60 / 60);

            viewHolder.subtitle.setText(hoursSinceCreation + " hrs ago" + " | " + submission.getAuthor() + " | " + submission.getSubreddit() + " | " + submission.getDomain());

            if (!submission.getThumbnail().equals("") && !submission.getThumbnail().equals("self")) {
                viewHolder.thumbnail.setVisibility(View.VISIBLE);
                Picasso.with(mContext)
                        .load(submission.getThumbnail())
                        .placeholder(R.drawable.reddit_red)
                        .into(viewHolder.thumbnail);
            } else {
                viewHolder.thumbnail.setVisibility(View.GONE);
            }

            viewHolder.numComments.setText(submission.getCommentCount().toString() + " comments");

//            viewHolder.upvote.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View view) {
//                    View parent = (View)view.getParent();
//
//                }
//            });
        }

        return view;
    }

}
