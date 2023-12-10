//package org.tensorflow.lite.examples.detection.Backup;
//
//
//import android.bluetooth.BluetoothAdapter;
//import android.bluetooth.BluetoothDevice;
//import android.bluetooth.le.BluetoothLeScanner;
//import android.speech.tts.TextToSpeech;
//import android.util.Log;
//
//import com.google.gson.Gson;
//import com.google.gson.GsonBuilder;
//
//import org.tensorflow.lite.examples.detection.navi.section1;
//import org.tensorflow.lite.examples.detection.navi.section2;
//import org.tensorflow.lite.examples.detection.navi.section3;
//import org.tensorflow.lite.examples.detection.navi.user_orientation;
//import org.tensorflow.lite.examples.detection.navi.work_information;
//import org.tensorflow.lite.examples.detection.storage.BLEStorage;
//import org.tensorflow.lite.examples.detection.storage.StoreManagement;
//
//import java.io.IOException;
//import java.util.Arrays;
//
//import retrofit2.Call;
//import retrofit2.Callback;
//import retrofit2.Response;
//import retrofit2.Retrofit;
//import retrofit2.converter.gson.GsonConverterFactory;
//import retrofit2.converter.scalars.ScalarsConverterFactory;
//
//
//public class Bluetooth_v1 {
//
//    private String[] sector1 = {"D8:3A:DD:1D:C4:45", "D8:3A:DD:1D:C5:EA", "D8:3A:DD:1D:C6:FA", "D8:3A:DD:1D:C6:9D", "D8:3A:DD:1D:C6:85", "D8:3A:DD:1D:C7:5A"};
//    private String[] sector2 = {"D8:3A:DD:1D:C4:90", "D8:3A:DD:1D:C2:B3", "D8:3A:DD:1D:C6:B5", "D8:3A:DD:1D:C6:1F", "D8:3A:DD:1D:C6:3A"};
//    private String[] sector3 = {"D8:3A:DD:1D:C6:48", "D8:3A:DD:1D:C7:09", "D8:3A:DD:1D:C7:00", "D8:3A:DD:1D:C6:AD", "D8:3A:DD:1D:C3:C1"};
//    private String[][] sections;
//
//    private Retrofit retrofit;
//    private BluetoothLeScanner scanner;
//    private BluetoothAdapter adapter;
//    private int[][] rssi_value;
//
//
//
//    // 구역에 있는 맥 주소
//    private String[] Macs;
//    private boolean[] check;
//    private int[] n_b;
//    private int[] rssi_cali;
//    //median filter에 사용할 rssi 값 개수
//    private int collect_num = 6;
//    //이전에 분류된 구역 번호
//    private double currentSector = 0;
//
//    //몇 번째 보내는 메시지인지 확인
//    private int send_num;
//
//    private user_orientation user_ori;
//    private String section;
//
//    //그림이 있는 map index 배열
//    int[] p_loc_1 = new int[]{0, 1, 4, 8, 10};
//    int[] corner_1 = new int[]{2, 7};
//    int[] route_1 = new int[]{0,1,2,3,4,5,6,7,8,9,10,11,12,13};
//
//    int[] p_loc_2 = new int[]{2, 6, 7};
//    int[] corner_2 = new int[]{1, 3, 8};
//    int[] route_2 = new int[]{0,1,2,3,4,5,6,7,8,9};
//
//    int[] p_loc_3 = new int[]{1, 3, 6};
//    int[] corner_3 = new int[]{2, 5, 7, 8, 9};
//    int[] route_3 = new int[]{0,1,2,3,4,5,6,7,8,9,10};
//
//    int[][] p_loc = new int[3][];
//    int[][] corner = new int[3][];
//    int[][] route = new int[3][];
//
//    //전시 구역이 끝나는 지점
//    int [][] last = new int[3][];
//
//    int [] secion_cali = new int[]{2,1,1};
//
//    private TextToSpeech tts;
//
//    private boolean changable;
//
//    //현재 사용자 방향 t: 정방향 f: 반대 방향
//    private boolean direction;
//
//
//    //location 정보가 들어가는 queue
//    //queue에 저장된 location들이 전부 일치하면 해당 location으로 변경
//    private int[] location_queue;
//    // location queue index 위치 n_lq % 30
//    private int n_lq;
//    private org.tensorflow.lite.examples.detection.navi.work_information wi;
//
//    //작품 설명중이면 T 아니면 F
//    private boolean explaining;
//    private BLEStorage[] ble_storage;
//    private StoreManagement store_m;
//    private int ble_storage_count;
//
//
//
//    public Bluetooth_v1(BluetoothAdapter adapter, user_orientation user_ori, TextToSpeech tts, StoreManagement store_m){
//        this.adapter = adapter;
//        scanner = adapter.getBluetoothLeScanner();
//        this.rssi_value = new int[6][collect_num];
//
//        this.user_ori = user_ori;
//        this.tts = tts;
//
//
//        changable = true;
//        //user_ori.set_ble(this);
//        location_queue = new int[7];
//
//        setRetrofitInit();
//        section = "0";
//
//        p_loc[0] = p_loc_1;
//        p_loc[1] = p_loc_2;
//        p_loc[2] = p_loc_3;
//
//        corner[0] = corner_1;
//        corner[1] = corner_2;
//        corner[2] = corner_3;
//
//        route[0] = route_1;
//        route[1] = route_2;
//        route[2] = route_3;
//
//        last[0] = new int[]{13};
//        last[1] = new int[]{5, 9};
//        last[2] = new int[]{10};
//
//        sections = new String[3][];
//        sections[0] = sector1;
//        sections[1] = sector2;
//        sections[2] = sector3;
//        wi = new work_information(3, 11);
//
//        //ble_storage = new BLEStorage[10000];
//
//        //this.store_m = store_m;
//
//        set_wi();
//    }
//
//    //작품 정보 입력
//    public void set_wi(){
//        wi.set_work(0,0, "", "김지은","\n" +
//                "김지은 작가의 개인전 <화성 and 지구에서>에 오신 것을 환영합니다.\n" +
//                "《화성 and 지구에서》는 제21회 우민미술상을 수상한 김지은 작가의 개인전입니다. 김지은 작가는 현대사회의 도시 개발 속에서 현대인의 삶의 공간이 고유한 장소성을 잃어버리고 획일화·상품화되고 있음에 주목합니다. 이번 전시는 김지은 작가가 2017년 경기도 화성시 봉담지구로 이주한 뒤, 서울과 화성을 오가거나 택지지구에서 생활하면서 낯선 풍경을 접하면서 시작한 작업을 소개합니다. 작가는 오늘의 주거환경을 어떤 시선으로 바라보고 있는지 만나보겠습니다. ");
//
//        wi.set_work(0,1, "화성 풍경: 가림막", "김지은", "작가가 화성 봉담지구로 이주한 후 마주한 병점역 앞 풍경입니다. 아스팔트 도로 좌우로 아름드리나무가 줄지어 있습니다. 유서 깊은 융건릉이 있던 이곳은 개발구역으로 지정된 뒤 철제 가림막이 세워졌고 사람이 발 딛고 걸을 수 없는 황량한 땅이 되었습니다. 작가는 지난 10년간 찍힌 로드뷰 사진 중 카메라 왜곡이 적고, 이곳의 풍경을 가장 아름다우면서도 황량하게 담은 사진을 골라 그림으로써 삶의 터전이 비장소화 되어가는 과정을 재현했습니다. ");
//        wi.set_work(0,2, "화성 정자와 화성 놀이터", "김지은","아파트 단지에서 흔히 볼 수 있는 육각 지붕의 정자와 놀이터입니다. 하지만 그 배경은 화성 탐사선이 보내준 황량한 화성 풍경으로 꽤나 낯선 조합입니다. 작가에게 택지개발지구의 놀이터와 정자는 특정 시간대에 아이들이 모이다가 갑자기 모두 다 사라지는 신비로운 곳처럼 느껴졌습니다. 택지개발자의 기능적인 배치가 만들어낸 낯선 풍경 그리고 이런 기능적인 사물/공간이 없다면 갈 곳이 없는 아이들, 이것이 오늘날 택지지구 아파트 단지의 풍경입니다.");
//        wi.set_work(0,3, "화성 풍경: 흙", "김지은","불도저가 한차례 쓸고 간 듯 마구 파헤쳐진 땅의 전경입니다. 공사현장에서 나온 쓰레기가 곳곳에 쌓여 있습니다. 작가는 공사현장을 다니며 도시 개발 이면의 현상에 주목했습니다. 도시 정비 과정에서 삶의 공간은 원래의 장소성을 잃고 방치되곤 합니다. 전시장 바닥에는 공사현장에 설치되는 도로 방호벽을 미니어처로 만든 종이 구조물이 놓여 있습니다. 일상 공간에서 개발이 진행될 때 펼쳐지는 비현실적인 풍경이 전시장에서 반복됩니다.");
//        wi.set_work(0,4, "주차장", "김지은","대형마트 주차장에 두 아이가 서 있습니다. 작가는 화성으로 이주한 뒤 미국에서나 보았던, 차로 가야 하는 빅박스 형태의 대형마트를 보게 되었습니다. 공공택지지구에서 이런 편의시설은 마치 전염병처럼 주변 부지의 개발을 이끌고 주민의 삶의 방식에 영향을 미칩니다. 작가는 본인에겐 낯선 택지지구의 삶이 아이들에게는 자연스러운 것으로 경험되리라 생각했습니다. 매 순간 자라나는 아이들의 모습과 어디나 동일한 교외 대형마트 주차장 풍경이 대비를 이룹니다.");
//
//        wi.set_work(1,0,"쇼룸 1", "김지은","모델하우스처럼 잘 꾸며진 침실입니다. 최신 유행을 따르면서도 무난한 스타일을 꾸며진 이 공간을 자세히 살펴보면 침구나 카펫, 가구에 이케아 카탈로그에서 수집한 실내 사진이 꼴라주되어 있음을 알 수 있습니다. 공간 너머엔 작가가 살고 있는 아파트 단지가 보입니다. 작가는 밀집된 대단지 아파트의 모습에서 바코드를 연상합니다. 언제나 시세를 확인할 수 있는 아파트에서의 삶에 대해 사적인 공간이 아닌, ‘가판대에 나와 있는 것 같다’라고 비유합니다. ");
//        wi.set_work(1,1,"아파트 컬러 트렌드 VS 르 꼬르뷔지에", "김지은","아파트 컬러에도 트렌드가 있다는 것을 아시나요? 작품의 앞쪽 면에는 요즘 아파트의 컬러 트렌드를 보여주는 파랑, 회색, 갈색 계열 시트지가 붙어 있고, 뒤쪽 면은 건축가 르코르뷔지에가 고안한 20세기 최초의 아파트 ‘유니테 다비타시옹’의 색 구성을 닮았습니다. 르코르뷔지에는 도시 주변부로 밀려난 사람을 위해 아파트를 고안했습니다. 하지만 오늘날 아파트는 그의 의도와는 달리 효율에 따라 수직, 수평의 표준화된 주거공간을 양산하고 있습니다. ");
//        wi.set_work(1,2,"그린 벨트", "김지은","서울 지도에 형형색색 폼폼이가 빽빽하게 붙어 있고 그 주위에는 그린벨트가 조경재료로 꾸며져 있습니다. 폼폼이가 펑! 튀겨나간 듯 서울 주변부로 퍼져 있습니다. 지도 속 알갱이처럼 오늘날 서울에는 절대다수의 인구가 거주하고 있습니다. 작가는 밀집된 색색의 폼폼이들이 폭발해서 그린벨트 바깥으로 터져 나온 듯한 양상을 통해 서울로 과도하게 집중된 사람들의 주거 욕망을 드러냅니다. 그린벨트가 해제되면 수도권의 주거 분포는 어떻게 달라질까요?");
//
//        wi.set_work(2,0,"재활용 수거일", "김지은","종이박스로 된 거대한 산이 척박한 화성 풍경 위에 그려져 있습니다. 작가가 사는 아파트 단지는 매주 재활용 수거일마다 쓰레기를 경비의 지도에 따라 일사 정연하게 분리수거한 뒤 쌓아 올려 그림과 같이 정리합니다. 이런 방식은 대단지 아파트에서 막대한 소비로 인해 생기는 쓰레기를 가장 효율적으로 처리하는 방법일 것입니다. 작가는 작품에 실제 상품라벨이 적힌 종이박스를 꼴라주하고 재활용 수거일 풍경을 화성을 배경으로 그림으로써 일상 풍경을 낯설고도 기념비적으로 재현합니다.");
//        wi.set_work(2,1,"옆집", "김지은","작가의 집 베란다 창을 열면 보이는 옆라인 아파트 벽면을 그린 작품입니다. 무난한 색으로 도색된 옆집 벽면은 시간대별 빛의 변화에 따라 달리 보입니다. 작은 사각 모듈로 구성된 옆집 이미지는 아파트 단지를 상공에서 촬영한 구글 이미지를 닮았습니다. 우리 아파트 옆집 벽이지만 어디에서 보아도 만날 수 있을 것 같은, 특유의 구조적이며 질서정연한 모습입니다. 작품은 택지지구 개발 이후에도 여전히 삭막한 현대인의 주거환경을 보여줍니다. ");
//        wi.set_work(2,2,"화성풍경: 모델하우스", "김지은","봉담 택지지구 인근에 있던 모델하우스의 철거 장면을 담은 작품입니다. 아파트 분양을 위해 지어지는 모델하우스는 아직 존재하지 않는 아파트를 실제 공간처럼 완벽하게 재현하여 구매자의 거주 욕망을 가상으로 실현해줍니다. 하지만 그 기능을 다 하고 나면 일시에 철거되어 폐허가 됩니다. 작가는 부동산 광고나 대출 전단지, 인테리어 시트지를 꼴라주하여 모델하우스 철거 풍경을 재현함으로써 아파트라는 공간의 일시성과 가상성을 드러냅니다.");
//    }
//
//    public void start(String section) {
//        this.direction = true;
//        this.Macs = sections[Integer.valueOf(section)];
//        n_b = new int[Macs.length];
//        check = new boolean[Macs.length];
//        for (int i = 0; i < n_b.length; i++) {
//            n_b[i] = 0;
//            check[i] = false;
//        }
//        this.section = section;
//
//        rssi_cali = new int[collect_num];
//        for (int i = 0; i < collect_num; i++)
//            rssi_cali[i] = 0;
//
//
//
//        for (int i = 0; i < location_queue.length; i++)
//            location_queue[i] = -1;
//
//        send_num = 0;
//
//        //store_m.reset_ble(ble_storage);
//        //ble_storage_count = 0;
//
//        scan();
//        Log.e("BLE", "시작");
//    }
//
//    public int getsection(){
//        return Integer.valueOf(section);
//    }
//    public double get_sector() {return currentSector;}
//
//    //현재 있는 section에서 몇번째 그림인지 반환
//    public int num_pic(){
//        return index_return(p_loc[Integer.valueOf(section)], currentSector);
//    }
//
//    public boolean getchangable(){
//        return changable;
//    }
//
//    public void stop() throws IOException {
//        stopscan();
//        for (int i = 0; i < n_b.length; i++) {
//            n_b[i] = 0;
//            check[i] = false;
//        }
//        //store_m.ble_store(ble_storage);
//    }
//
//
//    public void scan() {
//        Log.e("BLE", "Scan Button");
//        adapter.startLeScan(scancallback);
//    }
//
//
//    private void stopscan() {
//
//        adapter.stopLeScan(scancallback);
//    }
//
//    private BluetoothAdapter.LeScanCallback scancallback = new BluetoothAdapter.LeScanCallback() {
//        @Override
//        public void onLeScan(BluetoothDevice device, int rssi, byte[] bytes) {
//                    String macAdd = device.getAddress();
//
//                    if (true) {
//                        //모든 비콘 데이터들이 수집되었으면 서버로 전송
//                        if (check_rssi(check)) {
//                            for(int i =0; i<n_b.length; i++){
//                                n_b[i] = 0;
//                                check[i] = false;
//                            }
//                            //tts.speak("데이터 전송", TextToSpeech.QUEUE_FLUSH, null);
//                            send(rssi_value);
//                            send_num++;
//                            //Log.e("comm", "send");
//                        }
//
//                        //각 구역별 배치된 비콘 RSSI 수집
//                        for (int i = 0; i < Macs.length; i++) {
//                            if (macAdd.equals(Macs[i]) && !check[i]) {
//                                if (n_b[i] != collect_num) {
//                                    //Log.e("ble",String.valueOf(i) + ": " + String.valueOf(n_b[i]));
//                                    rssi_value[i][n_b[i]] = -rssi;
//                                    n_b[i]++;
//                                } else
//                                    check[i] = true;
//                            }
//                        }
//
//                    }
//        }
//    };
//
//    private void setRetrofitInit () {
//        Gson gson = new GsonBuilder().setLenient().create();
//        retrofit = new Retrofit.Builder()
//                .baseUrl("http://192.168.1.2:7777/")
//                .addConverterFactory(ScalarsConverterFactory.create())
//                .addConverterFactory(GsonConverterFactory.create(gson))
//                .build();
//    }
//
//    private void send (int[][] rssi_value) {
//        Log.e("comm", "전송");
//        String r1 = "0";
//        String r2 = "0";
//        String r3 = "0";
//        String r4 = "0";
//        String r5 = "0";
//        String r6 = "0";
//        Call<String> call = null;
//
//        if (section == "0"){
//            section1 service = retrofit.create(section1.class);
//            r1 = median_rssi(rssi_value[0]);
//            r2 = median_rssi(rssi_value[1]);
//            r3 = median_rssi(rssi_value[2]);
//            r4 = median_rssi(rssi_value[3]);
//            r5 = median_rssi(rssi_value[4]);
//            r6 = median_rssi(rssi_value[5]);
//            call = service.getMember(section, r1, r2, r3, r4, r5, r6, String.valueOf(send_num));
//        }
//
//        else if (section == "1"){
//            section2 service = retrofit.create(section2.class);
//            r1 = median_rssi(rssi_value[0]);
//            r2 = median_rssi(rssi_value[1]);
//            r3 = median_rssi(rssi_value[2]);
//            r4 = median_rssi(rssi_value[3]);
//            r5 = median_rssi(rssi_value[4]);
//            r6 = "x";
//            call = service.getMember(section, r1, r2, r3, r4, r5, r6);
//        }
//        else if (section == "2"){
//            section3 service = retrofit.create(section3.class);
//            r1 = median_rssi(rssi_value[0]);
//            r2 = median_rssi(rssi_value[1]);
//            r3 = median_rssi(rssi_value[2]);
//            r4 = median_rssi(rssi_value[3]);
//            r5 = median_rssi(rssi_value[4]);
//            r6 = "x";
//            call = service.getMember(section, r1, r2, r3, r4, r5, r6);
//        }
//        Log.e("Comm", "전송");
//
//        //int[] send_data = new int[]{Integer.valueOf(r1), Integer.valueOf(r2), Integer.valueOf(r3),Integer.valueOf(r4),Integer.valueOf(r5),Integer.valueOf(r6),};
//
//        call.enqueue(new Callback<String>() {
//            @Override
//            public void onResponse(Call<String> call, Response<String> response) {
//                //Log.e("TAG", "onResponse11: " + response.body());
//                String response_ = response.body();
//                if (response_ == "same"){
//                    return;
//                }
//                int map_index = Integer.valueOf(response_);
//
//                //long time = System.currentTimeMillis();
//                //BLEStorage ble_data = new BLEStorage();
//                //ble_data.set_values(time, section, response_, send_data);
//
//
//                if(section=="2" && currentSector >=8){
//                    if(map_index == 2)
//                        map_index = 9;
//                    else if(map_index == 0)
//                        map_index = 10;
//                }
//                else if(section == "1" && currentSector == 8){
//                    if(map_index == 1)
//                        map_index =9;
//                }
//
//                // 보고 있는 방향이 앞이라면 앞으로만 1칸씩만 이동 가능하게 설정
//                // 작품 설명 중이 아니여야 응답 인식 (explaining == false)
//                //if(changable && direction) {
//                Log.e("ble", String.valueOf(changable) + " / " + String.valueOf(explaining));
//                if(true && changable&&!explaining){
//                    //location queue에 들어가 있는 내용들이 전부 동일한지 체크
//                    location_queue[n_lq%location_queue.length] = map_index;
//                    n_lq++;
//
//                    if (check_route(currentSector, map_index)) {
//                        if (map_index - currentSector == 2)
//                            map_index -= 1;
//                        location_queue[n_lq%location_queue.length] = map_index;
//                        describe(map_index, true);
//                        section_change_check();
//
//                    }
//                }
//
//
//                //보고 있는 방향이 뒤라면 뒤로 1칸씩만 이동 가능하게 설정
//
//                if(false) {
//
//                    n_lq++;
//                    if ( currentSector - map_index <= 1 && currentSector - map_index >= 0) {
//                        if (currentSector - map_index== 2)
//                            map_index += 1;
//                        location_queue[n_lq%location_queue.length] = map_index;
//                        describe(map_index, false);
//                    }
//                }
//
//                if(check_lq(location_queue) && changable && false){
//                    map_index = location_queue[0];
//                    describe(map_index, direction);
//                    location_queue[0] = -1;
//                }
//
//
//            }
//            @Override
//            public void onFailure(Call<String> call, Throwable t) {
//                Log.e("TAG", "onFailure: " + t);
//            }
//        });
//
//    }
//
//    private void describe(int map_index, boolean direction){
//        // user_ori.sector_change(maps[map_index]);
//
//        String [] drec = new String[2];
//        double[] cali = new double[2];
//        if (direction == true){
//            drec[0] = "3시 방향";
//            drec[1] = "9시 방향";
//            cali[0] = 90;
//            cali[1] = -90;
//        }
//
//        //그림이 있는 곳에서 없는 곳으로 이동 시
//        //그림 설명 종료
//        if (list_search(p_loc[Integer.valueOf(section)], currentSector)) {
//            if (!list_search(p_loc[Integer.valueOf(section)], map_index)) {
//                user_ori.set_explain(false, "");
//            }
//
//        }
//
//        //코너 일 때 설정
//        if (list_search(corner[Integer.valueOf(section)], map_index)) {
//            if (map_index != currentSector) {
//                if ((section == "1" && map_index == 1)){
//                    tts.speak("막다른 길 입니다. "+drec[1] + "으로 돌아서 이동해주세요.", TextToSpeech.QUEUE_FLUSH, null);
//                    user_ori.change_cali_AtC(cali[1]);
//                }
//                else if (section == "2" && map_index == 8){
//                    tts.speak("막다른 길 입니다. "+ "1시 방향으로 이동해주세요.", TextToSpeech.QUEUE_FLUSH, null);
//                }
//                else if (section == "2" && map_index == 9){
//                    tts.speak(drec[1] + "으로 이동해주세요.", TextToSpeech.QUEUE_FLUSH, null);
//                }
//                else {
//                    tts.speak("막다른 길 입니다. "+drec[0] + "으로 돌아서 이동해주세요.", TextToSpeech.QUEUE_FLUSH, null);
//                    user_ori.change_cali_AtC(cali[0]);
//                }
//
//            }
//        }
//
//        //그림이 없는 곳에서 있는 곳으로 이동 시
//        //그림 있다는 알림 및 그림 설명 실해
//        if (list_search(p_loc[Integer.valueOf(section)], map_index)) {
//            if (map_index != currentSector) {
//                if(section == "0" && map_index == 0){
//                    tts.speak(wi.get_work(0,0)[2], TextToSpeech.QUEUE_ADD, null);
//                }
//                else {
//                    String name = wi.get_work(Integer.valueOf(section), index_return(p_loc[Integer.valueOf(section)], map_index))[0];
//                    String artist = wi.get_work(Integer.valueOf(section), index_return(p_loc[Integer.valueOf(section)], map_index))[1];
//                    tts.speak("잠시 멈춰주세요. " + artist + " 작가님의 작품 중 하나인 " + name + "이 좌측에 있습니다.", TextToSpeech.QUEUE_FLUSH, null);
//                    if((section == "0" && map_index == 10) || (section=="1" && map_index == 8)) {
//                        tts.speak("상세 설명을 들으시려면 몸을 좌우로 살짝 흔들어주세요.", TextToSpeech.QUEUE_ADD, null);
//                        user_ori.change_cali_AtC(cali[0]);
//                    }
//                    else {
//                        tts.speak("상세 설명을 들으시려면 " + drec[1] + "으로 돌아주세요.", TextToSpeech.QUEUE_ADD, null);
//                        tts.speak("설명이 나오지 않는다면 몸을 살짞 흔들어 주세요", TextToSpeech.QUEUE_ADD, null);
//                    }
//                    new Thread(new Runnable() {
//                        @Override
//                        public void run() {
//                            // TODO Auto-generated method stub
//                            // p_location time
//                            try {
//                                changable = false;
//                                Thread.sleep(15000);
//                                changable = true;
//                            } catch (InterruptedException e) {
//                                throw new RuntimeException(e);
//                            }
//                        }
//                    }).start();
//                }
//
//
//            }
//        }
//
//        if(list_search(last[Integer.valueOf(section)], map_index)){
//            if(map_index != currentSector){
//                if (section == "0") {
//                    tts.speak("해당 전시관이 끝났습니다", TextToSpeech.QUEUE_FLUSH, null);
//                    tts.speak("9시 방향으로 돌아 제 2관으로 이동해주세요", TextToSpeech.QUEUE_ADD, null);
//                    user_ori.change_cali_AtC(cali[1]);
//                }
//                if(section == "1"){
//                    if(currentSector == 4){
//                        tts.speak("앞으로 계속해서 이동해주세요.",TextToSpeech.QUEUE_FLUSH,null);
//                    }
//                        //tts.speak("앞쪽의 제 3관으로 이동해주세요", TextToSpeech.QUEUE_ADD, null);
//                    if(map_index == 9)
//                        tts.speak("지금까지 제21회 우민미술상 수상작가 김지은 개인전《화성N지구에서》를 관람하였습니다. 작가에게 화성시 봉담지구에서의 일상 풍경은 때로는 지구가 아닌 행성 마르스의 풍경처럼 낯설게 다가왔습니다. 지금도 현대사회의 도시 공간은 반복해서 개발되며 고유한 장소성을 잃어가고 있습니다. 여러분은 오늘의 도시를, 주거공간을 어떻게 바라보고 계신가요? 이번 전시가 화성N지구의 이야기에서 여러분의 N지구에 대한 사유로 이어지길 기대합니다. 감사합니다.", TextToSpeech.QUEUE_FLUSH, null);
//                }
//                if(section == "2") {
//                    new Thread(new Runnable() {
//                        @Override
//                        public void run() {
//                            //corner time
//                            // TODO Auto-generated method stub
//                            try {
//                                Thread.sleep(2000);
//                                tts.speak("11시 방향으로 이동해주세요.", TextToSpeech.QUEUE_ADD, null);
//                                user_ori.change_cali_AtC(180);
//                            } catch (InterruptedException e) {
//                                throw new RuntimeException(e);
//                            }
//                        }
//                    }).start();
//
//                }
//            }
//        }
//        currentSector = map_index;
//    }
//    private String median_rssi(int arr[]){
//        Arrays.sort(arr);
//        int med = arr[arr.length / 2];
//        return String.valueOf(med);
//    }
//    private boolean list_search(int[]arr, double value){
//        for(int i =0; i<arr.length; i++){
//            if (arr[i] == value){
//                return true;
//            }
//        }
//        return false;
//    }
//
//    private int index_return(int arr[], double value){
//        for(int i =0; i<arr.length; i++){
//            if (arr[i] == value){
//                return i;
//            }
//        }
//        return -1;
//    }
//
//    public void section_change_check(){
//        if(section == "0" && currentSector == last[Integer.valueOf(section)][0]){
//            start("1");
//            currentSector = 0;
//        }
//
//        if(section == "1" && currentSector == last[Integer.valueOf(section)][0]){
//            start("2");
//            currentSector = 0;
//        }
//        else if (section == "1" && currentSector == last[Integer.valueOf(section)][1]){
//
//        }
//
//        if(section == "2" && currentSector == last[Integer.valueOf(section)][0]){
//            start("1");
//            currentSector = 5.5;
//        }
//    }
//
//    public void speak_wi(){
//        tts.speak(wi.get_work(Integer.valueOf(section), index_return(p_loc[Integer.valueOf(section)], currentSector))[0] + "은" , TextToSpeech.QUEUE_ADD, null);
//        tts.speak(wi.get_work(Integer.valueOf(section), index_return(p_loc[Integer.valueOf(section)], currentSector))[2], TextToSpeech.QUEUE_ADD, null);
//        tts.speak("작품 설명이 끝났습니다. 3시 방향으로 돌아서 이동해주세요.", TextToSpeech.QUEUE_ADD, null);
//    }
//
//    public boolean p_location(){
//        if(section == "0" && currentSector == 0) return false;
//        return list_search(p_loc[Integer.valueOf(section)], currentSector);
//    }
//    public boolean corner_location(){
//        return list_search(corner[Integer.valueOf(section)], currentSector);
//    }
//
//    public void set_changable(boolean tf){
//        this.changable = tf;
//    }
//    public void set_direction(boolean tf){
//        this.direction = tf;
//    }
//    public void set_explaining(boolean tf){ this.explaining = tf;}
//
//    public boolean check_lq(int[] arr){
//        int s = arr[0];
//
//        int count = 0;
//        for (int i=0; i < location_queue.length; i++){
//            if(s==arr[i] || arr[i] != -1){
//                count++;
//            }
//
//        }
//        if(list_search(p_loc[Integer.valueOf(section)], arr[0]) && count >= 4)
//            return true;
//        return false;
//    }
//
//    public boolean check_route(double current, int index){
//        int[] rt = route[Integer.valueOf(section)];
//        if((index - current <= secion_cali[Integer.valueOf(section)] && index - current >= 0) || (section == "0" && index == 13 && current > 10)){
//            return true;
//        }
//        return  false;
//    }
//
//    public boolean check_rssi(boolean [] arr){
//        for(int i = 0; i< arr.length; i++)
//            if(arr[i] == false)
//                return false;
//        return true;
//    }
//
//}
//
