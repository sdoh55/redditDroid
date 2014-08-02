package com.danny_oh.reddit.fragments;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HeaderViewListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.danny_oh.reddit.R;
import com.danny_oh.reddit.SessionManager;
import com.danny_oh.reddit.adapters.CommentSparseArrayAdapter;
import com.danny_oh.reddit.util.CommentsListHelper;
import com.danny_oh.reddit.util.ExtendedSubmission;
import com.danny_oh.reddit.util.ImageViewWithVoteState;
import com.github.jreddit.entity.Comment;
import com.github.jreddit.entity.Submission;
import com.github.jreddit.retrieval.Comments;
import com.github.jreddit.retrieval.params.CommentSort;

import java.util.List;

import in.uncod.android.bypass.Bypass;


/**
 * Created by danny on 7/31/14.
 */
public class CommentsListFragment extends Fragment {

    private static final String ARG_SUBMISSION_KEY = "submission_key";

    private Submission mSubmission;

    private TextView mScoreLabel;
    private ImageViewWithVoteState mUpvoteIndicator;
    private ImageViewWithVoteState mDownvoteIndicator;


    private CommentSort mCommentSort = CommentSort.CONFIDENCE;

    private GetCommentsAsyncTask mCommentTask;

    private List<Comment> mCommentsList;

    private SparseArray<CommentsListHelper.CommentContainer> mCommentArray;

    private ListView mListView;
    private CommentSparseArrayAdapter mAdapter;
    private View mHeaderView;


    private OnSelfSubmissionFragmentDetachListener mListener;

    public interface OnSelfSubmissionFragmentDetachListener {
        public void onSelfSubmissionFragmentDetach(Submission submission);
    }


    private class GetCommentsAsyncTask extends AsyncTask<String, Integer, Void> {
        @Override
        protected Void doInBackground(String... submissionId) {
            SessionManager manager = SessionManager.getInstance(getActivity());

            Comments comments = new Comments(manager.getRestClient(), manager.getUser());

            // params: submissionId, commentId, parentsShown, depth, limit, CommentSort
            mCommentsList = comments.ofSubmission(submissionId[0], null, -1, -1, -1, mCommentSort);
            mCommentArray = CommentsListHelper.listToSparseArray(mCommentsList);

            Log.d("CommentListFragment", "mCommentList count: " + mCommentsList.size());
            Log.d("CommentListFragment", "mCommentArray count: " + mCommentArray.size());

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            mCommentsList = null;
            mAdapter = new CommentSparseArrayAdapter(getActivity(), mCommentArray, mSubmission);
            mListView.setAdapter(mAdapter);
        }
    }



/*
 * constructors and instantiation methods
 */
    public static CommentsListFragment newInstance(ExtendedSubmission submission) {
        CommentsListFragment fragment = new CommentsListFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_SUBMISSION_KEY, submission);
        fragment.setArguments(args);
        return fragment;
    }


    public CommentsListFragment() {

    }

/*
 * Fragment lifecycle methods
 */
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mListener = (OnSelfSubmissionFragmentDetachListener)activity;
        } catch (ClassCastException ce) {
            ce.printStackTrace();
            throw new ClassCastException("Parent activity of SelfSubmissionFragment must implement");
        }
    }

    @Override
    public void onDetach() {
        mListener.onSelfSubmissionFragmentDetach(mSubmission);
        super.onDetach();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mSubmission = (ExtendedSubmission)getArguments().getParcelable(ARG_SUBMISSION_KEY);
        } else {
            throw new InstantiationException("Fragments must be instantiated using factory method newInstance.", new Exception());
        }

        setHasOptionsMenu(true);
        setRetainInstance(true);
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_comments_list, container, false);

        mListView = (ListView)view.findViewById(android.R.id.list);

        mHeaderView = inflater.inflate(R.layout.list_header_self_submission, mListView, false);

        mListView.addHeaderView(mHeaderView);
        mListView.setEmptyView(view.findViewById(android.R.id.empty));

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (mCommentArray != null && mAdapter != null) {
            // fragment was restored from retained instance
            Log.d("SelfSubmissionFragment", "Fragment was restored from instance.");
            mListView.setAdapter(mAdapter);

        } else {
            // otherwise instantiate fragment
            mCommentArray = new SparseArray<CommentsListHelper.CommentContainer>();
            mAdapter = new CommentSparseArrayAdapter(getActivity(), mCommentArray, mSubmission);

            // retrieves comments for the selected submission
            initList();
        }


        final TextView text = (TextView)mHeaderView.findViewById(R.id.submission_self_text_html);
        final ProgressBar progressBar = (ProgressBar)mHeaderView.findViewById(R.id.progressBar);

        // start an async task to parse the markdown format selftext
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


        // initialize view elements
        TextView title = (TextView) mHeaderView.findViewById(R.id.submission_title);
        mScoreLabel = (TextView) mHeaderView.findViewById(R.id.submission_score);
        TextView timeElapsed = (TextView) mHeaderView.findViewById(R.id.hours_ago_label);
        TextView author = (TextView) mHeaderView.findViewById(R.id.author_label);
        TextView subreddit = (TextView) mHeaderView.findViewById(R.id.subreddit_label);
        mUpvoteIndicator = (ImageViewWithVoteState) mHeaderView.findViewById(R.id.submission_up_vote);
        mDownvoteIndicator = (ImageViewWithVoteState) mHeaderView.findViewById(R.id.submission_down_vote);


        title.setText(mSubmission.getTitle());
        mScoreLabel.setText(mSubmission.getScore().toString());

        // time elapsed calculation
        long timeNow = System.currentTimeMillis() / 1000;
        long timePosted = mSubmission.getCreatedUTC();
        int hoursElapsed = (int)((timeNow - timePosted) / 60 / 60);

        String timeElapsedString = (hoursElapsed > 0) ? hoursElapsed + " hrs ago" : ((timeNow - timePosted) / 60) + " mins ago";
        timeElapsed.setText(timeElapsedString);

        author.setText(mSubmission.getAuthor());
        subreddit.setText(mSubmission.getSubreddit());

        // sets the voted states for up/down vote image views and sets score for the score label.
        updateVoteIndicator(null);


        final Context context = getActivity();

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
    }

    @Override
    public void onResume() {
        Log.d("CommentsListFragment", "onResume()");

        super.onResume();
        ((ActionBarActivity)getActivity()).getSupportActionBar().setTitle(mSubmission.getTitle());
    }

    @Override
    public void onDestroy() {
        mCommentTask.cancel(true);
        super.onDestroy();
    }

    /*
         * Options Menu methods
         */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.comments_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        CommentSort sort;

        switch (id) {
            case R.id.comments_sort_new:
                sort = CommentSort.NEW;
                break;
            case R.id.comments_sort_hot:
                sort = CommentSort.HOT;
                break;
            case R.id.comments_sort_top:
                sort = CommentSort.TOP;
                break;
            case R.id.comments_sort_controversial:
                sort = CommentSort.CONTROVERSIAL;
                break;
            case R.id.comments_sort_old:
                sort = CommentSort.OLD;
                break;
            case R.id.comments_sort_random:
                sort = CommentSort.RANDOM;
                break;
            case R.id.comments_sort_confidence:
                sort = CommentSort.CONFIDENCE;
                break;

            default:
                return false;
        }

        if (mCommentSort != sort) {
            mCommentSort = sort;
            initList();
        }

        return true;
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


    private void initList() {
        mCommentArray.clear();
        mAdapter.notifyDataSetChanged();

        mCommentTask = new GetCommentsAsyncTask();
        mCommentTask.execute(mSubmission.getIdentifier());
    }

}
