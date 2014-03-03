package nl.gmt.data.migrate;

import nl.gmt.data.schema.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataSchemaTable {
    private String name;
    private final Map<String, DataSchemaField> fields;
    private final List<DataSchemaForeignKey> foreignKeys;
    private List<DataSchemaIndex> indexes;
    private String engine;
    private String defaultCharset;
    private String defaultCollation;
    private int autoIncrement;

    public DataSchemaTable() {
        fields = new HashMap<>();
        foreignKeys = new ArrayList<>();
        indexes = new ArrayList<>();
    }

    public static DataSchemaTable createFromSchemaClass(SchemaClass klass, Schema schema, SqlGenerator generator) throws SchemaMigrateException {
        DataSchemaTable result = new DataSchemaTable();

        result.name = klass.getResolvedDbName();

        SchemaMySqlSettings mySqlSettings = schema.getMySqlSettings();
        if (mySqlSettings != null) {
            result.engine = mySqlSettings.getEngine();
            result.defaultCharset = mySqlSettings.getCharset();
            result.defaultCollation = mySqlSettings.getCollation();

            if (result.defaultCollation == null)
                result.defaultCollation = generator.getDefaultCollation(result.defaultCharset);
        }

        result.autoIncrement = -1;

        // Add all fields

        result.addField(DataSchemaField.createIdField(schema, klass));

        for (SchemaProperty field : klass.getProperties().values()) {
            result.addField(DataSchemaField.createProperty(field));
        }

        for (SchemaForeignBase foreign : klass.getForeigns().values()) {
            if (foreign.getType() == SchemaForeignType.PARENT)
                result.addField(DataSchemaField.createFromForeignParent((SchemaForeignParent)foreign, schema));
        }

        // Add all indexes

        result.indexes.add(DataSchemaIndex.createPrimaryIndex(klass));

        for (SchemaForeignBase foreign : klass.getForeigns().values()) {
            if (foreign.getType() == SchemaForeignType.PARENT)
                result.indexes.add(DataSchemaIndex.createFromForeignParent((SchemaForeignParent)foreign));
        }

        for (SchemaProperty field : klass.getProperties().values()) {
            SchemaIndexType indexed = field.getIndexed();

            if (indexed == SchemaIndexType.UNSET)
                indexed = field.getResolvedDataType().getIndexed();

            if (indexed != SchemaIndexType.UNSET)
                result.indexes.add(DataSchemaIndex.createFromProperty(field, indexed));
        }

        for (SchemaIndex index : klass.getIndexes()) {
            result.indexes.add(DataSchemaIndex.createFromIndex(index, klass, schema));
        }

        // Add all foreign keys

        for (SchemaForeignBase foreign : klass.getForeigns().values()) {
            if (foreign.getType() == SchemaForeignType.PARENT)
                result.foreignKeys.add(DataSchemaForeignKey.createFromForeignParent((SchemaForeignParent)foreign, schema));
        }

        // Update the charset of all fields.

        for (DataSchemaField field : result.fields.values()) {
            field.setCharacterSet(result.defaultCharset);
            field.setCollation(result.defaultCollation);
        }

        return result;
    }

    public void addField(DataSchemaField field) {
        fields.put(field.getName(), field);
    }

    void stripForeignKeysAndIndexes() {
        foreignKeys.clear();

        List<DataSchemaIndex> oldIndexes = indexes;

        indexes = new ArrayList<>();

        for (DataSchemaIndex index : oldIndexes) {
            if (index.getType() == SchemaIndexType.PRIMARY)
                indexes.add(index);
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, DataSchemaField> getFields() {
        return fields;
    }

    public List<DataSchemaForeignKey> getForeignKeys() {
        return foreignKeys;
    }

    public List<DataSchemaIndex> getIndexes() {
        return indexes;
    }

    public String getEngine() {
        return engine;
    }

    public void setEngine(String engine) {
        this.engine = engine;
    }

    public String getDefaultCharset() {
        return defaultCharset;
    }

    public void setDefaultCharset(String defaultCharset) {
        this.defaultCharset = defaultCharset;
    }

    public String getDefaultCollation() {
        return defaultCollation;
    }

    public void setDefaultCollation(String defaultCollation) {
        this.defaultCollation = defaultCollation;
    }

    public int getAutoIncrement() {
        return autoIncrement;
    }

    public void setAutoIncrement(int autoIncrement) {
        this.autoIncrement = autoIncrement;
    }
}
