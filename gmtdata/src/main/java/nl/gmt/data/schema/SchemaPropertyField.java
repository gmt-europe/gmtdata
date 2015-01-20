package nl.gmt.data.schema;

import java.util.List;

public interface SchemaPropertyField extends SchemaField {
    String getDbName();

    String getResolvedDbName();

    SchemaResolvedDataType getResolvedDataType();

    String getComments();

    List<String> getTags();
}
