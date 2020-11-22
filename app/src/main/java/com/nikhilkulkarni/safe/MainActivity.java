package com.nikhilkulkarni.safe;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraMetadata;
import android.os.Handler;
import android.os.ParcelUuid;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private BluetoothGattServer bluetoothGattServer;
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    private BluetoothGattCharacteristic bluetoothGattCharacteristic;
    private BluetoothGatt bluetoothGatt;
    private boolean flag = false;
    private Runnable runnable;
    private int delay = 30000;
    private  Handler handler;
    private ScanCallback leScanCallback;
    private ScanFilter.Builder scanFilterBuilder;
    private ScanFilter scanFilter;
    private  BluetoothGattServerCallback bluetoothGattServerCallback;
    private AdvertiseCallback advertiseCallback;
    private List<ScanFilter> scanFilters;
    private List<BluetoothGattService> bluetoothGattServices;
    private String Arogya_uuid =  "45ed2b0c-50f9-4d2d-9ddc-c21ba2c0f825";
    private String my_uuid =  "cfe41070-9c06-11ea-bb37-0242ac130002";
    private String my_char_uuid =  "9d495d8e-0726-4ef4-8615-bba7780db90a";
    private String my_des_uuid =  "b9ee9369-9295-4245-8eda-dadc70d8a13b";
    private TextView msg;
    private byte [] data;
    private CameraMetadata cameraMetadata;
    private  CameraCharacteristics cameraCharacteristics;
    private CameraCharacteristics.Key cameraKey;

    private List<BluetoothGattCharacteristic> bluetoothGattCharacteristics;
    BluetoothGattCallback bluetoothGattCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initializes Bluetooth adapter.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        scanFilters = new ArrayList<ScanFilter>();
        bluetoothGattServices = new ArrayList<>();
        bluetoothGattCharacteristics = new ArrayList<>();
        scanFilterBuilder = new ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString(my_uuid));
        scanFilter = scanFilterBuilder.build();
        scanFilters.add(scanFilter);
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        msg = findViewById(R.id.msg_disp);

     handler = new Handler();
        if (bluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "Bluetooth is not supported by this device, This app will not work", Toast.LENGTH_SHORT).show();
        }
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 0);
        }
        else
        {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
        }


         leScanCallback = new ScanCallback() {
             @Override
             public void onScanResult(int callbackType, ScanResult result) {
                 super.onScanResult(callbackType, result);
                 BluetoothDevice device = result.getDevice();


                 int rssi = result.getRssi();
                 String deviceHardwareAddress = device.getAddress();
                 ScanRecord scanRecord = result.getScanRecord();
                 String name = scanRecord.getDeviceName();

                 if(rssi <= 65 )
                {
                     bluetoothGatt = device.connectGatt(getApplicationContext(),false,bluetoothGattCallback);
                     bluetoothLeScanner.stopScan(leScanCallback);
                }

                 if(scanRecord.getServiceUuids()!= null) {


                     String bytes = scanRecord.getServiceUuids().toString();
                     Log.d("TAGG", "\n" + name + " address:" + deviceHardwareAddress + " Rssi:" + rssi + " Ser Data uuid:" + bytes);

                 }

             }
         };
        bluetoothGattServerCallback = new BluetoothGattServerCallback() {
            @Override
            public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
                super.onConnectionStateChange(device, status, newState);
                Log.d("TAGG","Client Connected");



            }

            @Override
            public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
                super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
                String dataa = new String(value,StandardCharsets.UTF_8);
                Log.d("TAGG","DATA RECIEVED FROM CLIENT : "+ dataa);
                msg.setText(dataa);
            }
        };
        advertiseCallback = new AdvertiseCallback() {
            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                super.onStartSuccess(settingsInEffect);
                Log.d("TAGG","Advertise Started");
            }
        };
        bluetoothGattCallback = new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);
                if(newState == BluetoothProfile.STATE_CONNECTED){
                    Log.d("TAGG","Connected");
                    bluetoothGatt.discoverServices();

                }


            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                super.onServicesDiscovered(gatt, status);
                bluetoothGattServices = bluetoothGatt.getServices();

                Log.d("TAGG","services discovered ");
                read(bluetoothGattServices);

            }




        };
        bluetoothGattServer = bluetoothManager.openGattServer(getApplicationContext(), bluetoothGattServerCallback);
        BluetoothGattService bluetoothGattService;
        bluetoothGattService =new BluetoothGattService(UUID.fromString(my_uuid),0);
        BluetoothGattCharacteristic bluetoothGattCharacteristic =
                new BluetoothGattCharacteristic(UUID.fromString(my_char_uuid),
                BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_WRITE);

        bluetoothGattService.addCharacteristic(bluetoothGattCharacteristic);
        bluetoothGattServer.addService(bluetoothGattService);
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);
        Log.d("TAGG","Registered");
        startAdvertising();

    }}

    private void read(List<BluetoothGattService> bluetoothGattServices)
    {
        for( BluetoothGattService bluetoothGattService :bluetoothGattServices)
        {
            bluetoothGattCharacteristics = bluetoothGattService.getCharacteristics();
            Log.d("TAGG","service uuid"+bluetoothGattService.getUuid().toString()+" char = "+bluetoothGattCharacteristics.size());

           if(bluetoothGattService.getUuid().toString().matches(my_uuid)){
               BluetoothGattCharacteristic bluetoothGattCharacteristic = bluetoothGattService.getCharacteristic(UUID.fromString(my_char_uuid));
               bluetoothGattCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                bluetoothGatt.setCharacteristicNotification(bluetoothGattCharacteristic,true);

                try{

                    data = " hello ble".getBytes(StandardCharsets.UTF_8);
                }
                catch (Exception e)
                {
                    Log.d("TAGG"," Exception: "+ e );
                }
                bluetoothGattCharacteristic.setValue(data);
                bluetoothGatt.writeCharacteristic(bluetoothGattCharacteristic);



            /*for (BluetoothGattCharacteristic bluetoothGattCharacteristic :bluetoothGattCharacteristics)
            {
                Log.d("TAGG","data: "+bluetoothGattCharacteristic.toString());
                int per = bluetoothGattCharacteristic.getPermissions();
                int pro = bluetoothGattCharacteristic.getProperties();
                bluetoothGatt.readDescriptor(bluetoothGattCharacteristic.getDescriptor());
                bluetoothGatt.readCharacteristic(bluetoothGattCharacteristic);
                Log.d("TAGG","returned" + "permission: "+ per + "property: "+pro);
            }*/
        }

        }
    }


    private void startAdvertising() {
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        mBluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
        if (mBluetoothLeAdvertiser == null) {
            Log.w("TAGG", "Failed to create advertiser");
            return;
        }

        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setConnectable(true)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .build();

        AdvertiseData data = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .setIncludeTxPowerLevel(false)
                .addServiceUuid(new ParcelUuid(UUID.fromString(my_uuid)))
                .build();

        mBluetoothLeAdvertiser
                .startAdvertising(settings, data, advertiseCallback);
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                Log.d("TAGG","Device Found");
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                BluetoothClass bclass = intent.getParcelableExtra(BluetoothDevice.EXTRA_CLASS);
                String deviceName = device.getName();
                String btclass = bclass.toString();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                int  rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI,Short.MIN_VALUE);

                Log.d("TAGG", "\n" + deviceName + " class:" + btclass + " Rssi:" + rssi);

            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Don't forget to unregister the ACTION_FOUND receiver.
        unregisterReceiver(receiver);
    }

    public void enableDiscovery(View view) {

        flag = true;



            handler.postDelayed(runnable = new Runnable() {
                @Override
                public void run() {
                    Log.d("TAGG","Discovery End");
                    bluetoothAdapter.cancelDiscovery();
                    bluetoothAdapter.startDiscovery();
                    Log.d("TAGG","Discovery Started");

                    handler.postDelayed(runnable,delay);

                }
            }, delay);




    }

    public void disableDiscovery(View view) {
        handler.removeCallbacks(runnable);
        bluetoothAdapter.cancelDiscovery();
    }

    public void makeDiscoverable(View view) {
        Log.d("TAGG","Device is discoverable");
        Intent discoverableIntent =
                new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);
        startActivity(discoverableIntent);
    }

    public void stopDiscoverable(View view) {
        Log.d("TAGG","Device is undiscoverable");
        Intent discoverableIntent =
                new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 1);
        startActivity(discoverableIntent);
    }

    public void leScan(View view) {


        ScanSettings scanSettings = new ScanSettings.Builder().build();
        bluetoothLeScanner.startScan(scanFilters,scanSettings,leScanCallback);



    }


    public void stopLeScan(View view) {

        bluetoothLeScanner.stopScan(leScanCallback);
    }

    public void clear(View view) {
        msg.setText("");
        if(bluetoothGatt == null)
        {
            return;
        }
        bluetoothGatt.close();
            bluetoothGatt = null;


    }
}
