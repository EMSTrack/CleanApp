package org.emstrack.models.util;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class StateHelper {

    public static Map<String, Map<String, String>> states = new HashMap<>();

    public static String getStateAbbreviation(String country, String state, Locale locale) {
        Map<String, String> countryMap = states.get(country);
        if (countryMap != null) {
            return countryMap.get(state.toLowerCase(locale));
        }
        return null;
    }

    static {
        // Initialize
        Map<String, String> mexico = new HashMap<>();
        mexico.put("aguascalientes","AG");
        mexico.put("baja california","BC");
        mexico.put("baja california Sur","BS");
        mexico.put("campeche","CM");
        mexico.put("chiapas","CS");
        mexico.put("chihuahua","CH");
        mexico.put("coahuila","CO");
        mexico.put("colima","CL");
        mexico.put("mexico city","DF");
        mexico.put("durango","DG");
        mexico.put("guanajuato","GT");
        mexico.put("guerrero","GR");
        mexico.put("hidalgo","HG");
        mexico.put("jalisco","JA");
        mexico.put("méxico","EM");
        mexico.put("michoacán","MI");
        mexico.put("morelos","MO");
        mexico.put("nayarit","NA");
        mexico.put("nuevo León","NL");
        mexico.put("oaxaca","OA");
        mexico.put("puebla","PU");
        mexico.put("querétaro","QT");
        mexico.put("quintana Roo","QR");
        mexico.put("san luis potosí","SL");
        mexico.put("sinaloa","SI");
        mexico.put("sonora","SO");
        mexico.put("tabasco","TB");
        mexico.put("tamaulipas","TM");
        mexico.put("tlaxcala","TL");
        mexico.put("veracruz","VE");
        mexico.put("yucatán","YU");
        mexico.put("zacatecas","ZA");
        states.put("MX", mexico);

        Map<String, String> usa = new HashMap<>();
        usa.put("alabama", "AL");
        usa.put("alaska", "AK");
        usa.put("american samoa", "AS");
        usa.put("arizona", "AZ");
        usa.put("arkansas", "AR");
        usa.put("california", "CA");
        usa.put("colorado", "CO");
        usa.put("connecticut", "CT");
        usa.put("delaware", "DE");
        usa.put("district of columbia", "DC");
        usa.put("florida", "FL");
        usa.put("georgia", "GA");
        usa.put("guam", "GU");
        usa.put("hawaii", "HI");
        usa.put("idaho", "ID");
        usa.put("illinois", "IL");
        usa.put("indiana", "IN");
        usa.put("iowa", "IA");
        usa.put("kansas", "KS");
        usa.put("kentucky", "KY");
        usa.put("louisiana", "LA");
        usa.put("maine", "ME");
        usa.put("maryland", "MD");
        usa.put("massachusetts", "MA");
        usa.put("michigan", "MI");
        usa.put("minnesota", "MN");
        usa.put("mississippi", "MS");
        usa.put("missouri", "MO");
        usa.put("montana", "MT");
        usa.put("nebraska", "NE");
        usa.put("nevada", "NV");
        usa.put("new hampshire", "NH");
        usa.put("new jersey", "NJ");
        usa.put("new mexico", "NM");
        usa.put("new york", "NY");
        usa.put("north carolina", "NC");
        usa.put("north dakota", "ND");
        usa.put("ohio", "OH");
        usa.put("oklahoma", "OK");
        usa.put("oregon", "OR");
        usa.put("pennsylvania", "PA");
        usa.put("puerto rico", "PR");
        usa.put("RHODE ISLAND", "RI");
        usa.put("south carolina", "SC");
        usa.put("south dakota", "SD");
        usa.put("tennessee", "TN");
        usa.put("texas", "TX");
        usa.put("utah", "UT");
        usa.put("vermont", "VT");
        usa.put("virginia", "VA");
        usa.put("virgin islands", "VI");
        usa.put("washington", "WA");
        usa.put("west virginia", "WV");
        usa.put("wisconsin", "WI");
        usa.put("wyoming", "WY");
        states.put("US", usa);
    };

}
