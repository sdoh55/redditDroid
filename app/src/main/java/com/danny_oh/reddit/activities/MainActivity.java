package com.danny_oh.reddit.activities;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.danny_oh.reddit.SessionManager;
import com.danny_oh.reddit.adapters.SubmissionAdapter;
import com.danny_oh.reddit.fragments.CommentsListFragment;
import com.danny_oh.reddit.fragments.DrawerMenuFragment;
import com.danny_oh.reddit.R;
import com.danny_oh.reddit.fragments.LoginDialogFragment;
import com.danny_oh.reddit.fragments.SearchSubmissionsFragment;
import com.danny_oh.reddit.fragments.SubmissionFragment;
import com.danny_oh.reddit.fragments.SubmissionsListFragment;
import com.danny_oh.reddit.fragments.YouTubeSubmissionFragment;
import com.danny_oh.reddit.util.ExtendedSubmission;
import com.github.jreddit.entity.Submission;
import com.github.jreddit.entity.User;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;
import com.jeremyfeinstein.slidingmenu.lib.app.SlidingActivity;

import java.util.Locale;

/**
 * The main Activity of the redditDroid app.
 *
 * This app has only one activity on purpose, in order to minimize load times and deliver the fastest
 * user experience possible.
 */
public class MainActivity
        extends SlidingActivity
        implements FragmentManager.OnBackStackChangedListener,              // handles changes to fragment stack
        SubmissionsListFragment.OnSubmissionsListInteractionListener,
        DrawerMenuFragment.OnDrawerMenuInteractionListener,                 // handles side drawer menu item clicks
        LoginDialogFragment.LoginDialogListener,                            // handles user login dialog interaction
        CommentsListFragment.OnCommentsListFragmentDetachListener,
        // listener for items contained inside each individual submissions list view cell (e.g. up/down vote buttons)
        SubmissionAdapter.OnSubmissionAdapterInteractionListener,
        SubmissionFragment.SubmissionFragmentListener
{

    public static final String COMMENTS_LIST_FRAGMENT_TRANSACTION_TAG = "comments_list_fragment_transaction";
    private static final String LAST_SUBMISSION_CLICKED_SAVE_INSTANCE_KEY = "last_submission_clicked";
    private static final String SUBMISSIONS_LIST_FRAGMENT_TAG = "submissions_list_fragment_key";

    private static final String COMMENTS_SECONDARY_MENU_TRANSACTION_TAG = "comments_secondary_menu_transaction";

    private SlidingMenu mSlidingMenu;
    private FragmentManager mFragmentManager;

    private SessionManager mSessionManager;

    private Submission mLastSubmissionClicked;



/*
 * OnSubmissionAdapterInteractionListener interface implementation
 */
    @Override
    public void onSubmissionClick(Submission submission, int position) {
        mLastSubmissionClicked = submission;
        mLastSubmissionClicked.setVisited(true);

        Log.d("MainActivity", "Received fragment interaction. Selection: " + mLastSubmissionClicked.getFullName());

        if (mLastSubmissionClicked.isSelf()) {
            CommentsListFragment commentsListFragment = CommentsListFragment.newInstance(new ExtendedSubmission(mLastSubmissionClicked));
            mFragmentManager
                    .beginTransaction()
                    .addToBackStack(null)
                    .replace(R.id.content_frame, commentsListFragment, COMMENTS_LIST_FRAGMENT_TRANSACTION_TAG)
                    .commit();

        } else {

            if (mLastSubmissionClicked.getDomain().equals("youtube.com") || mLastSubmissionClicked.getDomain().equals("youtu.be")) {

                int index;
                int endIndex;
                int secondsEndIndex;

                String url = mLastSubmissionClicked.getUrl();


                final String videoId;
                int playOffset = 0;

                if (mLastSubmissionClicked.getDomain().equals("youtube.com")) {
                    // standard youtube link format: http://www.youtube.com/watch?v=LkVZhEtaQCA
                    index = url.indexOf("watch?v=");

                    if (index > 0) {
                        // url is standard text format

                        // skip the string "watch?v="
                        index += 8;

                        // strip url parameters if any
                        endIndex = url.indexOf('&', index);

                    } else {
                        // url symbols are encoded in hex ASCII
                        index = url.toLowerCase(Locale.US).indexOf("watch%3fv%3d");
                        index += 12;

                        // strip off params if any
                        endIndex = url.indexOf('%', index);
                    }


                    if (endIndex < 0) {
                        videoId = url.substring(index);
                    } else {
                        videoId = url.substring(index, endIndex);
                    }


                } else {
                    // youtube link format: http://www.youtu.be/LkVZhEtaQCA
                    index = mLastSubmissionClicked.getUrl().indexOf(".be/");
                    index += 4;


                    // extract the 't' parameter if it exists
                    endIndex = mLastSubmissionClicked.getUrl().indexOf("?t=", index);
                    if (endIndex > 0) {
                        videoId = url.substring(index, endIndex);

                        endIndex += 3;
                        secondsEndIndex = mLastSubmissionClicked.getUrl().indexOf('s', endIndex);

                        try {
                            // get the offset in seconds
                            playOffset = Integer.parseInt(url.substring(endIndex, secondsEndIndex));
                            // convert to millis
                            playOffset *= 1000;
                        } catch (NumberFormatException e) {
                            Log.e("MainActivity", "Failed to parse youtu.be url. Stack trace: " + e.getLocalizedMessage());
                            playOffset = 0;
                        }
                    } else {

                        videoId = url.substring(index);

                    }
                }


                YouTubeSubmissionFragment submissionFragment = YouTubeSubmissionFragment.newInstance(videoId, mLastSubmissionClicked.getTitle(), playOffset);
                mFragmentManager
                        .beginTransaction()
                        .addToBackStack(null)
                        .replace(R.id.content_frame, submissionFragment)
                        .commit();

            } else {

                SubmissionFragment submissionFragment = SubmissionFragment.newInstance(new ExtendedSubmission(mLastSubmissionClicked));
                mFragmentManager
                        .beginTransaction()
                        .addToBackStack(null)
                        .replace(R.id.content_frame, submissionFragment)
                        .commit();
            }

            mFragmentManager
                    .beginTransaction()
                    .replace(R.id.secondary_menu_frame, CommentsListFragment.newInstance(new ExtendedSubmission(mLastSubmissionClicked), true), COMMENTS_SECONDARY_MENU_TRANSACTION_TAG)
                    .commit();

        }

//        mSlidingMenu.setSlidingEnabled(false);

    }

    /**
     * Listener for clicks on the 'number of comments' View from submissions list view.
     * @param submission
     */
    @Override
    public void onCommentsClick(Submission submission) {
        mLastSubmissionClicked = submission;
        mLastSubmissionClicked.setVisited(true);

        getSupportFragmentManager()
                .beginTransaction()
                .addToBackStack(null)
                .replace(R.id.content_frame, CommentsListFragment.newInstance(new ExtendedSubmission(submission)), COMMENTS_LIST_FRAGMENT_TRANSACTION_TAG)
                .commit();


    }


    /**
     * Listener that listens to onDetach of CommentsListFragment to update the underlying submission object
      * @param submission
     */
    @Override
    public void onCommentsListFragmentDetach(Submission submission) {
        Log.d("MainActivity", "CommentsListFragment detach listener");

        if (mLastSubmissionClicked != null) {
            mLastSubmissionClicked.setLiked(submission.isLiked());
            mLastSubmissionClicked.setScore(submission.getScore());
            mLastSubmissionClicked.setSaved(submission.isSaved());
        }
    }

    @Override
    public void onSearchSubmissions(String subreddit, String queryString) {
        SearchSubmissionsFragment submissionsFragment = SearchSubmissionsFragment.newInstance(subreddit, queryString);
        mFragmentManager
                .beginTransaction()
                .addToBackStack(null)
                .replace(R.id.content_frame, submissionsFragment)
                .commit();
    }

    @Override
    public void onRequestShowSideMenuComments() {
        mSlidingMenu.showMenu();
    }

    /*
 * OnDrawerMenuInteractionListener interface implementation
 */

    /**
     * Click handler for user login button at top of drawer menu
     */
    @Override
    public void onLoginClick() {
        new LoginDialogFragment().show(getSupportFragmentManager(), "LoginDialogFragment");
    }

    @Override
    public void onLogoutClick() {
        mSessionManager.userLogout();

        finish();
        startActivity(getIntent());
    }

    @Override
    public void onSubredditClick(String subredditName) {
        SubmissionsListFragment fragment = (SubmissionsListFragment)mFragmentManager.findFragmentById(R.id.content_frame);
        fragment.refresh(subredditName);
        mSlidingMenu.showContent();
    }

    /**
     * LoginDialogListener interface implementation
     * @param username
     * @param password
     */
    @Override
    public void onLoginClick(String username, String password) {
        mSessionManager.userLogIn(username, password, new SessionManager.SessionListener<User>() {
            @Override
            public void onResponse(User object) {
                if (object != null) {
                    Log.d("MainActivity", String.format("User %s logged in.", object.getUsername()));

                    finish();
                    startActivity(getIntent());

                    Toast.makeText(getApplicationContext(), "Logged in as " + object.getUsername(), Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

/*
 * Other interface implementations
 */
    /**
     * Listener for FragmentManager back stack changes to handle action bar home icon and side menu sliding options
     */
    @Override
    public void onBackStackChanged() {
        refreshActionBar();


    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        View view = getCurrentFocus();

        if (view instanceof EditText) {
            int editTextCoordinates[] = new int[2];
            view.getLocationOnScreen(editTextCoordinates);

            float x = ev.getRawX() + view.getLeft() - editTextCoordinates[0];
            float y = ev.getRawY() + view.getTop() - editTextCoordinates[1];

            if (ev.getAction() == MotionEvent.ACTION_UP && (x < view.getLeft() || x >= view.getRight() || y < view.getTop() || y >= view.getBottom())) {
                // touch was outside the currently focused EditText view
                view.clearFocus();

                return true;
            }
        }

        return super.dispatchTouchEvent(ev);
    }

    /*
     * Fragment Lifecycle Methods
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d("MainActivity", "onCreate()");
        super.onCreate(savedInstanceState);
//        Crashlytics.start(this);

        // request FEATURE_PROGRESS to show progress bar while loading links
        getWindow().requestFeature(Window.FEATURE_PROGRESS);
//        getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);


        setContentView(R.layout.activity_main);

        mFragmentManager = getSupportFragmentManager();

        // register for changes to fragment manager back stack
        mFragmentManager.addOnBackStackChangedListener(this);

        mSessionManager = SessionManager.getInstance(this);


        // set up the sliding side menu
//        mSlidingMenu = new SlidingMenu(this);
//        mSlidingMenu.setMenu(R.layout.menu_frame);

        mSlidingMenu = getSlidingMenu();
        setBehindContentView(R.layout.menu_frame);

        mSlidingMenu.setSecondaryMenu(R.layout.secondary_menu_frame);

        mSlidingMenu.setTouchModeAbove(SlidingMenu.TOUCHMODE_MARGIN);
        mSlidingMenu.setShadowWidthRes(R.dimen.shadow_width);
        mSlidingMenu.setShadowDrawable(R.drawable.shadow);
        mSlidingMenu.setShadowWidth(2);
//        mSlidingMenu.setShadowDrawable(android.R.color.transparent);

        mSlidingMenu.setFadeDegree(0.35f);

//        mSlidingMenu.attachToActivity(this, SlidingMenu.SLIDING_CONTENT);

        mSlidingMenu.setSecondaryOnOpenListner(new SlidingMenu.OnOpenListener() {
            @Override
            public void onOpen() {
                Log.d("MainActivity", "Secondary Menu is open.");
                CommentsListFragment fragment = (CommentsListFragment)getSupportFragmentManager().findFragmentByTag(COMMENTS_SECONDARY_MENU_TRANSACTION_TAG);

                if (fragment != null && !fragment.isLoaded()) {
                    fragment.initList();
                }
            }
        });

        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_drawer);

        refreshActionBar();

        // enables dropdown menu of action bar
//        getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

        if (savedInstanceState == null) {
            Log.d("MainActivity", "savedInstanceState is null. Instantiating fragments.");

            mFragmentManager.beginTransaction()
                    .replace(R.id.menu_frame, DrawerMenuFragment.newInstance())
                    .commit();

            mFragmentManager.beginTransaction()
                    .replace(R.id.content_frame, SubmissionsListFragment.newInstance("", null), SUBMISSIONS_LIST_FRAGMENT_TAG)  // null defaults to frontpage and SubmissionSort.HOT
                    .commit();
        }

    }

    @Override
    protected void onResume() {
        Log.d("MainActivity", "onResume()");
        super.onResume();

//        refreshActionBar();
    }

    @Override
    protected void onPostResume() {
        Log.d("MainActivity", "onPostResume()");
        super.onPostResume();
    }

    @Override
    protected void onPause() {
        Log.d("MainActivity", "onPause()");
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        Log.d("MainActivity", "onDestroy()");
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Log.d("MainActivity", "onSaveInstanceState()");
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        Log.d("MainActivity", "onRestoreInstanceState()");
//        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        Log.d("MainActivity", "onOptionsItemSelected called.");

        int id = item.getItemId();

        switch (id) {
            case android.R.id.home: {

                if (mFragmentManager.getBackStackEntryCount() > 0) {
                    mFragmentManager.popBackStack();
                    return true;
                }

                if (mSlidingMenu.isMenuShowing()) {
                    mSlidingMenu.showContent();
                } else {

                    mSlidingMenu.showMenu();
                }

                return true;
            }
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onBackPressed() {
        if (mSlidingMenu.isMenuShowing()) {
            mSlidingMenu.showContent();
        } else {
            super.onBackPressed();
        }
    }

    private void refreshActionBar() {
        boolean backStackIsEmpty = mFragmentManager.getBackStackEntryCount() == 0;
//        mSlidingMenu.setSlidingEnabled(backStackIsEmpty);


        if (backStackIsEmpty) {
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_drawer);

            Log.d("MainActivity", "Back stack is empty. Refreshing SubmissionsListFragment");

            // if back stack is empty, the comments list displayed on the secondary menu should also be removed
            CommentsListFragment fragment = (CommentsListFragment)getSupportFragmentManager().findFragmentByTag(COMMENTS_SECONDARY_MENU_TRANSACTION_TAG);

            if (fragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .remove(fragment)
                        .commit();
            }

            mSlidingMenu.setSlidingEnabled(true);
            mSlidingMenu.setMode(SlidingMenu.LEFT);
            mSlidingMenu.setBehindOffsetRes(R.dimen.slidingmenu_offset_left);


        } else {
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_action_previous_item);

            // don't allow the right side panel to work when the submission is a self link
            CommentsListFragment fragment = (CommentsListFragment)mFragmentManager.findFragmentByTag(COMMENTS_LIST_FRAGMENT_TRANSACTION_TAG);
            if (fragment == null) {
                mSlidingMenu.setSlidingEnabled(true);
                mSlidingMenu.setMode(SlidingMenu.RIGHT);
                mSlidingMenu.setBehindOffsetRes(R.dimen.slidingmenu_offset_right);
            } else {
                mSlidingMenu.setSlidingEnabled(false);
            }
        }
    }

    // Added backstack transaction option (allows back button to work go back to previous fragment)
    public void showFragment(Fragment fragment, boolean addToBackStack){
        FragmentTransaction transaction = mFragmentManager.beginTransaction();

        if (addToBackStack)
            transaction.addToBackStack(null);

        transaction.replace(R.id.content_frame, fragment)
                .commit();

        mSlidingMenu.showContent();
    }
}
