package org.tensorflow.lite.examples.detection.navi;


import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.speech.tts.TextToSpeech;
import android.util.Log;

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
    private double[] ori_init_filter = new double[100];
    private double calib_ori;
    private double prv_ori;

    private int filter_length = 10;
    private float [] filter = new float[filter_length];
    private float [] moving_check_filter = new float[filter_length];
    private int count;
    private Bluetooth ble;
    private boolean time_interval;

    //유저가 작품 위치에 있는 지 확인
    private boolean explain;
    private boolean explain_ori = false;
    private boolean start_ori = false;






    private TextToSpeech tts;
    public user_orientation(SensorManager sm, Sensor gyro, Sensor acc, Sensor rotation, TextToSpeech tts){
        this.sm = sm;
        this.gyro = gyro;
        this.acc = acc;
        this.rotation = rotation;
        this.tts =tts;
        this.explain = false;
        this.time_interval = true;
        this.count = 0;


        sm.registerListener((SensorEventListener) this, gyro, SensorManager.SENSOR_DELAY_UI);
        sm.registerListener((SensorEventListener) this, acc, SensorManager.SENSOR_DELAY_UI);
        sm.registerListener((SensorEventListener) this, rotation, SensorManager.SENSOR_DELAY_UI);
        for(int i = 0; i < filter_length; i++) {
            filter[i] = 0;
            moving_check_filter[i] = 0;
        }



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

        if (event.sensor == gyro) {
            System.arraycopy(event.values, 0, gyro_value, 0, event.values.length);
            //Log.e("gyro",String.valueOf(gyro_value[0]) + " / " + String.valueOf(gyro_value[1]) + " / " + String.valueOf(gyro_value[2]) + " / " );
            filter[count%filter_length] = gyro_value[1];
            count++;
            if (check_filter(filter, -1) && time_interval && !explain && ble.p_location()){
                explain = true;
                ble.set_explaining(explain);
                tts.speak("작품 상세 설명을 시작합니다", TextToSpeech.QUEUE_FLUSH, null);
                ble.speak_wi();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            time_interval = false;
                            Thread.sleep(4000);
                            time_interval = true;
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }).start();
            }
            else if (check_filter(filter, 1)&& time_interval && explain && ble.p_location()){
                explain = false;
                tts.speak("앞 쪽으로 이동해주세요.", TextToSpeech.QUEUE_FLUSH, null);
                ble.set_explaining(false);
                ble.set_changable(true);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            time_interval = false;
                            Thread.sleep(1000);
                            time_interval = true;
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }).start();
            }
        }

        if(event.sensor == rotation && start_ori){
            SensorManager.getRotationMatrixFromVector(rotation_values, event.values);
            SensorManager.getOrientation(rotation_values, orientation_values);

            double azimuth = (Math.toDegrees(orientation_values[0]) + 360) % 360;

            if (ori_init_count < 100){
                ori_init_filter[ori_init_count] = azimuth;
                ori_init_count++;
            }
            else if (ori_init_count == 100){
                calib_ori = orientation_filter_median(ori_init_filter);
                ori_init_count++;
                tts.speak("방향 초기화 완료", TextToSpeech.QUEUE_FLUSH, null);
            }
            else {
                azimuth = azimuth - calib_ori;
                if(azimuth < 0)
                    azimuth += 360;
                if ((azimuth >= 260 && azimuth <= 280) && !explain_ori && ble.p_location()) {
                    tts.speak("그림이 있음", TextToSpeech.QUEUE_FLUSH, null);
                    Log.e("ori", String.valueOf(azimuth));
                    explain_ori = true;
                    //ble.set_explaining(true);
                    //tts.speak("작품 상세 설명을 시작합니다", TextToSpeech.QUEUE_FLUSH, null);
                    //ble.speak_wi();

                } else if ((azimuth >= 340 || azimuth <= 20) && explain_ori&& ble.p_location()){
                    tts.speak("그림이 있다가 없음", TextToSpeech.QUEUE_FLUSH, null);
                    Log.e("ori", String.valueOf(azimuth));
                    explain_ori = false;
                    //tts.speak("앞 쪽으로 이동해주세요.", TextToSpeech.QUEUE_FLUSH, null);
                    //ble.set_explaining(false);
                    //ble.set_changable(true);
                }
                else if (azimuth >= 160 && azimuth <= 200 ) {
                    //tts.speak("방향 변환", TextToSpeech.QUEUE_ADD, null);
                    //Log.e("ori", String.valueOf(azimuth));
                    //ble.set_direction(false);
                }
            }
        }


    }

    public void set_startori(boolean tf){
        if(!start_ori && tf)
            ori_init_count = 0;
        this.start_ori = tf;
    }

    public void change_cali_AtC(double value){
        this.calib_ori += value;
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

   boolean check_filter(float []arr, int direc){
        //direc == -1 이면 왼쪽
        //direc == 1 이면 오른 쪽
        float sum = 0;
        for(int i =0; i<arr.length; i++){
            sum += arr[i];
        }
        if(direc == -1) {
            if (sum / arr.length < -1 && sum < 0)
                return true;
            else
                return false;
        }
        else if(direc == 1){
            if (sum / arr.length > 1 && sum > 0)
                return true;
            else
                return false;
        }
        return false;
   }

   double orientation_filter_median(double []arr){
        Arrays.sort(arr);
        return arr[ori_init_count/2];
   }
}
