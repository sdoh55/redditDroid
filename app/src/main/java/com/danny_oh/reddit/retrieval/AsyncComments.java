package com.danny_oh.reddit.retrieval;

import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import com.danny_oh.reddit.util.ExtendedComment;
import com.danny_oh.reddit.util.RedditRestClient;
import com.github.jreddit.entity.Comment;
import com.github.jreddit.entity.Kind;
import com.github.jreddit.entity.Submission;
import com.github.jreddit.entity.User;
import com.github.jreddit.exception.RedditError;
import com.github.jreddit.exception.RetrievalFailedException;
import com.github.jreddit.retrieval.Comments;
import com.github.jreddit.retrieval.params.CommentSort;
import com.github.jreddit.utils.ApiEndpointUtils;
import com.github.jreddit.utils.ParamFormatter;
import com.github.jreddit.utils.restclient.RestClient;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.TextHttpResponseHandler;

import org.apache.http.Header;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

import static com.github.jreddit.utils.restclient.JsonUtils.safeJsonToString;

/**
 * Created by danny on 8/8/14.
 */
public class AsyncComments extends Comments {

    private RedditRestClient restClient;

    public static abstract class CommentsResponseHandler extends TextHttpResponseHandler {
        private final JSONParser mJsonParser;
        private ExtendedComment mParent;

        public CommentsResponseHandler() {
            mJsonParser = new JSONParser();
        }

        public CommentsResponseHandler(ExtendedComment parent) {
            this();
            mParent = parent;
        }

        public abstract void onParseFinished(List<ExtendedComment> comments);

        @Override
        public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {

        }

        @Override
        public void onSuccess(int statusCode, Header[] headers, String responseString) {
            try {
                Object json = mJsonParser.parse(responseString);

                new AsyncTask<Object, Void, List<ExtendedComment>>() {
                    @Override
                    protected List<ExtendedComment> doInBackground(Object... objects) {
                        Object response = objects[0];

                        List<ExtendedComment> comments = parseJson(response, mParent);

                        return comments;
                    }

                    @Override
                    protected void onPostExecute(List<ExtendedComment> comments) {
                        onParseFinished(comments);
                    }
                }.execute(json);

            } catch (ParseException pe) {
                Log.e("AsyncSubmissions", "Failed to parse response. Localized message: " + pe.getLocalizedMessage());
            }

        }
    }

    protected static List<ExtendedComment> parseJson(Object response) throws RetrievalFailedException, RedditError {
        return parseJson(response, null);
    }


    protected static List<ExtendedComment> parseJson(Object response, ExtendedComment parent) throws RetrievalFailedException, RedditError {

        // List of comments
        List<ExtendedComment> comments = new LinkedList<ExtendedComment>();

        if (parent == null && response instanceof JSONArray) {

            JSONObject object = (JSONObject) ((JSONArray) response).get(1);
            parseRecursive(comments, object, 0);

        } else if (parent != null && response instanceof JSONObject) {
            HashMap<String, ExtendedComment> commentsMap = new LinkedHashMap<String, ExtendedComment>();
            commentsMap.put(parent.getFullName(), parent);

            JSONObject object = (JSONObject) ((JSONObject) response).get("json");
            parseRecursiveWithParent(commentsMap, object, parent);

            comments = new LinkedList<ExtendedComment>(commentsMap.values());

        } else {
            throw new IllegalArgumentException("Parsing failed because JSON input is not from a comment.");
        }

        return comments;

    }

    protected static void parseRecursiveWithParent(HashMap<String, ExtendedComment> comments, JSONObject object, ExtendedComment parent) throws RetrievalFailedException, RedditError {
        JSONArray array;

        if (((JSONObject) object.get("data")).containsKey("things")) {

            array = (JSONArray) ((JSONObject) object.get("data")).get("things");

        } else {

            throw new RetrievalFailedException("failed to parse comments.");

        }

        // Iterate over the submission results
        JSONObject data;
        ExtendedComment comment;
        for (Object anArray : array) {
            data = (JSONObject) anArray;

            // Make sure it is of the correct kind
            String kind = safeJsonToString(data.get("kind"));

            if (kind.equals(Kind.COMMENT.value())) {

                // Contents of the comment
                data = ((JSONObject) data.get("data"));

                // Create and add the new comment
                comment = new ExtendedComment(data);

                ExtendedComment newParent = comments.get(comment.getParentId());

                if (newParent == null)
                    comment.setDepth(parent.getDepth());
                else
                    comment.setDepth(newParent.getDepth()+1);

                comments.put(comment.getFullName(), comment);

                Object o = data.get("replies");
                if (o instanceof JSONObject) {

                    // Dig towards the replies
                    JSONObject replies = (JSONObject) o;
                    parseRecursiveWithParent(comments, replies, newParent);

                }

            } else if (kind.equals(Kind.MORE.value())) {

                data = (JSONObject) data.get("data");
                JSONArray children = (JSONArray) data.get("children");

                String id = data.get("name").toString();
                int count = Integer.parseInt(data.get("count").toString());

                if (count > 0) {
                    LinkedList<String> moreChildren = new LinkedList<String>();

                    for (Object child : children) {
                        if (child instanceof String) {
                            moreChildren.add((String) child);
                        }
                    }

                    ExtendedComment newParent = comments.get(data.get("parent_id").toString());

                    if (newParent == null)
                        newParent = parent;

                    comment = new ExtendedComment(id, newParent.getDepth(), moreChildren, count);
                    comments.put(comment.getFullName(), comment);
                }
            }

        }

    }

