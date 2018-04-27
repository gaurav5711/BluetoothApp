package com.example.Bluetooths;


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;



//import com.example.bluetoothchatapp.BluetoothChatService;

//import android.support.v7.app.ActionBarActivity;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Toast;


public class MyActivity extends Activity {

    private static final String TAG = "DeviceListActivity";
    private static final boolean D = true;
    public static String EXTRA_DEVICE_ADDRESS = "device_address";


    private boolean counter=false;
    private char alpha;
    private boolean negative=false;
    private long[][] data;
    private long maxX=0,maxY=0,maxZ=0,minX=0,minY=0,minZ=0;
    private int index3=0,index1=0,index2=0,index4=0,index5=0,index6=0;  //accelerometer--index1--row1
    private int indexmaxX=0,indexmaxY=0,indexmaxZ=0,indexminX=0,indexminY=0,indexminZ=0;
    private boolean startCounting=true;
    private long presentTime;


    private static final UUID MY_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    private static final int SUCCESS_CONNECT = 1;
    private static final int FAILURE_TOAST = 2;
    private static final int MESSAGE_READ = 3;


    private ConnectThread connect_thread;
    private ConnectedThread connected_thread;


    private BluetoothAdapter mBtAdapter;
    private ArrayAdapter<String> mPairedDevicesArrayAdapter;
    private ArrayAdapter<String> mNewDevicesArrayAdapter;


    TextView txtmsg;
    TextView txtgesture;

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SUCCESS_CONNECT:
                    Log.d(TAG, "Successfully Connected");
                    break;
                case FAILURE_TOAST:
                    Toast.makeText(getApplicationContext(), "Connection Failed", Toast.LENGTH_LONG).show();
                    break;
                case MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    //used to display gesture after 3 seconds
//                    if(startCounting){
//                        presentTime=System.currentTimeMillis();
//                        startCounting=false;
//                    }
//                    if((System.currentTimeMillis()-presentTime)>3000){
//                        detectGesture();
//                        startCounting=true;
//                    }
                    //processData----gets only digits with one letter atindex 0 of string
                    String requiredValue = processData(readMessage);
                    if(requiredValue!=null){
                    //    storeData(requiredValue);               //store data in 6X150 2D-matrix in matrix "data[][]"
                        txtmsg.setText(requiredValue);
                    }
                    else {
                        txtmsg.setText("Not valid value");
                    }
                    break;
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        if(!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)){
            Toast.makeText(getApplicationContext(), "failed", Toast.LENGTH_LONG).show();
        }
        txtgesture = (TextView)findViewById(R.id.txtgesture);
        txtmsg = (TextView)findViewById(R.id.txtreading);
        data=new long[6][150];                           // store data received from arduino

        Button scanButton = (Button) findViewById(R.id.button_scan);
        scanButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                doDiscovery();
