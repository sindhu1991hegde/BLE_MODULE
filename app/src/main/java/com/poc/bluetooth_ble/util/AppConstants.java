package com.poc.bluetooth_ble.util;

import android.bluetooth.BluetoothDevice;

import java.util.UUID;

public class AppConstants {
    public static  BluetoothDevice BLUETOOTH_DEVICE;
    public static String COMMAND_AIR_POWER_ON = "{\"CMD\":\"IOT+CMD=01,0~\"}";
    public static String COMMAND_AIR_POWER_OFF = "{\"CMD\":\"IOT+CMD=02,0~\"}";
    public static String BEL_ENC_KEY = "3Lw9S2nE0Af8Zl1q";
    public static String String_GENUINO101_SERVICE_READ =
            "000000ee-0000-1000-8000-00805f9b34fb";
    public final static UUID UUID_GENUINO101_serviceRead =
            UUID.fromString(String_GENUINO101_SERVICE_READ);
    public final static String EXTRA_DATA =
            "android-er.EXTRA_DATA";

    public static String String_GENUINO101_SERVICE_WRITE =
            "000000ff-0000-1000-8000-00805f9b34fb";
    public final static UUID UUID_GENUINO101_serviceWrite =
            UUID.fromString(String_GENUINO101_SERVICE_WRITE);

    public static String String_GENUINO102_CHARREAD =
            "0000ee01-0000-1000-8000-00805f9b34fb";
    public final static UUID UUID_GENUINO102_charRead =
            UUID.fromString(String_GENUINO102_CHARREAD);

    public static String String_GENUINO102_CHARWRITE =
            "0000ff01-0000-1000-8000-00805f9b34fb";

    public final static UUID UUID_GENUINO102_charWrite =
            UUID.fromString(String_GENUINO102_CHARWRITE);
}
