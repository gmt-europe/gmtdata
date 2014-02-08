package nl.gmt.data.schema;

import nl.gmt.data.migrate.SqlStatement;

import java.io.InputStream;

public interface SchemaCallback {
    InputStream loadFile(String schema) throws Exception;
    void serializeSql(Iterable<SqlStatement> statements);
}
