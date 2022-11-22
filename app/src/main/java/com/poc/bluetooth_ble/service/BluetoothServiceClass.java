package com.poc.bluetooth_ble.service;

import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.poc.bluetooth_ble.util.AppConstants;
import com.poc.bluetooth_ble.util.Base64Hex;
import com.poc.bluetooth_ble.util.Encrypt;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;


public class BluetoothServiceClass extends Service {

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;
    private int mConnectionState = STATE_DISCONNECTED;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattCharacteristic mWriteCharacteristic;
    private BluetoothGattCharacteristic mReadCharacteristic;
    private BluetoothGattCharacteristic mNotifyCharacteristic;
    private boolean isDisconnected = false;
    String deviceName;
    //UUID in device
    public final static String ACTION_GATT_CONNECTED =
            "android-er.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "android-er.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "android-er.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "android-er.ACTION_DATA_AVAILABLE";
    public final static String ACTION_WRITE_DATA_AVAILABLE =
            "android-er.ACTION_DATA_WRITE_AVAILABLE";


    private String updateStringData = "";
    private int bleWiFICounter = 0;

    private String mDeviceAddres;

    //for bind service
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        deviceName= intent.getStringExtra("DEVICE");
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        disconnect();
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        public BluetoothServiceClass getService() {
            return BluetoothServiceClass.this;
        }
    }

    //for startService
    /*@Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        deviceName=(String) intent.getExtras().get("DEVICE");
        deviceName= intent.getStringExtra("DEVICE");
        return START_STICKY;
    }
*/
    public boolean initialize() {
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                return false;
            }
        }
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            return false;
        }
        return true;
    }

    private final BluetoothGattCallback mBluetoothGattCallback = new BluetoothGattCallback() {

        @SuppressLint("MissingPermission")
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                isDisconnected = false;
                broadcastUpdate(intentAction);
                gatt.requestMtu(500);
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                if (!isDisconnected) {
                    connect(mBluetoothDeviceAddress);
                }
                isDisconnected = true;
                Log.e(TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction);
            }
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {

            if (status != BluetoothGatt.GATT_SUCCESS) {
                gatt.discoverServices();
            } else {
                initializeGattServices(gatt.getServices());
                if (mReadCharacteristic != null) {
                    Log.e("not null", "------------- onCharacteristicRead status: " + status + "-->");
                    mBluetoothGatt.readCharacteristic(mReadCharacteristic);
                }
            }
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                //  setCharacteristicNotification(characteristic,true);
                String rawData = setCharData(characteristic);
                displayData(rawData);

            }

           /* if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }*/
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            //  broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);

        }

        @SuppressLint("MissingPermission")
        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {

            super.onMtuChanged(gatt, mtu, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                gatt.discoverServices();
            }
        }


    };

    String setCharData(BluetoothGattCharacteristic characteristic) {
        byte[] data = characteristic.getValue();
        if (data != null && data.length > 0) {
            StringBuilder stringBuilder = new StringBuilder(data.length);
            for (byte byteData : data) {
                stringBuilder.append(String.format("%02X", byteData));
            }

            return stringBuilder.toString();
        }
        return null;
    }

    @SuppressLint({"LongLogTag", "MissingPermission"})
    private void initializeGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;

        for (BluetoothGattService gattService : gattServices) {
            uuid = gattService.getUuid().toString();
            if (uuid.equalsIgnoreCase(AppConstants.String_GENUINO101_SERVICE_READ) || uuid.equalsIgnoreCase(AppConstants.String_GENUINO101_SERVICE_WRITE)) {
                List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();

                for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                    int charaProp = gattCharacteristic.getProperties();
                    Log.e("Properties", "" + charaProp);

                    if (charaProp > 0 || BluetoothGattCharacteristic.PROPERTY_READ > 0) {
                        if (mNotifyCharacteristic != null) {
                            mBluetoothGatt.setCharacteristicNotification(
                                    mNotifyCharacteristic, true);
                        }

                        Log.e("Read char String is -->", "" + gattCharacteristic.getUuid().toString());
                        mReadCharacteristic = gattCharacteristic;
                        mBluetoothGatt.readCharacteristic(gattCharacteristic);  /// For Testing removed
                    }
                    if (charaProp > 0 || BluetoothGattCharacteristic.PROPERTY_NOTIFY > 0) {
                        mNotifyCharacteristic = gattCharacteristic;
                        mBluetoothGatt.setCharacteristicNotification(
                                gattCharacteristic, true);
                        Log.e("Read char String is -->", "" + gattCharacteristic.getUuid().toString());

                        mReadCharacteristic = gattCharacteristic;
                        mBluetoothGatt.readCharacteristic(gattCharacteristic);  /// For Testing removed
                    }
                    if (charaProp == 8 && BluetoothGattCharacteristic.PROPERTY_WRITE > 0) {
                        Log.e("Write char String is -->", "" + gattCharacteristic.getUuid().toString());
                        mWriteCharacteristic = gattCharacteristic;
                    }
                }

            }
        }
    }

    public void displayData(String data) {
          try {
            if (data != null) {
                List<String> lines = Arrays.asList(data.split("\\r?\\n"));
                String hexDataReceived = lines.get(lines.size() - 1);

                updateStringData = "";
                updateStringData = Base64Hex.decrypt(data, AppConstants.BEL_ENC_KEY);
                if (hexDataReceived.length() == 40) {
                    Log.e(TAG, "update in 40" + updateStringData);
                    updateStringData = Base64Hex.decrypt(hexDataReceived.substring(0, 32), AppConstants.BEL_ENC_KEY);
                    updateStringData += "0\"}";

                } else {

                    updateStringData = Base64Hex.decrypt(hexDataReceived, AppConstants.BEL_ENC_KEY);
                    Log.e(TAG, "Update Data : " + updateStringData);


                }


                broadcastUpdate(ACTION_DATA_AVAILABLE,updateStringData);
            }

        } catch (Exception e) {
            Log.e(TAG, "coming in catch " + data);
            e.printStackTrace();
        }
    }

    @SuppressLint("MissingPermission")
    public String publishWiFiToDevice(byte[] byteArray) {
        String status;
        try {
            BluetoothServiceClass.this.mWriteCharacteristic.setValue(byteArray);
            mBluetoothGatt.writeCharacteristic(mWriteCharacteristic);

            status = "succes";
        } catch (Exception e) {
            e.printStackTrace();
            status = "Fail";
        }
        return status;
    }

    public void sendToBLEJsonUpateData(String cmd) {
        //  bleWiFICounter+= 1;
        byte[] byteArray;
        try {
            String encriptedData = Encrypt.encrypt(cmd.toString(), AppConstants.BEL_ENC_KEY);
            byteArray = Encrypt.hexStringToByteArray(encriptedData);
            String status = publishWiFiToDevice(byteArray);
            if (status == "succes") {
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @SuppressLint("MissingPermission")
                    @Override
                    public void run() {

                        if (bleWiFICounter < 5) {
                            mBluetoothGatt.readCharacteristic(mReadCharacteristic);
                        } else {
                            Log.e("bluetooth", "true");
                        }

                    }
                }, 2000);


            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action,
                                 final String data) {
        Log.e("BluetoothData== > ", data);
        final Intent intent = new Intent(action);
        Log.w(TAG, "broadcastUpdate()");
        intent.putExtra(AppConstants.EXTRA_DATA, data);
        sendBroadcast(intent);
    }

    @SuppressLint("MissingPermission")
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            return false;
        }
        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        if (device == null) {
            Toast.makeText(BluetoothServiceClass.this, "Bluetooth Device not found !!", Toast.LENGTH_SHORT).show();

            return false;
        }
        mBluetoothGatt = device.connectGatt(this, false, mBluetoothGattCallback);
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(
     * android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    @SuppressLint("MissingPermission")
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    @SuppressLint("MissingPermission")
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(
     * android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    @SuppressLint("MissingPermission")
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled        If true, enable notification.  False otherwise.
     */
    @SuppressLint("MissingPermission")
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);

        // This is specific to Genuino 101 ledService.
    /*    if (UUID_GENUINO101_switchChare.equals(characteristic.getUuid())) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID_GENUINO101_switchChare);
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
        }*/
    }

    @SuppressLint("MissingPermission")
    public void writeCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothGatt == null) {
            Log.e("", "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.writeCharacteristic(characteristic);
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */

    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;
        return mBluetoothGatt.getServices();
    }

    public BluetoothAdapter getBluetoothAdapter(Context context) {
        // Get BluetoothAdapter and BluetoothLeScanner.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        return bluetoothManager.getAdapter();

    }

    public BluetoothLeScanner getBluetoothLeScanner() {
        // Get BluetoothAdapter and BluetoothLeScanner.
        return getBluetoothAdapter(this).getBluetoothLeScanner();
    }

    @SuppressLint("MissingPermission")
    public BluetoothDevice getDevice(BluetoothDevice device, String connectDevice) {

        if (device.getName() != null) {
            if (device.getName().startsWith(connectDevice)) {
                   stopLE();
                AppConstants.BLUETOOTH_DEVICE = device;
                return device;
            }
        }

        return null;
    }
    @SuppressLint("MissingPermission")
    private void addBluetoothDevice(BluetoothDevice device) {

        BluetoothDevice device1=getDevice(device,deviceName);
      if(device1!=null)
           {
               AppConstants.BLUETOOTH_DEVICE = device1;
               mDeviceAddres= device1.getAddress();
               unpairDevice(AppConstants.BLUETOOTH_DEVICE);
               connect(mDeviceAddres);

           }
          }

    private final ScanCallback scanCallback = new ScanCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);

            addBluetoothDevice(result.getDevice());
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);


        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);

        }
    };

    @SuppressLint("MissingPermission")
    public void startLE()
    {
        getBluetoothLeScanner().startScan(scanCallback);
    }
    @SuppressLint("MissingPermission")
    public void stopLE()
    {
        getBluetoothLeScanner().stopScan(scanCallback);
    }

    @SuppressLint("MissingPermission")
    public void closeGatt()
    {
        if (mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
            mBluetoothGatt.close();
        }

    }


    @SuppressLint("MissingPermission")
    private void unpairDevice(BluetoothDevice device) {
        try {
            Method m = device.getClass().getMethod("removeBond", (Class[]) null);
            ((Method) m).invoke(device, (Object[]) null);
        } catch (Exception e) { Log.e(TAG, e.getMessage()); }
    }



}
