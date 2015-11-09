package com.example.tcp;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends Activity {

    //public static RunTCP runtcp;
    OnClickListener listenerStart = null;
    Button buttonStart;
    Button buttonStop;
    Button buttonSend;
    Intent serviceIntent;          //service
    static TextView logmsg;        //text box of log
    static TextView textbox;       //text box of email address
    int IsSent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {    //initialization
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        serviceIntent = new Intent(this, TCPService.class);
        logmsg = (TextView) findViewById(R.id.editText1);
        logmsg.setEnabled(false);

        textbox = (TextView) findViewById(R.id.editText2);

        buttonStart = (Button) findViewById(R.id.button1);
        buttonStart.setOnClickListener(new StartListener());
        buttonStop = (Button) findViewById(R.id.button2);
        buttonStop.setOnClickListener(new StopListener());
        buttonSend = (Button) findViewById(R.id.button3);
        buttonSend.setOnClickListener(new SendMail());

        System.out.println("Program is ready ");
        ShowMsg("Program is ready");
    }

    //"start" button listener
    class StartListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            System.out.println("pressed start botton");
            logmsg.setText("pressed start button");
            buttonStart.setEnabled(false);
            buttonStop.setEnabled(true);
            startService(serviceIntent);
        }
    }

    //"Stop" button listener
    class StopListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            System.out.println("pressed stop button");
            //TextView logmsg = (TextView) findViewById(R.id.editText1);
            ShowMsg("pressed stop button");

            stopService(serviceIntent);
            buttonStart.setEnabled(true);
            buttonStop.setEnabled(false);
            //Intent intent0 = new Intent(MainActivity.this, RunningTCP.class);
            //startActivity(intent0);
        }
    }

    //"send mail" button listener
    class SendMail implements OnClickListener {
        @Override
        public void onClick(View v) {
            System.out.println("Sending Email");
            ShowMsg("Sending Email");

            SendTask sTask = new SendTask();
            sTask.execute();
        }
    }

    //show massage on log text box
    public static void ShowMsg(String msg) {
        //TextView logmsg = (TextView) findViewById(R.id.editText1);
//        CharSequence oldmsg = logmsg.getText();
//        logmsg.setText(oldmsg + msg + "\n");
    }

    //email sending function
    class SendTask extends AsyncTask<Integer, Integer, String> {
        @Override
        protected void onPreExecute() {
            Toast.makeText(getApplicationContext(), "Begin Send!", Toast.LENGTH_SHORT).show();
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(Integer... params) {
            Mail m = new Mail("networkwarden@gmail.com", "network2014");     //username and password of email

            CharSequence address = textbox.getText();

            String[] toArr = {address.toString()};
            m.setTo(toArr);
            m.setFrom("NetworkWarden@gmain.com");
            m.setSubject("Network Traffic Log");
            m.setBody("Email body.");

            try {
                //If you want add attachment use function addAttachment.
                m.addAttachment("/data/local/Warden/log.txt");    //add attachment

                if (m.send()) {
                    IsSent = 1;
                } else {
                    IsSent = 2;

                }
            } catch (Exception e) {
                //Toast.makeText(MailApp.this, "There was a problem sending the email.", Toast.LENGTH_LONG).show();

                Log.e("MailApp", "Could not send email", e);
            }

            return "";
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
        }

        @Override
        protected void onPostExecute(String r) {
            super.onPostExecute(r);
        }

    }
}



