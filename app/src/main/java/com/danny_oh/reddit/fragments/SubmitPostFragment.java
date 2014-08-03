package com.danny_oh.reddit.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.danny_oh.reddit.R;
import com.danny_oh.reddit.SessionManager;
import com.github.jreddit.action.SubmitActions;

/**
 * Created by allen on 8/2/14.
 */
public class SubmitPostFragment extends Fragment {

    public static final String SUBREDDIT_NAME_KEY = "Subreddit_Name_Key";
    public static final String POST_TYPE_KEY = "Subreddit_Post_Type_Key";

    private PostType mPostType;
    private String mSubredditName = "";

    EditText mTitleEditText;
    EditText mUrlEditText;
    EditText mTextPostEditText;
    EditText mSubredditEditText;

    Activity mActivity;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = activity;
    }

    public static enum PostType {
        Self, Link, Comment
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mPostType = PostType.values()[getArguments().getInt(POST_TYPE_KEY)];
            if (getArguments().containsKey(SUBREDDIT_NAME_KEY)) {
                mSubredditName = getArguments().getString(SUBREDDIT_NAME_KEY);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.post_submission_fragment, container, false);

        mTitleEditText = (EditText) view.findViewById(R.id.submission_title_ET);
        mUrlEditText = (EditText) view.findViewById(R.id.submission_url_ET);
        mTextPostEditText = (EditText) view.findViewById(R.id.submission_content_ET);
        mSubredditEditText = (EditText) view.findViewById(R.id.submission_subreddit_ET);

        if (mSubredditName != null) {
            mSubredditEditText.setText(mSubredditName);
        }

        determineViewsShown();

        setHasOptionsMenu(true);

        return view;
    }

    public static SubmitPostFragment newInstance(PostType postType, String subreddit) {
        SubmitPostFragment fragment = new SubmitPostFragment();
        Bundle args = new Bundle();
        int type = postType.ordinal();
        args.putInt(POST_TYPE_KEY, type);
        if (subreddit != null) {
            args.putString(SUBREDDIT_NAME_KEY, subreddit);
        }
        fragment.setArguments(args);

        return fragment;
    }

    private void determineViewsShown() {
        switch (mPostType) {
            case Self:
                mUrlEditText.setVisibility(View.GONE);
                break;

            case Link:
                mTextPostEditText.setVisibility(View.GONE);
                break;

            case Comment:
                mTitleEditText.setVisibility(View.GONE);
                mUrlEditText.setVisibility(View.GONE);
                mSubredditEditText.setVisibility(View.GONE);
                break;
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.post_submission_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_send:
            //TODO
//                UserActionsServices.getInstance().submitPost();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }
}
