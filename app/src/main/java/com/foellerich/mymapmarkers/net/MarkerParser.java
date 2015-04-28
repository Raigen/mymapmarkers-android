package com.foellerich.mymapmarkers.net;

import android.util.JsonReader;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by foellerich on 27.04.2015.
 */
public class MarkerParser {

    private final static String TAG = "MarkerParser";

    public List<Entry> parse(InputStream in) throws IOException {
        JsonReader reader = new JsonReader(new InputStreamReader(in, "UTF-8"));
        List<Entry> messages = new ArrayList<Entry>();
        try {
            reader.beginObject();
            while(reader.hasNext()) {
                String name = reader.nextName();
                if (name.equals("markers")) {
                    messages = readMessagesArray(reader);
                }
            }
            return messages;
        } finally {
            reader.close();
        }
    }

    public List<Entry> readMessagesArray(JsonReader reader) throws IOException {
        List<Entry> markers = new ArrayList<Entry>();
        reader.beginArray();
        while (reader.hasNext()) {
            markers.add(readMarker(reader));
        }
        reader.endArray();
        return markers;
    }

    public Entry readMarker(JsonReader reader) throws IOException {
        String id = null;
        String name = null;
        String address = null;
        double lat = 0;
        double lng = 0;

        reader.beginObject();
        while (reader.hasNext()) {
            String key = reader.nextName();
            if (key.equals("_id")) {
                id = reader.nextString();
            } else if (key.equals("name")) {
                name = reader.nextString();
            } else if (key.equals("address")) {
                address = reader.nextString();
            } else if (key.equals("lat")) {
                lat = reader.nextDouble();
            } else if (key.equals("lng")) {
                lng = reader.nextDouble();
            } else {
                reader.skipValue();
            }
        }
        Log.i(TAG, "id: " + id);
        reader.endObject();
        return new Entry(id, name, address, lat, lng);
    }

    public static class Entry {
        public final String id;
        public final String name;
        public final String address;
        public final double lat;
        public final double lng;


        Entry(String id, String name, String address, double lat, double lng) {
            this.id = id;
            this.name = name;
            this.address = address;
            this.lat = lat;
            this.lng = lng;
        }
    }
}
