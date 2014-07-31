package com.danny_oh.reddit.fragments;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.danny_oh.reddit.R;
import com.danny_oh.reddit.SessionManager;
import com.danny_oh.reddit.adapters.UserMenuExpandableListAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Created by danny on 7/22/14.
 */
public class DrawerMenuFragment extends Fragment implements
        ExpandableListView.OnChildClickListener {
    private OnDrawerMenuInteractionListener mListener;

    private ExpandableListView mUserListView;
    private ListView mListView;

    private List<Pair<String, String>> mUserHeaderList;
    private HashMap<String, List<String>> mUserItemList;


    private SessionManager mSessionManager;

    public interface OnDrawerMenuInteractionListener {
        public void onLoginClick();
        public void onLogoutClick();
        public void onSubredditClick(String subredditName);
    }

    /* used if ListFragment
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        mListener.onDrawerItemClick(position);
    }
    */

    /**
     * OnChildClick handler for the user menu expandable list view
     * @param expandableListView
     * @param view
     * @param groupPosition
     * @param childPosition
     * @param id
     * @return
     */
    @Override
    public boolean onChildClick(ExpandableListView expandableListView, View view, int groupPosition, int childPosition, long id) {
        // TODO: right now there is only one group and multiple children so a switch works
        // but this should be replaced soon with something more robust

        switch (childPosition) {
            case 0:
                mListener.onLogoutClick();
                return true;
//            case 1:
//                return true;
        }

        return false;
    }

    public static DrawerMenuFragment newInstance() {
        DrawerMenuFragment listFragment = new DrawerMenuFragment();

        // get args and set arguments to be retrieved for reinstantiation

        return listFragment;
    }

    public DrawerMenuFragment() {

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        mSessionManager = SessionManager.getInstance(activity);

        try {
            mListener = (OnDrawerMenuInteractionListener)activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnDrawerMenuInteractionListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        if (getArguments() != null) {
//            // TODO: do something with arguments if needed
//        } else {
//            throw new InstantiationException("Use factory method newInstance to instantiate fragment.", new Exception());
//        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_main_drawer, container, false);

        // the RelativeLayout that contains the "Log In" button
        RelativeLayout loginLayout = (RelativeLayout)view.findViewById(R.id.login_button_view);
        // the logged in user's expandable menu
        mUserListView = (ExpandableListView)view.findViewById(R.id.user_menu_list_view);

        // if user is logged in
        if (mSessionManager.isUserLoggedIn()) {
            // hide the "Log In" portion of the drawer menu
            loginLayout.setVisibility(View.GONE);

            prepareUserMenuData();

            mUserListView.setAdapter(new UserMenuExpandableListAdapter(getActivity(), mUserHeaderList, mUserItemList));

            DisplayMetrics metrics = new DisplayMetrics();
            getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);

            int width = metrics.widthPixels;

            // subtract the sliding menu offset from width to get displayed width of sliding menu
            width -= (int)getResources().getDimension(R.dimen.slidingmenu_offset);

            // change the position of the group dropdown indicator
            if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
                mUserListView.setIndicatorBounds(width-GetPixelFromDips(35), width-GetPixelFromDips(5));
            } else {
                mUserListView.setIndicatorBoundsRelative(width-GetPixelFromDips(35), width-GetPixelFromDips(5));
            }

            mUserListView.setOnChildClickListener(this);

        } else {
            // hide the "Logged In User" portion of the drawer menu
            mUserListView.setVisibility(View.GONE);

            loginLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mListener.onLoginClick();
                }
            });
        }

        getFragmentManager().beginTransaction().replace(R.id.subreddit_list_view, new SubredditFragment()).commit();

//        mListView = (ListView)view.findViewById(R.id.subreddit_list_view);
//
//        mListView.setOnItemClickListener(this);
//        mListView.setSelector(R.drawable.selector_drawer_list_item);
//
//        final String[] drawerMenuItems = getResources().getStringArray(R.array.drawer_menu_items);
//        mListView.setAdapter(new DrawerMenuAdapter(getActivity(), drawerMenuItems));



        return view;
    }

    private int GetPixelFromDips(float pixels) {
        // Get the screen's density scale
        final float scale = getResources().getDisplayMetrics().density;
        // Convert the dps to pixels, based on density scale
        return (int) (pixels * scale + 0.5f);

    }

    private void prepareUserMenuData() {
        // the group header titles for the user menu (expandable list view)
        mUserHeaderList = new ArrayList<Pair<String, String>>();
        mUserHeaderList.add(new Pair<String, String>(mSessionManager.getUser().getUsername(), getResources().getString(R.string.drawer_user_menu_subtitle)));

        // the item header titles for the logged in user menu
        List<String> loggedInUserItems = Arrays.asList(getResources().getStringArray(R.array.drawer_menu_logged_in_user_items));

        // a HashMap that maps the list's headers to its list of items
        mUserItemList = new HashMap<String, List<String>>();
        mUserItemList.put(mUserHeaderList.get(0).first, loggedInUserItems);
    }


    private static class ViewHolder {
        private TextView title;
    }

    private class DrawerMenuAdapter extends BaseAdapter {


        private String[] mMenuItems;
        private Context mContext;

        public DrawerMenuAdapter(Context context, String[] menuItems) {
            mContext = context;
            mMenuItems = menuItems;
        }

        @Override
        public int getCount() {
            return mMenuItems.length;
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public Object getItem(int i) {
            return mMenuItems[i];
        }

        @Override
        public View getView(int position, View convertView, ViewGroup viewGroup) {
            View view = convertView;

            if (view == null) {
                view = LayoutInflater.from(mContext).inflate(R.layout.list_item_drawer_menu, viewGroup, false);

                ViewHolder viewHolder = new ViewHolder();
                viewHolder.title = (TextView)view.findViewById(android.R.id.text1);
                view.setTag(viewHolder);
            }

            ViewHolder viewHolder = (ViewHolder)view.getTag();
            viewHolder.title.setText(mMenuItems[position]);

            return view;
        }
    }
}
