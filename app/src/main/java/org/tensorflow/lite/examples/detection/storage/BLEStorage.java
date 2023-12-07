package org.tensorflow.lite.examples.detection.storage;

import android.os.Environment;

import java.io.File;
import java.io.IOException;

public class BLEStorage {

    private int[] RSSI = new int[6];
    private long time;
    private String sector;
    private String section;
    public BLEStorage() {}
    public int []get_RSSI() {
        return RSSI;
    }
    public long get_Time(){
        return time;
    }
    public String get_Sector() { return sector;}
    public String get_section(){return this.section;}

    public void set_values(long now, String section, String sector,  int []RSSI) {
        this.RSSI = RSSI;
        this.section= section;
        this.sector = sector;
        this.time = now;
    }
}
