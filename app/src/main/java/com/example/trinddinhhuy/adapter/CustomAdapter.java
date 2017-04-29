package com.example.trinddinhhuy.adapter;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.example.trinddinhhuy.thingsee.R;

import java.util.Date;
import java.util.List;

/**
 * Created by Trind Dinh Huy on 4/29/2017.
 */

public class CustomAdapter extends ArrayAdapter<Location> {
    private Activity context;
    private int resource;
    private List<Location> objects;

    public CustomAdapter(@NonNull Activity context, @LayoutRes int resource, @NonNull List<Location> objects) {
        super(context, resource, objects);
        this.context = context;
        this.resource = resource;
        this.objects = objects;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();
        View row = inflater.inflate(resource, null);

        TextView txtDate = (TextView) row.findViewById(R.id.txtDate);
        TextView txtLocation = (TextView)row.findViewById(R.id.txtLocation);

        Location location = objects.get(position);
        Date date = new Date(location.getTime());

        txtDate.setText(date.toString());
        txtLocation.setText(" (" + location.getLatitude() + " , " +
                location.getLongitude() + ")");

        return row;
    }
}
