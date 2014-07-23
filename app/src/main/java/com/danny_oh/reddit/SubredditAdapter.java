package com.danny_oh.reddit;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.github.jreddit.entity.Subreddit;

import java.util.List;

/**
 * Created by danny on 7/19/14.
 */
public class SubredditAdapter extends ArrayAdapter<Subreddit> {

    private List<Subreddit> mSubredditsList;

    public SubredditAdapter(Context context, int resource, int textViewResourceId, List<Subreddit> subreddits) {
        super(context, resource, textViewResourceId, subreddits);
        mSubredditsList = subreddits;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;

        if (view == null) {
            view = LayoutInflater.from(getContext()).inflate(R.layout.list_item_subreddit, parent, false);
        }

        Subreddit subreddit = mSubredditsList.get(position);

        TextView textView = (TextView)view.findViewById(R.id.subreddit_name);
        textView.setText(subreddit.getDisplayName());

        return view;
    }
}
