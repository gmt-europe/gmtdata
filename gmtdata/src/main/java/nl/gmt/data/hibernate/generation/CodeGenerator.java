package nl.gmt.data.hibernate.generation;

import nl.gmt.data.contrib.joda.PersistentDateTime;
import nl.gmt.data.contrib.joda.PersistentLocalDateTime;
import nl.gmt.data.schema.*;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;

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
    private final String packageName;
    private final boolean generateSchema;

    public CodeGenerator(Schema schema, File outputDirectory, File repositoriesOutputDirectory, String packageName, boolean generateSchema) {
        this.schema = schema;
        this.outputDirectory = outputDirectory;
        this.repositoriesOutputDirectory = repositoriesOutputDirectory;
        this.packageName = packageName;
        this.generateSchema = generateSchema;
    }

    public void generate(GeneratorWriter writer) throws SchemaException {
        if (generateSchema) {
            generateSchema(writer);
        }

        for (SchemaMixin mixin : sort(schema.getMixins().values())) {
            if (packageNameMatches(mixin.getPackageName())) {
                generateInterface(mixin, writer);
            }
        }

        for (SchemaClass klass : sort(schema.getClasses().values())) {
            if (packageNameMatches(klass.getPackageName())) {
                generateClass(klass, writer);
                generateType(klass, writer);
            }
        }

        for (SchemaEnumType enumType : sort(schema.getEnumTypes().values())) {
            if (packageNameMatches(enumType.getPackageName())) {
                generateEnumType(enumType, writer);
            }
        }

        if (repositoriesOutputDirectory != null) {
            for (SchemaClass klass : sort(schema.getClasses().values())) {
                if (packageNameMatches(klass.getPackageName())) {
                    generateRepository(klass, writer);
                }
            }
        }
    }

    private boolean packageNameMatches(String packageName) {
        return
            this.packageName != null &&
            packageName != null &&
            packageName.equals(this.packageName);
    }

    private void generateSchema(GeneratorWriter writer) {
        CodeWriter cw = new CodeWriter();

        cw.writeln("package %s.types;", packageName);
        cw.writeln();

        cw.writeln("public class EntitySchema extends nl.gmt.data.EntitySchema {");
        cw.indent();

        for (SchemaClass klass : schema.getClasses().values()) {
            cw.writeln("private %s %s;", getTypeClassName(klass.getFullName()), getFieldName(klass.getName()));
        }

        cw.writeln();
        cw.writeln("public EntitySchema(nl.gmt.data.schema.Schema schema) throws nl.gmt.data.DataException {");
        cw.indent();

        cw.writeln("super(schema);");

        cw.unIndent();
        cw.writeln("}");
        cw.writeln();

        cw.writeln("@Override");
        cw.writeln("protected nl.gmt.data.EntityType[] createTypes(nl.gmt.data.schema.Schema schema) {");
        cw.indent();

        cw.writeln("return new nl.gmt.data.EntityType[]{");
        cw.indent();

        List<SchemaClass> classes = new ArrayList<>(schema.getClasses().values());

        for (int i = 0; i < classes.size(); i++) {
            SchemaClass klass = classes.get(i);

            cw.writeln(
                "%s = new %s(schema.getClasses().get(\"%s\"))%s",
                getFieldName(klass.getName()),
                getTypeClassName(klass.getFullName()),
                klass.getFullName(),
                i == classes.size() - 1 ? "" : ","
            );
        }

        cw.unIndent();
        cw.writeln("};");

        cw.unIndent();
        cw.writeln("}");

        for (SchemaClass klass : schema.getClasses().values()) {
            cw.writeln();
            cw.writeln("public %s get%s() {", getTypeClassName(klass.getFullName()), klass.getName());
            cw.indent();

            cw.writeln("return %s;", getFieldName(klass.getName()));

            cw.unIndent();
            cw.writeln("}");
        }

        cw.unIndent();
        cw.writeln("}");

        File fileName = buildFileName(outputDirectory, packageName, "types", "EntitySchema.java");

        writer.writeFile(fileName, cw.toString(), true);
    }

    private void generateType(SchemaClass klass, GeneratorWriter writer) throws SchemaException {
        CodeWriter cw = new CodeWriter();

        cw.writeln("package %s.types;", packageName);
        cw.writeln();

        cw.writeln("public class %sType extends nl.gmt.data.EntityType {", klass.getName());
        cw.indent();

        cw.writeln("public %sType(nl.gmt.data.schema.SchemaClass schemaClass) {", klass.getName());
        cw.indent();

        cw.writeln("super(schemaClass, %s.%s.class);", getModelPackageName(klass), klass.getName());

        cw.unIndent();
        cw.writeln("}");

        generateTypeProperties(cw, klass);

        generateTypeMixins(cw, klass.getMixins(), new HashSet<SchemaMixin>());

        cw.unIndent();
        cw.writeln("}");

        File fileName = buildFileName(outputDirectory, klass.getPackageName(), "types", klass.getName() + "Type.java");

        writer.writeFile(fileName, cw.toString(), true);
    }

    private void generateTypeMixins(CodeWriter cw, List<String> mixins, Set<SchemaMixin> seen) throws SchemaException {
        for (String name : mixins) {
            SchemaMixin mixin = schema.getMixins().get(name);

            if (seen.contains(mixin))
                continue;
            seen.add(mixin);

            generateTypeProperties(cw, mixin);

            generateTypeMixins(cw, mixin.getMixins(), seen);
        }
    }

    private void generateTypeProperties(CodeWriter cw, SchemaClassBase klass) {
        for (SchemaField schemaField : klass.getFields()) {
            cw.writeln();

            if (schemaField instanceof SchemaProperty) {
                cw.writeln("public nl.gmt.data.EntityProperty get%s() {", schemaField.getName());
                cw.indent();

                cw.writeln("return (nl.gmt.data.EntityProperty)getField(\"%s\");", getFieldName(schemaField.getName()));

                cw.unIndent();
                cw.writeln("}");
            } else if (schemaField instanceof SchemaForeignParent) {
                cw.writeln("@SuppressWarnings(\"unchecked\")");
                cw.writeln(
                    "public nl.gmt.data.EntityForeignParent<%s> get%s() {",
                    getTypeClassName(((SchemaForeignParent)schemaField).getClassName()),
                    schemaField.getName()
                );
                cw.indent();

                cw.writeln(
                    "return (nl.gmt.data.EntityForeignParent<%s>)getField(\"%s\");",
                    getTypeClassName(((SchemaForeignParent)schemaField).getClassName()),
                    getFieldName(schemaField.getName())
                );

                cw.unIndent();
                cw.writeln("}");
            } else if (schemaField instanceof SchemaForeignChild) {
                cw.writeln("@SuppressWarnings(\"unchecked\")");
                cw.writeln(
                    "public nl.gmt.data.EntityForeignChild<%s> get%s() {",
                    getTypeClassName(((SchemaForeignChild)schemaField).getClassName()),
                    schemaField.getName()
                );
                cw.indent();

                cw.writeln(
                    "return (nl.gmt.data.EntityForeignChild<%s>)getField(\"%s\");",
                    getTypeClassName(((SchemaForeignChild)schemaField).getClassName()),
                    getFieldName(schemaField.getName())
                );

                cw.unIndent();
                cw.writeln("}");
            }
        }
    }

    private File buildFileName(File outputDirectory, String... parts) {
        File file = outputDirectory;

        for (int i = 0; i < parts.length - 1; i++) {
            for (String part : StringUtils.split(parts[i], '.')) {
                file = new File(file, part);
            }
        }

        // The last element is the file name. Don't process this for dots.

        return new File(file, parts[parts.length - 1]);
    }

    private String getTypesPackageName(SchemaClassBase klass) {
        return getTypesPackageName(klass, null);
    }

    private String getTypesPackageName(SchemaClassBase klass, String subPackage) {
        return getPackageName("types", klass, subPackage);
    }

    private String getModelPackageName(SchemaClassBase klass) {
        return getModelPackageName(klass, null);
    }

    private String getModelPackageName(SchemaClassBase klass, String subPackage) {
        return getPackageName("model", klass, subPackage);
    }

    private String getPackageName(String namespace, SchemaClassBase klass, String subPackage) {
        String result = klass.getPackageName();

        if (namespace != null) {
            result += "." + namespace;
        }

        if (subPackage != null) {
            result += "." + subPackage;
        }

        return result;
    }

    private void generateInterface(SchemaMixin mixin, GeneratorWriter writer) throws SchemaException {
        CodeWriter cw = new CodeWriter();

        cw.writeln("package %s;", getModelPackageName(mixin));
        cw.writeln();

        List<String> interfaces = new ArrayList<>();

        resolveMixins(interfaces, mixin.getMixins());

        cw.writeln(
            "public interface %s%s {",
            mixin.getName(),
            interfaces.size() == 0 ? "" : " extends " + StringUtils.join(interfaces, ", ")
        );
        cw.indent();

        // Generate accessors for the properties.

        for (SchemaProperty property : sort(mixin.getProperties().values())) {
            cw.writeln();

            generateInterfaceGetterSetter(cw, property.getResolvedDataType(), getEnumType(property.getEnumType()), property.getName());
        }

        for (SchemaForeignBase foreign : sort(mixin.getForeigns().values())) {
            cw.writeln();

            switch (foreign.getType()) {
                case CHILD:
                    SchemaClass linkClass = schema.getClasses().get(foreign.getClassName());
                    generateInterfaceGetterSetter(cw, foreign.getName(), "java.util.Set<" + getClassName(linkClass.getName()) + ">", false);
                    break;

                case PARENT:
                    generateInterfaceGetterSetter(cw, foreign.getName(), getClassName(foreign.getClassName()), false);
                    break;
            }
        }

        cw.unIndent();
        cw.writeln("}");

        File fileName = buildFileName(outputDirectory, mixin.getPackageName(), "model", mixin.getName() + ".java");

        writer.writeFile(fileName, cw.toString(), true);
    }

    private void resolveMixins(List<String> interfaces, List<String> mixins) {
        for (String name : mixins) {
            SchemaMixin mixin = schema.getMixins().get(name);

            interfaces.add(getClassName(mixin));
        }
    }

    private void generateClass(SchemaClass klass, GeneratorWriter writer) throws SchemaException {
        CodeWriter cw = new CodeWriter();

        cw.writeln("package %s;", getModelPackageName(klass));
        cw.writeln();

        List<String> interfaces = new ArrayList<>();

        resolveMixins(interfaces, klass.getMixins());

        cw.writeln("@javax.persistence.Entity");
        cw.writeln("@javax.persistence.Table(name = \"`%s`\")", StringEscapeUtils.escapeJava(klass.getResolvedDbName()));
        cw.writeln(
            "public class %s extends nl.gmt.data.Entity%s {",
            klass.getName(),
            interfaces.size() == 0 ? "" : " implements " + StringUtils.join(interfaces, ", ")
        );
        cw.indent();

        // Generate the default constructor.

        cw.writeln("public %s() {", klass.getName());
        cw.writeln("}");
        cw.writeln();

        // Generate the constructor with all properties except for the ID and set properties.

        generatePropertyConstructor(cw, klass);

        generateIdProperty(cw, klass.getResolvedIdProperty());

        generatePropertiesAndForeigns(cw, klass);

        generateClassMixins(cw, klass.getMixins(), new HashSet<SchemaMixin>());

        cw.writeln();

        generateClassBuilder(cw, klass);

        cw.unIndent();
        cw.writeln("}");

        File fileName = buildFileName(outputDirectory, klass.getPackageName(), "model", klass.getName() + ".java");

        writer.writeFile(fileName, cw.toString(), true);
    }

    private void generatePropertiesAndForeigns(CodeWriter cw, SchemaClassBase klass) throws SchemaException {
        for (SchemaProperty property : sort(klass.getProperties().values())) {
            cw.writeln();

            generateProperty(cw, property);
        }

        for (SchemaForeignBase foreign : sort(klass.getForeigns().values())) {
            cw.writeln();

            generateForeign(cw, foreign);
        }
    }

    private void generateClassMixins(CodeWriter cw, List<String> mixins, Set<SchemaMixin> seen) throws SchemaException {
        for (String name : mixins) {
            SchemaMixin mixin = schema.getMixins().get(name);

            if (seen.contains(mixin))
                continue;
            seen.add(mixin);

            generatePropertiesAndForeigns(cw, mixin);

            generateClassMixins(cw, mixin.getMixins(), seen);
        }
    }

    private void generateClassBuilder(CodeWriter cw, SchemaClass klass) throws SchemaException {
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
        generateField(cw, property.getResolvedDataType(), getEnumType(property.getEnumType()), property.getName());

        cw.writeln();

        generateBuilderSetter(cw, property.getResolvedDataType(), getEnumType(property.getEnumType()), property.getName());
    }

    private String getEnumType(String enumType) {
        if (enumType == null) {
            return null;
        }

        int pos = enumType.lastIndexOf('.');

        assert pos != -1;

        return enumType.substring(0, pos) + ".model." + enumType.substring(pos + 1);
    }

    private void generateBuilderForeignParent(CodeWriter cw, SchemaForeignParent foreign) throws SchemaException {
        cw.writeln(
            "private %s %s;",
            getClassName(foreign.getClassName()),
            getFieldName(foreign.getName())
        );

        cw.writeln();

        generateBuilderSetter(cw, foreign.getName(), getClassName(foreign.getClassName()));
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

    private void generatePropertyConstructor(CodeWriter cw, SchemaClass klass) throws SchemaException {
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
                    property.getEnumType() != null ? getEnumType(property.getEnumType()) : getTypeName(property.getResolvedDataType().getNativeType()),
                    fieldName
                ));

                assignments.add(String.format("this.%s = %s;", fieldName, fieldName));
            } else if (field instanceof SchemaForeignParent) {
                SchemaForeignParent foreign = (SchemaForeignParent)field;

                if (sb.length() > 0)
                    sb.append(", ");

                sb.append(String.format(
                    "%s %s",
                    getClassName(foreign.getClassName()),
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

    private String getClassName(String className) throws SchemaException {
        SchemaClass klass = schema.getClasses().get(className);
        if (klass == null) {
            throw new SchemaException(String.format("Cannot resolve class '%s'", className));
        }
        return getClassName(klass);
    }

    private String getClassName(SchemaClassBase klass) {
        return klass.getPackageName() + ".model." + klass.getName();
    }

    private String getTypeClassName(String className) {
        return getTypeClassName(schema.getClasses().get(className));
    }

    private String getTypeClassName(SchemaClassBase klass) {
        return klass.getPackageName() + ".types." + klass.getName() + "Type";
    }

    private void generateIdProperty(CodeWriter cw, SchemaClassIdProperty property) throws SchemaException {
        if (property.getCompositeId() != null)
            throw new SchemaException("Composite ID's are not supported");

        generateField(cw, property.getResolvedDataType(), null, "Id");

        cw.writeln();

        cw.writeln("@javax.persistence.Id");

        if (property.getAutoIncrement() == SchemaIdAutoIncrement.YES) {
            cw.writeln("@javax.persistence.GeneratedValue(strategy = javax.persistence.GenerationType.IDENTITY)");
        } else if (property.getDbSequence() != null) {
            cw.writeln("@javax.persistence.GeneratedValue(strategy = javax.persistence.GenerationType.SEQUENCE, generator = \"SEQ_GEN\")");
            cw.writeln(
                "@javax.persistence.SequenceGenerator(name = \"SEQ_GEN\", sequenceName = \"%s\")",
                StringEscapeUtils.escapeJava(property.getDbSequence())
            );
        } else if (property.getGenerator() != null) {
            SchemaGenerator generator = property.getGenerator();

            cw.writeln(
                "@javax.persistence.GeneratedValue(generator = \"%s\")",
                StringEscapeUtils.escapeJava(generator.getName())
            );

            StringBuilder sb = new StringBuilder();

            sb.append("@org.hibernate.annotations.GenericGenerator(name = \"");
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

                    sb.append("@org.hibernate.annotations.Parameter(name = \"");
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

        generateColumnAnnotations(cw, property.getResolvedDbName(), property.getResolvedDataType());

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

    private void generateInterfaceGetterSetter(CodeWriter cw, String name, String typeName, boolean isBoolean) {
        String fieldName = getFieldName(name);

        cw.writeln(
            "%s %s%s();",
            typeName,
            isBoolean ? "is" : "get",
            name
        );
        cw.writeln();
        cw.writeln(
            "void set%s(%s %s);",
            name,
            typeName,
            fieldName
        );
    }

    private void generateInterfaceGetterSetter(CodeWriter cw, SchemaResolvedDataType dataType, String enumType, String name) {
        generateInterfaceGetterSetter(
            cw,
            name,
            enumType != null ? enumType : getTypeName(dataType.getNativeType()),
            dataType.getNativeType() == Boolean.class
        );
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
        else if (klass == java.util.UUID.class || klass == java.util.Date.class || klass == java.math.BigDecimal.class || klass == org.joda.time.DateTime.class || klass == org.joda.time.LocalDateTime.class)
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
        if (dataType.getNativeType() == org.joda.time.DateTime.class) {
            cw.writeln(
                "@org.hibernate.annotations.Type(type=\"%s\")",
                PersistentDateTime.class.getName()
            );
        } else if (dataType.getNativeType() == org.joda.time.LocalDateTime.class) {
            cw.writeln(
                "@org.hibernate.annotations.Type(type=\"%s\")",
                PersistentLocalDateTime.class.getName()
            );
        }

        StringBuilder sb = new StringBuilder();

        sb.append("@javax.persistence.Column(name = \"`");
        sb.append(StringEscapeUtils.escapeJava(dbName));
        sb.append("`\"");

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
                cw.writeln("@javax.persistence.Lob");
                break;
        }
         */

        if (dataType.getLazy() == SchemaLazy.LAZY)
            cw.writeln("@javax.persistence.Basic(fetch = FetchType.LAZY");
    }

    private void generateProperty(CodeWriter cw, SchemaProperty property) {
        generateField(cw, property.getResolvedDataType(), getEnumType(property.getEnumType()), property.getName());

        cw.writeln();

        generateColumnAnnotations(cw, property.getResolvedDbName(), property.getResolvedDataType());

        if (property.getEnumType() != null)
            cw.writeln("@org.hibernate.annotations.Type(type = \"%s$UserType\")", getEnumType(property.getEnumType()));

        generateGetterSetter(cw, property.getResolvedDataType(), getEnumType(property.getEnumType()), property.getName());
    }

    private void generateForeign(CodeWriter cw, SchemaForeignBase foreign) throws SchemaException {
        switch (foreign.getType()) {
            case CHILD: generateForeignChild(cw, (SchemaForeignChild) foreign); break;
            case PARENT: generateForeignParent(cw, (SchemaForeignParent) foreign); break;
        }
    }

    private void generateForeignChild(CodeWriter cw, SchemaForeignChild foreign) throws SchemaException {
        SchemaClass linkClass = schema.getClasses().get(foreign.getClassName());

        cw.writeln(
            "private java.util.Set<%s> %s;",
            getClassName(linkClass),
            getFieldName(foreign.getName())
        );

        cw.writeln();

        cw.writeln("@javax.persistence.OneToMany(mappedBy = \"%s\", fetch = javax.persistence.FetchType.LAZY)", getFieldName(foreign.getClassProperty(), false));

        generateGetterSetter(cw, foreign.getName(), "java.util.Set<" + getClassName(linkClass) + ">", false);
    }

    private void generateForeignParent(CodeWriter cw, SchemaForeignParent foreign) throws SchemaException {
        cw.writeln(
            "private %s %s;",
            getClassName(foreign.getClassName()),
            getFieldName(foreign.getName())
        );

        cw.writeln();

        cw.writeln("@javax.persistence.ManyToOne(fetch = javax.persistence.FetchType.LAZY)");

        cw.writeln("@javax.persistence.JoinColumn(name = \"`%s`\")", StringEscapeUtils.escapeJava(foreign.getResolvedDbName()));

        generateGetterSetter(cw, foreign.getName(), getClassName(foreign.getClassName()), false);
    }

    private void generateEnumType(SchemaEnumType enumType, GeneratorWriter writer) {
        CodeWriter cw = new CodeWriter();

        cw.writeln("package %s.model;", enumType.getPackageName());
        cw.writeln();

        cw.writeln("public enum %s implements nl.gmt.data.hibernate.PersistentEnum {", enumType.getName());
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
            "public static class UserType extends nl.gmt.data.hibernate.PersistentEnumUserType<%s> {",
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

        File fileName = buildFileName(outputDirectory, enumType.getPackageName(), "model", enumType.getName() + ".java");

        writer.writeFile(fileName, cw.toString(), true);
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

        File fileName = buildFileName(repositoriesOutputDirectory, klass.getPackageName(), "model", klass.getName() + "Repository.java");

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

        File fileName = buildFileName(repositoriesOutputDirectory, klass.getPackageName(), "model", "implementation", klass.getName() + "RepositoryImpl.java");

        writer.writeFile(fileName, cw.toString(), false);
    }

    @SuppressWarnings("unchecked")
    private <T extends Comparable> List<T> sort(Collection<? extends T> collection) {
        List<T> result = new ArrayList<>(collection);

        Collections.sort(result);

        return result;
    }
}
