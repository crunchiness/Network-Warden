package network.warden;

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

import java.io.File;

import javax.mail.MessagingException;

import warden.R;


public class MainActivity extends Activity {

    //public static RunTCP runtcp;
    Button buttonStart;
    Button buttonStop;
    Button buttonSend;
    Intent serviceIntent;          //service
    static TextView logmsg;        //text box of log
    static TextView textbox;       //text box of email address

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

            stopService(serviceIntent);
            buttonStart.setEnabled(true);
            buttonStop.setEnabled(false);
        }
    }

    //"send mail" button listener
    class SendMail implements OnClickListener {
        @Override
        public void onClick(View v) {
            System.out.println("Sending Email");

            SendTask sTask = new SendTask();
            String emailAddress = textbox.getText().toString();
            sTask.execute(emailAddress);
        }
    }

    // email sending function
    class SendTask extends AsyncTask<String, Integer, String> {

        private boolean fileExists(String path) {
            return (new File(path)).exists();
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... params) {
            Mail m = new Mail(Secrets.senderEmail, Secrets.senderPassword);

            String address = (params[0].equals("")) ? "ingvaras@gmail.com" : params[0];

            String[] toArr = {address};
            m.setTo(toArr);
            m.setFrom(Secrets.senderEmail);
            m.setSubject("Network Traffic Log");
            m.setBody("Email body.");

            String[] attachments = {"log1.txt", "error.txt", "fail.txt"};

            try {
                for (String attachment : attachments) {
                    String path = "/data/local/Warden/" + attachment;
                    if (fileExists(path)) {
                        m.addAttachment(path);
                    } else {
                        Log.e("Network Warden", "Attachment file doesn't exist: " + path);
                    }
                }
                m.send();
            } catch (MessagingException e) {
                Log.e("Network Warden", "Could not send email", e);
                return "Could not send email";
            }
            return "Email sent!";
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
        }

        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(getApplicationContext(), result, Toast.LENGTH_SHORT).show();
            super.onPostExecute(result);
        }

    }
}
