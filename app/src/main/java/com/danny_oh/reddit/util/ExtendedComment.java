package com.danny_oh.reddit.util;

import com.github.jreddit.entity.Comment;

import org.json.simple.JSONObject;

/**
 * Created by danny on 7/23/14.
 */
public class ExtendedComment extends Comment {
    private boolean mShouldDisplay = true;

    // This constructor creates a mostly empty comment skeleton that only has the identifier set.
    // used to display 'xxx more comments' and load more when requested
    public ExtendedComment(String name) {
        super(name);
    }

    public ExtendedComment(JSONObject object) {
        super(object);
    }
}
