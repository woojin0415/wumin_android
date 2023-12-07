package org.tensorflow.lite.examples.detection.storage;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class StoreManagement {
    private String filepath_main = Environment.getExternalStorageDirectory().getAbsoluteFile() + "/Wumin";
    private String filepath_ori = Environment.getExternalStorageDirectory().getAbsoluteFile() + "/Wumin" +"/Orientation";
    private String filepath_ble = Environment.getExternalStorageDirectory().getAbsoluteFile() + "/Wumin" +"/BLE";
    private String filepath_detector = Environment.getExternalStorageDirectory().getAbsoluteFile() + "/Wumin" +"/Detector";
    public StoreManagement()throws IOException {

        File file_main = new File(filepath_main);
        if (!file_main.exists()) {
            file_main.mkdirs();
            Log.e("file", filepath_main);
        }

        File file_ble = new File(filepath_ble);
        if (!file_ble.exists()) {
            file_ble.mkdirs();
        }
        File file_ori = new File(filepath_ori);
        if (!file_ori.exists()) {
            file_ori.mkdirs();
        }

        File file_detector = new File(filepath_detector);
        if (!file_detector.exists()) {
            file_detector.mkdirs();
        }
    }

    public void ble_store(BLEStorage[] ble_data) throws IOException {
        long time_ = System.currentTimeMillis();
        Date day = new Date(time_);
        SimpleDateFormat mFormat = new SimpleDateFormat("yyyy-MM-dd_hh-mm-ss");
        Log.e("day",mFormat.format(day));
        File file = new File(filepath_ble + "/"+ mFormat.format(day)+".csv");
        if (!file.exists())
            file.createNewFile();
        FileWriter fw = new FileWriter(file.getAbsoluteFile());
        BufferedWriter bw = new BufferedWriter(fw);

        bw.write("time,section,sector,ble1,ble2,ble3,ble4,ble5,ble6");
        bw.newLine();

        for(int i=0; i< ble_data.length; i++){
            if(ble_data[i].get_Time() != -1) {
                int[] RSSI = ble_data[i].get_RSSI();
                String RSSI_string = "";
                for (int j = 0; j < RSSI.length; i++) {
                    RSSI_string += "," + String.valueOf(RSSI[j]);
                }
                String sector = ble_data[i].get_Sector();
                String time = String.valueOf(ble_data[i].get_Time());
                String section = ble_data[i].get_section();

                String w_data = time + "," + section+","+sector  + RSSI_string;
                bw.write(w_data);
                bw.newLine();
            }
        }
        bw.close();
        fw.close();
    }

    public void ori_store(OrientationStorage[] ori_data) throws  IOException{
        long time_ = System.currentTimeMillis();
        Date day = new Date(time_);
        SimpleDateFormat mFormat = new SimpleDateFormat("yyyy-MM-dd_hh-mm-ss");
        File file = new File(filepath_ori + "/"+ mFormat.format(day)+".csv");
        if (!file.exists())
            file.createNewFile();
        FileWriter fw = new FileWriter(file.getAbsoluteFile());
        BufferedWriter bw = new BufferedWriter(fw);

        bw.write("time,sector,azimuth");
        bw.newLine();

        for(int i=0; i< ori_data.length; i++){
            if(ori_data[i].get_time() != -1) {
                String azimuth = String.valueOf(ori_data[i].get_azimuth());
                String sector = ori_data[i].get_sector();
                String time = String.valueOf(ori_data[i].get_time());


                String w_data = time + "," + sector + "," + azimuth;
                bw.write(w_data);
                bw.newLine();
            }
        }
        bw.close();
        fw.close();
    }

    public void detector_store(DetectorStorage[] detector_data) throws IOException {
        long time_ = System.currentTimeMillis();
        Date day = new Date(time_);
        SimpleDateFormat mFormat = new SimpleDateFormat("yyyy-MM-dd_hh-mm-ss");
        File file = new File(filepath_detector + "/"+ mFormat.format(day)+".csv");
        if (!file.exists())
            file.createNewFile();
        FileWriter fw = new FileWriter(file.getAbsoluteFile());
        BufferedWriter bw = new BufferedWriter(fw);

        bw.write("time,detect,right,left,top,bottom");
        bw.newLine();

        for(int i=0; i< detector_data.length; i++){
            if(detector_data[i].get_time() != -1) {
                float[] location = detector_data[i].get_location();
                String location_string = "";
                for (int j = 0; j < location.length; i++) {
                    location_string += "," + String.valueOf(location[j]);
                }

                String detect = detector_data[i].get_detect();
                String time = String.valueOf(detector_data[i].get_time());

                String w_data = time + "," + detect + location_string;
                bw.write(w_data);
                bw.newLine();
            }
        }
        bw.close();
        fw.close();
    }

    public void reset_ble(BLEStorage[] ble){
        for(int i = 0; i<ble.length; i++){
            BLEStorage bs = new BLEStorage();
            bs.set_values(-1,"x","x", new int[]{1,1,1,1,1,1});
            ble[i] = bs;
        }
    }
    public void reset_ori(OrientationStorage[] ori){
        for(int i =0; i< ori.length; i++) {
            OrientationStorage os = new OrientationStorage();
            os.set_values(-1, -1, "x");
            ori[i] = os;
        }
    }
    public void reset_detector(DetectorStorage[] detect){
        for(int i =0; i<detect.length; i++) {
            DetectorStorage ds = new DetectorStorage();
            ds.set_values(-1, "x", new float[]{1, 1, 1, 1});
            detect[i] = ds;
        }

    }
}
