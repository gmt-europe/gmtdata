package nl.gmt.data.migrate;

import org.junit.Test;

public abstract class DatabaseTests extends DatabaseTestBase {
    @Test
    public void createTable() {
        execute(
"<class name=\"Table\" />"
        );
    }

    @Test
    public void createTableWithProperty() {
        execute(
"<class name=\"Table\">"+
"  <property name=\"Property\" type=\"string\" length=\"10\" />" +
"</class>"
        );
    }

    @Test
    public void createTableWithForeignKey() {
        execute(
"<class name=\"TableA\" />" +
"<class name=\"TableB\">" +
"  <foreignParent name=\"A\" class=\"TableA\" />" +
"</class>"
        );
    }

    @Test
    public void createTableWithSingleIndex() {
        execute(
"<class name=\"Table\">" +
"  <property name=\"Property\" type=\"int\" indexed=\"true\" />" +
"</class>"
        );
    }

    @Test
    public void createTableWithSingleUniqueIndex() {
        execute(
"<class name=\"Table\">" +
"  <property name=\"Property\" type=\"int\" indexed=\"unique\" />" +
"</class>"
        );
    }

    @Test
    public void createTableWithMultiFieldIndex() {
        execute(
"<class name=\"Table\">" +
"  <property name=\"PropertyA\" type=\"int\" />" +
"  <property name=\"PropertyB\" type=\"int\" />" +
"  <index properties=\"PropertyA,PropertyB\" />" +
"</class>"
        );
    }

    @Test
    public void addProperty() {
        execute(
"<class name=\"Table\" />"
        );

        execute(
"<class name=\"Table\">" +
"  <property name=\"Property\" type=\"int\" />" +
"</class>"
        );
    }

    @Test
    public void removeProperty() {
        execute(
"<class name=\"Table\">" +
"  <property name=\"Property\" type=\"int\" />" +
"</class>"
        );

        execute(
"<class name=\"Table\">" +
"</class>"
        );
    }

    @Test
    public void addForeignKey() {
        execute(
"<class name=\"TableA\" />" +
"<class name=\"TableB\" />"
        );

        execute(
"<class name=\"TableA\" />" +
"<class name=\"TableB\">" +
"  <foreignParent name=\"A\" class=\"TableA\" />" +
"</class>"
        );
    }

    @Test
    public void removeForeignKey() {
        execute(
"<class name=\"TableA\" />" +
"<class name=\"TableB\">" +
"  <foreignParent name=\"A\" class=\"TableA\" />" +
"</class>"
        );

        execute(
"<class name=\"TableA\" />" +
"<class name=\"TableB\" />"
        );
    }

    @Test
    public void changePropertyTypeLargerLength() {
        execute(
"<class name=\"Table\">" +
"  <property name=\"Property\" type=\"string\" length=\"10\" />" +
"</class>"
        );

        execute(
"<class name=\"Table\">" +
"  <property name=\"Property\" type=\"string\" length=\"20\" />" +
"</class>"
        );
    }

    @Test
    public void changePropertyTypeSmallerLength() {
        execute(
"<class name=\"Table\">" +
"  <property name=\"Property\" type=\"string\" length=\"20\" />" +
"</class>"
        );

        execute(
"<class name=\"Table\">" +
"  <property name=\"Property\" type=\"string\" length=\"10\" />" +
"</class>"
        );
    }

    @Test
    public void changePropertyTypeToNullable() {
        execute(
"<class name=\"Table\">" +
"  <property name=\"Property\" type=\"int\" />" +
"</class>"
        );

        execute(
"<class name=\"Table\">" +
"  <property name=\"Property\" type=\"int\" nullable=\"true\" />" +
"</class>"
        );
    }

    @Test
    public void changePropertyTypeToNotNullable() {
        execute(
"<class name=\"Table\">" +
"  <property name=\"Property\" type=\"int\" nullable=\"true\" />" +
"</class>"
        );

        execute(
"<class name=\"Table\">" +
"  <property name=\"Property\" type=\"int\" />" +
"</class>"
        );
    }

    @Test
    public void changePropertyTypeStringToInt() {
        execute(
"<class name=\"Table\">" +
"  <property name=\"Property\" type=\"string\" length=\"10\" />" +
"</class>"
        );

        execute(
"<class name=\"Table\">" +
"  <property name=\"Property\" type=\"int\" />" +
"</class>"
        );
    }

