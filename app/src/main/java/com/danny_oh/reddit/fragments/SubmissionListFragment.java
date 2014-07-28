package com.danny_oh.reddit.fragments;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SpinnerAdapter;


import com.danny_oh.reddit.SessionManager;
import com.danny_oh.reddit.util.EndlessScrollListener;
import com.danny_oh.reddit.R;
import com.danny_oh.reddit.adapters.SubmissionAdapter;
import com.danny_oh.reddit.util.PagedSubmissionsList;
import com.github.jreddit.entity.Submission;
import com.github.jreddit.retrieval.Submissions;
import com.github.jreddit.retrieval.params.SubmissionSort;
import com.github.jreddit.utils.restclient.PoliteHttpRestClient;

import java.util.List;

/**
 * A fragment that displays a list of jReddit Submissions
 */
public class SubmissionListFragment extends Fragment implements
        AbsListView.OnItemClickListener,
        SubmissionAdapter.OnSubmissionAdapterInteractionListener,
        ActionBar.OnNavigationListener
{
    private static final String ARG_SORT = "submission_sort";

    private Context mContext;

    private SubmissionSort mSubmissionSort;
    // default number of submissions to load per page
    private int mSubmissionsPerPage = 25;
    // this threshold is equal to the number of submissions that are not yet visible at the bottom of the list view
    private int mLoadMoreThreshold = 3;
    private boolean mShowAll = false;

    private OnSubmissionListFragmentInteractionListener mListener;
    private PagedSubmissionsList mPagedSubmissionsList;

    private Submissions mSubmissionsController;
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


    private EndlessScrollListener mEndlessListener;

    private static class SubmissionFetchParam {
        private String subreddit;
        private SubmissionSort sort;
        private int count;
        private int limit;
        private Submission before;
        private Submission after;
        private boolean show;
    }


    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     */
    public interface OnSubmissionListFragmentInteractionListener {
        public void onSubmissionClick(Submission submission);

//        public void onSubmissionCommentsClick(Submission submission);
    }


    private void loadMore(SubmissionFetchParam param) {
        List<Submission> list = mSubmissionsController.ofSubreddit(param.subreddit, param.sort, param.count, param.limit, param.after, param.before, param.show);
        mPagedSubmissionsList.add(list);
    }


    /**
     * OnItemClick listener to handle item clicks on mListView
     * @param parent
     * @param view
     * @param position
     * @param id
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mListView.getItemAtPosition(position);

        if (null != mListener) {
            // Notify the active callbacks interface (the activity, if the
            // fragment is attached to one) that an item has been selected.

            Submission submission = (Submission)mAdapter.getItem(position);
            mListener.onSubmissionClick(submission);
        }
    }

    /**
     * OnSubmissionAdapterInteractionListener interface implementation
     * Listener for clicks on the 'number of comments' View from submissions list view.
     * @param submission
     */
    @Override
    public void onCommentsClick(Submission submission) {
        getFragmentManager()
                .beginTransaction()
                .addToBackStack(null)
                .add(R.id.content_frame, CommentListFragment.newInstance(submission.getIdentifier()))
                .commit();
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public SubmissionListFragment() {

    }

    public static SubmissionListFragment newInstance(SubmissionSort sort) {
        SubmissionListFragment fragment = new SubmissionListFragment();

        if (sort != null) {
            Bundle args = new Bundle();
            args.putString(ARG_SORT, sort.toString());
            fragment.setArguments(args);
        }
        return fragment;
    }


    @Override
    public boolean onNavigationItemSelected(int position, long id) {
        SubmissionSort choice;

        switch (position) {
            case 0:
                choice = SubmissionSort.HOT;
                break;
            case 1:
                choice = SubmissionSort.NEW;
                break;
            case 2:
                choice = SubmissionSort.RISING;
                break;
            case 3:
                choice = SubmissionSort.CONTROVERSIAL;
                break;
            case 4:
                choice = SubmissionSort.TOP;
                break;
            default:
                choice = null;
        }

        if (mSubmissionSort != choice) {
            mSubmissionSort = choice;
            initList();
        }

        return true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();

        if (args != null) {
            mSubmissionSort = SubmissionSort.valueOf(args.getString(ARG_SORT));
        } else {
            mSubmissionSort = SubmissionSort.HOT;
        }

        mSubmissionsController = new Submissions(new PoliteHttpRestClient());
        mPagedSubmissionsList = new PagedSubmissionsList(mSubmissionsPerPage);

        mAdapter = new SubmissionAdapter(getActivity(), mPagedSubmissionsList);
        mAdapter.setOnSubmissionAdapterInteractionListener(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_submission_list, container, false);

        mProgressBar = (ProgressBar)view.findViewById(R.id.progressBar);
        mProgressBar.setVisibility(View.GONE);

        mListView = (ListView)view.findViewById(android.R.id.list);

        // Set OnItemClickListener so we can be notified on item clicks
        mListView.setOnItemClickListener(this);
        mListView.setAdapter(mAdapter);
        mListView.setOnScrollListener(new EndlessScrollListener(mLoadMoreThreshold) {

            @Override
            public void onLoadMore(int page, int totalItemsCount) {
                Submission lastSubmission = (Submission)mAdapter.getItem(totalItemsCount-1);

                // passing empty string requests for the reddit frontpage
                SessionManager.SubmissionFetchParam param = new SessionManager.SubmissionFetchParam();
                param.subreddit = "";
                param.sort = mSubmissionSort;
                param.count = 0;
                param.limit = mSubmissionsPerPage;
                param.after = lastSubmission;
                param.before = null;
                param.show = mShowAll;

                SessionManager.getInstance(getActivity()).fetchMoreSubmissions(param, new SessionManager.SessionListener<List<Submission>>() {
                    @Override
                    public void onResponse(List<Submission> list) {
                        mPagedSubmissionsList.add(list);
                        mAdapter.notifyDataSetChanged();
                    }
                });
                mProgressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onLoadComplete() {
                mProgressBar.setVisibility(View.GONE);
            }
        });

        // spinner for dropdown menu
        SpinnerAdapter adapter = ArrayAdapter.createFromResource(mContext, R.array.submission_sort_array, R.layout.navigation_item_submission);
        ((ActionBarActivity)mContext).getSupportActionBar().setListNavigationCallbacks(adapter, this);
        ((ActionBarActivity)mContext).getSupportActionBar().setSelectedNavigationItem(sortToIndex(mSubmissionSort));



        initList();

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        Log.d("SubmissionListFragment", "onAttach()");

        mContext = (Context)activity;

        super.onAttach(activity);
        try {
            mListener = (OnSubmissionListFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        Log.d("SubmissionListFragment", "onDetach()");

        super.onDetach();
        mListener = null;
    }


    private void initList() {
        mPagedSubmissionsList.clear();
        mAdapter.notifyDataSetChanged();

        // passing empty string requests for the reddit frontpage
        SessionManager.SubmissionFetchParam param = new SessionManager.SubmissionFetchParam();
        param.subreddit = "";
        param.sort = mSubmissionSort;
        param.count = 0;
        param.limit = mSubmissionsPerPage;
        param.after = null;
        param.before = null;
        param.show = mShowAll;

        SessionManager.getInstance(mContext).fetchMoreSubmissions(param, new SessionManager.SessionListener<List<Submission>>() {
            @Override
            public void onResponse(List<Submission> list) {
                mPagedSubmissionsList.add(list);
                mAdapter.notifyDataSetChanged();
            }
        });

    }

    private int sortToIndex(SubmissionSort sort) {
        switch (sort) {
            case HOT:
                return 0;
            case NEW:
                return 1;
            case RISING:
                return 2;
            case CONTROVERSIAL:
                return 3;
            case TOP:
                return 4;
            default:
                return -1;
        }
    }

}
