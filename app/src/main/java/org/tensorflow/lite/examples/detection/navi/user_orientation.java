package org.tensorflow.lite.examples.detection.navi;


import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import org.tensorflow.lite.examples.detection.storage.OrientationStorage;
import org.tensorflow.lite.examples.detection.storage.StoreManagement;

import java.io.IOException;
import java.util.Arrays;

public class user_orientation implements SensorEventListener{
    private SensorManager sm;
    private Sensor gyro;
    private Sensor acc;
    private Sensor rotation;
    private float[] gyro_value = new float[3];
    private float[] acc_values = new float[3];
    private float[] rotation_values = new float[16];
    private float[] orientation_values = new float[3];
    private int ori_init_count = 0;
    private double[] ori_init_filter = new double[50];
    private double calib_ori;
    private boolean ori_time_interval;

    private int filter_length = 10;
    private float [] filter = new float[filter_length];
    private float [] moving_check_filter = new float[filter_length];
    private int count;
    private Bluetooth ble;
    private boolean time_interval;

    //유저가 작품 위치에 있는 지 확인
    private boolean explain;
    private OrientationStorage[] ori_storage;
    private int ori_storage_count;
    private StoreManagement store_m;

    private int []numofworks = new int[]{5,3,4};


    int section;
    int sector;

    int [][] map_1 = new int [11][16];
    double [][] directions_1;

    int [][] map_2 = new int [8][11];
    double [][] directions_2;

    int [][] map_3 = new int [9][11];
    double [][] directions_3;

    int [][]current_map;
    double [][] current_direction;

    //현재 가리키는 작품 번호
    int forward_work;
    boolean forward_set;
    boolean section_change;

    //작품 위치 도착 시 true / 아니면 false
    boolean ready_explain = true;

    int[][] explain_direction;
    int [] expain_d_1 = new int[]{0, 270, 0, 90, 180};
    int [] expain_d_2 = new int[]{180, 0, 90, 90, 180};
    int [] expain_d_3 = new int[]{0, 90, 180, 270};







    private TextToSpeech tts;
    public user_orientation(SensorManager sm, Sensor gyro, Sensor acc, Sensor rotation, TextToSpeech tts, StoreManagement store_m){
        this.sm = sm;
        this.gyro = gyro;
        this.acc = acc;
        this.rotation = rotation;
        this.tts =tts;
        this.explain = false;
        this.time_interval = true;
        this.count = 0;

        directions_1 = new double[numofworks[0]][numofworks[0]];
        directions_2 = new double[numofworks[1]][numofworks[1]];
        directions_3 = new double[numofworks[2]][numofworks[2]];

        explain_direction = new int[3][];
        explain_direction[0] = expain_d_1;
        explain_direction[1] = expain_d_2;
        explain_direction[2] = expain_d_3;

        calib_ori = 209;

        forward_work = -1;
        current_map = map_1;




//        for(int i = 0; i < filter_length; i++) {
//            filter[i] = 0;
//            moving_check_filter[i] = 0;
//        }

        section = 0;
        sector = 1;

        reset_map();
        map_construciton();
        direction_setting();

        current_direction = directions_1;

        //ori_storage = new OrientationStorage[10000];
        //this.store_m = store_m;
    }


    private void reset_map(){
        for(int x = 0; x < map_1.length; x++){
            for(int y=0; y<map_1[0].length; y++)
                map_1[x][y] = -1;
        }

        for(int x = 0; x < map_2.length; x++){
            for(int y=0; y<map_2[0].length; y++)
                map_2[x][y] = -1;
        }

        for(int x = 0; x < map_3.length; x++){
            for(int y=0; y<map_3[0].length; y++)
                map_3[x][y] = -1;
        }
    }

    //map에 작품 위치 설정
    private void map_construciton(){
        map_1[0][0] = 0;
        map_1[6][0] = 1;
        map_1[10][4] = 2;
        map_1[6][15] = 3;
        map_1[0][15] = 4;

        map_2[7][0] = 0;
        map_2[7][7] = 1;
        map_2[1][2] = 2;
        map_2[1][5] = 3;

        map_3[8][10] = 0;
        map_3[4][10] = 1;
        map_3[0][5] = 2;
        map_3[5][0] = 3;
    }
    private void direction_setting(){
        for(int i = 0; i <directions_1.length; i++){
            for(int j=0; j< directions_1.length; j++){
                directions_1[i][j] = direction(i,j, map_1);
            }
        }

        for(int i = 0; i <directions_2.length; i++){
            for(int j=0; j< directions_2.length; j++){
                directions_2[i][j] = direction(i,j, map_2);
            }
        }

        for(int i = 0; i <directions_3.length; i++){
            for(int j=0; j< directions_3.length; j++){
                directions_3[i][j] = direction(i,j, map_3);
            }
        }
    }

    //현재 위치에서 현재 구역의 작품들이 있는 방향 값 설정
    private int direction(int w1, int w2, int [][] map){
        //작품 1(w1)과 작품 2(w2)간의 방향
        int[] cord_1 = get_coordinate(w1, map);
        int[] cord_2 = get_coordinate(w2, map);
        int x,y;
        x = cord_2[0] - cord_1[0];
        y = cord_2[1] - cord_1[1];

        double radian = Math.atan2(y,x);
        double degree = radian * 180 / Math.PI;

        return (int)degree;
    }

    private int[] get_coordinate(int nwk , int[][] map){
        //section: 현재 구역
        //nwk: 좌료를 알고자하는 설명 번호
        int[] cord = new int[2];

        for(int x = 0; x < map.length; x++) {
            for (int y = 0; y < map[0].length; y++){
                if(map[x][y] == nwk) {
                    cord[0] = x;
                    cord[1] = y;
                }
            }
        }
        return cord;
    }

