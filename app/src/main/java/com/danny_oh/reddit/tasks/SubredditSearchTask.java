package com.danny_oh.reddit.tasks;

import android.content.Context;
import android.os.AsyncTask;

import com.danny_oh.reddit.SessionManager;
import com.github.jreddit.entity.Subreddit;
import com.github.jreddit.retrieval.Subreddits;
import com.github.jreddit.utils.restclient.PoliteHttpRestClient;

import java.util.List;

/**
 * Created by allen on 7/25/14.
 */
public class SubredditSearchTask extends AsyncTask<Void, Void, List<Subreddit>> {
    SubredditSearchListener mListener;
    private String searchQuery;
    private Context mContext;

    public SubredditSearchTask(Context context, String query, SubredditSearchListener listener){
        searchQuery = query;
        mListener = listener;
        mContext = context;
    }

    @Override
    protected List<Subreddit> doInBackground(Void... params) {

        Subreddits subreddits = new Subreddits(SessionManager.getInstance(mContext).getRestClient());
        List<Subreddit> subredditList = subreddits.search(searchQuery, 0, 25, null, null);

        return subredditList;
    }

    @Override
    protected void onPostExecute(List<Subreddit> subreddits) {
        super.onPostExecute(subreddits);
        mListener.onSearchFinished(subreddits);
    }

    public interface SubredditSearchListener{
        public void onSearchFinished(List<Subreddit> list);
    }
}
