package com.poc.bluetooth_ble.views;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;

import com.poc.bluetooth_ble.R;
import com.poc.bluetooth_ble.service.BluetoothServiceClass;
import com.poc.bluetooth_ble.util.AppConstants;

public class BluetoothActionActivity extends AppCompatActivity {

    Button b1;
    private BluetoothServiceClass mBluetoothLeService;
    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetoothexample);
        b1=(Button)findViewById(R.id.button);
        b1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mBluetoothLeService.sendToBLEJsonUpateData(AppConstants.COMMAND_AIR_POWER_OFF);
            }
        });
    }
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {

            mBluetoothLeService = ((BluetoothServiceClass.LocalBinder) service).getService();
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
        try {
            Intent gattServiceIntent = new Intent(this, BluetoothServiceClass.class);
            bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        } catch (Exception e) {
        }
    }
}