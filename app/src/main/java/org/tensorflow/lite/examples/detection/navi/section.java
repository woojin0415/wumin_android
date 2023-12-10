package org.tensorflow.lite.examples.detection.navi;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface section {
    @GET("localization/section10/")
    Call<String> getMember(
            @Query("Section") String section,
            @Query("RSSI_1") String r1,
            @Query("RSSI_2") String r2,
            @Query("RSSI_3") String r3,
            @Query("nm") String mn);

}
