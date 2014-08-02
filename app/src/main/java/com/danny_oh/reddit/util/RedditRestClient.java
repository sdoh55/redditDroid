package com.danny_oh.reddit.util;

import android.content.Context;

import com.github.jreddit.exception.ActionFailedException;
import com.github.jreddit.exception.InvalidURIException;
import com.github.jreddit.exception.RetrievalFailedException;
import com.github.jreddit.utils.ApiEndpointUtils;
import com.github.jreddit.utils.restclient.HttpRestClient;
import com.github.jreddit.utils.restclient.Response;
import com.github.jreddit.utils.restclient.RestClient;
import com.github.jreddit.utils.restclient.RestResponseHandler;

import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.json.simple.parser.ParseException;

import com.github.jreddit.utils.restclient.methodbuilders.HttpGetMethodBuilder;
import com.github.jreddit.utils.restclient.methodbuilders.HttpPostMethodBuilder;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.PersistentCookieStore;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import static com.github.jreddit.utils.restclient.methodbuilders.HttpGetMethodBuilder.httpGetMethod;
import static com.github.jreddit.utils.restclient.methodbuilders.HttpPostMethodBuilder.httpPostMethod;


/**
 * Created by danny on 7/24/14.
 */
public class RedditRestClient implements RestClient {
    private static final String BASE_URL = ApiEndpointUtils.REDDIT_BASE_URL;

    private static AsyncHttpClient mClient = new AsyncHttpClient();
    private static PersistentCookieStore mCookieStore;

    private static RedditRestClient instance;

    private Context mContext;



    public AsyncHttpClient getAsyncClient() {
        return mClient;
    }



    /**
     * HTTP Client instance.
     */
    private final HttpClient mHttpClient;

    /**
     * Response handler instance.
     */
    private final ResponseHandler<Response> mResponseHandler;







    public static RedditRestClient getInstance(Context context) {
        if (instance == null) {
            instance = new RedditRestClient(context);
        }

        return instance;
    }

    public RedditRestClient(Context context) {
        mHttpClient = mClient.getHttpClient();
        mResponseHandler = new RestResponseHandler();

        mContext = context;
        mCookieStore = new PersistentCookieStore(mContext);
        mClient.setCookieStore(mCookieStore);

        setUserAgent(Constants.USER_AGENT);
    }


    public PersistentCookieStore getCookieStore(){
        return mCookieStore;
    }

/**
 * RestClient interface override
 */

    /**
     * Set the userAgent to be used when making http requests
     *
     * @param agent the string to be used as the userAgent
     */
    public void setUserAgent(String agent) {
        mClient.setUserAgent(agent);
    }

    @Override
    public Response get(String urlPath, String cookie) throws RetrievalFailedException {

        try {
            Response result = get(httpGetMethod().withUrl(ApiEndpointUtils.REDDIT_BASE_URL + urlPath));
            if (result == null) {
                throw new RetrievalFailedException("The given URI path does not exist on Reddit: " + urlPath);
            } else {
                return result;
            }
        } catch (URISyntaxException e) {
            throw new RetrievalFailedException("The syntax of the URI path was incorrect: " + urlPath);
        } catch (InvalidURIException e) {
            throw new RetrievalFailedException("The URI path was invalid: " + urlPath);
        } catch (IOException e) {
            throw new RetrievalFailedException("Input/output failed when retrieving from URI path: " + urlPath);
        } catch (ParseException e) {
            throw new RetrievalFailedException("Failed to parse the response from GET request to URI path: "+ urlPath);
        }

    }

    public Response get(HttpGetMethodBuilder getMethodBuilder) throws IOException, ParseException, InvalidURIException {
        HttpGet request = getMethodBuilder.build();

        // Execute request
        Response response = mHttpClient.execute(request, mResponseHandler, mClient.getHttpContext());

        // A HTTP error occurred
        if (response != null && response.getStatusCode() >= 300) {
            throw new RetrievalFailedException("HTTP Error (" + response.getStatusCode() + ") occurred for URI path: " + request.getURI().toString());
        }

        return response;
    }



    @Override
    public Response post(String apiParams, String urlPath, String cookie) {

        try {
            Response result = post(
                    httpPostMethod()
                            .withUrl(ApiEndpointUtils.REDDIT_BASE_URL + urlPath),
                    convertRequestStringToList(apiParams)
            );
            if (result == null) {
                throw new ActionFailedException("Due to unknown reasons, the response was undefined for URI path: " + urlPath);
            } else {
                return result;
            }
        } catch (URISyntaxException e) {
            throw new ActionFailedException("The syntax of the URI path was incorrect: " + urlPath);
        } catch (IOException e) {
            throw new ActionFailedException("Input/output failed when retrieving from URI path: " + urlPath);
        } catch (ParseException e) {
            throw new ActionFailedException("Failed to parse the response from GET request to URI path: "+ urlPath);
        }

    }


    public Response post(HttpPostMethodBuilder postMethodBuilder, List<NameValuePair> params) throws IOException, ParseException {
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params, "UTF-8");

        // Set entity
        HttpPost request = postMethodBuilder.build();
        request.setEntity(entity);

        // Execute request
        Response response = mHttpClient.execute(request, mResponseHandler, mClient.getHttpContext());


        // A HTTP error occurred
        if (response != null && response.getStatusCode() >= 300) {
            throw new ActionFailedException("HTTP Error (" + response.getStatusCode() + ") occurred for URI path: " + request.getURI().toString());
        }

        return response;

    }

    @Override
    public Response postSecure(String apiParams, String urlPath, String cookie) {
        try {
            Response result = post(
                    httpPostMethod().withUrl(ApiEndpointUtils.REDDIT_SSL_URL + urlPath),
                    convertRequestStringToList(apiParams)
            );
            if (result == null) {
                throw new ActionFailedException("Due to unknown reasons, the response was undefined for URI path: " + urlPath);
            } else {
                return result;
            }
        } catch (URISyntaxException e) {
            throw new ActionFailedException("The syntax of the URI path was incorrect: " + urlPath);
        } catch (IOException e) {
            throw new ActionFailedException("Input/output failed when retrieving from URI path: " + urlPath);
        } catch (ParseException e) {
            throw new ActionFailedException("Failed to parse the response from GET request to URI path: "+ urlPath);
        }
    }




    private List<NameValuePair> convertRequestStringToList(String apiParams) {
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        if (apiParams != null && !apiParams.isEmpty()) {
            String[] valuePairs = apiParams.split("&");
            for (String valuePair : valuePairs) {
                String[] nameValue = valuePair.split("=");
                params.add(new BasicNameValuePair(nameValue[0], nameValue[1]));
            }
        }
        return params;
    }

}