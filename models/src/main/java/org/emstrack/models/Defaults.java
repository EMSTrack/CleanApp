package org.emstrack.models;

/**
 * A class representing global default options.
 * @author mauricio
 * @since 2/8/18
 */
public class Defaults {

        private GPSLocation location;
        private String state;
        private String city;
        private String country;

        public GPSLocation getLocation() {
            return location;
        }

        public void setLocation(GPSLocation location) {
            this.location = location;
        }

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }

        public String getCountry() {
            return country;
        }

        public void setCountry(String country) {
            this.country = country;
        }

        public Defaults(GPSLocation location, String state, String city, String country) {
            this.location = location;
            this.state = state;
            this.city = city;
            this.country = country;
        }
    }