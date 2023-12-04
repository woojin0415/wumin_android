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
    private BluetoothAdapter adapter;
    private int[][] rssi_value;



    // 구역에 있는 맥 주소
    private String[] Macs;
    private boolean[] check;
    private int[] n_b;
    private int[] rssi_cali;
    //median filter에 사용할 rssi 값 개수
    private int collect_num = 6;
    //이전에 분류된 구역 번호
    private int currentSector = -1;

    //몇 번째 보내는 메시지인지 확인
    private int send_num;

    private user_orientation user_ori;
    private String section;

    //그림이 있는 map index 배열
    int[] p_loc_1 = new int[]{0, 1, 4, 8, 10};
    int[] corner_1 = new int[]{2, 7};
    int[] route_1 = new int[]{0,1,2,3,4,5,6,7,8,9,10,11,12,13};

    int[] p_loc_2 = new int[]{2, 7, 8};
    int[] corner_2 = new int[]{1, 3};
    int[] route_2 = new int[]{0,1,2,3,4,5,6,7,8,9};

    int[] p_loc_3 = new int[]{1, 3, 6};
    int[] corner_3 = new int[]{2, 5, 7, 8, 9, 10};
    int[] route_3 = new int[]{0,1,2,3,4,5,6,7,8,9,10};

    int[][] p_loc = new int[3][];
    int[][] corner = new int[3][];
    int[][] route = new int[3][];

    //전시 구역이 끝나는 지점
    int [][] last = new int[3][];

    int [] secion_cali = new int[]{2,1,2};

    private TextToSpeech tts;

    private boolean changable;

    //현재 사용자 방향 t: 정방향 f: 반대 방향
    private boolean direction;


    //location 정보가 들어가는 queue
    //queue에 저장된 location들이 전부 일치하면 해당 location으로 변경
    private int[] location_queue;
    // location queue index 위치 n_lq % 30
    private int n_lq;
    private work_information wi;

    //작품 설명중이면 T 아니면 F
    private boolean explaining;



    public Bluetooth(BluetoothAdapter adapter, user_orientation user_ori, TextToSpeech tts){
        this.adapter = adapter;
        scanner = adapter.getBluetoothLeScanner();
        this.rssi_value = new int[6][collect_num];

        this.user_ori = user_ori;
        this.tts = tts;


        changable = true;
        user_ori.set_ble(this);
        location_queue = new int[5];

        setRetrofitInit();
        section = "0";

        p_loc[0] = p_loc_1;
        p_loc[1] = p_loc_2;
        p_loc[2] = p_loc_3;

        corner[0] = corner_1;
        corner[1] = corner_2;
        corner[2] = corner_3;

        route[0] = route_1;
        route[1] = route_2;
        route[2] = route_3;

        last[0] = new int[]{13};
        last[1] = new int[]{5, 9};
        last[2] = new int[]{10};

        sections = new String[3][];
        sections[0] = sector1;
        sections[1] = sector2;
        sections[2] = sector3;
        wi = new work_information(3, 11);

        set_wi();
    }

    //작품 정보 입력
    public void set_wi(){
        wi.set_work(0,0, "", "김지은","\n" +
                "김지은 작가의 개인전 <화성 N 지구에서>에 오신 것을 환영합니다.\n" +
                "\n" +
                "이번 전시는 제21회 우민미술상 수상자 김지은 작가의 개인전입니다.\n" +
                "\n" +
                "여러분은 ‘화성’이란 말을 들으면 어떤 화성이 연상되나요? 이번 전시는 제 21회 우민미술상을 수상한 김지은 작가님이 2017년부터 거주하게 된 화성시 봉담택지지구에서의 삶을 비장소성의 맥락으로 분석한 뒤에 이를 예술로 표현한 작품들을 전시하고 있습니다. 여기서 비장소성은 어디에도 정착하지 않고 잠시 머물거나, 그 공간에 대한 관계와 정체성을 갖지 못하는 것을 의미합니다. \n" +
                "\n" +
                "김지은 작가는 이런 비장소성을 작품 소재로 다룸으로서 현대사회 속에 숨겨진 사회적 법규나 제도들을 드러내는 동시에, 이로 인해 소외받은 현대인들의 이면을 작품으로 표현합니다. \n" +
                "회화, 콜라주 그리고 설치미술과 같이 다양한 매체를 넘나들며 작품세계를 표현해온 김지은 작가님의 작품들을 만나봅시다.");

        wi.set_work(0,1, "화성 풍경: 가림막", "김지은", "<화성 풍경-가림막> 은 작가가 실제로 화성의 봉담택지지구로 이주한 후에 본 병점역 앞의 풍경을 담고 있습니다. 작가가 작업을 구상하던 당시 병점역 앞은 개발구역으로 지정되어 가림막으로 둘러싸인 황량한 땅이었습니다. 지금은 걸을 수 없는 땅이 되었지만, 주위의 느티나무들과 근처에 있는 용주사와 융건릉은 한때 이곳이 장소성을 지닌 곳이라는 것을 보여줍니다. \n" +
                "\n" +
                "작가는 이 길을 촬영한 10년에 걸친 거리뷰와 로드뷰에서 카메라 왜곡이 적은 동시에 가장 아름다우면서도 황량한 풍경을 골라 회화로 재현했습니다. 거리뷰 촬영 차량 위에서 발을 땅에 붙이지 않은 시점으로 그린 이 작품을 통해 작가는 장소가 비장소화 되어가는 과정을 적나라하게 보여주고 있습니다.");
        wi.set_work(0,2, "화성 놀이터", "김지은","아파트 안에 있는 놀이터와 정자는 특정시간대에 아이들이 우글우글 모이다가 갑자기 모두 다 사라지는 신비한 일이 일어나는 곳입니다. 시간대에 따라 놀이터에 모이는 아이들의 연령대는 모두 다르고 택지 지구 안에서 아이들이 놀 만한 곳은 딱히 없어 보입니다. 오래전부터 마을 사람들이 모여 쉬는 곳이었던 정자는 사각, 육각, 팔각 형태도 다양합니다. 이국적인 놀이터와 한국적인 정자의 만남은 아무런 개연성도 없어보입니다.\n" +
                "\n" +
                "작가는 공공공간의 기능성을 대표하는 두 공간 또는 사물이 한 장소를 공유하는 장면을 두 개의 화면으로 나누어 화성이라는 공간에 배치했습니다. 어느 단지에서나 볼 수 있는 풍경이기에 익숙하지만 한 걸음만 떨어져서 보면 참 이상한 풍경입니다. 기능적인 배치가 만들어낸 낯선 풍경 그리고 이러한 기능적인 사물/공간이 없다면 갈 곳이 없는 아이들, 이것이 오늘날 택지지구 아파트 단지 안의 풍경입니다. ");
        wi.set_work(0,3, "화성 풍경: 흙", "김지은","<화성 풍경-흙>은 앞서 보았던 <화성 풍경-가림막>과 대칭되는 작품입니다. 거대하게 쌓여진 흙 아래로 파란색 캔들과 공사현장에서 나온 쓰레기들이 그림 아래를 가득 채우고 있습니다. 번듯하게 완성된 신도시의 풍경 가림막 뒤에는 이렇든 개발 과정에서 생긴 삶의 풍경이 남아있습니다. 작가는 현장을 다니며 개발과정에서 일어난 현상들에 주목하고 도시 이면의 모습을 그림으로 담아왔습니다. \n");
        wi.set_work(0,4, "주차장", "김지은","서버비아는 자동차 위주의 생활양식으로 주로 미국 교외지역에서 발달했습니다. 작가는 유학시절 디트로이드에서 보았던 서버비아 형태의 대형마트를 화성에서도 보게 되었습니다. 서울의 대형마트들은 대중교통을 바로 이용할 수 있거나 지하주차장이 있는데, 이곳은 출퇴근용 차가 한 대 있고 동네를 다닐 때 쓰는 소형차가 세컨드카로 있는 경우가 많습니다. 작가 본인에게는 낯설었던 택지지구의 삶이 아이들에게는 자연스러운 풍경이 되어 새로운 장소성을 경험하리라고 작가는 생각했습니다. 매 순간마다 변화하는 아이들의 모습과 어디서나 동일한 모습을 보이는 주차장의 풍경은 서로 대비되어 보입니다. \n");

        wi.set_work(1,0,"쇼룸 1", "김지은","10번 작품 설명");
        wi.set_work(1,1,"그린 벨트", "김지은","13번 작품 설명");
        wi.set_work(1,2,"중첩 규제 지도", "김지은","14번 작품 설명");

        wi.set_work(2,0,"재활용 수거일", "김지은","15번 작품 설명");
        wi.set_work(2,1,"옆집", "김지은","16번 작품 설명");
        wi.set_work(2,2,"화성풍경: 모델하우스", "김지은","17번 작품 설명");
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



        for (int i = 0; i < location_queue.length; i++)
            location_queue[i] = -1;

        send_num = 0;

        scan();
        Log.e("BLE", "시작");
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
                    String macAdd = device.getAddress();
                    Log.e("BLE", "Scan");

                    if (true) {
                        //모든 비콘 데이터들이 수집되었으면 서버로 전송
                        if (check_rssi(check)) {
                            for(int i =0; i<n_b.length; i++){
                                n_b[i] = 0;
                                check[i] = false;
                            }
                            //tts.speak("데이터 전송", TextToSpeech.QUEUE_FLUSH, null);
                            send(rssi_value);
                            send_num++;
                            //Log.e("comm", "send");
                        }

                        //각 구역별 배치된 비콘 RSSI 수집
                        for (int i = 0; i < Macs.length; i++) {
                            if (macAdd.equals(Macs[i]) && !check[i]) {
                                Log.e("BLE", "Scan call");
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
        Log.e("comm", "전송");
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


                if(section=="2" && currentSector >=8){
                    if(map_index == 1)
                        map_index = 9;
                    else if(map_index == 0)
                        map_index = 10;
                }
                else if(section == "1" && currentSector >=7){
                    if(map_index == 1)
                        map_index =9;
                }

                // 보고 있는 방향이 앞이라면 앞으로만 1칸씩만 이동 가능하게 설정
                // 작품 설명 중이 아니여야 응답 인식 (explaining == false)
                //if(changable && direction) {
                if(true && changable&&!explaining){
                    //location queue에 들어가 있는 내용들이 전부 동일한지 체크
                    location_queue[n_lq%location_queue.length] = map_index;
                    n_lq++;

                    if (check_route(currentSector, map_index)) {
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

                if(check_lq(location_queue) && changable){
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
        // user_ori.sector_change(maps[map_index]);

        String [] drec = new String[2];
        if (direction == true){
            drec[0] = "3시 방향";
            drec[1] = "9시 방향";
        }

        //그림이 있는 곳에서 없는 곳으로 이동 시
        //그림 설명 종료
        if (list_search(p_loc[Integer.valueOf(section)], currentSector)) {
            if (!list_search(p_loc[Integer.valueOf(section)], map_index)) {
                user_ori.set_explain(false, "");
            }

        }

        //코너 일 때 설정
        if (list_search(corner[Integer.valueOf(section)], map_index)) {
            if (map_index != currentSector) {
                if ((section == "1" && map_index == 1) || (section == "2" && map_index == 9)){
                    tts.speak("막다른 길 입니다. "+drec[1] + "으로 돌아서 이동해주세요.", TextToSpeech.QUEUE_FLUSH, null);
                }
                else if (section == "2" && map_index == 8){
                    tts.speak("막다른 길 입니다. "+ "1시 방향으로 이동해주세요.", TextToSpeech.QUEUE_FLUSH, null);
                }
                else if (section == "2" && map_index == 10){
                    tts.speak("9시 방향으로 이동해주세요.", TextToSpeech.QUEUE_FLUSH, null);
                }
                else {
                    tts.speak("막다른 길 입니다. "+drec[0] + "으로 돌아서 이동해주세요.", TextToSpeech.QUEUE_FLUSH, null);
                }
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        //corner time
                        // TODO Auto-generated method stub
                        try {
                            changable = false;
                            Thread.sleep(2000);
                            changable = true;
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }).start();
            }
        }

        //그림이 없는 곳에서 있는 곳으로 이동 시
        //그림 있다는 알림 및 그림 설명 실해
        if (list_search(p_loc[Integer.valueOf(section)], map_index)) {
            if (map_index != currentSector) {
                if(section == "0" && map_index == 0){
                    tts.speak(wi.get_work(0,0)[2], TextToSpeech.QUEUE_ADD, null);
                }
                else {
                    String name = wi.get_work(Integer.valueOf(section), index_return(p_loc[Integer.valueOf(section)], map_index))[0];
                    String artist = wi.get_work(Integer.valueOf(section), index_return(p_loc[Integer.valueOf(section)], map_index))[1];
                    tts.speak("잠시 멈춰주세요. " + artist + " 작가님의 작품 중 하나인 " + name + "이 있는 위치입니다.", TextToSpeech.QUEUE_FLUSH, null);
                    if((section == "0" && map_index == 10) || (section=="1" && map_index == 8)) {
                        tts.speak("상세 설명을 들으시려면 몸을 좌우로 살짝 흔들어주세요.", TextToSpeech.QUEUE_ADD, null);
                    }
                    else
                        tts.speak("상세 설명을 들으시려면 "+drec[1]+"으로 돌아주세요.", TextToSpeech.QUEUE_ADD, null);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            // TODO Auto-generated method stub
                            // p_location time
                            try {
                                changable = false;
                                Thread.sleep(15000);
                                changable = true;
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }).start();
                }


            }
        }

        if(list_search(last[Integer.valueOf(section)], map_index)){
            if(map_index != currentSector){
                tts.speak("해당 전시관이 끝났습니다", TextToSpeech.QUEUE_FLUSH, null);
                if (section == "0")
                    tts.speak("왼쪽으로 돌아 제 2관으로 이동해주세요", TextToSpeech.QUEUE_ADD, null);
                if(section == "1"){
                    if(currentSector == 4){
                        tts.speak("",TextToSpeech.QUEUE_FLUSH,null);
                    }
                        //tts.speak("앞쪽의 제 3관으로 이동해주세요", TextToSpeech.QUEUE_ADD, null);
                    if(currentSector == 9)
                        tts.speak("모든 전시관 관람이 끝났습니다.", TextToSpeech.QUEUE_ADD, null);
                }
                if(section == "2"){}
                    //tts.speak("",TextToSpeech.QUEUE_FLUSH, null);
            }
        }
        currentSector = map_index;
    }
    private String median_rssi(int arr[]){
        Arrays.sort(arr);
        int med = arr[arr.length / 2];
        return String.valueOf(med);
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
        }

        if(section == "1" && currentSector == last[Integer.valueOf(section)][0]){
            start("2");
            currentSector = 0;
        }
        else if (section == "1" && currentSector == last[Integer.valueOf(section)][1]){

        }

        if(section == "2" && currentSector == last[Integer.valueOf(section)][0]){
            start("1");
            currentSector = 6;
        }
    }

    public void speak_wi(){
        tts.speak(wi.get_work(Integer.valueOf(section), index_return(p_loc[Integer.valueOf(section)], currentSector))[2], TextToSpeech.QUEUE_ADD, null);
        tts.speak("작품 설명이 끝났습니다. 오른쪽으로 돌아서 이동해주세요.", TextToSpeech.QUEUE_ADD, null);
    }

    public boolean p_location(){
        return list_search(p_loc[Integer.valueOf(section)], currentSector);
    }

    public void set_direction(boolean tf){
        this.direction = tf;
    }
    public void set_explaining(boolean tf){ this.explaining = tf;}
    public void set_changable(boolean tf){
        if(!(section =="0" && currentSector <1))
            this.changable = tf;}

    public boolean check_lq(int[] arr){
        int s = arr[0];
        for (int i=0; i < location_queue.length; i++){
            if(s!=arr[i] || arr[i] == -1)
                return false;
        }
        return true;
    }

    public boolean check_route(int current, int index){
        int[] rt = route[Integer.valueOf(section)];
        if((index - current <= secion_cali[Integer.valueOf(section)] && index - current >= 0) || (section == "0" && index == 13 && current > 10)){
            return true;
        }
        return  false;
    }

    public boolean check_rssi(boolean [] arr){
        for(int i = 0; i< arr.length; i++)
            if(arr[i] == false)
                return false;
        return true;
    }

}
