package nl.gmt.data;

public class TestConnection extends DbConnection {
    public TestConnection(String connectionString, DbType type) throws DataException {
        super(
            connectionString,
            type,
            "Database.schema",
            new RepositoryService(RepositoryService.getUrlFromClass(TestConnection.class))
        );
    }
}
