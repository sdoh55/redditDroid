package com.danny_oh.reddit.fragments;

import android.app.Activity;
import android.content.Context;
import android.hardware.input.InputManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;


import com.danny_oh.reddit.SessionManager;
import com.danny_oh.reddit.util.EndlessScrollListener;
import com.danny_oh.reddit.R;
import com.danny_oh.reddit.adapters.SubmissionAdapter;
import com.danny_oh.reddit.util.PagedSubmissionsList;
import com.github.jreddit.entity.Submission;
import com.github.jreddit.retrieval.params.SubmissionSort;

import java.util.List;

/**
 * A fragment that displays a list of jReddit Submissions
 */
public class SubmissionListFragment extends Fragment implements
        // listener for submissions list view clicks (displays the selected submission)
        AbsListView.OnItemClickListener,
        // listener for items contained inside each individual submissions list view cell (e.g. up/down vote buttons)
        SubmissionAdapter.OnSubmissionAdapterInteractionListener,
        // listener for action bar navigation clicks
        ActionBar.OnNavigationListener,
        // listener for action bar search box
        TextView.OnEditorActionListener
{
    public static final String ARG_SORT = "submission_sort";
    public static final String SUBREDDIT_VALUE_KEY = "SubmissionListFragment.Subbreddit";

    private MenuItem mSearchMenuItem;
    private EditText mSearchMenuEditText;

    private String mSubredditName = "";

    private Context mContext;

    private SubmissionSort mSubmissionSort;
    // default number of submissions to load per page
    private int mSubmissionsPerPage = 25;
    // this threshold is equal to the number of submissions that are not yet visible at the bottom of the list view
    private int mLoadMoreThreshold = 3;

    private boolean mShowAll = false;

    private OnSubmissionListFragmentInteractionListener mListener;
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




/*
 * Interface implementations
 */

    /**
     * ActionBar navigation listener
     * @param position
     * @param id
     * @return
     */
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
                CharSequence search = textView.getText();

                // clear focus (also hides keyboard)
                mSearchMenuEditText.clearFocus();
                // collapse the action view
                MenuItemCompat.collapseActionView(mSearchMenuItem);

                // TODO: search submissions
                Toast.makeText(mContext, search, Toast.LENGTH_SHORT).show();

                return true;
            }
        }
        return false;
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
        if (mListener != null) {
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
//        mSearchMenuEditText.clearFocus();

        getFragmentManager()
                .beginTransaction()
                .addToBackStack(null)
                .add(R.id.content_frame, CommentListFragment.newInstance(submission.getIdentifier()))
                .commit();
    }




/*
 * Constructors
 */

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public SubmissionListFragment() {

    }



    public static SubmissionListFragment newInstance(String subredditName, SubmissionSort sort) {
        SubmissionListFragment fragment = new SubmissionListFragment();

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
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();

        mSubredditName = args.getString(SUBREDDIT_VALUE_KEY);
        mSubmissionSort = SubmissionSort.valueOf(args.getString(ARG_SORT));

        mPagedSubmissionsList = new PagedSubmissionsList(mSubmissionsPerPage);

        mAdapter = new SubmissionAdapter(getActivity(), mPagedSubmissionsList);
        mAdapter.setOnSubmissionAdapterInteractionListener(this);

        setHasOptionsMenu(true);
    }

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
                if (mSearchMenuEditText != null)
                    mSearchMenuEditText.setText("");

                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                return true;
            }
        });

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

        // add EndlessScrollListener that loads more submissions when the scroll position reaches close to the end
        mListView.setOnScrollListener(new EndlessScrollListener(mLoadMoreThreshold) {

            @Override
            public void onLoadMore(int page, int totalItemsCount) {
                Submission lastSubmission = (Submission)mAdapter.getItem(totalItemsCount-1);

                // passing empty string requests for the reddit frontpage
                SessionManager.SubmissionFetchParam param = new SessionManager.SubmissionFetchParam();
                param.subreddit = mSubredditName;
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

        ProgressBar progressBar = (ProgressBar)view.findViewById(android.R.id.empty);
        mListView.setEmptyView(progressBar);


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



    /*
         * public methods
         */
    public void refresh(String subredditName) {
        if (!mSubredditName.equals(subredditName)) {
            mSubredditName = subredditName;
            initList();
        }
    }

    public void refresh(SubmissionSort sort) {
        if (mSubmissionSort != sort) {
            mSubmissionSort = sort;
            initList();
        }
    }

    public void refresh(String subredditName, SubmissionSort sort) {
        if (!mSubredditName.equals(subredditName) || mSubmissionSort != sort) {
            mSubredditName = subredditName;
            mSubmissionSort = sort;
            initList();
        }

    }

/*
 * private methods
 */
    private void initList() {
        mPagedSubmissionsList.clear();
        mAdapter.notifyDataSetChanged();

        if (mSubredditName.isEmpty()) {
            // default to front page
            ((ActionBarActivity) mContext).getSupportActionBar().setTitle("front page");
        } else {
            ((ActionBarActivity) mContext).getSupportActionBar().setTitle(mSubredditName);
        }

        // passing empty string requests for the reddit frontpage
        SessionManager.SubmissionFetchParam param = new SessionManager.SubmissionFetchParam();
        param.subreddit = mSubredditName;
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

    /**
     * hides the soft keyboard (the only ui element that can bring up the keyboard in this fragment is mSearchMenuEditText)
     */
    private void hideKeyboard() {
        // hide the soft keyboard
        InputMethodManager manager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        manager.hideSoftInputFromWindow(mSearchMenuEditText.getWindowToken(), 0);
    }

}
