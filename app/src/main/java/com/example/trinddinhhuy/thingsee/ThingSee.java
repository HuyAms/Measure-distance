/*
 * ThingSee.java -- ThingSee Cloud server client
 *
 * API documentation:
 *  http://api.thingsee.com/doc/rest
 *  https://thingsee.zendesk.com/hc/en-us/articles/205188962-Thingsee-Property-API-
 *  https://thingsee.zendesk.com/hc/en-us/articles/205188982-Thingsee-Events-API-
 * Web interface:
 *  http://app.thingsee.com
 *
 * Copyright (C) 2017 by ZyMIX Oy. All rights reserved.
 * Author(s): Jarkko Vuori
 * Modification(s):
 *   First version created on 04.02.2017
 */
package com.example.trinddinhhuy.thingsee;

import android.location.Location;
import android.util.Log;

import com.example.trinddinhhuy.model.Environment;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.json.*;

public class ThingSee {
    private final static String charset = "UTF-8"; 
    private final static String url     = "http://api.thingsee.com/v2";
    
    private URLConnection connection;
    private String        accountAuthUuid;
    private String        accountAuthToken;
    private Boolean       fConnection;

    /**
     * Authenticates the user
     * <p>
     * Credentials returned from the cloud server are used in the following method calls
     * (so that it is not needed to send email/password information every time to the server).
     *
     * @param email      User's email address used for the authentication
     * @param passwd     User's password
     * @throws Exception Gives an exception with text information if there was an error
     */
    ThingSee(String email, String passwd) throws Exception {
        JSONObject param = new JSONObject();

        param.put("email", email);
        param.put("password", passwd);

        JSONObject resp = getThingSeeObject(param, "/accounts/login");
        accountAuthUuid = resp.getString("accountAuthUuid");
        accountAuthToken = resp.getString("accountAuthToken");
    }
    
    /**
     * Send a request to the ThingSee server at the subpath
     * <p>
     * Authentication is supposed to have been done before (using a constructor)
     *
     * @param  request   Request parameter (optional, null if not needed)
     * @param  path      URI-name for the object to be requested
     * @return           Requested object in JSON-format
     * @throws Exception Gives an exception with text information if there was an error
     */
    private JSONObject getThingSeeObject(JSONObject request, String path) throws Exception {
        JSONObject     resp     = null;
        InputStream    response = null;
        BufferedReader reader   = null;
        
        
        fConnection = false;
        try {
            connection = new URL(url + path).openConnection();
            connection.setRequestProperty("Accept-Charset", charset);
            connection.setRequestProperty("Content-Type", "application/json;charset=" + charset);
            if (accountAuthToken != null)
                connection.setRequestProperty("Authorization", "Bearer " + accountAuthToken);

            // send a request (if needed)
            if (request != null) {
                connection.setDoOutput(true);   // Triggers HTTP POST request
                OutputStream output = connection.getOutputStream();
                output.write(request.toString().getBytes(charset));
            }

            // wait for the reply
            response = connection.getInputStream();
            reader = new BufferedReader(new InputStreamReader(response));
            StringBuilder out = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                out.append(line);
            }

            //System.out.println("Responce: " + out.toString());
            resp = new JSONObject(out.toString());
            fConnection = true;
        } finally {
            // ensure that streams are closed in all situations
            try {
                if (response != null)
                    response.close();
                if (reader != null)
                    reader.close();
            } catch (IOException ioe) {}
        }

