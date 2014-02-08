package nl.gmt.data.migrate;

public class DataSchemaFieldDifference {
    private boolean differ;
    private DataSchemaField field;
    private DataSchemaField oldField;

    public DataSchemaFieldDifference(DataSchemaField currentField, DataSchemaField newField, DataSchemaExecutor executor) throws SchemaMigrateException {
        field = newField;
        oldField = currentField;

        if (executor.getConfiguration().isNoConstraintsOrIndexes())
            field.setNullable(oldField.isNullable());

        differ = !newField.equals(oldField, executor.getRules());
    }

    public boolean isDiffer() {
        return differ;
    }

    public DataSchemaField getField() {
        return field;
    }

    public DataSchemaField getOldField() {
        return oldField;
    }
}
