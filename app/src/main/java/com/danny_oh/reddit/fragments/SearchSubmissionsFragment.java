package com.danny_oh.reddit.fragments;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.danny_oh.reddit.R;
import com.danny_oh.reddit.SessionManager;
import com.danny_oh.reddit.activities.MainActivity;
import com.danny_oh.reddit.adapters.SubmissionAdapter;
import com.danny_oh.reddit.retrieval.AsyncSubmissions;
import com.danny_oh.reddit.util.EndlessScrollListener;
import com.danny_oh.reddit.util.PagedSubmissionsList;
import com.github.jreddit.entity.Submission;
import com.github.jreddit.retrieval.params.QuerySyntax;
import com.github.jreddit.retrieval.params.SearchSort;
import com.github.jreddit.retrieval.params.SubmissionSort;
import com.github.jreddit.retrieval.params.TimeSpan;

import java.util.List;

/**
 * Created by danny on 8/2/14.
 */
public class SearchSubmissionsFragment extends Fragment {
    private static final String ARG_SUBREDDIT_NAME = "submission_search_subreddit_name";
    private static final String ARG_QUERY_STRING = "submission_search_query_string";

    private MenuItem mSearchMenuItem;
    private EditText mSearchMenuEditText;

    private Context mContext;

    // Search settings
    private String mSubredditName;
    private String mQueryString;
    private QuerySyntax mSyntax = QuerySyntax.LUCENE;
    private SearchSort mSort = SearchSort.RELEVANCE;
    private TimeSpan mTime = TimeSpan.ALL;
    private int mCount = 0;
    private int mLimit = 25;
    private boolean mShowAll = true;

    // this threshold is equal to the number of submissions that are not yet visible at the bottom of the list view
    private int mLoadMoreThreshold = 3;


    private PagedSubmissionsList mPagedSubmissionsList;

    private ProgressBar mProgressBar;

    /**
     * The fragment's ListView/GridView.
     */
    private ListView mListView;

    /**
     * The Adapter which will be used to populate the ListView/GridView with
     * Views.
     */
    private SubmissionAdapter mAdapter;


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.search_submissions_fragment_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d("SubmissionListFragment", "onOptionsItemSelected called.");

        int id = item.getItemId();
        SearchSort sort;

        switch (id) {
            case R.id.sort_relevance:
                sort = SearchSort.RELEVANCE;
                break;

            case R.id.sort_hot:
                sort = SearchSort.HOT;
                break;

            case R.id.sort_new:
                sort = SearchSort.NEW;
                break;

            case R.id.sort_top:
                sort = SearchSort.TOP;
                break;

            case R.id.sort_comments:
                sort = SearchSort.COMMENTS;
                break;

            default:
                return false;
        }

        if (mSort != sort) {
            mSort = sort;
            initList();
        }

        return true;
    }

/*
 * Constructors
 */

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public SearchSubmissionsFragment() {

    }


    public static SearchSubmissionsFragment newInstance(String subreddit, String query) {
        SearchSubmissionsFragment fragment = new SearchSubmissionsFragment();

        Bundle args = new Bundle();

        if (subreddit == null || query == null) {
            throw new Error("subreddit and query cannot be null. Pass an empty string for 'subreddit' to search all of reddit.");
        }

        args.putString(ARG_SUBREDDIT_NAME, subreddit);
        args.putString(ARG_QUERY_STRING, query);

        fragment.setArguments(args);

        return fragment;
    }




/*
 * Fragment lifecycle methods
 */

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mContext = activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d("SubmissionsListFragment", "onCreate");
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();

        mSubredditName = args.getString(ARG_SUBREDDIT_NAME);
        mQueryString = args.getString(ARG_QUERY_STRING);

        ((ActionBarActivity)mContext).getSupportActionBar().setTitle("\"" + mQueryString + "\"");

        setHasOptionsMenu(true);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_submission_list, container, false);

        mProgressBar = (ProgressBar)view.findViewById(R.id.progressBar);
        mProgressBar.setVisibility(View.GONE);

        mListView = (ListView)view.findViewById(android.R.id.list);

        ProgressBar progressBar = (ProgressBar)view.findViewById(android.R.id.empty);
        mListView.setEmptyView(progressBar);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (mPagedSubmissionsList != null) {
            mListView.setAdapter(mAdapter);
            // fragment was retained and restored
            Log.d("SubmissionListFragment", "Fragment was restored from instance.");
        } else {
            mPagedSubmissionsList = new PagedSubmissionsList(mLimit);
            mAdapter = new SubmissionAdapter(getActivity(), mPagedSubmissionsList);
            mListView.setAdapter(mAdapter);

            initList();
        }

        // add EndlessScrollListener that loads more submissions when the scroll position reaches close to the end
        mListView.setOnScrollListener(new EndlessScrollListener(mLoadMoreThreshold) {
            @Override
            public void onLoadMore(int page, int totalItemsCount) {
                mProgressBar.setVisibility(View.VISIBLE);

                Submission lastSubmission = (Submission) mAdapter.getItem(totalItemsCount - 1);

                SessionManager.getInstance(mContext).searchSubmissions(mSubredditName, mQueryString, mSyntax, mSort, mTime, mCount, mLimit,
                        lastSubmission, null, mShowAll, new AsyncSubmissions.SubmissionsResponseHandler() {
                            @Override
                            public void onParseFinished(List<Submission> submissions) {
                                mPagedSubmissionsList.add(submissions);
                                mAdapter.notifyDataSetChanged();
                                mProgressBar.setVisibility(View.GONE);
                            }
                        });
            }

            @Override
            public void onLoadComplete() {
            }
        });

    }

    @Override
    public void onResume() {
        super.onResume();

        String title;
        if (mSubredditName.isEmpty()) {
            title = String.format("\"%s\" in %s", mQueryString, "reddit");
        } else {
            title = String.format("\"%s\" in %s", mQueryString, mSubredditName);
        }

        ((MainActivity)getActivity()).getSupportActionBar().setTitle(title);
    }

    private void initList() {
        mPagedSubmissionsList.clear();
        mAdapter.notifyDataSetChanged();


        SessionManager.getInstance(mContext).searchSubmissions(mSubredditName, mQueryString, mSyntax, mSort, mTime,
                mCount, mLimit, null, null, mShowAll, new AsyncSubmissions.SubmissionsResponseHandler() {
                    @Override
                    public void onParseFinished(List<Submission> submissions) {
                        mPagedSubmissionsList.add(submissions);
                        mAdapter.notifyDataSetChanged();
                    }
                });
    }

}
