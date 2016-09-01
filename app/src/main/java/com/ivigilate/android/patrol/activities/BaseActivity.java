package com.ivigilate.android.patrol.activities;


import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;


import com.google.android.gms.common.GoogleApiAvailability;
import com.ivigilate.android.patrol.utils.Logger;

public class BaseActivity extends Activity {
    private static final int PERMISSIONS_REQUEST = 1;
    private static final int REQUEST_CODE = 1;


    protected void runToastOnUIThread(final String toastText, final boolean isLong) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast toast = Toast.makeText(getApplicationContext(), toastText, isLong ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT);
                toast.show();
            }
        });
    }

    protected void checkRequiredEnabledFeatures() {
        //Verify if Google API needs to be updated
        int errorCode = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(getApplicationContext());
        GoogleApiAvailability.getInstance().showErrorDialogFragment(this, errorCode, REQUEST_CODE);

        // Verify if location services are enabled
        LocationManager service = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (!service.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(this, "Please enable the GPS (location services).", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
            return;
        }

        // Verify if Bluetooth is enabled
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
            return;
        }

        //Verify if NFC is supported and enabled
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter != null && !nfcAdapter.isEnabled()) {
            Toast.makeText(this, "Please enable NFC in Settings", Toast.LENGTH_SHORT).show();
        }
        // Verify if Mobile Data is enabled
        if (!com.ivigilate.android.library.utils.PhoneUtils.isMobileDataEnabled(this)) {
            showMobileDataErrorDialog();
        }


    }

    @TargetApi(Build.VERSION_CODES.M)
    protected void checkRequiredPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.READ_PHONE_STATE,
                            Manifest.permission.READ_CONTACTS,
                            Manifest.permission.SEND_SMS,
                            Manifest.permission.CALL_PHONE,
                            Manifest.permission.VIBRATE},
                    PERMISSIONS_REQUEST);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    Logger.d("Coarse location permission granted");
                } else if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }
                    });
                    builder.show();
                } else if (grantResults[1] == PackageManager.PERMISSION_DENIED) {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("This app needs to be able to access the device's IMEI to be able to function properly.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }
                    });
                    builder.show();
                }
                return;
            }
        }
    }

    protected void showMobileDataErrorDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("No connection")
                .setTitle("No connection")
                .setCancelable(false)
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                            }
                        }
                );
        AlertDialog alert = builder.create();
        alert.show();
    }
}
