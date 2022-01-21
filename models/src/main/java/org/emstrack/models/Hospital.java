package org.emstrack.models;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.Calendar;
import java.util.Locale;

/**
 * A class representing a hospital.
 * @author mauricio
 * @since 3/11/2018
 */
public class Hospital extends NamedAddress {

    private int id;
    private String comment;
    private int updatedBy;
    private Calendar updatedOn;

    public Hospital(int id,
                    String number,
                    String street,
                    String unit,
                    String neighborhood,
                    String city,
                    String state,
                    String zipcode,
                    String country,
                    String name,
                    GPSLocation location,
                    String comment,
                    int updatedBy, Calendar updatedOn
                    ) {
        super(name, number, street, unit, neighborhood, city, state, zipcode, country, location);

        this.id = id;
        this.comment = comment;
        this.updatedBy = updatedBy;
        this.updatedOn = updatedOn;

    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public int getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(int updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Calendar getUpdatedOn() {
        return updatedOn;
    }

    public void setUpdatedOn(Calendar updatedOn) {
        this.updatedOn = updatedOn;
    }

    @NonNull
    @Override
    public String toString() {
        return getName();
    }

    @NonNull
    @Override
    public String toAddress(Context context) {
        return context.getResources().getString(R.string.Hospital).toUpperCase(Locale.getDefault()) +"\n" + super.toString();
    }
}