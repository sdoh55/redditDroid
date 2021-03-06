package com.danny_oh.reddit.fragments;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.danny_oh.reddit.R;
import com.danny_oh.reddit.SessionManager;
import com.danny_oh.reddit.adapters.CommentAdapter;
import com.danny_oh.reddit.retrieval.AsyncComments;
import com.danny_oh.reddit.retrieval.AsyncMarkActions;
import com.danny_oh.reddit.util.CommentsListHelper;
import com.danny_oh.reddit.util.ExtendedComment;
import com.danny_oh.reddit.util.ExtendedSubmission;
import com.danny_oh.reddit.util.ImageViewWithVoteState;
import com.github.jreddit.entity.Comment;
import com.github.jreddit.entity.Submission;
import com.github.jreddit.retrieval.Comments;
import com.github.jreddit.retrieval.params.CommentSort;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import in.uncod.android.bypass.Bypass;


/**
 * Created by danny on 7/31/14.
 *
 * A Fragment that displays a list of comments along with the submission's content and information
 * in the header of the ListView.
 */
public class CommentsListFragment extends Fragment {

    private static final String ARG_SUBMISSION_KEY = "submission_key";
    private static final String ARG_DELAYED_KEY = "delayed_key";

    private Submission mSubmission;
    private boolean mDelayed;       // if this is true, the fragment doesn't load comments right away and must call initList();

    private TextView mScoreLabel;
    private ImageViewWithVoteState mUpvoteIndicator;
    private ImageViewWithVoteState mDownvoteIndicator;


    private CommentSort mCommentSort = CommentSort.CONFIDENCE;

    private List<ExtendedComment> mComments;

    private ListView mListView;
    private CommentAdapter mAdapter;
    private View mHeaderView;


    private OnCommentsListFragmentDetachListener mListener;

    public interface OnCommentsListFragmentDetachListener {
        public void onCommentsListFragmentDetach(Submission submission);
    }

/*
 * constructors and instantiation methods
 */
    public static CommentsListFragment newInstance(ExtendedSubmission submission) {
        return newInstance(submission, false);
    }

    public static CommentsListFragment newInstance(ExtendedSubmission submission, boolean delayed) {
        CommentsListFragment fragment = new CommentsListFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_SUBMISSION_KEY, submission);
        args.putBoolean(ARG_DELAYED_KEY, delayed);
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
            mListener = (OnCommentsListFragmentDetachListener)activity;
        } catch (ClassCastException ce) {
            throw new ClassCastException("Parent activity of SelfSubmissionFragment must implement");
        }
    }

    @Override
    public void onDetach() {
        mListener.onCommentsListFragmentDetach(mSubmission);
        super.onDetach();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mSubmission = (ExtendedSubmission)getArguments().getParcelable(ARG_SUBMISSION_KEY);
            mDelayed = getArguments().getBoolean(ARG_DELAYED_KEY);
        } else {
            throw new InstantiationException("Fragments must be instantiated using factory method newInstance.", new Exception());
        }

//        setHasOptionsMenu(true);
        setRetainInstance(true);
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_comments_list, container, false);

        mListView = (ListView)view.findViewById(android.R.id.list);

        mHeaderView = inflater.inflate(R.layout.list_header_self_submission, mListView, false);

        mListView.addHeaderView(mHeaderView);
        mListView.setEmptyView(view.findViewById(android.R.id.empty));

        Spinner spinner = new Spinner(getActivity());
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(), R.array.comments_sort_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                onOptionsItemSelected(i);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        mListView.addHeaderView(spinner);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (mComments != null && mAdapter != null) {
            // fragment was restored from retained instance
            Log.d("SelfSubmissionFragment", "Fragment was restored from instance.");
            mListView.setAdapter(mAdapter);

        } else {
            // otherwise instantiate fragment
            mComments = new LinkedList<ExtendedComment>();
            mAdapter = new CommentAdapter(getActivity(), mComments, mSubmission);

            if (!mDelayed) {
                // retrieves comments for the selected submission
                initList();
            }
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
        final ImageViewWithVoteState saved = (ImageViewWithVoteState) mHeaderView.findViewById(R.id.submission_saved);

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


        if (mSubmission.isSaved()) {
            saved.setStateVoted(true);
        } else {
            saved.setStateVoted(false);
        }

        saved.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!mSubmission.isSaved()) {
                    SessionManager.getInstance(context).saveThing(mSubmission.getFullName(),
                            new AsyncMarkActions.MarkActionsResponseHandler(){

                                @Override
                                public void onSuccess(boolean actionSuccessful) {
                                    if (actionSuccessful) {
                                        mSubmission.setSaved(true);
                                        saved.setStateVoted(true);
                                        Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(context, "Failed to save. Please try again later.", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });

                } else {
                    SessionManager.getInstance(context).unsaveThing(mSubmission.getFullName(),
                            new AsyncMarkActions.MarkActionsResponseHandler(){

                                @Override
                                public void onSuccess(boolean actionSuccessful) {
                                    if (actionSuccessful) {
                                        mSubmission.setSaved(false);
                                        saved.setStateVoted(false);
                                        Toast.makeText(context, "Unsaved", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(context, "Failed to unsave. Please try again later.", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                }
            }
        });




        mUpvoteIndicator.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                final int direction;

                if (mSubmission.isLiked() == null || !mSubmission.isLiked()) {
                    direction = 1;
                } else {
                    direction = 0;
                }

                SessionManager.getInstance(context).vote(mSubmission.getFullName(), direction, new AsyncMarkActions.MarkActionsResponseHandler() {
                    @Override
                    public void onSuccess(boolean actionSuccessful) {
                        if (actionSuccessful) {
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

                SessionManager.getInstance(context).vote(mSubmission.getFullName(), direction, new AsyncMarkActions.MarkActionsResponseHandler() {
                    @Override
                    public void onSuccess(boolean actionSuccessful) {
                        if (actionSuccessful) {
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
//        if (mCommentTask != null)
//            mCommentTask.cancel(true);
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

    public boolean onOptionsItemSelected(int id) {
        CommentSort sort;

        switch (id) {
            case 0:
                sort = CommentSort.CONFIDENCE;
                break;
            case 1:
                sort = CommentSort.NEW;
                break;
            case 2:
                sort = CommentSort.HOT;
                break;
            case 3:
                sort = CommentSort.TOP;
                break;
            case 4:
                sort = CommentSort.CONTROVERSIAL;
                break;
            case 5:
                sort = CommentSort.OLD;
                break;
            case 6:
                sort = CommentSort.RANDOM;
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


    public void initList() {
        mComments.clear();
        mAdapter.notifyDataSetChanged();

        SessionManager.getInstance(getActivity()).getCommentsFromSubmission(mSubmission, "", -1, -1, -1, mCommentSort, new AsyncComments.CommentsResponseHandler(){
            @Override
            public void onParseFinished(List<ExtendedComment> comments) {
                mComments = comments;
                mAdapter = new CommentAdapter(getActivity(), mComments, mSubmission);
                mListView.setAdapter(mAdapter);
            }
        });


//        mCommentTask = new GetCommentsAsyncTask();
//        mCommentTask.execute(mSubmission.getIdentifier());
    }

    public boolean isLoaded() {
        return mComments.size() > 0;
    }
}
