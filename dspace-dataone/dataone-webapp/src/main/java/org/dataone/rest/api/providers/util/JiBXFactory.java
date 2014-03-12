package org.dataone.rest.api.providers.util;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang.time.DateUtils;

/**
 * @author Diptamay Sanyal (diptamay@yahoo.com)
 */
public class JiBXFactory {

    public static final String dateFormat = "yyyy-MM-dd'T'HH:mm:ssZ";;
    public static final String dateFormat2 = "yyyy-MM-dd'T'HH:mm:ss";
    public static final String dateFormat3 = "yyyy-MM-dd";

    public static final Pattern DATE_PATTERN_1 = Pattern.compile("[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}[-+][0-9]{4}");
    public static final Pattern DATE_PATTERN_2 = Pattern.compile("[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}");
    public static final Pattern DATE_PATTERN_3 = Pattern.compile("[0-9]{4}-[0-9]{2}-[0-9]{2}");

    public static Character deserializeCharacter(String text) {
        if (text == null || "".equals(text)) return null;
        else return new Character(text.charAt(0));
    }

    public static String serializeCharacter(Character c) {
        return c.toString();
    }

    public static String serializeDate(Date date) {
        return formatDate(date, dateFormat);
    }

    public static String serializeDate2(Date date) {
        return formatDate(date, dateFormat2);
    }

    public static Date deserializeDate(String dateString) {
        if (StringUtils.isEmpty(dateString) || dateString.contains("NaN")) return null;
        try {
            if (DATE_PATTERN_1.matcher(dateString).matches()) {
                // System.out.println(text + " matched pattern " + dateFormat);
                return parseDate(dateString, dateFormat);
            } else if (DATE_PATTERN_2.matcher(dateString).matches()) {
                // System.out.println(text + " matched pattern " + dateFormat2);
                return parseDate(dateString, dateFormat2);
            } else if (DATE_PATTERN_3.matcher(dateString).matches()) {
                // System.out.println(text + " matched pattern " + dateFormat3);
                return parseDate(dateString, dateFormat3);
            } else {
                // System.out.println("Bad Date " + text + ". Not in any expected format");
                System.out.println("Bad Date " + dateString + ". Not in any expected format");
            }
        } catch (ParseException e) {
            e.printStackTrace();
            //log.warn("Bad Date " + dateString + ". Not in any expected format", e);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static List listFactory() {
        return new ArrayList();
    }

    @SuppressWarnings("unchecked")
    public static Set setFactory() {
        return new HashSet();
    }

    @SuppressWarnings("unchecked")
    public static Map mapFactory() {
        return new HashMap();
    }

    private static Date parseDate(String text, String dateFormat) throws ParseException {
        // this creates a new, simpledateformat with each
        // call, so is threadsafe, but it might be faster to
        // synchronize a call to a single simpleDateFormat;
        return DateUtils.parseDate(text, new String[] { dateFormat });
    }

    private static String formatDate(Date date, String pattern) {
        if (date != null) {
            return DateFormatUtils.format(date, pattern, TimeZone.getTimeZone("America/New_York"));
        }
        return null;
    }
}