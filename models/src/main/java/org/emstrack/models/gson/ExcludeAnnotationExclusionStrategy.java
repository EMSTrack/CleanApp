package org.emstrack.models.gson;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;

// Excludes any field (or class) that is tagged with an "@FooAnnotation"
public class ExcludeAnnotationExclusionStrategy implements ExclusionStrategy {

    @Override
    public boolean shouldSkipClass(Class<?> clazz) {
        return clazz.getAnnotation(Exclude.class) != null;
    }

    public boolean shouldSkipField(FieldAttributes f) {
        return f.getAnnotation(Exclude.class) != null;
    }
}
