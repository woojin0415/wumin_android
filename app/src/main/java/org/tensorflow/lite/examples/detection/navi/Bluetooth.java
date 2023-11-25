package org.tensorflow.lite.examples.detection.navi;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.tensorflow.lite.examples.detection.DetectorActivity;

import java.util.Arrays;

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
    private TextView tv;
    private ImageView[] maps;
    private BluetoothAdapter adapter;
    private int[][] rssi_value;

    //응답을 받고 나서 ble scan을 하도록 설정하는 변수 (t: 스캔, f: 스캔 x)
    private boolean scan;
    private int interval_count;



    // 구역에 있는 맥 주소
    private String[] Macs;
    private boolean[] check;
    private int[] n_b;

    private String cali;
    private int[] rssi_cali;
    private int cali_median;
    private int num_of_cali;

    //median filter에 사용할 rssi 값 개수
    private int collect_num = 10;
    //이전에 분류된 구역 번호
    private int currentSector = -1;

    //몇 번째 보내는 메시지인지 확인
    private int send_num;

    private user_orientation user_ori;
    private String section;

    //그림이 있는 map index 배열
    int[] p_loc_1 = new int[]{0, 1, 4, 8, 9};
    int[] corner_1 = new int[]{2, 7, 10};

    int[] p_loc_2 = new int[]{2, 7, 8};
    int[] corner_2 = new int[]{1, 3, 8};

    int[] p_loc_3 = new int[]{1, 3, 6};
    int[] corner_3 = new int[]{2, 5, 7};

    int[][] p_loc = new int[3][];
    int[][] corner = new int[3][];

    //전시 구역이 끝나는 지점
    int [][] last = new int[3][];

    int [] secion_cali = new int[]{2,1,2};

    private TextToSpeech tts;

    private boolean changable;

    //현재 사용자 방향 t: 정방향 f: 반대 방향
    private boolean direction;

    //현재 구역에 있는 작품 이름 및 설명
    private String[] p_name;

    //location 정보가 들어가는 queue
    //queue에 저장된 location들이 전부 일치하면 해당 location으로 변경
    private int[] location_queue;
    // location queue index 위치 n_lq % 30
    private int n_lq;
    DetectorActivity da;
    private boolean in_pic = true;



    public Bluetooth(BluetoothAdapter adapter, TextView tv, ImageView[] maps, user_orientation user_ori, TextToSpeech tts, DetectorActivity da){
        this.adapter = adapter;
        scanner = adapter.getBluetoothLeScanner();
        this.rssi_value = new int[6][collect_num];
        this.tv = tv;
        this.maps = maps;
        this.user_ori = user_ori;
        this.tts = tts;
        this.da = da;


        changable = true;
        user_ori.set_ble(this);
        location_queue = new int[5];

        setRetrofitInit();
        section = "0";

        user_ori.sector_change(maps[0]);

        p_loc[0] = p_loc_1;
        p_loc[1] = p_loc_2;
        p_loc[2] = p_loc_3;

        corner[0] = corner_1;
        corner[1] = corner_2;
        corner[2] = corner_3;

        last[0] = new int[]{13};
        last[1] = new int[]{5, 9};
        last[2] = new int[]{8};

        sections = new String[3][];
        sections[0] = sector1;
        sections[1] = sector2;
        sections[2] = sector3;


    }

    public void start(String section) {
        this.direction = true;
        this.Macs = sections[Integer.valueOf(section)];
        n_b = new int[Macs.length];
        check = new boolean[Macs.length];
        for (int i = 0; i < n_b.length; i++) {
            n_b[i] = 0;
            check[i] = false;
        }
        this.section = section;

        rssi_cali = new int[collect_num];
        for (int i = 0; i < collect_num; i++)
            rssi_cali[i] = 0;
        num_of_cali = 0;
        cali_median = 0;

        scan = true;
        interval_count = 0;



        for (int i = 0; i < location_queue.length; i++)
            location_queue[i] = -1;

        send_num = 0;

        scan();
        Log.e("TAG", "시작");
    }

    public int getsection(){
        return Integer.valueOf(section);
    }

    //현재 있는 section에서 몇번째 그림인지 반환
    public int num_pic(){
        return index_return(p_loc[Integer.valueOf(section)], currentSector);
    }

    public boolean getchangable(){
        return changable;
    }
    public void set_changable(boolean tf){
        Log.e("BLE", "Changable: " + String.valueOf(tf));
        this.changable = tf;
    }

    public void stop() {
        stopscan();
        for (int i = 0; i < n_b.length; i++) {
            n_b[i] = 0;
            check[i] = false;
        }
    }


    private void scan() {
        Log.e("BLE", "Scan Button");
        adapter.startLeScan(scancallback);
    }


    private void stopscan() {

        adapter.stopLeScan(scancallback);
    }

    private BluetoothAdapter.LeScanCallback scancallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] bytes) {
            //Log.e("BLE", "Scan call");


                    String macAdd = device.getAddress();


                    if (in_pic) {
                        //모든 비콘 데이터들이 수집되었으면 서버로 전송
                        if (check_rssi(check)) {
                            for(int i =0; i<n_b.length; i++){
                                n_b[i] = 0;
                                check[i] = false;
                            }
                            send(rssi_value);
                            send_num++;
                            //Log.e("comm", "send");
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

                        //보정용 비콘 수집
                        if (macAdd.equals(cali)) {
                            if (num_of_cali == collect_num) {
                                cali_median = Integer.valueOf(median_rssi(rssi_cali));
                            } else {
                                rssi_cali[num_of_cali % collect_num] = rssi;
                                num_of_cali += 1;
                            }
                        }
                    }
                    interval_count++;



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
        //Log.e("TAG", "전송");
        String r1 = "0";
        String r2 = "0";
        String r3 = "0";
        String r4 = "0";
        String r5 = "0";
        String r6 = "0";
        Call<String> call = null;

        if (section == "0"){
            section1 service = retrofit.create(section1.class);
            r1 = median_rssi(rssi_value[0]);
            r2 = median_rssi(rssi_value[1]);
            r3 = median_rssi(rssi_value[2]);
            r4 = median_rssi(rssi_value[3]);
            r5 = median_rssi(rssi_value[4]);
            r6 = median_rssi(rssi_value[5]);
            call = service.getMember(section, r1, r2, r3, r4, r5, r6, String.valueOf(send_num));
        }

        else if (section == "1"){
            section2 service = retrofit.create(section2.class);
            r1 = median_rssi(rssi_value[0]);
            r2 = median_rssi(rssi_value[1]);
            r3 = median_rssi(rssi_value[2]);
            r4 = median_rssi(rssi_value[3]);
            r5 = median_rssi(rssi_value[4]);
            r6 = "x";
            call = service.getMember(section, r1, r2, r3, r4, r5, r6);
        }
        else if (section == "2"){
            section3 service = retrofit.create(section3.class);
            r1 = median_rssi(rssi_value[0]);
            r2 = median_rssi(rssi_value[1]);
            r3 = median_rssi(rssi_value[2]);
            r4 = median_rssi(rssi_value[3]);
            r5 = median_rssi(rssi_value[4]);
            r6 = "x";
            call = service.getMember(section, r1, r2, r3, r4, r5, r6);
        }
        Log.e("Comm", "전송");

        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                //Log.e("TAG", "onResponse11: " + response.body());
                String response_ = response.body();
                if (response_ == "same"){
                    return;
                }
                int map_index = Integer.valueOf(response_);


                // 보고 있는 방향이 앞이라면 앞으로만 1칸씩만 이동 가능하게 설정
                //if(changable && direction) {
                if(true){
                    //location queue에 들어가 있는 내용들이 전부 동일한지 체크
                    location_queue[n_lq%location_queue.length] = map_index;
                    n_lq++;

                    if (map_index - currentSector <= secion_cali[Integer.valueOf(section)] && map_index - currentSector >= 0) {
                        if (map_index - currentSector == 2)
                            map_index -= 1;
                        location_queue[n_lq%location_queue.length] = map_index;
                        describe(map_index, true);
                        section_change_check();

                    }
                }


                //보고 있는 방향이 뒤라면 뒤로 1칸씩만 이동 가능하게 설정

                if(false) {

                    n_lq++;
                    if ( currentSector - map_index <= 1 && currentSector - map_index >= 0) {
                        if (currentSector - map_index== 2)
                            map_index += 1;
                        location_queue[n_lq%location_queue.length] = map_index;
                        describe(map_index, false);
                    }
                }

                if(check_lq(location_queue)){
                    map_index = location_queue[0];
                    describe(map_index, direction);
                    location_queue[0] = -1;
                }


            }
            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Log.e("TAG", "onFailure: " + t);
            }
        });

    }

    private void describe(int map_index, boolean direction){
        tv.setText(String.valueOf(map_index));
        // user_ori.sector_change(maps[map_index]);

        String [] drec = new String[2];
        if (direction == true){
            drec[0] = "오른쪽";
            drec[1] = "왼쪽";
        }
        else{
            drec[0] = "왼쪽";
            drec[1] = "오른쪽";
        }

        //그림이 있는 곳에서 없는 곳으로 이동 시
        //그림 설명 종료
        if (list_search(p_loc[Integer.valueOf(section)], currentSector)) {
            if (!list_search(p_loc[Integer.valueOf(section)], map_index)) {
                tts.speak("", TextToSpeech.QUEUE_FLUSH, null);
                //orientation 모듈에 설명중이 아니라고 세팅
                user_ori.set_explain(false, "");
                changable = true;
            }

        }

        //코너 일 때 설정
        if (list_search(corner[Integer.valueOf(section)], map_index)) {
            if (map_index != currentSector) {
                if (section == "1" && currentSector == 1){
                    tts.speak(drec[1] + "으로 돌아주세요.", TextToSpeech.QUEUE_FLUSH, null);
                }
                else {
                    tts.speak(drec[0] + "으로 돌아주세요.", TextToSpeech.QUEUE_FLUSH, null);
                }
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        try {
                            in_pic = false;
                            Thread.sleep(1000);
                            in_pic = true;
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }).start();
                user_ori.change_forward(90);
            }
        }

        //그림이 없는 곳에서 있는 곳으로 이동 시
        //그림 있다는 알림 및 그림 설명 실해
        if (list_search(p_loc[Integer.valueOf(section)], map_index)) {
            if (map_index != currentSector) {
                tts.speak("주위에 다음 작품이 있습니다", TextToSpeech.QUEUE_FLUSH, null);
                //user_ori.set_explain(true, p_name[index_return(p_loc[Integer.valueOf(section)], map_index)]);
                da.setExplaining(true);
                changable = false;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        try {
                            in_pic = false;
                            Thread.sleep(1000);
                            in_pic = true;
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }).start();

            }
        }

        if(list_search(last[Integer.valueOf(section)], map_index)){
            if(map_index != currentSector){
                tts.speak("해당 전시관이 끝났습니다", TextToSpeech.QUEUE_FLUSH, null);
                if (section == "0")
                    tts.speak("왼쪽으로 돌아 제 2관으로 이동해주세요", TextToSpeech.QUEUE_ADD, null);
                if(section == "1"){
                    if(currentSector == 4)
                        tts.speak("앞쪽의 제 3관으로 이동해주세요", TextToSpeech.QUEUE_ADD, null);
                    if(currentSector == 8)
                        tts.speak("모든 전시관 관람이 끝났습니다.", TextToSpeech.QUEUE_ADD, null);
                }
                if(section == "2"){
                    tts.speak("제 2관으로 이동해주세요", TextToSpeech.QUEUE_ADD, null);
                }
            }
        }
        currentSector = map_index;
    }
    private String median_rssi(int arr[]){
        Arrays.sort(arr);
        int med = arr[arr.length / 2];
        return String.valueOf(med);
    }
    private  int min_rssi(int arr[]){
        Arrays.sort(arr);
        return arr[0];
    }

    private boolean list_search(int[]arr, int value){
        for(int i =0; i<arr.length; i++){
            if (arr[i] == value){
                return true;
            }
        }
        return false;
    }

    private int index_return(int arr[], int value){
        for(int i =0; i<arr.length; i++){
            if (arr[i] == value){
                return i;
            }
        }
        return -1;
    }

    public void section_change_check(){
        if(section == "0" && currentSector == last[Integer.valueOf(section)][0]){
            start("1");
            currentSector = 0;
            tv.setText("0");
        }

        if(section == "1" && currentSector == last[Integer.valueOf(section)][0]){
            start("2");
            currentSector = 0;
            tv.setText("0");
        }
        else if (section == "1" && currentSector == last[Integer.valueOf(section)][1]){

        }

        if(section == "2" && currentSector == last[Integer.valueOf(section)][0]){
            start("1");
            currentSector = 6;
            tv.setText("5");
        }
    }

    public void set_direction(boolean tf){
        this.direction = tf;
    }

    public boolean check_lq(int[] arr){
        int s = arr[0];
        for (int i=0; i < location_queue.length; i++){
            if(s!=arr[i] || arr[i] == -1)
                return false;
        }
        return true;
    }

    public boolean check_rssi(boolean [] arr){
        for(int i = 0; i< arr.length; i++)
            if(arr[i] == false)
                return false;
        return true;
    }
}
