package com.thematic.blindTool;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemClickListener, View.OnLongClickListener {

    public static final UUID DEVICE_SERVICE_UUID = UUID.fromString("0000dfb0-0000-1000-8000-00805f9b34fb");
    public static final UUID DEVICE_CHARACTERISTIC_UUID = UUID.fromString("0000dfb1-0000-1000-8000-00805f9b34fb");

    String spmacaddress;
    String originalString = "234924";
    String get_address = "";
    String da = "080";
    String sensor = "011";

    TextToSpeech textToSpeech;

    //String device_connect_check = "";
    //String find_device = "";
    //String device_far = "";
    //String device_info = "";

    int speechtotext_check_button_number = 1;

    private SharedPreferencesConfig sharedPreferencesConfig;

    BluetoothGatt bluetoothGatt;
    BluetoothAdapter mBluetoothAdapter;
    BluetoothManager bluetoothManager;
    BluetoothDevice device;

    private boolean mScanning = false;
    private boolean now_activity_status = false;

    //搜尋時間 10秒
    private static final int SCAN_TIME = 10000;
    //用來將取地的藍牙放入，連線時使用
    private ArrayList<String> mBluetoothDevices = new ArrayList<String>();

    //這是listview的列表
    private ArrayList<String> mble_device = new ArrayList<>();

    private Handler mHandler = new Handler();

    //用來檢查是否掃描過
    private ArrayList<String> deviceName;
    private ArrayAdapter ble_list_adapter;

    ListView ble_dis_item;
    Button discovery_btn, device_find_btn, ble_device_change, get_device_info, device_change_distance, device_feature_open;
    ImageButton about_imagebtn;


    //進入第一頁，根據 boolean 決定 sharedpreferences 讀取第二頁(若有 address 則開啟第二頁)
    //第一頁搜尋連接藍芽，傳值確認藍牙設備，確認後寫入 sharedpreferences 並進入第二頁
    //
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        set_view_main_layout(true);
    }

    private void set_view_main_layout(boolean now_activity) {
        setContentView(R.layout.activity_main);
        now_activity_status = false;
        main_layout_init();
        if (now_activity) {
            sp_config();
        }
    }

    private boolean permission_check() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
        }
        return true;
    }

    //檢查藍牙4.0支援
    private void ble_check() {
        //利用getPackageManager().hasSystemFeature()檢查手機是否支援BLE設備，否則利用finish()關閉程式。
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void open_bluetooth() {
        mBluetoothAdapter.getDefaultAdapter();
        mBluetoothAdapter.enable();
        Toast.makeText(this,
                R.string.open_bluetooth, Toast.LENGTH_SHORT).show();
    }

    private void sp_config() {
        sharedPreferencesConfig = new SharedPreferencesConfig(getApplicationContext());
        if (sharedPreferencesConfig.ble_macaddress_check()) {
            set_view_activity_func_layout();
        }
    }

    private String sp_macaddress_get() {
        sharedPreferencesConfig = new SharedPreferencesConfig(getApplicationContext());
        return sharedPreferencesConfig.ble_macaddress();
    }

    //listview布局，獲取藍牙功能
    private void main_layout_init() {
        ble_check();
        permission_check();

        discovery_btn = findViewById(R.id.ble_discovery_btn);
        discovery_btn.setOnClickListener(this);

        //list 設定
        ble_dis_item = findViewById(R.id.item_paired);
        deviceName = new ArrayList();   //此ArrayList屬性為String，用來裝Devices Name
        ble_list_adapter = new ArrayAdapter(getApplicationContext(),
                R.layout.listview_item_paired, mble_device);
        ble_dis_item.setAdapter(ble_list_adapter);

        ble_dis_item.setOnItemClickListener(this);

        ble_adapter();

        //main的藍牙要額外寫
        //mHandler = new Handler();

        //final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        //mBluetoothAdapter = bluetoothManager.getAdapter();
        texttospeech_init();
    }

    private void ble_adapter() {
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        if (!mBluetoothAdapter.isEnabled()) {
            open_bluetooth();
        }
    }

    private void set_view_activity_func_layout() {
        setContentView(R.layout.activity_func_btn);
        now_activity_status = true;
        func_layout_init();
        if (sp_macaddress_get().equals(null)) {
            finish();
        } else {
            spmacaddress = sp_macaddress_get();
        }
    }

    private void func_layout_init() {
        about_imagebtn = findViewById(R.id.about_us_btn);
        about_imagebtn.setOnClickListener(this);

        ble_device_change = findViewById(R.id.ble_device_change);
        ble_device_change.setOnClickListener(this);
        ble_device_change.setOnLongClickListener(this);

        device_find_btn = findViewById(R.id.ble_device_find_btn);
        device_find_btn.setOnClickListener(this);
        device_find_btn.setOnLongClickListener(this);

        device_change_distance = findViewById(R.id.ble_device_change_distance);
        device_change_distance.setOnClickListener(this);
        device_change_distance.setOnLongClickListener(this);

        get_device_info = findViewById(R.id.ble_device_get_info);
        get_device_info.setOnClickListener(this);
        get_device_info.setOnLongClickListener(this);

        device_feature_open = findViewById(R.id.features_open);
        device_feature_open.setOnClickListener(this);
        device_feature_open.setOnLongClickListener(this);

        ble_adapter();
        texttospeech_init();
    }

    //按鈕點擊事件
    @Override
    public void onClick(View view) {


        String clickspeech = "";
        float speech_speed_check = 1.3f;
        textToSpeech.setPitch(speech_speed_check);
        textToSpeech.setSpeechRate(speech_speed_check);
        if (view.getId() == R.id.about_us_btn){
            Intent i = new Intent(this,AboutActivity.class);
            startActivity(i);
        }

        switch (view.getId()) {

            case R.id.ble_discovery_btn:
                if (permission_check()) {
                    if (!mBluetoothAdapter.isEnabled()) {
                        open_bluetooth();
                        return;
                    } else {
                        ScanFunction(true);
                    }
                } else {
                    permission_check();
                    return;
                }
                break;
            case R.id.ble_device_change:
                //clickspeech = (String) ble_device_change.getText();
                clickspeech = "更換藍芽連結";
                break;
            case R.id.ble_device_find_btn:
                //clickspeech = (String) device_find_btn.getText();
                clickspeech = "尋找設備";
                break;
            case R.id.ble_device_change_distance:
                //clickspeech = (String) device_change_distance.getText();
                clickspeech = "改變設備距離";
                break;
            case R.id.ble_device_get_info:
                //clickspeech = (String) get_device_info.getText();
                clickspeech = "目前設備資訊";
                break;
            case R.id.features_open:
                //clickspeech = (String) device_feature_open.getText();
                clickspeech = "功能設定";
                break;
        }
        textToSpeech.speak(clickspeech, TextToSpeech.QUEUE_FLUSH, null);
    }

    //搜尋到的藍牙以 Item 顯示，item點擊事件，做連線用
    //以 address 做連線，並取消搜尋，
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {

        get_address = mBluetoothDevices.get(position);
        //Toast.makeText(this, address, Toast.LENGTH_SHORT).show();
        device = mBluetoothAdapter.getRemoteDevice(get_address);
        mBluetoothAdapter.getBluetoothLeScanner().stopScan(scanCallback);

        bluetoothGatt = device.connectGatt(MainActivity.this, true, gattCallback);
    }

    //藍芽搜尋
    //此為ScanFunction，輸入函數為boolean，如果true則開始搜尋，false則停止搜尋
    private void ScanFunction(boolean enable) {
        final BluetoothLeScanner bluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

        if (enable) {
            mHandler.postDelayed(new Runnable() {
                //啟動一個Handler，並使用postDelayed在10秒後自動執行此Runnable()
                @Override
                public void run() {
                    bluetoothLeScanner.stopScan(scanCallback);//停止搜尋
                    mScanning = false; //搜尋旗標設為false
                    Log.d("AA", "取消搜尋");
                }
            }, SCAN_TIME); //SCAN_TIME為幾秒後要執行此Runnable，此範例中為10秒
            mScanning = true; //搜尋旗標設為true
            bluetoothLeScanner.startScan(scanCallback);//開始搜尋BLE設備
            Log.d("AA", "開始搜尋");
        } else {
            mScanning = false;
            bluetoothLeScanner.stopScan(scanCallback);
        }
    }

//注意，在此enable==true中的Runnable是在10秒後才會執行，因此是先startLeScan，10秒後才會執行Runnable內的stopLeScan
//在BLE Devices Scan中，使用的方法為startLeScan()與stopLeScan()，兩個方法都需填入callback，當搜尋到設備時，都會跳到
//callback的方法中

    //搜尋藍牙4.0 Callback
    private ScanCallback scanCallback = new ScanCallback() {

        @Override
        //ScanResult主要用到的就是這getDevice()、getScanRecord()、getRssi()這三個方法，通過這三個get我們可以知道掃描到的設備，信號強度，以及一些掃描記錄。
        public void onScanResult(int callbackType, final ScanResult result) {
            super.onScanResult(callbackType, result);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String device_name = null;
                    device_name = result.getDevice().getName();
                    String device_address = result.getDevice().getAddress();

                    /*
                    if (device_name.equals("null") || device_name.equals("") || device_name.equals(null)) {
                        device_name = "無名稱裝置";
                    }
                    */

                    //ScanRecord scanRecord = result.getScanRecord();
                    //device_name = scanRecord == null ? "unknown" : scanRecord.getDeviceName();

                    if (result.getDevice().getName() == null) {
                        //Toast.makeText(MainActivity.this, "BB", Toast.LENGTH_SHORT).show();
                        device_name = "裝置名稱未知";
                    }

                    if (now_activity_status) {
                        device = mBluetoothAdapter.getRemoteDevice(sharedPreferencesConfig.ble_macaddress());
                        mBluetoothAdapter.getBluetoothLeScanner().stopScan(scanCallback);
                        bluetoothGatt = device.connectGatt(MainActivity.this, true, gattCallback);
                    } else {
            /*
                    String address = "0C:B2:B7:46:49:A3";
                    if (deviceName.contains(address)) {
                        device = mBluetoothAdapter.getRemoteDevice(address);
                        mBluetoothAdapter.getBluetoothLeScanner().stopScan(scanCallback);
                        bluetoothGatt = device.connectGatt(MainActivity.this, true, gattCallback);

                    }
*/
                        //每搜尋到一個就更新列表一次
                        if (!deviceName.contains(device_address)) {
                            deviceName.add(result.getDevice().getAddress());

                            mBluetoothDevices.add(result.getDevice().getAddress());
                            mble_device.add(" " + device_name);
                            ble_list_adapter.notifyDataSetChanged();
                        }
                    }
                }
            });
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            //抓取掃描記錄
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            //返回失敗參數
            //errorCode=1;Fails to start scan as BLE scan with the same settings is already started by the app.
            //errorCode=2;Fails to start scan as app cannot be registered.
            //errorCode=3;Fails to start scan due an internal error
            //errorCode=4;Fails to start power optimized scan as this feature is not supported.
        }
    };

    //連接回調
    private BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onPhyUpdate(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
            super.onPhyUpdate(gatt, txPhy, rxPhy, status);
        }

        @Override
        public void onPhyRead(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
            super.onPhyRead(gatt, txPhy, rxPhy, status);
        }

        //連線狀態改變
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            //連線成功後掃描服務
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                if (status == BluetoothProfile.STATE_DISCONNECTED) {
                    gatt.connect();
                } else {//嘗试重連
                    Log.e("connect", "reconnect");
                    gatt.disconnect();
                    gatt.close();
                    device.connectGatt(MainActivity.this, false, gattCallback);
                }
                Toast.makeText(MainActivity.this, "斷開", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            BluetoothGattCharacteristic alertLevel = null;

            //通過uuid找到服務
            BluetoothGattService gattservice = gatt.getService(DEVICE_SERVICE_UUID);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                //讀取服務和對應特徵值
                if (gattservice != null) {
                    Log.e("gatt", "service_uuid_success");

                    BluetoothGattCharacteristic ble_Characteristic = gattservice.getCharacteristic(DEVICE_CHARACTERISTIC_UUID);
                    //List<BluetoothGattCharacteristic> characteristics = gattservice.getCharacteristics();


                    String uuid = null;
                    //列出服務及特徵值
                    List<BluetoothGattService> gattServices = gatt.getServices();
                    for (BluetoothGattService gattService : gattServices) {
                        uuid = gattService.getUuid().toString();
                        Log.e("OnServiceDiscovery", "serviceUUid--" + uuid);
                        List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
                        for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                            uuid = gattCharacteristic.getUuid().toString();
                            Log.e("OnCharacteristic_Dis", "characteristic--" + uuid + "--" + gattCharacteristic.getProperties());

                            if ("0000ffe1-0000-1000-8000-00805f9b34fb".equals(gattCharacteristic.getUuid().toString())) {
                                //linkLossService = bluetoothGattService;
                                alertLevel = gattCharacteristic;
                                Log.e("daole", alertLevel.getUuid().toString());
                            }
                        }
                        enableNotification(true, gatt, alertLevel);
                    }

                    if (ble_Characteristic != null) {
                        if (now_activity_status) {
                            //啟用onCharacteristicChanged()，用於接收資料
                            Boolean isTrue = gatt.setCharacteristicNotification(ble_Characteristic, true);

                            byte[] b = hexStringToByteArray(originalString);

                            //ble_Characteristic.setValue(new byte[]{0x01, 0x20, 0x03});
                            ble_Characteristic.setValue(b);
                            Log.e("writegatt", "E");
                            bluetoothGatt.writeCharacteristic(ble_Characteristic);

                            BluetoothGattDescriptor descriptor = ble_Characteristic.getDescriptor(DEVICE_CHARACTERISTIC_UUID);
                            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            gatt.writeDescriptor(descriptor);
                            Log.e("writegatt", "success");
                        } else {
                            sharedPreferencesConfig.ble_macaddress_status(get_address, true);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    set_view_activity_func_layout();
                                }
                            });
                        }
                        gatt.disconnect();
                        gatt.close();
                        //sendMSG(SERVICE_DISCOVERED);
                    } else {
                        Log.e("writegatt", "failed");
                    }
                } else {
                    Log.e("gatt", "service_uuid_failed");
                }
                /*BluetoothGattCharacteristic writer = bluetoothGatt.getService(DEVICE_UUID).getCharacteristic(DEVICE_UUID);
                byte[] data = new byte[10];
                writer.setValue(data);
                gatt.writeCharacteristic(writer);*/
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);

            //讀取成功顯示
            if (BluetoothGatt.GATT_SUCCESS == status) {
                String receive = "";
                for (int i = 0; i < characteristic.getValue().length; i++) {
                    int v = characteristic.getValue()[i] & 0xFF;
                    receive += Integer.toHexString(v);
                }
                Toast.makeText(MainActivity.this, receive, Toast.LENGTH_SHORT).show();
            }
        }

        //監聽寫入成功
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            if (BluetoothGatt.GATT_SUCCESS == status) {
                //sendMSG(WRITE_SUCCESS);
                Toast.makeText(MainActivity.this, "Write Success", Toast.LENGTH_SHORT).show();
            }
        }

        //監聽寫入之後 返回的數據
        @Override
        public void onCharacteristicChanged(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);

            if (characteristic.getUuid().equals(DEVICE_CHARACTERISTIC_UUID)) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        if (characteristic.getValue().toString().equals(null)) {
                            Toast.makeText(MainActivity.this, "null", Toast.LENGTH_SHORT).show();
                        } else {
                            byte[] value = characteristic.getValue();
                            String a = "";
                            Log.i("BLE", "receive value ----------------------------");
                            for (int i = 0; i < value.length; i++) {
                                Log.i("BLE", "character_value = " + (char) value[i]);
                                a += (char) value[i];
                            }
                            //Toast.makeText(getApplicationContext(), a, Toast.LENGTH_SHORT).show();
                            Log.e("AAA", a);
                            if (a.length() == 9) {
                                Toast.makeText(MainActivity.this, string1(a), Toast.LENGTH_SHORT).show();
                                textTospeech(string1(a));
                            }
                        }
                        gatt.disconnect();
                        gatt.close();
                    }
                });
            }

            //使用的藍芽裝置傳送的是16進位制，需要處理，否則會亂碼
            //String receive = bytesToHexString(characteristic.getValue());
        }

        //监听写入数据之后数据的返回通知
        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            super.onReliableWriteCompleted(gatt, status);
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
        }
    };

    //取得BLE SERVICE連接更新
    private void enableNotification(boolean enable, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        if (gatt == null || characteristic == null)
            return; //这一步必須要有 否則收不到通知
        gatt.setCharacteristicNotification(characteristic, enable);
    }

    //轉為陣列 用於 連接 傳送數據
    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    //點擊事件判斷使用
    private String ble_send_data_check(int status) {
        String data = null;
        switch (status) {
            case 1:
                data = "#F$";
                data = convertAscii(data);
                break;
            case 2:

                break;
            case 3:
                data = "#I$";
                data = convertAscii(data);
                break;
        }
        return data;
    }

    //轉為ASCII HEX
    private String convertAscii(String convert) {

        String hex = "";
        for (int i = 0; i < convert.length(); i++) {
            char a = convert.charAt(i);
            int ascii = (int) a;
            String decimal = Integer.toHexString(ascii);
            hex += String.valueOf(decimal);
        }
        return hex;
    }

    //字串
    private String string1(String check_substring) {
        String re_string = "";
        if (check_substring != null) {
            re_string = check_substring.substring(2, check_substring.lastIndexOf("$"));
        }
        return re_string;
    }

    private void texttospeech_init() {
        textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                if (i == TextToSpeech.SUCCESS) {
                    int result = textToSpeech.setLanguage(Locale.TAIWAN);
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Toast.makeText(MainActivity.this, "not sup", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "text to speech suppport", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "fail", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void textTospeech(String speechtext) {
        String str_1 = "電量";
        String str_2 = "避障功能";
        String str_3 = "聲音提示";
        String str_4 = "震動提示";

        //textToSpeech = new TextToSpeech(this,this);

        float speech_speed = 1.1f;
        textToSpeech.setPitch(speech_speed);
        textToSpeech.setSpeechRate(speech_speed);
        //textToSpeech.speak(str_1,TextToSpeech.QUEUE_FLUSH,null);
        sensor = speechtext.substring(3, 6);
        //
        Toast.makeText(this, sensor, Toast.LENGTH_SHORT).show();
        String fl = speechtext.substring(0, 3);
        String fl_status_text = "";
        if (fl.equals("777")) {
            fl_status_text = "充電中";
        } else if (fl.equals("999")) {
            fl_status_text = "電力異常";
        } else {
            fl_status_text = fl + "%";
        }
        textToSpeech.speak(str_1 + "、" + fl_status_text + "、" +
                //str_2 + speechtext.substring(3, 4) +
                str_2 + "、" + check_sensoe_status(speechtext.substring(3, 4)) + "、" +
                str_3 + "、" + check_sensoe_status(speechtext.substring(4, 5)) + "、" +
                str_4 + "、" + check_sensoe_status(speechtext.substring(5, 6)), TextToSpeech.QUEUE_FLUSH, null);
    }

    private String check_sensoe_status(String sensoe_status) {
        if (sensoe_status.equals("0")) {
            return "關閉";
        } else {
            return "開啟";
        }
    }

    private void speechTotext_control() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "說些什麼吧");
        try {
            if (speechtotext_check_button_number == 1) {
                startActivityForResult(intent, 1);
            } else if (speechtotext_check_button_number == 0) {
                startActivityForResult(intent, 2);
            }
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
        }
    }

    //語音回傳
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case 1:
                if (data != null) {
                    ArrayList<String> arrayList = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    String getspeechText = arrayList.get(0);

                    String data_1;
                    da = speech_control_sensor(getspeechText);
                    data_1 = "#S" + da + sensor + "$";
                    originalString = convertAscii(data_1);
                    ScanFunction(true);

                } else {
                    Toast.makeText(this, "語音無法辨識", Toast.LENGTH_SHORT).show();
                }
                break;
            case 2:
                if (data != null) {
                    ArrayList<String> arrayList = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    String getspeechText_status = arrayList.get(0);

                    String data_1;
                    data_1 = "#S" + da + getspeechText_status + "$";
                    originalString = convertAscii(data_1);
                    ScanFunction(true);

                } else {
                    Toast.makeText(this, "語音無法辨識", Toast.LENGTH_SHORT).show();
                }
                break;
            case 3:
                Bundle bundle = data.getExtras();
                sensor = bundle.getString("check_status");

                String data_1;
                data_1 = "#S" + da + sensor + "$";
                originalString = convertAscii(data_1);
                ScanFunction(true);
                break;
        }
    }

    private String speech_control_sensor(String control) {
        String a1[] = {"遠距離", "遠距離。", "中距離", "中距離。", "近距離", "近距離。", "近距，離"};
        da = "080";
        for (int i = 0; i < a1.length; i++) {
            if (control.equals(a1[i])) {
                if (i == 0 || i == 1) {
                    da = "120";
                } else if (i == 2 || i == 3) {
                    da = "080";
                } else if (i == 4 || i == 5 || i == 6) {
                    da = "040";
                } else {
                    Toast.makeText(this, "語音無法辨識", Toast.LENGTH_SHORT).show();
                }
            }
        }
        return da;
    }

    @Override
    public boolean onLongClick(View view) {


        switch (view.getId()) {

            case R.id.ble_discovery_btn:
                break;
            case R.id.ble_device_change:
                set_view_main_layout(false);
                break;
            case R.id.ble_device_find_btn:
                originalString = ble_send_data_check(1);
                ScanFunction(true);
                break;
            case R.id.ble_device_change_distance:
                textToSpeech.stop();
                speechtotext_check_button_number = 1;
                speechTotext_control();
                break;
            case R.id.ble_device_get_info:
                originalString = ble_send_data_check(3);
                ScanFunction(true);
                break;
            case R.id.features_open:
                textToSpeech.stop();
                //
                Intent features_intent = new Intent(this, Features_Activity.class);
                features_intent.putExtra("sensor_1", sensor.substring(0, 1));
                features_intent.putExtra("sensor_2", sensor.substring(1, 2));
                features_intent.putExtra("sensor_3", sensor.substring(2, 3));
                startActivityForResult(features_intent, 3);
                //
                //speechtotext_check_button_number = 0;
                //speechTotext_control();
                break;
        }

        return true;
    }
}
