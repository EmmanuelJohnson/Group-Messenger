package edu.buffalo.cse.cse486586.groupmessenger1;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Button;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 * 
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {


    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    static final int SERVER_PORT = 10000;
    static final int [] allPorts = new int[]{11108, 11112, 11116, 11120, 11124};

    //From PA2A OnPTestClickListener.java File
    private static final String KEY_FIELD = "key";
    private static final String VALUE_FIELD = "value";
    private Uri mUri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger1.provider");;
    private ContentValues keyValueToInsert;

    //From PA2A OnPTestClickListener.java File
    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);
        //From PA1
        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        Log.d(TAG, "onCreate: "+myPort);
        try {
            /*
             * Create a server socket as well as a thread (AsyncTask) that listens on the server
             * port.
             *
             * AsyncTask is a simplified thread construct that Android provides. Please make sure
             * you know how it works by reading
             * http://developer.android.com/reference/android/os/AsyncTask.html
             */
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            Log.d(TAG, "onCreate: initialized server socket");
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);//Creating the async task
            Log.d(TAG, "onCreate: initialized a new server task");
            //This will run in parallel - thread pool executer
        } catch (IOException e) {
            /*
             * Log is a good way to debug your code. LogCat prints out all the messages that
             * Log class writes.
             *
             * Please read http://developer.android.com/tools/debugging/debugging-projects.html
             * and http://developer.android.com/tools/debugging/debugging-log.html
             * for more information on debugging.
             */
            Log.e(TAG, "onCreate: ", e);
            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }
        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        final TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        
        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));
        
        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */
        final EditText editText = (EditText) findViewById(R.id.editText1);
        final Button sendButton = (Button) findViewById(R.id.button4);
        Log.d(TAG, "onCreate: before click listener");
        sendButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Code here executes on main thread after user presses button
                String grpMsg = editText.getText().toString() + "\n";
                editText.setText("");
                tv.append("**"+grpMsg);
                Log.d(TAG, "myport: "+myPort);
                setContentUsingProvider(grpMsg);
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, grpMsg, myPort);//Serializing async
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

    //From PA1
    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            Log.d(TAG, "here in server task do in background");
            try {
                while(true) {
                    Socket socket = serverSocket.accept();
                    InputStream msgIpStrm = socket.getInputStream();
                    BufferedReader recieveInp = new BufferedReader(new InputStreamReader(msgIpStrm));
                    publishProgress(recieveInp.readLine());
                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        protected void onProgressUpdate(String... strings) {
            /*
             * The following code displays what is received in doInBackground().
             */
            String strReceived = strings[0].trim();
            TextView tv = (TextView) findViewById(R.id.textView1);
            tv.append(strReceived + "\n");
            /*
             * The following code creates a file in the AVD's internal storage and stores a file.
             *
             * For more information on file I/O on Android, please take a look at
             * http://developer.android.com/training/basics/data-storage/files.html
             */

            /*String filename = "GroupMessengerOutput";
            String string = strReceived + "\n";
            FileOutputStream outputStream;

            try {
                outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
                outputStream.write(string.getBytes());
                outputStream.close();
            } catch (Exception e) {
                Log.e(TAG, "File write failed");
            }*/
            setContentUsingProvider(strReceived);

            return;
        }
    }

    /***
     * ClientTask is an AsyncTask that should send a string over the network.
     * It is created by ClientTask.executeOnExecutor() call whenever OnKeyListener.onKey() detects
     * an enter key press event.
     *
     * @author stevko
     *
     */
    //From PA1
    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {
            int currentPort = Integer.parseInt(msgs[1]);
            int nDevices = 0;
            //Run a loop for five devices and create a socket to connect to other devices except the current device
            while(nDevices < 5){
                //All remotes ports except itself
                if(allPorts[nDevices] != currentPort) {
                    try {
                        int remotePort =  allPorts[nDevices];
                        //From PA1
                        Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                remotePort);
                        String msgToSend = msgs[0];
                        /*
                         * TODO: Fill in your client code that sends out a message.
                         */
                        //https://stackoverflow.com/questions/5680259/using-sockets-to-send-and-receive-data
                        //Log.d("debug", "Message : "+msgToSend+", isEmpty :"+msgToSend.trim().isEmpty());
                        OutputStream msgOpStrm = socket.getOutputStream();
                        DataOutputStream sendOp = new DataOutputStream(msgOpStrm);
                        sendOp.writeBytes(msgToSend);
                        sendOp.flush();
                        socket.close();
                    } catch (UnknownHostException e) {
                        Log.e(TAG, "ClientTask UnknownHostException");
                    } catch (IOException e) {
                        Log.e(TAG, "ClientTask socket IOException"+e);
                    }
                }
                nDevices+=1;
            }
            return null;
        }
    }

    int key = 0;
    public void setContentUsingProvider(String msg){
        try {
            keyValueToInsert = new ContentValues();
            //From Documentation
            keyValueToInsert.put(KEY_FIELD, Integer.toString(key));
            keyValueToInsert.put(VALUE_FIELD, msg);
            Uri newUri = getContentResolver().insert(
                    mUri,    // assume we already created a Uri object with our provider URI
                    keyValueToInsert
            );
            key++;
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }
}
