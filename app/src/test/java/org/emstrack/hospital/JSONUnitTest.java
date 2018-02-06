package org.emstrack.hospital;

import com.google.gson.Gson;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.ArrayList;

import org.emstrack.hospital.models.AmbulancePermission;
import org.emstrack.hospital.models.HospitalPermission;
import org.emstrack.hospital.models.Profile;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class JSONUnitTest {

    @Test
    public void test_profile() throws Exception {

        List<AmbulancePermission> ambulances = new ArrayList<AmbulancePermission>();
        ambulances.add(new AmbulancePermission(1,"BUD1234", Boolean.TRUE, Boolean.TRUE));
        ambulances.add(new AmbulancePermission(2,"BUH4321", Boolean.TRUE, Boolean.FALSE));

        List<HospitalPermission> hospitals = new ArrayList<HospitalPermission>();
        hospitals.add(new HospitalPermission(34,"Hospital Nuevo", Boolean.TRUE, Boolean.FALSE));
        hospitals.add(new HospitalPermission(35,"Hospital Viejo", Boolean.TRUE, Boolean.TRUE));

        Profile profile = new Profile();
        profile.setAmbulances(ambulances);
        profile.setHospitals(hospitals);

        Gson gson = new Gson();

        String to_json = gson.toJson(profile);

        Profile from_json = gson.fromJson(to_json, Profile.class);

        // Check hospitals
        for (int i = 0; i < 2; i++) {
            
            Integer expectedId = profile.getHospitals().get(i).getHospitalId();
            Integer answerId = from_json.getHospitals().get(i).getHospitalId();
            assertEquals(expectedId, answerId);

            String expectedName = profile.getHospitals().get(i).getHospitalName();
            String answerName = from_json.getHospitals().get(i).getHospitalName();
            assertEquals(expectedName, answerName);

            Boolean expectedCanRead = profile.getHospitals().get(i).isCanRead();
            Boolean answerCanRead = from_json.getHospitals().get(i).isCanRead();
            assertEquals(expectedCanRead, answerCanRead);

            Boolean expectedCanWrite = profile.getHospitals().get(i).isCanWrite();
            Boolean answerCanWrite = from_json.getHospitals().get(i).isCanWrite();
            assertEquals(expectedCanWrite, answerCanWrite);

        }
        
        // Check ambulances
        for (int i = 0; i < 2; i++) {
            
            Integer expectedId = profile.getAmbulances().get(i).getAmbulanceId();
            Integer answerId = from_json.getAmbulances().get(i).getAmbulanceId();
            assertEquals(expectedId, answerId);

            String expectedIdentifier = profile.getAmbulances().get(i).getAmbulanceIdentifier();
            String answerIdentifier = from_json.getAmbulances().get(i).getAmbulanceIdentifier();
            assertEquals(expectedIdentifier, answerIdentifier);

            Boolean expectedCanRead = profile.getAmbulances().get(i).isCanRead();
            Boolean answerCanRead = from_json.getAmbulances().get(i).isCanRead();
            assertEquals(expectedCanRead, answerCanRead);

            Boolean expectedCanWrite = profile.getAmbulances().get(i).isCanWrite();
            Boolean answerCanWrite = from_json.getAmbulances().get(i).isCanWrite();
            assertEquals(expectedCanWrite, answerCanWrite);

        }
    }

}
