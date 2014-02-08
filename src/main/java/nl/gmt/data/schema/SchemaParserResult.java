package nl.gmt.data.schema;

import java.util.Collections;
import java.util.List;

public class SchemaParserResult {
    private List<String> includes;
    private boolean generateResources;

    SchemaParserResult(List<String> includes, boolean generateResources) {
        this.includes = Collections.unmodifiableList(includes);
        this.generateResources = generateResources;
    }

    public List<String> getIncludes() {
        return includes;
    }

    public boolean isGenerateResources() {
        return generateResources;
    }
}
