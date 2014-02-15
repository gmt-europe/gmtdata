package nl.gmt.data.hibernate.generation;

import nl.gmt.data.schema.*;
import org.apache.commons.lang.StringEscapeUtils;

import javax.tools.JavaCompiler;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CodeGenerator {
    private Schema schema;

    public CodeGenerator(Schema schema) {
        this.schema = schema;
    }

    public void generate(GeneratorWriter writer) throws SchemaException {
        for (SchemaClass klass : schema.getClasses().values()) {
            generateClass(klass, writer);
        }

        for (SchemaEnumType enumType : schema.getEnumTypes().values()) {
            generateEnumType(enumType, writer);
        }
    }

    private void generateClass(SchemaClass klass, GeneratorWriter writer) throws SchemaException {
        CodeWriter cw = new CodeWriter();

        String packageName = schema.getNamespace() + ".model";
        if (klass.getBoundedContext() != null)
            packageName += "." + klass.getBoundedContext();

        cw.writeln("package %s;", packageName);
        cw.writeln();

        cw.writeln("import org.hibernate.annotations.GenericGenerator;");
        cw.writeln("import org.hibernate.annotations.Parameter;");
        cw.writeln("import org.hibernate.annotations.Type;");
        cw.writeln();

        cw.writeln("import javax.persistence.*;");
        cw.writeln("import java.util.Date;");
        cw.writeln("import java.util.Set;");
        cw.writeln();

        cw.writeln("@Entity");
        cw.writeln("@Table(name = \"%s\")", StringEscapeUtils.escapeJava(klass.getResolvedDbName()));
        cw.writeln(
            "public class %s implements nl.gmt.data.Entity {",
            klass.getName(),
            getTypeName(klass.getResolvedIdProperty().getResolvedDataType().getNativeType())
        );
        cw.indent();

        generateIdProperty(cw, klass.getResolvedIdProperty());

        for (SchemaProperty property : klass.getProperties().values()) {
            cw.writeln();

            generateProperty(cw, property);
        }

        for (SchemaForeignBase foreign : klass.getForeigns().values()) {
            cw.writeln();

            generateForeign(cw, foreign);
        }

        cw.unIndent();
        cw.writeln("}");

        String fileName = "model/";

        if (klass.getBoundedContext() != null)
            fileName += klass.getBoundedContext() + "/";

        fileName += klass.getName() + ".java";

        writer.writeFile(fileName, cw.toString());
    }

    private void generateIdProperty(CodeWriter cw, SchemaClassIdProperty property) throws SchemaException {
        if (property.getCompositeId() != null)
            throw new SchemaException("Composite ID's are not supported");

        generateField(cw, property.getResolvedDataType(), null, "Id");

        cw.writeln();

        cw.writeln("@Id");

        if (property.getAutoIncrement() == SchemaIdAutoIncrement.YES) {
            cw.writeln("@GeneratedValue(strategy = GenerationType.IDENTITY)");
        } else if (property.getDbSequence() != null) {
            cw.writeln("@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = \"SEQ_GEN\")");
            cw.writeln(
                "@SequenceGenerator(name = \"SEQ_GEN\", sequenceName = \"%s\")",
                StringEscapeUtils.escapeJava(property.getDbSequence())
            );
        } else if (property.getGenerator() != null) {
            SchemaGenerator generator = property.getGenerator();

            cw.writeln(
                "@GeneratedValue(generator = \"%s\")",
                StringEscapeUtils.escapeJava(generator.getName())
            );

            StringBuilder sb = new StringBuilder();

            sb.append("@GenericGenerator(name = \"");
            sb.append(StringEscapeUtils.escapeJava(generator.getName()));
            sb.append("\", strategy = \"");
            sb.append(StringEscapeUtils.escapeJava(generator.getStrategy()));
            sb.append("\"");

            if (generator.getParameters().size() > 0) {
                sb.append(", parameters = { ");

                boolean hadOne = false;

                for (SchemaParameter parameter : generator.getParameters()) {
                    if (hadOne)
                        sb.append(", ");
                    else
                        hadOne = true;

                    sb.append("@Parameter(name = \"");
                    sb.append(StringEscapeUtils.escapeJava(parameter.getName()));
                    sb.append("\", value = \"");
                    sb.append(StringEscapeUtils.escapeJava(parameter.getValue()));
                    sb.append("\")");
                }

                sb.append(" }");
            }

            sb.append(")");

            cw.writeln(sb.toString());
        }

        generateColumnAnnotations(cw, property.getResolvedDbIdName(), property.getResolvedDataType());

        cw.writeln("@Override");

        generateGetterSetter(cw, property.getResolvedDataType(), null, "Id");
    }

    private void generateField(CodeWriter cw, SchemaResolvedDataType dataType, String enumType, String name) {
        cw.writeln(
            "private %s %s;",
            enumType != null ? enumType : getTypeName(dataType.getNativeType()),
            getFieldName(name)
        );
    }

    private void generateGetterSetter(CodeWriter cw, SchemaResolvedDataType dataType, String enumType, String name) {
        generateGetterSetter(cw, name, enumType != null ? enumType : getTypeName(dataType.getNativeType()));
    }

    private void generateGetterSetter(CodeWriter cw, String name, String typeName) {
        String fieldName = getFieldName(name);

        cw.writeln(
            "public %s get%s() {",
            typeName,
            name
        );
        cw.indent();
        cw.writeln("return this.%s;", fieldName);
        cw.unIndent();
        cw.writeln("}");
        cw.writeln();
        cw.writeln(
            "public void set%s(%s %s) {",
            name,
            typeName,
            fieldName
        );
        cw.indent();
        cw.writeln("this.%s = %s;", fieldName, fieldName);
        cw.unIndent();
        cw.writeln("}");
    }

    private String getTypeName(Class<?> klass) {
        boolean isArray = false;

        if (klass.isArray()) {
            isArray = true;
            klass = klass.getComponentType();
        }

        String typeName;

        if (klass == java.lang.Byte.class)
            typeName = "byte";
        else if (klass == java.util.UUID.class)
            typeName = klass.getName();
        else if (klass.getPackage().getName().equals("java.lang"))
            typeName = klass.getSimpleName();
        else
            throw new RuntimeException("Cannot generate type name for " + klass.toString());

        if (isArray)
            typeName += "[]";

        return typeName;
    }

    private String getFieldName(String name) {
        return name.substring(0, 1).toLowerCase() + name.substring(1);
    }

    private void generateColumnAnnotations(CodeWriter cw, String dbName, SchemaResolvedDataType dataType) {
        StringBuilder sb = new StringBuilder();

        sb.append("@Column(name = \"");
        sb.append(StringEscapeUtils.escapeJava(dbName));
        sb.append("\"");

        if (dataType.getAllowNull() != SchemaAllowNull.ALLOW)
            sb.append(", nullable = false");
        if (dataType.getLength() != -1) {
            if (dataType.getPositions() != -1) {
                // In the schema, the length includes the scale.

                int scale = dataType.getPositions();
                int precision = dataType.getLength() - scale;

                sb.append(String.format(", precision = %d, scale = %d", precision, scale));
            } else {
                sb.append(String.format(", length = %d", dataType.getLength()));
            }
        }

        sb.append(")");

        cw.writeln(sb.toString());

        /*
         * This forces NHibernate to use the SQL LOB mechanism. However, our SQLite
         * dialect doesn't support it, so we don't output this.
         *
        switch (dataType.getDbType()) {
            case BLOB:
            case LONG_BLOB:
            case MEDIUM_BLOB:
            case TINY_BLOB:
            case TEXT:
            case LOCAL_TEXT:
            case LONG_TEXT:
            case MEDIUM_TEXT:
            case TINY_TEXT:
                cw.writeln("@Lob");
                break;
        }
         */

        if (dataType.getLazy() == SchemaLazy.LAZY)
            cw.writeln("@Basic(fetch = FetchType.LAZY");
    }

    private void generateProperty(CodeWriter cw, SchemaProperty property) {
        generateField(cw, property.getResolvedDataType(), property.getEnumType(), property.getName());

        cw.writeln();

        generateColumnAnnotations(cw, property.getResolvedDbName(), property.getResolvedDataType());

        if (property.getEnumType() != null)
            cw.writeln("@Type(type = \"%s.model.%s$UserType\")", schema.getNamespace(), property.getEnumType());

        generateGetterSetter(cw, property.getResolvedDataType(), property.getEnumType(), property.getName());
    }

    private void generateForeign(CodeWriter cw, SchemaForeignBase foreign) throws SchemaException {
        switch (foreign.getType()) {
            case CHILD: generateForeignChild(cw, (SchemaForeignChild) foreign); break;
            case PARENT: generateForeignParent(cw, (SchemaForeignParent) foreign); break;
        }
    }

    private void generateForeignChild(CodeWriter cw, SchemaForeignChild foreign) {
        SchemaClass linkClass = schema.getClasses().get(foreign.getClassName());

        cw.writeln(
            "private Set<%s> %s;",
            linkClass.getName(),
            getFieldName(foreign.getName())
        );

        cw.writeln();

        cw.writeln("@OneToMany(mappedBy = \"%s\")", getFieldName(foreign.getClassProperty()));

        generateGetterSetter(cw, foreign.getName(), "Set<" + linkClass.getName() + ">");
    }

    private void generateForeignParent(CodeWriter cw, SchemaForeignParent foreign) {
        SchemaClass linkClass = schema.getClasses().get(foreign.getClassName());

        cw.writeln(
            "private %s %s;",
            linkClass.getName(),
            getFieldName(foreign.getName())
        );

        cw.writeln();

        cw.writeln("@ManyToOne");

        cw.writeln("@JoinColumn(name = \"%s\")", StringEscapeUtils.escapeJava(foreign.getResolvedDbName()));

        generateGetterSetter(cw, foreign.getName(), linkClass.getName());
    }

    private void generateEnumType(SchemaEnumType enumType, GeneratorWriter writer) {
        CodeWriter cw = new CodeWriter();

        cw.writeln("package %s.model;", schema.getNamespace());
        cw.writeln();

        cw.writeln("import nl.gmt.data.hibernate.PersistentEnum;");
        cw.writeln("import nl.gmt.data.hibernate.PersistentEnumUserType;");
        cw.writeln();

        cw.writeln("public enum %s implements PersistentEnum {", enumType.getName());
        cw.indent();

        List<SchemaEnumTypeField> fields = new ArrayList<>(enumType.getFields().values());

        for (int i = 0; i < fields.size(); i++) {
            SchemaEnumTypeField field = fields.get(i);

            cw.writeln(
                "%s(%d)%s",
                field.getName(),
                field.getValue(),
                i == fields.size() - 1 ? ";" : ","
            );
        }

        cw.writeln();

        cw.writeln("private final int value;");
        cw.writeln();

        cw.writeln("private %s(int value) {", enumType.getName());
        cw.indent();
        cw.writeln("this.value = value;");
        cw.unIndent();
        cw.writeln("}");
        cw.writeln();

        cw.writeln("public int getValue() {");
        cw.indent();
        cw.writeln("return value;");
        cw.unIndent();
        cw.writeln("}");
        cw.writeln();

        cw.writeln(
            "public static class UserType extends PersistentEnumUserType<%s> {",
            enumType.getName()
        );
        cw.indent();
        cw.writeln("@Override");
        cw.writeln("public Class<%s> returnedClass() {", enumType.getName());
        cw.indent();
        cw.writeln("return %s.class;", enumType.getName());
        cw.unIndent();
        cw.writeln("}");
        cw.unIndent();
        cw.writeln("}");

        cw.unIndent();
        cw.writeln("}");

        writer.writeFile("model/" + enumType.getName() + ".java", cw.toString());
    }
}
