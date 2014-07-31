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
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;

import com.danny_oh.reddit.R;
import com.danny_oh.reddit.SessionManager;
import com.danny_oh.reddit.adapters.CommentMapAdapter;
import com.danny_oh.reddit.util.CommentsListHelper;
import com.danny_oh.reddit.util.ExtendedSubmission;
import com.danny_oh.reddit.util.RedditRestClient;
import com.github.jreddit.entity.Comment;
import com.github.jreddit.entity.Submission;
import com.github.jreddit.retrieval.Comments;
import com.github.jreddit.retrieval.params.CommentSort;
import com.github.jreddit.utils.restclient.PoliteHttpRestClient;

import java.util.HashMap;
import java.util.List;


public class CommentListFragment extends ListFragment {
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_SUBMISSION = "submission";

    // the fullname of a reddit 'Thing'
    private String mSubmissionId;
    private CommentSort mCommentSort = CommentSort.CONFIDENCE;

    private GetCommentsAsyncTask mCommentTask;

    private List<Comment> mCommentsList;

    private HashMap<Integer, CommentsListHelper.CommentContainer> mCommentMap;

    private Submission mSubmission;


    private class GetCommentsAsyncTask extends AsyncTask<String, Integer, Void> {
        @Override
        protected Void doInBackground(String... submissionId) {
            SessionManager manager = SessionManager.getInstance(getActivity());

            Comments comments = new Comments(manager.getRestClient(), manager.getUser());

            // params: submissionId, commentId, parentsShown, depth, limit, CommentSort
            mCommentsList = comments.ofSubmission(submissionId[0], null, -1, -1, -1, mCommentSort);
            mCommentMap = CommentsListHelper.listToMap(mCommentsList);

            Log.d("CommentListFragment", "mCommentList count: " + mCommentsList.size());
            Log.d("CommentListFragment", "mCommentMap count: " + mCommentMap.size());

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            mCommentsList = null;
            setListAdapter(new CommentMapAdapter(getActivity(), mCommentMap, mSubmission));
            ((CommentMapAdapter)getListAdapter()).notifyDataSetChanged();
        }
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
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param submission Parcelable Submission of a reddit link.
     * @return A new instance of fragment CommentListFragment.
     */
    public static CommentListFragment newInstance(ExtendedSubmission submission) {
        CommentListFragment fragment = new CommentListFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_SUBMISSION, submission);
        fragment.setArguments(args);
        return fragment;
    }
    public CommentListFragment() {
        // Required empty public constructor
        mCommentMap = new HashMap<Integer, CommentsListHelper.CommentContainer>();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mSubmission = (ExtendedSubmission)getArguments().getParcelable(ARG_SUBMISSION);
        } else {
            throw new InstantiationException("Use factory method newInstance to instantiate fragment.", new Exception());
        }

        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.comments_menu, menu);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_comment_list, container, false);

        ListView listView = (ListView)view.findViewById(android.R.id.list);
        listView.setEmptyView(view.findViewById(android.R.id.empty));

        initList();

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
    public void onDestroyView() {
        Log.d("CommentListFragment", "onDestroyView called.");
        super.onDestroyView();
        mCommentTask.cancel(true);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    private void initList() {
        if (!mCommentMap.isEmpty()) {
            mCommentMap.clear();
            ((CommentMapAdapter)getListAdapter()).notifyDataSetChanged();
        }

        mCommentTask = new GetCommentsAsyncTask();
        mCommentTask.execute(mSubmission.getIdentifier());
    }

}