    @Test
    public void changePropertyTypeIntToString() {
        execute(
"<class name=\"Table\">" +
"  <property name=\"Property\" type=\"int\" />" +
"</class>"
        );

        execute(
"<class name=\"Table\">" +
"  <property name=\"Property\" type=\"string\" length=\"10\" />" +
"</class>"
        );
    }

    @Test
    public void changePropertyTypeStringToText() {
        execute(
"<class name=\"Table\">" +
"  <property name=\"Property\" type=\"string\" length=\"10\" />" +
"</class>"
        );

        execute(
"<class name=\"Table\">" +
"  <property name=\"Property\" type=\"text\" />" +
"</class>"
        );
    }

    @Test
    public void changePropertyTypeTextToString() {
        execute(
"<class name=\"Table\">" +
"  <property name=\"Property\" type=\"text\" />" +
"</class>"
        );

        execute(
"<class name=\"Table\">" +
"  <property name=\"Property\" type=\"string\" length=\"10\" />" +
"</class>"
        );
    }

    @Test
    public void applyIndexOnDataType() {
        execute(
"<class name=\"Table\">" +
"  <property name=\"Property\" type=\"string\" length=\"10\" />" +
"</class>"
        );

        execute(
"<dataType name=\"indexed-string\" type=\"string\" length=\"10\" indexed=\"true\" />" +
"" +
"<class name=\"Table\">" +
"  <property name=\"Property\" type=\"indexed-string\" length=\"10\" />" +
"</class>"
        );
    }

    @Test
    public void noChangeWhenIndexLocationChanged() {
        execute(
"<class name=\"Table\">" +
"  <property name=\"Property\" type=\"string\" length=\"10\" indexed=\"true\" />" +
"</class>"
        );

        execute(
"<dataType name=\"indexed-string\" type=\"string\" length=\"10\" indexed=\"true\" />" +
"" +
"<class name=\"Table\">" +
"  <property name=\"Property\" type=\"indexed-string\" length=\"10\" />" +
"</class>",
            ExpectChanges.NO
        );
    }

    @Test
    public void longTextShouldNotChange() {
        execute(
"<class name=\"Table\">" +
"      <property name=\"Field\" type=\"long-text\" />" +
"</class>"
        );
    }

    @Test
    public void longBinaryShouldNotChange() {
        execute(
"<class name=\"Table\">" +
"  <property name=\"Field\" type=\"long-blob\" />" +
"</class>"
        );
    }

    @Test
    public void guidHasNoLength() {
        execute(
"<class name=\"Table\">" +
"  <property name=\"PropertyA\" type=\"guid\" />" +
"</class>"
        );

        execute(
"<class name=\"Table\">" +
"  <property name=\"PropertyB\" type=\"guid\" />" +
"</class>"
);
    }

    @Test
    public void alternateIdPropertyType() {
        execute(
"<class name=\"Table\">" +
"  <idProperty type=\"short\" />" +
"  <property name=\"Property\" type=\"int\" />" +
"</class>"
);
    }

    @Test
    public void dateTimeFieldDoesNotHaveLength() {
        execute(
"<class name=\"Table\">" +
"  <property name=\"Property\" type=\"datetime\" />" +
"</class>"
);
    }

    @Test
    public void dbNames() {
        execute(
"<class name=\"TableA\" dbName=\"table_1\">" +
"  <idProperty dbIdName=\"id_property\" />" +
"  <property name=\"Property\" type=\"int\" dbName=\"property_1\" />" +
"  <foreignParent name=\"Foreign\" class=\"TableB\" dbName=\"foreign_1\" />" +
"  <index properties=\"Property,Foreign\" />" +
"</class>" +
"<class name=\"TableB\" dbName=\"table_2\">" +
"  <idProperty dbIdName=\"id_property\" />" +
"  <property name=\"Property\" type=\"int\" dbName=\"property_2\" />" +
"</class>"
        );
    }

    @Test
    public void createUniqueIndex() {
        execute(
"<class name=\"TableA\">" +
"  <property name=\"PropertyA\" type=\"int\" />" +
"  <property name=\"PropertyB\" type=\"int\" />" +
"  <index properties=\"PropertyA,PropertyB\" unique=\"true\" />" +
"</class>"
        );
    }

    @Test
    public void includeMixins() {
        execute(
"<mixin name=\"MixinA\">" +
"  <property name=\"MixinPropertyA\" type=\"int\"/>" +
"</mixin>" +
"<class name=\"TableA\" mixins=\"MixinA\">" +
"  <property name=\"PropertyA\" type=\"int\"/>" +
"</class>"
        );
    }
}
