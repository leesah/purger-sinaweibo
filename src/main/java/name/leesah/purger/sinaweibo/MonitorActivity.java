package name.leesah.purger.sinaweibo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.widget.TextView;

import org.apache.commons.lang3.StringUtils;

import static name.leesah.purger.sinaweibo.purger.Purger.EXTRA_IS_DEBUG;
import static name.leesah.purger.sinaweibo.purger.Purger.EXTRA_MESSAGE;
import static name.leesah.purger.sinaweibo.purger.Purger.PURGER_REPORT_ACTION;

public class MonitorActivity extends AppCompatActivity {

    private TextView logView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monitor);

        logView = (TextView) findViewById(R.id.log);
        logView.setMovementMethod(new ScrollingMovementMethod());

        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                boolean isDebug = intent.getBooleanExtra(EXTRA_IS_DEBUG, false);
                String message = intent.getStringExtra(EXTRA_MESSAGE);
                if (!isDebug || BuildConfig.DEBUG) {
                    logView.append(message);
                    logView.append(StringUtils.LF);
                }
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, new IntentFilter(PURGER_REPORT_ACTION));

    }


}
