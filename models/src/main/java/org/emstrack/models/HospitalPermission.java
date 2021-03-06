package org.emstrack.models;

/**
 * A class representing hospital permissions.
 * @author mauricio
 * @since 2/5/18
 */
public class HospitalPermission {

    private int hospitalId;
    private String hospitalName;
    private boolean canRead;
    private boolean canWrite;

    public HospitalPermission(int hospitalId, String hospitalName,
                               Boolean canRead, Boolean canWrite) {
        this.hospitalId = hospitalId;
        this.hospitalName = hospitalName;
        this.canRead = canRead;
        this.canWrite = canWrite;
    }

    public int getHospitalId() {
        return hospitalId;
    }

    public void setHospitalId(int hospitalId) {
        this.hospitalId = hospitalId;
    }

    public String getHospitalName() {
        return hospitalName;
    }

    public void setHospitalName(String hospitalName) {
        this.hospitalName = hospitalName;
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
