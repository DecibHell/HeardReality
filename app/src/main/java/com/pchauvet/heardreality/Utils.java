/*
 * Copyright (c) 2010 - 2017, Nordic Semiconductor ASA
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form, except as embedded into a Nordic
 *    Semiconductor ASA integrated circuit in a product or a software update for
 *    such product, must reproduce the above copyright notice, this list of
 *    conditions and the following disclaimer in the documentation and/or other
 *    materials provided with the distribution.
 *
 * 3. Neither the name of Nordic Semiconductor ASA nor the names of its
 *    contributors may be used to endorse or promote products derived from this
 *    software without specific prior written permission.
 *
 * 4. This software, with or without modification, must only be used with a
 *    Nordic Semiconductor ASA integrated circuit.
 *
 * 5. Any software provided in binary form under this license must not be reverse
 *    engineered, decompiled, modified and/or disassembled.
 *
 * THIS SOFTWARE IS PROVIDED BY NORDIC SEMICONDUCTOR ASA "AS IS" AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY, NONINFRINGEMENT, AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL NORDIC SEMICONDUCTOR ASA OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.pchauvet.heardreality;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.firestore.GeoPoint;
import com.pchauvet.heardreality.objects.HeardProject;
import com.pchauvet.heardreality.objects.Range;
import com.pchauvet.heardreality.objects.Sound;
import com.pchauvet.heardreality.objects.Source;
import com.pchauvet.heardreality.thingy.Thingy;

public class Utils {

    public static final String EXTRA_DEVICE = "EXTRA_DEVICE";

    public static final int REQUEST_ENABLE_BT = 1020;
    public static final int REQUEST_ACCESS_FINE_LOCATION = 1022;

    public static boolean isConnected(final String address, final List<BluetoothDevice> connectedDevices) {
        for (BluetoothDevice device : connectedDevices) {
            if (address.equals(device.getAddress())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isConnected(final Thingy thingy, final List<BluetoothDevice> connectedDevices) {
        for (BluetoothDevice device : connectedDevices) {
            if (thingy.getDeviceAddress().equals(device.getAddress())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isConnected(final BluetoothDevice thingyDevice, final List<BluetoothDevice> connectedDevices) {
        for (BluetoothDevice device : connectedDevices) {
            if (thingyDevice != null) {
                if (thingyDevice.getAddress().equals(device.getAddress())) {
                    return true;
                }
            } else {
                return false;
            }
        }
        return false;
    }

    public static BluetoothDevice getBluetoothDevice(final Context context, final String address) {
        final BluetoothManager bm = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        final BluetoothAdapter ba = bm.getAdapter();
        if (ba != null && ba.isEnabled()) {
            try {
                return ba.getRemoteDevice(address);
            } catch (Exception ex) {
                return null;
            }
        }
        return null;
    }

    public static Range getProjectStartingRange(HeardProject project){
        if (project.getStartingPoint() != null && project.getRanges() != null) {
            for(Range range : project.getRanges()){
                // We find the range whose id corresponds to the starting point reference
                if(range.getId().equals(project.getStartingPoint())){
                    return range;
                }
            }
        }
        return null;
    }

    public static LatLng getLatLngFromGeoPoint(GeoPoint geoPoint){ return new LatLng(geoPoint.getLatitude(), geoPoint.getLongitude()); }

    public static LatLng getLatLngFromLocation(Location location){ return new LatLng(location.getLatitude(), location.getLongitude()); }

    public static Location getLocationFromGeoPoint(GeoPoint geoPoint){
        Location location = new Location("");
        location.setLatitude(geoPoint.getLatitude());
        location.setLongitude(geoPoint.getLongitude());
        return location;

    }

    public static List<LatLng> getLatLngFromGeoPointList(List<GeoPoint> geoPoints){
        List<LatLng> latLngPoints = new ArrayList<>();
        for (GeoPoint point : geoPoints){
            latLngPoints.add(getLatLngFromGeoPoint(point));
        }
        return latLngPoints;
    }

    public static LatLng getProjectStartingPoint(HeardProject project){
        Range range = getProjectStartingRange(project);
        if (range != null) {
            GeoPoint center;
            if(range.getType().equals("CIRCULAR")) {
                center = range.getCenter();
            } else {
                center = range.getPoints().get(0);
            }
            return getLatLngFromGeoPoint(center);
        }
        return null;
    }

    public static Address getAddressFromLatLng(Context context, LatLng location){
        Geocoder geoCoder = new Geocoder(context);
        try {
            List<Address> matches = geoCoder.getFromLocation(location.latitude, location.longitude, 1);
            return (matches.isEmpty() ? null : matches.get(0));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
