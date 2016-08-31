package com.ivigilate.android.patrol.utils;
import android.telephony.SmsManager;

public class SMSUtils {

    public void sendSMS(String phoneNumber, String message) {
        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(phoneNumber, null, message, null, null);
    }
}
