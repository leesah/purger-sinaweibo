package name.leesah.purger.sinaweibo.purger;

import android.annotation.SuppressLint;
import android.app.job.JobParameters;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;

import com.android.volley.NetworkResponse;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import name.leesah.sinaweiboapi.DestroyingRequest;
import name.leesah.sinaweiboapi.ListingRequest;

import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static name.leesah.purger.sinaweibo.purger.PurgerTest.FooPurger.DESTROYING_REQUEST_1;
import static name.leesah.purger.sinaweibo.purger.PurgerTest.FooPurger.DESTROYING_REQUEST_2;
import static name.leesah.purger.sinaweibo.purger.PurgerTest.FooPurger.DESTROYING_RESPONSE;
import static name.leesah.purger.sinaweibo.purger.PurgerTest.FooPurger.LISTING_REQUEST;
import static name.leesah.purger.sinaweibo.purger.PurgerTest.FooPurger.LISTING_RESPONSE_WITH_0_ID;
import static name.leesah.purger.sinaweibo.purger.PurgerTest.FooPurger.LISTING_RESPONSE_WITH_2_IDS;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * Created by sah on 2017-04-14.
 */
@RunWith(RobolectricTestRunner.class)
public class PurgerTest {
    private static final String NETWORK_OVERHEAT_JSON = format("{\"error_code\":\"%d\"}", 10022);

    @Mock
    private RequestQueue requestQueue;
    @Mock
    private JobParameters params;
    @Mock
    private LocalBroadcastManager broadcastManager;

    private Purger purger;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        purger = new FooPurger(requestQueue, params, broadcastManager);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void purge() throws Exception {
        purger.listItems();

        verify(requestQueue).add(same(LISTING_REQUEST));
        verifyNoMoreInteractions(requestQueue);
    }

    @Test
    public void onStopJob() throws Exception {
        purger.onStopJob(params);

        verify(requestQueue).cancelAll(notNull());
        verify(requestQueue).stop();
        verifyNoMoreInteractions(requestQueue);
    }

    @Test
    public void onListingResponseWith2Ids() throws Exception {
        purger.onListingResponse(LISTING_RESPONSE_WITH_2_IDS);

        verify(requestQueue).add(same(DESTROYING_REQUEST_1));
        verify(requestQueue).add(same(DESTROYING_REQUEST_2));
        verify(requestQueue).add(same(LISTING_REQUEST));
        verifyNoMoreInteractions(requestQueue);
    }

    @Test
    public void onListingResponseWith0Id() throws Exception {
        purger.onListingResponse(LISTING_RESPONSE_WITH_0_ID);

        verify(requestQueue).cancelAll(notNull());
        verify(requestQueue).stop();
        verifyNoMoreInteractions(requestQueue);
    }

    @Test
    public void onListingErrorNetworkOverheat() throws Exception {
        NetworkResponse response = new NetworkResponse(403, NETWORK_OVERHEAT_JSON.getBytes(), emptyMap(), false);
        purger.onListingError(new VolleyError(response));

        verify(requestQueue).cancelAll(notNull());
        verify(requestQueue).stop();
        verifyNoMoreInteractions(requestQueue);
    }

    @Test
    public void onListingErrorOther() throws Exception {
        NetworkResponse response = new NetworkResponse(403, new byte[]{}, emptyMap(), false);
        purger.onListingError(new VolleyError(response));

        verify(requestQueue).cancelAll(notNull());
        verify(requestQueue).stop();
        verifyNoMoreInteractions(requestQueue);
    }

    @Test
    public void onDestroyingResponse() throws Exception {
        purger.onDestroyingResponse(DESTROYING_RESPONSE);

        verifyZeroInteractions(requestQueue);
    }

    @Test
    public void onDestroyingErrorNetworkOverheat() throws Exception {
        NetworkResponse response = new NetworkResponse(403, NETWORK_OVERHEAT_JSON.getBytes(), emptyMap(), false);
        purger.onDestroyingError(new VolleyError(response));

        verify(requestQueue).cancelAll(notNull());
        verify(requestQueue).stop();
        verifyNoMoreInteractions(requestQueue);
    }

    @Test
    public void onDestroyingErrorOther() throws Exception {
        NetworkResponse response = new NetworkResponse(403, new byte[]{}, emptyMap(), false);
        purger.onDestroyingError(new VolleyError(response));

        verifyZeroInteractions(requestQueue);
    }

    /**
     * Created by sah on 2017-04-14.
     */

    @SuppressLint("Registered")
    static class FooPurger extends Purger {

        static final ListingRequest LISTING_REQUEST = mock(ListingRequest.class);
        static final List<String> IDS = Arrays.asList("1", "2");
        static final DestroyingRequest DESTROYING_REQUEST_1 = mock(DestroyingRequest.class);
        static final DestroyingRequest DESTROYING_REQUEST_2 = mock(DestroyingRequest.class);
        static final JSONObject LISTING_RESPONSE_WITH_2_IDS = mock(JSONObject.class);
        static final JSONObject LISTING_RESPONSE_WITH_0_ID = mock(JSONObject.class);
        static final JSONObject DESTROYING_RESPONSE = mock(JSONObject.class);
        ;


        public FooPurger(RequestQueue requestQueue, JobParameters params, LocalBroadcastManager broadcastManager) {
            super();
            setRequestQueue(requestQueue);
            setParams(params);
            setBroadcastManager(broadcastManager);
        }

        @NonNull
        @Override
        protected ListingRequest buildListingRequest(String appkey, String uid, String token, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
            return LISTING_REQUEST;
        }

        @Override
        protected List<String> getIdsFromListingResponse(JSONObject jsonObject) throws JSONException {
            if (jsonObject == LISTING_RESPONSE_WITH_2_IDS)
                return IDS;
            else if (jsonObject == LISTING_RESPONSE_WITH_0_ID)
                return Collections.emptyList();
            else
                return null;
        }

        @Override
        protected DestroyingRequest buildDestroyingRequest(String appkey, String uid, String token, String id, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
            switch (id) {
                case "1":
                    return DESTROYING_REQUEST_1;
                case "2":
                    return DESTROYING_REQUEST_2;
                default:
                    return mock(DestroyingRequest.class);
            }
        }

        @Override
        protected void onPurgeComplete() {
            stopRequestQueue();
        }

        @Override
        protected void stopAndWaitForReschedule() {
            stopRequestQueue();
        }

    }

}