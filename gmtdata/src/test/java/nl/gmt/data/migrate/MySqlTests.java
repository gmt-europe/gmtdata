package nl.gmt.data.migrate;

import nl.gmt.data.DataException;
import nl.gmt.data.drivers.DatabaseDriver;
import nl.gmt.data.drivers.MySqlDatabaseDriver;
import org.junit.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class MySqlTests extends DatabaseTests {
    @Override
    protected DatabaseDriver createDatabaseDriver() {
        return new MySqlDatabaseDriver();
    }

    @Override
    protected String getConnectionStringName() {
        return "mysql";
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
"<schema xmlns=\"http://schemas.gmt.nl/gmtdata/2014/02/hibernate-schema\"" +
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

    private void clearDatabase() {
        execute("", ExpectChanges.DONT_CARE);
    }

    @Test
    public void changeCharacterSet() throws SQLException {
        clearDatabase();

        try (Statement stmt = getConnection().createStatement()) {
            stmt.execute(
"CREATE TABLE `Table` ( " +
"`Id` INT AUTO_INCREMENT NOT NULL, " +
"PRIMARY KEY (`Id`) " +
") ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_bin"
            );
        }

        execute(
            "<classes>" +
                "  <class name=\"Table\" />" +
                "</classes>"
        );
    }

    @Test
    public void changeCollation() throws SQLException {
        clearDatabase();

        try (Statement stmt = getConnection().createStatement()) {
            stmt.execute(
"CREATE TABLE `Table` ( " +
"`Id` INT AUTO_INCREMENT NOT NULL, " +
"PRIMARY KEY (`Id`) " +
") ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin"
            );
        }

        execute(
            "<classes>" +
                "  <class name=\"Table\" />" +
                "</classes>"
        );
    }

    @Test
    public void changeEngine() throws SQLException {
        clearDatabase();

        try (Statement stmt = getConnection().createStatement()) {
            stmt.execute(
"CREATE TABLE `Table` ( " +
"`Id` INT AUTO_INCREMENT NOT NULL, " +
"PRIMARY KEY (`Id`) " +
") ENGINE=MyISAM DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci"
            );
        }

        execute(
            "<classes>" +
                "  <class name=\"Table\" />" +
                "</classes>"
        );
    }

    @Test
    public void changeFieldCharset() throws SQLException {
        clearDatabase();

        try (Statement stmt = getConnection().createStatement()) {
            stmt.execute(
"CREATE TABLE `Table` ( " +
"`Id` INT AUTO_INCREMENT NOT NULL, " +
"`Property` VARCHAR(40) CHARACTER SET latin1 COLLATE latin1_bin NOT NULL, " +
"PRIMARY KEY (`Id`) " +
") ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci"
            );
        }

        execute(
"<classes>" +
"  <class name=\"Table\">" +
"    <property name=\"Property\" type=\"string\" length=\"40\"/>" +
"  </class>" +
"</classes>"
        );
    }
}
