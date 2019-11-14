package com.andreaskyratzis.ptp;

import com.andreaskyratzis.ptp.Remote.MGoogleAPIService;
import com.andreaskyratzis.ptp.Remote.RetrofitClient;

public class Com {
    private  static final String GOOGLE_API_URL = "https://maps.googleapis.com/";
    public static MGoogleAPIService getGoogleAPIService(){
        return RetrofitClient.getClient(GOOGLE_API_URL).create(MGoogleAPIService.class);
    }
}
