package nl.gmt.data.schema;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SchemaCompositeId extends SchemaElement {
    private final List<SchemaCompositeIdProperty> properties = new ArrayList<>();
    private final List<SchemaCompositeIdProperty> unmodifiableProperties = Collections.unmodifiableList(properties);

    public SchemaCompositeId(SchemaParserLocation location) {
        super(location);
    }

    public List<SchemaCompositeIdProperty> getProperties() {
        return unmodifiableProperties;
    }

    void addProperty(SchemaCompositeIdProperty property) {
        properties.add(property);
    }
}
