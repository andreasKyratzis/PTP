package com.andreaskyratzis.ptp;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.arch.persistence.room.Room;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.Toast;

import com.andreaskyratzis.ptp.Model.NearPlaces;
import com.andreaskyratzis.ptp.Model.Results;
import com.andreaskyratzis.ptp.Remote.MGoogleAPIService;
import com.andreaskyratzis.ptp.RoomDatabase.FavoritePlaces;
import com.andreaskyratzis.ptp.RoomDatabase.PlacesDataBase;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener,
        GoogleMap.OnMapLongClickListener {

    private static final int MY_PERMISSION_KEY = 1000;
    private GoogleApiClient mGoogleApiClient;
    private GoogleMap mMap;

    PlacesDataBase dataBase;
    private double latitude, longitude;
    private Location myLocation;
    private LocationRequest myLocationRequest;
    MGoogleAPIService myService;
    String placeType = "";
    double userLat, userLng;
    List<FavoritePlaces> favoritePlacesList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        dataBase = Room.databaseBuilder(MapsActivity.this, PlacesDataBase.class, "placesDB").fallbackToDestructiveMigration().allowMainThreadQueries().build();
        favoritePlacesList = new ArrayList<>();
        myService = Com.getGoogleAPIService();  //initialise service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {  //request permission for runtime
            checkLocationPermission();
        }
        BottomNavigationView bottomNavigationView = (BottomNavigationView) findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_groceries:
                        favoritePlacesList.clear();
                        nearbyPlaces("supermarket");
                        break;
                    case R.id.action_restaurants:
                        favoritePlacesList.clear();
                        nearbyPlaces("restaurant");
                        break;
                    case R.id.action_cafes:
                        favoritePlacesList.clear();
                        nearbyPlaces("cafe");
                        break;
                    case R.id.action_bars:
                        favoritePlacesList.clear();
                        nearbyPlaces("bar");
                        break;
                    case R.id.action_favourite:
                        getFavoritePlaces();
                    default:
                        break;
                }
                //Move camera
                double lat = Double.parseDouble(String.valueOf(myLocation.getLatitude()));
                double lng = Double.parseDouble(String.valueOf(myLocation.getLongitude()));
                LatLng latLng = new LatLng(lat, lng);
                mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                mMap.animateCamera(CameraUpdateFactory.zoomTo(13));
                return true;
            }
        });
    }

    private void nearbyPlaces(final String placeT) {
        mMap.clear();
        placeType = placeT;
        String url = getUrl(latitude, longitude, placeT);
        myService.getNearByPlaces(url)
                .enqueue(new Callback<NearPlaces>() {
                    @Override
                    public void onResponse(Call<NearPlaces> call, Response<NearPlaces> response) {
                        if (response.isSuccessful()) {
                            for (int i = 0; i < response.body().getResults().length; i++) {
                                MarkerOptions markerOptions = new MarkerOptions();
                                Results googlePlace = response.body().getResults()[i];
                                double lat = Double.parseDouble(googlePlace.getGeometry().getLocation().getLat());
                                double lng = Double.parseDouble(googlePlace.getGeometry().getLocation().getLng());
                                String nameOfPlace = googlePlace.getName();
                                String vicOfPlace = googlePlace.getVicinity();
                                LatLng latLng = new LatLng(lat, lng);
                                markerOptions.position(latLng);
                                markerOptions.title(nameOfPlace);
                                if (placeT.equals("supermarket"))
                                    markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_groceries));
                                else if (placeT.equals("restaurant"))
                                    markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_restaurant));
                                else if (placeT.equals("cafe"))
                                    markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_cafes));
                                else if (placeT.equals("bar"))
                                    markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_bars));
                                else
                                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                                //Add Markers to Map
                                mMap.addMarker(markerOptions);

                                //Move camera
                                mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                                mMap.animateCamera(CameraUpdateFactory.zoomTo(13));  //14
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<NearPlaces> call, Throwable t) {

                    }
                });

    }

    private String getUrl(double latitude, double longitude, String placeT) {
        StringBuilder googlePlaceUrl = new StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json?");
        googlePlaceUrl.append("location=" + latitude + "," + longitude);
        googlePlaceUrl.append("&radius=" + 1500);  //1500 meters radius
        googlePlaceUrl.append("&type=" + placeT);
        googlePlaceUrl.append("&sensor=true");
        googlePlaceUrl.append("&key=AIzaSyCi6qBjZYTr77Sf_eIT0_C_ZkGivPlS68U");
        Log.d("getUrl", googlePlaceUrl.toString());
        return (googlePlaceUrl.toString());
    }

    private boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION
                }, MY_PERMISSION_KEY);
            } else
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION
                }, MY_PERMISSION_KEY);
            return false;
        } else
            return true;
    }

    //Overrides this method so we can get location from first run of app
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSION_KEY:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        if (mGoogleApiClient == null) {
                            buildGoogleApiClient();
                        }
                        mMap.setMyLocationEnabled(true);
                    }
                }
                else {
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        //Initialise Google Play Services
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                buildGoogleApiClient();
                mMap.setMyLocationEnabled(true);
            }
        }
        else {
            buildGoogleApiClient();
            mMap.setMyLocationEnabled(true);
        }
        mMap.setOnMapLongClickListener(this);
        getFavoritePlaces();     //Show favourite places on start of app

    }

    private boolean buildGoogleApiClient() {
        synchronized (this) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
            mGoogleApiClient.connect();
        }
        return true;
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        myLocationRequest = new LocationRequest();
        myLocationRequest.setInterval(1000);
        myLocationRequest.setFastestInterval(1000);
        myLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, myLocationRequest, (LocationListener) this);
        }

    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {

        //Get Location
        userLat = location.getLatitude();
        userLng = location.getLongitude();
        myLocation = location;
        latitude = location.getLatitude();
        longitude = location.getLongitude();
        LatLng latLng = new LatLng(latitude, longitude);


        //get camera to location
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(13));  //14

        if (mGoogleApiClient != null)
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    public void onMapSearch(View view) {
        TextInputLayout locationSearch = findViewById(R.id.editText);
        if(locationSearch.getEditText().getText().toString().trim().equals("")){
            Toast.makeText(this, "Search using address", Toast.LENGTH_SHORT).show();
        }
        else{
            String location = locationSearch.getEditText().getText().toString();
            List<android.location.Address> addressList = null;

            if (location != null || !location.equals("")) {
                Geocoder geocoder = new Geocoder(this);
                try {
                    addressList = geocoder.getFromLocationName(location, 1);

                } catch (IOException e) {
                    e.printStackTrace();
                }
                android.location.Address address = addressList.get(0);
                LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
                mMap.addMarker(new MarkerOptions().position(latLng).title(""));
                mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
                locationSearch.getEditText().getText().clear(); //empty search bar
            }
        }
    }

    @Override
    public void onMapLongClick(final LatLng latLng) {
        // if location saved in favourites show delete popup
        if (favoritePlacesList.size() > 0) {
            Log.d("markerLat", String.valueOf(latLng.latitude));
            Log.d("markerLong", String.valueOf(latLng.longitude));
            for (int i = 0; i < favoritePlacesList.size(); i++) {
                final FavoritePlaces places = favoritePlacesList.get(i);
                if (Math.abs(places.getLat() - latLng.latitude) < 0.01 && Math.abs(places.getLng() - latLng.longitude) < 0.01) {
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MapsActivity.this);
                    alertDialogBuilder.setTitle("Delete Place")
                            .setMessage("Do you want to delete it from favorites?")
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    favoritePlacesList.remove(places);
                                    dataBase.placeDao().deletePlace(places);
                                    getFavoritePlaces();
                                }
                            }).setNegativeButton("No", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    }).show();
                }
                break;
            }
        }
        else
        {
            //Add place to favorites

            DisplayMetrics metrics = getResources().getDisplayMetrics();
            int width = metrics.widthPixels;
            int height = metrics.heightPixels;

            final Dialog dialog = new Dialog(this);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.add_popup);
            dialog.getWindow().setLayout((6 * width)/7, (4 * height)/10);
            final TextInputLayout nameOfPlace = dialog.findViewById(R.id.nameOfPlace);
            Button saveBtn = dialog.findViewById(R.id.saveBtn);
            saveBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!nameOfPlace.getEditText().getText().toString().isEmpty()) {

                        FavoritePlaces place = new FavoritePlaces();
                        place.setName(nameOfPlace.getEditText().getText().toString());
                        place.setLat(latLng.latitude);
                        place.setLng(latLng.longitude);
                        place.setType(placeType);
                        long id = dataBase.placeDao().insertPlace(place);
                        Log.d("placeID", String.valueOf(id));
                        if (id > 0) {
                            Toast.makeText(MapsActivity.this, "Save Successful", Toast.LENGTH_SHORT).show();
                            nameOfPlace.getEditText().setText("");
                            dialog.dismiss();
                        }
                    }
                    else {
                        Toast.makeText(MapsActivity.this, "Save unsuccessful, Try again", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }
                }
            });

            dialog.show();
        }
    }


    public void getFavoritePlaces() {

        mMap.clear();
        favoritePlacesList = dataBase.placeDao().getFavoritePlaces();

        if (favoritePlacesList.size() > 0) {

            for (int i = 0; i < favoritePlacesList.size(); i++) {
                FavoritePlaces place = favoritePlacesList.get(i);
                MarkerOptions markerOptions = new MarkerOptions();
                double lat = place.getLat();
                double lng = place.getLng();
                String type = place.getType();
                String nameOfPlace = place.getName();
                LatLng latLng = new LatLng(lat, lng);
                markerOptions.position(latLng);
                markerOptions.title(nameOfPlace);
                if (type.equals("supermarket"))
                    markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.favourite_marker_groceries));
                else if (type.equals("restaurant"))
                    markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.favourite_marker_restaurant));
                else if (type.equals("cafe"))
                    markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.favourite_marker_cafes));
                else if (type.equals("bar"))
                    markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.favourite_marker_bars));

                //Add Markers to Map
                mMap.addMarker(markerOptions);

                //Move camera
                mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                mMap.animateCamera(CameraUpdateFactory.zoomTo(13));  //14
            }

        } else {
            Toast.makeText(MapsActivity.this, "No Favorites Places Added", Toast.LENGTH_SHORT).show();
            LatLng latLng = new LatLng(userLat,userLng);
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(13));  //14

        }
    }

}
