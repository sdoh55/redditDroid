package com.danny_oh.reddit.tasks;

import android.os.AsyncTask;

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

    public SubredditSearchTask(String query, SubredditSearchListener listener){
        searchQuery = query;
        mListener = listener;
    }

    @Override
    protected List<Subreddit> doInBackground(Void... params) {

        Subreddits subreddits = new Subreddits(new PoliteHttpRestClient());
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
