package nl.gmt.data.migrate;

import nl.gmt.data.schema.*;
import org.apache.commons.lang.StringUtils;

public class DataSchemaField {
    private String name;
    private int length;
    private int positions;
    private SchemaDbType type;
    private boolean nullable;
    private boolean unsigned;
    private boolean autoIncrement;
    private boolean hasDefault;
    private String defaultValue;
    private String characterSet;
    private String collation;

    public static DataSchemaField createIdField(Schema schema, SchemaClass klass) throws SchemaMigrateException {
        DataSchemaField result = new DataSchemaField();

        result.name = klass.getResolvedIdProperty().getResolvedDbIdName();
        result.autoIncrement = klass.getResolvedIdProperty().getAutoIncrement() == SchemaIdAutoIncrement.YES;

        result.initializeType(klass.getResolvedIdProperty().getResolvedDataType());

        return result;
    }

    public static DataSchemaField createProperty(SchemaProperty field) throws SchemaMigrateException {
        DataSchemaField result = new DataSchemaField();

        result.name = field.getResolvedDbName();
        result.autoIncrement = false;

        result.initializeType(field.getResolvedDataType());

        return result;
    }

    public static DataSchemaField createFromForeignParent(SchemaForeignParent foreign, Schema schema) throws SchemaMigrateException {
        DataSchemaField result = new DataSchemaField();

        result.name = foreign.getResolvedDbName();
        result.autoIncrement = false;

        result.initializeType(
            schema.getClasses().get(foreign.getClassName()).getResolvedIdProperty().getResolvedDataType()
        );

        if (foreign.getAllowNull() != SchemaAllowNull.UNSET)
            result.nullable = foreign.getAllowNull() == SchemaAllowNull.ALLOW;

        return result;
    }

    private void initializeType(SchemaResolvedDataType dataType) throws SchemaMigrateException {
        length = -1;
        positions = -1;
        unsigned = false;
        hasDefault = false;
        defaultValue = null;

        type = dataType.getDbType();

        switch (dataType.getAllowNull()) {
            case ALLOW:
                nullable = true;
                break;

            case DISALLOW:
                nullable = false;
                break;

            default:
                throw new SchemaMigrateException(String.format("Schema data type '%s' without nullable", dataType.getName()), dataType.getLocation());
        }

        switch (type) {
            case TINY_INT:
            case SMALL_INT:
            case MEDIUM_INT:
            case INT:
            case BIG_INT:
            case FLOAT:
            case DOUBLE:
            case DECIMAL:
                switch (dataType.getSigned()) {
                    case UNSIGNED:
                        unsigned = true;
                        break;

                    case SIGNED:
                        unsigned = false;
                        break;

                    default:
                        throw new SchemaMigrateException(String.format("Schema data type '%s' without signed", dataType.getName()), dataType.getLocation());
                }
        }

        length = dataType.getLength();
        positions = dataType.getPositions();
    }

    public boolean equals(DataSchemaField other, SchemaRules rules) throws SchemaMigrateException {
        if (this == other)
            return true;

        return
            StringUtils.equalsIgnoreCase(name, other.name) &&
            nullable == other.nullable &&
            autoIncrement == other.autoIncrement &&
            rules.dbTypesEqual(this, other) && (
                !rules.dbTypeSupportsCharset(type) || (
                    StringUtils.equalsIgnoreCase(characterSet, other.characterSet) &&
                    StringUtils.equalsIgnoreCase(collation, other.collation)
                )
            );
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public int getPositions() {
        return positions;
    }

    public void setPositions(int positions) {
        this.positions = positions;
    }

    public SchemaDbType getType() {
        return type;
    }

    public void setType(SchemaDbType type) {
        this.type = type;
    }

    public boolean isNullable() {
        return nullable;
    }

    public void setNullable(boolean nullable) {
        this.nullable = nullable;
    }

    public boolean isUnsigned() {
        return unsigned;
    }

    public void setUnsigned(boolean unsigned) {
        this.unsigned = unsigned;
    }

    public boolean isAutoIncrement() {
        return autoIncrement;
    }

    public void setAutoIncrement(boolean autoIncrement) {
        this.autoIncrement = autoIncrement;
    }

    public boolean isHasDefault() {
        return hasDefault;
    }

    public void setHasDefault(boolean hasDefault) {
        this.hasDefault = hasDefault;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getCharacterSet() {
        return characterSet;
    }

    public void setCharacterSet(String characterSet) {
        this.characterSet = characterSet;
    }

    public String getCollation() {
        return collation;
    }

    public void setCollation(String collation) {
        this.collation = collation;
    }
}
