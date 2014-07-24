package com.danny_oh.reddit.fragments;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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


/**
 * A {@link Fragment} subclass for viewing submission links.
 * Use the {@link SubmissionFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
public class SubmissionFragment extends Fragment {
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_URL = "url";
    private static final String ARG_SUBMISSION = "submission";

    private String mUrl;
    private WebView mWebView;

    private Submission mSubmission;


    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param url URL of submission link.
     * @return A new instance of fragment SubmissionFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static SubmissionFragment newInstance(ExtendedSubmission submission) {
        SubmissionFragment fragment = new SubmissionFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_SUBMISSION, submission);
        args.putString(ARG_URL, submission.getURL());
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
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mUrl = getArguments().getString(ARG_URL);
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

        mWebView = (WebView)view.findViewById(R.id.submission_webview);
        WebSettings webSettings = mWebView.getSettings();

        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setJavaScriptEnabled(true);

        // setDisplayZoomControls(boolean) is available since API 11
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            webSettings.setDisplayZoomControls(false);


        final Activity activity = getActivity();

        mWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                // param 'newProgress' ranges from 0 - 100
                // activity.setProgress has range 0 - 10000
                activity.setProgress(newProgress * 100);

                Log.d("SubmissionFragment", "WebView progress: " + newProgress);
            }

        });

        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                mWebView.stopLoading();
                activity.setProgress(10000);
                Log.e("SubmissionFragment", "WebView received error.");

                Toast.makeText(getActivity(), "Network error. Please check you internet connection and try again.", Toast.LENGTH_SHORT);
            }
        });

        mWebView.loadUrl(mUrl);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // stop WebView loading and reset the progress bar on go back
        mWebView.destroy();
        getActivity().setProgress(10000);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.d("SubmissionFragment", "onDetach() called.");
    }


    /*
     * Options Menu Methods
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.submission_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.show_comments:
                getFragmentManager()
                        .beginTransaction()
                        .addToBackStack(null)
                        .add(R.id.content_frame, CommentListFragment.newInstance(mSubmission.getIdentifier()))
                        .commit();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }




}
