package org.emstrack.models.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class StringHelper {
    private static Pattern intPattern = Pattern.compile("-?\\d+");
    private static Pattern floatPattern = Pattern.compile("-?\\d+(\\.\\d+)?");

    public static boolean isInt(String str) {
        if (str == null) {
            return false;
        }
        return intPattern.matcher(str).matches();
    }

    public static boolean isFloat(String str) {
        if (str == null) {
            return false;
        }
        return floatPattern.matcher(str).matches();
    }

    public static List<String> removeEmpty(String[] array) {
        List<String> list = new ArrayList<>();
        for (String entry : array) {
            if (!entry.equals("")) {
                list.add(entry);
            }
        }
        return list;
    }

    public static String concatenateStringArray(String[] array, int begin, int size, String delimiter) {
        StringBuilder result = new StringBuilder(array[begin]);
        for (int i = begin + 1; i < begin + size; i++) {
            result.append(delimiter).append(array[i]);
        }
        return result.toString();
    }

    public static String concatenateStringList(List<String> array, int begin, int size, String delimiter) {
        StringBuilder result = new StringBuilder(array.get(begin));
        for (int i = begin + 1; i < begin + size; i++) {
            result.append(delimiter).append(array.get(i));
        }
        return result.toString();
    }
}