    public void start(){
        sm.registerListener((SensorEventListener) this, gyro, SensorManager.SENSOR_DELAY_UI);
        sm.registerListener((SensorEventListener) this, acc, SensorManager.SENSOR_DELAY_UI);
        sm.registerListener((SensorEventListener) this, rotation, SensorManager.SENSOR_DELAY_UI);
        forward_set = false;
        ori_time_interval = true;
        section_change = false;
        //ori_storage_count = 0;
        //ori_init_count = 0;
        //store_m.reset_ori(ori_storage);
    }

    public void stop() throws IOException {
        sm.unregisterListener((SensorEventListener) this, gyro);
        sm.unregisterListener((SensorEventListener) this, acc);
        sm.unregisterListener((SensorEventListener) this, rotation);
        //store_m.ori_store(ori_storage);
    }
    public void set_ble(Bluetooth ble){
        this.ble = ble;
    }

    //그림 설명 중인지 아닌지 세팅
    public void set_explain(boolean value, String p_name){
        this.explain = value;
        Log.e("ori", String.valueOf(explain));
        Log.e("ori", p_name);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {


        if(event.sensor == rotation && ori_time_interval) {
            SensorManager.getRotationMatrixFromVector(rotation_values, event.values);
            SensorManager.getOrientation(rotation_values, orientation_values);

            long time_ = System.currentTimeMillis();
            //String sector_ = String.valueOf(ble.getsection());
            double azimuth = (Math.toDegrees(orientation_values[0]) + 360) % 360;

            if (!(Math.abs(Math.toDegrees(orientation_values[1])) > 50)) {


                azimuth = azimuth - calib_ori;

                if (azimuth < 0)
                    azimuth = (azimuth + 360) % 360;
                //Log.e("azi", String.valueOf(azimuth));

                for (int i = 0; i < current_direction.length; i++) {

                    double diff = current_direction[sector][i] - azimuth;

                    //Log.e("diff", String.valueOf(diff));

                    if (diff < 0)
                        diff = (diff + 360) % 360;

                    if ((diff >= 350 || diff <= 10) && forward_work != i && !forward_set && sector != i) {
                        Log.e("ori", String.valueOf(diff) + " / " + String.valueOf(i) + " / " + String.valueOf(forward_work));
                        forward_work = i;
                        forward_set = true;
                        tts.speak("전방" + String.valueOf((int) distance(i, sector)) + "m 앞에 ", TextToSpeech.QUEUE_FLUSH, null);

                        if (i == 0)
                            tts.speak("전시관의 시작 지점이 있습니다.", TextToSpeech.QUEUE_ADD, null);
                        else
                            tts.speak(ble.get_wi(section, i) + "작품이 있습니다.", TextToSpeech.QUEUE_ADD, null);
                    } else if (!(diff >= 350 || diff <= 10) && forward_work == i && forward_set) {
                        tts.speak("", TextToSpeech.QUEUE_FLUSH, null);
                        forward_work = -1;
                        forward_set = false;
                    }
                }

                if (section == 0 && sector == 0 && azimuth <= 190 && azimuth >= 170 && !section_change) {
                    tts.speak("제 2 전시관으로 이동하실려면 앞으로 이동해주세요.", TextToSpeech.QUEUE_FLUSH, null);
                    section_change = true;
                } else if (section == 0 && sector == 0 && !(azimuth <= 190 && azimuth >= 170) && section_change) {
                    tts.speak("", TextToSpeech.QUEUE_FLUSH, null);
                    section_change = false;
                }

                if (section == 1 && sector == 0 && (azimuth <= 350 || azimuth >= 10) && !section_change) {
                    tts.speak("제 1 전시관으로 이동하실려면 앞으로 이동해주세요.", TextToSpeech.QUEUE_FLUSH, null);
                    section_change = true;
                } else if (section == 0 && sector == 0 && !(azimuth <= 350 || azimuth >= 10) && section_change) {
                    tts.speak("", TextToSpeech.QUEUE_FLUSH, null);
                    section_change = false;
                }

                if (ready_explain && explain_check(azimuth)) {
                    Log.e("work", "설명");
                    tts.speak("작품 설명을 시작합니다.", TextToSpeech.QUEUE_FLUSH, null);
                    ble.speak_wi(section, sector);
                    ready_explain = false;
                } else if (!ready_explain && !explain_check(azimuth)) {
                    tts.speak("", TextToSpeech.QUEUE_FLUSH, null);
                    ready_explain = true;
                }


                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            ori_time_interval = false;
                            Thread.sleep(500);
                            ori_time_interval = true;
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }).start();
            }
        }
    }



    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }


   double orientation_filter_median(double []arr){
        Arrays.sort(arr);
        return arr[ori_init_count/2];
   }

   boolean explain_check(double azimuth){
        double direc = explain_direction[section][sector];
        double d1 = direc - 25;
        double d2 = direc + 25;

        if(d1 < 0){
            d1 += 360;
            return (azimuth >= d1 || azimuth <= d2);
        }
        else{
            return (azimuth >= d1 && azimuth <= d2);
        }
   }

   double distance(int s1, int s2){
        int [] cord_1 = get_coordinate(s1, current_map);
        int [] cord_2 = get_coordinate(s2, current_map);
        double distance = Math.sqrt(Math.pow((cord_1[0] - cord_2[0]),2) + Math.pow(cord_1[1] - cord_2[1],2));

        return distance;
   }


   void set_section(int section){
        this.section = section;
   }
   void set_sector(int sector){
        this.sector = sector;
   }

}
