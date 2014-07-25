package com.danny_oh.reddit.fragments;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.danny_oh.reddit.R;

/**
 * Created by danny on 7/22/14.
 */
public class DrawerMenuListFragment extends Fragment implements AdapterView.OnItemClickListener {
    private OnDrawerMenuInteractionListener mListener;

    private ListView mListView;


    public interface OnDrawerMenuInteractionListener {
        public void onLoginClick();
        public void onDrawerItemClick(int position);
    }

    /* used if ListFragment
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        mListener.onDrawerItemClick(position);
    }
    */

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        mListener.onDrawerItemClick(position);
    }

    public static DrawerMenuListFragment newInstance() {
        DrawerMenuListFragment listFragment = new DrawerMenuListFragment();

        // get args and set arguments to be retrieved for reinstantiation

        return listFragment;
    }

    public DrawerMenuListFragment() {

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

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
        View view = inflater.inflate(R.layout.activity_main_drawer, null);

        mListView = (ListView)view.findViewById(android.R.id.list);
        mListView.setOnItemClickListener(this);
        mListView.setSelector(R.drawable.selector_drawer_list_item);

        final String[] drawerMenuItems = getResources().getStringArray(R.array.drawer_menu_items);
        mListView.setAdapter(new DrawerMenuAdapter(getActivity(), drawerMenuItems));

        RelativeLayout layout = (RelativeLayout)view.findViewById(R.id.login_button_view);
        layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mListener.onLoginClick();
            }
        });


        return view;
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
