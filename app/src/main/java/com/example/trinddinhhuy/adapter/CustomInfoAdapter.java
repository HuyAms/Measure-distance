package com.example.trinddinhhuy.adapter;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.example.trinddinhhuy.model.Environment;
import com.example.trinddinhhuy.thingsee.R;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;

/**
 * Created by Trind Dinh Huy on 4/29/2017.
 */

public class CustomInfoAdapter implements GoogleMap.InfoWindowAdapter {
    private Activity context;
    private Environment environment;
    private double speed;

    public CustomInfoAdapter(Activity context, Environment environment, double speed){
        this.context = context;
        this.environment = environment;
        this.speed = speed;
    }

    @Override
    public View getInfoWindow(Marker marker) {
        return null;
    }

    @Override
    public View getInfoContents(Marker marker) {
        LayoutInflater inflater = this.context.getLayoutInflater();
        View v = inflater.inflate(R.layout.map_custom, null);

        TextView txtSpeed = (TextView) v.findViewById(R.id.txtSpeed);
        TextView txtTemperature = (TextView) v.findViewById(R.id.txtTemperature);
        TextView txtHumidity = (TextView) v.findViewById(R.id.txtHumidity);
        TextView txtAirPressure = (TextView) v.findViewById(R.id.txtAirPressure);

        txtSpeed.setText(Double.toString(speed)+ " m/s");
        txtTemperature.setText(Double.toString(environment.getTemperature())+ "C");
        txtHumidity.setText(Double.toString(environment.getHumidity())+ "%");
        txtAirPressure.setText(Double.toString(environment.getAirPressure())+ " hPA");

        return v;
    }
}
