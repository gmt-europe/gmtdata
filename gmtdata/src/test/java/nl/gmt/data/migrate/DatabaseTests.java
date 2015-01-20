package nl.gmt.data.migrate;

import org.junit.Test;

public abstract class DatabaseTests extends DatabaseTestBase {
    @Test
    public void createTable() {
        execute(
"<classes>" +
"  <class name=\"Table\" />" +
"</classes>"
        );
    }

    @Test
    public void createTableWithProperty() {
        execute(
"<classes>" +
"  <class name=\"Table\">"+
"    <property name=\"Property\" type=\"string\" length=\"10\" />" +
"  </class>" +
"</classes>"
        );
    }

    @Test
    public void createTableWithForeignKey() {
        execute(
"<classes>" +
"  <class name=\"TableA\" />" +
"  <class name=\"TableB\">" +
"    <foreignParent name=\"A\" class=\"TableA\" />" +
"  </class>" +
"</classes>"
        );
    }

    @Test
    public void createTableWithSingleIndex() {
        execute(
"<classes>" +
"  <class name=\"Table\">" +
"    <property name=\"Property\" type=\"int\" indexed=\"true\" />" +
"  </class>" +
"</classes>"
        );
    }

    @Test
    public void createTableWithSingleUniqueIndex() {
        execute(
"<classes>" +
"  <class name=\"Table\">" +
"    <property name=\"Property\" type=\"int\" indexed=\"unique\" />" +
"  </class>" +
"</classes>"
        );
    }

    @Test
    public void createTableWithMultiFieldIndex() {
        execute(
"<classes>" +
"  <class name=\"Table\">" +
"    <property name=\"PropertyA\" type=\"int\" />" +
"    <property name=\"PropertyB\" type=\"int\" />" +
"    <index properties=\"PropertyA,PropertyB\" />" +
"  </class>" +
"</classes>"
        );
    }

    @Test
    public void addProperty() {
        execute(
"<classes>" +
"  <class name=\"Table\" />" +
"</classes>"
        );

        execute(
"<classes>" +
"  <class name=\"Table\">" +
"    <property name=\"Property\" type=\"int\" />" +
"  </class>" +
"</classes>"
        );
    }

    @Test
    public void removeProperty() {
        execute(
"<classes>" +
"  <class name=\"Table\">" +
"    <property name=\"Property\" type=\"int\" />" +
"  </class>" +
"</classes>"
        );

        execute(
"<classes>" +
"  <class name=\"Table\">" +
"  </class>" +
"</classes>"
        );
    }

    @Test
    public void addForeignKey() {
        execute(
"<classes>" +
"  <class name=\"TableA\" />" +
"  <class name=\"TableB\" />" +
"</classes>"
        );

        execute(
"<classes>" +
"  <class name=\"TableA\" />" +
"  <class name=\"TableB\">" +
"    <foreignParent name=\"A\" class=\"TableA\" />" +
"  </class>" +
"</classes>"
        );
    }

    @Test
    public void removeForeignKey() {
        execute(
"<classes>" +
"  <class name=\"TableA\" />" +
"  <class name=\"TableB\">" +
"    <foreignParent name=\"A\" class=\"TableA\" />" +
"  </class>" +
"</classes>"
        );

        execute(
"<classes>" +
"  <class name=\"TableA\" />" +
"  <class name=\"TableB\" />" +
"</classes>"
        );
    }

