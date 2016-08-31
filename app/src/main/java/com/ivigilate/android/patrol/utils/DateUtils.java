package com.ivigilate.android.patrol.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class DateUtils {
    static final String DATEFORMAT = "yyyy-MM-dd HH:mm";

    public static Date getUTCdatetimeAsDate()
    {
        //note: doesn't check for null
        return stringDateToDate(getUTCdatetimeAsString());
    }

    public static String getUTCdatetimeAsString()
    {
        final SimpleDateFormat sdf = new SimpleDateFormat(DATEFORMAT);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        final String utcTime = sdf.format(new Date());

        return utcTime;
    }

    public static Date stringDateToDate(String StrDate)
    {
        Date dateToReturn = null;
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATEFORMAT);

        try
        {
            StrDate = StrDate.length() == 10 ? StrDate + " 00:00" : StrDate;
            dateToReturn = (Date)dateFormat.parse(StrDate);
        }
        catch (ParseException e)
        {
            e.printStackTrace();
        }

        return dateToReturn;
    }

    public static String getDatePart(Date date) {
        SimpleDateFormat simpleDateformat = new SimpleDateFormat("yyyy-MM-dd");
        return simpleDateformat.format(date);
    }

    public static String getTime(Date date) {
        SimpleDateFormat simpleDateformat = new SimpleDateFormat("HH:mm");
        simpleDateformat.setTimeZone(TimeZone.getTimeZone("GMT+2"));
        return simpleDateformat.format(date);
    }

    public static String getDayOfWeek(Date date) {
        SimpleDateFormat simpleDateformat = new SimpleDateFormat("E", Locale.ENGLISH); // the day of the week abbreviated
        return simpleDateformat.format(date);
    }

    public static int getDayOfMonth(Date date) {
        date = date != null ? date : new Date();
        SimpleDateFormat simpleDateformat = new SimpleDateFormat("d");
        return Integer.parseInt(simpleDateformat.format(date));
    }
}
