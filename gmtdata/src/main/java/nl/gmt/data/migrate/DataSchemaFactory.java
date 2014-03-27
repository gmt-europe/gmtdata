package nl.gmt.data.migrate;

import nl.gmt.data.schema.Schema;
import nl.gmt.data.schema.SchemaClass;
import nl.gmt.data.schema.SchemaRules;

public interface DataSchemaFactory {
    DataSchemaTable createClass(SchemaClass klass, Schema schema, SchemaRules rules) throws SchemaMigrateException;
}
