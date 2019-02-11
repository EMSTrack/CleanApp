package org.emstrack.models;

/**
 * A class representing the API version
 */

public class Version {
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
