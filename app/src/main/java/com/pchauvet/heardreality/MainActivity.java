package com.pchauvet.heardreality;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.pchauvet.heardreality.MathUtils.Quaternion;
import com.pchauvet.heardreality.database.DatabaseHelper;
import com.pchauvet.heardreality.dialogs.HTConfigFragment;
import com.pchauvet.heardreality.dialogs.LoginFragment;
import com.pchauvet.heardreality.dialogs.ScannerFragment;
import com.pchauvet.heardreality.dialogs.UserProfileFragment;
import com.pchauvet.heardreality.fragments.PlayingProjectFragment;
import com.pchauvet.heardreality.fragments.StartingProjectFragment;
import com.pchauvet.heardreality.fragments.WorldMapFragment;
import com.pchauvet.heardreality.objects.HeardProject;
import com.pchauvet.heardreality.thingy.EmptyThingyListener;
import com.pchauvet.heardreality.thingy.Thingy;
import com.pchauvet.heardreality.thingy.ThingyService;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat;
import no.nordicsemi.android.support.v18.scanner.ScanCallback;
import no.nordicsemi.android.support.v18.scanner.ScanResult;
import no.nordicsemi.android.thingylib.ThingyListener;
import no.nordicsemi.android.thingylib.ThingyListenerHelper;
import no.nordicsemi.android.thingylib.ThingySdkManager;
import no.nordicsemi.android.thingylib.utils.ThingyUtils;

import static com.pchauvet.heardreality.Utils.REQUEST_ENABLE_BT;
import static com.pchauvet.heardreality.Utils.getBluetoothDevice;
import static no.nordicsemi.android.thingylib.utils.ThingyUtils.showToast;

public class MainActivity extends AppCompatActivity implements ThingySdkManager.ServiceConnectionListener{

    private Toolbar mActivityToolbar;

    private ArrayList<BluetoothDevice> mConnectedBleDeviceList;

    private BluetoothDevice mDevice;

    private ThingySdkManager mThingySdkManager;

    private Handler mProgressHandler = new Handler();
    private boolean mIsScanning;

    private ThingyService.ThingyBinder mBinder;

    private TextView mBatteryLevel;
    private ImageView mBatteryLevelImg;
    private TextView mHTStatus;
    private Button mHTButton;

    private DatabaseHelper mDatabaseHelper;

    private Quaternion quaternionReading = new Quaternion();
    private Quaternion quaternionCalibrated;
    private Quaternion quaternionRefConjugate;

    private FragmentManager mFragmentManager;
    private FragmentTransaction fragmentTransaction;
    private ScannerFragment mScannerFragment;
    private HTConfigFragment mConfigFragment;
    private LoginFragment mLoginFragment;
    private UserProfileFragment mUserProfileFragment;

    private ThingyListener mThingyListener = new EmptyThingyListener() {
        @Override
        public void onDeviceConnected(BluetoothDevice device, int connectionState) {
            mDevice = device;
            if (!mConnectedBleDeviceList.contains(device)) {
                mConnectedBleDeviceList.add(device);
            }
            updateUiOnDeviceConnected();

            // Notify listeners of the change
            for (MainActivityListener listener : listeners){
                listener.onDeviceConnected(getDeviceName());
            }
        }

        @Override
        public void onDeviceDisconnected(BluetoothDevice device, int connectionState) {
            mDevice = null;
            mConnectedBleDeviceList.remove(device);
            updateBatteryLevelVisibility(View.GONE);
            updateUiOnDeviceDisconnected();

            // Notify listeners of the change
            for (MainActivityListener listener : listeners){
                listener.onDeviceDisconnected();
            }
        }

        @Override
        public void onServiceDiscoveryCompleted(BluetoothDevice device) {
            updateBatteryLevelVisibility(View.VISIBLE);
            updateBatteryLevel(mThingySdkManager.getBatteryLevel(mDevice));

            mThingySdkManager.enableBatteryLevelNotifications(device, true);
            mThingySdkManager.enableQuaternionNotifications(mDevice, true);
        }

        @Override
        public void onBatteryLevelChanged(final BluetoothDevice bluetoothDevice, final int batteryLevel) {
            Log.v(ThingyUtils.TAG, "Battery Level: " + batteryLevel + "  address: " + bluetoothDevice.getAddress() + " name: " + getDeviceName());
            if (bluetoothDevice.equals(mThingySdkManager.getSelectedDevice())) {
                    updateBatteryLevel(batteryLevel);
            }
        }

        @Override
        public void onQuaternionValueChangedEvent(final BluetoothDevice bluetoothDevice, float w, float x, float y, float z) {
            // Log.v(ThingyUtils.TAG, "/ w :" + w + "/ x :" + x + "/ y :" + y + "/ z :" + z);
            quaternionReading.set(w,x,y,z);

            if(quaternionRefConjugate != null){
                quaternionCalibrated = quaternionReading.decode(quaternionRefConjugate);
            }else{
                quaternionCalibrated = quaternionReading;
            }

            AudioProcess.rotateHead(quaternionCalibrated.getConjugate());

            if(mConfigFragment.isAdded()) {
                mConfigFragment.onOrientationChanged(quaternionCalibrated.toEulerAngles());
            }
        }
    };

