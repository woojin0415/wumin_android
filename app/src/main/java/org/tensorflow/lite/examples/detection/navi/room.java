package org.tensorflow.lite.examples.detection.navi;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface room {
    @GET("localization/")
    Call<String> getMember(
            @Query("room") String section,
            @Query("direction") String direction,
            @Query("R1") String r1,
            @Query("R2") String r2,
            @Query("R3") String r3,
            @Query("R4") String r4,
            @Query("R5") String r5,
            @Query("R6") String r6);

}
