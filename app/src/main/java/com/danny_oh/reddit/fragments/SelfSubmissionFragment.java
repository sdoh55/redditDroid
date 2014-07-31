package com.danny_oh.reddit.fragments;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.danny_oh.reddit.R;
import com.danny_oh.reddit.SessionManager;
import com.danny_oh.reddit.util.ExtendedSubmission;
import com.danny_oh.reddit.util.ImageViewWithVoteState;
import com.github.jreddit.entity.Submission;

import in.uncod.android.bypass.Bypass;


/**
 * Created by danny on 7/31/14.
 */
public class SelfSubmissionFragment extends Fragment {

    private static final String ARG_SUBMISSION_KEY = "submission_key";

    private Submission mSubmission;

    private TextView mScoreLabel;
    private ImageViewWithVoteState mUpvoteIndicator;
    private ImageViewWithVoteState mDownvoteIndicator;

    public static SelfSubmissionFragment newInstance(ExtendedSubmission submission) {
        SelfSubmissionFragment fragment = new SelfSubmissionFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_SUBMISSION_KEY, submission);
        fragment.setArguments(args);
        return fragment;
    }


    public SelfSubmissionFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mSubmission = (ExtendedSubmission)getArguments().getParcelable(ARG_SUBMISSION_KEY);
        } else {
            throw new InstantiationException("Fragments must be instantiated using factory method newInstance.", new Exception());
        }

        // has to be set in order to display fragment's own options menu (i.e. calling onCreateOptionsMenu)
        setHasOptionsMenu(true);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_submission_self, container, false);

        TextView title = (TextView)view.findViewById(R.id.submission_title);
        mScoreLabel = (TextView)view.findViewById(R.id.submission_score);
        mUpvoteIndicator = (ImageViewWithVoteState)view.findViewById(R.id.submission_up_vote);
        mDownvoteIndicator = (ImageViewWithVoteState)view.findViewById(R.id.submission_down_vote);

        title.setText(mSubmission.getTitle());
        mScoreLabel.setText(mSubmission.getScore().toString());

        updateVoteIndicator(null);

        final Context context = (Context)getActivity();

        mUpvoteIndicator.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                final int direction;

                if (mSubmission.isLiked() == null || !mSubmission.isLiked()) {
                    direction = 1;
                } else {
                    direction = 0;
                }

                SessionManager.getInstance(context).vote(mSubmission.getFullName(), direction, new SessionManager.SessionListener<Boolean>() {
                    @Override
                    public void onResponse(Boolean object) {
                        // if vote was successful
                        if (object) {
                            updateVoteIndicator(direction);
                        } else {
                            Toast.makeText(context, "Failed to vote. Please try again later.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });


        mDownvoteIndicator.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final int direction;

                if (mSubmission.isLiked() == null || mSubmission.isLiked()) {
                    direction = -1;
                } else {
                    direction = 0;
                }

                SessionManager.getInstance(context).vote(mSubmission.getFullName(), direction, new SessionManager.SessionListener<Boolean>() {
                    @Override
                    public void onResponse(Boolean object) {
                        // if vote was successful
                        if (object) {
                            updateVoteIndicator(direction);
                        } else {
                            Toast.makeText(context, "Failed to vote. Please try again later.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });



        final TextView text = (TextView)view.findViewById(R.id.submission_self_text_html);
        final ProgressBar progressBar = (ProgressBar)view.findViewById(R.id.progressBar);

        new AsyncTask<Void, Void, CharSequence>() {
            @Override
            protected CharSequence doInBackground(Void... voids) {
                Bypass bypass = new Bypass();
                String markdownString = mSubmission.getSelftext();
                return bypass.markdownToSpannable(markdownString);
            }

            @Override
            protected void onPostExecute(CharSequence charSequence) {
                text.setText(charSequence);
                text.setMovementMethod(LinkMovementMethod.getInstance());

                progressBar.setVisibility(View.GONE);
            }
        }.execute();


        // add the CommentListFragment
        getActivity().getSupportFragmentManager().beginTransaction()
                .add(R.id.comments_container, CommentListFragment.newInstance((ExtendedSubmission)mSubmission))
                .commit();

        return view;
    }

    /**
     * updates the underlying submission object, sets the states for up/down vote icons, and sets text for the score label.
     * @param direction direction of vote for the submission object. pass null to set the initial states.
     */
    private void updateVoteIndicator(Integer direction) {
        if (direction != null) {
            int liked;

            if (mSubmission.isLiked() == null) {
                liked = 0;
            } else {
                liked = (mSubmission.isLiked() ? 1 : -1);
            }

            mSubmission.setScore(mSubmission.getScore() + direction - liked);
            mSubmission.setLiked(direction);
        }

        if (mSubmission.isLiked() == null) {
            mUpvoteIndicator.setStateVoted(false);
            mDownvoteIndicator.setStateVoted(false);
        } else {
            if (mSubmission.isLiked()) {
                mUpvoteIndicator.setStateVoted(true);
                mDownvoteIndicator.setStateVoted(false);
            } else {
                mUpvoteIndicator.setStateVoted(false);
                mDownvoteIndicator.setStateVoted(true);
            }
        }

        mScoreLabel.setText(mSubmission.getScore().toString());
    }

}
