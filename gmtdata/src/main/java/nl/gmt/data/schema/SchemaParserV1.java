package nl.gmt.data.schema;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.*;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.InputStream;
import java.util.*;

public class SchemaParserV1 {
    private Schema schema;
    private String schemaName;

    public static final String NS = "http://schemas.gmt.nl/gmtdata/2014/02/hibernate-schema";

    public SchemaParserResult parse(InputStream is, String schemaName, Schema schema) throws SchemaException {
        this.schema = schema;
        this.schemaName = schemaName;

        validate(is);

        Document document;

        try {
            is.reset();

            document = PositionalDocumentLoader.loadDocument(is);
        } catch (Throwable ex) {
            throw new SchemaException("Cannot load XML document", ex);
        }

        List<String> includes = new ArrayList<>();
        boolean generateResources = true;

        for (Node node = document.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node.getNodeType() == Node.PROCESSING_INSTRUCTION_NODE) {
                switch (node.getNodeName()) {
                    case "include": includes.add(node.getNodeValue()); break;
                    case "generate": generateResources = parseXmlBoolean(node.getNodeValue()); break;
                    default: throw new SchemaException(String.format("Invalid processing instruction '%s'", node.getNodeName()));
                }
            }
        }

        for (Element element : children(document)) {
            switch (element.getNodeName()) {
                case "schema": parseSchema(element); break;
            }
        }

