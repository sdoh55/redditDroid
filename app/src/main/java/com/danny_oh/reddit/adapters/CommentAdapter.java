package com.danny_oh.reddit.adapters;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.danny_oh.reddit.R;
import com.danny_oh.reddit.SessionManager;
import com.danny_oh.reddit.retrieval.AsyncComments;
import com.danny_oh.reddit.retrieval.AsyncMarkActions;
import com.danny_oh.reddit.util.ExtendedComment;
import com.danny_oh.reddit.util.ImageViewWithVoteState;
import com.github.jreddit.entity.Submission;
import com.github.jreddit.retrieval.params.CommentSort;

import java.util.ArrayList;
import java.util.List;

import in.uncod.android.bypass.Bypass;

/**
 * Created by danny on 7/31/14.
 */
public class CommentAdapter extends BaseAdapter {

    static class ViewHolder {
        private RelativeLayout commentContainer;
        private TextView username;
        private TextView score;
        private TextView timeCreated;
        private TextView body;

        private ImageViewWithVoteState upvote;
        private ImageViewWithVoteState downvote;

        private View depthIndicator;

        private ImageViewWithVoteState gilded;

        private RelativeLayout loadMoreOverlay;
    }

    private Context mContext;
    private List<ExtendedComment> mComments;

    private Submission mSubmission;

    private List<Integer> mDepthColors;

    private Bypass mBypass;

    public CommentAdapter(Context context, List<ExtendedComment> comments, Submission submission) {
        mContext = context;
        mComments = comments;
        mSubmission = submission;

        mDepthColors = new ArrayList<Integer>();
        mBypass = new Bypass();

        TypedArray typedArray = context.getResources().obtainTypedArray(R.array.comment_depth_colors);
        for (int i = 0; i < typedArray.length(); i++) {
            mDepthColors.add(typedArray.getColor(i, 0));
        }
        typedArray.recycle();
    }

