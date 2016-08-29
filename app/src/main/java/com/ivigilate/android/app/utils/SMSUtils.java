package com.ivigilate.android.app.utils;
import android.telephony.SmsManager;

import com.ivigilate.android.library.utils.StringUtils;

public class SMSUtils {

    public void sendSMS(String phoneNumber, String message) {
        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(phoneNumber, null, message, null, null);
    }
}
