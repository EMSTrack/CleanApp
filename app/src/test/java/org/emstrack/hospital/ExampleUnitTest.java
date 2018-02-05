package org.emstrack.hospital;

import org.junit.Test;

import static org.junit.Assert.*;

import com.google.gson.Gson;

import org.emstrack.hospital.models.Profile;
import org.emstrack.hospital.models.AmbulancePermission;
import org.emstrack.hospital.models.HospitalPermission;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {

    @Test
    public void addition_isCorrect() throws Exception {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void profile_test() throws Exception {

        String json =
                "{" +
                " 'ambulances': [" +
                "   {" +
                "     'ambulance_id':1," +
                "     'ambulance_identifier':'BUD1234'," +
                "     'can_read':true," +
                "     'can_write':true" +
                "   }," +
                "   {" +
                "     'ambulance_id':2," +
                "     'ambulance_identifier':'BUH4321'," +
                "     'can_read':true," +
                "     'can_write':true" +
                "   }," +
                " ]," +
                " 'hospitals': [" +
                "   {" +
                "     'hospital_id':34," +
                "     'hospital_name':'Hospital Nuevo'," +
                "     'can_read':true," +
                "     'can_write':true" +
                "   }," +
                "   {" +
                "     'hospital_id':35," +
                "     'hospital_name':'Hospital Viejo'," +
                "     'can_read':true," +
                "     'can_write':true" +
                "   }" +
                " ]" +
                "}";

        Gson gson = new Gson();

        Profile profile = gson.fromJson(json, Profile.class);

    }

}