    @Override
    public int getCount() {
        return mComments.size();
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View view = convertView;

        if (view == null) {
            ViewHolder viewHolder = new ViewHolder();
            view = LayoutInflater.from(mContext).inflate(R.layout.list_item_comment, parent, false);

            viewHolder.commentContainer = (RelativeLayout)view.findViewById(R.id.comment_container);
            viewHolder.username = (TextView)view.findViewById(R.id.comment_username);
            viewHolder.score = (TextView)view.findViewById(R.id.comment_score);
            viewHolder.timeCreated = (TextView)view.findViewById(R.id.comment_time_created);
            viewHolder.body = (TextView)view.findViewById(R.id.comment_body);
            viewHolder.upvote = (ImageViewWithVoteState)view.findViewById(R.id.comment_up_vote);
            viewHolder.downvote = (ImageViewWithVoteState)view.findViewById(R.id.comment_down_vote);
            viewHolder.depthIndicator = (View)view.findViewById(R.id.comment_depth_indicator);
            viewHolder.gilded = (ImageViewWithVoteState)view.findViewById(R.id.comment_gilded);
            viewHolder.loadMoreOverlay = (RelativeLayout)view.findViewById(R.id.load_more_overlay);

            view.setTag(viewHolder);
        }

        // get comment at position
        final ExtendedComment comment = mComments.get(position);
        int depth = mComments.get(position).getDepth();

        if (mDepthColors.size() < depth + 1) {
            mDepthColors.add(Color.argb(255, (int)(255 * Math.random()), (int)(255 * Math.random()), (int)(255 * Math.random())));
        }

        if (comment != null) {
            if (!comment.isFault()) {
                // time since comment posted calculation
                long timeNow = System.currentTimeMillis() / 1000;
                long timePosted = comment.getCreatedUTC();
                int hoursElapsed = (int) ((timeNow - timePosted) / 60 / 60);
                String timeElapsed = (hoursElapsed > 0) ? hoursElapsed + " hrs ago" : ((timeNow - timePosted) / 60) + " mins ago";

                ViewHolder viewHolder = (ViewHolder) view.getTag();

                viewHolder.commentContainer.setVisibility(View.VISIBLE);
                viewHolder.loadMoreOverlay.setVisibility(View.GONE);

                viewHolder.username.setText(comment.getAuthor());
                viewHolder.score.setText(comment.getScore().toString() + " points");
                viewHolder.timeCreated.setText(timeElapsed);

                viewHolder.body.setText(mBypass.markdownToSpannable(comment.getBody()));
//            viewHolder.body.setMovementMethod(LinkMovementMethod.getInstance());

                viewHolder.depthIndicator.setBackgroundColor(mDepthColors.get(depth));
                ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) viewHolder.commentContainer.getLayoutParams();

                // Get the screen's density scale
                final float scale = mContext.getResources().getDisplayMetrics().density;

                layoutParams.setMargins((int) (depth * 3 * scale), layoutParams.topMargin, layoutParams.rightMargin, layoutParams.bottomMargin);

                if (comment.getAuthor().equals(mSubmission.getAuthor())) {
                    viewHolder.username.setTextColor(mContext.getResources().getColor(R.color.comment_author_font_color));
                } else {
                    viewHolder.username.setTextColor(mContext.getResources().getColor(R.color.comment_username_font_color));
                }


                if (comment.isLiked() == null) {

                    viewHolder.upvote.setStateVoted(false);
                    viewHolder.downvote.setStateVoted(false);
                } else {
                    if (comment.isLiked()) {
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

                        if (comment.isLiked() == null || !comment.isLiked()) {
                            direction = 1;
                        } else {
                            direction = 0;
                        }


                        SessionManager.getInstance(mContext).vote(comment.getFullName(), direction, new AsyncMarkActions.MarkActionsResponseHandler() {
                            @Override
                            public void onSuccess(boolean actionSuccessful) {
                                if (actionSuccessful) {
                                    if (comment.isLiked() == null) {
                                        comment.setScore(comment.getScore() + 1);
                                    } else {
                                        comment.setScore(comment.getScore() + (comment.isLiked() ? -1 : 2));
                                    }
                                    comment.setLiked(direction);
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

                        if (comment.isLiked() == null || comment.isLiked()) {
                            direction = -1;
                        } else {
                            direction = 0;
                        }

                        SessionManager.getInstance(mContext).vote(comment.getFullName(), direction, new AsyncMarkActions.MarkActionsResponseHandler() {
                            @Override
                            public void onSuccess(boolean actionSuccessful) {
                                if (actionSuccessful) {
                                    if (comment.isLiked() == null) {
                                        comment.setScore(comment.getScore() - 1);
                                    } else {
                                        comment.setScore(comment.getScore() - (comment.isLiked() ? 2 : -1));
                                    }
                                    comment.setLiked(direction);
                                    notifyDataSetChanged();
                                } else {
                                    Toast.makeText(mContext, "Failed to vote. Please try again later.", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }
                });

                if (comment.getGilded() > 0) {
                    viewHolder.gilded.setVisibility(View.VISIBLE);
                } else {
                    viewHolder.gilded.setVisibility(View.GONE);
                }
            } else {
                // comment is fault (not loaded)
                ViewHolder viewHolder = (ViewHolder) view.getTag();

                viewHolder.commentContainer.setVisibility(View.GONE);
                viewHolder.loadMoreOverlay.setVisibility(View.VISIBLE);

                TextView loadMoreText = (TextView)viewHolder.loadMoreOverlay.findViewById(R.id.load_more_label);
                loadMoreText.setText("Load " + comment.getMoreChildrenCount() + (comment.getMoreChildrenCount() == 1 ? " more comment" : " more comments"));

                ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) viewHolder.loadMoreOverlay.getLayoutParams();

                // Get the screen's density scale
                final float scale = mContext.getResources().getDisplayMetrics().density;

                layoutParams.setMargins((int) (depth * 3 * scale), layoutParams.topMargin, layoutParams.rightMargin, layoutParams.bottomMargin);

                viewHolder.loadMoreOverlay.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        SessionManager.getInstance(mContext).getMoreChildren(mSubmission, comment.getMoreChildren(), CommentSort.CONFIDENCE,
                                new AsyncComments.CommentsResponseHandler(comment) {
                            @Override
                            public void onParseFinished(List<ExtendedComment> comments) {
                                mComments.remove(position);
                                mComments.addAll(position, comments);
                                notifyDataSetChanged();
                            }
                        });
                    }
                });
            }

        }

        return view;
    }

    @Override
    public Object getItem(int i) {
        return mComments.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }
}
