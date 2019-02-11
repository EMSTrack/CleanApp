package org.emstrack.models;

/**
 * A class representing the API version
 *
 * @author Leon Wu
 * @since 02/10/2019
 */

public class Version {

    // returns -1 if v1 > v2, 1 if v1 < v2, 0 if equal
    public static int compare(String v1, String v2) {

        // split version numbers
        String[] v1digits = v1.split("\\.");
        String[] v2digits = v2.split("\\.");

        int m = Math.max(v1digits.length, v2digits.length);
        for (int i = 0; i < m; i++) {
            int a = i < v1digits.length ? Integer.parseInt(v1digits[i]) : 0;
            int b = i < v2digits.length ? Integer.parseInt(v2digits[i]) : 0;

            if (a < b)
                return 1;

            else if (a > b)
                return -1;

            // continue if equal
        }

        return 0;
    }

    private String current;
    private String minimum;

    public Version(String current, String minimum) {
        this.current = current;
        this.minimum = minimum;
    }

    public String getCurrentVersion() {
        return current;
    }

    public String getMinimumVersion() {
        return minimum;
    }
}
