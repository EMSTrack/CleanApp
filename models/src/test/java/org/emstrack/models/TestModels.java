package org.emstrack.models;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.JsonSerializer;

import org.emstrack.models.gson.ExcludeAnnotationExclusionStrategy;
import org.emstrack.models.util.CalendarDateTypeAdapter;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class TestModels {

    @Test
    public void test_profile() {

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
    public void test_hospital_equipment_metadata() {

        List<EquipmentMetadata> metadata = new ArrayList<>();
        metadata.add(new EquipmentMetadata(1,"beds", 'I', true));
        metadata.add(new EquipmentMetadata(2,"x-ray", 'B', true));

        Gson gson = new Gson();

        String to_json = gson.toJson(metadata);

        EquipmentMetadata[] from_json = gson.fromJson(to_json, EquipmentMetadata[].class);

        // Check hospital permissions
        for (int i = 0; i < 2; i++) {

            Integer expectedId = metadata.get(i).getId();
            Integer answerId = from_json[i].getId();
            assertEquals(expectedId, answerId);

            String expectedName = metadata.get(i).getName();
            String answerName = from_json[i].getName();
            assertEquals(expectedName, answerName);

            Character expectedEtype = metadata.get(i).getType();
            Character answerEtype = from_json[i].getType();
            assertEquals(expectedEtype, answerEtype);

            boolean expectedToggleable = metadata.get(i).isToggleable();
            boolean answerToggleable = from_json[i].isToggleable();
            assertEquals(expectedToggleable, answerToggleable);

        }
    }

    @Test
    public void test_hospital_equipment() {

        EquipmentItem equipment = new EquipmentItem(1,
                                                            2, "beds",'I',
                                                            "12", "000",
                                                            1, new Date());

        Gson gson = new Gson();

        String to_json = gson.toJson(equipment);

        EquipmentItem from_json = gson.fromJson(to_json, EquipmentItem.class);

        Integer expectedId = equipment.getEquipmentHolderId();
        Integer answerId = from_json.getEquipmentHolderId();
        assertEquals(expectedId, answerId);

/*
        String expectedName = equipment.getHospitalName();
        String answerName = from_json.getHospitalName();
        assertEquals(expectedName, answerName);
*/

        expectedId = equipment.getEquipmentId();
        answerId = from_json.getEquipmentId();
        assertEquals(expectedId, answerId);

        String expectedName = equipment.getEquipmentName();
        String answerName = from_json.getEquipmentName();
        assertEquals(expectedName, answerName);

        Character expectedEtype = equipment.getEquipmentType();
        Character answerEtype = from_json.getEquipmentType();
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
    public void test_settings() {

        Map<String,String> ambulanceStatus = new HashMap<>();
        ambulanceStatus.put("UK", "Unknown");
        ambulanceStatus.put("AV", "Available");

        List<String> ambulanceStatusOrder = new ArrayList<>();
        ambulanceStatusOrder.add("UK");
        ambulanceStatusOrder.add("AV");

        Map<String,String> ambulanceCapability = new HashMap<>();
        ambulanceCapability.put("B", "Basic");
        ambulanceCapability.put("A", "Advanced");

        List<String> ambulanceCapabilityOrder = new ArrayList<>();
        ambulanceCapabilityOrder.add("B");
        ambulanceCapabilityOrder.add("A");

        Map<String,String> callPriority = new HashMap<>();
        callPriority.put("A", "Urgent");
        callPriority.put("B", "Not urgent");

        List<String> callPriorityOrder = new ArrayList<>();
        ambulanceStatusOrder.add("B");
        ambulanceStatusOrder.add("A");

        Map<String,String> callStatus = new HashMap<>();
        callStatus.put("S", "Started");
        callStatus.put("E", "Ended");

        List<String> callStatusOrder = new ArrayList<>();
        callStatusOrder.add("B");
        callStatusOrder.add("A");

        Map<String,String> ambulancecallStatus = new HashMap<>();
        ambulancecallStatus.put("S", "Started");
        ambulancecallStatus.put("E", "Ended");

        Map<String,String> locationType = new HashMap<>();
        locationType.put("B", "Base");
        locationType.put("A", "AED");
        locationType.put("O", "Other");

        List<String> locationTypeOrder = new ArrayList<>();
        locationTypeOrder.add("B");
        locationTypeOrder.add("A");
        locationTypeOrder.add("O");

        Map<String,String> equipmentType = new HashMap<>();
        equipmentType.put("B", "Boolean");
        equipmentType.put("I", "Integer");

        Map<String,String> equipmentTypeDefaults = new HashMap<>();
        equipmentTypeDefaults.put("B", "True");
        equipmentTypeDefaults.put("I", "0");

        String guestUsername = "guest";
        boolean enableVideo = false;
        Map<String, String> turnServer = new HashMap<>();
        turnServer.put("ip", "127.0.0.1");
        turnServer.put("port", "80");
        turnServer.put("user", "turnuser");
        turnServer.put("pass", "secret");

        Defaults defaults = new Defaults(new GPSLocation(32.5149,-117.0382),"BC","Tijuana","MX");

        Settings settings = new Settings(ambulanceStatus, ambulanceStatusOrder,
                ambulanceCapability, ambulanceCapabilityOrder,
                callPriority, callPriorityOrder,
                callStatus, callStatusOrder,
                ambulancecallStatus,
                locationType, locationTypeOrder,
                equipmentType, equipmentTypeDefaults,
                guestUsername, enableVideo, turnServer,
                defaults);

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

        Map<String,String> expectedLocationType = settings.getLocationType();
        Map<String,String> answerLocationType = from_json.getLocationType();
        for (Map.Entry<String,String> entry : expectedLocationType.entrySet()) {
            assertEquals(entry.getValue(), answerLocationType.get(entry.getKey()));
        }

        String expectedGuestUsername = settings.getGuestUsername();
        assertEquals(expectedGuestUsername, from_json.getGuestUsername());

        boolean expectedEnableVideo = settings.isEnableVideo();
        assertEquals(expectedEnableVideo, from_json.isEnableVideo());

        Map<String,String> expectedTurnServer = settings.getTurnServer();
        Map<String,String> answerTurnServer = from_json.getTurnServer();
        for (Map.Entry<String,String> entry : expectedTurnServer.entrySet()) {
            assertEquals(entry.getValue(), answerTurnServer.get(entry.getKey()));
        }

        to_json = "{\"ambulance_status\":{\"PB\":\"Patient bound\",\"HB\":\"Hospital bound\",\"UK\":\"Unknown\",\"AH\":\"At hospital\",\"AV\":\"Available\",\"AP\":\"At patient\",\"OS\":\"Out of service\"},\"defaults\":{\"state\":\"BC\",\"location\":{\"latitude\":\"32.5149\",\"longitude\":\"-117.0382\"},\"city\":\"Tijuana\",\"country\":\"MX\"},\"equipment_type\":{\"I\":\"Integer\",\"S\":\"String\",\"B\":\"Boolean\"},\"ambulance_capability\":{\"R\":\"Rescue\",\"B\":\"Basic\",\"A\":\"Advanced\"},\"location_type\":{\"B\":\"Base\",\"A\":\"AED\",\"O\":\"Other\"}}";

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

        locationType = new HashMap<>();
        locationType.put("B", "Base");
        locationType.put("A", "AED");
        locationType.put("O", "Other");

        // TODO: this does not test everything!
        settings = new Settings(ambulanceStatus, ambulanceStatusOrder,
                ambulanceCapability, ambulanceCapabilityOrder,
                callPriority, callPriorityOrder,
                callStatus, callStatusOrder,
                ambulancecallStatus,
                locationType, locationTypeOrder,
                equipmentType, equipmentTypeDefaults,
                guestUsername, enableVideo, turnServer,
                defaults);

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

        expectedLocationType = settings.getLocationType();
        answerLocationType = from_json.getLocationType();
        for (Map.Entry<String,String> entry : expectedLocationType.entrySet()) {
            assertEquals(entry.getValue(), answerLocationType.get(entry.getKey()));
        }

    }

    @Test
    public void test_ambulance() {

        double epsilon = 1e-4;

        Ambulance ambulance = new Ambulance(1,"1SDH2345","B");
        
        ambulance.setStatus("UK");
        ambulance.setOrientation(12.0);
        ambulance.setLocation(new GPSLocation(32.5149,-117.0382));
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

        GPSLocation expectedLocation = ambulance.getLocation();
        GPSLocation answerLocation = from_json.getLocation();
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

        df = new SimpleDateFormat("MMM d, y, h:mm:ss a");
        String expected_to_json = "{\"capability\":\"B\",\"status\":\"UK\",\"orientation\":12.0,\"location\":{\"latitude\":32.5149,\"longitude\":-117.0382},\"timestamp\":\"" + df.format(ambulance.getTimestamp()) + "\"}";

        assertEquals(expected_to_json, to_json);

        df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        String timestamp = df.format(new Date());
        System.out.println("date = '" + timestamp + "'");

    }

    @Test
    public void test_hospital() {

        double epsilon = 1e-4;

        List<EquipmentItem> equipment = new ArrayList<EquipmentItem>();
        equipment.add(new EquipmentItem(1,
                2, "beds",'I',
                "12", "",
                1, new Date()));
        equipment.add(new EquipmentItem(1,
                3, "x-rays",'B',
                "True", "no comment",
                1, new Date()));

        Hospital hospital = new Hospital(1,
                "123","Some Street", null, null,
                "Tijuana","BCN","28334","MX",
                "Hospital Viejo", new GPSLocation(32.5149,-117.0382),
                "No comments",1, new Date());

        Gson gson = new Gson();

        String to_json = gson.toJson(hospital);

        Hospital from_json = gson.fromJson(to_json, Hospital.class);
        System.out.println("to_json = '" + to_json + "'");

        Integer expectedId = hospital.getId();
        Integer answerId = from_json.getId();
        assertEquals(expectedId, answerId);

        String expectedString = hospital.getNumber();
        String answerString = from_json.getNumber();
        assertEquals(expectedString, answerString);

        expectedString = hospital.getStreet();
        answerString = from_json.getStreet();
        assertEquals(expectedString, answerString);

        expectedString = hospital.getUnit();
        answerString = from_json.getUnit();
        assertEquals(expectedString, answerString);

        expectedString = hospital.getNeighborhood();
        answerString = from_json.getNeighborhood();
        assertEquals(expectedString, answerString);

        expectedString = hospital.getCity();
        answerString = from_json.getCity();
        assertEquals(expectedString, answerString);

        expectedString = hospital.getState();
        answerString = from_json.getState();
        assertEquals(expectedString, answerString);

        expectedString = hospital.getZipcode();
        answerString = from_json.getZipcode();
        assertEquals(expectedString, answerString);

        expectedString = hospital.getCountry();
        answerString = from_json.getCountry();
        assertEquals(expectedString, answerString);


        expectedString = hospital.getName();
        answerString = from_json.getName();
        assertEquals(expectedString, answerString);

        GPSLocation expectedLocation = hospital.getLocation();
        GPSLocation answerLocation = from_json.getLocation();
        assertEquals(expectedLocation.getLatitude(),answerLocation.getLatitude(),epsilon);
        assertEquals(expectedLocation.getLongitude(),answerLocation.getLongitude(),epsilon);

        expectedString = hospital.getComment();
        answerString = from_json.getComment();
        assertEquals(expectedString, answerString);

        expectedId = hospital.getUpdatedBy();
        answerId = from_json.getUpdatedBy();
        assertEquals(expectedId, answerId);

        Date expectedDate = hospital.getUpdatedOn();
        Date answerDate = hospital.getUpdatedOn();
        SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        assertEquals(df.format(expectedDate), df.format(answerDate));

        /*

        // EQUIPMENT TESTS!

        List<EquipmentItem> expectedList = hospital.getHospitalequipmentSet();
        List<EquipmentItem> answerList = from_json.getHospitalequipmentSet();

        int n = expectedList.size();
        assertEquals(n, 2);
        for (int i = 0; i < n; i++) {

            EquipmentItem expectedEquipment = expectedList.get(i);
            EquipmentItem answerEquipment = expectedList.get(i);
            
            expectedId = expectedEquipment.getEquipmentHolderId();
            answerId = answerEquipment.getEquipmentHolderId();
            assertEquals(expectedId, answerId);

            expectedId = expectedEquipment.getEquipmentId();
            answerId = answerEquipment.getEquipmentId();
            assertEquals(expectedId, answerId);

            String expectedName = expectedEquipment.getEquipmentName();
            String answerName = answerEquipment.getEquipmentName();
            assertEquals(expectedName, answerName);

            Character expectedEtype = expectedEquipment.getEquipmentType();
            Character answerEtype = answerEquipment.getEquipmentType();
            assertEquals(expectedEtype, answerEtype);

            String expectedValue = expectedEquipment.getValue();
            String answerValue = answerEquipment.getValue();
            assertEquals(expectedValue, answerValue);

            String expectedComment = expectedEquipment.getComment();
            String answerComment = answerEquipment.getComment();
            assertEquals(expectedComment, answerComment);

            expectedId = expectedEquipment.getUpdatedBy();
            answerId = answerEquipment.getUpdatedBy();
            assertEquals(expectedId, answerId);

            expectedDate = expectedEquipment.getUpdatedOn();
            answerDate = answerEquipment.getUpdatedOn();
            df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
            assertEquals(df.format(expectedDate), df.format(answerDate));

        }
        */
    }

    @Test
    public void test_patient() {

        Patient patient = new Patient(1,"Jose",35);

        Gson gson = new Gson();

        String to_json = gson.toJson(patient);

        Patient from_json = gson.fromJson(to_json, Patient.class);

        int expectedId = patient.getId();
        int answerId = from_json.getId();
        assertEquals(expectedId, answerId);

        String expectedName = patient.getName();
        String answerName = from_json.getName();
        assertEquals(expectedName, answerName);

        Integer expectedAge = patient.getAge();
        Integer answerAge = from_json.getAge();
        assertEquals(expectedAge, answerAge);

        patient.setAge(null);

        to_json = "{\"id\":1,\"name\":\"Jose\",\"age\":null}";
        from_json = gson.fromJson(to_json, Patient.class);

        expectedId = patient.getId();
        answerId = from_json.getId();
        assertEquals(expectedId, answerId);

        expectedName = patient.getName();
        answerName = from_json.getName();
        assertEquals(expectedName, answerName);

        expectedAge = patient.getAge();
        answerAge = from_json.getAge();
        assertEquals(expectedAge, answerAge);

    }

    @Test
    public void test_location() {

        Location location = new Location(null,"i",
                "O","Bonifácio Avilés", null, null,
                "Tijuana","BCN","" ,"MX",
                new GPSLocation(32.51543632662701,-117.03812250149775));
        
        Gson gson = new Gson();
        
        String to_json = gson.toJson(location);
        
        Location from_json = gson.fromJson(to_json, Location.class);

        String expectedName = location.getName();
        String answerName = from_json.getName();
        assertEquals(expectedName, answerName);

        String expectedStreet = location.getStreet();
        String answerStreet = from_json.getStreet();
        assertEquals(expectedStreet, answerStreet);

        to_json = "{\"type\":\"i\",\"number\":\"\",\"street\":\"Bonifácio Avilés\",\"unit\":null,\"neighborhood\":null,\"city\":\"Tijuana\",\"state\":\"BCN\",\"zipcode\":\"\",\"country\":\"MX\",\"location\":{\"latitude\":\"32.51543632662701\",\"longitude\":\"-117.03812250149775\"},\"updated_on\":\"2018-11-14T22:33:46.055339Z\",\"pending_at\":\"2018-11-14T22:33:46.054955Z\",\"started_at\":\"2018-11-14T22:34:50.329321Z\",\"ended_at\":null,\"comment\":null,\"updated_by\":1,\"updated_on\":\"2018-11-14T22:34:50.329428Z\"}";
        from_json = gson.fromJson(to_json, Location.class);

        expectedName = location.getName();
        answerName = from_json.getName();
        assertEquals(expectedName, answerName);

        expectedStreet = location.getStreet();
        answerStreet = from_json.getStreet();
        assertEquals(expectedStreet, answerStreet);

    }

    @Test
    public void test_waypoint() {

        Waypoint waypoint = new Waypoint(0,Waypoint.STATUS_VISITING,
                new Location(null, Location.TYPE_INCIDENT,"O",
                        "Bonifácio Avilés", null, null,
                        "Tijuana","BCN","" ,"MX",
                        new GPSLocation(32.51543632662701,-117.03812250149775)));
        
        Gson gson = new GsonBuilder()
                .setExclusionStrategies(new ExcludeAnnotationExclusionStrategy())
                .create();

        String to_json = gson.toJson(waypoint);
        System.out.println("to_json = '" + to_json + "'");

        Waypoint from_json = gson.fromJson(to_json, Waypoint.class);

        int expectedOrder = waypoint.getOrder();
        int answerOrder = from_json.getOrder();
        assertEquals(expectedOrder, answerOrder);
        
        boolean expectedVisited = waypoint.isVisited();
        boolean answerVisited = from_json.isVisited();
        assertEquals(expectedVisited, answerVisited);

        Location expectedLocation = waypoint.getLocation();
        Location answerLocation = from_json.getLocation();

        String expectedName = expectedLocation.getName();
        String answerName = answerLocation.getName();
        assertEquals(expectedName, answerName);

        String expectedStreet = expectedLocation.getStreet();
        String answerStreet = answerLocation.getStreet();
        assertEquals(expectedStreet, answerStreet);

        to_json = "{\"order\":0,\"status\":\"V\",\"location\":{\"type\":\"i\",\"number\":\"\",\"street\":\"Bonifácio Avilés\",\"unit\":null,\"neighborhood\":null,\"city\":\"Tijuana\",\"state\":\"BCN\",\"zipcode\":\"\",\"country\":\"MX\",\"waypoint\":{\"latitude\":\"32.51543632662701\",\"longitude\":\"-117.03812250149775\"},\"updated_on\":\"2018-11-14T22:33:46.055339Z\",\"pending_at\":\"2018-11-14T22:33:46.054955Z\",\"started_at\":\"2018-11-14T22:34:50.329321Z\",\"ended_at\":null,\"comment\":null,\"updated_by\":1,\"updated_on\":\"2018-11-14T22:34:50.329428Z\"}}";
        from_json = gson.fromJson(to_json, Waypoint.class);

        expectedOrder = waypoint.getOrder();
        answerOrder = from_json.getOrder();
        assertEquals(expectedOrder, answerOrder);

        expectedVisited = waypoint.isVisited();
        answerVisited = from_json.isVisited();
        assertEquals(expectedVisited, answerVisited);

        expectedLocation = waypoint.getLocation();
        answerLocation = from_json.getLocation();

        expectedName = expectedLocation.getName();
        answerName = answerLocation.getName();
        assertEquals(expectedName, answerName);

        expectedStreet = expectedLocation.getStreet();
        answerStreet = answerLocation.getStreet();
        assertEquals(expectedStreet, answerStreet);
    }

    @Test
    public void test_ambulance_call() {

        Waypoint waypoint = new Waypoint(0,Waypoint.STATUS_CREATED,
                new Location(null, Location.TYPE_INCIDENT,"O",
                        "Bonifácio Avilés", null, null,
                        "Tijuana","BCN","" ,"MX",
                        new GPSLocation(32.51543632662701,-117.03812250149775)));
        List<Waypoint> waypointSet = new ArrayList<>();
        waypointSet.add(waypoint);
        
        AmbulanceCall ambulanceCall = new AmbulanceCall(1,2,AmbulanceCall.STATUS_SUSPENDED, "", 1, new Date(), waypointSet);

        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
        Gson gson = gsonBuilder.create();

        String to_json = gson.toJson(ambulanceCall);

        AmbulanceCall from_json = gson.fromJson(to_json, AmbulanceCall.class);
        
        int expectedId = ambulanceCall.getId();
        int answerId = from_json.getId();
        assertEquals(expectedId, answerId);

        int expectedAmbulanceId = ambulanceCall.getAmbulanceId();
        int answerAmbulanceId = from_json.getAmbulanceId();
        assertEquals(expectedAmbulanceId, answerAmbulanceId);
        
        String expectedStatus = ambulanceCall.getStatus();
        String answerStatus = from_json.getStatus();
        assertEquals(expectedStatus, answerStatus);

        Waypoint expectedWaypoint = ambulanceCall.getWaypointSet().get(0);
        Waypoint answerWaypoint = from_json.getWaypointSet().get(0);
        
        int expectedOrder = expectedWaypoint.getOrder();
        int answerOrder = answerWaypoint.getOrder();
        assertEquals(expectedOrder, answerOrder);

        boolean expectedVisited = expectedWaypoint.isVisited();
        boolean answerVisited = answerWaypoint.isVisited();
        assertEquals(expectedVisited, answerVisited);

        Location expectedLocation = expectedWaypoint.getLocation();
        Location answerLocation = answerWaypoint.getLocation();

        String expectedName = expectedLocation.getName();
        String answerName = answerLocation.getName();
        assertEquals(expectedName, answerName);

        String expectedStreet = expectedLocation.getStreet();
        String answerStreet = answerLocation.getStreet();
        assertEquals(expectedStreet, answerStreet);

        DateFormat df = new SimpleDateFormat("MMM d, y, K:mm:ss a");

        to_json = "{\"id\":1,\"ambulance_id\":2,\"status\":\"S\",\"updated_on\":\"" + df.format(ambulanceCall.getUpdatedOn()) + "\",\"waypoint_set\":[{\"order\":0,\"status\":\"C\",\"location\":{\"type\":\"i\",\"number\":\"\",\"street\":\"Bonifácio Avilés\",\"unit\":null,\"neighborhood\":null,\"city\":\"Tijuana\",\"state\":\"BCN\",\"zipcode\":\"\",\"country\":\"MX\",\"waypoint\":{\"latitude\":\"32.51543632662701\",\"longitude\":\"-117.03812250149775\"},\"updated_on\":\"2018-11-14T22:33:46.055339Z\",\"pending_at\":\"2018-11-14T22:33:46.054955Z\",\"started_at\":\"2018-11-14T22:34:50.329321Z\",\"ended_at\":null,\"comment\":null,\"updated_by\":1,\"updated_on\":\"2018-11-14T22:34:50.329428Z\"}}]}";
        from_json = gson.fromJson(to_json, AmbulanceCall.class);
        System.out.println("to_json = '" + to_json + "'");

        expectedId = ambulanceCall.getId();
        answerId = from_json.getId();
        assertEquals(expectedId, answerId);

        expectedAmbulanceId = ambulanceCall.getAmbulanceId();
        answerAmbulanceId = from_json.getAmbulanceId();
        assertEquals(expectedAmbulanceId, answerAmbulanceId);

        expectedStatus = ambulanceCall.getStatus();
        answerStatus = from_json.getStatus();
        assertEquals(expectedStatus, answerStatus);

        expectedWaypoint = ambulanceCall.getWaypointSet().get(0);
        answerWaypoint = from_json.getWaypointSet().get(0);

        expectedOrder = expectedWaypoint.getOrder();
        answerOrder = answerWaypoint.getOrder();
        assertEquals(expectedOrder, answerOrder);

        expectedVisited = expectedWaypoint.isVisited();
        answerVisited = answerWaypoint.isVisited();
        assertEquals(expectedVisited, answerVisited);

        expectedLocation = expectedWaypoint.getLocation();
        answerLocation = answerWaypoint.getLocation();

        expectedName = expectedLocation.getName();
        answerName = answerLocation.getName();
        assertEquals(expectedName, answerName);

        expectedStreet = expectedLocation.getStreet();
        answerStreet = answerLocation.getStreet();
        assertEquals(expectedStreet, answerStreet);

    }

    @Test
    public void test_call() {

        List<Patient> patientSet = new ArrayList<>();
        patientSet.add(new Patient(31, "Maria",0));
        patientSet.add(new Patient(30, "Jose",13));

        Waypoint waypoint = new Waypoint(0,Waypoint.STATUS_CREATED,
                new Location(null, Location.TYPE_INCIDENT,"O",
                        "Bonifácio Avilés", null, null,
                        "Tijuana","BCN","" ,"MX",
                        new GPSLocation(32.51543632662701,-117.03812250149775)));
        List<Waypoint> waypointSet = new ArrayList<>();
        waypointSet.add(waypoint);

        AmbulanceCall ambulanceCall = new AmbulanceCall(1,2,AmbulanceCall.STATUS_SUSPENDED, "", 1, new Date(), waypointSet);
        List<AmbulanceCall> ambulanceCallSet = new ArrayList<>();
        ambulanceCallSet.add(ambulanceCall);

        List<CallNote> callNoteSet = new ArrayList<>();
        CallNote callNote = new CallNote("new note after call", "user", 1, Calendar.getInstance());
        callNoteSet.add(callNote);
        CallNote secondCallNote = new CallNote("note made after creation of call", "user", 1, Calendar.getInstance());
        callNoteSet.add(secondCallNote);

        Call call = new Call(
                64,
                Call.STATUS_STARTED, "ads asd",
                null,null,null,null,
                null,"",1, null,
                ambulanceCallSet, patientSet, callNoteSet);

        double epsilon = 1e-4;

        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeHierarchyAdapter(Calendar.class, new CalendarDateTypeAdapter());
        gsonBuilder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
        Gson gson = gsonBuilder.create();

        String to_json = gson.toJson(call);

        Call from_json = gson.fromJson(to_json, Call.class);
        System.out.println("to_json = " + to_json + "'");

        Integer expectedId = call.getId();
        Integer answerId = from_json.getId();
        assertEquals(expectedId, answerId);

        String expectedString = call.getDetails();
        String answerString = from_json.getDetails();
        assertEquals(expectedString, answerString);

        AmbulanceCall expectedAmbulanceCall = call.getAmbulancecallSet().get(0);
        AmbulanceCall answerAmbulanceCall = from_json.getAmbulancecallSet().get(0);

        expectedId = expectedAmbulanceCall.getId();
        answerId = answerAmbulanceCall.getId();
        assertEquals(expectedId, answerId);

        int expectedAmbulanceId = expectedAmbulanceCall.getAmbulanceId();
        int answerAmbulanceId = answerAmbulanceCall.getAmbulanceId();
        assertEquals(expectedAmbulanceId, answerAmbulanceId);

        String expectedStatus = expectedAmbulanceCall.getStatus();
        String answerStatus = answerAmbulanceCall.getStatus();
        assertEquals(expectedStatus, answerStatus);

        Waypoint expectedWaypoint = expectedAmbulanceCall.getWaypointSet().get(0);
        Waypoint answerWaypoint = answerAmbulanceCall.getWaypointSet().get(0);

        int expectedOrder = expectedWaypoint.getOrder();
        int answerOrder = answerWaypoint.getOrder();
        assertEquals(expectedOrder, answerOrder);

        boolean expectedVisited = expectedWaypoint.isVisited();
        boolean answerVisited = answerWaypoint.isVisited();
        assertEquals(expectedVisited, answerVisited);

        Location expectedLocation = expectedWaypoint.getLocation();
        Location answerLocation = answerWaypoint.getLocation();

        String expectedName = expectedLocation.getName();
        String answerName = answerLocation.getName();
        assertEquals(expectedName, answerName);

        String expectedStreet = expectedLocation.getStreet();
        String answerStreet = answerLocation.getStreet();
        assertEquals(expectedStreet, answerStreet);

        DateFormat df = new SimpleDateFormat("MMM d, y, K:mm:ss a");
        String ambulance_call_json = "{\"id\":1,\"ambulance_id\":2,\"status\":\"S\",\"updated_on\":\"" + df.format(ambulanceCall.getUpdatedOn()) + "\",\"waypoint_set\":[{\"order\":0,\"status\":\"C\",\"location\":{\"type\":\"i\",\"number\":\"\",\"street\":\"Bonifácio Avilés\",\"unit\":null,\"neighborhood\":null,\"city\":\"Tijuana\",\"state\":\"BCN\",\"zipcode\":\"\",\"country\":\"MX\",\"waypoint\":{\"latitude\":\"32.51543632662701\",\"longitude\":\"-117.03812250149775\"},\"updated_on\":\"2018-11-14T22:33:46.055339Z\",\"pending_at\":\"2018-11-14T22:33:46.054955Z\",\"started_at\":\"2018-11-14T22:34:50.329321Z\",\"ended_at\":null,\"comment\":null,\"updated_by\":1,\"updated_on\":\"2018-11-14T22:34:50.329428Z\"}}]}";
        to_json = "{\"id\":64,\"status\":\"S\",\"details\":\"ads asd\",\"priority\":\"O\",\"updated_on\":\"2018-11-14T22:33:46.055339Z\",\"pending_at\":\"2018-11-14T22:33:46.054955Z\",\"started_at\":\"2018-11-14T22:34:50.329321Z\",\"ended_at\":null,\"comment\":null,\"updated_by\":1,\"updated_on\":\"2018-11-14T22:34:50.329428Z\",\"ambulancecall_set\":[" + ambulance_call_json + "],\"patient_set\":[{\"id\":31,\"name\":\"Maria\",\"age\":null},{\"id\":30,\"name\":\"Jose\",\"age\":13}],\n" +
                "    \"callnote_set\": [\n" +
                "        {\n" +
                "            \"comment\": \"new note after call\",\n" +
                "            \"updated_by\": 1,\n" +
                "            \"updated_on\": \"" + df.format(callNote.getUpdatedOn().getTime()) + "\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"comment\": \"note made after creation of call\",\n" +
                "            \"updated_by\": 1,\n" +
                "            \"updated_on\": \"" + df.format(secondCallNote.getUpdatedOn().getTime()) + "\"\n" +
                "        }\n" +
                "    ]}";

        from_json = gson.fromJson(to_json, Call.class);
        System.out.println("to_json = " + to_json + "'");

        //CallNote testing
        assertEquals(call.getCallnoteSet().size(), from_json.getCallnoteSet().size());

        // call note 0
        CallNote expectedCallNote = call.getCallnoteSet().get(0);
        CallNote answerCallNote = from_json.getCallnoteSet().get(0);

        String expectedCallNoteComment = expectedCallNote.getComment();
        String answerCallNoteComment = answerCallNote.getComment();
        assertEquals(expectedCallNoteComment, answerCallNoteComment);

        expectedId = expectedCallNote.getUpdatedBy();
        answerId = answerCallNote.getUpdatedBy();
        assertEquals(expectedId, answerId);

        Calendar expectedDate = expectedCallNote.getUpdatedOn();
        Calendar answerDate = answerCallNote.getUpdatedOn();
        assertEquals(df.format(expectedDate.getTime()), df.format(answerDate.getTime()));

        // call note 1
        expectedCallNote = call.getCallnoteSet().get(1);
        answerCallNote = from_json.getCallnoteSet().get(1);

        expectedCallNoteComment = expectedCallNote.getComment();
        answerCallNoteComment = answerCallNote.getComment();
        assertEquals(expectedCallNoteComment, answerCallNoteComment);

        expectedId = expectedCallNote.getUpdatedBy();
        answerId = answerCallNote.getUpdatedBy();
        assertEquals(expectedId, answerId);

        expectedDate = expectedCallNote.getUpdatedOn();
        answerDate = answerCallNote.getUpdatedOn();
        assertEquals(df.format(expectedDate.getTime()), df.format(answerDate.getTime()));

        // test call

        expectedId = call.getId();
        answerId = from_json.getId();
        assertEquals(expectedId, answerId);

        expectedString = call.getDetails();
        answerString = from_json.getDetails();
        assertEquals(expectedString, answerString);

        expectedAmbulanceCall = call.getAmbulancecallSet().get(0);
        answerAmbulanceCall = from_json.getAmbulancecallSet().get(0);

        expectedId = expectedAmbulanceCall.getId();
        answerId = answerAmbulanceCall.getId();
        assertEquals(expectedId, answerId);

        expectedAmbulanceId = expectedAmbulanceCall.getAmbulanceId();
        answerAmbulanceId = answerAmbulanceCall.getAmbulanceId();
        assertEquals(expectedAmbulanceId, answerAmbulanceId);

        expectedStatus = expectedAmbulanceCall.getStatus();
        answerStatus = answerAmbulanceCall.getStatus();
        assertEquals(expectedStatus, answerStatus);

        expectedWaypoint = expectedAmbulanceCall.getWaypointSet().get(0);
        answerWaypoint = answerAmbulanceCall.getWaypointSet().get(0);

        expectedOrder = expectedWaypoint.getOrder();
        answerOrder = answerWaypoint.getOrder();
        assertEquals(expectedOrder, answerOrder);

        expectedVisited = expectedWaypoint.isVisited();
        answerVisited = answerWaypoint.isVisited();
        assertEquals(expectedVisited, answerVisited);

        expectedLocation = expectedWaypoint.getLocation();
        answerLocation = answerWaypoint.getLocation();

        expectedName = expectedLocation.getName();
        answerName = answerLocation.getName();
        assertEquals(expectedName, answerName);

        expectedStreet = expectedLocation.getStreet();
        answerStreet = answerLocation.getStreet();
        assertEquals(expectedStreet, answerStreet);

    }

    @Test
    public void test_ambulance_equipment() {

        EquipmentItem equipment = new EquipmentItem(1,
                2, "gauze",'I',
                "20", "2 packages",
                1, new Date());

        Gson gson = new Gson();

        String to_json = gson.toJson(equipment);

        EquipmentItem from_json = gson.fromJson(to_json, EquipmentItem.class);

        Integer expectedId = equipment.getEquipmentHolderId();
        Integer answerId = from_json.getEquipmentHolderId();
        assertEquals(expectedId, answerId);

        expectedId = equipment.getEquipmentId();
        answerId = from_json.getEquipmentId();
        assertEquals(expectedId, answerId);

        String expectedName = equipment.getEquipmentName();
        String answerName = from_json.getEquipmentName();
        assertEquals(expectedName, answerName);

        Character expectedType = equipment.getEquipmentType();
        Character answerType = from_json.getEquipmentType();
        assertEquals(expectedType, answerType);

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
