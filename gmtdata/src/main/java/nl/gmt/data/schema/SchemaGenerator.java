package nl.gmt.data.schema;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SchemaGenerator extends SchemaAnnotatableElement {
    private String name;
    private String strategy;
    private final List<SchemaParameter> parameters = new ArrayList<>();
    private final List<SchemaParameter> unmodifiableParameters = Collections.unmodifiableList(parameters);

    SchemaGenerator(SchemaParserLocation location) {
        super(location);
    }

    public String getName() {
        return name;
    }

    void setName(String name) {
        this.name = name;
    }

    public String getStrategy() {
        return strategy;
    }

    void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public List<SchemaParameter> getParameters() {
        return unmodifiableParameters;
    }

    void addParameter(SchemaParameter parameter) {
        parameters.add(parameter);
    }
}