    private void updateBatteryLevelVisibility(final int visibility) {
        mBatteryLevel.setVisibility(visibility);
        mBatteryLevelImg.setVisibility(visibility);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        setContentView(R.layout.activity_main);

        mActivityToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mActivityToolbar);

        mThingySdkManager = ThingySdkManager.getInstance();
        mDatabaseHelper = new DatabaseHelper(this);

        mBatteryLevel = findViewById(R.id.battery_level);
        mBatteryLevelImg = findViewById(R.id.battery_level_img);

        mHTStatus = findViewById(R.id.ht_status);

        mScannerFragment = ScannerFragment.getInstance(ThingyUtils.THINGY_BASE_UUID);
        mConfigFragment = new HTConfigFragment();

        mHTButton = findViewById(R.id.ht_config_button);
        mHTButton.setOnClickListener(v -> onClickConnect());

        // Make sure that the window pans to adjust to screen changes (like opening the keyboard)
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        // Ensure that Bluetooth exists
        if (!ensureBleExists())
            finish();

        // Check that Google Play Services are available
        int resultCode = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS){
            GoogleApiAvailability.getInstance().getErrorDialog(this, resultCode, 1).show();
        }

        mConnectedBleDeviceList = new ArrayList<>();

        mFragmentManager = getSupportFragmentManager();

        // INIT STORAGE MANAGER
        StorageManager.init(this);

        // INIT AUDIO ENGINE
        AudioProcess.initAudioEngine(this);

        // OPEN THE WORLD MAP FRAGMENT AT START
        openWorldMapFragment();

        // CONNECT TO FIRESTORE
        mLoginFragment = new LoginFragment();
        if(AuthManager.currentUser == null){
            mLoginFragment.show(getSupportFragmentManager(), null);
        }

        mUserProfileFragment = new UserProfileFragment();

        // Initialize an empty list of listeners
        listeners = new ArrayList<>();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!isBleEnabled()) {
            enableBle();
        }
        mThingySdkManager.bindService(this, ThingyService.class);
        ThingyListenerHelper.registerThingyListener(this, mThingyListener);
        registerReceiver(mBleStateChangedReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_user:
                if(AuthManager.currentUser == null || FirestoreManager.getUser(AuthManager.currentUser.getUid()) == null){
                    mLoginFragment.show(getSupportFragmentManager(), null);
                }else{
                    mUserProfileFragment.show(getSupportFragmentManager(), null);
                }
                return true;

            case R.id.action_settings:
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Log.e("",getSupportFragmentManager().getFragments().toString());
    }

    @Override
    protected void onStop() {
        super.onStop();

        mThingySdkManager.unbindService(this);
        mBinder = null;
        ThingyListenerHelper.unregisterThingyListener(this, mThingyListener);
        unregisterReceiver(mBleStateChangedReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (isFinishing()) {
            ThingySdkManager.clearInstance();
        }
    }

    public void onDeviceSelected(final BluetoothDevice device, final String name) {
        if (mThingySdkManager != null) {
            mThingySdkManager.connectToThingy(this, device, ThingyService.class);
        }

        mDevice = device;
        final String address = mDevice.getAddress();
        if (!mDatabaseHelper.isExist(address)) {
            String mDeviceName = name;
            if (mDeviceName == null || mDeviceName.isEmpty()) {
                mDeviceName = mDevice.getName();
            }
            mDatabaseHelper.insertDevice(address, mDeviceName);
            mThingySdkManager.setSelectedDevice(mDevice);
        }
        updateSelectionInDb(new Thingy(mDevice), true);
        updateUiOnDeviceConnected();

        // Notify listeners of the change
        for (MainActivityListener listener : listeners){
            listener.onDeviceConnected(getDeviceName());
        }
    }

    private void updateSelectionInDb(final Thingy thingy, final boolean selected) {
        final ArrayList<Thingy> thingyList = mDatabaseHelper.getSavedDevices();
        for (int i = 0; i < thingyList.size(); i++) {
            if (thingy.getDeviceAddress().equals(thingyList.get(i).getDeviceAddress())) {
                mDatabaseHelper.setLastSelected(thingy.getDeviceAddress(), selected);
            } else {
                mDatabaseHelper.setLastSelected(thingyList.get(i).getDeviceAddress(), !selected);
            }
        }
    }

    /**
     * Checks whether the device supports Bluetooth Low Energy communication
     *
     * @return <code>true</code> if BLE is supported, <code>false</code> otherwise
     */
    private boolean ensureBleExists() {
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.no_ble, Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    /**
     * Checks whether the Bluetooth adapter is enabled.
     */
    private boolean isBleEnabled() {
        final BluetoothManager bm = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        final BluetoothAdapter ba = bm.getAdapter();
        return ba != null && ba.isEnabled();
    }

    /**
     * Tries to start Bluetooth adapter.
     */
    private void enableBle() {
        final Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
    }

    // Connects to a thingy on a scan callback
    private void connect() {
        mThingySdkManager.connectToThingy(this, mDevice, ThingyService.class);
        final Thingy thingy = new Thingy(mDevice);
        mThingySdkManager.setSelectedDevice(mDevice);
        updateSelectionInDb(thingy, true);
    }

    private void connect(final BluetoothDevice device) {
        mThingySdkManager.connectToThingy(this, device, ThingyService.class);
        final Thingy thingy = new Thingy(device);
        mThingySdkManager.setSelectedDevice(device);
        updateSelectionInDb(thingy, true);
    }

    @SuppressWarnings("ConstantConditions")
    private void updateHTStatus() {
        String deviceName = getDeviceName();
        if (deviceName != null){
            mHTStatus.setText(getString(R.string.ht_connected, deviceName));
        } else {
            mHTStatus.setText(getString(R.string.ht_missing));
        }
    }

    private void updateUiOnDeviceConnected() {
        updateHTStatus();
        mHTButton.setText(R.string.ht_config);
        mHTButton.setOnClickListener(v -> onClickConfig());
    }

    private void updateUiOnDeviceDisconnected() {
        updateHTStatus();
        mHTButton.setText(R.string.ht_connect);
        mHTButton.setOnClickListener(v -> onClickConnect());
    }

    /**
     * Stop scan on rotation or on app closing.
     * In case the stopScan is called inside onDestroy we have to check if the app is finishing as the mIsScanning flag becomes false on rotation
     */
    private void stopScan() {
        if (mBinder != null) {
            mBinder.setScanningState(false);
        }

        if (mIsScanning) {
            final BluetoothLeScannerCompat scanner = BluetoothLeScannerCompat.getScanner();
            scanner.stopScan(mScanCallback);
            mProgressHandler.removeCallbacks(mBleScannerTimeoutRunnable);
            mIsScanning = false;
        }
    }

    private String mAddress;
    private ScanCallback mScanCallback = new ScanCallback() {

        @Override
        public void onScanResult(final int callbackType, @NonNull final ScanResult result) {
            final BluetoothDevice device = result.getDevice();
            if (mAddress != null && mAddress.equals(device.getAddress())) {
                stopScan();
                connect(device);
                mAddress = null;
                return;
            }

            if (device.equals(mDevice)) {
                new Handler().post(() -> {
                    stopScan();
                    connect();
                });
            }
        }

        @Override
        public void onBatchScanResults(@NonNull final List<ScanResult> results) {
        }

        @Override
        public void onScanFailed(final int errorCode) {
            // should never be called
        }
    };

    final BroadcastReceiver mBleStateChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            final String action = intent.getAction();
            if (action != null && action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                if (state == BluetoothAdapter.STATE_OFF) {
                    showToast(MainActivity.this, getString(R.string.ble_turned_off));
                    enableBle();
                }
            }
        }
    };

    final Runnable mBleScannerTimeoutRunnable = this::stopScan;

    private void updateBatteryLevel(final int batteryLevel) {
        if (batteryLevel > -1) {
            updateBatteryLevelVisibility(View.VISIBLE);
            mBatteryLevel.setText(getString(R.string.battery_level_percent, batteryLevel));
            mBatteryLevelImg.setImageLevel(batteryLevel);
        }
    }

    private void onClickConnect(){
        if (!isBleEnabled()) {
            enableBle();
        }
        if(!mScannerFragment.isAdded()){
            mScannerFragment.show(getSupportFragmentManager(), null);
        }
    }

    private void onClickConfig(){
        mConfigFragment.setDeviceName(getDeviceName());
        if(!mConfigFragment.isAdded()) {
            mConfigFragment.show(getSupportFragmentManager(), null);
        }
    }

    public String getDeviceName(){
        String deviceName = null;
        if (mDevice != null) {
            final String address = mDevice.getAddress();
            if (mDatabaseHelper.isExist(address)) {
                deviceName = mDatabaseHelper.getDeviceName(mDevice.getAddress());
            }
            if (deviceName == null || deviceName.isEmpty()) {
                deviceName = mDevice.getName();
            }
        }
        return deviceName;
    }

    @Override
    public void onServiceConnected() {
        //Use this binder to access you own methods declared in the ThingyService
        mBinder = (ThingyService.ThingyBinder) mThingySdkManager.getThingyBinder();

        final ArrayList<Thingy> savedDevices = mDatabaseHelper.getSavedDevices();
        // If there are devices saved in the database
        if (savedDevices.size() != 0) {
            // Try to get a selected device from the SDKManager
            BluetoothDevice device = mThingySdkManager.getSelectedDevice();
            // If there are none
            if (device == null) {
                // Get the last selected from the database, or the first in the list
                Thingy thingy = mDatabaseHelper.getLastSelected();
                if (thingy == null) {
                    thingy = savedDevices.get(0);
                }
                // Try to find it with Bluetooth
                device = getBluetoothDevice(this, thingy.getDeviceAddress());
            }

            if (device != null) {
                mThingySdkManager.connectToThingy(this, device, ThingyService.class);
            }
        }
    }

    public void onRequestPermission(final String permission, final int requestCode) {
        ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);
    }

    public void onCancellingPermissionRationale() {
        showToast(this, getString(R.string.requested_permission_not_granted_rationale));
    }

    public void onNameChanged(String name) {
        mThingySdkManager.setDeviceName(mDevice, name);
        mDatabaseHelper.updateDeviceName(mDevice.getAddress(), name);

        updateHTStatus();
    }

    public void onDisconnectOrder() {
        mDatabaseHelper.removeDevice(mDevice.getAddress());
        if (mThingySdkManager.isConnected(mDevice)) {
            mThingySdkManager.disconnectFromThingy(mDevice);
        } else {
            mDevice = null;
            updateUiOnDeviceDisconnected();
        }
    }

    public void onCalibration() { quaternionRefConjugate = quaternionReading.getConjugate(); }

    public void openWorldMapFragment(){
        fragmentTransaction = mFragmentManager.beginTransaction();
        WorldMapFragment worldMapFragment = new WorldMapFragment();
        fragmentTransaction.replace(R.id.fragment_container, worldMapFragment);
        fragmentTransaction.commit();
    }

    public void openStartingProjectFragment(HeardProject project){
        fragmentTransaction = mFragmentManager.beginTransaction();
        StartingProjectFragment startingProjectFragment = new StartingProjectFragment(project);
        fragmentTransaction.replace(R.id.fragment_container, startingProjectFragment);
        fragmentTransaction.commit();
    }

    public void openPlayingProjectFragment(HeardProject project, Location ref){
        fragmentTransaction = mFragmentManager.beginTransaction();
        PlayingProjectFragment playingProjectFragment = new PlayingProjectFragment(project, ref);
        fragmentTransaction.replace(R.id.fragment_container, playingProjectFragment);
        fragmentTransaction.commit();
    }

    public ArrayList<MainActivityListener> listeners;
    public interface MainActivityListener{
        void onDeviceConnected(String deviceName);
        void onDeviceDisconnected();
    }
}
