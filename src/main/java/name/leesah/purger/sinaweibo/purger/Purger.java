package name.leesah.purger.sinaweibo.purger;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.sina.weibo.sdk.auth.Oauth2AccessToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;

import name.leesah.sinaweiboapi.DestroyingRequest;
import name.leesah.sinaweiboapi.ListingRequest;

import static com.android.volley.toolbox.Volley.newRequestQueue;
import static java.lang.String.format;
import static name.leesah.purger.sinaweibo.Constants.APP_KEY;

/**
 * Created by sah on 2017-04-13.
 */

public abstract class Purger extends JobService {
    static final String EXTRA_UID = "uid";
    static final String EXTRA_TOKEN = "token";
    public static final String PURGER_REPORT_ACTION = "Purger Report";
    public static final String EXTRA_MESSAGE = "report message";
    public static final String EXTRA_IS_DEBUG = "is debug message";
    private RequestQueue requestQueue;
    private JobParameters params;
    private String uid;
    private String token;
    private LocalBroadcastManager broadcastManager;

    @Override
    public boolean onStartJob(JobParameters params) {
        this.params = params;
        PersistableBundle extras = params.getExtras();
        uid = extras.getString(EXTRA_UID);
        token = extras.getString(EXTRA_TOKEN);

        requestQueue = newRequestQueue(this);
        new Handler().post(this::listItems);
        i("Purge started.");
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        stopRequestQueue();
        i("Purge stopped.");
        return true;
    }

    void listItems() {
        ListingRequest request = buildListingRequest(APP_KEY, uid, token, this::onListingResponse, this::onListingError);
        requestQueue.add(request);
        d(format("Request sent for listing: [%s]", request));
    }

    private void destroyItems(List<String> ids) {
        d(format("IDs found in listing response: [%s].", ids));
        for (String id : ids) {
            DestroyingRequest request = buildDestroyingRequest(APP_KEY, uid, token, id, this::onDestroyingResponse, this::onDestroyingError);
            requestQueue.add(request);
            d(format("Request sent for destroying: [%s].", request));
        }
    }

    public void onListingResponse(JSONObject response) {
        d(format("Response received for listing: [%s].", response));
        try {
            List<String> ids = getIdsFromListingResponse(response);
            if (ids.isEmpty())
                onPurgeComplete();
            else {
                destroyItems(ids);
                listItems();
            }
        } catch (JSONException e) {
            wtf("Error occurred while processing listing response.", e);
        }
    }

    public void onListingError(VolleyError error) {
        if (isNetworkOverheated(error))
            onNetworkOverheat();
        else {
            w(format("Error [] occurred while listing: [%s].", new String(error.networkResponse.data)));
            stopAndWaitForReschedule();
        }
    }

    void onDestroyingResponse(JSONObject response) {
        d(format("Response received for destroying: [%s]", response));
    }

    public void onDestroyingError(VolleyError error) {
        if (isNetworkOverheated(error))
            onNetworkOverheat();
        else {
            w(format("Error [%s] occurred while destroying: [%s].", error.networkResponse.statusCode, new String(error.networkResponse.data)));
        }
    }

    void onPurgeComplete() {
        i("No more item to destroy. Purge complete.");
        stopRequestQueue();
        jobFinished(params, false);
    }

    private void onNetworkOverheat() {
        i("Weibo API invocation limit exceeded. Cooling down.");
        stopAndWaitForReschedule();
    }

    void stopAndWaitForReschedule() {
        stopRequestQueue();
        jobFinished(params, true);
    }

    List<String> NETWORK_OVERHEAT_ERROR_CODES = Arrays.asList("10022", "10023", "10024");

    protected boolean isNetworkOverheated(VolleyError error) {
        try {
            JSONObject jsonObject = new JSONObject(new String(error.networkResponse.data));
            return NETWORK_OVERHEAT_ERROR_CODES.contains(jsonObject.getString("error_code"));
        } catch (JSONException e) {
            return false;
        }
    }

    void stopRequestQueue() {
        requestQueue.cancelAll(request -> true);
        requestQueue.stop();
        d("All requests are canceled and queue is stopped.");
    }

    private static final Class[] PURGE_JOB_CLASSES = new Class[]{
            StatusPurger.class,
            FriendshipPurger.class,
            CommentPurger.class};

    public static void scheduleJobs(Context context, Oauth2AccessToken accessToken) {
        JobScheduler jobScheduler = context.getSystemService(JobScheduler.class);
        ;
        for (Class clazz : PURGE_JOB_CLASSES)
            jobScheduler.schedule(buildJobInfo(context, accessToken, clazz));
    }

    private static JobInfo buildJobInfo(Context context, Oauth2AccessToken accessToken, Class clazz) {
        PersistableBundle extras = new PersistableBundle();
        extras.putString(EXTRA_UID, accessToken.getUid());
        extras.putString(EXTRA_TOKEN, accessToken.getToken());
        return new JobInfo.Builder(clazz.hashCode(), new ComponentName(context, clazz))
                .setExtras(extras)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setRequiresDeviceIdle(true)
                .setRequiresCharging(false)
                .setOverrideDeadline(1024)
                .setPersisted(true)
                .build();
    }

    private void w(String message) {
        Log.w(tag(), message);
        report(message, false);
    }

    private void i(String message) {
        Log.i(tag(), message);
        report(message, false);
    }

    private void d(String msg) {
        Log.d(tag(), msg);
        report(msg, true);
    }

    private void wtf(String message, JSONException e) {
        Log.wtf(tag(), message, e);
        report(message, true);
    }

    private void report(String message, boolean isDebug) {
        Intent intent = new Intent(PURGER_REPORT_ACTION);
        intent.putExtra(EXTRA_MESSAGE, message);
        intent.putExtra(EXTRA_IS_DEBUG, isDebug);
        if (broadcastManager == null)
            broadcastManager = LocalBroadcastManager.getInstance(this);
        broadcastManager.sendBroadcast(intent);
    }

    private String tag() {
        return getClass().getSimpleName();
    }

    @NonNull
    protected abstract ListingRequest buildListingRequest(String appkey, String uid, String token, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener);

    protected abstract List<String> getIdsFromListingResponse(JSONObject jsonObject) throws JSONException;

    protected abstract DestroyingRequest buildDestroyingRequest(String appkey, String uid, String token, String id, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener);

    void setRequestQueue(RequestQueue requestQueue) {
        this.requestQueue = requestQueue;
    }

    void setParams(JobParameters params) {
        this.params = params;
    }

    void setBroadcastManager(LocalBroadcastManager broadcastManager) {
        this.broadcastManager = broadcastManager;
    }
}
