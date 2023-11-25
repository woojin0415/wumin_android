package org.tensorflow.lite.examples.detection.navi;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface test {
    @GET("test/")
    Call<String> getMember(
            @Query("Section") String section);
}