    protected static void parseRecursive(List<ExtendedComment> comments, JSONObject object, int depth) throws RetrievalFailedException, RedditError {
        assert comments != null : "List of comments must be instantiated.";
        assert object != null : "JSON Object must be instantiated.";

        JSONArray array;

        if (((JSONObject) object.get("data")).containsKey("children")) {
            // Get the comments in an array
            array = (JSONArray) ((JSONObject) object.get("data")).get("children");

        } else {

            throw new RetrievalFailedException("failed to parse comments.");

        }


        // Iterate over the submission results
        JSONObject data;
        ExtendedComment comment;
        for (Object anArray : array) {
            data = (JSONObject) anArray;

            // Make sure it is of the correct kind
            String kind = safeJsonToString(data.get("kind"));
            if (kind.equals(Kind.COMMENT.value())) {

                // Contents of the comment
                data = ((JSONObject) data.get("data"));

                // Create and add the new comment
                comment = new ExtendedComment(data, depth);
                comments.add(comment);

                Object o = data.get("replies");
                if (o instanceof JSONObject) {

                    // Dig towards the replies
                    JSONObject replies = (JSONObject) o;
                    parseRecursive(comments, replies, depth+1);

                }

            } else if (kind.equals(Kind.MORE.value())) {

                data = (JSONObject) data.get("data");
                JSONArray children = (JSONArray) data.get("children");

                String id = data.get("name").toString();
                int count = Integer.parseInt(data.get("count").toString());

                if (count > 0) {
                    LinkedList<String> moreChildren = new LinkedList<String>();

                    for (Object child : children) {
                        if (child instanceof String) {
                            moreChildren.add((String) child);
                        }
                    }

                    comment = new ExtendedComment(id, depth, moreChildren, count);
                    comments.add(comment);
                }
            }

        }

    }



    /**
     * Constructor. Global default user (null) is used.
     * @param restClient REST Client instance
     */
    public AsyncComments(RedditRestClient restClient) {
        super(restClient);
        this.restClient = restClient;
    }

    /**
     * Constructor.
     * @param restClient REST Client instance
     * @param actor User instance
     */
    public AsyncComments(RedditRestClient restClient, User actor) {
        super(restClient, actor);
        this.restClient = restClient;
    }


    public void ofSubmissionAsync(Submission submission, String commentId, int parentsShown, int depth, int limit, CommentSort sort, CommentsResponseHandler responseHandler) throws RetrievalFailedException, RedditError {

        if (submission == null) {
            throw new IllegalArgumentException("The submission must be defined.");
        }

        String params = "";
        params = ParamFormatter.addParameter(params, "comment", commentId);
        params = ParamFormatter.addParameter(params, "context", String.valueOf(parentsShown));
        params = ParamFormatter.addParameter(params, "depth", String.valueOf(depth));
        params = ParamFormatter.addParameter(params, "limit", String.valueOf(limit));
        params = ParamFormatter.addParameter(params, "sort", sort.value());

        restClient.getAsyncClient().get(ApiEndpointUtils.REDDIT_BASE_URL + String.format(ApiEndpointUtils.SUBMISSION_COMMENTS, submission.getIdentifier(), params), responseHandler);

    }


    public void moreChildrenAsync(Submission submission, List<String> children, CommentSort sort, CommentsResponseHandler responseHandler) {

        String childrenIds = TextUtils.join(",", children);

        RequestParams params = new RequestParams();
        params.put("api_type", "json");
        params.put("link_id", submission.getFullName());
        params.put("children", childrenIds);
        params.put("sort", sort.value());

        String url = ApiEndpointUtils.REDDIT_BASE_URL + ApiEndpointUtils.MORE_CHILDREN;

        restClient.getAsyncClient().post(url, params, responseHandler);
    }

}
