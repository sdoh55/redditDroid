package com.danny_oh.reddit.activities;

import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.Toast;

import com.danny_oh.reddit.SessionManager;
import com.danny_oh.reddit.fragments.DrawerMenuListFragment;
import com.danny_oh.reddit.R;
import com.danny_oh.reddit.fragments.LoginDialogFragment;
import com.danny_oh.reddit.fragments.SubmissionFragment;
import com.danny_oh.reddit.fragments.SubmissionListFragment;
import com.danny_oh.reddit.util.ExtendedSubmission;
import com.github.jreddit.entity.Submission;
import com.github.jreddit.entity.User;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;


public class MainActivity
        extends ActionBarActivity
        implements FragmentManager.OnBackStackChangedListener,
        SubmissionListFragment.OnSubmissionListFragmentInteractionListener,
        DrawerMenuListFragment.OnDrawerMenuInteractionListener,
        LoginDialogFragment.LoginDialogListener {

    private SlidingMenu mSlidingMenu;
    private FragmentManager mFragmentManager;

    private User mUser;

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

    /**
     * OnDrawerMenuInteractionListener interface implementation
     * @param position
     */
    @Override
    public void onDrawerItemClick(int position) {
        switch (position) {
            // login
            case 0:
                new LoginDialogFragment().show(getSupportFragmentManager(), "LoginDialogFragment");
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
        SessionManager.getInstance().userLogIn(username, password, new SessionManager.SessionListener<User>() {
            @Override
            public void onResponse(User object) {
                Log.d("MainActivity", String.format("User %s logged in.", object.getUsername()));
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
        mSlidingMenu.setMenu(R.layout.activity_main_drawer);
        mSlidingMenu.attachToActivity(this, SlidingMenu.SLIDING_WINDOW);


//        mSlidingMenu.setSelectorEnabled(true);
//        mSlidingMenu.setSelectorDrawable(R.drawable.ic_drawer);
//        mSlidingMenu.setSelectedView(mSlidingMenu.getContent());


        mFragmentManager
                .beginTransaction()
                .replace(R.id.menu_frame, new DrawerMenuListFragment())
                .commit();

        getSupportActionBar().setHomeButtonEnabled(true);

        if (savedInstanceState == null) {
            mFragmentManager.beginTransaction()
                    .add(R.id.content_frame, new SubmissionListFragment())
                    .commit();
        }


    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
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
}
