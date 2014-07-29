package com.danny_oh.reddit.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;


import com.danny_oh.reddit.R;
import com.danny_oh.reddit.activities.MainActivity;
import com.danny_oh.reddit.adapters.SubredditAdapter;
import com.danny_oh.reddit.tasks.SubredditSearchTask;
import com.github.jreddit.entity.Subreddit;
import com.github.jreddit.retrieval.Subreddits;
import com.github.jreddit.retrieval.params.SubredditsView;
import com.github.jreddit.utils.restclient.PoliteHttpRestClient;

import java.util.List;

/**
 * A fragment representing a list of Items.
 * <p />
 * Large screen devices (such as tablets) are supported by replacing the ListView
 * with a GridView.
 * <p />
 * Activities containing this fragment MUST implement the {@link Callbacks}
 * interface.
 */
public class SubredditFragment extends Fragment implements AbsListView.OnItemClickListener,
        SubredditSearchTask.SubredditSearchListener{

    private static final String TAG = "Reddit - SubredditFragment";

    private List<Subreddit> mSubredditList;
    private SubredditAdapter mSubredditAdapter;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

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
            Subreddits subreddits = new Subreddits(new PoliteHttpRestClient());
            mSubredditList = subreddits.get(view, 0, 25, null, null);

            return null;
        }

        protected void onPostExecute(Void v) {
            mSubredditAdapter = new SubredditAdapter(getActivity(), R.layout.list_item_subreddit, R.id.submission_score, mSubredditList);
            mListView.setAdapter(mSubredditAdapter);
            mSubredditAdapter.notifyDataSetChanged();
        }
    }

    // TODO: Rename and change types of parameters
    public static SubredditFragment newInstance(String param1, String param2) {
        SubredditFragment fragment = new SubredditFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

        // disabled for side drawer integration
//        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_subreddit_list, container, false);

        // Set the adapter
        mListView = (ListView)view.findViewById(R.id.subreddit_list_listview);

        // Set OnItemClickListener so we can be notified on item clicks
        mListView.setOnItemClickListener(this);

        mSearchSubredditEditText = (EditText)view.findViewById(R.id.search_subreddits);
        mSearchSubredditEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                // if 'return' key was pressed
                if (keyEvent.getAction() == KeyEvent.ACTION_DOWN && keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                    mSearchSubredditEditText.clearFocus();
                    mSubredditsTask = new SubredditSearchTask(mSearchSubredditEditText.getText().toString(),SubredditFragment.this).execute();

                    return true;
                }

                return false;
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

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        mSubredditsTask = new GetSubredditsTask(SubredditsView.POPULAR).execute();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = (MainActivity)activity;

        // TODO: may want to specify listener for subreddit fragment, maybe the subreddit list will be displayed somewhere else too?
        try {
            mListener = (DrawerMenuFragment.OnDrawerMenuInteractionListener)activity;
        } catch (ClassCastException e) {
            e.printStackTrace();
            throw new ClassCastException("The activity containing SubredditFragment must implement OnDrawerMenuInteractionListener");
        }


    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
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
        mListener.onSubredditClick(mSubredditAdapter.getItem(position).getDisplayName());

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

                mSubredditsTask = new SubredditSearchTask(input.getText().toString(),SubredditFragment.this).execute();
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
