package com.danny_oh.reddit.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.danny_oh.reddit.R;
import com.danny_oh.reddit.util.Constants;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerSupportFragment;

/**
 * Created by danny on 8/2/14.
 */
public class YouTubeSubmissionFragment extends Fragment implements YouTubePlayer.OnInitializedListener,
        YouTubePlayer.PlayerStateChangeListener
{
    // newInstance constructor argument key
    private static final String ARG_VIDEO_ID = "youtube_video_id";

    private static final String YOUTUBE_FRAGMENT_BUNDLE_KEY = "youtube_fragment_bundle_key";

    private static final String YOUTUBE_FRAGMENT_TRANSACTION_TAG = "youtube_fragment_transaction_tag";


    private String mVideoId;

    private YouTubePlayer mYouTubePlayer;





    public static YouTubeSubmissionFragment newInstance(String videoId) {
        YouTubeSubmissionFragment fragment = new YouTubeSubmissionFragment();

        Bundle args = new Bundle();
        args.putString(ARG_VIDEO_ID, videoId);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mVideoId = getArguments().getString(ARG_VIDEO_ID);
        } else {
            throw new InstantiationException("Fragments must be instantiated using factory method newInstance.", new Exception());
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_youtube_submission, container, false);

        YouTubePlayerSupportFragment youTubePlayerFragment;

        if (savedInstanceState != null) {
            Log.d("YouTubeSubmissionFragment", "savedInstanceState is not null. Restoring fragment.");
            youTubePlayerFragment = (YouTubePlayerSupportFragment)getFragmentManager().getFragment(savedInstanceState, YOUTUBE_FRAGMENT_BUNDLE_KEY);

        } else {
            Log.d("YouTubeSubmissionFragment", "savedInstanceState is null. Initializing fragment.");
            youTubePlayerFragment = new YouTubePlayerSupportFragment();

            getFragmentManager().beginTransaction()
                    .replace(R.id.youtube_container, youTubePlayerFragment, YOUTUBE_FRAGMENT_TRANSACTION_TAG)
                    .commit();
        }

        youTubePlayerFragment.initialize(Constants.GOOGLE_API_KEY, this);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.d("YouTubeSubmissionFragment", "onSaveInstanceState(). Saving YouTubePlayerSupportFragment.");
        super.onSaveInstanceState(outState);

        YouTubePlayerSupportFragment fragment = (YouTubePlayerSupportFragment)getFragmentManager().findFragmentByTag(YOUTUBE_FRAGMENT_TRANSACTION_TAG);

        if (fragment != null)
            getFragmentManager().putFragment(outState, YOUTUBE_FRAGMENT_BUNDLE_KEY, fragment);
    }

    @Override
    public void onDestroyView() {
        mYouTubePlayer.setFullscreen(false);
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    /*
 * YouTubePlayer Interface Implementations
 */

/* OnInitializedListener */
    @Override
    public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer youTubePlayer, boolean wasRestored) {
        mYouTubePlayer = youTubePlayer;

//        youTubePlayer.setPlayerStateChangeListener(this);

        if (!wasRestored) {
            Log.d("YouTubeSubmissionFragment", "YouTubePlayer was not restored. Initializing video.");
            youTubePlayer.loadVideo(mVideoId);

        } else {
            Log.d("YouTubeSubmissionFragment", "YouTubePlayer was restored. Resuming video.");
        }
    }

    @Override
    public void onInitializationFailure(YouTubePlayer.Provider provider, YouTubeInitializationResult youTubeInitializationResult) {
        Log.d("SubmissionFragment", "Failed to initialize YouTubePlayer.");

        Toast.makeText(getActivity(), "Failed to initialize YouTube Player. Please try again later.", Toast.LENGTH_SHORT).show();
    }

/* OnPlayerStateChangeListener */

    @Override
    public void onAdStarted() {

    }

    @Override
    public void onError(YouTubePlayer.ErrorReason errorReason) {

    }

    @Override
    public void onLoading() {

    }

    @Override
    public void onLoaded(String s) {
        // auto play
        mYouTubePlayer.play();
    }

    @Override
    public void onVideoStarted() {

    }

    @Override
    public void onVideoEnded() {

    }
}
