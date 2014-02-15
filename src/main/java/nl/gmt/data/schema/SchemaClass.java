package nl.gmt.data.schema;

import java.util.*;

public class SchemaClass extends SchemaAnnotatableElement {
    private Map<String, SchemaProperty> properties = new HashMap<>();
    private Map<String, SchemaProperty> unmodifiableProperties = Collections.unmodifiableMap(properties);
    private Map<String, SchemaForeignBase> foreigns = new HashMap<>();
    private Map<String, SchemaForeignBase> unmodifiableForeigns = Collections.unmodifiableMap(foreigns);
    private List<SchemaIndex> indexes = new ArrayList<>();
    private List<SchemaIndex> unmodifiableIndexes = Collections.unmodifiableList(indexes);
    private String name;
    private String boundedContext;
    private String dbName;
    private String persister;
    private SchemaClassIdProperty idProperty;
    private SchemaClassIdProperty resolvedIdProperty;
    private String resolvedDbName;

    SchemaClass(SchemaParserLocation location) {
        super(location);
    }

    public String getFullName() {
        if (boundedContext == null)
            return name;

        return boundedContext + "." + name;
    }

    public String getFullViewName() {
        return getFullName() + "View";
    }

    public Map<String, SchemaProperty> getProperties() {
        return unmodifiableProperties;
    }

    public Map<String, SchemaForeignBase> getForeigns() {
        return unmodifiableForeigns;
    }

    public List<SchemaIndex> getIndexes() {
        return unmodifiableIndexes;
    }

    public String getName() {
        return name;
    }

    void setName(String name) {
        this.name = name;
    }

    public String getBoundedContext() {
        return boundedContext;
    }

    void setBoundedContext(String boundedContext) {
        this.boundedContext = boundedContext;
    }

    public String getDbName() {
        return dbName;
    }

    void setDbName(String dbName) {
        this.dbName = dbName;
    }

    public String getPersister() {
        return persister;
    }

    void setPersister(String persister) {
        this.persister = persister;
    }

    public SchemaClassIdProperty getIdProperty() {
        return idProperty;
    }

    void setIdProperty(SchemaClassIdProperty idProperty) {
        this.idProperty = idProperty;
    }

    public SchemaClassIdProperty getResolvedIdProperty() {
        return resolvedIdProperty;
    }

    void setResolvedIdProperty(SchemaClassIdProperty resolvedIdProperty) {
        this.resolvedIdProperty = resolvedIdProperty;
    }

    public String getResolvedDbName() {
        return resolvedDbName;
    }

    void setResolvedDbName(String resolvedDbName) {
        this.resolvedDbName = resolvedDbName;
    }

    void addProperty(SchemaProperty property) {
        properties.put(property.getName(), property);
    }

    void addForeign(SchemaForeignBase foreign) {
        foreigns.put(foreign.getName(), foreign);
    }

    void addIndex(SchemaIndex index) {
        indexes.add(index);
    }

    @Override
    public String toString() {
        return name;
    }
}
