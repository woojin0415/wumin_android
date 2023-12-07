package org.tensorflow.lite.examples.detection.storage;

import android.os.Environment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class StoreManagement {
    private String filepath_ori = Environment.getExternalStorageDirectory().getAbsoluteFile() + "/Wumin" +"/Orientation";
    private String filepath_ble = Environment.getExternalStorageDirectory().getAbsoluteFile() + "/Wumin" +"/BLE";
    StoreManagement()throws IOException {
        File file_ble = new File(filepath_ble);
        if (!file_ble.exists()) {
            file_ble.createNewFile();
        }
        File file_ori = new File(filepath_ori);
        if (!file_ori.exists()) {
            file_ori.createNewFile();
        }
    }

    public void ble_store(BLEStorage[] ble_data) throws IOException {
        File file = new File(filepath_ble + "/"+ String.valueOf(System.currentTimeMillis())+".csv");
        if (!file.exists())
            file.createNewFile();
        FileWriter fw = new FileWriter(file.getAbsoluteFile());
        BufferedWriter bw = new BufferedWriter(fw);

        for(int i=0; i< ble_data.length; i++){
            double []RSSI = ble_data[i].get_RSSI();
            String RSSI_string = "";
            for(int j =0; j< RSSI.length; i++){
                RSSI_string += ","+String.valueOf(RSSI[j]);
            }
            String sector = ble_data[i].get_Sector();
            String time = String.valueOf(ble_data[i].get_Time());

            String w_data = time + "," + sector +"," +RSSI_string;
            bw.write(w_data);
            bw.newLine();
        }
        bw.close();
        fw.close();
    }

    public void ori_store(OrientationStorage[] ori_data) throws  IOException{
        File file = new File(filepath_ori + "/"+ String.valueOf(System.currentTimeMillis())+".csv");
        if (!file.exists())
            file.createNewFile();
        FileWriter fw = new FileWriter(file.getAbsoluteFile());
        BufferedWriter bw = new BufferedWriter(fw);

        for(int i=0; i< ori_data.length; i++){
            String azimuth = String.valueOf(ori_data[i].get_azimuth());
            String sector = ori_data[i].get_sector();
            String time = String.valueOf(ori_data[i].get_time());


            String w_data = time + "," + sector +"," +azimuth;
            bw.write(w_data);
            bw.newLine();
        }
        bw.close();
        fw.close();
    }
}
