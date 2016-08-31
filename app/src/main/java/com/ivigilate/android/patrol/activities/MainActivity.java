package com.ivigilate.android.patrol.activities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.support.v4.content.ContextCompat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.ivigilate.android.patrol.AppContext;
import com.ivigilate.android.patrol.R;
import com.ivigilate.android.patrol.classes.SightingEx;
import com.ivigilate.android.patrol.utils.Logger;
import com.ivigilate.android.patrol.utils.SMSUtils;
import com.ivigilate.android.library.IVigilateManager;
import com.ivigilate.android.library.classes.DeviceProvisioning;
import com.ivigilate.android.library.classes.GPSLocation;
import com.ivigilate.android.library.classes.RegisteredDevice;
import com.ivigilate.android.library.classes.Sighting;
import com.ivigilate.android.library.interfaces.ILocationListener;
import com.ivigilate.android.library.interfaces.ISighting;
import com.ivigilate.android.library.interfaces.ISightingListener;
import com.ivigilate.android.library.interfaces.IVigilateApiCallback;
import com.ivigilate.android.library.utils.PhoneUtils;
import com.ivigilate.android.library.utils.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

public class MainActivity extends BaseActivity {
    // UI references.
    private ImageView mIvLogout;
    private ImageButton mBtnScan;
    private ImageButton sosBtn;
    private TextView nameCall1Txtview;
    private TextView nameCall2Txtview;
    private TextView panicDialogText;

    private String sosNumber;
    private String firstNumber;
    private String secondNumber;
    private String nameCall1;
    private String nameCall2;
    private LinkedHashMap<String, SightingEx> mSightings;
    private HashMap<String, RegisteredDevice> mProvisionedDevices;
    private List<String> mStatusChanges;
    private boolean mPanicMode;
    private AlertDialog mAlertDialog;
    private GradientDrawable mPanicBorder;

    private static GPSLocation sLastKnownLocation;
    private RegisteredDevice mRegisteredDevice;

