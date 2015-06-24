package nl.gmt.data.migrate;

import nl.gmt.data.DataException;
import nl.gmt.data.drivers.DatabaseDriver;
import nl.gmt.data.drivers.PostgresDatabaseDriver;
import org.junit.Test;

import java.sql.Connection;

public class PostgresTests extends DatabaseTests {
    @Override
    protected DatabaseDriver createDatabaseDriver() {
        return new PostgresDatabaseDriver();
    }

    @Override
    protected String getConnectionStringName() {
        return "postgres";
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

    @Override
    public void changePropertyTypeStringToInt() {
        // Postgres does not allow data type changes between types that cannot be
        // cast to each other.
    }

    @Test
    public void jsonDataType() {
        execute(
"<class name=\"Table\">" +
"  <property name=\"Property\" type=\"json\" />" +
"</class>"
        );
    }

    @Test
    public void binaryJsonDataType() {
        execute(
"<class name=\"Table\">" +
"  <property name=\"Property\" type=\"jsonb\" />" +
"</class>"
        );
    }

    @Test
    public void stringArrayPropertyType() {
        execute(
"<class name=\"Table\">" +
"  <property name=\"Property\" type=\"text\" arity=\"1\" />" +
"</class>"
        );
    }

    @Test
    public void stringMultiDimensionalArrayPropertyType() {
        execute(
"<class name=\"Table\">" +
"  <property name=\"Property\" type=\"text\" arity=\"2\" />" +
"</class>"
        );
    }

    @Test
    public void changeToArrayType() {
        execute(
"<class name=\"Table\">" +
"  <property name=\"Property\" type=\"text\" />" +
"</class>"
        );

        // This fails because the types cannot be cast to each other. However we are still testing
        // this to make sure that the difference in type is detected.

        execute(
"<class name=\"Table\">" +
"  <property name=\"Property\" type=\"text\" arity=\"1\" />" +
"</class>",
            ExpectChanges.THROWS
        );
    }
}
