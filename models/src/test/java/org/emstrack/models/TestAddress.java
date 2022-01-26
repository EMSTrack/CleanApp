package org.emstrack.models;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Locale;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class TestAddress {

    @Test
    public void test_parseThoroughfare_english() {

        String test = "1600 Amphitheater Parkway";
        String[] result = Address.parseThoroughfare(test, new Locale("es"));
        assertEquals("1600", result[0]);
        assertEquals("Amphitheater Parkway", result[1]);
        assertEquals("", result[2]);

        test = "1600 Amphitheater Parkway 133";
        result = Address.parseThoroughfare(test, new Locale("es"));
        assertEquals("1600", result[0]);
        assertEquals("Amphitheater Parkway", result[1]);
        assertEquals("133", result[2]);

        test = "1600 Amphitheater Parkway #133";
        result = Address.parseThoroughfare(test, new Locale("es"));
        assertEquals("1600", result[0]);
        assertEquals("Amphitheater Parkway", result[1]);
        assertEquals("#133", result[2]);

        test = "1600 Amphitheater Parkway Suite 133";
        result = Address.parseThoroughfare(test, new Locale("es"));
        assertEquals("1600", result[0]);
        assertEquals("Amphitheater Parkway", result[1]);
        assertEquals("Suite 133", result[2]);

        test = "1600 Amphitheater Parkway Suite #133";
        result = Address.parseThoroughfare(test, new Locale("es"));
        assertEquals("1600", result[0]);
        assertEquals("Amphitheater Parkway", result[1]);
        assertEquals("Suite #133", result[2]);

    }

    @Test
    public void test_parseThoroughfare_spanish() {

        String test = "Av Centenario 3000";
        String[] result = Address.parseThoroughfare(test, new Locale("es"));
        assertEquals("3000", result[0]);
        assertEquals("Av Centenario", result[1]);
        assertEquals("", result[2]);

        test = "Av Centenario 3000 ap 34";
        result = Address.parseThoroughfare(test, new Locale("es"));
        assertEquals("3000", result[0]);
        assertEquals("Av Centenario", result[1]);
        assertEquals("ap 34", result[2]);

        test = "Av Centenario 3000/34";
        result = Address.parseThoroughfare(test, new Locale("es"));
        assertEquals("3000", result[0]);
        assertEquals("Av Centenario", result[1]);
        assertEquals("34", result[2]);

        test = "Av Centenario 3000/ 34";
        result = Address.parseThoroughfare(test, new Locale("es"));
        assertEquals("3000", result[0]);
        assertEquals("Av Centenario", result[1]);
        assertEquals("34", result[2]);

    }

}
