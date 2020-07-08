package org.emstrack.models.api;

import com.google.gson.JsonObject;

public class APIError extends Throwable {

    private JsonObject json;

    public APIError() {
    }

    public APIError(JsonObject json) {
        this.json = json;
    }

    public JsonObject getJson() {
        return json;
    }

    @Override
    public String toString() {
        return json.toString();
    }
}