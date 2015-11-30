package network.warden;

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
        try {
            runtcp.destroyTCP();
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
                runtcp.openFiles();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            runtcp.start();
        } else {
            System.out.println("failed to start");
        }
    }
}
