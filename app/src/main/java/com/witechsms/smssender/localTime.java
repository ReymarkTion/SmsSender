package com.witechsms.smssender;

import java.util.Calendar;
import java.util.HashMap;

public class localTime {
    public static HashMap<String, String> getTime() {
        HashMap<String, String> hm = new HashMap<>();
        Calendar calendar = Calendar.getInstance();
        hm.put("year", Integer.toString(calendar.get(Calendar.YEAR)));
        hm.put("month", (calendar.get(Calendar.MONTH) + 1) < 10
                ? "0" + Integer.toString(calendar.get(Calendar.MONTH) + 1)
                : Integer.toString(calendar.get(Calendar.MONTH) + 1));
        hm.put("hour", calendar.get(Calendar.HOUR) < 10
                ? "0" + Integer.toString(calendar.get(Calendar.HOUR))
                : calendar.get(Calendar.HOUR) == 00 ? "12"
                : Integer.toString(calendar.get(Calendar.HOUR)));
        hm.put("am_pm", calendar.get(Calendar.AM_PM) == 1 ? "PM" : "AM");
        hm.put("seconds", Integer.toString(calendar.get(Calendar.SECOND)));
        hm.put("ms", Integer.toString(calendar.get(Calendar.MILLISECOND)));
        hm.put("min", calendar.get(Calendar.MINUTE) < 10
                ? "0" + Integer.toString(calendar.get(Calendar.MINUTE))
                : Integer.toString(calendar.get(Calendar.MINUTE)));
        hm.put("day_month", calendar.get(Calendar.DAY_OF_MONTH) < 10
                ? "0" + Integer.toString(calendar.get(Calendar.DAY_OF_MONTH))
                : Integer.toString(calendar.get(Calendar.DAY_OF_MONTH)));
        return hm;
    }
}
