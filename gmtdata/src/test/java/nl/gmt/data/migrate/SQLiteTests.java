package nl.gmt.data.migrate;

import nl.gmt.data.DataException;
import nl.gmt.data.drivers.DatabaseDriver;
import nl.gmt.data.drivers.SQLiteDatabaseDriver;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.sql.Connection;

@RunWith(JUnit4.class)
public class SQLiteTests extends DatabaseTests {
    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected DatabaseDriver createDatabaseDriver() {
        return new SQLiteDatabaseDriver();
    }

    @Override
    protected String getConnectionStringName() {
        return "sqlite";
    }

    @Override
    protected Connection createConnection(String connectionString) throws DataException {
        int index = connectionString.lastIndexOf(':');

        File file = new File(connectionString.substring(index + 1));

        if (!file.getParentFile().exists())
            file.getParentFile().mkdirs();
        if (file.exists())
            file.delete();

        return createDatabaseDriver().createConnection(connectionString);
    }

    @Override
    protected void execute(String testScript, boolean suppressConsole, ExpectChanges expectChanges) {
        super.execute(
            String.format(
"<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
"<schema xmlns=\"http://schemas.gmt.nl/gmtdata/2015/01/hibernate-schema\"" +
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
    @Override
    public void changePropertyTypeLargerLength() {
        execute(
"<classes>" +
"  <class name=\"Table\">" +
"    <property name=\"Property\" type=\"string\" length=\"10\" />" +
"  </class>" +
"</classes>"
    );

        execute(
"<classes>" +
"  <class name=\"Table\">" +
"    <property name=\"Property\" type=\"string\" length=\"20\" />" +
"  </class>" +
"</classes>",
            ExpectChanges.NO
        );
    }

    @Test
    @Override
    public void changePropertyTypeSmallerLength() {
        execute(
"<classes>" +
"  <class name=\"Table\">" +
"    <property name=\"Property\" type=\"string\" length=\"20\" />" +
"  </class>" +
"</classes>"
        );

        execute(
"<classes>" +
"  <class name=\"Table\">" +
"    <property name=\"Property\" type=\"string\" length=\"10\" />" +
"  </class>" +
"</classes>",
            ExpectChanges.NO
        );
    }

    @Test
    @Override
    public void changePropertyTypeStringToText() {
        execute(
"<classes>" +
"  <class name=\"Table\">" +
"    <property name=\"Property\" type=\"string\" length=\"10\" />" +
"  </class>" +
"</classes>"
        );

        execute(
"<classes>" +
"  <class name=\"Table\">" +
"    <property name=\"Property\" type=\"text\" />" +
"  </class>" +
"</classes>",
            ExpectChanges.NO
        );
    }

    @Test
    @Override
    public void changePropertyTypeTextToString() {
        execute(
"<classes>" +
"  <class name=\"Table\">" +
"    <property name=\"Property\" type=\"text\" />" +
"  </class>" +
"</classes>"
        );

        execute(
"<classes>" +
"  <class name=\"Table\">" +
"    <property name=\"Property\" type=\"string\" length=\"10\" />" +
"  </class>" +
"</classes>",
            ExpectChanges.NO
        );
    }

    @Test
    public void recreateWithIndex() {
        execute(
"<classes>" +
"  <class name=\"Table\">" +
"    <property name=\"Property\" type=\"int\" indexed=\"true\" />" +
"  </class>" +
"</classes>"
        );

        execute(
"<classes>" +
"  <class name=\"Table\">" +
"    <property name=\"Property\" type=\"string\" length=\"20\" indexed=\"true\" />" +
"  </class>" +
"</classes>"
        );

        execute(
"<classes>" +
"  <class name=\"Table\">" +
"    <property name=\"Property\" type=\"double\" indexed=\"true\" />" +
"  </class>" +
"</classes>"
        );
    }
}
