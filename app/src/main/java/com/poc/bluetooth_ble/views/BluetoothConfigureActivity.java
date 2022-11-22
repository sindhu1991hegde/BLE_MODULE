package com.poc.bluetooth_ble.views;

import static com.poc.bluetooth_ble.service.BluetoothServiceClass.ACTION_DATA_AVAILABLE;
import static com.poc.bluetooth_ble.service.BluetoothServiceClass.ACTION_GATT_DISCONNECTED;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.poc.bluetooth_ble.R;
import com.poc.bluetooth_ble.service.BluetoothServiceClass;
import com.poc.bluetooth_ble.util.AppConstants;
import com.poc.bluetooth_ble.util.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class BluetoothConfigureActivity extends AppCompatActivity {

    TextView tv1,tv2,tv3,tv4;
    private boolean notNowCheck = false;
    BluetoothAdapter mBluetoothAdapter;
    private static final int RQS_ENABLE_BLUETOOTH = 123;
    private Handler mHandler;
    private static final long SCAN_PERIOD = 10000;
    private BluetoothServiceClass mBluetoothLeService;
    String deviceName,displayData;
    int PERMISSION_ALL = 1;
    String[] PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,

    };
    private StringBuilder name_password = new StringBuilder();
    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetoothconfigure);

        tv1 = (TextView) findViewById(R.id.tv1);
        tv2 = (TextView) findViewById(R.id.tv2);
        tv3 = (TextView) findViewById(R.id.tv3);
        tv4 = (TextView) findViewById(R.id.tv4);

        mHandler = new Handler();
        tv4.setText("Device Status");
        //request permission
        if (!Utils.hasPermissions(getApplicationContext(), PERMISSIONS)) {
            ActivityCompat.requestPermissions(BluetoothConfigureActivity.this, PERMISSIONS, PERMISSION_ALL);
        }
        else {
            mBluetoothLeService.closeGatt();
            startBluetooth();
        }

        tv1.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onClick(View view) {

                //mattress
            /*    name_password = new StringBuilder();
                name_password.append("{\"");
                name_password.append("CMD\":");
                name_password.append("\"IOT+CMD=21,");
                name_password.append(ssidLength + ",");
                name_password.append(spinTxt + ",");
                name_password.append(passwordLength + ",");
                name_password.append(password.trim() + "~" + "\"}");

                sendToBLEJsonUpateData("{\"CMD\":\"IOT+CMD=24~\"}");

                //{"SKEY":"2NK14BD621000002","MAC":"000000000000"}

                if (updateStringData.contains("{\"SKEY\"")) {
                    bleWiFICounter = 5;
                    if (notNowCheck == false) sendToBLEJsonUpateData(name_password.toString());

                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                JSONObject jsonObject = new JSONObject(updateStringData);
                                AppConstants.MATTRESS_S_KEY = jsonObject.getString("SKEY");
                                AppConstants.MATTRESS_MAC_ID = jsonObject.getString("MAC");
                                //  gotoDeviceDetails();
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }, 2000);
                }*/

                String ssidLength = "";
                String passwordLength = "";
                String spinTxt = "Radiant_Dev";
                String password = "rpsdev@BLR2017";

                if (spinTxt.length() < 10) {
                    ssidLength = "0" + spinTxt.length();
                } else {
                    ssidLength = "" + spinTxt.length();
                }


                if (password.length() < 10) {
                    passwordLength = "0" + password.length();
                } else {
                    passwordLength = "" + password.length();
                }
                //air purifier
                name_password = new StringBuilder();
                name_password.append("{\"");
                name_password.append("CMD\":");
                name_password.append("\"IOT+CMD=14,");
                name_password.append(ssidLength + ",");
                name_password.append(spinTxt + ",");
                name_password.append(passwordLength + ",");
                name_password.append(password.trim() + "~" + "\"}");
                if (!notNowCheck) {
                    notNowCheck = true;
                  mBluetoothLeService.sendToBLEJsonUpateData(name_password.toString());
                }

            }
        });

        tv2.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onClick(View view) {
                mBluetoothLeService.sendToBLEJsonUpateData(AppConstants.COMMAND_AIR_POWER_ON);

            }
        });

        tv3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(BluetoothConfigureActivity.this, BluetoothActionActivity.class));

            }
        });

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_ALL) {
            mBluetoothLeService.closeGatt();
            startBluetooth();
        }
    }

    @SuppressLint("MissingPermission")
    private void startBluetooth() {
        if (!mBluetoothAdapter.isEnabled()) {
            Intent intent1 = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent1, RQS_ENABLE_BLUETOOTH);
        } else {
            scanLeDevice(true);
        }
    }
    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && requestCode == RQS_ENABLE_BLUETOOTH) {
            scanLeDevice(true);
        }
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mBluetoothLeService.stopLE();
                }
            }, SCAN_PERIOD);
            mBluetoothLeService.startLE();
        } else {
            mBluetoothLeService.stopLE();
        }
    }


    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothServiceClass.LocalBinder) service).getService();
            mBluetoothAdapter=mBluetoothLeService.getBluetoothAdapter(getApplicationContext());
            if (!mBluetoothLeService.initialize()) {
                finish();
            }

        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        deviceName = "LIV-ARP";
        try {
            Intent gattServiceIntent = new Intent(this, BluetoothServiceClass.class);
            gattServiceIntent.putExtra("DEVICE",deviceName);
            bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        } catch (Exception e) {
        }
        registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter());

    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothServiceClass.ACTION_GATT_CONNECTED);
        intentFilter.addAction(ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    private final BroadcastReceiver gattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            final String action = intent.getAction();

            if (BluetoothServiceClass.ACTION_GATT_CONNECTED.equals(action)) {
                Toast.makeText(getApplicationContext(),"GATT CONNECTED",Toast.LENGTH_SHORT).show();
            } else if (ACTION_GATT_DISCONNECTED.equals(action)) {
                Toast.makeText(getApplicationContext(),"GATT DISCONNECTED",Toast.LENGTH_SHORT).show();
            }else if(ACTION_DATA_AVAILABLE.equals(action))
            {
                displayData=intent.getStringExtra(AppConstants.EXTRA_DATA);
                try {
                    JSONObject json=new JSONObject(displayData);
                    String json1=json.getString("ST");
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                String[]  st=displayData.split(",");

                if(st[1].equalsIgnoreCase("1"))
                {
                    tv4.setText("Device status: ON");
                }
                else
                {
                    tv4.setText("Device status: OFF");
                }

            }
        }
    };

    @SuppressLint("MissingPermission")
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(gattUpdateReceiver);
    }

}