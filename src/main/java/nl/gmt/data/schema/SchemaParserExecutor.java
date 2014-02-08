package nl.gmt.data.schema;

import com.sun.xml.internal.ws.wsdl.writer.UsingAddressing;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class SchemaParserExecutor {
    private Map<String, Boolean> includes = new HashMap<>();
    private List<String> hashes = new ArrayList<>();
    private SchemaCallback callback;

    public SchemaParserExecutor(SchemaCallback callback) {
        this.callback = callback;
    }

    public Schema parse(String schemaName, SchemaRules rules) throws SchemaException {
        Schema schema = new Schema();

        try (InputStream is = getClass().getResourceAsStream("System.schema")) {
            parse(is, "resource://System.schema", schema);
        } catch (Throwable e) {
            throw new SchemaException("Cannot parse system schema", e);
        }

        includes.put(schemaName, true);

        try {
            parse(schemaName, schema);
        } catch (IOException e) {
            throw new SchemaException("Cannot parse schema", e);
        }

        List<String> baseIncludes = new ArrayList<>();

        for (Map.Entry<String, Boolean> entry : includes.entrySet()) {
            if (!StringUtils.equals(entry.getKey(), schemaName))
                baseIncludes.add(entry.getKey());
        }

        schema.setBaseIncludes(Collections.unmodifiableList(baseIncludes));

        while (true) {
            List<String> toParse = new ArrayList<>();

            for (Map.Entry<String, Boolean> entry : includes.entrySet()) {
                if (!entry.getValue())
                    toParse.add(entry.getKey());
            }

            if (toParse.size() == 0)
                break;

            for (String include : toParse) {
                includes.put(include, true);
            }

            for (String include : toParse) {
                try {
                    parse(include, schema);
                } catch (IOException e) {
                    throw new SchemaException("Cannot parse schema", e);
                }
            }
        }

        // The hash we calculate return is a combination of the hashes
        // of all file contents used while parsing. This includes
        // the System.schema definition. We order the hashes by the
        // hash themselves to ensure a stable ordering so the end
        // result doesn't change just because files were read in a
        // different order (for some reason).

        Collections.sort(hashes);

        schema.setSchemaHash(Hex.encodeHexString(DigestUtils.sha1(StringUtils.join(hashes, ":"))));

        schema.validate(rules);

        return schema;
    }

    private void parse(String schemaName, Schema schema) throws IOException, SchemaException {
        InputStream is;

        try {
            is = callback.loadFile(schemaName);
        } catch (Exception e) {
            throw new SchemaException("Cannot load schema", e);
        }

        try {
            parse(is, schemaName, schema);
        } finally {
            is.close();
        }
    }

    private void parse(InputStream is, String schemaName, Schema schema) throws IOException, SchemaException {
        byte[] bytes = IOUtils.toByteArray(is);

        hashes.add(Hex.encodeHexString(DigestUtils.sha1(bytes)));

        SchemaParserV1 parser = new SchemaParserV1();

        SchemaParserResult result;

        try (InputStream bis = new ByteArrayInputStream(bytes)) {
            result = parser.parse(bis, schemaName, schema);
        }

        for (String include : result.getIncludes()) {
            includes.put(include, false);
        }

        if (!result.isGenerateResources())
            schema.addSuppressResourceGeneration(schemaName);
    }
}
