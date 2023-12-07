package org.tensorflow.lite.examples.detection.storage;

public class DetectorStorage {
    private long time;
    private String detect;
    private float[] location = new float[4];

    public void set_values(long time, String detect, float[] location){
        this.time = time;
        this.detect = detect;
        this.location = location;
    }

    public long get_time(){return this.time;}
    public String get_detect(){return this.detect;}
    public float[] get_location(){return this.location;}
}
