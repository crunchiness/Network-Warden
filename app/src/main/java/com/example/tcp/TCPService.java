package com.example.tcp;

import java.io.IOException;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class TCPService extends Service {
    RunTCP runtcp;

    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void onCreate() {
        System.out.println("service created");
        //MainActivity.ShowMsg("service created");
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        System.out.println("service destroyed");
        MainActivity.ShowMsg("service destroyed");
        try {
            runtcp.DestroyTCP();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        super.onDestroy();
    }

    @Override
    public void onStart(Intent intent, int startid) {
        System.out.println("Starting service ......");
//        MainActivity.ShowMsg("Starting service ......");
        runtcp = new RunTCP();
        if (runtcp.ready) {
            try {
                runtcp.OpenFile();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            runtcp.start();
        } else {
            System.out.println("failed to start");
            MainActivity.ShowMsg("failed to start");
        }
    }
}
/*
5.MainActivity
public class MainActivity extends Activity implements OnClickListener {
private static final String TAG = "ServiceDemo";
Button buttonStart;
Button buttonStop;

@Override
public void onCreate(Bundle savedInstanceState) {
super.onCreate(savedInstanceState);
setContentView(R.layout.main);

/*
* TextView tv = (TextView)findViewById(R.id.tvTemp); tv.setText("");
*/
/*
buttonStart = (Button) findViewById(R.id.btnStart);
buttonStop = (Button) findViewById(R.id.btnStop);

buttonStart.setOnClickListener(this);
buttonStop.setOnClickListener(this);
}

public void onClick(View src) {
switch (src.getId()) {
case R.id.btnStart:
Log.d(TAG, "onClick: starting srvice");
startService(new Intent(this, MyService.class));
break;
case R.id.btnStop:
Log.d(TAG, "onClick: stopping srvice");
stopService(new Intent(this, MyService.class));
break;
}
}
}
*/