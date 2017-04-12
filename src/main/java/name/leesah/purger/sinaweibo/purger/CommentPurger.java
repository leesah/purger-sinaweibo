package name.leesah.purger.sinaweibo.purger;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.android.volley.Response;
import com.google.common.collect.Lists;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import name.leesah.sinaweiboapi.DestroyingRequest;
import name.leesah.sinaweiboapi.ListingRequest;
import name.leesah.sinaweiboapi.comment.DestroyComment;
import name.leesah.sinaweiboapi.comment.ListComments;

/**
 * Created by sah on 2017-04-09.
 */

public class CommentPurger extends Purger {

    @NonNull
    protected ListingRequest buildListingRequest(String appkey, String uid, String token, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        return new ListComments(appkey, uid, token, listener, errorListener);
    }

    @Override
    protected List<String> getIdsFromListingResponse(JSONObject jsonObject) throws JSONException {
        JSONArray jsonArray = jsonObject.getJSONArray("comments");
        List<String> flat = new ArrayList<>(jsonArray.length());
        for (int i = 0; i < jsonArray.length(); i++)
            flat.add(jsonArray.getJSONObject(i).getString("id"));

        List<List<String>> partitioned = Lists.partition(flat, 20);
        List<String> ids = new ArrayList<>(partitioned.size());
        for (List<String> partition : partitioned) {
            ids.add(TextUtils.join(",", partition));
        }

        return ids;
    }

    @Override
    protected DestroyingRequest buildDestroyingRequest(String appkey, String uid, String token, String id, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        return new DestroyComment(appkey, token, id, listener, errorListener);
    }

}
