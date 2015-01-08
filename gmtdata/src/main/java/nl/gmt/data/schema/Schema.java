package nl.gmt.data.schema;

import nl.gmt.data.migrate.SqlGenerator;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.*;

public class Schema extends SchemaAnnotatableElement {
    private String namespace;
    private String enumDataType;
    private SchemaParserLocation namespaceLocation;
    private SchemaParserLocation enumDataTypeLocation;
    private final Map<String, SchemaDataType> dataTypes = new HashMap<>();
    private final Map<String, SchemaDataType> unmodifiableDataTypes = Collections.unmodifiableMap(dataTypes);
    private final Map<String, SchemaClass> classes = new HashMap<>();
    private final Map<String, SchemaClass> unmodifiableClasses = Collections.unmodifiableMap(classes);
    private final Map<String, SchemaEnumType> enumTypes = new HashMap<>();
    private final Map<String, SchemaEnumType> unmodifiableEnumTypes = Collections.unmodifiableMap(enumTypes);
    private SchemaIdProperty idProperty;
    private List<String> baseIncludes;
    private String schemaHash;
    private final List<String> suppressResourceGeneration = new ArrayList<>();
    private final List<String> unmodifiableSuppressResourceGeneration = Collections.unmodifiableList(suppressResourceGeneration);

    Schema() {
        super(null);
    }

    public String getNamespace() {
        return namespace;
    }

    void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getEnumDataType() {
        return enumDataType;
    }

    void setEnumDataType(String enumDataType) {
        this.enumDataType = enumDataType;
    }

    public SchemaParserLocation getNamespaceLocation() {
        return namespaceLocation;
    }

    void setNamespaceLocation(SchemaParserLocation namespaceLocation) {
        this.namespaceLocation = namespaceLocation;
    }

    public SchemaParserLocation getEnumDataTypeLocation() {
        return enumDataTypeLocation;
    }

    void setEnumDataTypeLocation(SchemaParserLocation enumDataTypeLocation) {
        this.enumDataTypeLocation = enumDataTypeLocation;
    }

    public Map<String, SchemaDataType> getDataTypes() {
        return unmodifiableDataTypes;
    }

    public Map<String, SchemaClass> getClasses() {
        return unmodifiableClasses;
    }

    public Map<String, SchemaEnumType> getEnumTypes() {
        return unmodifiableEnumTypes;
    }

    public SchemaIdProperty getIdProperty() {
        return idProperty;
    }

    void setIdProperty(SchemaIdProperty idProperty) {
        this.idProperty = idProperty;
    }

    public List<String> getBaseIncludes() {
        return baseIncludes;
    }

    void setBaseIncludes(List<String> baseIncludes) {
        this.baseIncludes = baseIncludes;
    }

    public String getSchemaHash() {
        return schemaHash;
    }

    void setSchemaHash(String schemaHash) {
        this.schemaHash = schemaHash;
    }

    public List<String> getSuppressResourceGeneration() {
        return unmodifiableSuppressResourceGeneration;
    }

    void validate(SchemaRules rules) throws SchemaException {
        resolveDataTypes(rules);
        resolveDbNames();
        validateSettings();
        validateForeignKeys();
        validateIndexConflicts();
        validateEnumTypes();
        validateDecimalProperties();
        validateIndexes();
        validateEnumTypeFields();
    }

    private void resolveDataTypes(SchemaRules rules) throws SchemaException {
        for (SchemaDataType dataType : dataTypes.values()) {
            resolveDataTypeElement(dataType);
        }

        if (idProperty != null && idProperty.getType() != null)
            idProperty.setResolvedDataType(SchemaResolvedDataType.create(this, rules, idProperty.getType(), idProperty.getLocation()));

        for (SchemaClass klass : classes.values()) {
            if (klass.getIdProperty() != null && klass.getIdProperty().getType() != null)
                klass.getIdProperty().setResolvedDataType(SchemaResolvedDataType.create(this, rules, klass.getIdProperty().getType(), klass.getIdProperty().getLocation()));

            klass.setResolvedIdProperty(createResolvedIdProperty(klass));

            for (SchemaProperty property : klass.getProperties().values()) {
                resolveDataTypeElement(property);

                if (property.getType() != null)
                    property.setResolvedDataType(SchemaResolvedDataType.create(this, rules, property));
            }
        }
    }

