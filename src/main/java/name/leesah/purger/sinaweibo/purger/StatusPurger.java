package name.leesah.purger.sinaweibo.purger;

import android.support.annotation.NonNull;

import com.android.volley.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import name.leesah.sinaweiboapi.DestroyingRequest;
import name.leesah.sinaweiboapi.ListingRequest;
import name.leesah.sinaweiboapi.status.DestroyStatus;
import name.leesah.sinaweiboapi.status.ListStatuses;

/**
 * Created by sah on 2017-04-09.
 */

public class StatusPurger extends Purger {

    @Override
    @NonNull
    protected ListingRequest buildListingRequest(String appkey, String uid, String token, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        return new ListStatuses(appkey, uid, token, listener, errorListener);
    }

    @Override
    protected List<String> getIdsFromListingResponse(JSONObject jsonObject) throws JSONException {
        JSONArray jsonArray = jsonObject.getJSONArray("statuses");
        List<String> ids = new ArrayList<>(jsonArray.length());
        for (int i = 0; i < jsonArray.length(); i++)
            ids.add(jsonArray.getString(i));
        return ids;
    }

    @Override
    protected DestroyingRequest buildDestroyingRequest(String appkey, String uid, String token, String id, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        return new DestroyStatus(appkey, token, id, listener, errorListener);
    }

}
