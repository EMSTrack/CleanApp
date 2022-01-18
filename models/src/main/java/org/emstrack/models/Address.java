package org.emstrack.models;

import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.model.AddressComponent;
import com.google.android.libraries.places.api.model.AddressComponents;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A class representing an address.
 *
 * @author mauricio
 * @since 3/11/2018.
 */
public class Address {

    private static final String TAG = Address.class.getSimpleName();

    private String number;
    private String street;
    private String unit;
    private String neighborhood;
    private String city;
    private String state;
    private String zipcode;
    private String country;
    private GPSLocation location;

    /**
     * An address without a GPS location.
     *
     * @param number       the address number
     * @param street       the address street
     * @param unit         the address unit
     * @param neighborhood the address neighborhood
     * @param city         the address city
     * @param state        the address state (3 characters max)
     * @param zipcode      the address zipcode
     * @param country      the address country code (2 characters max)
     */
    public Address(String number,
                   String street,
                   String unit,
                   String neighborhood,
                   String city,
                   String state,
                   String zipcode,
                   String country) {
        this.number = number;
        this.street = street;
        this.unit = unit;
        this.neighborhood = neighborhood;
        this.city = city;
        this.state = state;
        this.zipcode = zipcode;
        this.country = country;
    }

    /**
     * An address with a GPS location.
     *
     * @param number       the address number
     * @param street       the address street
     * @param unit         the address unit
     * @param neighborhood the address neighborhood
     * @param city         the address city
     * @param state        the address state (3 characters max)
     * @param zipcode      the address zipcode
     * @param country      the address country code (2 characters max)
     * @param location     the address GPS location
     */
    public Address(String number,
                   String street,
                   String unit,
                   String neighborhood,
                   String city,
                   String state,
                   String zipcode,
                   String country,
                   GPSLocation location) {
        this.number = number;
        this.street = street;
        this.unit = unit;
        this.neighborhood = neighborhood;
        this.city = city;
        this.state = state;
        this.zipcode = zipcode;
        this.country = country;
        this.location = location;
    }

    /**
     * An address with only a GPS location.
     *
     * @param location the address GPS location
     */
    public Address(GPSLocation location) {
        this.number = "";
        this.street = "";
        this.unit = "";
        this.neighborhood = "";
        this.city = "";
        this.state = "";
        this.zipcode = "";
        this.country = "";
        this.location = location;
    }


    /**
     * Copy constructor
     *
     * @param address an address
     */
    public Address(Address address) {
        this.number = address.number;
        this.street = address.street;
        this.unit = address.unit;
        this.neighborhood = address.neighborhood;
        this.city = address.city;
        this.state = address.state;
        this.zipcode = address.zipcode;
        this.country = address.country;
        this.location = address.location;
    }

    /**
     * @return the address number
     */
    public String getNumber() {
        return number;
    }

    /**
     * @param number the address number
     */
    public void setNumber(String number) {
        this.number = number;
    }

    /**
     * @return the address street
     */
    public String getStreet() {
        return street;
    }

    /**
     * @param street the address street
     */
    public void setStreet(String street) {
        this.street = street;
    }

    /**
     * @return the address unit
     */
    public String getUnit() {
        return unit;
    }

    /**
     * @param unit the address unit
     */
    public void setUnit(String unit) {
        this.unit = unit;
    }

    /**
     * @return the address neighborhood
     */
    public String getNeighborhood() {
        return neighborhood;
    }

    /**
     * @param neighborhood the address neighborhood
     */
    public void setNeighborhood(String neighborhood) {
        this.neighborhood = neighborhood;
    }

    /**
     * @return the address city
     */
    public String getCity() {
        return city;
    }

    /**
     * @param city the address city
     */
    public void setCity(String city) {
        this.city = city;
    }

    /**
     * @return the address state
     */
    public String getState() {
        return state;
    }

    /**
     * @param state the address state
     */
    public void setState(String state) {
        this.state = state;
    }

    /**
     * @return the address zipcode
     */
    public String getZipcode() {
        return zipcode;
    }

    /**
     * @param zipcode the address zipcode
     */
    public void setZipcode(String zipcode) {
        this.zipcode = zipcode;
    }

    /**
     * @return the address country
     */
    public String getCountry() {
        return country;
    }

    /**
     * @param country the address country
     */
    public void setCountry(String country) {
        this.country = country;
    }

    /**
     * @return the address GPS location
     */
    public GPSLocation getLocation() {
        return location;
    }

    /**
     * @param location the address GPS location
     */
    public void setLocation(GPSLocation location) {
        this.location = location;
    }

