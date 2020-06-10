package com.witechsms.smssender;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.telephony.SmsManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.concurrent.TimeUnit.SECONDS;

public class MainActivity extends AppCompatActivity {

    private static final int SMS_PERMISSION_CODE = 0;
    HttpExchange httpExchange;
    char sms_sending = 'p'; // p - processing, s - sms sent response, n - sms not sent response
    public static Runnable sendAt;
    public static ScheduledExecutorService scheduler;
    private static int smsPeriodTosent = 1; // sms time interval in seconds
    private TextView log;
    private TextView queue_log;
    private TextView received_ts_log;
    private TextView sms_ts_log;
    private TextView sms_num_log;
    private TextView sms_stat_log;

    //private Queue<String> ts_queue;

    /**
     *
     * Update tomorrow
     * Set Database Instance to global
     * Check SMS Sender MEthod not sending sms
     *
     *
     * @param savedInstanceState
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        log = findViewById(R.id.textView_smsLog);
        queue_log = findViewById(R.id.textView_queued);
        received_ts_log = findViewById(R.id.textView_received_ts);
        sms_ts_log = findViewById(R.id.textView_sms_ts);
        sms_num_log = findViewById(R.id.textView_sms_num);
        sms_stat_log = findViewById(R.id.textView_sms_status);
        //ts_queue = new LinkedList<>();

        // update
        DatabaseHandler databaseHandler = new DatabaseHandler(getApplication());
        int log_q_s = databaseHandler.getLogSize();
        databaseHandler.close();
        // update logs
        queue_log.setText(log_q_s+"");
        received_ts_log.setText("--");
        sms_ts_log.setText("--");
        sms_num_log.setText("--");
        sms_stat_log.setText("--");

        buttonListener();
        setup_sever();
        smsSendingScheduler();
        requestReadAndSendSmsPermission();
    }

    public void smsSendingScheduler() {
        final Context context = getApplication();
        //Toast.makeText(context, "Scheduler started!", Toast.LENGTH_SHORT).show();
        log.setText("Initializing...");
        log.setTextColor(Color.GREEN);

        sendAt = new Runnable() {
            public void run() {
                try {
                    DatabaseHandler databaseHandler = new DatabaseHandler(context);
                    int log_size = databaseHandler.getLogSize();
                    log.setText("--Sending SMS--");
                    log.setTextColor(Color.BLUE);
                    //log.setText(hm.get("time_stamp"));
                    if (log_size == 0) {
                        log.setText("--no sms to sent--");
                        log.setTextColor(Color.MAGENTA);
                        scheduler.shutdown();
                        databaseHandler.close();
                    } else {

                        HashMap<String, String> hm = databaseHandler.getAnSms();

                        if (hm != null) {

                            String number = hm.get("number");
                            String smsBody = hm.get("sms");
                            sms_ts_log.setText(hm.get("time_stamp"));
                            sms_num_log.setText(hm.get("number"));
                            sms_stat_log.setText("Sending...");
                            sms_stat_log.setTextColor(Color.MAGENTA);

                            // update sms state
                            databaseHandler.updateSmsState(hm.get("time_stamp"), 1);
                            sendSms(number, smsBody, hm.get("time_stamp"));
                        } else {
                            sms_stat_log.setText("Sending SMS!");
                            sms_stat_log.setTextColor(Color.MAGENTA);
                        }
                        databaseHandler.close();
                    }

                } catch (Exception e) {
                    Log.d("error-initialize", e.getMessage());
                    scheduler.shutdown();
                }
            }
        };
        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(sendAt, 2, smsPeriodTosent, SECONDS);
    }

    public void sendSms(String number, String smsBody, String ts) {

        //final TextView log = findViewById(R.id.textView_smsLog);
        String SENT = "SMS_SENT";
        Intent in = new Intent(SENT);

        in.putExtra("time_stamp", ts);
        getApplication().sendBroadcast(in);
        PendingIntent sentPI = PendingIntent.getBroadcast(getApplication(), 0, in, 0);
        PendingIntent deliveredPI = PendingIntent.getBroadcast(getApplication(), 0, in, 0);
        broadcast_reciever(SENT);

        SmsManager smsMgr = SmsManager.getDefault();
        smsMgr.sendTextMessage(number, null, smsBody, sentPI, deliveredPI);
    }

    private void broadcast_reciever(String SENT) {
        getApplication().registerReceiver(new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context arg0, Intent arg1)
            {
                int resultCode = getResultCode();
                DatabaseHandler databaseHandler;

                Bundle extras = arg1.getExtras();
                String ts = extras.getString("time_stamp");

                switch (resultCode)
                {
                    case Activity.RESULT_OK:

                        try {
                            if (!ts.equals(null)||!ts.equals("")) {
                                databaseHandler = new DatabaseHandler(getApplication());
                                databaseHandler.deleteSms(ts);
                                queue_log.setText(databaseHandler.getLogSize() + "");
                                databaseHandler.close();
                                Log.d("time-stamp", "sms sent");
                                sms_stat_log.setText("SMS Sent!");
                                sms_stat_log.setTextColor(Color.GREEN);
                            }
                        } catch (Exception e) { Log.d("ts", e.getMessage()); }
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                    case SmsManager.RESULT_ERROR_RADIO_OFF:

                        try {
                            if (!ts.equals(null)||!ts.equals("")) {
                                sms_stat_log.setText("Not Sent!");
                                sms_stat_log.setTextColor(Color.RED);
                                String o_ts = ts;
                                Log.d("time-stamp", "not sent -> " + o_ts);

                                databaseHandler = new DatabaseHandler(getApplication());
                                databaseHandler.updateSmsState(o_ts, 0);
                                //databaseHandler.updateSmsTS(getTimeStamp(), o_ts);
                                databaseHandler.close();
                            }
                        } catch (Exception e) { Log.d("ts", e.getMessage()); }
                        break;
                }
            }
        }, new IntentFilter(SENT));
    }


    /**
     * http server
     */
    public static final String CONTEXT = "/witechsms";
    public static final int PORT = 8000;

