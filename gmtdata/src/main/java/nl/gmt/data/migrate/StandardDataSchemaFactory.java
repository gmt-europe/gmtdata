package nl.gmt.data.migrate;

import nl.gmt.data.schema.*;

public class StandardDataSchemaFactory implements DataSchemaFactory {
    @Override
    public DataSchemaTable createClass(SchemaClass klass, Schema schema, SchemaRules rules) throws SchemaMigrateException {
        DataSchemaTable result = new DataSchemaTable();

        result.setName(klass.getResolvedDbName());
        result.setAutoIncrement(-1);

        // Add details for the ID field.

        result.addField(createIdField(klass));
        result.getIndexes().add(createPrimaryIndex(klass));

        addClassItems(klass, klass, schema, result);

        return result;
    }

    private void addClassItems(SchemaClass baseClass, SchemaClassBase klass, Schema schema, DataSchemaTable result) throws SchemaMigrateException {
        // Add all fields

        for (SchemaProperty field : klass.getProperties().values()) {
            result.addField(createProperty(field));
        }

        for (SchemaForeignBase foreign : klass.getForeigns().values()) {
            if (foreign.getType() == SchemaForeignType.PARENT) {
                result.addField(createPropertyFromForeignParent((SchemaForeignParent)foreign, schema));
            }
        }

        // Add all indexes

        for (SchemaForeignBase foreign : klass.getForeigns().values()) {
            if (foreign.getType() == SchemaForeignType.PARENT) {
                result.getIndexes().add(createIndexFromForeignParent((SchemaForeignParent)foreign));
            }
        }

        for (SchemaProperty field : klass.getProperties().values()) {
            SchemaIndexType indexed = field.getIndexType();

            if (indexed == SchemaIndexType.UNSET) {
                indexed = field.getResolvedDataType().getIndexType();
            }

            if (indexed != SchemaIndexType.UNSET) {
                result.getIndexes().add(createIndexFromProperty(field, indexed));
            }
        }

        for (SchemaIndex index : klass.getIndexes()) {
            result.getIndexes().add(createFromIndex(index, baseClass, klass, schema));
        }

        // Add all foreign keys

        for (SchemaForeignBase foreign : klass.getForeigns().values()) {
            if (foreign.getType() == SchemaForeignType.PARENT) {
                result.getForeignKeys().add(createFromForeignParent((SchemaForeignParent)foreign, schema));
            }
        }

        // Recurse into the mixins.

        for (String name : klass.getMixins()) {
            SchemaMixin mixin = schema.getMixins().get(name);

            addClassItems(baseClass, mixin, schema, result);
        }
    }

    public DataSchemaField createIdField(SchemaClass klass) throws SchemaMigrateException {
        DataSchemaField result = new DataSchemaField();

        result.setName(klass.getResolvedIdProperty().getResolvedDbName());
        result.setAutoIncrement(klass.getResolvedIdProperty().getAutoIncrement() == SchemaIdAutoIncrement.YES);

        result.initializeType(klass.getResolvedIdProperty().getResolvedDataType());

        return result;
    }

    public DataSchemaField createProperty(SchemaProperty field) throws SchemaMigrateException {
        DataSchemaField result = new DataSchemaField();

        result.setName(field.getResolvedDbName());
        result.setAutoIncrement(false);

        result.initializeType(field.getResolvedDataType());

        return result;
    }

    public DataSchemaField createPropertyFromForeignParent(SchemaForeignParent foreign, Schema schema) throws SchemaMigrateException {
        DataSchemaField result = new DataSchemaField();

        result.setName(foreign.getResolvedDbName());
        result.setAutoIncrement(false);

        result.initializeType(
            schema.getClasses().get(foreign.getClassName()).getResolvedIdProperty().getResolvedDataType()
        );

        if (foreign.getAllowNull() != SchemaAllowNull.UNSET) {
            result.setNullable(foreign.getAllowNull() == SchemaAllowNull.ALLOW);
        }

        return result;
    }

    public DataSchemaIndex createPrimaryIndex(SchemaClass klass) {
        DataSchemaIndex result = new DataSchemaIndex();

        result.setName(null);
        result.getFields().add(klass.getResolvedIdProperty().getResolvedDbName());
        result.setType(SchemaIndexType.PRIMARY);

        return result;
    }

    public DataSchemaIndex createIndexFromForeignParent(SchemaForeignParent foreign) {
        DataSchemaIndex result = new DataSchemaIndex();

        result.setName(null);
        result.getFields().add(foreign.getResolvedDbName());
        result.setType(foreign.getIndexType() == SchemaIndexType.UNSET ? SchemaIndexType.INDEX : foreign.getIndexType());

        return result;
    }

    public DataSchemaIndex createIndexFromProperty(SchemaProperty field, SchemaIndexType indexed) throws SchemaMigrateException {
        switch (indexed) {
            case PRIMARY:
            case UNSET:
                throw new SchemaMigrateException(String.format("Illegal index type '%s' for field '%s'", indexed, field.getName()), field.getLocation());
        }

        DataSchemaIndex result = new DataSchemaIndex();

        result.setName(null);
        result.getFields().add(field.getResolvedDbName());
        result.setType(indexed);

        return result;
    }

    public DataSchemaIndex createFromIndex(SchemaIndex index, SchemaClass baseClass, SchemaClassBase klass, Schema schema) {
        DataSchemaIndex result = new DataSchemaIndex();

        for (String field : index.getFields()) {
            String name;

            if (field.equals(schema.getIdProperty().getName()) && klass instanceof SchemaClass) {
                name = baseClass.getResolvedIdProperty().getResolvedDbName();
            } else if (klass.getProperties().containsKey(field)) {
                name = klass.getProperties().get(field).getResolvedDbName();
            } else {
                name = ((SchemaForeignParent)klass.getForeigns().get(field)).getResolvedDbName();
            }

            result.getFields().add(name);
        }

        result.setName(null);
        result.setType(index.getType());

        return result;
    }

    public DataSchemaForeignKey createFromForeignParent(SchemaForeignParent foreign, Schema schema) {
        DataSchemaForeignKey result = new DataSchemaForeignKey();

        SchemaClass linkTable = schema.getClasses().get(foreign.getClassName());

        result.setName(null);
        result.setField(foreign.getResolvedDbName());
        result.setLinkTable(linkTable.getResolvedDbName());
        result.setLinkField(linkTable.getResolvedIdProperty().getResolvedDbName());

        return result;
    }
}
