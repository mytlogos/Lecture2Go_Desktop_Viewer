package main.background;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.QueryMap;
import retrofit2.http.Streaming;

import java.util.Map;

/**
 *
 */
interface Requests {
    @GET("l2go/-/get/{section}/{faculty}/{category}/0/{semester}")
    Call<ResponseBody> getList(@Path("faculty") int faculty, @Path("section") int section, @Path("semester") int semester, @Path("category") int category);

    @GET("l2go")
    Call<ResponseBody> query(@QueryMap Map<String, String> query);

    @GET("rss/{videoId}.mp4.xml")
    Call<ResponseBody> getVideoFeed(@Path("videoId") int videoId);

    @GET("l2go/-/get/l/{videoId}")
    Call<ResponseBody> getVideoPage(@Path("videoId") int videoId);

    @Streaming
    @GET("https://l2gdownload.rrz.uni-hamburg.de/abo/{video}")
    Call<ResponseBody> getVideoStream(@Path("video") String video);

}
