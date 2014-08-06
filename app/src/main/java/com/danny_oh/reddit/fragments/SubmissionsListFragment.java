package com.danny_oh.reddit.fragments;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;


import com.danny_oh.reddit.SessionManager;
import com.danny_oh.reddit.activities.MainActivity;
import com.danny_oh.reddit.retrieval.AsyncSubmissions;
import com.danny_oh.reddit.util.EndlessScrollListener;
import com.danny_oh.reddit.R;
import com.danny_oh.reddit.adapters.SubmissionAdapter;
import com.danny_oh.reddit.util.PagedSubmissionsList;
import com.github.jreddit.entity.Submission;
import com.github.jreddit.retrieval.params.SubmissionSort;
import com.github.jreddit.retrieval.params.UserOverviewSort;
import com.github.jreddit.retrieval.params.UserSubmissionsCategory;

import java.util.List;

/**
 * A fragment that displays a list of jReddit Submissions
 */
public class SubmissionsListFragment extends Fragment implements
        // listener for action bar search box
        TextView.OnEditorActionListener
{
    public static final String ARG_SORT = "submission_sort";
    public static final String SUBREDDIT_VALUE_KEY = "SubmissionListFragment.Subbreddit";
    public static final String USER_SUBMISSIONS_CATEGORY_KEY = "user_submissions_category";

    private MenuItem mSearchMenuItem;
    private EditText mSearchMenuEditText;

    private String mSubredditName = "";

    private MainActivity mMainActivity;

    private UserSubmissionsCategory mUserSubmissionsCategory;

    private String mSubmissionSort;
    // default number of submissions to load per page
    private int mSubmissionsPerPage = 25;
    // this threshold is equal to the number of submissions that are not yet visible at the bottom of the list view
    private int mLoadMoreThreshold = 3;

    private boolean mShowAll = false;

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


    private static class SubmissionFetchParam {
        private String subreddit;
        private SubmissionSort sort;
        private int count;
        private int limit;
        private Submission before;
        private Submission after;
        private boolean show;
    }


    private OnSubmissionsListInteractionListener mListener;

    public interface OnSubmissionsListInteractionListener {
        public void onSearchSubmissions(String subreddit, String queryString);
    }



/*
 * Interface implementations
 */
    /**
     * listener for ActionBar search menu interactions
     * @param textView
     * @param i
     * @param keyEvent
     * @return
     */
    @Override
    public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
        if (keyEvent != null) {
            // if the 'return' key is pressed, get the text inside the search box (edit text) and collapse action view
            if (keyEvent.getAction() == KeyEvent.ACTION_DOWN && keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                // get user input
                String search = textView.getText().toString();

                // clear focus (also hides keyboard)
                mSearchMenuEditText.clearFocus();
                // collapse the action view
                MenuItemCompat.collapseActionView(mSearchMenuItem);

                mListener.onSearchSubmissions(mSubredditName, search);

                return true;
            }
        }
        return false;
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d("SubmissionListFragment", "onOptionsItemSelected called.");

        int id = item.getItemId();
        SubmissionSort sort = null;

        switch (id) {
            case R.id.sort_hot:
                sort = SubmissionSort.HOT;
                break;

            case R.id.sort_new:
                sort = SubmissionSort.NEW;
                break;

            case R.id.sort_rising:
                sort = SubmissionSort.RISING;
                break;

            case R.id.sort_controversial:
                sort = SubmissionSort.CONTROVERSIAL;
                break;

            case R.id.sort_top:
                sort = SubmissionSort.TOP;
                break;

            case R.id.refresh_submissions:
                initList();
                return true;
            case R.id.link_post_submission:
                mMainActivity.showFragment(SubmitPostFragment.newInstance(SubmitPostFragment.PostType.Link, mSubredditName), true);
                break;
            case R.id.text_post_submission:
                mMainActivity.showFragment(SubmitPostFragment.newInstance(SubmitPostFragment.PostType.Self, mSubredditName), true);
                break;

            default:
                return false;
        }

        if (sort != null && !mSubmissionSort.equals(sort.toString())) {
            mSubmissionSort = sort.toString();
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
    public SubmissionsListFragment() {

    }


    public static SubmissionsListFragment newInstance(UserSubmissionsCategory category, UserOverviewSort sort) {
        SubmissionsListFragment fragment = new SubmissionsListFragment();

        Bundle args = new Bundle();

        if (sort != null) {
            args.putString(ARG_SORT, sort.toString());
        } else {
            args.putString(ARG_SORT, UserOverviewSort.NEW.toString());
        }

        if (category != null) {
            args.putString(USER_SUBMISSIONS_CATEGORY_KEY, category.toString());
        } else {
            throw new Error("User submissions category cannot be missing.");
        }

        args.putString(SUBREDDIT_VALUE_KEY, "");

        fragment.setArguments(args);

        return fragment;
    }

    public static SubmissionsListFragment newInstance(String subredditName, SubmissionSort sort) {
        SubmissionsListFragment fragment = new SubmissionsListFragment();

        Bundle args = new Bundle();

        if (sort != null) {
            args.putString(ARG_SORT, sort.toString());
        } else {
            args.putString(ARG_SORT, SubmissionSort.HOT.toString());
        }

        if (subredditName != null) {
            args.putString(SUBREDDIT_VALUE_KEY, subredditName);
        } else {
            args.putString(SUBREDDIT_VALUE_KEY, "");
        }

        fragment.setArguments(args);

        return fragment;
    }




/*
 * Fragment lifecycle methods
 */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d("SubmissionsListFragment", "onCreate");
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();

        mSubredditName = args.getString(SUBREDDIT_VALUE_KEY);
        mSubmissionSort = args.getString(ARG_SORT);

        if (args.getString(USER_SUBMISSIONS_CATEGORY_KEY) != null)
            mUserSubmissionsCategory = UserSubmissionsCategory.valueOf(args.getString(USER_SUBMISSIONS_CATEGORY_KEY));

        setHasOptionsMenu(true);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d("SubmissionListFragment", "onCreateView()");

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
        Log.d("SubmissionListFragment", "onActivityCreated()");

        super.onActivityCreated(savedInstanceState);

        if (mPagedSubmissionsList != null) {
            // fragment was retained and restored
            Log.d("SubmissionListFragment", "Fragment was restored from instance.");

            // the adapter needs to be recreated because it maintains a reference to the old activity
            mAdapter = new SubmissionAdapter(getActivity(), mPagedSubmissionsList);
        } else {
            mPagedSubmissionsList = new PagedSubmissionsList(mSubmissionsPerPage);
            mAdapter = new SubmissionAdapter(getActivity(), mPagedSubmissionsList);
            initList();
        }

        // Set OnItemClickListener so we can be notified on item clicks
//        mListView.setOnItemClickListener(this);
        mListView.setAdapter(mAdapter);


        // add EndlessScrollListener that loads more submissions when the scroll position reaches close to the end

        if (mUserSubmissionsCategory == null) {
            // get submissions from the currently selected subreddit
            mListView.setOnScrollListener(new EndlessScrollListener(mLoadMoreThreshold) {

                @Override
                public void onLoadMore(int page, int totalItemsCount) {
                    Submission lastSubmission = (Submission) mAdapter.getItem(totalItemsCount - 1);

                    // passing empty string requests for the reddit frontpage
                    SessionManager.SubmissionFetchParam param = new SessionManager.SubmissionFetchParam();
                    param.subreddit = mSubredditName;
                    param.sort = SubmissionSort.valueOf(mSubmissionSort);
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
        } else {
            // get logged in user's saved submissions
            mListView.setOnScrollListener(new EndlessScrollListener(mLoadMoreThreshold) {
                @Override
                public void onLoadMore(int page, int totalItemsCount) {
                    Submission lastSubmission = (Submission) mAdapter.getItem(totalItemsCount - 1);

                    mProgressBar.setVisibility(View.VISIBLE);

                    SessionManager.getInstance(mMainActivity).getUserSubmissions(UserSubmissionsCategory.SAVED, UserOverviewSort.NEW, 0, 25, lastSubmission, null, false, new AsyncSubmissions.SubmissionsResponseHandler() {
                        @Override
                        public void onParseFinished(List<Submission> submissions) {
                            mPagedSubmissionsList.add(submissions);
                            mAdapter.notifyDataSetChanged();


                        }
                    });
                }

                ;

                @Override
                public void onLoadComplete() {
                    mProgressBar.setVisibility(View.GONE);
                }
            });
        }
    }

    @Override
    public void onResume() {
        Log.d("SubmissionListFragment", "onResume()");

        super.onResume();

        if (mUserSubmissionsCategory != null) {
            ((ActionBarActivity) mMainActivity).getSupportActionBar().setTitle(mUserSubmissionsCategory.toString());
        } else {
            if (mSubredditName.isEmpty()) {
                // default to front page
                ((ActionBarActivity) mMainActivity).getSupportActionBar().setTitle("front page");
            } else {
                ((ActionBarActivity) mMainActivity).getSupportActionBar().setTitle(mSubredditName);
            }
        }
    }

    @Override
    public void onDestroy() {
        Log.d("SubmissionListFragment", "onDestroy()");

        super.onDestroy();
    }

    @Override
    public void onAttach(Activity activity) {
        Log.d("SubmissionListFragment", "onAttach()");

        mMainActivity = (MainActivity) activity;

        super.onAttach(activity);
        try {
            mListener = (OnSubmissionsListInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        Log.d("SubmissionListFragment", "onDetach()");

        super.onDetach();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.d("SubmissionsListFragment", "onSaveInstanceState()");
        super.onSaveInstanceState(outState);
    }




    /*
     * Options Menu
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.submission_list_menu, menu);

        // get the custom defined action view
        mSearchMenuItem = menu.findItem(R.id.search_submissions);
        View actionView = MenuItemCompat.getActionView(mSearchMenuItem);

        if (actionView != null) {
            mSearchMenuEditText = (EditText)actionView.findViewById(R.id.action_search_edit_text);

            if (mSearchMenuEditText != null) {
                mSearchMenuEditText.setOnEditorActionListener(this);
                mSearchMenuEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View view, boolean hasFocus) {
                        if (!hasFocus) {
                            hideKeyboard();
                            MenuItemCompat.collapseActionView(mSearchMenuItem);
                        }
                    }
                });
            }
        }

        // to support API levels < 14, the SearchView widget was replaced with a custom action view.
        // therefore the action view must be reset manually
        MenuItemCompat.setOnActionExpandListener(mSearchMenuItem, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                // reset the search box (EditText)
                if (mSearchMenuEditText != null) {
                    mSearchMenuEditText.setText("");

                    mSearchMenuEditText.post(new Runnable() {
                        @Override
                        public void run() {
                            mSearchMenuEditText.requestFocus();
                            InputMethodManager manager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                            manager.showSoftInput(mSearchMenuEditText, InputMethodManager.SHOW_IMPLICIT);
                        }
                    });
                }

                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                return true;
            }
        });

    }

/*
 * public methods
 */
    public void refresh(String subredditName) {
        if (!mSubredditName.equals(subredditName)) {
            mSubredditName = subredditName;
            initList();
        }
    }

    public void refreshSort(String sort) {
        if (mSubmissionSort != sort) {
            mSubmissionSort = sort;
            initList();
        }
    }



    public void updateList() {
        mAdapter.notifyDataSetChanged();
    }
/*
 * private methods
 */
    private void initList() {
        // update the ActionBar title
        if (mSubredditName.isEmpty()) {
            // default to front page
            ((ActionBarActivity) mMainActivity).getSupportActionBar().setTitle("front page");
        } else {
            ((ActionBarActivity) mMainActivity).getSupportActionBar().setTitle(mSubredditName);
        }

        // set the ActionBar search box hint
        if (mSearchMenuEditText != null) {
            mSearchMenuEditText.setHint(mSubredditName.isEmpty() ? "reddit" : mSubredditName);
        }


        mPagedSubmissionsList.clear();
        mAdapter.notifyDataSetChanged();

        if (mUserSubmissionsCategory == null) {
            // passing empty string requests for the reddit frontpage
            SessionManager.SubmissionFetchParam param = new SessionManager.SubmissionFetchParam();
            param.subreddit = mSubredditName;
            param.sort = SubmissionSort.valueOf(mSubmissionSort);
            param.count = 0;
            param.limit = mSubmissionsPerPage;
            param.after = null;
            param.before = null;
            param.show = mShowAll;

            SessionManager.getInstance(mMainActivity).fetchMoreSubmissions(param, new SessionManager.SessionListener<List<Submission>>() {
                @Override
                public void onResponse(List<Submission> list) {
                    mPagedSubmissionsList.add(list);
                    mAdapter.notifyDataSetChanged();
                }
            });


        } else {

            SessionManager.getInstance(mMainActivity).getUserSubmissions(UserSubmissionsCategory.SAVED, UserOverviewSort.NEW, 0, 25, null, null, false, new AsyncSubmissions.SubmissionsResponseHandler() {
                @Override
                public void onParseFinished(List<Submission> submissions) {
                    mPagedSubmissionsList.add(submissions);
                    mAdapter.notifyDataSetChanged();


                }
            });
        }
    }


    /**
     * hides the soft keyboard (the only ui element that can bring up the keyboard in this fragment is mSearchMenuEditText)
     */
    private void hideKeyboard() {
        // hide the soft keyboard
        InputMethodManager manager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        manager.hideSoftInputFromWindow(mSearchMenuEditText.getWindowToken(), 0);
    }

}