    private void setup_sever() {
        // Create a new SimpleHttpServer
        try {
            SimpleHttpServer simpleHttpServer = new SimpleHttpServer(PORT, CONTEXT,
                    new HttpRequestHandler());

            // Start the server
            simpleHttpServer.start();
            //Log.d("server", simpleHttpServer.httpServer.getAddress() + "");
            Log.d("server", "Server is started and listening on port " + PORT);
            Toast.makeText(getApplicationContext(), "Server started!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            //Toast.makeText(getApplicationContext(), "Cannot start server!", Toast.LENGTH_SHORT).show();
            Log.d("server", e.getMessage());
        }
    }

    @SuppressWarnings("restriction")
    public class SimpleHttpServer {
        private HttpServer httpServer;
        public SimpleHttpServer(int port, String context, HttpHandler handler) {
            try {
                //Create HttpServer which is listening on the given port
                //InetSocketAddress is = new InetSocketAddress(port);
                httpServer = HttpServer.create( new InetSocketAddress(port), 0);
                Log.d("server", "error create server ! ");
                //httpServer = HttpServer.create();
                //Create a new context for the given context and handler
                httpServer.createContext(context, handler);
                //Create a default executor
                httpServer.setExecutor(null);
            } catch (IOException e) {
                e.printStackTrace();
                Log.d("server", "error create server -> " + e.getMessage());
            }
        }

        public void start() {
            this.httpServer.start();
        }
    }


    @SuppressWarnings("restriction")
    public class HttpRequestHandler implements HttpHandler {

        private static final String UID = "number";
        private static final String STATE = "sms";

        private static final int PARAM_NAME_IDX = 0;
        private static final int PARAM_VALUE_IDX = 1;

        private static final int HTTP_OK_STATUS = 200;
        private static final int HTTP_NOTFOUND_STATUS = 404;

        private static final String AND_DELIMITER = "&";
        private static final String EQUAL_DELIMITER = "=";

        public void handle(HttpExchange t) throws IOException {

            //Create a response form the request query parameters
            URI uri = t.getRequestURI();

            // check if valid ang parameter sa get request
            final String params[] = createResponseFromQueryParams(uri);
            Log.d("server", "number  : " + params[0]);
            Log.d("server", "sms     : " + params[1]);

            String response = "ok";
            if (params[0].equals("") || params[1].equals("")) {
                Log.d("server", "Invalid parameters!");
                response = "Invalid request!";
            }

            if (isValid(params[0])) {
                Log.d("server", "Invalid parameters!");
                response = "Invalid mobile number!";
            }

            // save sms to db for sending
            String ts = getTimeStamp();
            DatabaseHandler databaseHandler = new DatabaseHandler(getApplication());
            databaseHandler.insertSms(ts, params[0], params[1]);
            queue_log.setText(databaseHandler.getLogSize()+"");
            databaseHandler.close();

            // update log
            received_ts_log.setText(ts);

            // start sms scheduler
            checkSmsScheduler();

            //sendSms(params[0], params[1]);
            //Set the response header status and length

            t.sendResponseHeaders( response == "ok"
                    ? HTTP_OK_STATUS
                    : HTTP_NOTFOUND_STATUS,
                    response.getBytes().length);
            //Write the response string
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }

        public String getTimeStamp() {
            HashMap<String, String> hm = localTime.getTime();
            String time_stamp = hm.get("year") + "-" + hm.get("month") + "-" + hm.get("day_month") + " " +
                    hm.get("hour") + ":" + hm.get("min") + ":" + hm.get("seconds") + "." + hm.get("ms");
            Log.d("time-stamp-debug", time_stamp);
            return time_stamp;
        }

        public boolean isValid(String s) {
            Pattern p = Pattern.compile("(0/91)?[7-9][0-9]{9}");
            Matcher m = p.matcher(s);
            return (m.find() && m.group().equals(s));
        }

        /**
         * Creates the response from query params.
         *
         * @param uri the uri
         * @return the string
         */
        private String[] createResponseFromQueryParams(URI uri) {
            String uid = "";
            String state = "";
            //Get the request query
            String query = uri.getQuery();
            if (query != null) {
                //System.out.println("Query: " + query);
                String[] queryParams = query.split(AND_DELIMITER);
                if (queryParams.length > 0) {
                    for (String qParam : queryParams) {
                        String[] param = qParam.split(EQUAL_DELIMITER);
                        if (param.length > 0) {
                            for (int i = 0; i < param.length; i++) {
                                if (UID.equalsIgnoreCase(param[PARAM_NAME_IDX])) {
                                    uid = param[PARAM_VALUE_IDX];
                                }
                                if (STATE.equalsIgnoreCase(param[PARAM_NAME_IDX])) {
                                    state = param[PARAM_VALUE_IDX];
                                }
                            }
                        }
                    }
                }
            }
            return new String[] { uid, state};
        }
    }


    /**
     *  Helper Methods
     */

    public String getTimeStamp() {
        HashMap<String, String> hm = localTime.getTime();
        String time_stamp = hm.get("year") + "-" + hm.get("month") + "-" + hm.get("day_month") + " " +
                hm.get("hour") + ":" + hm.get("min") + ":" + hm.get("seconds") + "." + hm.get("ms");
        Log.d("time-stamp-debug", time_stamp);
        return time_stamp;
    }

    public void checkSmsScheduler() {
        if (scheduler.isShutdown()) {
            log.setText("--scheduler started--");
            log.setTextColor(Color.BLUE);
            //Log.d("smsdebug", "scheduler started!");
            scheduler = Executors.newScheduledThreadPool(1);
            scheduler.scheduleAtFixedRate(sendAt, 1, smsPeriodTosent, SECONDS);
        }
    }

    private void buttonListener() {
        findViewById(R.id.button_set_view_permission).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                set_view_permission(v);
                return true;
            }
        });
        findViewById(R.id.button_sendSms).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                send_smsButton(v);
                return true;
            }
        });
    }

    private void set_view_permission(View view) {
        requestReadAndSendSmsPermission();
        Toast.makeText(getApplicationContext(), "sms permission: " +hasReadSmsPermission(), Toast.LENGTH_SHORT).show();
    }

    private void send_smsButton(View view) {
        /*TextView log = findViewById(R.id.textView_smsLog);
        EditText mNumber = findViewById(R.id.editText_mNumber);
        EditText smsBody = findViewById(R.id.editText_smsBody);
        Log.d("sms", "sms:" + mNumber.getText().toString());
        Log.d("sms", "sms:" + smsBody.getText().toString());
        if (mNumber.getText().toString().equals("") || smsBody.getText().toString().equals("")) {
            log.setText("Error sms");
            log.setTextColor(Color.RED);
            //setup_sever();
        } else {
            sendSms(mNumber.getText().toString(), smsBody.getText().toString());
        }*/
    }


    /**
     *  Get SMS Permissions
     */
    private boolean hasReadSmsPermission() {
        return ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestReadAndSendSmsPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.READ_SMS)) {
            Log.d("sms", "shouldShowRequestPermissionRationale(), no permission requested");
            return;
        }
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.SEND_SMS, Manifest.permission.RECEIVE_SMS},
                SMS_PERMISSION_CODE);
    }



}
