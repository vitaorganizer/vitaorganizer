package com.soywiz.vitaorganizer;

import kotlin.Pair;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.PropertyKey;

import java.util.Map;
import java.util.ResourceBundle;

public class Texts {
    @NonNls
    static private final String BUNDLE = "Texts";

    static private ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE);

    static public String format(@NonNls @PropertyKey(resourceBundle = BUNDLE) String key, Pair<String, Object> ...pairs) {
        return TextFormatter.format(bundle.getString(key), pairs);
    }

    static public String formatMap(@NonNls @PropertyKey(resourceBundle = BUNDLE) String key, Map<String, Object> map) {
        return TextFormatter.format(bundle.getString(key), map);
    }
}
