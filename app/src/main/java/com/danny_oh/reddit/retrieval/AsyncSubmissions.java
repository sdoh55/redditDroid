package com.danny_oh.reddit.retrieval;

import com.danny_oh.reddit.util.RedditRestClient;
import com.github.jreddit.entity.Kind;
import com.github.jreddit.entity.Submission;
import com.github.jreddit.entity.User;
import com.github.jreddit.exception.RedditError;
import com.github.jreddit.exception.RetrievalFailedException;
import com.github.jreddit.retrieval.Submissions;
import com.github.jreddit.retrieval.params.QuerySyntax;
import com.github.jreddit.retrieval.params.SearchSort;
import com.github.jreddit.retrieval.params.TimeSpan;
import com.github.jreddit.retrieval.params.UserOverviewSort;
import com.github.jreddit.retrieval.params.UserSubmissionsCategory;
import com.github.jreddit.utils.ApiEndpointUtils;
import com.github.jreddit.utils.ParamFormatter;
import com.github.jreddit.utils.RedditConstants;
import com.github.jreddit.utils.restclient.Response;
import com.github.jreddit.utils.restclient.RestClient;
import com.github.jreddit.utils.restclient.RestResponse;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.ResponseHandlerInterface;
import com.loopj.android.http.TextHttpResponseHandler;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.LinkedList;
import java.util.List;

import static com.github.jreddit.utils.restclient.JsonUtils.safeJsonToString;

/**
 * Created by danny on 8/1/14.
 */
public class AsyncSubmissions extends Submissions {
    private RedditRestClient mRestClient;


    public static abstract class SubmissionsResponseHandler extends TextHttpResponseHandler {
        private final JSONParser mJsonParser;

        public SubmissionsResponseHandler() {
            mJsonParser = new JSONParser();
        }

        public abstract void onParseFinished(List<Submission> submissions);

        @Override
        public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {

        }

        @Override
        public void onSuccess(int statusCode, Header[] headers, String responseString) {
            try {
                Object json = mJsonParser.parse(responseString);
                List<Submission> submissions = parseJson(json);

                onParseFinished(submissions);

            } catch (ParseException pe) {
                pe.printStackTrace();

            }

        }
    }

    /**
     * Parses a JSON feed received from Reddit (URL) into a nice list of Submission objects.
     *
     * @param response
     * @return 		Listing of submissions
     */
    private static List<Submission> parseJson(Object response) {

        // List of submissions
        List<Submission> submissions = new LinkedList<Submission>();

        if (response instanceof JSONObject) {

            JSONObject object = (JSONObject) response;
            if (object.get("error") != null) {
                throw new RedditError("Response contained error code " + object.get("error") + ".");
            }
            JSONArray array = (JSONArray) ((JSONObject) object.get("data")).get("children");

            // Iterate over the submission results
            JSONObject data;
            Submission submission;
            for (Object anArray : array) {
                data = (JSONObject) anArray;

                // Make sure it is of the correct kind
                String kind = safeJsonToString(data.get("kind"));
                if (kind.equals(Kind.LINK.value())) {

                    // Create and add submission
                    data = ((JSONObject) data.get("data"));
                    submission = new Submission(data);
                    submissions.add(submission);

                }

            }

        } else {
            System.err.println("Cannot cast to JSON Object: '" + response.toString() + "'");
        }

        // Finally return list of submissions
        return submissions;

    }






    /**
     * Constructor.
     * Default general actor will be used.
     * @param restClient REST client handle
     */
    public AsyncSubmissions(RedditRestClient restClient) {
        super(restClient);
        mRestClient = restClient;
    }

    /**
     * Constructor. The actor is the user who will
     * be used to perform the retrieval.
     *
     * @param restClient REST Client instance
     * @param actor User instance
     */
    public AsyncSubmissions(RedditRestClient restClient, User actor) {
        super(restClient, actor);
        mRestClient = restClient;
    }


