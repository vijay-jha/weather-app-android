package com.example.weatherapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.textfield.TextInputEditText;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private RelativeLayout homeRL;
    private ProgressBar loadingPB;
    private TextView cityNameTV, temperatureTV, conditionTV;
    private RecyclerView weatherRv;
    private TextInputEditText cityEdt;
    private ImageView backTV, iconTV, searchTV;
    private ArrayList<WeatherRVModel> weatherRVModelArrayList;
    private WeatherRVAdapter weatherRVAdapter;
    private LocationManager locationManager;
    private int PERMISSION_CODE = 1;
    private String cityName;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        setContentView(R.layout.activity_main);

        homeRL = findViewById(R.id.idRLHome);
        loadingPB = findViewById(R.id.idPBLoading);
        cityNameTV = findViewById(R.id.idTVCityName);
        temperatureTV = findViewById(R.id.idTVTemperature);
        conditionTV = findViewById(R.id.idTVCondition);
        weatherRv = findViewById(R.id.idRVWeather);
        cityEdt = findViewById(R.id.idEdtCity);
        backTV = findViewById(R.id.idIVBack);
        iconTV = findViewById(R.id.idTVIcon);
        searchTV = findViewById(R.id.idTVSearch);

        weatherRVModelArrayList = new ArrayList<>();
        weatherRVAdapter = new WeatherRVAdapter(this, weatherRVModelArrayList);
        weatherRv.setAdapter(weatherRVAdapter);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_CODE);
        }
        Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        if (location != null) {
            cityName = getCityName(location.getLongitude(), location.getLatitude());
            Log.d("location:", location.toString());
            Log.d("cityname:", cityName);
        } else {
            cityName = "Pune";
        }
        getWeatherInfo(cityName);

        searchTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String city = cityEdt.getText().toString();
                if (city.isEmpty())
                    Toast.makeText(MainActivity.this, "Please enter city name", Toast.LENGTH_SHORT).show();
                else {
                    cityNameTV.setText(city);
                    getWeatherInfo(city);
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(MainActivity.this, "Permission Granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "Please provide permissons", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private String getCityName(double longitude, double lattitude) {
        String cityName = "Not Found";
        Geocoder gcd = new Geocoder(getBaseContext(), Locale.getDefault());
        try {
            List<Address> addresses = gcd.getFromLocation(lattitude, longitude, 10);
            for (Address adr : addresses) {
                if (adr != null) {
                    String city = adr.getLocality();
                    if (city != null && !city.equals("")) {
                        cityName = city;
                    } else {
                        Log.d("TAG", "CITY NOT FOUND");
                        Toast.makeText(this, "User City not found", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return cityName;
    }

    private void getWeatherInfo(String cityName) {
        String url = "http://api.weatherapi.com/v1/forecast.json?key=dd2516b7fec04d89bb345352222305&q=" + cityName + "&days=1&aqi=no&alerts=no";

        Log.d("url", url);

        String newUrl = url;

        cityNameTV.setText(cityName);

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, newUrl, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.d("response", "onResponse: ");
                loadingPB.setVisibility(View.GONE);
                homeRL.setVisibility(View.VISIBLE);
                weatherRVModelArrayList.clear();
                try {
                    String temperature = response.getJSONObject("current").getString("temp_c");
                    temperatureTV.setText(temperature + "°C");
                    int is_Day = response.getJSONObject("current").getInt("is_day");
                    String conditon = response.getJSONObject("current").getJSONObject("condition").getString("text");
                    String conditonIcon = response.getJSONObject("current").getJSONObject("condition").getString("icon");
                    Picasso.get().load("http:".concat(conditonIcon)).into(iconTV);
                    conditionTV.setText(conditon);

                    if (is_Day == 1) {
                        Picasso.get().load(R.drawable.day_image).into(backTV);
                    } else {
                        Picasso.get().load(R.drawable.night_image).into(backTV);
                    }

                    JSONObject forecastObj = response.getJSONObject("forecast");
                    JSONObject forecastO = forecastObj.getJSONArray("forecastday").getJSONObject(0);
                    JSONArray hourArray = forecastO.getJSONArray("hour");

                    for (int i = 0; i < hourArray.length(); i++) {

                        JSONObject hourObj = hourArray.getJSONObject(i);
                        String time = hourObj.getString("time");
                        String temper = hourObj.getString("temp_c");
                        String img = hourObj.getJSONObject("condition").getString("icon");
                        String wind = hourObj.getString("wind_kph");

                        weatherRVModelArrayList.add(new WeatherRVModel(time, temper, img, wind));

                        Log.d("hour", hourObj.toString());
                    }
                    weatherRVAdapter.notifyDataSetChanged();

                } catch (JSONException e) {
                    Log.d("raju", "mera nam");
                    e.printStackTrace();
                }
                    Log.d("tink", "lalala");
            }
        } , new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("error", error.toString());
                Toast.makeText(MainActivity.this, "please enter valid city name", Toast.LENGTH_SHORT).show();
            }
        });

        requestQueue.add(jsonObjectRequest);
    }
}