    @Test
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
"</classes>"
        );
    }

    @Test
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
"</classes>"
        );
    }

    @Test
    public void changePropertyTypeToNullable() {
        execute(
"<classes>" +
"  <class name=\"Table\">" +
"    <property name=\"Property\" type=\"int\" />" +
"  </class>" +
"</classes>"
        );

        execute(
"<classes>" +
"  <class name=\"Table\">" +
"    <property name=\"Property\" type=\"int\" nullable=\"true\" />" +
"  </class>" +
"</classes>"
        );
    }

    @Test
    public void changePropertyTypeToNotNullable() {
        execute(
"<classes>" +
"  <class name=\"Table\">" +
"    <property name=\"Property\" type=\"int\" nullable=\"true\" />" +
"  </class>" +
"</classes>"
        );

        execute(
"<classes>" +
"  <class name=\"Table\">" +
"    <property name=\"Property\" type=\"int\" />" +
"  </class>" +
"</classes>"
        );
    }

    @Test
    public void changePropertyTypeStringToInt() {
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
"    <property name=\"Property\" type=\"int\" />" +
"  </class>" +
"</classes>"
        );
    }

    @Test
    public void changePropertyTypeIntToString() {
        execute(
"<classes>" +
"  <class name=\"Table\">" +
"    <property name=\"Property\" type=\"int\" />" +
"  </class>" +
"</classes>"
        );

        execute(
"<classes>" +
"  <class name=\"Table\">" +
"    <property name=\"Property\" type=\"string\" length=\"10\" />" +
"  </class>" +
"</classes>"
        );
    }

    @Test
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
"</classes>"
        );
    }

    @Test
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
"</classes>"
        );
    }

    @Test
    public void applyIndexOnDataType() {
        execute(
"<classes>" +
"  <class name=\"Table\">" +
"    <property name=\"Property\" type=\"string\" length=\"10\" />" +
"  </class>" +
"</classes>"
        );

        execute(
"<dataTypes>" +
"  <dataType name=\"indexed-string\" type=\"string\" length=\"10\" indexed=\"true\" />" +
"</dataTypes>" +
"" +
"<classes>" +
"  <class name=\"Table\">" +
"    <property name=\"Property\" type=\"indexed-string\" length=\"10\" />" +
"  </class>" +
"</classes>"
        );
    }

    @Test
    public void noChangeWhenIndexLocationChanged() {
        execute(
"<classes>" +
"  <class name=\"Table\">" +
"    <property name=\"Property\" type=\"string\" length=\"10\" indexed=\"true\" />" +
"  </class>" +
"</classes>"
        );

        execute(
"<dataTypes>" +
"  <dataType name=\"indexed-string\" type=\"string\" length=\"10\" indexed=\"true\" />" +
"</dataTypes>" +
"" +
"<classes>" +
"  <class name=\"Table\">" +
"    <property name=\"Property\" type=\"indexed-string\" length=\"10\" />" +
"  </class>" +
"</classes>",
            ExpectChanges.NO
        );
    }

    @Test
    public void longTextShouldNotChange() {
        execute(
"<classes>" +
"  <class name=\"Table\">" +
"      <property name=\"Field\" type=\"long-text\" />" +
"  </class>" +
"</classes>"
        );
    }

    @Test
    public void longBinaryShouldNotChange() {
        execute(
"<classes>" +
"  <class name=\"Table\">" +
"    <property name=\"Field\" type=\"long-blob\" />" +
"  </class>" +
"</classes>"
        );
    }

    @Test
    public void guidHasNoLength() {
        execute(
"<classes>" +
"  <class name=\"Table\">" +
"    <property name=\"PropertyA\" type=\"guid\" />" +
"  </class>" +
"</classes>"
        );

        execute(
"<classes>" +
"  <class name=\"Table\">" +
"    <property name=\"PropertyB\" type=\"guid\" />" +
"  </class>" +
"</classes>"
);
    }

    @Test
    public void alternateIdPropertyType() {
        execute(
"<classes>" +
"  <class name=\"Table\">" +
"    <idProperty type=\"short\" />" +
"    <property name=\"Property\" type=\"int\" />" +
"  </class>" +
"</classes>"
);
    }

    @Test
    public void dateTimeFieldDoesNotHaveLength() {
        execute(
"<classes>" +
"  <class name=\"Table\">" +
"    <property name=\"Property\" type=\"datetime\" />" +
"  </class>" +
"</classes>"
);
    }

    @Test
    public void dbNames() {
        execute(
"<classes>" +
"  <class name=\"TableA\" dbName=\"table_1\">" +
"    <idProperty dbIdName=\"id_property\" />" +
"    <property name=\"Property\" type=\"int\" dbName=\"property_1\" />" +
"    <foreignParent name=\"Foreign\" class=\"TableB\" dbName=\"foreign_1\" />" +
"    <index properties=\"Property,Foreign\" />" +
"  </class>" +
"  <class name=\"TableB\" dbName=\"table_2\">" +
"    <idProperty dbIdName=\"id_property\" />" +
"    <property name=\"Property\" type=\"int\" dbName=\"property_2\" />" +
"  </class>" +
"</classes>"
        );
    }

    @Test
    public void createUniqueIndex() {
        execute(
"<classes>" +
"  <class name=\"TableA\">" +
"    <property name=\"PropertyA\" type=\"int\" />" +
"    <property name=\"PropertyB\" type=\"int\" />" +
"    <index properties=\"PropertyA,PropertyB\" unique=\"true\" />" +
"  </class>" +
"</classes>"
        );
    }

    @Test
    public void includeMixins() {
        execute(
"<mixins>" +
"  <mixin name=\"MixinA\">" +
"    <property name=\"MixinPropertyA\" type=\"int\"/>" +
"  </mixin>" +
"</mixins>" +
"<classes>" +
"  <class name=\"TableA\" mixins=\"MixinA\">" +
"    <property name=\"PropertyA\" type=\"int\"/>" +
"  </class>" +
"</classes>"
        );
    }
}
