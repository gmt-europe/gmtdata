package nl.gmt.data.migrate;

public class SqlStatement {
    private final SqlStatementType type;
    private final String value;

    public SqlStatement(SqlStatementType type, String value) {
        this.type = type;
        this.value = value;
    }

    public SqlStatementType getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
