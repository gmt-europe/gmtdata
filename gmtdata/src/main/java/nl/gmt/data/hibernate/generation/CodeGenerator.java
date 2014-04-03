package nl.gmt.data.hibernate.generation;

import nl.gmt.data.schema.*;
import org.apache.commons.lang.StringEscapeUtils;

import java.io.File;
import java.util.*;

public class CodeGenerator {
    private static final Set<String> RESERVED_WORDS = new HashSet<>(Arrays.asList(
        "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const", "continue",
        "default", "do", "double", "else", "enum", "extends", "final", "finally", "float", "for", "goto", "if",
        "implements", "import", "instanceof", "int", "interface", "long", "native", "new", "package", "private",
        "protected", "public", "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this",
        "throw", "throws", "transient", "try", "void", "volatile", "while"
    ));

    private final Schema schema;
    private final File outputDirectory;
    private final File repositoriesOutputDirectory;

    public CodeGenerator(Schema schema, File outputDirectory, File repositoriesOutputDirectory) {
        this.schema = schema;
        this.outputDirectory = outputDirectory;
        this.repositoriesOutputDirectory = repositoriesOutputDirectory;
    }

    public void generate(GeneratorWriter writer) throws SchemaException {
        generateSchema(writer);

        for (SchemaClass klass : sort(schema.getClasses().values())) {
            generateClass(klass, writer);
            generateType(klass, writer);
        }

        for (SchemaEnumType enumType : sort(schema.getEnumTypes().values())) {
            generateEnumType(enumType, writer);
        }

        if (repositoriesOutputDirectory != null) {
            for (SchemaClass klass : sort(schema.getClasses().values())) {
                generateRepository(klass, writer);
            }
        }
    }

    private void generateSchema(GeneratorWriter writer) {
        CodeWriter cw = new CodeWriter();

        cw.writeln("package %s;", getTypesPackageName(null));
        cw.writeln();

        cw.writeln("import nl.gmt.data.DataException;");
        cw.writeln("import nl.gmt.data.EntityType;");
        cw.writeln("import nl.gmt.data.schema.Schema;");
        cw.writeln();

        cw.writeln("public class EntitySchema extends nl.gmt.data.EntitySchema {");
        cw.indent();

        for (SchemaClass klass : schema.getClasses().values()) {
            cw.writeln("private %sType %s;", klass.getName(), getFieldName(klass.getName()));
        }

        cw.writeln();
        cw.writeln("public EntitySchema(Schema schema) throws DataException {");
        cw.indent();

        cw.writeln("super(schema);");

        cw.unIndent();
        cw.writeln("}");
        cw.writeln();

        cw.writeln("@Override");
        cw.writeln("protected EntityType[] createTypes(Schema schema) {");
        cw.indent();

        cw.writeln("return new EntityType[]{");
        cw.indent();

        List<SchemaClass> classes = new ArrayList<>(schema.getClasses().values());

        for (int i = 0; i < classes.size(); i++) {
            SchemaClass klass = classes.get(i);

            cw.writeln(
                "%s = new %sType(schema.getClasses().get(\"%s\"))%s",
                getFieldName(klass.getName()),
                klass.getName(),
                klass.getName(),
                i == classes.size() - 1 ? "" : ","
            );
        }

        cw.unIndent();
        cw.writeln("};");

        cw.unIndent();
        cw.writeln("}");

        for (SchemaClass klass : schema.getClasses().values()) {
            cw.writeln();
            cw.writeln("public %sType get%s() {", klass.getName(), klass.getName());
            cw.indent();

            cw.writeln("return %s;", getFieldName(klass.getName()));

            cw.unIndent();
            cw.writeln("}");
        }

        cw.unIndent();
        cw.writeln("}");

        writer.writeFile(new File(new File(outputDirectory, "types"), "EntitySchema.java"), cw.toString(), true);
    }

