package com.danny_oh.reddit.services;

import android.content.Context;

import com.danny_oh.reddit.SessionManager;
import com.github.jreddit.action.SubmitActions;

/**
 * Created by allen on 8/2/14.
 */
public class UserActionsServices {
    //TODO: Not sure how we should make this class
    private static UserActionsServices sUserActionServices;
    private Context mContext;

    private SubmitActions mSubmitter;
    private SessionManager mSessionManager;

    public static UserActionsServices getInstance(Context context) {
        if (sUserActionServices != null) {
            sUserActionServices = new UserActionsServices(context);
        }
        return sUserActionServices;
    }

    private UserActionsServices(Context context){
        mContext = context;
        mSessionManager = SessionManager.getInstance(mContext);
        mSubmitter = new SubmitActions(mSessionManager.getRestClient());
    }

    public void sendComment(String fullName, String comment) {
        mSubmitter.comment(fullName, comment);
    }

    public void submitLinkPost(String title, String link, String subreddit) {
        mSubmitter.submitLink(title, link, subreddit, "", "");
    }

    public void submitTextPost(String title, String text, String subreddit) {
        mSubmitter.submitSelfPost(title, text, subreddit, "", "");
    }
}
