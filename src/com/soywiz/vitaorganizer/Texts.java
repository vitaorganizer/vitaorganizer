package com.soywiz.vitaorganizer;

import kotlin.Pair;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.PropertyKey;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Locale;
import java.util.Map;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

public class Texts {
	@NonNls
	static private final String BUNDLE = "com.soywiz.vitaorganizer.Texts";

	static public final Locale[] SUPPORTED_LOCALES = new Locale[]{
		new Locale("en"),
		new Locale("de"),
		new Locale("es"),
		new Locale("fr"),
		new Locale("it"),
		new Locale("nb"),
		new Locale("pl"),
		new Locale("pt"),
		new Locale("ru"),
		new Locale("zh"),
	};

	static private ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE, Locale.getDefault(), new UTF8Control());

	static public void setLanguage(Locale locale) {
		System.out.println("Locale.getDefault():" + Locale.getDefault() + " : " + locale);
		bundle = ResourceBundle.getBundle(BUNDLE, locale, new UTF8Control());
	}

	static private String getString(String key) {
		try {
			return bundle.getString(key);
		} catch (Throwable e) {
			e.printStackTrace();
			return key;
		}
	}

	// @TODO: Disabled @PropertyKey(resourceBundle) until fixed for kotlin in intelliJ
	// Please, switch the comment line with the other active when adding new texts so it adds to all the .properties files

	//static public String format(@NonNls @PropertyKey(resourceBundle = BUNDLE) String key, Pair<String, ?>... pairs) {
	static public String format(String key, Pair<String, ?>... pairs) {
		return TextFormatter.format(getString(key), (Pair<String, ?>[]) pairs);
	}

	//static public String formatMap(@NonNls @PropertyKey(resourceBundle = BUNDLE) String key, Map<String, ?> map) {
	static public String formatMap(String key, Map<String, ?> map) {
		return TextFormatter.format(getString(key), map);
	}

	static private class UTF8Control extends ResourceBundle.Control {
		public ResourceBundle newBundle
			(String baseName, Locale locale, String format, ClassLoader loader, boolean reload)
			throws IllegalAccessException, InstantiationException, IOException {
			// The below is a copy of the default implementation.
			String bundleName = toBundleName(baseName, locale);
			String resourceName = toResourceName(bundleName, "properties");
			ResourceBundle bundle = null;
			InputStream stream = null;
			if (reload) {
				URL url = loader.getResource(resourceName);
				if (url != null) {
					URLConnection connection = url.openConnection();
					if (connection != null) {
						connection.setUseCaches(false);
						stream = connection.getInputStream();
					}
				}
			} else {
				stream = loader.getResourceAsStream(resourceName);
			}
			if (stream != null) {
				try {
					// Only this line is changed to make it to read properties files as UTF-8.
					bundle = new PropertyResourceBundle(new InputStreamReader(stream, "UTF-8"));
				} finally {
					stream.close();
				}
			}
			return bundle;
		}
	}
}
