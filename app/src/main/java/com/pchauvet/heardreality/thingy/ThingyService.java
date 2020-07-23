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

package com.pchauvet.heardreality.thingy;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import androidx.annotation.Nullable;
import com.pchauvet.heardreality.MainActivity;
import com.pchauvet.heardreality.R;
import com.pchauvet.heardreality.database.DatabaseHelper;
import no.nordicsemi.android.thingylib.BaseThingyService;
import no.nordicsemi.android.thingylib.ThingyConnection;

public class ThingyService extends BaseThingyService {
    private DatabaseHelper mDatabaseHelper;
    private boolean mIsActivityFinishing = false;
    private Map<BluetoothDevice, Integer> mLastSelectedAudioTrack;

    public class ThingyBinder extends BaseThingyBinder {
        private boolean mIsScanning;

        /**
         * Saves the activity state.
         *
         * @param activityFinishing if the activity is finishing or not
         */
        public final void setActivityFinishing(final boolean activityFinishing) {
            mIsActivityFinishing = activityFinishing;
        }

        /**
         * Returns the activity state.
         */
        public final boolean getActivityFinishing() {
            return mIsActivityFinishing;
        }

        /**
         * Saves the last visible fragment in the service
         */
        public final void setLastSelectedAudioTrack(final BluetoothDevice device, final int index) {
            mLastSelectedAudioTrack.put(device, index);
        }

        public final int getLastSelectedAudioTrack(final BluetoothDevice device) {
            final Integer track = mLastSelectedAudioTrack.get(device);
            if (track != null)
                return track;
            return 0;
        }

        public void setScanningState(final boolean isScanning) {
            mIsScanning = isScanning;
        }

        public boolean isScanningState() {
            return mIsScanning;
        }

        @Override
        public ThingyConnection getThingyConnection(BluetoothDevice device) {
            return mThingyConnections.get(device);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    //@Override
    private Class<? extends Activity> getNotificationTarget() {
        return MainActivity.class;
    }

    @Override
    public void onDeviceConnected(final BluetoothDevice device, final int connectionState) {
    }

    @Override
    public void onDeviceDisconnected(final BluetoothDevice device, final int connectionState) {
        super.onDeviceDisconnected(device, connectionState);
        removeLastSelectedAudioTracks(device);
    }

    @Nullable
    @Override
    public ThingyBinder onBind(final Intent intent) {
        return new ThingyBinder();
    }

    @Override
    protected void onRebind() {
    }

    @Override
    protected void onUnbind() {
        if (mIsActivityFinishing) {
            final ArrayList<BluetoothDevice> devices = mDevices;
            if (devices != null && devices.size() == 0) {
                stopForegroundThingyService();
                return;
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mLastSelectedAudioTrack = new HashMap<>();
        mDatabaseHelper = new DatabaseHelper(getApplicationContext());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onTaskRemoved(final Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        stopForegroundThingyService();
    }

    private void stopForegroundThingyService() {
        stopForeground(true);
        stopSelf();
    }


    /**
     * Checks if the device is among the connected devices list.
     *
     * @param thingy           device to be checked
     * @param connectedDevices list of connected devices
     */
    private boolean isConnected(Thingy thingy, ArrayList<BluetoothDevice> connectedDevices) {
        for (BluetoothDevice device : connectedDevices) {
            if (thingy.getDeviceAddress().equals(device.getAddress())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the name of a device
     */
    private String getDeviceName(final BluetoothDevice device) {
        if (device != null) {
            final String deviceName = device.getName();
            if (!TextUtils.isEmpty(deviceName)) {
                return deviceName;
            }
        }
        return getString(R.string.default_thingy_name);
    }


    private void removeLastSelectedAudioTracks(final BluetoothDevice device) {
        if (mLastSelectedAudioTrack.containsKey(device)) {
            mLastSelectedAudioTrack.remove(device);
        }
    }
}
