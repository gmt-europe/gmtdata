package nl.gmt.data.support;

import org.apache.commons.lang3.Validate;

import java.net.MalformedURLException;
import java.net.URL;

public class ReflectionUtils {
    private ReflectionUtils() {
    }

    private static final String WEB_INF_CLASSES_PATH = "WEB-INF/classes/";

    public static URL getUrlFromClass(Class<?> klass) {
        Validate.notNull(klass, "klass");

        URL url = klass.getProtectionDomain().getCodeSource().getLocation();

        // Check for web applications.

        String path = url.toString();
        int pos = path.indexOf(WEB_INF_CLASSES_PATH);

        if (pos != -1) {
            try {
                return new URL(path.substring(0, pos + WEB_INF_CLASSES_PATH.length()));
            } catch (MalformedURLException e) {
                // This shouldn't occur because we have control over the URL we're creating.

                throw new RuntimeException(e);
            }
        }

        return url;
    }
}
