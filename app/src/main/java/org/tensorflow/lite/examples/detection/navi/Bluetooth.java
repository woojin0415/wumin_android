package org.tensorflow.lite.examples.detection.navi;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.tensorflow.lite.examples.detection.storage.BLEStorage;
import org.tensorflow.lite.examples.detection.storage.StoreManagement;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;


public class Bluetooth {

    private String[] sector1 = {"D8:3A:DD:1D:C4:45", "D8:3A:DD:1D:C5:EA", "D8:3A:DD:1D:C6:FA", "D8:3A:DD:1D:C6:9D", "D8:3A:DD:1D:C6:85", "D8:3A:DD:1D:C7:5A"};
    private String[] sector2 = {"D8:3A:DD:1D:C4:90", "D8:3A:DD:1D:C2:B3", "D8:3A:DD:1D:C6:B5", "D8:3A:DD:1D:C6:1F", "D8:3A:DD:1D:C6:3A"};
    private String[] sector3 = {"D8:3A:DD:1D:C6:48", "D8:3A:DD:1D:C7:09", "D8:3A:DD:1D:C7:00", "D8:3A:DD:1D:C6:AD", "D8:3A:DD:1D:C3:C1"};
    private String[][] sections;

    private Retrofit retrofit;
    private BluetoothLeScanner scanner;
    private BluetoothAdapter adapter;
    private int[][] rssi_value;



    // 구역에 있는 맥 주소
    private String[] Macs;
    private boolean[] check;
    private int[] n_b;
    private int collect_num = 6;
    //이전에 분류된 구역 번호
    private double currentSector = -1;

    //몇 번째 보내는 메시지인지 확인
    private int send_num;

    private user_orientation user_ori;
    private String room;


    private TextToSpeech tts;


    //location 정보가 들어가는 queue
    //queue에 저장된 location들이 전부 일치하면 해당 location으로 변경
    private org.tensorflow.lite.examples.detection.navi.work_information wi;

    //작품 설명중이면 T 아니면 F
    private BLEStorage[] ble_storage;
    private StoreManagement store_m;
    private int ble_storage_count;

    private boolean ble_start = false;
    private TextView tv;



    public Bluetooth(BluetoothAdapter adapter, user_orientation user_ori, TextToSpeech tts, StoreManagement store_m, TextView tv){
        this.adapter = adapter;
        scanner = adapter.getBluetoothLeScanner();
        this.rssi_value = new int[6][collect_num];

        this.user_ori = user_ori;
        this.tts = tts;


        //section = "0";
        sections = new String[3][];
        sections[0] = sector1;
        sections[1] = sector2;
        sections[2] = sector3;
        wi = new work_information(3, 11);

        Macs = sections[0];

        //ble_storage = new BLEStorage[10000];

        //this.store_m = store_m;

        this.tv = tv;
    }


    //작품 정보 입력
    public void set_init(String section, double sector){
        this.room = section;
        this.currentSector = sector;
        this.Macs = sections[Integer.valueOf(section)];
        n_b = new int[Macs.length];
        check = new boolean[Macs.length];

        for (int i = 0; i < n_b.length; i++) {
            n_b[i] = 0;
            check[i] = false;
        }
        setRetrofitInit();

    }

    public void start() {
        //store_m.reset_ble(ble_storage);
        //ble_storage_count = 0;

        scan();
        //Log.e("BLE", "시작");
    }

    public int getsection(){
        return Integer.valueOf(room);
    }
    public double get_sector() {return currentSector;}


    public void stop() throws IOException {
        stopscan();
        for (int i = 0; i < n_b.length; i++) {
            n_b[i] = 0;
            check[i] = false;
        }
        //store_m.ble_store(ble_storage);
    }


    public void scan() {
        //Log.e("BLE", "Scan Button");
        adapter.startLeScan(scancallback);
    }


    private void stopscan() {

        //adapter.stopLeScan(scancallback);
    }

    private BluetoothAdapter.LeScanCallback scancallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] bytes) {
                    String macAdd = device.getAddress();
                    Log.e("ble", "Scan");

                    if (true && ble_start) {
                        //모든 비콘 데이터들이 수집되었으면 서버로 전송
                        if (check_rssi(check)) {
                            for(int i =0; i<n_b.length; i++){
                                n_b[i] = 0;
                                check[i] = false;
                            }
                            //tts.speak("데이터 전송", TextToSpeech.QUEUE_FLUSH, null);
                            send(rssi_value);
                            send_num++;
                            Log.e("comm", "send");
                            scan();
                        }

                        //각 구역별 배치된 비콘 RSSI 수집
                        for (int i = 0; i < Macs.length; i++) {
                            if (macAdd.equals(Macs[i]) && !check[i]) {
                                if (n_b[i] != collect_num) {
                                    //Log.e("ble",String.valueOf(i) + ": " + String.valueOf(n_b[i]));
                                    rssi_value[i][n_b[i]] = -rssi;
                                    n_b[i]++;
                                } else
                                    check[i] = true;
                            }
                        }

                    }
        }
    };

    private void setRetrofitInit () {
        Gson gson = new GsonBuilder().setLenient().create();
        retrofit = new Retrofit.Builder()
                .baseUrl("http://192.168.1.2:7777/")
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
    }

    private void send (int[][] rssi_value) {
        //Log.e("comm", "전송");
        String r1 = "0";
        String r2 = "0";
        String r3 = "0";
        String r4 = "0";
        String r5 = "0";
        String r6 = "0";
        Call<String> call = null;

        Log.e("comm", room);


        room service = retrofit.create(room.class);
        r1 = mean_rssi(rssi_value[0]);
        r2 = mean_rssi(rssi_value[1]);
        r3 = mean_rssi(rssi_value[2]);
        r4 = mean_rssi(rssi_value[3]);
        r5 = mean_rssi(rssi_value[4]);
        if (room.equals("1"))
            r6 = mean_rssi(rssi_value[5]);
        else
            r6 = "x";
        call = service.getMember(room, user_ori.get_direction(), r1, r2, r3, r4, r5, r6);


        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                String response_ = response.body();

                tv.setText(response_);


            }
            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Log.e("TAG", "onFailure: " + t);
            }
        });

    }

    private String mean_rssi(int arr[]){
        int sum = 0;
        for (int rssi: arr){
            sum+= rssi;
        }
        int med = sum/arr.length;
        return String.valueOf(med);
    }

    public boolean check_rssi(boolean [] arr){
        for(int i = 0; i< arr.length; i++)
            if(arr[i] == false)
                return false;
        return true;
    }

}

