package com.danny_oh.reddit.fragments;

import android.app.Activity;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import com.danny_oh.reddit.adapters.CommentMapAdapter;
import com.danny_oh.reddit.util.CommentsListHelper;
import com.github.jreddit.entity.Comment;
import com.github.jreddit.retrieval.Comments;
import com.github.jreddit.retrieval.params.CommentSort;
import com.github.jreddit.utils.restclient.PoliteHttpRestClient;

import java.util.HashMap;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link CommentListFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link CommentListFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
public class CommentListFragment extends ListFragment {
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_SUBMISSION_ID = "submission_id";

    // the fullname of a reddit 'Thing'
    private String mSubmissionId;
    private CommentSort mCommentSort = CommentSort.CONFIDENCE;


    private List<Comment> mCommentsList;

    private HashMap<Integer, CommentsListHelper.CommentContainer> mCommentMap;

    private OnFragmentInteractionListener mListener;


    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onFragmentInteraction(Uri uri);
    }



    private class GetCommentsAsyncTask extends AsyncTask<String, Integer, Void> {
        @Override
        protected Void doInBackground(String... submissionId) {

            // TODO: when user login is implemented, check if user is logged in and pass in place of {null}
            Comments comments = new Comments(new PoliteHttpRestClient(), null);

            // params: submissionId, commentId, parentsShown, depth, limit, CommentSort
            mCommentsList = comments.ofSubmission(submissionId[0], null, -1, -1, -1, mCommentSort);
            mCommentMap = CommentsListHelper.listToMap(mCommentsList);

            Log.d("CommentListFragment", "mCommentList count: " + mCommentsList.size());
            Log.d("CommentListFragment", "mCommentMap count: " + mCommentMap.size());

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
//            setListAdapter(new CommentAdapter(getActivity(), mCommentsList));
            setListAdapter(new CommentMapAdapter(getActivity(), mCommentMap));
        }
    }





    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param submissionId Submission 'id' of a reddit link. Note this is not the 'fullname' (i.e. excludes the type parameter).
     * @return A new instance of fragment CommentListFragment.
     */
    public static CommentListFragment newInstance(String submissionId) {
        CommentListFragment fragment = new CommentListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SUBMISSION_ID, submissionId);
        fragment.setArguments(args);
        return fragment;
    }
    public CommentListFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mSubmissionId = getArguments().getString(ARG_SUBMISSION_ID);
        } else {
            throw new InstantiationException("Use factory method newInstance to instantiate fragment.", new Exception());
        }

        new GetCommentsAsyncTask().execute(mSubmissionId);

        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        view.setBackgroundColor(Color.argb(255, 255, 255, 255));

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
//        try {
//            mListener = (OnFragmentInteractionListener) activity;
//        } catch (ClassCastException e) {
//            throw new ClassCastException(activity.toString()
//                    + " must implement OnFragmentInteractionListener");
//        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


}
