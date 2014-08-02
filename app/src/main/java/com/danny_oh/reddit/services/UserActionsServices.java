package com.danny_oh.reddit.services;

import com.danny_oh.reddit.SessionManager;
import com.github.jreddit.action.SubmitActions;

/**
 * Created by allen on 8/2/14.
 */
public class UserActionsServices {

    private static UserActionsServices mUserActionServices = new UserActionsServices();

    private SubmitActions mSubmitter;

    public static UserActionsServices getInstance() {
        return mUserActionServices;
    }

    private UserActionsServices(){
        SessionManager manager = SessionManager.getInstance();
        mSubmitter = new SubmitActions(manager.getRestClient());
    }

    public void sendComment(String title, String comment) {
        mSubmitter.comment(title, comment);
    }

    public void submitPost() {

    }
}
