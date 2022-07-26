package com.ivan.andatest.activities.cliente;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQueryDataEventListener;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.maps.android.SphericalUtil;
import com.ivan.andatest.R;
import com.ivan.andatest.activities.MainActivity;
import com.ivan.andatest.activities.driver.MapDriverActivity;
import com.ivan.andatest.includes.MyToolbar;
import com.ivan.andatest.providers.AuthProvider;
import com.ivan.andatest.providers.GeofireProvider;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MapClientActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private SupportMapFragment mMapFragment;
    private AuthProvider mAuthProvider;

    private LocationRequest mLocationRequest;
    private FusedLocationProviderClient mFusedLocation;
    private GeofireProvider mGeofireProvider;

    private final static int LOCATION_REQUEST_CODE = 1;
    private final static int SETTINGS_REQUEST_CODE = 2;

    private Marker mMarker;
    private LatLng mCurrentLatLng;

    private List<Marker> mDriverMarkers = new ArrayList<>();

    private boolean mIsFirstTime = true;

    private PlacesClient mPlaces;
    private AutocompleteSupportFragment mAutocomplete;
    private AutocompleteSupportFragment mAutocompleteDestination;

    private String mOrigin;
    private LatLng mOriginLntLng;

    private String mDestination;
    private LatLng mDestinationLntLng;

    private GoogleMap.OnCameraIdleListener mCameraListener;

    private Button mButtonRequestDriver;


    LocationCallback mLocationCallback = new LocationCallback() {

        @Override
        public void onLocationResult(LocationResult locationResult) {
            for (Location location : locationResult.getLocations()) {
                if (getApplicationContext() != null) {
                    /*if (mMarker != null) {
                        mMarker.remove();
                    }*/
                    mCurrentLatLng = new LatLng(location.getLatitude(), location.getLongitude());

                   /* mMarker = mMap.addMarker(new MarkerOptions().position(
                            new LatLng(location.getLatitude(), location.getLongitude())
                            )
                                    .title("Tu posición")
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.user_location))
                    );*/

                    // Get user current localization in real time
                    mMap.moveCamera(CameraUpdateFactory.newCameraPosition(
                            new CameraPosition.Builder()
                                    .target(new LatLng(location.getLatitude(), location.getLongitude()))
                                    .zoom(16f)
                                    .build()
                    ));

                    if (mIsFirstTime) {
                        mIsFirstTime = false;
                        getActiveDrivers();
                        limitSearch();
                    }
                }
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_client);
        MyToolbar.show(this, "Cliente", false);

        mAuthProvider = new AuthProvider();
        mGeofireProvider = new GeofireProvider();
        mFusedLocation = LocationServices.getFusedLocationProviderClient(this);

        mMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mMapFragment.getMapAsync(this);
        mButtonRequestDriver = findViewById(R.id.btnRequestDriver);


        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), getResources().getString(R.string.google_maps_key));
        }

        mPlaces = Places.createClient(this);
        instanceAutoCompleteOrigin();
        instanceAutoCompleteDestination();
        onCameraMove();

        mButtonRequestDriver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestDriver();
            }
        });

    }

    private void requestDriver() {

        if (mOriginLntLng != null && mDestinationLntLng != null) {
            Intent intent = new Intent(MapClientActivity.this, DetailRequestActivity.class);
            intent.putExtra("origin lat", mOriginLntLng.latitude);
            intent.putExtra("origin lng", mOriginLntLng.longitude);
            intent.putExtra("destination lat", mDestinationLntLng.latitude);
            intent.putExtra("destination lat", mDestinationLntLng.latitude);
            startActivity(intent);
        }
        else {
            Toast.makeText(this, "Debe seleccionar el lugar de recogida y el destino", Toast.LENGTH_SHORT).show();
        }
    }

    private void limitSearch() {
        LatLng northSide = SphericalUtil.computeOffset(mCurrentLatLng, 5000, 0);
        LatLng southSide = SphericalUtil.computeOffset(mCurrentLatLng, 5000, 180);
        mAutocomplete.setCountry("COL");
        mAutocomplete.setLocationBias(RectangularBounds.newInstance(southSide, northSide));
        mAutocompleteDestination.setCountry("COL");
        mAutocompleteDestination.setLocationBias(RectangularBounds.newInstance(southSide, northSide));
    }

    private void onCameraMove(){
        mCameraListener = new GoogleMap.OnCameraIdleListener() {
            @Override
            public void onCameraIdle() {
                try {
                    Geocoder geocoder = new Geocoder(MapClientActivity.this);
                    mOriginLntLng = mMap.getCameraPosition().target;
                    List<Address> addressList = geocoder.getFromLocation(mOriginLntLng.latitude, mOriginLntLng.longitude, 1);
                    String city = addressList.get(0).getLocality();
                    String country = addressList.get(0).getCountryName();
                    String address = addressList.get(0).getAddressLine(0);
                    mOrigin = address + " " + city;
                    mAutocomplete.setText(address + " " + city);


                } catch (Exception e) {
                    Log.d("Error:", "Mensaje error: " + e.getMessage());
                }
            }
        };
    }

    private void instanceAutoCompleteOrigin() {
        mAutocomplete = (AutocompleteSupportFragment) getSupportFragmentManager().findFragmentById(R.id.placeAutocompleteOrigin);
        mAutocomplete.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.LAT_LNG, Place.Field.NAME));
        mAutocomplete.setHint("Lugar de recogida");
        mAutocomplete.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                mOrigin = place.getName();
                mOriginLntLng = place.getLatLng();
                Log.d("PLACE", "Name: " + mOrigin);
                Log.d("PLACE", "Name: " + mOriginLntLng);
            }

            @Override
            public void onError(@NonNull Status status) {

            }
        });

    }

    private void instanceAutoCompleteDestination() {
        mAutocompleteDestination = (AutocompleteSupportFragment) getSupportFragmentManager().findFragmentById(R.id.placeAutocompleteDestination);
        mAutocompleteDestination.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.LAT_LNG, Place.Field.NAME));
        mAutocompleteDestination.setHint("Destino");
        mAutocompleteDestination.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                mDestination = place.getName();
                mDestinationLntLng = place.getLatLng();
                Log.d("PLACE", "Name: " + mDestination);
                Log.d("PLACE", "Lat: " + mDestinationLntLng.latitude);
                Log.d("PLACE", "Lng: " + mDestinationLntLng.longitude);
            }

            @Override
            public void onError(@NonNull Status status) {

            }
        });
    }

    private void getActiveDrivers() {
        mGeofireProvider.getActiveDrivers(mCurrentLatLng).addGeoQueryDataEventListener(new GeoQueryDataEventListener() {
            @Override
            public void onDataEntered(DataSnapshot dataSnapshot, GeoLocation location) {
                // Add connect drivers markers which connected
                for (Marker marker : mDriverMarkers) {
                    if (marker.getTag() != null) {
                        if (marker.getTag().equals(dataSnapshot)) {
                            return;
                        }
                    }
                }

                LatLng driverLatLng = new LatLng(location.latitude, location.longitude);
                Marker marker = mMap.addMarker(new MarkerOptions().position(driverLatLng)
                        .title("Conductor disponible")
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.top_carview)));
                marker.setTag(dataSnapshot);
                mDriverMarkers.add(marker);
            }

            @Override
            public void onDataExited(DataSnapshot dataSnapshot) {
                // When driver disconnected
                for (Marker marker : mDriverMarkers) {
                    if (marker.getTag() != null) {
                        if (marker.getTag().equals(dataSnapshot)) {
                            marker.remove();
                            mDriverMarkers.remove(marker);
                            return;
                        }
                    }
                }

            }

            @Override
            public void onDataMoved(DataSnapshot dataSnapshot, GeoLocation location) {
                // Update driver position
                for (Marker marker : mDriverMarkers) {
                    if (marker.getTag() != null) {
                        if (marker.getTag().equals(dataSnapshot)) {
                            marker.setPosition(new LatLng(location.latitude, location.longitude));
                        }
                    }
                }

            }

            @Override
            public void onDataChanged(DataSnapshot dataSnapshot, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.setOnCameraIdleListener(mCameraListener);

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(5);

        startLocation();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    if (gpsActive()) {
                        mFusedLocation.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                        mMap.setMyLocationEnabled(true);
                    }
                    else {
                        showAlertDialogNoGps();
                    }
                } else {
                    checkLocationPermissions();
                }
            } else {
                checkLocationPermissions();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SETTINGS_REQUEST_CODE && gpsActive()) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                mFusedLocation.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                mMap.setMyLocationEnabled(true);
            }

        }
        else if (requestCode == SETTINGS_REQUEST_CODE && gpsActive()){
            showAlertDialogNoGps();
        }
    }

    private void showAlertDialogNoGps() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Por favor activa tu ubicación para continuar")
                .setPositiveButton("Configuraciones", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startActivityForResult(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), SETTINGS_REQUEST_CODE);
                    }
                }).create().show();
    }

    private boolean gpsActive() {
        boolean isActive = false;
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            isActive = true;
        }
        return isActive;
    }

    private void startLocation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                if (gpsActive()) {
                    mFusedLocation.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                    mMap.setMyLocationEnabled(true);
                } else {
                    showAlertDialogNoGps();
                }
            } else {
                checkLocationPermissions();
            }
        } else {
            if (gpsActive()) {
                mFusedLocation.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                mMap.setMyLocationEnabled(true);
            }
            else {
                showAlertDialogNoGps();
            }
        }
    }

    private void checkLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            new AlertDialog.Builder(this)
                    .setTitle("Proporciona los permisos para continuar")
                    .setMessage("Esta aplicación requiere de los permisos de ubicación para poder utilizarce")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(MapClientActivity.this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
                        }
                    })
                    .create()
                    .show();
        }
        else {
            ActivityCompat.requestPermissions(MapClientActivity.this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);

        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.client_menu, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            logout();
        }

        return super.onOptionsItemSelected(item);
    }

    public void logout(){
        mAuthProvider.logout();
        Intent intent = new Intent(MapClientActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}