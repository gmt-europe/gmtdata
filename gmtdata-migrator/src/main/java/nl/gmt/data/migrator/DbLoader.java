package nl.gmt.data.migrator;

import nl.gmt.data.drivers.DatabaseDriver;
import nl.gmt.data.drivers.MySqlDatabaseDriver;
import nl.gmt.data.drivers.PostgresDatabaseDriver;
import nl.gmt.data.drivers.SqlServerDatabaseDriver;
import nl.gmt.data.schema.*;
import org.apache.commons.lang.Validate;
import org.postgresql.util.PGobject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@SuppressWarnings("JavaDoc")
public abstract class DbLoader {
    public static DbLoader fromDriver(DatabaseDriver driver, Schema schema, SchemaClass klass) {
        Validate.notNull(driver, "driver");

        if (driver instanceof MySqlDatabaseDriver) {
            return new MySqlDbLoader(schema, klass);
        } else if (driver instanceof PostgresDatabaseDriver) {
            return new PostgresDbLoader(schema, klass);
        } else if (driver instanceof SqlServerDatabaseDriver) {
            return new SqlServerDbLoader(schema, klass);
        } else {
            throw new IllegalStateException("Unsupported database driver");
        }
    }

    protected final Schema schema;
    protected final SchemaClass klass;
    protected final List<SchemaField> schemaFields;

    protected DbLoader(Schema schema, SchemaClass klass) {
        Validate.notNull(schema, "schema");
        Validate.notNull(klass, "klass");

        this.schema = schema;
        this.klass = klass;

        schemaFields = getSchemaFields(schema, klass);
    }

    protected abstract String escapeName(String name);

    public String buildCountQuery() {
        return String.format("SELECT COUNT(*) FROM %s", escapeName(klass.getResolvedDbName()));
    }

    public String buildSelectQuery() {
        StringBuilder select = new StringBuilder();

        select.append(String.format("SELECT %s", escapeName("Id")));

        for (SchemaField field : schemaFields) {
            if (field instanceof SchemaForeignChild) {
                continue;
            }

            String fieldName;
            if (field instanceof SchemaForeignParent) {
                fieldName = ((SchemaForeignParent)field).getResolvedDbName();
            } else {
                fieldName = ((SchemaProperty)field).getResolvedDbName();
            }

            select.append(", ").append(escapeName(fieldName));
        }

        select.append(" FROM ").append(escapeName(klass.getResolvedDbName()));

        return select.toString();
    }

    public String buildInsertQuery() {
        StringBuilder insert = new StringBuilder();

        insert
            .append("INSERT INTO ")
            .append(escapeName(klass.getResolvedDbName()))
            .append(" (")
            .append(escapeName("Id"));

        List<SchemaField> schemaFields = getSchemaFields(schema, klass);

        for (SchemaField field : schemaFields) {
            if (field instanceof SchemaForeignChild) {
                continue;
            }

            String fieldName;
            if (field instanceof SchemaForeignParent) {
                fieldName = ((SchemaForeignParent)field).getResolvedDbName();
            } else {
                fieldName = ((SchemaProperty)field).getResolvedDbName();
            }

            insert.append(", ").append(escapeName(fieldName));
        }

        insert.append(") VALUES (?");

        for (int i = 0; i < schemaFields.size(); i++) {
            insert.append(", ?");
        }

        insert.append(')');

        return insert.toString();
    }

    private List<SchemaField> getSchemaFields(Schema schema, SchemaClass klass) {
        List<SchemaField> fields = new ArrayList<>();

        addSchemaFields(schema, klass, fields);

        return fields;
    }

    private void addSchemaFields(Schema schema, SchemaClassBase klass, List<SchemaField> fields) {
        for (SchemaField field : klass.getFields()) {
            if (!(field instanceof SchemaForeignChild)) {
                fields.add(field);
            }
        }

        for (String mixin : klass.getMixins()) {
            addSchemaFields(schema, schema.getMixins().get(mixin), fields);
        }
    }

    public Object parseValue(int index, Object value) {
        return value;
    }

    public Object printValue(int index, Object value) {
        return value;
    }

    private static class MySqlDbLoader extends DbLoader {
        private final boolean[] uuidFields;

        protected MySqlDbLoader(Schema schema, SchemaClass klass) {
            super(schema, klass);

            uuidFields = getUuidFields(schemaFields);
        }

        private boolean[] getUuidFields(List<SchemaField> fields) {
            boolean[] uuidFields = new boolean[fields.size() + 1];
            boolean idIsUuid = schema.getIdProperty().getResolvedDataType().getNativeType() == UUID.class;
            int offset = 1;

            uuidFields[0] = idIsUuid;

            for (SchemaField field : fields) {
                if (field instanceof SchemaForeignChild) {
                    continue;
                }

                if (field instanceof SchemaProperty) {
                    uuidFields[offset] = ((SchemaProperty)field).getResolvedDataType().getNativeType() == UUID.class;
                } else {
                    uuidFields[offset] = idIsUuid;
                }

                offset++;
            }

            return uuidFields;
        }

        @Override
        public String escapeName(String name) {
            return "`" + name + "`";
        }

        @Override
        public Object parseValue(int index, Object value) {
            if (uuidFields[index]) {
                return bytesToUuid((byte[])value);
            }

            return super.parseValue(index, value);
        }

        private UUID bytesToUuid(byte[] value) {
            if (value == null) {
                return null;
            }

            byte[] msb = new byte[8];
            byte[] lsb = new byte[8];
            System.arraycopy(value, 0, msb, 0, 8);
            System.arraycopy(value, 8, lsb, 0, 8);
            return new UUID(asLong(msb), asLong(lsb));
        }

        private static long asLong(byte[] bytes) {
            if(bytes == null) {
                return 0L;
            } else if(bytes.length != 8) {
                throw new IllegalArgumentException("Expecting 8 byte values to construct a long");
            } else {
                long value = 0L;

                for(int i = 0; i < 8; ++i) {
                    value = value << 8 | (long)(bytes[i] & 255);
                }

                return value;
            }
        }
    }

    private static class PostgresDbLoader extends DbLoader {
        protected PostgresDbLoader(Schema schema, SchemaClass klass) {
            super(schema, klass);
        }

        @Override
        public String escapeName(String name) {
            return "\"" + name + "\"";
        }

        @Override
        public Object parseValue(int index, Object value) {
            if (value instanceof PGobject) {
                PGobject object = (PGobject)value;
                switch (object.getType()) {
                    case "citext":
                        return object.getValue();
                    default:
                        throw new IllegalStateException();
                }
            }

            return super.parseValue(index, value);
        }
    }

    private static class SqlServerDbLoader extends DbLoader {
        protected SqlServerDbLoader(Schema schema, SchemaClass klass) {
            super(schema, klass);
        }

        @Override
        public String escapeName(String name) {
            return "[" + name + "]";
        }

        @Override
        public Object printValue(int index, Object value) {
            if (value instanceof UUID) {
                return value.toString();
            }

            return super.printValue(index, value);
        }
    }
}
