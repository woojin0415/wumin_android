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
    private float[] rotation_values = new float[16];
    private float[] orientation_values = new float[3];
    private int ori_init_count = 0;
    private int filter_length = 10;
    private float [] filter = new float[filter_length];
    private float [] moving_check_filter = new float[filter_length];
    private int count;
    private Bluetooth ble;
    private int direction;
    private boolean time_interval;
    private float default_rotation = 0;

    //유저가 작품 위치에 있는 지 확인
    private boolean explain_ori = false;
    private OrientationStorage[] ori_storage;
    private int ori_storage_count;
    private StoreManagement store_m;







    private TextToSpeech tts;
    public user_orientation(SensorManager sm, Sensor gyro, Sensor acc, Sensor rotation, TextToSpeech tts, StoreManagement store_m){
        this.sm = sm;
        this.gyro = gyro;
        this.acc = acc;
        this.rotation = rotation;
        this.tts =tts;
        this.time_interval = true;
        this.count = 0;
        this.direction = 0;

        this.default_rotation = 0;




        for(int i = 0; i < filter_length; i++) {
            filter[i] = 0;
            moving_check_filter[i] = 0;
        }

        //ori_storage = new OrientationStorage[10000];
        //this.store_m = store_m;




    }

    public void start(){
        sm.registerListener((SensorEventListener) this, gyro, SensorManager.SENSOR_DELAY_UI);
        sm.registerListener((SensorEventListener) this, acc, SensorManager.SENSOR_DELAY_UI);
        sm.registerListener((SensorEventListener) this, rotation, SensorManager.SENSOR_DELAY_UI);
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


    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.sensor == gyro) {

        }

        if(event.sensor == rotation){
            SensorManager.getRotationMatrixFromVector(rotation_values, event.values);
            SensorManager.getOrientation(rotation_values, orientation_values);

            double azimuth = (Math.toDegrees(orientation_values[0]) + 360) % 360;




            azimuth -= default_rotation;
            if (azimuth < 0) {
                azimuth += 360;
            }

            if (azimuth >= 315 || azimuth <= 45) {
                direction = 0;
            } else if (azimuth > 45 && azimuth <= 90) {
                direction = 1;
            } else if (azimuth > 90 && azimuth <= 270) {
                direction = 2;
            } else {
                direction = 3;
            }

        }

    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }



    String get_direction(){
        return String.valueOf(this.direction);
    }

    double orientation_filter_median(double []arr){
        Arrays.sort(arr);
        return arr[ori_init_count/2];
    }
}