    /**
     * @return a string representation of the address
     */
    @NonNull
    public String toString() {

        // TODO: Take into account the locale

        String retValue = "";

        // street address
        retValue += this.number + " " + this.street;
        if (this.unit != null && !this.unit.isEmpty())
            retValue += " " + this.unit;
        if (this.neighborhood != null && !this.neighborhood.isEmpty()) {
            retValue += ", " + this.neighborhood;
        }
        if (this.city != null && !this.city.isEmpty())
            retValue += "\n" + this.city;
        if (this.state != null && !this.state.isEmpty())
            retValue += ", " + this.state;
        if (this.zipcode != null && !this.zipcode.isEmpty())
            retValue += " " + this.zipcode;
        if (this.country != null && !this.country.isEmpty())
            retValue += ", " + this.country;

        return retValue.trim();

    }

    enum AddressComponentType {
        NUMBER,
        STREET,
        NEIGHBORHOOD,
        STATE,
        CITY,
        COUNTRY,
        ZIPCODE
    };

    private static final Object[][] addressComponentsArray = new Object[][] {
            {AddressComponentType.NUMBER, new String[] {"street_number"}},
            {AddressComponentType.STREET, new String[] {"street_address", "route"}},
            {AddressComponentType.STATE, new String[] {"administrative_area_level_1", "administrative_area_level_2", "administrative_area_level_3", "administrative_area_level_4", "administrative_area_level_5"}},
            {AddressComponentType.NEIGHBORHOOD, new String[] {"neighborhood", "sublocality", "sublocality_level_1"}},
            {AddressComponentType.CITY, new String[] {"locality"}},
            {AddressComponentType.COUNTRY, new String[] {"country"}},
            {AddressComponentType.ZIPCODE, new String[] {"postal_code"}},
    };
    private static Map<AddressComponentType, String[]> addressComponentTypeMap;
    private static Map<AddressComponentType, String[]> getAddressComponentTypeMap() {
        if (addressComponentTypeMap == null) {
            // initialize
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                addressComponentTypeMap = Stream
                        .of(addressComponentsArray)
                        .collect(Collectors
                                .toMap(data -> (AddressComponentType) data[0],
                                        data -> (String[]) data[1]));
            } else {
                addressComponentTypeMap = new HashMap<>();
                for (Object[] data: addressComponentsArray) {
                    addressComponentTypeMap.put((AddressComponentType) data[0], (String[]) data[1]);
                }
            }
        }
        return addressComponentTypeMap;
    }

    private static String matchFirstType(List<String> addressComponentTypes, String[] possibleTypes) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Arrays.stream(possibleTypes)
                    .filter(addressComponentTypes::contains)
                    .findFirst()
                    .orElse(null);
        } else {
            for (String possibleType: possibleTypes) {
                if (addressComponentTypes.contains(possibleType)) {
                    return possibleType;
                }
            }
            return null;
        }
    }

    public static Address parseAddressComponents(LatLng latLng, AddressComponents addressComponents) {

        // add gps location
        Address location = new Address(new GPSLocation(latLng));

        // parse fields
        Map<AddressComponentType, String[]> componentTypeMap = getAddressComponentTypeMap();
        for (AddressComponent component: addressComponents.asList()) {
            List<String> types = component.getTypes();
            Log.d(TAG, String.format("address component: %s", component));

            // match number, street, city, country, and postal code
            if (matchFirstType(types, componentTypeMap.get(AddressComponentType.NUMBER)) != null) {
                location.setNumber(component.getName());
            } else if (matchFirstType(types, componentTypeMap.get(AddressComponentType.STREET)) != null) {
                location.setStreet(component.getName());
            } else if (matchFirstType(types, componentTypeMap.get(AddressComponentType.NEIGHBORHOOD)) != null) {
                location.setNeighborhood(component.getName());
            } else if (matchFirstType(types, componentTypeMap.get(AddressComponentType.CITY)) != null) {
                location.setCity(component.getName());
            } else if (matchFirstType(types, componentTypeMap.get(AddressComponentType.STATE)) != null) {
                String state = component.getShortName();
                if (state != null) {
                    location.setState(state.replace(".", ""));
                } else {
                    location.setState(component.getName());
                }
            } else if (matchFirstType(types, componentTypeMap.get(AddressComponentType.COUNTRY)) != null) {
                location.setCountry(component.getShortName());
            } else if (matchFirstType(types, componentTypeMap.get(AddressComponentType.ZIPCODE)) != null) {
                location.setZipcode(component.getShortName());
            }
        }

        return location;
    }

}