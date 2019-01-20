package org.emstrack.models;

/**
 * A class representing ambulance permissions.
 *
 * @author mauricio
 * @since 2/5/18
 */
public class AmbulancePermission {

    private int ambulanceId;
    private String ambulanceIdentifier;
    private boolean canRead;
    private boolean canWrite;

    public AmbulancePermission(int ambulanceId, String ambulanceIdentifier,
                               Boolean canRead, Boolean canWrite) {
        this.ambulanceId = ambulanceId;
        this.ambulanceIdentifier = ambulanceIdentifier;
        this.canRead = canRead;
        this.canWrite = canWrite;
    }

    public int getAmbulanceId() {
        return ambulanceId;
    }

    public void setAmbulanceId(int ambulanceId) {
        this.ambulanceId = ambulanceId;
    }

    public String getAmbulanceIdentifier() {
        return ambulanceIdentifier;
    }

    public void setAmbulanceIdentifier(String ambulanceIdentifier) {
        this.ambulanceIdentifier = ambulanceIdentifier;
    }

    public boolean isCanRead() {
        return canRead;
    }

    public void setCanRead(boolean canRead) {
        this.canRead = canRead;
    }

    public boolean isCanWrite() {
        return canWrite;
    }

    public void setCanWrite(boolean canWrite) {
        this.canWrite = canWrite;
    }

}