    private void generateType(SchemaClass klass, GeneratorWriter writer) {
        CodeWriter cw = new CodeWriter();

        cw.writeln("package %s;", getTypesPackageName(klass));
        cw.writeln();

        cw.writeln("import nl.gmt.data.EntityForeignChild;");
        cw.writeln("import nl.gmt.data.EntityForeignParent;");
        cw.writeln("import nl.gmt.data.EntityProperty;");
        cw.writeln("import nl.gmt.data.schema.SchemaClass;");
        cw.writeln();

        cw.writeln("public class %sType extends nl.gmt.data.EntityType {", klass.getName());
        cw.indent();

        cw.writeln("public %sType(SchemaClass schemaClass) {", klass.getName());
        cw.indent();

        cw.writeln("super(schemaClass);");

        cw.unIndent();
        cw.writeln("}");

        for (SchemaField schemaField : klass.getFields()) {
            cw.writeln();

            if (schemaField instanceof SchemaProperty) {
                cw.writeln("public EntityProperty get%s() {", schemaField.getName());
                cw.indent();

                cw.writeln("return (EntityProperty)getField(\"%s\");", getFieldName(schemaField.getName()));

                cw.unIndent();
                cw.writeln("}");
            } else if (schemaField instanceof SchemaForeignParent) {
                cw.writeln("@SuppressWarnings(\"unchecked\")");
                cw.writeln(
                    "public EntityForeignParent<%sType> get%s() {",
                    ((SchemaForeignParent)schemaField).getClassName(),
                    schemaField.getName()
                );
                cw.indent();

                cw.writeln(
                    "return (EntityForeignParent<%sType>)getField(\"%s\");",
                    ((SchemaForeignParent)schemaField).getClassName(),
                    getFieldName(schemaField.getName())
                );

                cw.unIndent();
                cw.writeln("}");
            } else if (schemaField instanceof SchemaForeignChild) {
                cw.writeln("@SuppressWarnings(\"unchecked\")");
                cw.writeln(
                    "public EntityForeignChild<%sType> get%s() {",
                    ((SchemaForeignChild)schemaField).getClassName(),
                    schemaField.getName()
                );
                cw.indent();

                cw.writeln(
                    "return (EntityForeignChild<%sType>)getField(\"%s\");",
                    ((SchemaForeignChild)schemaField).getClassName(),
                    getFieldName(schemaField.getName())
                );

                cw.unIndent();
                cw.writeln("}");
            }
        }

        cw.unIndent();
        cw.writeln("}");

        File fileName = new File(outputDirectory, "types");

        if (klass.getBoundedContext() != null) {
            fileName = new File(fileName, klass.getBoundedContext());
        }

        fileName = new File(fileName, klass.getName() + "Type.java");

        writer.writeFile(fileName, cw.toString(), true);
    }

    private String getTypesPackageName(SchemaClass klass) {
        return getPackageName("types", klass, null);
    }

    private String getModelPackageName(SchemaClass klass) {
        return getModelPackageName(klass, null);
    }

    private String getModelPackageName(SchemaClass klass, String subPackage) {
        return getPackageName("model", klass, subPackage);
    }

    private String getPackageName(String namespace, SchemaClass klass, String subPackage) {
        String result = schema.getNamespace() + "." + namespace;

        if (subPackage != null) {
            result += "." + subPackage;
        }

        if (klass != null && klass.getBoundedContext() != null) {
            result += "." + klass.getBoundedContext();
        }

        return result;
    }

    private void generateClass(SchemaClass klass, GeneratorWriter writer) throws SchemaException {
        CodeWriter cw = new CodeWriter();

        cw.writeln("package %s;", getModelPackageName(klass));
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

        // Generate the default constructor.

        cw.writeln("public %s() {", klass.getName());
        cw.writeln("}");
        cw.writeln();

        // Generate the constructor with all properties except for the ID and set properties.

        generatePropertyConstructor(cw, klass);

        generateIdProperty(cw, klass.getResolvedIdProperty());

        for (SchemaProperty property : sort(klass.getProperties().values())) {
            cw.writeln();

            generateProperty(cw, property);
        }

        for (SchemaForeignBase foreign : sort(klass.getForeigns().values())) {
            cw.writeln();

            generateForeign(cw, foreign);
        }

        cw.writeln();

        generateClassBuilder(cw, klass);

        cw.unIndent();
        cw.writeln("}");

        File fileName = new File(outputDirectory, "model");

        if (klass.getBoundedContext() != null) {
            fileName = new File(fileName, klass.getBoundedContext());
        }

        fileName = new File(fileName, klass.getName() + ".java");

        writer.writeFile(fileName, cw.toString(), true);
    }

