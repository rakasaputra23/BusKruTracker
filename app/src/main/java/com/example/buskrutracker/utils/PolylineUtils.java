package com.example.buskrutracker.utils;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

/**
 * PolylineUtils - Decode Google Encoded Polyline
 */
public class PolylineUtils {

    private static final String TAG = "PolylineUtils";

    /**
     * Decode Google Encoded Polyline ke List<LatLng>
     *
     * @param encoded Encoded polyline string
     * @return List of LatLng coordinates
     */
    public static List<LatLng> decode(String encoded) {
        List<LatLng> poly = new ArrayList<>();
        int index = 0;
        int len = encoded.length();
        int lat = 0;
        int lng = 0;

        while (index < len) {
            int b;
            int shift = 0;
            int result = 0;

            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);

            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;

            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);

            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng position = new LatLng(
                    (double) lat / 1E5,
                    (double) lng / 1E5
            );
            poly.add(position);
        }

        return poly;
    }

    /**
     * Get destination coordinates (last point) dari polyline
     */
    public static LatLng getDestination(String encodedPolyline) {
        try {
            List<LatLng> points = decode(encodedPolyline);
            if (!points.isEmpty()) {
                return points.get(points.size() - 1);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error decoding polyline: " + e.getMessage());
        }
        return null;
    }

    /**
     * Get origin coordinates (first point) dari polyline
     */
    public static LatLng getOrigin(String encodedPolyline) {
        try {
            List<LatLng> points = decode(encodedPolyline);
            if (!points.isEmpty()) {
                return points.get(0);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error decoding polyline: " + e.getMessage());
        }
        return null;
    }
}