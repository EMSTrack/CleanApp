package org.emstrack.hospital;

import com.google.gson.Gson;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.ArrayList;

import org.emstrack.hospital.models.AmbulancePermission;
import org.emstrack.hospital.models.HospitalPermission;
import org.emstrack.hospital.models.Profile;
import org.emstrack.hospital.models.HospitalEquipmentMetadata;
import org.emstrack.hospital.models.HospitalEquipment;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class JSONUnitTest {

    @Test
    public void test_profile() throws Exception {

        List<AmbulancePermission> ambulances = new ArrayList<AmbulancePermission>();
        ambulances.add(new AmbulancePermission(1,"BUD1234", true, true));
        ambulances.add(new AmbulancePermission(2,"BUH4321", true, false));

        List<HospitalPermission> hospitals = new ArrayList<HospitalPermission>();
        hospitals.add(new HospitalPermission(34,"HospitalEquipmentMetadata Nuevo", true, false));
        hospitals.add(new HospitalPermission(35,"HospitalEquipmentMetadata Viejo", true, true));

        Profile profile = new Profile();
        profile.setAmbulances(ambulances);
        profile.setHospitals(hospitals);

        Gson gson = new Gson();

        String to_json = gson.toJson(profile);

        Profile from_json = gson.fromJson(to_json, Profile.class);

        // Check hospital permissions
        for (int i = 0; i < 2; i++) {

            Integer expectedId = profile.getHospitals().get(i).getHospitalId();
            Integer answerId = from_json.getHospitals().get(i).getHospitalId();
            assertEquals(expectedId, answerId);

            String expectedName = profile.getHospitals().get(i).getHospitalName();
            String answerName = from_json.getHospitals().get(i).getHospitalName();
            assertEquals(expectedName, answerName);

            boolean expectedCanRead = profile.getHospitals().get(i).isCanRead();
            boolean answerCanRead = from_json.getHospitals().get(i).isCanRead();
            assertEquals(expectedCanRead, answerCanRead);

            boolean expectedCanWrite = profile.getHospitals().get(i).isCanWrite();
            boolean answerCanWrite = from_json.getHospitals().get(i).isCanWrite();
            assertEquals(expectedCanWrite, answerCanWrite);

        }

        // Check ambulance permissions
        for (int i = 0; i < 2; i++) {

            Integer expectedId = profile.getAmbulances().get(i).getAmbulanceId();
            Integer answerId = from_json.getAmbulances().get(i).getAmbulanceId();
            assertEquals(expectedId, answerId);

            String expectedIdentifier = profile.getAmbulances().get(i).getAmbulanceIdentifier();
            String answerIdentifier = from_json.getAmbulances().get(i).getAmbulanceIdentifier();
            assertEquals(expectedIdentifier, answerIdentifier);

            boolean expectedCanRead = profile.getAmbulances().get(i).isCanRead();
            boolean answerCanRead = from_json.getAmbulances().get(i).isCanRead();
            assertEquals(expectedCanRead, answerCanRead);

            boolean expectedCanWrite = profile.getAmbulances().get(i).isCanWrite();
            boolean answerCanWrite = from_json.getAmbulances().get(i).isCanWrite();
            assertEquals(expectedCanWrite, answerCanWrite);

        }
    }

    @Test
    public void test_hospital_equipment_metadata() throws Exception {

        List<HospitalEquipmentMetadata> metadata = new ArrayList<HospitalEquipmentMetadata>();
        metadata.add(new HospitalEquipmentMetadata(1,"beds", 'I', true));
        metadata.add(new HospitalEquipmentMetadata(2,"x-ray", 'B', true));

        Gson gson = new Gson();

        String to_json = gson.toJson(metadata);

        HospitalEquipmentMetadata[] from_json = gson.fromJson(to_json, HospitalEquipmentMetadata[].class);

        // Check hospital permissions
        for (int i = 0; i < 2; i++) {

            Integer expectedId = metadata.get(i).getId();
            Integer answerId = from_json[i].getId();
            assertEquals(expectedId, answerId);

            String expectedName = metadata.get(i).getName();
            String answerName = from_json[i].getName();
            assertEquals(expectedName, answerName);

            Character expectedEtype = metadata.get(i).getEtype();
            Character answerEtype = from_json[i].getEtype();
            assertEquals(expectedEtype, answerEtype);

            boolean expectedToggleable = metadata.get(i).isToggleable();
            boolean answerToggleable = from_json[i].isToggleable();
            assertEquals(expectedToggleable, answerToggleable);

        }
    }

    @Test
    public void test_hospital_equipment() throws Exception {

        HospitalEquipment equipment = new HospitalEquipment(1,"Hospital General",
                                                            2, "beds",
                                                            'I',
                                                            "12", "",
                                                            1, new Date());

        Gson gson = new Gson();

        String to_json = gson.toJson(equipment);

        HospitalEquipment from_json = gson.fromJson(to_json, HospitalEquipment.class);

        Integer expectedId = equipment.getHospitalId();
        Integer answerId = from_json.getHospitalId();
        assertEquals(expectedId, answerId);

        String expectedName = equipment.getHospitalName();
        String answerName = from_json.getHospitalName();
        assertEquals(expectedName, answerName);

        expectedId = equipment.getEquipmentId();
        answerId = from_json.getEquipmentId();
        assertEquals(expectedId, answerId);

        expectedName = equipment.getEquipmentName();
        answerName = from_json.getEquipmentName();
        assertEquals(expectedName, answerName);

        Character expectedEtype = equipment.getEquipmentEtype();
        Character answerEtype = from_json.getEquipmentEtype();
        assertEquals(expectedEtype, answerEtype);

        String expectedValue = equipment.getValue();
        String answerValue = from_json.getValue();
        assertEquals(expectedValue, answerValue);

        String expectedComment = equipment.getComment();
        String answerComment = from_json.getComment();
        assertEquals(expectedComment, answerComment);

        expectedId = equipment.getUpdatedBy();
        answerId = from_json.getUpdatedBy();
        assertEquals(expectedId, answerId);

        Date expectedDate = equipment.getUpdatedOn();
        Date answerDate = from_json.getUpdatedOn();
        SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        assertEquals(df.format(expectedDate), df.format(answerDate));

    }
}
