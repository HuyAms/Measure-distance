package com.example.trinddinhhuy.model;

import java.io.Serializable;

/**
 * Created by Trind Dinh Huy on 4/29/2017.
 */

public class Environment implements Serializable {
    private double temperature;
    private double humidity;
    private double airPressure;

    public Environment() {
    }

    public Environment(double temperature, double humidity, double airPressure) {
        this.temperature = temperature;
        this.humidity = humidity;
        this.airPressure = airPressure;
    }

    public double getTemperature() {
        return temperature;
    }

    public double getHumidity() {
        return humidity;
    }

    public double getAirPressure() {
        return airPressure;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public void setHumidity(double humidity) {
        this.humidity = humidity;
    }

    public void setAirPressure(double airPressure) {
        this.airPressure = airPressure;
    }

    @Override
    public String toString() {
        return  "Temperature: " + temperature + "C" + "\n" +
                "Humidity: " + humidity + "%" +"\n" +
                "Air pressure: "+ airPressure + "hPA";
    }
}
