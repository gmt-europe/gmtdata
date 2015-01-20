package nl.gmt;

import nl.gmt.data.hibernate.generation.CodeGenerator;
import nl.gmt.data.hibernate.generation.GeneratorWriter;
import nl.gmt.data.migrate.SqlStatement;
import nl.gmt.data.schema.Schema;
import nl.gmt.data.schema.SchemaCallback;
import nl.gmt.data.schema.SchemaException;
import nl.gmt.data.schema.SchemaParserExecutor;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.*;

@RunWith(JUnit4.class)
public class GenerateFixture {
    private boolean generateRepositories = true;
    private File repositoriesOutputDirectory = new File("gmtdata-test/src/main/java/nl/gmt/data");
    private File[] searchPaths = new File[]{new File("gmtdata-test/src/main/resources")};
    private File outputDirectory = new File("gmtdata-test/target/generated-sources/hibernate-model");

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    public void execute() throws SchemaException {
        for (String module : new String[]{"baseModule", "module1", "module2", "superModule"}) {
            generate("nl/gmt/data/" + module + "/Database.schema", "nl.gmt.data." + module, module.equals("superModule"));
        }
    }

    private void generate(String schemaName, String packageName, boolean generateSchema) throws SchemaException {
        // Verify the parameters.

        if (generateRepositories) {
            Validate.notNull(repositoriesOutputDirectory, "Repositories output directory is mandatory when generating repositories");
        }

        // Setup for parsing the schema.

        SchemaParserExecutor parserExecutor = new SchemaParserExecutor(new SchemaCallback() {
            @Override
            public InputStream loadFile(String schema) throws Exception {
                for (File searchPath : searchPaths) {
                    File file = new File(searchPath, schema);

                    if (file.exists()) {
                        return new FileInputStream(file);
                    }
                }

                throw new FileNotFoundException(schema);
            }

            @Override
            public void serializeSql(Iterable<SqlStatement> statements) {
            }
        });

        // Parse the schema.

        Schema schema = parserExecutor.parse(schemaName, GenericSchemaRules.INSTANCE);

        // Run the code generator.

        File repositoriesOutputDirectory = null;
        if (generateRepositories) {
            repositoriesOutputDirectory = this.repositoriesOutputDirectory;
        }

        CodeGenerator generator = new CodeGenerator(
            schema,
            outputDirectory,
            repositoriesOutputDirectory != null ? repositoriesOutputDirectory : null,
            packageName,
            generateSchema
        );

        generator.generate(new GeneratorWriter() {
            @Override
            public void writeFile(File fileName, String content, boolean overwrite) {
                if (!overwrite && fileName.exists()) {
                    return;
                }

                fileName.getParentFile().mkdirs();

                try {
                    try (OutputStream os = new FileOutputStream(fileName)) {
                        IOUtils.write(content, os);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