        return (resp);
    }

    /**
     * Request the handle to the first ThingSee device at the cloud account
     * <p>
     * Cloud account may have multiple devices, this function selects always the first device
     *
     * @return Return JSON decription of the selected device
     */
    public JSONObject Devices() {
        JSONObject item, resp;

        try {
            resp = getThingSeeObject(null, "/devices");
            JSONArray devices = (JSONArray) resp.get("devices");
            item = devices.getJSONObject(0);
        } catch (Exception e) {
            Log.d("THINGSEE", "No Thingsee device");
            item = null;
        }
        
        return (item);
    }

    /**
     * Request all device events
     * <p>
     * Every device has an event log. This method read the given devices event log.
     *
     * @param  device    Device JSON description (given by Devices() method
     * @param  limit     Maximum number of events retrieved
     * @return           Events in JSON format
     * @throws Exception Gives an exception with text information if there was an error
     */
    public JSONArray Events(JSONObject device, int limit) throws Exception {
        JSONObject resp;
        JSONArray  events;

        try {
            resp = getThingSeeObject(null, "/events/" + device.getString("uuid") + "?limit=" + limit);
            events  = (JSONArray)resp.get("events");
        } catch (Exception e) {
            Log.d("THINGSEE", "ThingseeEvents error " + e);
            throw new Exception("No events");
        }
        
        return (events);
    }

    /* senseID groupID field */
    private static final int GROUP_LOCATION     = 0x01 << 16;
    private static final int GROUP_SPEED        = 0x02 << 16;
    private static final int GROUP_ENERGY       = 0x03 << 16;
    private static final int GROUP_ORIENTATION  = 0x04 << 16;
    private static final int GROUP_ACCELERATION = 0x05 << 16;
    private static final int GROUP_ENVIRONMENT  = 0x06 << 16;
    private static final int GROUP_HW_KEYS      = 0x07 << 16;

    /* senseID propertyID field */
    private static final int PROPERTY1          = 0x01 << 8;
    private static final int PROPERTY2          = 0x02 << 8;
    private static final int PROPERTY3          = 0x03 << 8;
    private static final int PROPERTY4          = 0x04 << 8;

    /**
     * Obtain Location objects from the events array
     * <p>
     * Collects all location events and construct Location object
     *
     * @param  events Device JSON description (given by Devices() method)
     * @return        List of Location objects (coordinates), empty if there are no coordinates available
     * @throws        Exception Gives an exception with text information if there was an error
     */
    public List getPath(JSONArray events) throws Exception {
        List   coordinates = new ArrayList();
        int    k;

        try {
            for (int i = 0; i < events.length(); i++) {
                JSONObject event = events.getJSONObject(i);
                Location   loc   = new Location("ThingseeONE");

                loc.setTime(event.getLong("timestamp"));
                k = 0;
                JSONArray senses = event.getJSONObject("cause").getJSONArray("senses");
                for (int j = 0; j < senses.length(); j++) {
                    JSONObject sense   = senses.getJSONObject(j);
                    int        senseID = Integer.decode(sense.getString("sId"));
                    double     value   = sense.getDouble("val");


                    switch (senseID) {
                        case GROUP_LOCATION | PROPERTY1:
                            loc.setLatitude(value);
                            k++;
                            break;

                        case GROUP_LOCATION | PROPERTY2:
                            loc.setLongitude(value);
                            k++;
                            break;

                    }

                    if (k == 2) {
                        coordinates.add(loc);
                        k = 0;
                    }
                }
            }
        } catch (Exception e) {
            throw new Exception("No coordinates");
        }
        
        return coordinates;
    }

    /**
     * Obtain Environment objects from the events array
     * <p>
     * Collects all environment events and construct Environment object
     *
     * @param  events Device JSON description (given by Devices() method)
     * @return        List of Environment objects (environmentList), empty if there are no environment available
     * @throws        Exception Gives an exception with text information if there was an error
     */
    public List getEnvironment(JSONArray events) throws Exception {
        List   environmentList = new ArrayList();
        int    k;

        try {
            for (int i = 0; i < events.length(); i++) {
                JSONObject event = events.getJSONObject(i);
                Environment environment   = new Environment();

                k = 0;
                JSONArray senses = event.getJSONObject("cause").getJSONArray("senses");
                for (int j = 0; j < senses.length(); j++) {
                    JSONObject sense   = senses.getJSONObject(j);
                    int        senseID = Integer.decode(sense.getString("sId"));
                    double     value   = sense.getDouble("val");


                    switch (senseID) {
                        case GROUP_ENVIRONMENT | PROPERTY1:
                            environment.setTemperature(value);
                            k++;
                            break;

                        case GROUP_ENVIRONMENT | PROPERTY2:
                            environment.setHumidity(value);
                            k++;
                            break;

                        case GROUP_ENVIRONMENT | PROPERTY4:
                            environment.setAirPressure(value);
                            k++;
                            break;
                        case GROUP_SPEED | PROPERTY1:
                            environment.setSpeed(value);
                            k++;
                            break;
                    }

                    if (k == 4) {
                        environmentList.add(environment);
                        k = 0;
                    }
                }
            }
        } catch (Exception e) {
            throw new Exception("No environment");
        }

        return environmentList;
    }
    
    @Override
    public String toString() {
        String s;
        
        if (fConnection)
            s = "Uuid: " + accountAuthUuid + "\nToken: " + accountAuthToken;
        else
            s = "Not authenticated";
        
        return (s);
    }
    
    /**
     * Convert events to string
     * <p>
     * Converts timestamp and senses information of the event to string
     *
     * @param  events Events in JSON format
     * @return        Events in string format
     */
    public String toString(JSONArray events) {
        StringBuilder s = new StringBuilder();
        String        ss;

        try {
            for (int i = 0; i < events.length(); i++) {
                JSONObject event = events.getJSONObject(i);

                s.append(new Date(event.getLong("timestamp")) + ": ");
                //System.out.println("    type: " + event.getString("type"));
                JSONArray senses = event.getJSONObject("cause").getJSONArray("senses");
                for (int j = 0; j < senses.length(); j++) {
                    JSONObject sense = senses.getJSONObject(j);

                    s.append("sId " + sense.getString("sId") + ": " + sense.getDouble("val") + ",");
                }
                s.append("\n");
            }
            ss = s.toString();
        } catch (Exception e) {
            ss = null;
        }

        return ss;
    }
}
