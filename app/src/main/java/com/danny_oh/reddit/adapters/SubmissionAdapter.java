package com.danny_oh.reddit.adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.text.format.Time;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.danny_oh.reddit.R;
import com.danny_oh.reddit.SessionManager;
import com.danny_oh.reddit.util.ImageViewWithVoteState;
import com.danny_oh.reddit.util.PagedSubmissionsList;
import com.github.jreddit.entity.Submission;
import com.github.jreddit.entity.Subreddit;
import com.squareup.picasso.Picasso;

import java.util.List;

/**
 * Created by danny on 7/21/14.
 */
public class SubmissionAdapter extends BaseAdapter {
    private Context mContext;

    private PagedSubmissionsList mPagedSubmissions;
    private OnSubmissionAdapterInteractionListener mListener;

    public interface OnSubmissionAdapterInteractionListener {
        public void onCommentsClick(Submission submission);
    }

    /*
     * Holder for the View that represents the list item in the list
     * The view holder allows obtaining a contained item without using findViewById
     */
    static class ViewHolder {
        private ImageViewWithVoteState upvote;
        private ImageViewWithVoteState downvote;
        private TextView score;
        private ImageView thumbnail;
        private TextView title;
        private TextView subtitle;
        private TextView numComments;
    }

//    public SubmissionAdapter(Context context, List<Submission> submissions) {
//        mContext = context;
//        mSubmissions = submissions;
//    }

    public SubmissionAdapter(Context context, PagedSubmissionsList pagedSubmissions) {
        mContext = context;
        mPagedSubmissions = pagedSubmissions;
    }

    public void setOnSubmissionAdapterInteractionListener(OnSubmissionAdapterInteractionListener listener) {
        mListener = listener;
    }

    @Override
    public int getCount() {
        return mPagedSubmissions.count();
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public Object getItem(int i) {
        return mPagedSubmissions.getSubmissionAtIndex(i);
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View view = convertView;
        final ViewHolder viewHolder;
        if (view == null) {
            view = LayoutInflater.from(mContext).inflate(R.layout.list_item_submission, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.upvote = (ImageViewWithVoteState)view.findViewById(R.id.submission_up_vote);
            viewHolder.downvote = (ImageViewWithVoteState)view.findViewById(R.id.submission_down_vote);
            viewHolder.score = (TextView)view.findViewById(R.id.submission_score);
            viewHolder.thumbnail = (ImageView)view.findViewById(R.id.submission_thumbnail);
            viewHolder.title = (TextView)view.findViewById(R.id.submission_title);
            viewHolder.subtitle = (TextView)view.findViewById(R.id.submission_subtitle);
            viewHolder.numComments = (TextView)view.findViewById(R.id.submission_num_comments);

            view.setTag(viewHolder);
        } else {
             viewHolder = (ViewHolder) view.getTag();
        }

        final Submission submission = (Submission)getItem(position);

        if (submission != null) {
            viewHolder.score.setText(submission.getScore().toString());
            viewHolder.title.setText(submission.getTitle().toString());

            Time time = new Time("UTC");
            long timeNow = System.currentTimeMillis() / 1000;
            long timePosted = submission.getCreatedUTC();
            int hoursElapsed = (int)((timeNow - timePosted) / 60 / 60);

            String timeElapsed = (hoursElapsed > 0) ? hoursElapsed + " hrs ago" : ((timeNow - timePosted) / 60) + " mins ago";

            viewHolder.subtitle.setText(timeElapsed + " | " + submission.getAuthor() + " | " + submission.getSubreddit() + " | " + submission.getDomain());


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

            if (mListener != null) {
                viewHolder.numComments.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mListener.onCommentsClick(mPagedSubmissions.getSubmissionAtIndex(position));
                    }
                });
            }


//            /*

            if (submission.isLiked() == null) {

                viewHolder.upvote.setStateVoted(false);
                viewHolder.downvote.setStateVoted(false);
            } else {
                if (submission.isLiked()) {
                    viewHolder.upvote.setStateVoted(true);
                    viewHolder.downvote.setStateVoted(false);
                } else {
                    viewHolder.upvote.setStateVoted(false);
                    viewHolder.downvote.setStateVoted(true);
                }
            }
//            */


            viewHolder.upvote.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View view) {
                    final int direction;

                    if (submission.isLiked() == null || !submission.isLiked()) {
                        direction = 1;
                    } else {
                        direction = 0;
                    }

                    SessionManager.getInstance(mContext).vote(mPagedSubmissions.getSubmissionAtIndex(position).getFullName(), direction, new SessionManager.SessionListener<Boolean>() {
                        @Override
                        public void onResponse(Boolean object) {
                            // if vote was successful
                            if (object) {
                                submission.setScore(submission.getScore() + (direction == 1 ? 1 : -1));
                                submission.setLiked(direction);
                                notifyDataSetChanged();
                            } else {
                                Toast.makeText(mContext, "Failed to vote. Please try again later.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            });


            viewHolder.downvote.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final int direction;

                    if (submission.isLiked() == null || submission.isLiked()) {
                        direction = -1;
                    } else {
                        direction = 0;
                    }

                    SessionManager.getInstance(mContext).vote(mPagedSubmissions.getSubmissionAtIndex(position).getFullName(), direction, new SessionManager.SessionListener<Boolean>() {
                        @Override
                        public void onResponse(Boolean object) {
                            // if vote was successful
                            if (object) {
                                submission.setScore(submission.getScore() + (direction == -1 ? -1 : 1));
                                submission.setLiked(direction);
                                notifyDataSetChanged();
                            } else {
                                Toast.makeText(mContext, "Failed to vote. Please try again later.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            });
        }

        return view;
    }

}
