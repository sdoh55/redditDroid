package com.danny_oh.reddit.retrieval;

import com.danny_oh.reddit.util.RedditRestClient;
import com.github.jreddit.entity.Kind;
import com.github.jreddit.entity.Subreddit;
import com.github.jreddit.entity.User;
import com.github.jreddit.exception.RedditError;
import com.github.jreddit.exception.RetrievalFailedException;
import com.github.jreddit.retrieval.Subreddits;
import com.github.jreddit.utils.restclient.RestClient;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.apache.http.Header;
import org.json.JSONException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.LinkedList;
import java.util.List;

import static com.github.jreddit.utils.restclient.JsonUtils.safeJsonToString;

/**
 * Created by danny on 8/3/14.
 */
public class AsyncSubreddits extends Subreddits {
    private RedditRestClient mRestClient;
    private User mUser;

    public static abstract class SubredditsResponseHandler extends JsonHttpResponseHandler {
        public abstract void onParseFinished(List<Subreddit> subreddits);

        @Override
        public void onSuccess(int statusCode, Header[] headers, org.json.JSONObject response) {
            super.onSuccess(statusCode, headers, response);

            org.json.JSONObject object = response;

            JSONArray array;

            try {
                array = (JSONArray) ((JSONObject) object.get("data")).get("children");
            } catch (JSONException je) {
                je.printStackTrace();
                return;
            }

            List<Subreddit> subreddits = new LinkedList<Subreddit>();

            // Iterate over the subreddit results
            JSONObject data;
            for (Object anArray : array) {
                data = (JSONObject) anArray;

                // Make sure it is of the correct kind
                String kind = safeJsonToString(data.get("kind"));
                if (kind.equals(Kind.SUBREDDIT.value())) {

                    // Create and add subreddit
                    data = ((JSONObject) data.get("data"));
                    subreddits.add(new Subreddit(data));

                }

            }

            onParseFinished(subreddits);
        }

        @Override
        public void onSuccess(int statusCode, Header[] headers, org.json.JSONArray response) {
            super.onSuccess(statusCode, headers, response);
        }
    }

    /**
     * Constructor.
     *
     * @param restClient REST client instance
     */
    public AsyncSubreddits(RedditRestClient restClient) {
        super(restClient);
        mRestClient = restClient;
    }

    /**
     * Constructor.
     * @param restClient REST Client instance
     * @param actor User instance
     */
    public AsyncSubreddits(RedditRestClient restClient, User actor) {
        super(restClient, actor);
        mRestClient = restClient;
        mUser = actor;

    }

}

