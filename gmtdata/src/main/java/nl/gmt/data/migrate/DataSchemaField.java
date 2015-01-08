package nl.gmt.data.migrate;

import nl.gmt.data.schema.*;
import org.apache.commons.lang3.StringUtils;

public class DataSchemaField extends DataSchemaObject {
    private String name;
    private int length;
    private int positions;
    private SchemaDbType type;
    private boolean nullable;
    private boolean unsigned;
    private boolean autoIncrement;
    private boolean hasDefault;
    private String defaultValue;

    void initializeType(SchemaResolvedDataType dataType) throws SchemaMigrateException {
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
            rules.dbTypesEqual(this, other) &&
            DataSchemaTable.extensionsEquals(this, other);
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
}
