package com.danny_oh.reddit.util;

import android.util.SparseArray;

import com.github.jreddit.entity.Comment;
import com.github.jreddit.entity.Kind;
import com.github.jreddit.exception.RedditError;
import com.github.jreddit.exception.RetrievalFailedException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.HashMap;
import java.util.List;

import static com.github.jreddit.utils.restclient.JsonUtils.safeJsonToString;

/**
 * Created by danny on 7/23/14.
 */
public class CommentsListHelper {
    public static class CommentContainer {
        public Comment comment;
        public int depth;
    }

    static int index = 0;

    public static HashMap<Integer, CommentContainer> listToMap(List<Comment> commentsList) {
        HashMap<Integer, CommentContainer> comments = new HashMap<Integer, CommentContainer>();

        index = 0;
        parseRecursiveMap(comments, commentsList, 0);

        return comments;
    }

    public static SparseArray<CommentContainer> listToSparseArray(List<Comment> commentsList) {
        SparseArray<CommentContainer> comments = new SparseArray<CommentContainer>();

        index = 0;
        parseRecursiveArray(comments, commentsList, index);

        return comments;
    }

    protected static void parseRecursiveMap(HashMap<Integer, CommentContainer> comments, List<Comment> commentList, int depth) throws RetrievalFailedException, RedditError {
        assert comments != null : "List of comments must be instantiated.";
        assert commentList != null : "JSON Object must be instantiated.";

        for (Comment comment : commentList) {
            CommentContainer container = new CommentContainer();
            container.comment = comment;
            container.depth = depth;
            comments.put(index++, container);

            if (comment.hasRepliesSomewhere()) {
                parseRecursiveMap(comments, comment.getReplies(), ++depth);
            }
        }

    }

    protected static void parseRecursiveArray(SparseArray<CommentContainer> comments, List<Comment> commentList, int depth) throws RetrievalFailedException, RedditError {
        assert comments != null : "List of comments must be instantiated.";
        assert commentList != null : "JSON Object must be instantiated.";

        for (Comment comment : commentList) {
            CommentContainer container = new CommentContainer();
            container.comment = comment;
            container.depth = depth;
            comments.put(index++, container);

            if (comment.hasRepliesSomewhere()) {
                parseRecursiveArray(comments, comment.getReplies(), ++depth);
            }
        }

    }

}
