package com.danny_oh.reddit;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;


import com.github.jreddit.entity.Submission;
import com.github.jreddit.retrieval.Submissions;
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
public class SubmissionListFragment extends Fragment implements AbsListView.OnItemClickListener {

    private OnSubmissionListFragmentInteractionListener mListener;
    private List<Submission> mSubmissionsList;



    /**
     * The fragment's ListView/GridView.
     */
    private ListView mListView;

    /**
     * The Adapter which will be used to populate the ListView/GridView with
     * Views.
     */
    private ListAdapter mAdapter;


    /**
     * AsyncTask subclass that retrieves submissions from a specified subreddit
     */
    private class GetSubmissionsAsyncTask extends AsyncTask<String, Integer, Void> {
        private AbsListView.OnItemClickListener mListener;

        public GetSubmissionsAsyncTask(AbsListView.OnItemClickListener listener) {
            mListener = listener;
        }

        protected Void doInBackground(String... subreddit) {
            Submissions submissions = new Submissions(new PoliteHttpRestClient());
            mSubmissionsList = submissions.parse("/.json");

            return null;
        }

        protected void onPostExecute(Void v) {
            mListView.setAdapter(new SubmissionAdapter(getActivity(), mSubmissionsList));
            mListView.setOnItemClickListener(mListener);
        }
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
    }



    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (null != mListener) {
            // Notify the active callbacks interface (the activity, if the
            // fragment is attached to one) that an item has been selected.

            Submission submission = mSubmissionsList.get(position);

            mListener.onSubmissionClick(submission);
        }
    }



    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public SubmissionListFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // passing empty string requests for the reddit frontpage
        new GetSubmissionsAsyncTask(this).execute("");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_submission_list, container, false);

        // Set the adapter
        mListView = (ListView)view.findViewById(android.R.id.list);

        // Set OnItemClickListener so we can be notified on item clicks
        mListView.setOnItemClickListener(this);

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        Log.d("SubmissionListFragment", "onAttach()");

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


}
