package com.danny_oh.reddit.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


import com.danny_oh.reddit.R;
import com.danny_oh.reddit.SessionManager;
import com.danny_oh.reddit.activities.MainActivity;
import com.danny_oh.reddit.adapters.SubredditAdapter;
import com.danny_oh.reddit.tasks.SubredditSearchTask;
import com.github.jreddit.entity.Subreddit;
import com.github.jreddit.exception.RetrievalFailedException;
import com.github.jreddit.retrieval.Subreddits;
import com.github.jreddit.retrieval.params.SubredditsView;
import com.github.jreddit.utils.restclient.PoliteHttpRestClient;

import java.util.LinkedList;
import java.util.List;

/**
 * A fragment that displays subreddits in a simple ListView
 */
public class SubredditFragment extends Fragment implements AbsListView.OnItemClickListener,
        SubredditSearchTask.SubredditSearchListener{

    private static final String TAG = "Reddit - SubredditFragment";

    private List<Subreddit> mSubredditList;
    private SubredditAdapter mSubredditAdapter;



    private MainActivity mActivity;
    private DrawerMenuFragment.OnDrawerMenuInteractionListener mListener;

    private EditText mSearchSubredditEditText;

    /**
     * The fragment's ListView/GridView.
     */
    private ListView mListView;

    AsyncTask mSubredditsTask;


    private class GetSubredditsTask extends AsyncTask<Void, Integer, Void> {
        SubredditsView view;

        protected GetSubredditsTask(SubredditsView subredditsView){
            super();
            view = subredditsView;
        }
        protected Void doInBackground(Void... voids) {
            Log.d("SubredditFragment", "Fetching subreddits.");
            Subreddits subreddits;

            SessionManager manager = SessionManager.getInstance(getActivity());

            if (manager.isUserLoggedIn()) {
                subreddits = new Subreddits(manager.getRestClient(), manager.getUser());
            } else {
                subreddits = new Subreddits(manager.getRestClient());
            }

            try {
                mSubredditList = subreddits.get(view, 0, 30, null, null);
            } catch (RetrievalFailedException e) {
                Log.e("SubredditFragment", "Failed to fetch subreddits. Localized message: " + e.getLocalizedMessage());

                mSubredditList = new LinkedList<Subreddit>();

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getActivity(), "Failed to fetch links. Please try again later.", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            return null;
        }

        protected void onPostExecute(Void v) {
            mSubredditAdapter = new SubredditAdapter(getActivity(), R.layout.list_item_subreddit, R.id.submission_score, mSubredditList);
            mListView.setAdapter(mSubredditAdapter);
            mSubredditAdapter.notifyDataSetChanged();
        }
    }

    // TODO: Rename and change types of parameters
    public static SubredditFragment newInstance() {
        SubredditFragment fragment = new SubredditFragment();
        Bundle args = new Bundle();

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d("SubredditFragment", "onCreate()");
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {

        }

        setRetainInstance(true);

        // disabled for side drawer integration
//        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d("SubredditFragment", "onCreateView()");

        View view = inflater.inflate(R.layout.fragment_subreddit_list, container, false);

        // Set the adapter
        mListView = (ListView)view.findViewById(R.id.subreddit_list_listview);

        mSearchSubredditEditText = (EditText)view.findViewById(R.id.search_subreddits);

        mListView.addHeaderView(inflater.inflate(R.layout.list_header_subreddit, mListView, false));

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.d("SubredditFragment", "onActivityCreated()");

        super.onActivityCreated(savedInstanceState);

        if (mSubredditList != null && mSubredditAdapter != null) {
            // fragment was restored
            mListView.setAdapter(mSubredditAdapter);
        } else {
            mSubredditsTask = new GetSubredditsTask(SubredditsView.POPULAR).execute();
        }

        // Set OnItemClickListener so we can be notified on item clicks
        mListView.setOnItemClickListener(this);

        mSearchSubredditEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                mSubredditsTask = new SubredditSearchTask(getActivity(), mSearchSubredditEditText.getText().toString(),SubredditFragment.this).execute();
                mSearchSubredditEditText.clearFocus();
                return true;
            }
        });
        mSearchSubredditEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (b) {
                    mSearchSubredditEditText.setText("");
                } else {
                    hideKeyboard();
                }
            }
        });
    }

    @Override
    public void onResume() {
        Log.d("SubredditFragment", "onResume()");
        super.onResume();
    }

    @Override
    public void onAttach(Activity activity) {
        Log.d("SubredditFragment", "onAttach()");
        super.onAttach(activity);
        mActivity = (MainActivity)activity;

        // TODO: may want to specify listener for subreddit fragment, maybe the subreddit list will be displayed somewhere else too?
        try {
            mListener = (DrawerMenuFragment.OnDrawerMenuInteractionListener)activity;
        } catch (ClassCastException e) {
            throw new ClassCastException("The activity containing SubredditFragment must implement OnDrawerMenuInteractionListener");
        }


    }

    @Override
    public void onDetach() {
        Log.d("SubredditFragment", "onDetach()");
        super.onDetach();
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.d("SubredditFragment", "onSaveInstanceState()");
        super.onSaveInstanceState(outState);
    }




    /**
     * OnItemClickListener for mListView
     * @param parent
     * @param view
     * @param position
     * @param id
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (position == 0) {
            // front page was selected
            mListener.onSubredditClick("");
        } else {
            // changed getting Subreddit from adapter because the "front page" header shifts the position of the list items
            // down by one. By calling getItemAtPosition on the list view, the correct Subreddit is retrieved from the list.
            mListener.onSubredditClick(((Subreddit) mListView.getItemAtPosition(position)).getDisplayName());
        }

//        SubmissionListFragment fragment = SubmissionListFragment.newInstance(mSubredditAdapter.getItem(position).getDisplayName());
//        Log.d(TAG,mSubredditList.get(position).getDisplayName());
//        mActivity.showFragment(fragment,true);
    }

    /**
     * The default content for this Fragment has a TextView that is shown when
     * the list is empty. If you would like to change the text, call this method
     * to supply the text it should use.
     */
    public void setEmptyText(CharSequence emptyText) {
        View emptyView = mListView.getEmptyView();

        if (emptyText instanceof TextView) {
            ((TextView) emptyView).setText(emptyText);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.subreddits_menu,menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.search_subreddits:
                showBuilder();
                break;

            case R.id.new_subreddits:
                mSubredditsTask = new GetSubredditsTask(SubredditsView.NEW).execute();
                break;

            case R.id.popular_subreddits:
                mSubredditsTask = new GetSubredditsTask(SubredditsView.POPULAR).execute();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSearchFinished(List<Subreddit> list) {
        setupAdapter(list);
    }

    private void setupAdapter(List<Subreddit> subreddits){
        mSubredditAdapter = new SubredditAdapter(getActivity(), R.layout.list_item_subreddit, R.id.submission_score, subreddits);
        mListView.setAdapter(mSubredditAdapter);
        mSubredditAdapter.notifyDataSetChanged();
    }

    private void showBuilder(){
        AlertDialog.Builder alert = new AlertDialog.Builder(mActivity);

        alert.setTitle(getString(R.string.search_subreddits));
        final EditText input = new EditText(mActivity);
        alert.setView(input);
        alert.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

                mSubredditsTask = new SubredditSearchTask(getActivity(), input.getText().toString(),SubredditFragment.this).execute();
                dialog.dismiss();
            }
        });

        alert.show();
    }

    private void hideKeyboard() {
        InputMethodManager manager = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        manager.hideSoftInputFromWindow(mSearchSubredditEditText.getWindowToken(), 0);
    }
}