//                v.setVisibility(View.GONE);
            }
        });


        mPairedDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);
        mNewDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);

        ListView pairedListView = (ListView) findViewById(R.id.lst_paired);
        pairedListView.setAdapter(mPairedDevicesArrayAdapter);
        pairedListView.setOnItemClickListener(mDeviceClickListener);
        // Find and set up the ListView for newly discovered devices
        ListView newDevicesListView = (ListView) findViewById(R.id.lst_active);
        newDevicesListView.setAdapter(mNewDevicesArrayAdapter);
        newDevicesListView.setOnItemClickListener(mDeviceClickListener);


        // Register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);
        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);


        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        // Get a set of currently paired devices
        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                mPairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        }


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Make sure we're not doing discovery anymore
        if (mBtAdapter != null) {
            mBtAdapter.cancelDiscovery();
        }
        // Unregister broadcast listeners
        this.unregisterReceiver(mReceiver);
    }

    protected void doDiscovery() {

        if (D) Log.d(TAG, "doDiscovery()");
        if (mBtAdapter.isDiscovering()) {
            mBtAdapter.cancelDiscovery();
        }
        mBtAdapter.startDiscovery();
    }

    public String processData(String msg){
        String process_string="";
        int[] characterIndex=new int[10];
        int cnt=0;
        for(int i=0;i<msg.length();i++){
            char character = msg.charAt(i);
            if(character=='a'||character=='b'||character=='c'||character=='d'||character=='e'||character=='f') {
                characterIndex[cnt]=i;
                cnt++;
            }
        }
        for(int i=0;i<cnt;i++){
            if(msg.charAt(characterIndex[i])==msg.charAt(characterIndex[i+1])){
                process_string = msg.charAt(i)+msg.substring(characterIndex[i],characterIndex[i+1]);
            }
        }
        if(process_string==""){
            return null;
        }else{
            return process_string;
        }
//        for(int i=0;i<msg.length();i++){
//            char character = msg.charAt(i);
//            if(character=='a'||character=='b'||character=='c'||character=='d'||character=='e'||character=='f') {
//                counter=!counter;                     //value is coming
//                if(character==alpha){
//                    if(negative){
//                        return character+'-'+process_string;
//                    }
//                    return character+process_string;
//                }
//                alpha=character;
//                negative=false;
//            }
//            else if(character=='-'){
//                negative = true;
//            }
//            if(counter){
//                if(!(character=='-')) {
//                    process_string = process_string + character;
//                }
//            }
//        }
//            return null;
    }

    private void storeData(String message){
        if(message.contains("-")){
            if(message.contains("a")){
                data[0][index1]=0-Long.parseLong(message.substring(2));             //AccX
                index1++;
            }else if (message.contains("b")){
                data[1][index2]=0-Long.parseLong(message.substring(2));
                index2++;
            }else if (message.contains("c")){
                data[2][index3]=0-Long.parseLong(message.substring(2));
                index3++;
            }else if (message.contains("d")){
                data[3][index4]=0-Long.parseLong(message.substring(2));
                index4++;
            }else if (message.contains("e")){
                data[4][index5]=0-Long.parseLong(message.substring(2));
                index5++;
            }else if (message.contains("f")){
                data[5][index6]=0-Long.parseLong(message.substring(2));
                index6++;
            }
        }else {
            if(message.contains("a")){
                data[0][index1]=Long.parseLong(message.substring(1));
                index1++;
            }else if (message.contains("b")){
                data[1][index2]=Long.parseLong(message.substring(1));
                index2++;
            }else if (message.contains("c")){
                data[2][index3]=Long.parseLong(message.substring(1));
                index3++;
            }else if (message.contains("d")){
                data[3][index4]=Long.parseLong(message.substring(1));
                index4++;
            }else if (message.contains("e")){
                data[4][index5]=Long.parseLong(message.substring(1));
                index5++;
            }else if (message.contains("f")){
                data[5][index6]=Long.parseLong(message.substring(1));
                index6++;
            }
        }
        if(maxX < data[0][index1])
        {
            maxX = data[0][index1];
            indexmaxX = index1;
        }

        if(maxY < data[1][index2])
        {
            maxY = data[1][index2];
            indexmaxY = index2;
        }


        if(maxZ < data[2][index3])
        {
            maxZ = data[2][index3];
            indexmaxZ = index3;
        }


        if(minX > data[0][index1])
        {
            minX = data[0][index1];
            indexminX = index1;
        }

        if(minY > data[1][index2])
        {
            minY = data[1][index2];
            indexminY=index2;
        }

        if(minZ > data[2][index3])
        {
            minZ = data[2][index3];
            indexminZ = index3;
        }
    }

    private void detectGesture(){
        if(Math.abs(minY - maxY)>Math.abs(minX - maxX) && Math.abs(minY - maxY)>Math.abs(minZ - maxZ))
        {
            //if(dataY[10]>dataY[arrayIndex-5])
            txtgesture.setText("Roll");
        }
        else if(Math.abs(minX - maxX) < Math.abs(minZ - maxZ)){
            if(Math.abs(minZ)>maxZ)
            {
            //    Serial.println("LEFT");
            txtgesture.setText("LEFT");
            }
            else
            {
            //    Serial.println("RIGHT");
            txtgesture.setText("RIGHT");
            }
        }else{
            if(data[0][10]>data[0][index1-5])
            {
            //    Serial.println("UP");
            txtgesture.setText("UP");
            }
            else
            {
            //    Serial.println("DOWN");
            txtgesture.setText("DOWN");
            }
        }
        index1=0;index2=0;index3=0;index4=0;index5=0;index6=0;
        data=null;
        maxX=0;maxY=0;maxZ=0;minX=0;minY=0;minZ=0;
        indexmaxX=0;indexmaxY=0;indexmaxZ=0;indexminX=0;indexminY=0;indexminZ=0;
    }

    private AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            // Cancel discovery because it's costly and we're about to connect
            mBtAdapter.cancelDiscovery();
            // Get the device MAC address, which is the last 17 chars in the View
            String info = ((TextView) v).getText().toString();

            String address = info.substring(info.length() - 17);

            Toast.makeText(getApplicationContext(),address ,Toast.LENGTH_LONG).show();

            BluetoothDevice device = mBtAdapter.getRemoteDevice(address);

            connect_thread = new ConnectThread(device);
            connect_thread.start();
            // Create the result Intent and include the MAC address
//            Intent intent = new Intent();
//            intent.putExtra(EXTRA_DEVICE_ADDRESS, address);
            // Set result and finish this Activity
//            setResult(Activity.RESULT_OK, intent);
//            finish();
        }
    };


    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // If it's already paired, skip it, because it's been listed already
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    mNewDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                }
                // When discovery is finished, change the Activity title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                //            setProgressBarIndeterminateVisibility(false);
                //            setTitle(R.string.select_device);
                if (mNewDevicesArrayAdapter.getCount() == 0) {
                    String noDevices = "no Devices Found";
                    mNewDevicesArrayAdapter.add(noDevices);
                }
            }
        }
    };


    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            BluetoothSocket tmp = null;
            mmDevice = device;

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // MY_UUID is the app's UUID string, also used by the server code

                Method m = device.getClass().getMethod("createRfcommSocket", new Class[]{ int.class });
                tmp = (BluetoothSocket) m.invoke(device, 1);
                //               tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (Exception e) {

                Log.d(TAG,"Rfcomm failed");
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it will slow down the connection
            mBtAdapter.cancelDiscovery();

            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and get out
                try {
                    mmSocket.close();
                    Log.d(TAG,"Connection Failed");
                    mHandler.obtainMessage(FAILURE_TOAST).sendToTarget();
                } catch (IOException closeException) { }
                return;
            }

            synchronized (MyActivity.this) {
                connect_thread = null;
            }


            connected(mmSocket,mmDevice);

            // Do work to manage the connection (in a separate thread)
//            manageConnectedSocket(mmSocket);
//            mHandler.obtainMessage(SUCCESS_CONNECT).sendToTarget();
        }

        /** Will cancel an in-progress connection, and close the socket */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }


    public void connected(BluetoothSocket mmSocket, BluetoothDevice mmDevice) {

        if (D) Log.d(TAG, "connected");
        // Cancel the thread that completed the connection
        if (connect_thread != null) {connect_thread.cancel(); connect_thread = null;}
        // Cancel any thread currently running a connection
        if (connected_thread != null) {connected_thread.cancel(); connected_thread = null;}

        connected_thread = new ConnectedThread(mmSocket);
        connected_thread.start();

    }

    //Connected Thread

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    // Send the obtained bytes to the UI activity
                    mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                            .sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) { }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }
}

