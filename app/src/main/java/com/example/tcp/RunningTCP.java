package com.example.tcp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class RunningTCP extends Activity {

    OnClickListener listenerStop = null;
    Button buttonStop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_running_tcp);
        listenerStop = new OnClickListener() {
            public void onClick(View v) {
                //MainActivity.runtcp.stop();
                //stopService();
                RunningTCP.this.finish();

                //Intent intent0 = new Intent(RunningTCP.this, MainActivity.class);
                //setTitle("FrameLayout");
                //startActivity(intent0);
            }
        };
        buttonStop = (Button) findViewById(R.id.button1);
        buttonStop.setOnClickListener(listenerStop);
    }

}
