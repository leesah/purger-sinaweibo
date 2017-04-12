package name.leesah.purger.sinaweibo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.sina.weibo.sdk.auth.AuthInfo;
import com.sina.weibo.sdk.auth.Oauth2AccessToken;
import com.sina.weibo.sdk.auth.WeiboAuthListener;
import com.sina.weibo.sdk.auth.sso.SsoHandler;
import com.sina.weibo.sdk.exception.WeiboException;

import name.leesah.purger.sinaweibo.purger.Purger;

import static com.sina.weibo.sdk.auth.sso.AccessTokenKeeper.readAccessToken;
import static com.sina.weibo.sdk.auth.sso.AccessTokenKeeper.writeAccessToken;
import static java.lang.String.format;
import static name.leesah.purger.sinaweibo.Constants.APP_KEY;
import static name.leesah.purger.sinaweibo.Constants.REDIRECT_URL;
import static name.leesah.purger.sinaweibo.Constants.SCOPE;

public class MainActivity extends Activity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private Oauth2AccessToken accessToken;
    private SsoHandler ssoHandler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.fab_purge).setOnClickListener(v -> step1_thisDeletesData());
    }

    private void step1_thisDeletesData() {
        showAlertDialogAndContinue(R.string.dialog_title_step_1, R.string.dialog_message_step_1, this::step2_login);
    }

    private void step2_login() {
        accessToken = readAccessToken(this);
        if (accessToken.isSessionValid()) {
            Log.i(TAG, format("Token found: [%s]. No need for authentication.", accessToken));
            step3_areYouSure();
        } else {
            doLoginAndContinue(this::step3_areYouSure);
        }
    }

    private void step3_areYouSure() {
        showAlertDialogAndContinue(R.string.dialog_title_step_3, R.string.dialog_message_step_3,  this::step4_schedulePurgerJob);
    }

    private void step4_schedulePurgerJob() {
        Purger.scheduleJobs(this, accessToken);

        step5_switchingToBackground();
    }

    private void step5_switchingToBackground() {
        showAlertDialogAndContinue(R.string.dialog_title_step_5, R.string.dialog_message_step_5, this::step6_showMonitorOrDisappear);
    }

    private void step6_showMonitorOrDisappear() {
        if (BuildConfig.DEBUG)
            startActivity(new Intent(this, MonitorActivity.class));

        finish();
    }

    private void showAlertDialogAndContinue(int title, int message, Runnable nextStep) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, (d, b) -> nextStep.run())
                .setNegativeButton(android.R.string.no, (d, b) -> Log.w(TAG, "Canceled by user."))
                .show();
    }

    private void doLoginAndContinue(Runnable nextStep) {
        Log.d(TAG, "Starting SSO authentication...");
        ssoHandler = new SsoHandler(this, new AuthInfo(this, APP_KEY, REDIRECT_URL, SCOPE));

        ssoHandler.authorize(new WeiboAuthListener() {
            @Override
            public void onComplete(final Bundle values) {
                MainActivity.this.runOnUiThread(() -> {
                    accessToken = Oauth2AccessToken.parseAccessToken(values);
                    if (accessToken.isSessionValid()) {
                        Log.i(TAG, format("Authentication succeeded: [%s].", accessToken));
                        writeAccessToken(MainActivity.this, accessToken);
                        nextStep.run();

                    } else {
                        Log.e(TAG, format("Error occurred during authentication: code = [%s].", values.getString("code")));
                    }
                });
            }

            @Override
            public void onCancel() {
                Log.w(TAG, "Authentication canceled by user.");
            }

            @Override
            public void onWeiboException(WeiboException e) {
                Log.e(TAG, "Exception caught during authentication.", e);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (ssoHandler != null)
            ssoHandler.authorizeCallBack(requestCode, resultCode, data);
    }

}