package com.example.postoffline;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateUtils {

    public static String formatDateString(String dateString) {
        try {
            SimpleDateFormat dbFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
            Date date = dbFormat.parse(dateString);

            SimpleDateFormat displayFormat = new SimpleDateFormat("EEE dd MMM yyyy");
            return displayFormat.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
            return dateString; // return original string if parsing fails
        }


    }
    public static Date parseDate(String dateString, String format) throws ParseException {
        SimpleDateFormat dateFormat = new SimpleDateFormat(format, Locale.ENGLISH);
        return dateFormat.parse(dateString);
    }
}
