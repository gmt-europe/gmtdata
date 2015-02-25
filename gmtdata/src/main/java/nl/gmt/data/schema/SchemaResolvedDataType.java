package nl.gmt.data.schema;

import nl.gmt.data.migrate.SchemaMigrateException;
import nl.gmt.data.migrate.SqlGenerator;
import org.apache.commons.lang.StringUtils;

public class SchemaResolvedDataType extends SchemaDataTypeBase {
    static SchemaResolvedDataType create(Schema schema, SchemaRules rules, SchemaProperty property) throws SchemaException {
        SchemaResolvedDataType resolvedDataType = new SchemaResolvedDataType(property.getType(), property.getLocation());

        resolvedDataType.integrateType(resolvedDataType.getName(), schema);
        resolvedDataType.overlayType(property);
        resolvedDataType.validate(rules);

        return resolvedDataType;
    }

    static SchemaResolvedDataType create(Schema schema, SchemaRules rules, String type, SchemaParserLocation location) throws SchemaException {
        SchemaResolvedDataType resolvedDataType = new SchemaResolvedDataType(type, location);

        resolvedDataType.integrateType(resolvedDataType.getName(), schema);
        resolvedDataType.validate(rules);

        return resolvedDataType;
    }

    private SchemaResolvedDataType(String type, SchemaParserLocation location) {
        super(location);

        setName(type);
    }

    private void validate(SchemaRules rules) throws SchemaException {
        if (getAllowNull() == SchemaAllowNull.UNSET)
            throw new SchemaException(String.format("Data type '%s' without nullable", getName()), getLocation());

        if (getNativeType() == null)
            throw new SchemaException(String.format("Data type '%s' has a DB type without native type", getName()), getLocation());

        if (
            rules != null &&
            rules.dbTypeSupportsSign(getDbType()) &&
            getSigned() == SchemaSigned.UNSET
        )
            throw new SchemaException(String.format("Data type '%s' has a DB type without signed", getName()), getLocation());

        try {
            if (SqlGenerator.dbTypeRequiresLength(getDbType()) && getLength() == -1)
                throw new SchemaException(String.format("Data type '%s' has a DB type without length", getName()), getLocation());
        } catch (SchemaMigrateException e) {
            throw new SchemaException(e.getMessage(), e);
        }
    }

    private void integrateType(String type, Schema schema) throws SchemaException {
        SchemaDataType dataType = schema.getDataTypes().get(type);
        if (dataType == null)
            throw new SchemaException(String.format("Data type '%s' not found", type), getLocation());

        if (dataType.getDbType() == SchemaDbType.UNSET) {
            if (dataType.getType() == null) {
                if (getDbType() == SchemaDbType.UNSET)
                    throw new SchemaException(String.format("Data type '%s' without DB type", type), getLocation());
            } else if (StringUtils.equals(dataType.getType(), type)) {
                throw new SchemaException(String.format("Data type '%s' cannot reference self", type), getLocation());
            } else {
                integrateType(dataType.getType(), schema);
            }
        } else {
            setDbType(dataType.getDbType());
        }

        overlayType(dataType);
    }

    private void overlayType(SchemaDataTypeBase dataType) {
        if (dataType.getNativeType() != null)
            setNativeType(dataType.getNativeType());
        if (dataType.getLength() != -1)
            setLength(dataType.getLength());
        if (dataType.getPositions() != -1)
            setPositions(dataType.getPositions());
        if (dataType.getAllowNull() != SchemaAllowNull.UNSET)
            setAllowNull(dataType.getAllowNull());
        if (dataType.getSigned() != SchemaSigned.UNSET)
            setSigned(dataType.getSigned());
        if (dataType.getLazy() != SchemaLazy.UNSET)
            setLazy(dataType.getLazy());
        if (dataType.getEnumType() != null)
            setEnumType(dataType.getEnumType());
        if (dataType.getUserType() != null)
            setUserType(dataType.getUserType());
        if (dataType.getIndexType() != SchemaIndexType.UNSET)
            setIndexType(dataType.getIndexType());
    }
}