    private boolean mGuardTourMode;
    private CountDownTimer mCountDownTimer;
    private Window wind;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Logger.d("Started...");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main_activity);

        getIVigilateManager().startService();
        mSightings = new LinkedHashMap<String, SightingEx>();

        mProvisionedDevices = new HashMap<String, RegisteredDevice>();
        mStatusChanges = new ArrayList<String>();

        if (getIVigilateManager().getUser() != null) {

            final Intent callIntent = new Intent(Intent.ACTION_CALL);
            final Vibrator vibrate = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);

            getIVigilateManager().setSightingListener(new ISightingListener() {
                @Override
                public void onTagSighting(final ISighting unprocessedSighting) {
                    final SightingEx sighting = new SightingEx(unprocessedSighting);
                    //check for changes in sighting status
                    if (sighting.getStatus() != null) {
                        if (!sighting.getStatus().equals(Sighting.Status.Normal)) {
                            synchronized (mStatusChanges) {
                                if (mStatusChanges.isEmpty() || !mStatusChanges.contains(sighting.getMac())) {
                                    mStatusChanges.add(sighting.getMac());
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            mPanicMode = true;
                                            setUpPanicView();
                                            mAlertDialog = createPanicModeDialog();
                                            mAlertDialog.show();
                                            mCountDownTimer.start();
                                            activateSosProcedure(vibrate, callIntent);
                                        }
                                    });
                                }
                            }
                        } else {
                            synchronized (mStatusChanges) {
                                if (!mStatusChanges.isEmpty() && mStatusChanges.contains(sighting.getMac())) {
                                    mStatusChanges.remove(sighting.getMac());
                                }
                            }
                        }

                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            String key = sighting.getMac() + "|" + sighting.getUUID();
                            mSightings.put(key, sighting);
                        }
                    });
                }
            });
            getIVigilateManager().setLocationListener(new ILocationListener() {
                @Override
                public void onLocationChanged(GPSLocation gpsLocation) {
                    sLastKnownLocation = gpsLocation;
                }
            });
            downloadBeacons();
            downloadDetectors();
        }

        bindControls();

        checkRequiredPermissions();

        Logger.d("Finished.");
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkRequiredEnabledFeatures();
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    private IVigilateManager getIVigilateManager() {
        return ((AppContext) getApplicationContext()).getIVigilateManager();
    }

    public static GPSLocation getLastKnownLocation() {
        return sLastKnownLocation;
    }

    public static void restartService(Context context) {
        Logger.d("Restarting iVigilate service...");
        ((AppContext) context.getApplicationContext()).getIVigilateManager().stopService();
        ((AppContext) context.getApplicationContext()).getIVigilateManager().startService();
        Logger.d("Restarting iVigilate service...Done!");
    }

    private void downloadBeacons() {
        getIVigilateManager().getBeacons(new IVigilateApiCallback<List<RegisteredDevice>>() {
            @Override
            public void success(List<RegisteredDevice> registeredDevices) {
                for (RegisteredDevice registeredDevice : registeredDevices) {
                    switch (registeredDevice.getType()) {
                        case "M":
                            registeredDevice.setDeviceType(DeviceProvisioning.DeviceType.TagMovable);
                            break;
                        case "F":
                        default:
                            registeredDevice.setDeviceType(DeviceProvisioning.DeviceType.TagFixed);
                            break;
                    }
                    mProvisionedDevices.put(registeredDevice.getUid().toUpperCase(), registeredDevice);
                }
            }

            @Override
            public void failure(String errorMsg) {
                runToastOnUIThread("Failure getting Beacons " + errorMsg, true);
            }
        });
    }

    private void downloadDetectors() {
        getIVigilateManager().getDetectors(new IVigilateApiCallback<List<RegisteredDevice>>() {
            @Override
            public void success(List<RegisteredDevice> registeredDevices) {
                for (RegisteredDevice registeredDevice : registeredDevices) {
                    switch (registeredDevice.getType()) {
                        case "M":
                            registeredDevice.setDeviceType(DeviceProvisioning.DeviceType.DetectorMovable);
                            break;
                        case "F":
                        default:
                            registeredDevice.setDeviceType(DeviceProvisioning.DeviceType.DetectorFixed);
                            break;
                    }
                    mProvisionedDevices.put(registeredDevice.getUid().toUpperCase(), registeredDevice);
                }
                loadPreferences();
                setButtonsNames();
            }

            @Override
            public void failure(String errorMsg) {
                runToastOnUIThread("Failure getting Detectors " + errorMsg, true);
            }
        });
    }

    private void bindControls() {

        final Intent callIntent = new Intent(Intent.ACTION_CALL);
        final Vibrator vibrate = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);

        ImageView ivLogo = (ImageView) findViewById(R.id.ivLogo);
        ivLogo.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Logger.d("Opening website...");
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(getIVigilateManager().getServerAddress()));
                startActivity(i);
            }
        });

        mIvLogout = (ImageView) findViewById(R.id.ivLogout);
        mIvLogout.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Logger.d("Logging out...");
                getIVigilateManager().stopService();
                if (getIVigilateManager().getUser() != null) {
                    getIVigilateManager().logout(null);
                }
                Intent i = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(i);
                finish();
            }
        });

        //barcode or QR code scanning
        mBtnScan = (ImageButton) findViewById(R.id.btnScan);
        mBtnScan.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent("com.google.zxing.client.android.SCAN");
                //set generic mode to scan all types of barcodes
                intent.putExtra("SCAN_MODE", "");
                startActivityForResult(intent, 1);
            }
        });

        sosBtn = (ImageButton) findViewById(R.id.sosBtn);
        ImageButton speedDial1Btn = (ImageButton) findViewById(R.id.speeddial1_btn);
        ImageButton speedDial2Btn = (ImageButton) findViewById(R.id.speeddial2_btn);
        nameCall1Txtview = (TextView) findViewById(R.id.nameCall1);
        nameCall2Txtview = (TextView) findViewById(R.id.nameCall2);
        nameCall1Txtview.setGravity(Gravity.CENTER);
        nameCall2Txtview.setGravity(Gravity.CENTER);

        sosBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                activateSosProcedure(vibrate, callIntent);
            }
        });

        speedDial1Btn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                vibrate.vibrate(250);
                if (StringUtils.isNullOrBlank("firstNumber")) {
                    Toast.makeText(getApplicationContext(), "Number not configured!", Toast.LENGTH_SHORT).show();
                } else {
                    callIntent.setData(Uri.parse("tel: " + firstNumber));
                    startActivity(callIntent);
                }
            }
        });

        speedDial2Btn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                vibrate.vibrate(250);
                if (StringUtils.isNullOrBlank("secondNumber")) {
                    Toast.makeText(getApplicationContext(), "Number not configured!", Toast.LENGTH_SHORT).show();
                } else {
                    callIntent.setData(Uri.parse("tel: " + secondNumber));
                    startActivity(callIntent);
                }
            }
        });

        final Switch guardModeSwitch = (Switch) findViewById(R.id.togglebutton);
        guardModeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    getIVigilateManager().setLocationRequestInterval(60 * 1000);
                    restartService(getApplicationContext());
                }else{
                    getIVigilateManager().setLocationRequestInterval(600 * 1000);
                }
                mGuardTourMode = isChecked;
                JsonObject sighting_metadata = getIVigilateManager().getServiceSightingMetadata();
                sighting_metadata.remove("guardTourMode");
                sighting_metadata.addProperty("guardTourMode", mGuardTourMode);
                getIVigilateManager().setServiceSightingMetadata(sighting_metadata);
            }
        });

        mPanicBorder = (GradientDrawable) ContextCompat.getDrawable(getApplicationContext(), R.drawable.panic_border);
        mAlertDialog = createPanicModeDialog();
        setUpCountdownTimer(vibrate, callIntent);
    }

    //Handle scanned barcodes or QR codes
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                String scanContent = intent.getStringExtra("SCAN_RESULT");
                String scanFormat = intent.getStringExtra("SCAN_RESULT_FORMAT");
                getIVigilateManager().scanSighted(scanContent, scanFormat);
            } else if (requestCode == RESULT_CANCELED) {
                Toast toast = Toast.makeText(getApplicationContext(),
                        "Scan was cancelled!", Toast.LENGTH_SHORT);
                toast.show();
            }
        } else {
            Toast toast = Toast.makeText(getApplicationContext(),
                    "No scan data received!", Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    public void loadPreferences() {

        JsonObject sighting_metadata = getIVigilateManager().getServiceSightingMetadata();
        sighting_metadata.addProperty("guardTourMode", mGuardTourMode);

        mRegisteredDevice = mProvisionedDevices.get(PhoneUtils.getDeviceUniqueId(this));
        if (mRegisteredDevice == null) {
            return;
        }
        JsonObject metadata = mRegisteredDevice.getMetadata();

        if(metadata.getAsJsonObject("contact_settings")!= null){
            JsonObject contactSettings = metadata.getAsJsonObject("contact_settings");
            sosNumber = contactSettings.get("sos_number")!= null ? contactSettings.get("sos_number").getAsString() : "";
            firstNumber = contactSettings.get("number1")!= null ? contactSettings.get("number1").getAsString() : "";
            secondNumber = contactSettings.get("number2")!= null ? contactSettings.get("number2").getAsString() : "";
            nameCall1 = contactSettings.get("name_call1")!= null ? contactSettings.get("name_call1").getAsString() : "";
            nameCall2 = contactSettings.get("name_call2")!= null ? contactSettings.get("name_call2").getAsString() : "";
        }
    }

    public void setButtonsNames() {
        nameCall1Txtview.setText(nameCall1);
        nameCall2Txtview.setText(nameCall2);
    }

    private void activateSosProcedure(Vibrator vibrate, Intent callIntent) {
        if (StringUtils.isNullOrBlank(sosNumber)) {
            Toast.makeText(getApplicationContext(), "SOS number not configured!", Toast.LENGTH_SHORT).show();
            return;
        }
        vibrate.vibrate(2000);
        GPSLocation gps = getLastKnownLocation();
        final SMSUtils smstool = new SMSUtils();
        smstool.sendSMS(sosNumber, "SOS \nLat: " + gps.getLatitude() + "\nLng: " + gps.getLongitude() + "\nhttp://maps.google.com?q=" + gps.getLatitude() + "," + gps.getLongitude());
        callIntent.setData(Uri.parse("tel: " + sosNumber));
        startActivity(callIntent);
        mPanicMode = false;
    }


    private void setUpPanicView() {

        if (mPanicMode == true) {
            mPanicBorder.setStroke(4, Color.RED);
            mPanicBorder.setAlpha(200);
        } else {
            mPanicBorder.setStroke(0, Color.TRANSPARENT);
        }
        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.panic_border_view);
        linearLayout.setBackground(mPanicBorder);
    }

    private AlertDialog createPanicModeDialog() {

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();

        View dialogView = inflater.inflate(R.layout.panic_dialog, null);
        dialogBuilder.setView(dialogView);

        dialogBuilder.setTitle("Panic activating in...");
        dialogBuilder.setCancelable(false);

        dialogBuilder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                mCountDownTimer.cancel();
                mPanicMode = false;
                setUpPanicView();
            }
        });

        panicDialogText = (TextView) dialogView.findViewById(R.id.tvPanicDialog);
        final AlertDialog alertDialog = dialogBuilder.create();

        //panic mode should start activity on lock screen
        wind = alertDialog.getWindow();
        wind.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        wind.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        wind.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        return alertDialog;
    }

    private void setUpCountdownTimer(final Vibrator vibrate, final Intent callIntent) {
        mCountDownTimer = new CountDownTimer(10000, 1000) {
            public void onTick(long millisUntilFinished) {
                String remainingSeconds = "" + millisUntilFinished / 1000;
                panicDialogText.setText(remainingSeconds);
                vibrate.vibrate(2000);
            }

            public void onFinish() {
                if (mAlertDialog != null) {
                    mAlertDialog.dismiss();
                    activateSosProcedure(vibrate, callIntent);
                    mPanicMode = false;
                    setUpPanicView();
                }
            }
        };
    }

}

