package nl.gmt.data.schema;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class SchemaAnnotatableElement extends SchemaElement {
    private static final List<String> EMPTY_TAGS = Collections.unmodifiableList(new ArrayList<String>());

    private String comments;
    private List<String> tags;

    SchemaAnnotatableElement(SchemaParserLocation location) {
        super(location);

        tags = EMPTY_TAGS;
    }

    public String getComments() {
        return comments;
    }

    void setComments(String comments) {
        this.comments = comments;
    }

    public List<String> getTags() {
        return tags;
    }

    void setTags(List<String> tags) {
        this.tags = tags;
    }
}
