package nl.gmt.data.schema;

import java.util.Collections;
import java.util.List;

public class SchemaParserResult {
    private final List<String> includes;

    SchemaParserResult(List<String> includes) {
        this.includes = Collections.unmodifiableList(includes);
    }

    public List<String> getIncludes() {
        return includes;
    }
}