        return new SchemaParserResult(includes, generateResources);
    }

    private void validate(InputStream is) throws SchemaException {
        javax.xml.validation.Schema schema;

        try {
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            try (InputStream sis = getClass().getResourceAsStream("HibernateSchema-v1.xsd")) {
                schema = schemaFactory.newSchema(new StreamSource(sis));
            }
        } catch (Throwable e) {
            throw new SchemaException("Cannot load XSD schema", e);
        }

        Validator validator = schema.newValidator();

        try {
            validator.validate(new StreamSource(is));
        } catch (Throwable e) {
            throw new SchemaException("XML file does not validate against the schema", e);
        }
    }

    private void parseSchema(Element node) throws SchemaException {
        for (Attr attribute : attributes(node)) {
            if (!parseSchemaElementAttribute(schema, attribute))
            {
                switch (attribute.getNodeName()) {
                    case "namespace":
                        schema.setNamespaceLocation(getLocation(attribute));
                        schema.setNamespace(attribute.getValue());
                        break;
                }
            }
        }

        for (Element element : children(node)) {
            switch (element.getNodeName()) {
                case "settings": parseSettings(element); break;
                case "enumTypes": parseEnumTypes(element); break;
                case "dataTypes": parseDataTypes(element); break;
                case "classes": parseClasses(element); break;
            }
        }
    }

    private void parseSettings(Element node) throws SchemaException {
        for (Element element : children(node)) {
            switch (element.getNodeName()) {
                case "idProperty":
                    parseIdProperty(element);
                    break;
                case "enumDataType":
                    parseEnumDataType(element);
                    break;
            }
        }
    }

    private void parseIdProperty(Element node) throws SchemaException {
        SchemaIdProperty idProperty = new SchemaIdProperty(getLocation(node));
        schema.setIdProperty(idProperty);

        for (Element element : children(node)) {
            parseIdPropertyElement(idProperty, element);
        }

        for (Attr attribute : attributes(node)) {
            if (!parseIdPropertyAttribute(idProperty, attribute)) {
                switch (attribute.getNodeName()) {
                    case "name":
                        idProperty.setName(attribute.getValue());
                        break;
                    case "foreignPostfix":
                        idProperty.setForeignPostfix(attribute.getValue());
                        break;
                }
            }
        }
    }

    private void parseEnumDataType(Element node) throws SchemaException {
        schema.setEnumDataTypeLocation(getLocation(node));

        for (Attr attribute : attributes(node)) {
            switch (attribute.getNodeName()) {
                case "type": schema.setEnumDataType(attribute.getValue()); break;
            }
        }
    }

    private void parseEnumTypes(Element node) throws SchemaException {
        for (Element element : children(node)) {
            SchemaEnumType enumType = parseEnumType(element);

            if (schema.getEnumTypes().containsKey(enumType.getName()))
                throw new SchemaException(String.format("Duplicate enum type '%s'", enumType.getName()), enumType.getLocation());

            schema.addEnumType(enumType);
        }
    }

    private SchemaEnumType parseEnumType(Element node) throws SchemaException {
        SchemaEnumType result = new SchemaEnumType(getLocation(node));

        for (Attr attribute : attributes(node)) {
            if (!parseSchemaElementAttribute(result, attribute)) {
                switch (attribute.getNodeName()) {
                    case "name": result.setName(attribute.getValue()); break;
                }
            }
        }

        for (Element element : children(node)) {
            switch (element.getNodeName()) {
                case "field":
                    SchemaEnumTypeField field = parseEnumTypeField(element);
                    if (result.getFields().containsKey(field.getName()))
                        throw new SchemaException(String.format("Duplicate enum type field '%s' of enum type '%s' found", field.getName(), result.getName()), result.getLocation());

                    result.addField(field);
                    break;
            }
        }

        return result;
    }

    private SchemaEnumTypeField parseEnumTypeField(Element node) throws SchemaException {
        SchemaEnumTypeField result = new SchemaEnumTypeField(getLocation(node));

        for (Attr attribute : attributes(node)) {
            if (!parseSchemaElementAttribute(result, attribute)) {
                switch (attribute.getNodeName()) {
                    case "name":
                        result.setName(attribute.getValue());
                        break;
                    case "value":
                        result.setValue(parseXmlInteger(attribute.getValue()));
                        break;
                }
            }
        }

        return result;
    }

    private void parseDataTypes(Element node) throws SchemaException {
        for (Element element : children(node)) {
            switch (element.getNodeName()) {
                case "dataType":
                    SchemaDataType dataType = parseDataType(element);
                    if (schema.getDataTypes().containsKey(dataType.getName()))
                        throw new SchemaException(String.format("Duplicate data type '%s' found", dataType.getName()), dataType.getLocation());

                    schema.addDataType(dataType);
                    break;
            }
        }
    }

    private SchemaDataType parseDataType(Element node) throws SchemaException {
        SchemaDataType result = new SchemaDataType(getLocation(node));

        for (Attr attribute : attributes(node)) {
            parseDataTypeElementAttribute(result, attribute);
        }

        return result;
    }

    private void parseClasses(Element node) throws SchemaException {
        for (Element element : children(node)) {
            SchemaClass klass = parseClass(element);
            if (schema.getClasses().containsKey(klass.getName()))
                throw new SchemaException(String.format("Duplicate class found '%s'", klass.getName()), klass.getLocation());

            schema.addClass(klass);
        }
    }

    private SchemaClass parseClass(Element node) throws SchemaException {
        SchemaClass result = new SchemaClass(getLocation(node));

        for (Attr attribute : attributes(node)) {
            if (!parseSchemaElementAttribute(result, attribute)) {
                switch (attribute.getNodeName()) {
                    case "name": result.setName(attribute.getValue()); break;
                    case "boundedContext": result.setBoundedContext(attribute.getValue()); break;
                    case "dbName": result.setDbName(attribute.getValue()); break;
                    case "persister": result.setPersister(attribute.getValue()); break;
                }
            }
        }

        for (Element element : children(node)) {
            switch (element.getNodeName()) {
                case "idProperty":
                    result.setIdProperty(parseClassIdProperty(element));
                    break;

                case "property":
                    SchemaProperty property = parseProperty(element);
                    if (result.getProperties().containsKey(property.getName()))
                        throw new SchemaException(String.format("Duplicate property '%s' found", property.getName()), property.getLocation());

                    result.addProperty(property);
                    break;

                case "foreignParent":
                    SchemaForeignParent foreignParent = parseForeignParent(element);
                    if (result.getForeigns().containsKey(foreignParent.getName()))
                        throw new SchemaException(String.format("Duplicate foreign '%s' found", foreignParent.getName()), foreignParent.getLocation());

                    result.addForeign(foreignParent);
                    break;

                case "foreignChild":
                    SchemaForeignChild foreignChild = parseForeignChild(element);
                    if (result.getForeigns().containsKey(foreignChild.getName()))
                        throw new SchemaException(String.format("Duplicate foreign '%s' found", foreignChild.getName()), foreignChild.getLocation());

                    result.addForeign(foreignChild);
                    break;

                case "index":
                    result.addIndex(parseIndex(element));
                    break;
            }
        }

        return result;
    }

    private SchemaClassIdProperty parseClassIdProperty(Element node) throws SchemaException {
        SchemaClassIdProperty result = new SchemaClassIdProperty(getLocation(node));

        for (Element element : children(node)) {
            parseIdPropertyElement(result, element);
        }

        for (Attr attribute : attributes(node)) {
            if (!parseIdPropertyAttribute(result, attribute)) {
                switch (attribute.getNodeName()) {
                    case "dbSequence":
                        result.setDbSequence(attribute.getValue());
                        break;
                    case "dbIdName":
                        result.setDbIdName(attribute.getValue());
                        break;
                    case "compositeId":
                        SchemaCompositeId compositeId = new SchemaCompositeId(result.getLocation());
                        result.setCompositeId(compositeId);

                        for (String name : splitCommaField(attribute.getValue())) {
                            SchemaCompositeIdProperty property = new SchemaCompositeIdProperty(result.getLocation());

                            property.setName(name);

                            compositeId.addProperty(property);
                        }
                        break;
                }
            }
        }

        return result;
    }

    private SchemaProperty parseProperty(Element node) throws SchemaException {
        SchemaProperty result = new SchemaProperty(getLocation(node));

        for (Attr attribute : attributes(node)) {
            parseDataTypeElementAttribute(result, attribute);
        }

        return result;
    }

    private SchemaForeignParent parseForeignParent(Element node) throws SchemaException {
        SchemaForeignParent result = new SchemaForeignParent(getLocation(node));

        for (Attr attribute : attributes(node)) {
            if (!parseSchemaElementAttribute(result, attribute)) {
                switch (attribute.getNodeName()) {
                    case "name": result.setName(attribute.getValue()); break;
                    case "class": result.setClassName(attribute.getValue()); break;
                    case "indexed": result.setIndexType(parseIndexed(attribute.getValue())); break;
                    case "nullable": result.setAllowNull(parseNullable(attribute.getValue())); break;
                    case "dbName": result.setDbName(attribute.getValue()); break;
                }
            }
        }

        return result;
    }

    private SchemaForeignChild parseForeignChild(Element node) throws SchemaException {
        SchemaForeignChild result = new SchemaForeignChild(getLocation(node));

        for (Attr attribute : attributes(node)) {
            if (!parseSchemaElementAttribute(result, attribute)) {
                switch (attribute.getNodeName()) {
                    case "name": result.setName(attribute.getValue()); break;
                    case "class": result.setClassName(attribute.getValue()); break;
                    case "classProperty": result.setClassProperty(attribute.getValue()); break;
                }
            }
        }

        return result;
    }

    private SchemaIndex parseIndex(Element node) throws SchemaException {
        SchemaIndex result = new SchemaIndex(getLocation(node));

        result.setType(SchemaIndexType.INDEX);

        for (Attr attribute : attributes(node)) {
            if (!parseSchemaElementAttribute(result, attribute)) {
                switch (attribute.getNodeName()) {
                    case "unique": result.setType(parseIndexUnique(attribute.getValue())); break;
                    case "properties": result.setFields(Collections.unmodifiableList(Arrays.asList(splitCommaField(attribute.getValue())))); break;
                }
            }
        }

        return result;
    }

    private SchemaIndexType parseIndexUnique(String value) throws SchemaException {
        return parseXmlBoolean(value) ? SchemaIndexType.UNIQUE : SchemaIndexType.INDEX;
    }

    private boolean parseSchemaElementAttribute(SchemaAnnotatableElement element, Attr attribute) {
        switch (attribute.getNodeName()) {
            case "comments":
                element.setComments(attribute.getValue());
                return true;

            case "tags":
                element.setTags(parseTags(attribute.getValue()));
                return true;

            default:
                return false;
        }
    }

    private List<String> parseTags(String tags) {
        List<String> parsedTags = new ArrayList<>();

        if (!StringUtils.isEmpty(tags)) {
            for (String tag : tags.split(",")) {
                tag = tag.trim();

                if (tag.length() > 0) {
                    // We expect there to be a very low number of different tags.
                    // Interning them allows the lookups to be faster.

                    parsedTags.add(tag.intern());
                }
            }
        }

        return Collections.unmodifiableList(parsedTags);
    }

    private SchemaParserLocation getLocation(Node node) {
        Integer line = (Integer)node.getUserData(PositionalDocumentLoader.LINE_NUMBER_KEY_NAME);
        Integer column = (Integer)node.getUserData(PositionalDocumentLoader.COLUMN_NUMBER_KEY_NAME);

        if (line == null || column == null)
            return null;

        return new SchemaParserLocation(schemaName, line, column);
    }

    private boolean parseIdPropertyAttribute(SchemaIdPropertyBase element, Attr attribute) throws SchemaException {
        if (parseSchemaElementAttribute(element, attribute))
            return true;

        switch (attribute.getNodeName()) {
            case "type": element.setType(attribute.getValue()); return true;
            case "autoIncrement": element.setAutoIncrement(parseXmlBoolean(attribute.getValue()) ? SchemaIdAutoIncrement.YES : SchemaIdAutoIncrement.NO); return true;
            default: return false;
        }
    }

    private boolean parseIdPropertyElement(SchemaIdPropertyBase element, Element node) throws SchemaException {
        switch (node.getNodeName()) {
            case "generator": element.setGenerator(parseGenerator(node)); return true;
            default: return false;
        }
    }

    private SchemaGenerator parseGenerator(Element node) throws SchemaException {
        SchemaGenerator result = new SchemaGenerator(getLocation(node));

        for (Element element : children(node)) {
            switch (element.getNodeName()) {
                case "parameter": result.addParameter(parseParameter(element)); break;
            }
        }

        for (Attr attribute : attributes(node)) {
            switch (attribute.getNodeName()) {
                case "name": result.setName(attribute.getValue()); break;
                case "strategy": result.setStrategy(attribute.getValue()); break;
            }
        }

        return result;
    }

    private SchemaParameter parseParameter(Element node) throws SchemaException {
        SchemaParameter result = new SchemaParameter(getLocation(node));

        for (Attr attribute : attributes(node)) {
            switch (attribute.getNodeName()) {
                case "name": result.setName(attribute.getValue()); break;
                case "value": result.setValue(attribute.getValue()); break;
            }
        }

        return result;
    }

    private boolean parseDataTypeElementAttribute(SchemaDataTypeBase element, Attr attribute) throws SchemaException {
        if (parseSchemaElementAttribute(element, attribute))
            return true;

        switch (attribute.getNodeName()) {
            case "name": element.setName(attribute.getValue()); return true;
            case "type": element.setRawType(attribute.getValue()); return true;
            case "dbType": element.setRawDbType(attribute.getValue()); return true;
            case "nativeType": element.setNativeType(parseNativeType(attribute.getValue())); return true;
            case "enumType": element.setEnumType(attribute.getValue()); return true;
            case "signed": element.setSigned(parseSigned(attribute.getValue())); return true;
            case "length": element.setLength(parseXmlInteger(attribute.getValue())); return true;
            case "positions": element.setPositions(parseXmlInteger(attribute.getValue())); return true;
            case "nullable": element.setAllowNull(parseNullable(attribute.getValue())); return true;
            case "indexed": element.setIndexType(parseIndexed(attribute.getValue())); return true;
            case "lazy": element.setLazy(parseLazy(attribute.getValue())); return true;
            case "dbName": element.setDbName(attribute.getValue()); return true;
            case "userType": element.setUserType(attribute.getValue()); return true;
            default: return false;
        }
    }

    private SchemaSigned parseSigned(String value) throws SchemaException {
        return parseXmlBoolean(value) ? SchemaSigned.SIGNED : SchemaSigned.UNSIGNED;
    }

    private SchemaAllowNull parseNullable(String value) throws SchemaException {
        return parseXmlBoolean(value) ? SchemaAllowNull.ALLOW : SchemaAllowNull.DISALLOW;
    }

    private SchemaIndexType parseIndexed(String value) throws SchemaException {
        switch (value) {
            case "true": return SchemaIndexType.INDEX;
            case "false": return SchemaIndexType.UNSET;
            case "unique": return SchemaIndexType.UNIQUE;
            default: throw new SchemaException(String.format("Invalid indexed '%s'", value));
        }
    }

    private SchemaLazy parseLazy(String value) throws SchemaException {
        return parseXmlBoolean(value) ? SchemaLazy.LAZY : SchemaLazy.NOT_LAZY;
    }

    private Class<?> parseNativeType(String value) throws SchemaException {
        try {
            return Class.forName(value);
        } catch (ClassNotFoundException e) {
            throw new SchemaException(String.format("Invalid native type '%s'", value), e);
        }
    }

    private String[] splitCommaField(String value) {
        if (StringUtils.isEmpty(value))
            return new String[0];

        String[] result = value.split(",");

        for (int i = 0; i < result.length; i++) {
            result[i] = result[i].trim();
        }

        return result;
    }

    private boolean parseXmlBoolean(String value) throws SchemaException {
        switch (value) {
            case "0":
            case "false":
                return false;

            case "1":
            case "true":
                return true;

            default:
                throw new SchemaException(String.format("Invalid boolean value '%s'", value));
        }
    }

    private int parseXmlInteger(String value) {
        return Integer.parseInt(value, 10);
    }

    private Iterable<Element> children(Node node) {
        List<Element> result = new ArrayList<>();

        for (Node current = node.getFirstChild(); current != null; current = current.getNextSibling()) {
            if (current.getNodeType() == Node.ELEMENT_NODE)
                result.add((Element)current);
        }

        return result;
    }

    private Iterable<Attr> attributes(Node node) {
        List<Attr> result = new ArrayList<>();
        NamedNodeMap attributes = node.getAttributes();

        for (int i = 0; i < attributes.getLength(); i++) {
            result.add((Attr) attributes.item(i));
        }

        return result;
    }
}
