package com.danny_oh.reddit.util;

import com.github.jreddit.entity.Comment;

import org.json.simple.JSONObject;

import java.util.LinkedList;
import java.util.List;

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
    private boolean mIsFault;
    private int mDepth;

    private List<String> mMoreChildren = null;
    private int mMoreChildrenCount = 0;

    // This constructor creates a mostly empty comment skeleton that only has the identifier set.
    // used to display 'xxx more comments' and load more when requested
    public ExtendedComment(String name, int depth, List<String> moreChildren, int moreChildrenCount) {
        super(name);
        mIsFault = true;
        mDepth = depth;
        mMoreChildren = moreChildren;
        mMoreChildrenCount = moreChildrenCount;
        this.setLinkId("");
        this.setBody("");
    }

    public ExtendedComment(JSONObject object) {
        this(object, 0);
    }
    public ExtendedComment(JSONObject object, int depth) {
        super(object);
        mIsFault = false;
        mDepth = depth;
    }

    public void setDepth(int depth) { mDepth = depth; }

    public int getDepth() { return mDepth; }


    public boolean hasMoreChildren() { return (mMoreChildren != null && mMoreChildren.size() > 0); }

    public List<String> getMoreChildren() { return mMoreChildren; }
    public int getMoreChildrenCount() { return mMoreChildrenCount; }

    public void setMoreChildren(List<String> moreChilren) {
        this.mMoreChildren = moreChilren;
    }

    public boolean isFault() { return mIsFault; }
}
