package com.danny_oh.reddit.adapters;

import android.content.Context;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import com.danny_oh.reddit.R;

import java.util.HashMap;
import java.util.List;

/**
 * Created by danny on 7/25/14.
 *
 * Expandable list adapter for use with the left side drawer menu for user actions (e.g. view profile,
 * view saved links, messages, etc).
 */
public class UserMenuExpandableListAdapter extends BaseExpandableListAdapter {

    private Context mContext;

    private List<Pair<String, String>> mHeaderList;
    private HashMap<String, List<String>> mChildTitleList;

    static class ChildViewHolder {
        public TextView mTitleText;
    }


    public UserMenuExpandableListAdapter(Context context, List<Pair<String, String>> headerList, HashMap<String, List<String>> childTitleList) {
        this.mContext = context;
        this.mHeaderList = headerList;
        this.mChildTitleList = childTitleList;
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return this.mChildTitleList.get(this.mHeaderList.get(groupPosition).first).get(childPosition);
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        View view = convertView;
        ChildViewHolder viewHolder;

        if (view == null) {
            viewHolder = new ChildViewHolder();

            view = LayoutInflater.from(mContext).inflate(R.layout.list_item_user_menu, parent, false);
            viewHolder.mTitleText = (TextView)view.findViewById(R.id.user_menu_item_title);

            view.setTag(viewHolder);
        } else {
            viewHolder = (ChildViewHolder)view.getTag();
        }

        String title = (String)getChild(groupPosition, childPosition);

        if (title != null) {
            viewHolder.mTitleText.setText(title);
        }

        return view;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return mChildTitleList.get(mHeaderList.get(groupPosition).first).size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return mHeaderList.get(groupPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public int getGroupCount() {
        return mHeaderList.size();
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        View view = convertView;

        if (view == null) {
            view = LayoutInflater.from(mContext).inflate(R.layout.list_group_user_menu, parent, false);
        }

        Pair<String, String> header = (Pair<String, String>)getGroup(groupPosition);

        TextView titleTextView = (TextView)view.findViewById(R.id.user_menu_group_title);
        titleTextView.setText(header.first);

        TextView subtitleTextView = (TextView)view.findViewById(R.id.user_menu_group_subtitle);
        subtitleTextView.setText(header.second);

        return view;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }
}
