package com.andreaskyratzis.ptp.Remote;

import com.andreaskyratzis.ptp.Model.NearPlaces;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Url;

public interface MGoogleAPIService {
    @GET
    Call<NearPlaces> getNearByPlaces(@Url String url);

}
