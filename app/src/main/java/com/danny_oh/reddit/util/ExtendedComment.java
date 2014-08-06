package com.danny_oh.reddit.util;

import com.github.jreddit.entity.Comment;

import org.json.simple.JSONObject;

/**
 * Created by danny on 7/23/14.
 *
 * An extension of the Comment class from the jReddit library that allows the creation of
 * 'abstract comments' that only have IDs with no content that has been loaded.
 *
 * This class will be required in order to support 'load more' of comments that has not been
 * fully fetched from the initial request.
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