    private void resolveDbNames() {
        for (SchemaClass klass : classes.values()) {
            klass.setResolvedDbName(
                klass.getDbName() != null
                    ? klass.getDbName()
                    : klass.getName()
            );
            klass.getResolvedIdProperty().setResolvedDbIdName(
                klass.getResolvedIdProperty().getDbIdName() != null
                    ? klass.getResolvedIdProperty().getDbIdName()
                    : idProperty.getName()
            );

            for (SchemaProperty property : klass.getProperties().values()) {
                property.setResolvedDbName(
                    property.getDbName() != null
                        ? property.getDbName()
                        : property.getName()
                );
            }

            for (SchemaForeignBase foreign : klass.getForeigns().values()) {
                if (foreign.getType() != SchemaForeignType.PARENT)
                    continue;

                SchemaForeignParent foreignParent = (SchemaForeignParent)foreign;

                foreignParent.setResolvedDbName(
                    foreignParent.getDbName() != null
                      ? foreignParent.getDbName()
                        : (foreignParent.getName() + idProperty.getForeignPostfix())
                );
            }
        }
    }

    private SchemaClassIdProperty createResolvedIdProperty(SchemaClass klass) {
        SchemaClassIdProperty result = new SchemaClassIdProperty(null);

        if (idProperty != null) {
            result.setType(idProperty.getType());
            result.setAutoIncrement(idProperty.getAutoIncrement());
            result.setGenerator(idProperty.getGenerator());
            result.setResolvedDataType(idProperty.getResolvedDataType());
            result.setTags(idProperty.getTags());
            result.setComments(idProperty.getComments());
        }

        SchemaClassIdProperty klassIdProperty = klass.getIdProperty();
        if (klassIdProperty != null) {
            result.setDbSequence(klassIdProperty.getDbSequence());
            result.setDbIdName(klassIdProperty.getDbIdName());
            result.setCompositeId(klassIdProperty.getCompositeId());

            if (klassIdProperty.getType() != null)
                result.setType(klassIdProperty.getType());
            if (klassIdProperty.getAutoIncrement() != SchemaIdAutoIncrement.UNSET)
                result.setAutoIncrement(klassIdProperty.getAutoIncrement());
            if (klassIdProperty.getGenerator() != null)
                result.setGenerator(klassIdProperty.getGenerator());
            if (klassIdProperty.getResolvedDataType() != null)
                result.setResolvedDataType(klassIdProperty.getResolvedDataType());
            if (klassIdProperty.getComments() != null)
                result.setComments(klassIdProperty.getComments());
            if (klassIdProperty.getTags() != null)
                result.setTags(klassIdProperty.getTags());
        }

        if (result.getAutoIncrement() == SchemaIdAutoIncrement.UNSET)
            result.setAutoIncrement(SchemaIdAutoIncrement.NO);

        return result;
    }

    private void resolveDataTypeElement(SchemaDataTypeBase element) throws SchemaException {
        if (
            !StringUtils.equals(element.getLocation().getFileName(), "resource://System.schema") &&
            element.getRawDbType() != null
        )
            throw new SchemaException("Illegal database type", element.getLocation());

        element.setType(element.getRawType());

        if (element.getType() == null && element.getEnumType() != null)
            element.setType(enumDataType);

        element.setDbType(
            element.getRawDbType() == null
                ? SchemaDbType.UNSET
                : SqlGenerator.getDbType(element.getRawDbType())
        );
    }

    private void validateSettings() throws SchemaException {
        if (idProperty == null)
            throw new SchemaException("ID property invalid");
    }

