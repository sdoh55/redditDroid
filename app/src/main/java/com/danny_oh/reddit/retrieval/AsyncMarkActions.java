package com.danny_oh.reddit.retrieval;

import com.danny_oh.reddit.util.RedditRestClient;
import com.github.jreddit.action.MarkActions;
import com.github.jreddit.entity.User;
import com.github.jreddit.exception.ActionFailedException;
import com.github.jreddit.utils.ApiEndpointUtils;
import com.github.jreddit.utils.restclient.RestClient;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.simple.JSONObject;

/**
 * Created by danny on 8/2/14.
 */
public class AsyncMarkActions extends MarkActions {
    private RedditRestClient mRestClient;
    private User mUser;


    public static abstract class MarkActionsResponseHandler extends JsonHttpResponseHandler {
        public abstract void onSuccess(boolean actionSuccessful);

        @Override
        public void onSuccess(int statusCode, Header[] headers, org.json.JSONObject response) {
            if (response.length() == 0) {
                onSuccess(true);
            } else {
                onSuccess(false);
            }
        }

        @Override
        public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
            if (response.length() == 0) {
                onSuccess(true);
            } else {
                onSuccess(false);
            }
        }
    }



    /**
     * Constructor. Global default user (null) is used.
     * @param restClient REST Client instance
     */
    public AsyncMarkActions(RedditRestClient restClient) {
        super(restClient);
        this.mRestClient = restClient;
        this.mUser = null;
    }

    /**
     * Constructor.
     * @param restClient REST Client instance
     * @param actor User instance
     */
    public AsyncMarkActions(RedditRestClient restClient, User actor) {
        super(restClient, actor);
        this.mRestClient = restClient;
        this.mUser = actor;
    }

    @Override
    public void switchActor(User new_actor) {
        super.switchActor(new_actor);
        this.mUser = new_actor;
    }

    /**
     * This function saves a submission or comment with the given full name.
     * @param fullName Full name of the thing
     * @param responseHandler
     */
    public void saveAsync(String fullName, MarkActionsResponseHandler responseHandler) {
        String url = ApiEndpointUtils.REDDIT_BASE_URL + ApiEndpointUtils.SAVE + "?id=" + fullName + "&uh=" + mUser.getModhash();
        mRestClient.getAsyncClient().post(url, responseHandler);

    }

    /**
     * This function unsaves a submission or comment with the given full name.
     * @param fullName Full name of the thing
     *
     * @throws ActionFailedException If the action failed
     */
    public void unsaveAsync(String fullName, MarkActionsResponseHandler responseHandler) {
        String url = ApiEndpointUtils.REDDIT_BASE_URL + ApiEndpointUtils.UNSAVE + "?id=" + fullName + "&uh=" + mUser.getModhash();
        mRestClient.getAsyncClient().post(url, responseHandler);
    }
}
