package com.danny_oh.reddit.activities;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.SpinnerAdapter;
import android.widget.Toast;

import com.danny_oh.reddit.SessionManager;
import com.danny_oh.reddit.fragments.DrawerMenuFragment;
import com.danny_oh.reddit.R;
import com.danny_oh.reddit.fragments.LoginDialogFragment;
import com.danny_oh.reddit.fragments.SubmissionFragment;
import com.danny_oh.reddit.fragments.SubmissionListFragment;
import com.danny_oh.reddit.fragments.SubredditFragment;
import com.danny_oh.reddit.util.ExtendedSubmission;
import com.github.jreddit.entity.Submission;
import com.github.jreddit.entity.User;
import com.github.jreddit.retrieval.params.SubmissionSort;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;


public class MainActivity
        extends ActionBarActivity
        implements FragmentManager.OnBackStackChangedListener,              // handles changes to fragment stack
        SubmissionListFragment.OnSubmissionListFragmentInteractionListener, // handles individual submission item clicks
        DrawerMenuFragment.OnDrawerMenuInteractionListener,                 // handles side drawer menu item clicks
        LoginDialogFragment.LoginDialogListener,                            // handles user login dialog interaction
        ActionBar.OnNavigationListener                                      // handles action bar navigation interaction

{

    private SlidingMenu mSlidingMenu;
    private FragmentManager mFragmentManager;

    private SessionManager mSessionManager;

    /**
     * OnSubmissionListFragmentInteractionListener interface implementation
     * @param submission the submission that was clicked by the user
     */
    public void onSubmissionClick(Submission submission) {
        Log.d("MainActivity", "Received fragment interaction. Selection: " + submission.getFullName());


        SubmissionFragment submissionFragment = SubmissionFragment.newInstance(new ExtendedSubmission(submission));
        mFragmentManager
                .beginTransaction()
                .addToBackStack(null)
                .add(R.id.content_frame, submissionFragment)
                .commit();

        /*
        CommentListFragment commentListFragment = CommentListFragment.newInstance(submission.getIdentifier());
        mFragmentManager
                .beginTransaction()
                .addToBackStack(null)
                .add(R.id.content_frame, commentListFragment)
                .commit();
        */

        mSlidingMenu.setSlidingEnabled(false);
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

    /**
     * Click handler for items in the list view of the drawer menu
     * @param position
     */
    @Override
    public void onDrawerItemClick(int position) {
        switch (position) {
            // login
            case 0:
                // subreddits_menu
                showFragment(new SubredditFragment(), true);
                break;
        }

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
                Log.d("MainActivity", String.format("User %s logged in.", object.getUsername()));

                finish();
                startActivity(getIntent());

                Toast.makeText(getApplicationContext(), "Logged in as " + object.getUsername(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Listener for FragmentManager back stack changes to handle action bar home icon and side menu sliding options
     */
    @Override
    public void onBackStackChanged() {
        boolean backStackIsEmpty = mFragmentManager.getBackStackEntryCount() == 0;
        getSupportActionBar().setDisplayHomeAsUpEnabled(!backStackIsEmpty);
        mSlidingMenu.setSlidingEnabled(backStackIsEmpty);
    }

    /*
     * Fragment Lifecycle Methods
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // request FEATURE_PROGRESS to show progress bar while loading links
        getWindow().requestFeature(Window.FEATURE_PROGRESS);
//        getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);

        mFragmentManager = getSupportFragmentManager();

        // register for changes to fragment manager back stack
        mFragmentManager.addOnBackStackChangedListener(this);

        setContentView(R.layout.activity_main);

        // set up the sliding side menu
        mSlidingMenu = new SlidingMenu(this);
        mSlidingMenu.setTouchModeAbove(SlidingMenu.TOUCHMODE_FULLSCREEN);
//        mSlidingMenu.setTouchmodeMarginThreshold();
        mSlidingMenu.setShadowWidthRes(R.dimen.shadow_width);
        mSlidingMenu.setShadowDrawable(R.drawable.shadow);
        mSlidingMenu.setBehindOffsetRes(R.dimen.slidingmenu_offset);
        mSlidingMenu.setFadeDegree(0.35f);
        mSlidingMenu.setMenu(R.layout.menu_frame);
        mSlidingMenu.attachToActivity(this, SlidingMenu.SLIDING_WINDOW);


        mSessionManager = SessionManager.getInstance(this);

//        mSlidingMenu.setSelectorEnabled(true);
//        mSlidingMenu.setSelectorDrawable(R.drawable.ic_drawer);
//        mSlidingMenu.setSelectedView(mSlidingMenu.getContent());


        mFragmentManager
                .beginTransaction()
                .replace(R.id.menu_frame, new DrawerMenuFragment())
                .commit();

        getSupportActionBar().setHomeButtonEnabled(true);

        // enables dropdown menu of action bar
        getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);


        if (savedInstanceState == null) {
            mFragmentManager.beginTransaction()
                    .add(R.id.content_frame, SubmissionListFragment.newInstance(SubmissionSort.NEW))  // null defaults to SubmissionSort.HOT
                    .commit();
        }


    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onNavigationItemSelected(int position, long id) {
        switch (position) {
            case 0:
                return true;
        }

        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
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

            case R.id.action_settings: {
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

    // Added backstack transaction option (allows back button to work go back to previous fragment)
    private void showFragment(Fragment fragment, boolean addToBackStack){
        FragmentTransaction transaction = mFragmentManager.beginTransaction();

        if (addToBackStack)
            transaction.addToBackStack(null);

        transaction.replace(R.id.content_frame, fragment)
                .commit();
    }
}
