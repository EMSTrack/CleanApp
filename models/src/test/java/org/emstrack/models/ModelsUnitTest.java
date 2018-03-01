package org.emstrack.models;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.FieldNamingPolicy;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import org.emstrack.models.AmbulancePermission;
import org.emstrack.models.HospitalPermission;
import org.emstrack.models.Profile;
import org.emstrack.models.HospitalEquipmentMetadata;
import org.emstrack.models.HospitalEquipment;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ModelsUnitTest {

    @Test
    public void test_profile() throws Exception {

        List<AmbulancePermission> ambulances = new ArrayList<>();
        ambulances.add(new AmbulancePermission(1,"BUD1234", true, true));
        ambulances.add(new AmbulancePermission(2,"BUH4321", true, false));

        List<HospitalPermission> hospitals = new ArrayList<>();
        hospitals.add(new HospitalPermission(34,"Hospital Nuevo", true, false));
        hospitals.add(new HospitalPermission(35,"Hospital Viejo", true, true));

        Profile profile = new Profile();
        profile.setAmbulances(ambulances);
        profile.setHospitals(hospitals);

        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
        Gson gson = gsonBuilder.create();

        String to_json = gson.toJson(profile);

        Profile from_json = gson.fromJson(to_json, Profile.class);

        // Check hospital permissions
        for (int i = 0; i < hospitals.size(); i++) {

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
        for (int i = 0; i < ambulances.size(); i++) {

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

        to_json = "{\"ambulances\":[{\"ambulance_id\":1,\"can_write\":true,\"can_read\":true,\"ambulance_identifier\":\"BUC1234\"},{\"ambulance_id\":2,\"can_write\":true,\"can_read\":true,\"ambulance_identifier\":\"BUC4321\"}],\"hospitals\":[{\"hospital_name\":\"General Hospital\",\"can_write\":true,\"can_read\":true,\"hospital_id\":1}]}";

        from_json = gson.fromJson(to_json, Profile.class);

        ambulances = new ArrayList<>();
        ambulances.add(new AmbulancePermission(1,"BUC1234", true, true));
        ambulances.add(new AmbulancePermission(2,"BUC4321", true, true));

        hospitals = new ArrayList<>();
        hospitals.add(new HospitalPermission(1,"General Hospital", true, true));

        profile = new Profile();
        profile.setAmbulances(ambulances);
        profile.setHospitals(hospitals);

        // Check hospital permissions
        for (int i = 0; i < hospitals.size(); i++) {

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
        for (int i = 0; i < ambulances.size(); i++) {

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

        List<HospitalEquipmentMetadata> metadata = new ArrayList<>();
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

    @Test
    public void test_settings() throws Exception {

        Map<String,String> ambulanceStatus = new HashMap<>();
        ambulanceStatus.put("UK", "Unknown");
        ambulanceStatus.put("AV", "Available");

        Map<String,String> ambulanceCapability = new HashMap<>();
        ambulanceCapability.put("B", "Basic");
        ambulanceCapability.put("A", "Advanced");

        Map<String,String> equipmentType = new HashMap<>();
        equipmentType.put("B", "Boolean");
        equipmentType.put("I", "Integer");

        Defaults defaults = new Defaults(new Location(32.5149,-117.0382),"BC","Tijuana","MX");

        Settings settings = new Settings(ambulanceCapability, ambulanceStatus, equipmentType, defaults);

        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
        Gson gson = gsonBuilder.create();

        String to_json = gson.toJson(settings);

        Settings from_json = gson.fromJson(to_json, Settings.class);

        double epsilon = 1e-4;

        Defaults expectedDefaults = settings.getDefaults();
        Defaults answerDefaults = from_json.getDefaults();
        assertEquals(expectedDefaults.getCity(), answerDefaults.getCity());
        assertEquals(expectedDefaults.getState(), answerDefaults.getState());
        assertEquals(expectedDefaults.getCountry(), answerDefaults.getCountry());
        assertEquals(expectedDefaults.getLocation().getLatitude(), answerDefaults.getLocation().getLatitude(), epsilon);
        assertEquals(expectedDefaults.getLocation().getLongitude(), answerDefaults.getLocation().getLongitude(), epsilon);

        Map<String,String> expectedAmbulanceCapability = settings.getAmbulanceCapability();
        Map<String,String> answerAmbulanceCapability = from_json.getAmbulanceCapability();
        for (Map.Entry<String,String> entry : expectedAmbulanceCapability.entrySet()) {
            assertEquals(entry.getValue(), answerAmbulanceCapability.get(entry.getKey()));
        }

        Map<String,String> expectedAmbulanceStatus = settings.getAmbulanceStatus();
        Map<String,String> answerAmbulanceStatus = from_json.getAmbulanceStatus();
        for (Map.Entry<String,String> entry : expectedAmbulanceStatus.entrySet()) {
            assertEquals(entry.getValue(), answerAmbulanceStatus.get(entry.getKey()));
        }

        Map<String,String> expectedEquipmentType = settings.getEquipmentType();
        Map<String,String> answerEquipmentType = from_json.getEquipmentType();
        for (Map.Entry<String,String> entry : expectedEquipmentType.entrySet()) {
            assertEquals(entry.getValue(), answerEquipmentType.get(entry.getKey()));
        }

        to_json = "{\"ambulance_status\":{\"PB\":\"Patient bound\",\"HB\":\"Hospital bound\",\"UK\":\"Unknown\",\"AH\":\"At hospital\",\"AV\":\"Available\",\"AP\":\"At patient\",\"OS\":\"Out of service\"},\"defaults\":{\"state\":\"BC\",\"location\":{\"latitude\":\"32.5149\",\"longitude\":\"-117.0382\"},\"city\":\"Tijuana\",\"country\":\"MX\"},\"equipment_type\":{\"I\":\"Integer\",\"S\":\"String\",\"B\":\"Boolean\"},\"ambulance_capability\":{\"R\":\"Rescue\",\"B\":\"Basic\",\"A\":\"Advanced\"}}";

        from_json = gson.fromJson(to_json, Settings.class);

        ambulanceStatus = new HashMap<>();
        ambulanceStatus.put("UK", "Unknown");
        ambulanceStatus.put("AV", "Available");
        ambulanceStatus.put("OS", "Out of service");
        ambulanceStatus.put("AH", "At hospital");
        ambulanceStatus.put("HB", "Hospital bound");
        ambulanceStatus.put("PB", "Patient bound");
        ambulanceStatus.put("AP", "At patient");

        ambulanceCapability = new HashMap<>();
        ambulanceCapability.put("B", "Basic");
        ambulanceCapability.put("A", "Advanced");
        ambulanceCapability.put("R", "Rescue");

        equipmentType = new HashMap<>();
        equipmentType.put("B", "Boolean");
        equipmentType.put("I", "Integer");
        equipmentType.put("S", "String");

        settings = new Settings(ambulanceCapability, ambulanceStatus, equipmentType, defaults);

        expectedDefaults = settings.getDefaults();
        answerDefaults = from_json.getDefaults();
        assertEquals(expectedDefaults.getCity(), answerDefaults.getCity());
        assertEquals(expectedDefaults.getState(), answerDefaults.getState());
        assertEquals(expectedDefaults.getCountry(), answerDefaults.getCountry());
        assertEquals(expectedDefaults.getLocation().getLatitude(), answerDefaults.getLocation().getLatitude(), epsilon);
        assertEquals(expectedDefaults.getLocation().getLongitude(), answerDefaults.getLocation().getLongitude(), epsilon);

        expectedAmbulanceCapability = settings.getAmbulanceCapability();
        answerAmbulanceCapability = from_json.getAmbulanceCapability();
        for (Map.Entry<String,String> entry : expectedAmbulanceCapability.entrySet()) {
            assertEquals(entry.getValue(), answerAmbulanceCapability.get(entry.getKey()));
        }

        expectedAmbulanceStatus = settings.getAmbulanceStatus();
        answerAmbulanceStatus = from_json.getAmbulanceStatus();
        for (Map.Entry<String,String> entry : expectedAmbulanceStatus.entrySet()) {
            assertEquals(entry.getValue(), answerAmbulanceStatus.get(entry.getKey()));
        }

        expectedEquipmentType = settings.getEquipmentType();
        answerEquipmentType = from_json.getEquipmentType();
        for (Map.Entry<String,String> entry : expectedEquipmentType.entrySet()) {
            assertEquals(entry.getValue(), answerEquipmentType.get(entry.getKey()));
        }


    }

    @Test
    public void test_ambulance() throws Exception {

        double epsilon = 1e-4;

        Ambulance ambulance = new Ambulance(1,"1SDH2345","B");
        
        ambulance.setStatus("UK");
        ambulance.setOrientation(12.0);
        ambulance.setLocation(new Location(32.5149,-117.0382));
        ambulance.setTimestamp(new Date());
        ambulance.setUpdatedOn(new Date());

        Gson gson = new Gson();

        String to_json = gson.toJson(ambulance);

        Ambulance from_json = gson.fromJson(to_json, Ambulance.class);

        Integer expectedId = ambulance.getId();
        Integer answerId = from_json.getId();
        assertEquals(expectedId, answerId);

        String expectedName = ambulance.getIdentifier();
        String answerName = from_json.getIdentifier();
        assertEquals(expectedName, answerName);

        expectedName = ambulance.getCapability();
        answerName = from_json.getCapability();
        assertEquals(expectedName, answerName);

        expectedName = ambulance.getStatus();
        answerName = from_json.getStatus();
        assertEquals(expectedName, answerName);

        double expectedDouble = ambulance.getOrientation();
        double answerDouble = from_json.getOrientation();
        assertEquals(expectedDouble, answerDouble, epsilon);

        Location expectedLocation = ambulance.getLocation();
        Location answerLocation = from_json.getLocation();
        assertEquals(expectedLocation.getLatitude(),answerLocation.getLatitude(),epsilon);
        assertEquals(expectedLocation.getLongitude(),answerLocation.getLongitude(),epsilon);

        Date expectedDate = ambulance.getTimestamp();
        Date answerDate = from_json.getTimestamp();
        SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        assertEquals(df.format(expectedDate), df.format(answerDate));

        expectedId = ambulance.getUpdatedBy();
        answerId = from_json.getUpdatedBy();
        assertEquals(expectedId, answerId);

        expectedDate = ambulance.getUpdatedOn();
        answerDate = ambulance.getUpdatedOn();
        assertEquals(df.format(expectedDate), df.format(answerDate));

        // Test partial serialization
        gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

        to_json = gson.toJson(ambulance);

        df = new SimpleDateFormat("MMM d, y K:mm:ss a");
        String expected_to_json = "{\"status\":\"UK\",\"orientation\":12.0,\"location\":{\"latitude\":32.5149,\"longitude\":-117.0382},\"timestamp\":\"" + df.format(ambulance.getTimestamp()) + "\"}";

        assertEquals(expected_to_json, to_json);

    }

}
