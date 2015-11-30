package network.warden;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import warden.R;

public class RunningTCP extends Activity {

    OnClickListener listenerStop = null;
    Button buttonStop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_running_tcp);
        listenerStop = new OnClickListener() {
            public void onClick(View v) {
                RunningTCP.this.finish();
            }
        };
        buttonStop = (Button) findViewById(R.id.button1);
        buttonStop.setOnClickListener(listenerStop);
    }
}
