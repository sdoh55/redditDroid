package com.danny_oh.reddit.adapters;

import android.content.Context;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.format.Time;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.danny_oh.reddit.R;
import com.danny_oh.reddit.SessionManager;
import com.danny_oh.reddit.retrieval.AsyncMarkActions;
import com.danny_oh.reddit.util.ImageViewWithVoteState;
import com.danny_oh.reddit.util.PagedSubmissionsList;
import com.github.jreddit.entity.Submission;
import com.squareup.picasso.Picasso;

/**
 * Created by danny on 7/21/14.
 */
public class SubmissionAdapter extends BaseAdapter {
    private Context mContext;

    private PagedSubmissionsList mPagedSubmissions;
    private OnSubmissionAdapterInteractionListener mListener;

    private int mSubmissionTitleColor;
    private int mVisitedSubmissionTitleColor;


    private int mTimeElapsedFontColor;
    private int mAuthorFontColor;


    public interface OnSubmissionAdapterInteractionListener {
        public void onCommentsClick(Submission submission);
        public void onSubmissionClick(Submission submission, int position);
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
        private LinearLayout titleContainer;
        private ImageViewWithVoteState saved;
    }

//    public SubmissionAdapter(Context context, List<Submission> submissions) {
//        mContext = context;
//        mSubmissions = submissions;
//    }

    public SubmissionAdapter(Context context, PagedSubmissionsList pagedSubmissions) {
        try {
            mListener = (OnSubmissionAdapterInteractionListener)context;
        } catch (ClassCastException ce) {
            ce.printStackTrace();
            throw new ClassCastException("Parent activity of SubmissionAdapter must implement OnSubmissionAdapterInteractionListener interface.");
        }

        mContext = context;
        mPagedSubmissions = pagedSubmissions;

        mSubmissionTitleColor = mContext.getResources().getColor(R.color.submission_title_font_color);
        mVisitedSubmissionTitleColor = mContext.getResources().getColor(R.color.submission_visited_title_font_color);
        mAuthorFontColor = mContext.getResources().getColor(R.color.submission_author_font_color);
        mTimeElapsedFontColor = mContext.getResources().getColor(R.color.submission_time_elapsed_font_color);
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
            viewHolder.titleContainer = (LinearLayout)view.findViewById(R.id.submission_title_container);
            viewHolder.saved = (ImageViewWithVoteState)view.findViewById(R.id.submission_saved);

            view.setTag(viewHolder);
        } else {
             viewHolder = (ViewHolder) view.getTag();
        }

        final Submission submission = (Submission)getItem(position);

        if (submission != null) {
            viewHolder.score.setText(submission.getScore().toString());
            viewHolder.title.setText(submission.getTitle().toString());

            if (submission.isVisited()) {
                viewHolder.title.setTextColor(mVisitedSubmissionTitleColor);
            } else {
                viewHolder.title.setTextColor(mSubmissionTitleColor);
            }

            Time time = new Time("UTC");
            long timeNow = System.currentTimeMillis() / 1000;
            long timePosted = submission.getCreatedUTC();
            int hoursElapsed = (int)((timeNow - timePosted) / 60 / 60);

            String timeElapsed = (hoursElapsed > 0) ? hoursElapsed + " hrs ago" : ((timeNow - timePosted) / 60) + " mins ago";

            viewHolder.subtitle.setText(buildSubtitle(timeElapsed, submission.getAuthor(), submission.getSubreddit(), submission.getDomain()));





            // TODO: thumbnail types/strings to consider
            // - "nsfw"
            // - "default"
            // - sometimes thumbnails are embeded in { media } (extend Submission to save media)
            if (!submission.getThumbnail().equals("") && !submission.isSelf()) {
                viewHolder.thumbnail.setVisibility(View.VISIBLE);
                Picasso.with(mContext)
                        .load(submission.getThumbnail())
                        .placeholder(R.drawable.redditandroid_head)
                        .into(viewHolder.thumbnail);
            } else {
                viewHolder.thumbnail.setVisibility(View.GONE);
            }

            viewHolder.numComments.setText(submission.getCommentCount().toString() + " comments");

            if (mListener != null) {
                viewHolder.titleContainer.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mListener.onSubmissionClick(submission, position);
                    }
                });
                viewHolder.numComments.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mListener.onCommentsClick(submission);
                    }
                });
            }


            if (submission.isSaved()) {
                viewHolder.saved.setStateVoted(true);
            } else {
                viewHolder.saved.setStateVoted(false);
            }

            viewHolder.saved.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!submission.isSaved()) {
                        SessionManager.getInstance(mContext).saveThing(submission.getFullName(),
                                new AsyncMarkActions.MarkActionsResponseHandler(){

                                @Override
                                public void onSuccess(boolean actionSuccessful) {
                                    if (actionSuccessful) {
                                        submission.setSaved(true);
                                        notifyDataSetChanged();
                                        Toast.makeText(mContext, "Saved", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(mContext, "Failed to save. Please try again later.", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });

                    } else {
                        SessionManager.getInstance(mContext).unsaveThing(submission.getFullName(),
                                new AsyncMarkActions.MarkActionsResponseHandler(){

                                    @Override
                                    public void onSuccess(boolean actionSuccessful) {
                                        if (actionSuccessful) {
                                            submission.setSaved(false);
                                            notifyDataSetChanged();
                                            Toast.makeText(mContext, "Unsaved", Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(mContext, "Failed to unsave. Please try again later.", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                    }
                }
            });


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
                                if (submission.isLiked() == null) {
                                    submission.setScore(submission.getScore() + 1);
                                } else {
                                    submission.setScore(submission.getScore() + (submission.isLiked() ? -1 : 2));
                                }
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
                                if (submission.isLiked() == null) {
                                    submission.setScore(submission.getScore() - 1);
                                } else {
                                    submission.setScore(submission.getScore() - (submission.isLiked() ? 2 : -1));
                                }
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

    private Spannable buildSubtitle(String timeElapsed, String author, String subreddit, String domain) {
        Spannable subtitleSpannable = new SpannableString(timeElapsed + " " + author + " " + subreddit + " " + domain);

        // set time elapsed font color
        subtitleSpannable.setSpan(new ForegroundColorSpan(mTimeElapsedFontColor),
                0,
                timeElapsed.length(),
                Spanned.SPAN_INCLUSIVE_INCLUSIVE);

        // set time elapsed to italic
        subtitleSpannable.setSpan(new StyleSpan(Typeface.ITALIC),
                0,
                timeElapsed.length(),
                Spanned.SPAN_INCLUSIVE_INCLUSIVE);

        // set the author text color
        subtitleSpannable.setSpan(new ForegroundColorSpan(mAuthorFontColor),
                timeElapsed.length() + 1,
                timeElapsed.length() + 1 + author.length(),
                Spanned.SPAN_INCLUSIVE_INCLUSIVE);

        // set the subreddit text color
        subtitleSpannable.setSpan(new StyleSpan(Typeface.BOLD),
                timeElapsed.length() + 1 + author.length() + 1,
                timeElapsed.length() + 1 + author.length() + 1 + subreddit.length(),
                Spanned.SPAN_INCLUSIVE_INCLUSIVE);

        // set the domain text size
        subtitleSpannable.setSpan(new RelativeSizeSpan(0.7f),
                timeElapsed.length() + 1 + author.length() + 1 + subreddit.length() + 1,
                subtitleSpannable.length(),
                Spanned.SPAN_INCLUSIVE_INCLUSIVE);

        return subtitleSpannable;
    }

}
