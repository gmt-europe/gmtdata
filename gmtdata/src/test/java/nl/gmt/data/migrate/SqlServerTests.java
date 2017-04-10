package nl.gmt.data.migrate;

import nl.gmt.data.DataException;
import nl.gmt.data.drivers.DatabaseDriver;
import nl.gmt.data.drivers.SqlServerDatabaseDriver;
import org.junit.Test;

import java.sql.Connection;

public class SqlServerTests extends DatabaseTests {
    @Override
    protected DatabaseDriver createDatabaseDriver() {
        return new SqlServerDatabaseDriver();
    }

    @Override
    protected String getConnectionStringName() {
        return "sqlserver";
    }

    @Override
    protected Connection createConnection(String connectionString) throws DataException {
        return createDatabaseDriver().createConnection(connectionString);
    }

    @Override
    protected void execute(String testScript, boolean suppressConsole, ExpectChanges expectChanges) {
        super.execute(
            String.format(
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                    "<schema xmlns=\"http://schemas.gmt.nl/gmtdata/2015/02/hibernate-schema\"" +
                    "        namespace=\"UnitTestAssembly\">" +
                    "  <settings>" +
                    "    <idProperty name=\"Id\" type=\"int\" autoIncrement=\"true\" foreignPostfix=\"Id\">" +
                    "      <generator name=\"native\" />" +
                    "    </idProperty>" +
                    "  </settings>" +
                    "  %s" +
                    "</schema>",
                testScript
            ),
            suppressConsole,
            expectChanges
        );
    }

    @Test
    public void partialIndexes() {
        execute(
"<class name=\"TableA\">" +
"  <property name=\"PropertyA\" type=\"int\" />" +
"  <property name=\"PropertyB\" type=\"int\" />" +
"  <index properties=\"PropertyA,PropertyB\" filter='[PropertyA] is not null' />" +
"</class>"
        );
    }

    @Test
    public void partialUniqueIndexes() {
        execute(
"<class name=\"TableA\">" +
"  <property name=\"PropertyA\" type=\"int\" />" +
"  <property name=\"PropertyB\" type=\"int\" />" +
"  <index properties=\"PropertyA,PropertyB\" unique=\"true\" filter='[PropertyA] is not null' />" +
"</class>"
        );
    }

    @Test
    public void verifyFilterIsChange() {
        execute(
"<class name=\"TableA\">" +
"  <property name=\"PropertyA\" type=\"int\" />" +
"  <property name=\"PropertyB\" type=\"int\" />" +
"  <index properties=\"PropertyA,PropertyB\" />" +
"</class>"
        );

        execute(
"<class name=\"TableA\">" +
"  <property name=\"PropertyA\" type=\"int\" />" +
"  <property name=\"PropertyB\" type=\"int\" />" +
"  <index properties=\"PropertyA,PropertyB\" filter='[PropertyA] is not null' />" +
"</class>"
);
    }

    @Test
    public void verifySameFilterIsNoChange() {
        execute(
            "<class name=\"TableA\">" +
                "  <property name=\"PropertyA\" type=\"int\" />" +
                "  <property name=\"PropertyB\" type=\"int\" />" +
                "  <index properties=\"PropertyA,PropertyB\" filter='[PropertyA] is not null' />" +
                "</class>"
        );

        execute(
            "<class name=\"TableA\">" +
                "  <property name=\"PropertyA\" type=\"int\" />" +
                "  <property name=\"PropertyB\" type=\"int\" />" +
                "  <index properties=\"PropertyA,PropertyB\" filter='[PropertyA] is not null' />" +
                "</class>",
            ExpectChanges.NO
        );
    }

    @Test
    public void stringLengthIsOptional() {
        execute(
            "<class name=\"Table\">" +
                "  <property name=\"Property\" type=\"string\" />" +
                "</class>"
        );
    }

    @Test
    public void binaryLengthIsOptional() {
        execute(
            "<class name=\"Table\">" +
                "  <property name=\"Property\" type=\"binary\" />" +
                "</class>"
        );
    }

    @Test
    public void settingLengthRequiresChange() {
        execute(
            "<class name=\"Table\">" +
                "  <property name=\"Property\" type=\"string\" />" +
                "</class>"
        );

        execute(
            "<class name=\"Table\">" +
                "  <property name=\"Property\" type=\"string\" length=\"10\" />" +
                "</class>"
        );
    }
}
