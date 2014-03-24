package nl.gmt.data.migrate;

import nl.gmt.data.schema.SchemaIndexType;
import org.apache.commons.lang.StringUtils;

import java.util.*;

public class DataSchemaTableDifference {
    private final DataSchemaTable schema;
    private final DataSchemaTable oldSchema;
    private final boolean differ;
    private final List<String> removedFields = new ArrayList<>();
    private final List<DataSchemaFieldDifference> changedFields = new ArrayList<>();
    private final Map<String, DataSchemaField> newFields = new HashMap<>();
    private final List<String> removedIndexes = new ArrayList<>();
    private final List<DataSchemaIndex> newIndexes = new ArrayList<>();
    private final List<String> removedForeignKeys = new ArrayList<>();
    private final List<DataSchemaForeignKey> newForeignKeys = new ArrayList<>();

    public DataSchemaTableDifference(DataSchemaTable currentSchema, DataSchemaTable newSchema, DataSchemaExecutor executor) throws SchemaMigrateException {
        schema = newSchema;
        oldSchema = currentSchema;

        compareFields(executor);
        compareIndexes(executor);
        compareForeignKeys(executor);

        differ =
            removedFields.size() > 0 ||
            changedFields.size() > 0 ||
            newFields.size() > 0 ||
            removedIndexes.size() > 0 ||
            newIndexes.size() > 0 ||
            removedForeignKeys.size() > 0 ||
            newForeignKeys.size() > 0 ||
            !StringUtils.equalsIgnoreCase(schema.getDefaultCollation(), oldSchema.getDefaultCollation()) ||
            !StringUtils.equalsIgnoreCase(schema.getDefaultCharset(), oldSchema.getDefaultCharset()) ||
            !StringUtils.equalsIgnoreCase(schema.getEngine(), oldSchema.getEngine());
    }

    private void compareFields(DataSchemaExecutor executor) throws SchemaMigrateException {
        Map<String, ChangedState> fields = DataSchema.getChanges(oldSchema.getFields().keySet(), schema.getFields().keySet());

        for (Map.Entry<String, ChangedState> field : fields.entrySet()) {
            switch (field.getValue()) {
                case REMOVED:
                    removedFields.add(field.getKey());
                    break;

                case NEW:
                    DataSchemaField newField = schema.getFields().get(field.getKey());
                    newFields.put(newField.getName(), newField);
                    break;

                case EXISTING:
                    DataSchemaFieldDifference item = new DataSchemaFieldDifference(
                        oldSchema.getFields().get(field.getKey()),
                        schema.getFields().get(field.getKey()),
                        executor
                    );

                    if (item.isDiffer())
                        changedFields.add(item);
                    break;
            }
        }

        if (executor.getConfiguration().isNoConstraintsOrIndexes()) {
            for (DataSchemaField field : newFields.values()) {
                field.setNullable(true);
            }
        }
    }

    private void compareIndexes(DataSchemaExecutor executor) throws SchemaMigrateException {
        Map<DataSchemaIndex, Boolean> indexes = new HashMap<>();

        for (DataSchemaIndex index : schema.getIndexes()) {
            indexes.put(index, false);
        }

        for (DataSchemaIndex index : oldSchema.getIndexes()) {
            boolean found = false;

            for (DataSchemaIndex newIndex : schema.getIndexes()) {
                if (index.equals(newIndex) && !indexes.get(newIndex)) {
                    found = true;
                    indexes.put(newIndex, true);
                    break;
                }
            }

            if (!found) {
                if (index.getType() == SchemaIndexType.PRIMARY)
                    throw new SchemaMigrateException("Cannot remove primary index");
                if (index.getName() == null)
                    throw new SchemaMigrateException("Unnamed index");

                removedIndexes.add(index.getName());
            }
        }

        if (!executor.getConfiguration().isNoConstraintsOrIndexes()) {
            for (Map.Entry<DataSchemaIndex, Boolean> index : indexes.entrySet()) {
                if (!index.getValue())
                    newIndexes.add(index.getKey());
            }
        }
    }

    private void compareForeignKeys(DataSchemaExecutor executor) throws SchemaMigrateException {
        Map<DataSchemaForeignKey, Boolean> foreignKeys = new HashMap<>();

        for (DataSchemaForeignKey foreignKey : schema.getForeignKeys()) {
            foreignKeys.put(foreignKey, false);
        }

        for (DataSchemaForeignKey foreignKey : oldSchema.getForeignKeys()) {
            boolean found = false;

            for (DataSchemaForeignKey newForeignKey : schema.getForeignKeys()) {
                if (foreignKey.equals(newForeignKey)) {
                    found = true;
                    foreignKeys.put(newForeignKey, true);
                    break;
                }
            }

            if (!found) {
                if (foreignKey.getName() == null)
                    throw new SchemaMigrateException("Unnamed foreign key");

                removedForeignKeys.add(foreignKey.getName());
            }
        }

        if (!executor.getConfiguration().isNoConstraintsOrIndexes()) {
            for (Map.Entry<DataSchemaForeignKey, Boolean> foreignKey : foreignKeys.entrySet()) {
                if (!foreignKey.getValue())
                    newForeignKeys.add(foreignKey.getKey());
            }
        }
    }

    public DataSchemaTable getSchema() {
        return schema;
    }

    public DataSchemaTable getOldSchema() {
        return oldSchema;
    }

    public boolean isDiffer() {
        return differ;
    }

    public List<String> getRemovedFields() {
        return removedFields;
    }

    public List<DataSchemaFieldDifference> getChangedFields() {
        return changedFields;
    }

    public Map<String, DataSchemaField> getNewFields() {
        return newFields;
    }

    public List<String> getRemovedIndexes() {
        return removedIndexes;
    }

    public List<DataSchemaIndex> getNewIndexes() {
        return newIndexes;
    }

    public List<String> getRemovedForeignKeys() {
        return removedForeignKeys;
    }

    public List<DataSchemaForeignKey> getNewForeignKeys() {
        return newForeignKeys;
    }
}
