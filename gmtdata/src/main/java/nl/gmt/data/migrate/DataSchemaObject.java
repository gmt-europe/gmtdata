package nl.gmt.data.migrate;

import org.apache.commons.lang3.Validate;

import java.util.HashMap;
import java.util.Map;

public abstract class DataSchemaObject {
    private Map<String, String> extensions;

    public String getExtension(String name) {
        Validate.notNull(name, "name");

        if (extensions == null) {
            return null;
        }

        return extensions.get(name);
    }

    public void setExtensions(String name, String value) {
        if (extensions == null) {
            extensions = new HashMap<>();
        }

        if (value == null) {
            extensions.remove(name);
        } else {
            extensions.put(name, value);
        }
    }

    public static boolean extensionsEquals(DataSchemaObject a, DataSchemaObject b) {
        Validate.notNull(a, "a");
        Validate.notNull(b, "b");

        int aSize = a.extensions == null ? 0 : a.extensions.size();
        int bSize = b.extensions == null ? 0 : b.extensions.size();

        if (aSize != bSize) {
            return false;
        }

        if (aSize == 0) {
            return true;
        }

        // The below works because we ensure that there are no null values in the map. When an entry in a does not
        // appear b, two things happen: first, get returns null so equals returns false; second, b will have an entry
        // that does not appear in a, because we just check that the number of items are equal.

        for (Map.Entry<String, String> entry : a.extensions.entrySet()) {
            if (!entry.getValue().equals(b.extensions.get(entry.getKey()))) {
                return false;
            }
        }

        return true;
    }
}