    /**
     * Searches with the given query using the constraints given as parameters.
     * The parameters here are in Strings instead of wrapper objects, which allows users
     * to manually adjust the parameters (if the API changes and jReddit is not updated
     * in time yet).
     *
     * @param query 			The query
     * @param syntax			The query syntax
     * @param sort				Search sorting method
     * @param time				Search time
     * @param count				Count at which the submissions are started being numbered
     * @param limit				Maximum amount of submissions that can be returned (0-100, 25 default (see Reddit API))
     * @param after				The submission after which needs to be retrieved
     * @param before			The submission before which needs to be retrieved
     * @param show  			Show all (disables filters such as "hide links that I have voted on")
     * @return 					The linked list containing submissions
     */
    protected void searchAsync(String subreddit,
                               String query,
                               String syntax,
                               String sort,
                               String time,
                               String count,
                               String limit,
                               String after,
                               String before,
                               String show,
                               SubmissionsResponseHandler responseHandler) throws RetrievalFailedException, RedditError {
        // Format parameters
        String params = "";
        try {
            params = ParamFormatter.addParameter(params, "q", URLEncoder.encode(query, "ISO-8859-1"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        params = ParamFormatter.addParameter(params, "syntax", syntax);
        params = ParamFormatter.addParameter(params, "sort", sort);
        params = ParamFormatter.addParameter(params, "t", time);
        params = ParamFormatter.addParameter(params, "count", count);
        params = ParamFormatter.addParameter(params, "limit", limit);
        params = ParamFormatter.addParameter(params, "after", after);
        params = ParamFormatter.addParameter(params, "before", before);
        params = ParamFormatter.addParameter(params, "show", show);

        String url;

        if (subreddit.isEmpty())
            url = ApiEndpointUtils.REDDIT_BASE_URL + String.format(ApiEndpointUtils.SUBMISSIONS_SEARCH, params);
        else
            url = ApiEndpointUtils.REDDIT_BASE_URL + "/r/" + subreddit + String.format(ApiEndpointUtils.SUBMISSIONS_SEARCH, params) + "&restrict_sr=true";

        // Retrieve submissions from the given URL
        mRestClient.getAsyncClient().get(url, responseHandler);

    }

    /**
     * Searches with the given query using the constraints given as parameters.
     *
     * @param query 			The query
     * @param syntax			The query syntax
     * @param sort				Search sorting method
     * @param time				Search time
     * @param count				Count at which the submissions are started being numbered
     * @param limit				Maximum amount of submissions that can be returned (0-100, 25 default (see Reddit API))
     * @param after				The submission after which needs to be retrieved
     * @param before			The submission before which needs to be retrieved
     * @param show_all			Show all (disables filters such as "hide links that I have voted on")
     */
    public void searchAsync(String subreddit,
                            String query,
                            QuerySyntax syntax,
                            SearchSort sort,
                            TimeSpan time,
                            int count,
                            int limit,
                            Submission after,
                            Submission before,
                            boolean show_all,
                            SubmissionsResponseHandler responseHandler) throws RetrievalFailedException, IllegalArgumentException {

        if (query == null || query.isEmpty()) {
            throw new IllegalArgumentException("The query must be defined.");
        }

        if (limit < -1 || limit > RedditConstants.MAX_LIMIT_LISTING) {
            throw new IllegalArgumentException("The limit needs to be between 0 and 100 (or -1 for default).");
        }

        searchAsync(
                subreddit,
                query,
                (syntax != null) ? syntax.value() : "",
                (sort != null) ? sort.value() : "",
                (time != null) ? time.value() : "",
                String.valueOf(count),
                String.valueOf(limit),
                (after != null) ? after.getFullName() : "",
                (before != null) ? before.getFullName() : "",
                (show_all) ? "all" : "",
                responseHandler
        );
    }



    /**
     * Get the submissions of a user.
     * In this variant all parameters are Strings.
     *
     * @param username	 		Username of the user you want to retrieve from.
     * @param category    		(Optional, set null/empty if not used) Category in the user overview to retrieve submissions from
     * @param sort	    		(Optional, set null/empty if not used) Sorting method.
     * @param time		 		(Optional, set null/empty is not used) Time window
     * @param count        		(Optional, set null/empty if not used) Number at which the counter starts
     * @param limit        		(Optional, set null/empty if not used) Integer representing the maximum number of comments to return
     * @param after				(Optional, set null/empty if not used) After which comment to retrieve
     * @param before			(Optional, set null/empty if not used) Before which comment to retrieve
     * @param show				(Optional, set null/empty if not used) Show parameter ('given' is only acceptable value)
     *
     * @return Comments of a user.
     */
    protected void ofUserAsync(String username, String category, String sort, String count, String limit, String after, String before, String show, SubmissionsResponseHandler handler) throws RetrievalFailedException, RedditError {

        // Format parameters
        String params = "";
        params = ParamFormatter.addParameter(params, "sort", sort);
        params = ParamFormatter.addParameter(params, "count", count);
        params = ParamFormatter.addParameter(params, "limit", limit);
        params = ParamFormatter.addParameter(params, "after", after);
        params = ParamFormatter.addParameter(params, "before", before);
        params = ParamFormatter.addParameter(params, "show", show);

        // Retrieve submissions from the given URL
        mRestClient.getAsyncClient().get(ApiEndpointUtils.REDDIT_BASE_URL + String.format(ApiEndpointUtils.USER_SUBMISSIONS_INTERACTION, username, category, params), handler);

    }

    /**
     * Get the submissions of a user.
     * In this variant all parameters are Strings.
     *
     * @param username	 		Username of the user you want to retrieve from.
     * @param category    		Category in the user overview to retrieve submissions from
     * @param sort	    		(Optional, set null if not used) Sorting method.
     * @param time		 		(Optional, set null is not used) Time window
     * @param count        		(Optional, set -1 if not used) Number at which the counter starts
     * @param limit        		(Optional, set -1 if not used) Integer representing the maximum number of comments to return
     * @param after				(Optional, set null if not used) After which comment to retrieve
     * @param before			(Optional, set null if not used) Before which comment to retrieve
     * @param show				(Optional, set false if not used) Show parameter ('given' is only acceptable value)
     *
     * @return Submissions of a user.
     */
    public void ofUserAsync(String username, UserSubmissionsCategory category, UserOverviewSort sort, int count, int limit, Submission after, Submission before, boolean show_given, SubmissionsResponseHandler handler) throws RetrievalFailedException, IllegalArgumentException {

        if (username == null || username.isEmpty()) {
            throw new IllegalArgumentException("The username must be defined.");
        }

        if (category == null) {
            throw new IllegalArgumentException("The category must be defined.");
        }

        if (limit < -1 || limit > RedditConstants.MAX_LIMIT_LISTING) {
            throw new IllegalArgumentException("The limit needs to be between 0 and 100 (or -1 for default).");
        }

        ofUserAsync(
                username,
                (category != null) ? category.value() : "",
                (sort != null) ? sort.value() : "",
                String.valueOf(count),
                String.valueOf(limit),
                (after != null) ? after.getFullName() : "",
                (before != null) ? before.getFullName() : "",
                (show_given) ? "given" : "",
                handler
        );
    }

}