    private void generateClassBuilder(CodeWriter cw, SchemaClass klass) {
        cw.writeln(
            "public static class Builder {",
            klass.getName(),
            getTypeName(klass.getResolvedIdProperty().getResolvedDataType().getNativeType())
        );
        cw.indent();

        for (SchemaProperty property : sort(klass.getProperties().values())) {
            generateBuilderProperty(cw, property);

            cw.writeln();
        }

        for (SchemaForeignBase foreign : sort(klass.getForeigns().values())) {
            if (foreign.getType() == SchemaForeignType.PARENT) {
                generateBuilderForeignParent(cw, (SchemaForeignParent)foreign);

                cw.writeln();
            }
        }

        generateBuilderBuild(cw, klass);

        cw.unIndent();
        cw.writeln("}");
    }

    private void generateBuilderProperty(CodeWriter cw, SchemaProperty property) {
        generateField(cw, property.getResolvedDataType(), property.getEnumType(), property.getName());

        cw.writeln();

        generateBuilderSetter(cw, property.getResolvedDataType(), property.getEnumType(), property.getName());
    }

    private void generateBuilderForeignParent(CodeWriter cw, SchemaForeignParent foreign) {
        cw.writeln(
            "private %s %s;",
            foreign.getClassName(),
            getFieldName(foreign.getName())
        );

        cw.writeln();

        generateBuilderSetter(cw, foreign.getName(), foreign.getClassName());
    }

    private void generateBuilderSetter(CodeWriter cw, SchemaResolvedDataType dataType, String enumType, String name) {
        generateBuilderSetter(
            cw,
            name,
            enumType != null ? enumType : getTypeName(dataType.getNativeType())
        );
    }

    private void generateBuilderSetter(CodeWriter cw, String name, String typeName) {
        String fieldName = getFieldName(name);

        cw.writeln(
            "public Builder set%s(%s %s) {",
            name,
            typeName,
            fieldName
        );
        cw.indent();
        cw.writeln("this.%s = %s;", fieldName, fieldName);
        cw.writeln("return this;");
        cw.unIndent();
        cw.writeln("}");
    }

    private void generateBuilderBuild(CodeWriter cw, SchemaClass klass) {
        cw.writeln("public %s build() {", klass.getName());
        cw.indent();

        StringBuilder sb = new StringBuilder();

        for (SchemaField field : sort(klass.getFields())) {
            String fieldName = getFieldName(field.getName());

            if (field instanceof SchemaProperty) {
                if (sb.length() > 0)
                    sb.append(", ");

                sb.append(String.format(
                    "this.%s", fieldName
                ));
            } else if (field instanceof SchemaForeignParent) {
                if (sb.length() > 0)
                    sb.append(", ");

                sb.append(String.format(
                    "this.%s",
                    fieldName
                ));
            }
        }

        // Write the declaration.

        cw.writeln("return new %s(%s);", klass.getName(), sb.toString());

        cw.unIndent();
        cw.writeln("}");
    }

    private void generatePropertyConstructor(CodeWriter cw, SchemaClass klass) {
        StringBuilder sb = new StringBuilder();
        List<String> assignments = new ArrayList<>();

        for (SchemaField field : sort(klass.getFields())) {
            String fieldName = getFieldName(field.getName());

            if (field instanceof SchemaProperty) {
                SchemaProperty property = (SchemaProperty)field;

                if (sb.length() > 0)
                    sb.append(", ");

                sb.append(String.format(
                    "%s %s",
                    property.getEnumType() != null ? property.getEnumType() : getTypeName(property.getResolvedDataType().getNativeType()),
                    fieldName
                ));

                assignments.add(String.format("this.%s = %s;", fieldName, fieldName));
            } else if (field instanceof SchemaForeignParent) {
                SchemaForeignParent foreign = (SchemaForeignParent)field;

                if (sb.length() > 0)
                    sb.append(", ");

                sb.append(String.format(
                    "%s %s",
                    foreign.getClassName(),
                    fieldName
                ));

                assignments.add(String.format("this.%s = %s;", fieldName, fieldName));
            }
        }

        // If we didn't have any properties, don't generate a constructor because we already have the
        // default constructor.

        if (sb.length() == 0)
            return;

        // Write the declaration.

        cw.writeln("private %s(%s) {", klass.getName(), sb.toString());
        cw.indent();

        // Write all assignments.

        for (String assignment : assignments) {
            cw.writeln(assignment);
        }

        // And close the method.

        cw.unIndent();
        cw.writeln("}");
        cw.writeln();
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
        generateGetterSetter(
            cw,
            name,
            enumType != null ? enumType : getTypeName(dataType.getNativeType()),
            dataType.getNativeType() == Boolean.class
        );
    }

