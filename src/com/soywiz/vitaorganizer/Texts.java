package com.soywiz.vitaorganizer;

import kotlin.Pair;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.PropertyKey;

import java.util.Map;
import java.util.ResourceBundle;

public class Texts {
    @NonNls
    static private final String BUNDLE = "com.soywiz.vitaorganizer.Texts";

    static private ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE);

    static public String format(@NonNls @PropertyKey(resourceBundle = BUNDLE) String key, Pair<String, ?>... pairs) {
        return TextFormatter.format(bundle.getString(key), (Pair<String, ?>[]) pairs);
    }

    static public String formatMap(@NonNls @PropertyKey(resourceBundle = BUNDLE) String key, Map<String, ?> map) {
        return TextFormatter.format(bundle.getString(key), map);
    }
}
