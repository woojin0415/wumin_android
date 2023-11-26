package org.tensorflow.lite.examples.detection.navi;


import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.ImageView;

import java.util.Arrays;

public class user_orientation implements SensorEventListener{
    private ImageView tv;
    private SensorManager sm;
    private Sensor acc;
    private Sensor mag;
    private Sensor grv;
    private float[] acc_values = new float[3];
    private float[] mag_values = new float[3];
    private boolean acc_set = false;
    private boolean mag_set = false;
    private int count = 0;
    private int filter_length = 29;
    private float [] filter = new float[filter_length];
    private boolean start = false;

    private int[] ori_queue = new int[5];
    private int ori_queue_count;

    //이전 방위각
    private float prive_ori;
    private Bluetooth ble;

    //유저가 작품 위치에 있는 지 확인
    private boolean explain;

    //보정 방위각
    private float correction_degree;

    private boolean init;
    private int init_num;
    private float[] init_array;

    //현재 구역에 있는 그림 정보
    private String p_name;
    //설명 1번만 진행하게 하는 변수 (0: 설명 함 / 그 외: 설명 안함)
    private int exp;


    private TextToSpeech tts;
    public user_orientation(SensorManager sm, Sensor acc, Sensor mag, TextToSpeech tts){
        this.sm = sm;
        this.acc = acc;
        this.mag = mag;
        this.tts =tts;
        prive_ori = 0;
        correction_degree = 0;
        this.explain = false;
        init = true;
        exp = 1;


        sm.registerListener((SensorEventListener) this, acc, SensorManager.SENSOR_DELAY_UI);
        sm.registerListener((SensorEventListener) this, mag, SensorManager.SENSOR_DELAY_UI);
        for(int i = 0; i < filter_length; i++)
            filter[i] = 0;



    }
    public void set_ble(Bluetooth ble){
        this.ble = ble;
    }
    public void set_cali(int value){
        this.correction_degree = value;
    }

    public void init(){
        init = false;
        init_num = 0;
        init_array = new float[200];

        for(int i=0; i < ori_queue.length; i++){
            ori_queue[i] = 0;
        }
        ori_queue_count = 0;
    }
    //그림 설명 중인지 아닌지 세팅
    public void set_explain(boolean value, String p_name){
        this.explain = value;
        this.p_name = p_name;
        Log.e("ori", String.valueOf(explain));
        Log.e("ori", p_name);
    }

    //코너를 마주쳤을 때 사람의 방향 수정
    public void change_forward(float degree){
        correction_degree += degree;
    }
    @Override
    public void onSensorChanged(SensorEvent event) {

                if(event.sensor == acc){
                    System.arraycopy(event.values, 0, acc_values, 0, event.values.length);
                    acc_set = true;
                }
                else if(event.sensor == mag){
                    System.arraycopy(event.values, 0, mag_values, 0, event.values.length);
                    mag_set = true;
                }
                if (acc_set && mag_set && start) {
                    float[] R = new float[9];
                    float[] I = new float[9];


                    SensorManager.getRotationMatrix(R, I, acc_values, mag_values);

                    float[] values = new float[3];
                    SensorManager.getOrientation(R, values);

                    float azimuth = (int) Math.toDegrees(values[0]);
                    float roll = (int) Math.toDegrees(values[2]);


                    //Log.e("ori",String.valueOf(azimuth));
                    //Log.e("ori", String.valueOf(mag_values[0]) + " / " + String.valueOf(mag_values[1]) + " / "+ String.valueOf(mag_values[2]));

                    //tv.setRotation(azimuth);

                    //방향 설정 초기화
                    if(!init){
                        if(init_num == 200){
                            init = true;
                            //tts.speak("방향 설정이 완료되었습니다.", TextToSpeech.QUEUE_ADD, null);
                            correction_degree = moveaverage(init_array);
                        }
                        else {
                            init_array[init_num] = azimuth - roll;
                            init_num += 1;
                        }
                    }

                    else {
                        if(count == filter_length-1) {

                            float value = moveaverage(filter);

                            if (value < 0)
                                value += 360;

                            int direc = direction(value);
                            ori_queue[(ori_queue_count++) % ori_queue.length] = direc;

                            direc = ori_queue_many(ori_queue);


                            //tv.setRotation(direc*45);
                            tv.setRotation(value);


                            //Log.e("ori", String.valueOf(direction(value)));

                            //유저가 그림이 있는 쪽읇 바라볼 때
                            if(direction(value) == 6 && explain) {
                                Log.e("ori-changable false", String.valueOf(direction(value)));
                                //tts.speak(p_name, TextToSpeech.QUEUE_ADD, null);
                                //Log.e("ori", "그림 설명");
                                //위치 측위 중지
                                ble.set_changable(false);
                                exp = 1;
                            }

                            //유저가 진행방향을 바라 볼 떄
                            if((direction(value) == 4 || direction(value) == 0) && explain) {
                                Log.e("ori-changable true", String.valueOf(direction(value)));
                                //위치 측위 시작
                                ble.set_changable(true);
                                exp = 0;
                            }

                            //유저가 반대 방향을 바라 볼 때 BLE class에 이를 통보
                            if(direction(value) == 4)
                                ble.set_direction(false);
                            if(direction(value) == 0)
                                ble.set_direction(true);

                            prive_ori = direction(value);
                            count =0;
                        }
                        else{
                            filter[count] = azimuth - roll - correction_degree;
                            //filter[count] = azimuth;
                            count++;
                        }

                    }

                }



    }

    public void sector_change(ImageView new_tv){
        if(tv == null) {
            start = true;
            tv = new_tv;
            //tv.setImageResource(R.drawable.resize_compass);
            //tv.setBackgroundColor(Color.parseColor("#646464"));
        }
        else {
            tv.setRotation(0);
            //tv.setImageResource(0);
            //tv.setBackgroundColor(Color.parseColor("#646464"));
            tv = new_tv;
            //tv.setImageResource(R.drawable.resize_compass);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private float moveaverage(float [] array){
        float [] new_array = new float[filter_length];
        float average = 0;
        for (int i = 0; i<filter_length; i++) {
            new_array[i] = array[i];
            average += array[i];
        }
        Arrays.sort(new_array);
        //System.out.println(new_array[filter_length/2]);
        //return new_array[filter_length/2];
        return average / filter_length;
    }

    //방위각을 동서남북으로 표시
    private int direction(float value){
        int result = 0;
        float co = value / 45;
        float[] direc = {0,1,2,3,4,5,6,7,8};
        for(int i =0; i<direc.length; i++){
            direc[i] = Math.abs(direc[i]-co);
        }

        //direc의 원소들과 co의 차이 값 중 가장 작은 index 찾기
        float min = 180;
        for(int i =0; i<direc.length; i++){
            if (min > direc[i]) {
                min = direc[i];
                result = i;
            }
        }

        if(result == 7 || result == 8 || result == 1){
            result = 0;
        }

        if(result == 3 || result == 4 || result == 5 ){
            result = 4;
        }
        return result;
    }

    int ori_queue_many(int [] arr){
        int [] orientation = {0,0,0,0,0,0,0,0,0};
        for(int i=0; i < arr.length; i++){
            orientation[arr[i]] ++;
        }
        int count = 0;
        int result = 0;
        for (int i=0; i<orientation.length; i++){
            if(count < orientation[i]){
                count = orientation[i];
                result = i;
            }
        }
        return result;
    }
}
