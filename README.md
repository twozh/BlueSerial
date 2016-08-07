##  BlueSerialNativeModule -- a react-native android module

### module name: BlueSerialNativeModule

### module method
* isBluetoothSupported(Promise promise)
  - promise.resolve: true/false

* isBluetoothEnabled(Promise promise)
  - promise.resolve: true/false

* startDiscovery(Promise promise
  - promise.resolve("Bluetooth has been enabled. Now start discovery");
  - promise.resolve("Bluetooth enabled successfully");
  - promise.reject(ERR_CODE, "Bluetooth enable fail");
  - ERR_CODE = "1";

* connect(String macAddr, Promise promise)
  - @macAddr: bluetooth device's MAC address
  - promise.resolve("connect successfuly, stream thread has began");
  - promise.reject(ERR_CODE, error message);

* send(String msg)

### module event
* event name: find
  - param: {
        devName: String,
        devAddr: String,
    }
* event name: findFinished
  - param: {
	msg: "Discovery finished",
	}
* event name: recv
  - param: {
	msg: message recieved, //string contains '\n'
	}
