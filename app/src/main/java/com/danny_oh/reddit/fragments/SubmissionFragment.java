package com.danny_oh.reddit.fragments;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.danny_oh.reddit.R;
import com.danny_oh.reddit.util.ExtendedSubmission;
import com.github.jreddit.entity.Submission;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerSupportFragment;


/**
 * A {@link Fragment} subclass for viewing submission links.
 * Use the {@link SubmissionFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
public class SubmissionFragment extends Fragment {
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_SUBMISSION = "submission";

    // for saving instance states
    private static final String YOUTUBE_PLAYER_MILLIS_KEY = "youtube_player_current_millis";

    private WebView mWebView;

    private Submission mSubmission;

    private YouTubePlayerSupportFragment mYouTubeFragment;
    private YouTubePlayer mYouTubePlayer;

    private boolean mWebViewFinishedLoading = false;

    private SubmissionFragmentListener mListener;


    public interface SubmissionFragmentListener {
        public void onRequestShowSideMenuComments();
    }


    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment SubmissionFragment.
     */
    public static SubmissionFragment newInstance(ExtendedSubmission submission) {
        SubmissionFragment fragment = new SubmissionFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_SUBMISSION, submission);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * empty public constructor for fragment reinitialization by Android
     */
    public SubmissionFragment() {

    }

    /*
     * Fragment Lifecycle Methods
     *
     */
    @Override
    public void onAttach(Activity activity) {
        Log.d("SubmissionFragment", "onAttach() called.");
        super.onAttach(activity);

        try {
            mListener = (SubmissionFragmentListener)activity;
        } catch (ClassCastException e) {
            throw new ClassCastException("Parent activity must implement SubmissionFragmentListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d("SubmissionFragment", "onCreate");

        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mSubmission = (ExtendedSubmission)getArguments().getParcelable(ARG_SUBMISSION);
        } else {
            throw new InstantiationException("Fragments must be instantiated using factory method newInstance.", new Exception());
        }

        // has to be set in order to display fragment's own options menu (i.e. calling onCreateOptionsMenu)
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_submission, container, false);

        mWebView = (WebView) view.findViewById(R.id.submission_webview);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null && mWebView.restoreState(savedInstanceState) != null) {
            Log.d("SubmissionFragment", "savedInstanceState is not null. Restoring WebView state.");

        } else {

            mWebView.setInitialScale(30);

            WebSettings webSettings = mWebView.getSettings();

            webSettings.setUseWideViewPort(true);
            webSettings.setLoadWithOverviewMode(true);

            webSettings.setSupportZoom(true);
            webSettings.setBuiltInZoomControls(true);
            webSettings.setJavaScriptEnabled(true);

            // setDisplayZoomControls(boolean) is available since API 11
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                setWebViewDisplayZoomControls(webSettings, false);

            final Activity activity = getActivity();

            mWebView.setWebChromeClient(new WebChromeClient() {
                @Override
                public void onProgressChanged(WebView view, int newProgress) {
                    // param 'newProgress' ranges from 0 - 100
                    // activity.setProgress has range 0 - 10000
                    activity.setProgress(newProgress * 100);

//                Log.d("SubmissionFragment", "WebView progress: " + newProgress);
                }

            });



            mWebView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    mWebViewFinishedLoading = true;
                }

                @Override
                public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                    mWebView.stopLoading();
                    activity.setProgress(10000);
                    Log.e("SubmissionFragment", "WebView received error.");

                    Toast.makeText(getActivity(), "Network error. Please check you internet connection and try again.", Toast.LENGTH_SHORT).show();
                }
            });

            mWebView.loadUrl(mSubmission.getUrl());
        }
    }

    @TargetApi(11)
    private void setWebViewDisplayZoomControls(WebSettings settings, boolean show) {
        settings.setDisplayZoomControls(show);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mWebViewFinishedLoading) {
            mWebView.saveState(outState);
        }
    }

    @Override
    public void onResume() {
        Log.d("SubmissionFragment", "onResume");

        super.onResume();
        ((ActionBarActivity)getActivity()).getSupportActionBar().setTitle(mSubmission.getTitle());
    }

    @Override
    public void onPause() {
        Log.d("SubmissionFragment", "onPause");

        super.onPause();
    }

    @Override
    public void onStop() {
        Log.d("SubmissionFragment", "onStop");

        super.onStop();
    }

    @Override
    public void onDestroy() {
        Log.d("SubmissionFragment", "onDestroy");

        super.onDestroy();

        // stop WebView loading and reset the progress bar on go back
        mWebView.destroy();
        getActivity().setProgress(10000);
    }

    @Override
    public void onDetach() {
        Log.d("SubmissionFragment", "onDetach() called.");
        super.onDetach();
    }


    /*
     * Options Menu Methods
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.submission_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d("SubmissionFragment", "onOptionsItemSelected called.");

        int id = item.getItemId();

        switch (id) {
            case R.id.show_comments:
//                getFragmentManager()
//                        .beginTransaction()
//                        .addToBackStack(null)
//                        .replace(R.id.content_frame, CommentsOnlyListFragment.newInstance((ExtendedSubmission) mSubmission))
//                        .commit();
                mListener.onRequestShowSideMenuComments();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }




}