    private void validateEnumTypes() throws SchemaException {
        for (SchemaDataType dataType : dataTypes.values()) {
            if (dataType.getEnumType() != null && !enumTypes.containsKey(dataType.getEnumType()))
                throw new SchemaException(String.format("Enum type '%s' not found", dataType.getEnumType()));
        }

        for (SchemaClass klass : classes.values()) {
            for (SchemaProperty property : klass.getProperties().values()) {
                if (property.getEnumType() != null && !enumTypes.containsKey(property.getEnumType()))
                    throw new SchemaException(String.format("Enum type '%s' not found", property.getEnumType()));
            }
        }
    }

    private void validateForeignKeys() throws SchemaException {
        for (SchemaClass klass : classes.values()) {
            for (SchemaForeignBase foreign : klass.getForeigns().values()) {
                SchemaClass linkedClass = classes.get(foreign.getClassName());

                if (linkedClass == null)
                    throw new SchemaException(String.format("Class '%s' not found", foreign.getClassName()));

                switch (foreign.getType()) {
                    case CHILD:
                        SchemaForeignChild foreignChild = (SchemaForeignChild)foreign;

                        SchemaForeignBase linkForeign = linkedClass.getForeigns().get(foreignChild.getClassProperty());
                        if (linkForeign == null)
                            throw new SchemaException(String.format("Foreign child property '%s' not found", foreignChild.getClassProperty()), foreign.getLocation());
                        if (linkForeign.getType() != SchemaForeignType.PARENT)
                            throw new SchemaException(String.format("Foreign child property '%s' is not a parent", foreignChild.getClassName()), foreign.getLocation());
                        break;
                }
            }
        }
    }

    private void validateIndexConflicts() throws SchemaException {
        for (SchemaClass klass : classes.values()) {
            for (SchemaIndex index : klass.getIndexes()) {
                for (SchemaIndex conflicting : klass.getIndexes()) {
                    if (index != conflicting && index.conflictsWith(conflicting))
                        throw new SchemaException(String.format("Index '%s' conflicts with index '%s'", index.toSmallString(), conflicting.toSmallString()), index.getLocation());
                }

                for (SchemaForeignBase foreign : klass.getForeigns().values()) {
                    if (
                        foreign.getType() == SchemaForeignType.PARENT &&
                        index.conflictsWith((SchemaForeignParent)foreign)
                    )
                        throw new SchemaException(String.format("Index '%s' conflicts with foreign '%s'", index.toSmallString(), foreign.getName()), index.getLocation());
                }
            }
        }
    }

    private void validateDecimalProperties() throws SchemaException {
        for (SchemaClass klass : classes.values()) {
            for (SchemaProperty property : klass.getProperties().values()) {
                if (
                    property.getResolvedDataType() != null &&
                    property.getResolvedDataType().getNativeType() == BigDecimal.class && (
                        property.getResolvedDataType().getLength() == -1 ||
                        property.getResolvedDataType().getPositions() == -1
                    )
                )
                    throw new SchemaException(String.format("Decimal property '%s' has no length or positions set", property.getName()), property.getLocation());
            }
        }
    }

    private void validateIndexes() throws SchemaException {
        for (SchemaClass klass : classes.values()) {
            for (SchemaIndex index : klass.getIndexes()) {
                if (index.getFields().size() == 0)
                    throw new SchemaException("Index has zero fields", index.getLocation());
            }
        }
    }

    private void validateEnumTypeFields() throws SchemaException {
        for (SchemaEnumType enumType : enumTypes.values()) {
            if (enumType.getFields().size() == 0)
                throw new SchemaException(String.format("Enum type '%s' has zero fields", enumType.getName()), enumType.getLocation());
        }
    }

    void addDataType(SchemaDataType dataType) {
        dataTypes.put(dataType.getName(), dataType);
    }

    void addClass(SchemaClass klass) {
        classes.put(klass.getName(), klass);
    }

    void addEnumType(SchemaEnumType enumType) {
        enumTypes.put(enumType.getName(), enumType);
    }

    void addSuppressResourceGeneration(String schemaName) {
        suppressResourceGeneration.add(schemaName);
    }
}
