package nl.gmt.data.migrate;

import com.sun.org.apache.xalan.internal.xsltc.compiler.util.ResultTreeType;
import com.sun.xml.internal.org.jvnet.staxex.NamespaceContextEx;
import nl.gmt.data.schema.Schema;
import nl.gmt.data.schema.SchemaClass;
import nl.gmt.data.schema.SchemaForeignParent;
import nl.gmt.data.schema.SchemaRules;
import org.apache.commons.lang.StringUtils;

import java.util.List;

public class DataSchemaForeignKey {
    private String name;
    private String field;
    private String linkTable;
    private String linkField;


    public static DataSchemaForeignKey createFromForeignParent(SchemaForeignParent foreign, Schema schema) {
        DataSchemaForeignKey result = new DataSchemaForeignKey();

        SchemaClass linkTable = schema.getClasses().get(foreign.getClassName());

        result.name = null;
        result.field = foreign.getResolvedDbName();
        result.linkTable = linkTable.getResolvedDbName();
        result.linkField = linkTable.getResolvedIdProperty().getResolvedDbIdName();

        return result;
    }

    public boolean equals(DataSchemaForeignKey other) {
        if (this == other)
            return true;

        return
            StringUtils.equalsIgnoreCase(field, other.field) &&
            StringUtils.equalsIgnoreCase(linkTable, other.linkTable) &&
            StringUtils.equalsIgnoreCase(linkField, other.linkField);
    }

    void createName(DataSchemaTable table, DataSchemaTable currentTable) {
        int nameOffset = 1;
        String template = "FK_" + table.getName() + "_%d";

        String keyName;

        while (true) {
            keyName = String.format(template, nameOffset);

            if (
                nameExists(keyName, table.getForeignKeys()) ||
                (currentTable != null && nameExists(keyName, currentTable.getForeignKeys()))
            )
                nameOffset++;
            else
                break;
        }

        name = keyName;
    }

    private boolean nameExists(String keyName, List<DataSchemaForeignKey> keys) {
        for (DataSchemaForeignKey index : keys) {
            if (
                index.name != null &&
                StringUtils.equalsIgnoreCase(index.name, keyName)
            )
                return true;
        }

        return false;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getLinkTable() {
        return linkTable;
    }

    public void setLinkTable(String linkTable) {
        this.linkTable = linkTable;
    }

    public String getLinkField() {
        return linkField;
    }

    public void setLinkField(String linkField) {
        this.linkField = linkField;
    }
}
