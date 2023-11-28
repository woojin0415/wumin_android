package org.tensorflow.lite.examples.detection.navi_test;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface section3 {
    @GET("localization/section3/")
    Call<String> getMember(
            @Query("Section") String section,
            @Query("RSSI_1") String r1,
            @Query("RSSI_2") String r2,
            @Query("RSSI_3") String r3,
            @Query("RSSI_4") String r4,
            @Query("RSSI_5") String r5,
            @Query("RSSI_6") String r6);
}
