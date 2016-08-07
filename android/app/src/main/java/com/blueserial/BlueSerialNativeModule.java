package com.blueserial;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.facebook.react.ReactPackage;
import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.JavaScriptModule;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.uimanager.ViewManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

/**
 * Created by Administrator on 2016/7/31.
 */
public class BlueSerialNativeModule  extends ReactContextBaseJavaModule  implements ActivityEventListener, LifecycleEventListener {
    private BluetoothAdapter mBluetoothAdapter;
    private ReactApplicationContext mReactContext;
    private final static int REQUEST_ENABLE_BT = 1;
    private final static String ERR_CODE = "1";
    private Promise mStartDiscoveryPromise;
    private Promise mConnectPromise;
    private String TAG = "BlueSerialNativeModule: ";
    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");    //UUID for SPP service
    private StreamThread mStreamThread;
    private ConnectThread mConnectThread;

    public BlueSerialNativeModule(ReactApplicationContext reactContext){
        super(reactContext);
        mReactContext = reactContext;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Add the listener for `onActivityResult`
        reactContext.addActivityEventListener(this);
        // Add the listener for `LifecycleEvent`
        reactContext.addLifecycleEventListener(this);

        // Register the BroadcastReceiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        reactContext.registerReceiver(mReceiver, filter);
    }

    //send event to javascript
    private void sendEvent(ReactContext reactContext,
                           String eventName,
                           @Nullable WritableMap params) {
        reactContext
        .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
        .emit(eventName, params);
    }

    // Create a BroadcastReceiver for bluetooth action
    public final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.d(TAG, "device find, name: "+device.getName());

                // send device.getName() and device.getAddress() to javascript
                WritableMap params = Arguments.createMap();
                params.putString("devName",device.getName());
                params.putString("devAddr",device.getAddress());
                sendEvent(mReactContext, "find", params);
            }
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Log.d(TAG, "discovery finished");

                WritableMap params = Arguments.createMap();
                params.putString("msg","Discovery finished");
                sendEvent(mReactContext, "findFinished", params);
            } else if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG, "turn off");
                        break;
                }
            }
        }
    };

    @Override
    public String getName(){
        return "BlueSerialNativeModule";
    }

    @ReactMethod
    public void testMethod(boolean ok, Promise promise){
        if (ok){
            promise.resolve("Hello BlueSerial!");
        } else{
            promise.reject(ERR_CODE, "call testMethod: return error");
        }
    }

    @ReactMethod
    public void isBluetoothSupported(Promise promise) {
        if (null != mBluetoothAdapter){
            promise.resolve(true);
        } else {
            promise.resolve(false);
        }
    }

    @ReactMethod
    public void isBluetoothEnabled(Promise promise){
        promise.resolve(mBluetoothAdapter.isEnabled());
    }

    @ReactMethod
    public void startDiscovery(Promise promise){
        Activity currentActivity = getCurrentActivity();
        if (currentActivity == null) {
            promise.reject(ERR_CODE, "Activity doesn't exist");
            return;
        }

        if (!mBluetoothAdapter.isEnabled()){
            mStartDiscoveryPromise = promise;
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            currentActivity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else{
            mBluetoothAdapter.startDiscovery();
            Log.d(TAG, "start discovery");
            promise.resolve("Bluetooth has been enabled. Now start discovery");
        }
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                //showToast("Turn on bluetooth ok!");
                mStartDiscoveryPromise.resolve("Bluetooth enabled successfully");
            } else {
                //showToast("Turn on bluetooth fail!");
                mStartDiscoveryPromise.reject(ERR_CODE, "Bluetooth enable fail");
            }
        }
        mStartDiscoveryPromise = null;
    }

    @Override
    public void onNewIntent(Intent intent) {

    }

    @Override
    public void onHostDestroy(){
        Log.d(TAG, "Native module's onHostDestroy occur.");
        mReactContext.unregisterReceiver(mReceiver);
        stopBtService();
    }

    @Override
    public void onHostResume(){
        Log.d(TAG, "Native module's onHostResume occur.");
    }

    @Override
    public void onHostPause(){
        Log.d(TAG, "Native module's onHostPause occur.");
    }

    @ReactMethod
    public void connect(String macAddr, Promise promise){
        BluetoothDevice device;

        mConnectPromise = promise;

        mBluetoothAdapter.cancelDiscovery();
        device = mBluetoothAdapter.getRemoteDevice(macAddr);

        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
    }

    @ReactMethod
    public void send(String msg){
        if (mStreamThread != null){
            mStreamThread.write(msg.getBytes());
        }
    }

    private synchronized void startStream(BluetoothSocket socket) {
        // Start the thread to manage the connection and perform transmissions
        mStreamThread = new StreamThread(socket);
        mStreamThread.start();
    }

    private synchronized void stopBtService() {
        Log.d(TAG, "now stop bt service");
        if (mStreamThread != null){
            mStreamThread.cancel();
            mStreamThread = null;
        }
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
    }

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
                tmp = device.createRfcommSocketToServiceRecord(SPP_UUID);
            } catch (IOException e) {
                mConnectPromise.reject(ERR_CODE, e.getMessage());
            }
            mmSocket = tmp;
        }
        public void run() {
            // Cancel discovery because it will slow down the connection
            mBluetoothAdapter.cancelDiscovery();
            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                //showToast("try to connect");
                Log.d(TAG, "run: mmSocket.connect");
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and get out
                try {
                    mmSocket.close();
                } catch (IOException closeException) { }

                mConnectPromise.reject(ERR_CODE, connectException.getMessage());
                return;
            }
            // Do work to manage the connection (in a separate thread)
            Log.d(TAG, "connected to: " + mmDevice.getName() + ", now try to create stream");
            startStream(mmSocket);
        }
        // Will cancel an in-progress connection, and close the socket
        public void cancel() {
            Log.d(TAG, "ConnectThread, try cancel");
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

    private class StreamThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public StreamThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Temp sockets not created", e);
                mConnectPromise.reject(ERR_CODE, e.getMessage());
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            mConnectPromise.resolve("connect successfuly, stream thread has began");
            Log.d(TAG, "connect successfuly, stream thread has began");
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()
            int i;

            StringBuffer  readMessage = new StringBuffer();

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {

                    bytes = mmInStream.read(buffer);
                    String read = new String(buffer, 0, bytes);

                    readMessage.append(read);
                    if (read.contains("\n")) {
                        // send recvMsg event to javascript
                        WritableMap params = Arguments.createMap();
                        params.putString("msg",readMessage.toString());
                        sendEvent(mReactContext, "recv", params);

                        Log.d(TAG, readMessage.toString());
                        readMessage.setLength(0);
                    }

                } catch (IOException e) {
                    Log.e(TAG, "Connection Lost", e);
                    break;
                }
            }
        }

        // Call this from the main activity to send data to the remote device
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);

            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        // Call this from the main activity to shutdown the connection
        // Will cancel an in-progress connection, and close the socket
        public void cancel() {
            Log.d(TAG, "StreamThread, try cancel");
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);}
        }
    }

}

class BlueSerialReactPackage implements ReactPackage {

    @Override
    public List<Class<? extends JavaScriptModule>> createJSModules() {
        return Collections.emptyList();
    }

    @Override
    public List<ViewManager> createViewManagers(ReactApplicationContext reactContext) {
        return Collections.emptyList();
    }

    @Override
    public List<NativeModule> createNativeModules(
            ReactApplicationContext reactContext) {
        List<NativeModule> modules = new ArrayList<>();

        modules.add(new BlueSerialNativeModule(reactContext));

        return modules;
    }


}