    private void generateGetterSetter(CodeWriter cw, String name, String typeName, boolean isBoolean) {
        String fieldName = getFieldName(name);

        cw.writeln(
            "public %s %s%s() {",
            typeName,
            isBoolean ? "is" : "get",
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
        else if (klass == java.util.UUID.class || klass == java.util.Date.class)
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
        return getFieldName(name, true);
    }

    private String getFieldName(String name, boolean safe) {
        String result = name.substring(0, 1).toLowerCase() + name.substring(1);

        if (safe && RESERVED_WORDS.contains(result)) {
            result += "_";
        }

        return result;
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

        cw.writeln("@OneToMany(mappedBy = \"%s\")", getFieldName(foreign.getClassProperty(), false));

        generateGetterSetter(cw, foreign.getName(), "Set<" + linkClass.getName() + ">", false);
    }

    private void generateForeignParent(CodeWriter cw, SchemaForeignParent foreign) {
        cw.writeln(
            "private %s %s;",
            foreign.getClassName(),
            getFieldName(foreign.getName())
        );

        cw.writeln();

        cw.writeln("@ManyToOne");

        cw.writeln("@JoinColumn(name = \"%s\")", StringEscapeUtils.escapeJava(foreign.getResolvedDbName()));

        generateGetterSetter(cw, foreign.getName(), foreign.getClassName(), false);
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

        List<SchemaEnumTypeField> fields = sort(enumType.getFields().values());

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

        writer.writeFile(new File(new File(outputDirectory, "model"), enumType.getName() + ".java"), cw.toString(), true);
    }

    private void generateRepository(SchemaClass klass, GeneratorWriter writer) throws SchemaException {
        generateRepositoryInterface(klass, writer);
        generateRepositoryImplementation(klass, writer);
    }

    private void generateRepositoryInterface(SchemaClass klass, GeneratorWriter writer) {
        CodeWriter cw = new CodeWriter();

        cw.writeln("package %s;", getModelPackageName(klass));
        cw.writeln();

        cw.writeln("import nl.gmt.data.Repository;");
        cw.writeln();

        cw.writeln(
            "public interface %sRepository extends Repository<%s> {",
            klass.getName(),
            klass.getName()
        );
        cw.writeln("}");

        File fileName = new File(repositoriesOutputDirectory, "model");

        if (klass.getBoundedContext() != null) {
            fileName = new File(fileName, klass.getBoundedContext());
        }

        fileName = new File(fileName, klass.getName() + "Repository.java");

        writer.writeFile(fileName, cw.toString(), false);
    }

    private void generateRepositoryImplementation(SchemaClass klass, GeneratorWriter writer) {
        CodeWriter cw = new CodeWriter();

        cw.writeln("package %s;", getModelPackageName(klass, "implementation"));
        cw.writeln();

        cw.writeln("import nl.gmt.data.hibernate.HibernateRepository;");
        cw.writeln();

        cw.writeln("import %s.%s;", getModelPackageName(klass), klass.getName());
        cw.writeln("import %s.%sRepository;", getModelPackageName(klass), klass.getName());
        cw.writeln();

        cw.writeln("@SuppressWarnings(\"UnusedDeclaration\")");
        cw.writeln(
            "public class %sRepositoryImpl extends HibernateRepository<%s> implements %sRepository {",
            klass.getName(),
            klass.getName(),
            klass.getName()
        );
        cw.indent();

        cw.writeln("public %sRepositoryImpl() {", klass.getName());
        cw.indent();

        cw.writeln("super(%s.class);", klass.getName());

        cw.unIndent();
        cw.writeln("}");

        cw.unIndent();
        cw.writeln("}");

        File fileName = new File(new File(repositoriesOutputDirectory, "model"), "implementation");

        if (klass.getBoundedContext() != null) {
            fileName = new File(fileName, klass.getBoundedContext());
        }

        fileName = new File(fileName, klass.getName() + "RepositoryImpl.java");

        writer.writeFile(fileName, cw.toString(), false);
    }

    @SuppressWarnings("unchecked")
    private <T extends Comparable> List<T> sort(Collection<? extends T> collection) {
        List<T> result = new ArrayList<>(collection);

        Collections.sort(result);

        return result;
    }
}
