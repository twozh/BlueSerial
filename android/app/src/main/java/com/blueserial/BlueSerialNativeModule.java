package com.blueserial;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    private String TAG = "BluetoothService: ";

    public BlueSerialNativeModule(ReactApplicationContext reactContext){
        super(reactContext);
        mReactContext = reactContext;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Add the listener for `onActivityResult`
        reactContext.addActivityEventListener(this);

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
                Log.d(TAG, "device find. name "+device.getName());

                // send device.getName() and device.getAddress() to javascript
                WritableMap params = Arguments.createMap();
                params.putString("devName",device.getName());
                params.putString("devAddr",device.getAddress());
                sendEvent(mReactContext, "deviceFind", params);
            }
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Log.d(TAG, "discovery finished");
                //TBD
                WritableMap params = Arguments.createMap();
                params.putString("devName","discovery finished");
                sendEvent(mReactContext, "deviceFind", params);
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
    public boolean isBluetoothSupported() {
        if (null != mBluetoothAdapter){
            return true;
        } else {
            return false;
        }
    }

    @ReactMethod
    public void isBluetoothEnable(Promise promise){
        promise.resolve(mBluetoothAdapter.isEnabled());
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
            promise.resolve("Bluetooth is enabled. Now start discovery");
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
    }

    @Override
    public void onHostResume(){
        Log.d(TAG, "Native module's onHostResume occur.");
    }

    @Override
    public void onHostPause(){
        Log.d(TAG, "Native module's onHostPause occur.");
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