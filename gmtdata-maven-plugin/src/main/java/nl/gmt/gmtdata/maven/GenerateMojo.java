package nl.gmt.gmtdata.maven;

import nl.gmt.data.hibernate.generation.CodeGenerator;
import nl.gmt.data.hibernate.generation.GeneratorWriter;
import nl.gmt.data.migrate.SqlStatement;
import nl.gmt.data.schema.Schema;
import nl.gmt.data.schema.SchemaCallback;
import nl.gmt.data.schema.SchemaException;
import nl.gmt.data.schema.SchemaParserExecutor;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.Validate;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.*;

@SuppressWarnings("UnusedDeclaration")
@Mojo(name = "hibernate-model", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class GenerateMojo extends AbstractMojo {
    @Parameter(property = "schema", required = true)
    private String schema;

    @Parameter(property = "searchPaths", required = true)
    private File[] searchPaths;

    @Parameter(defaultValue = "${project.build.directory}/generated-sources/hibernate-model", property = "outputDir", required = true)
    private File outputDirectory;

    @Parameter(property = "generateRepositories", required = false, defaultValue = "false")
    private boolean generateRepositories;

    @Parameter(defaultValue = "${project.build.sourceDirectory}", property = "repositoriesOutputDir", required = false)
    private File repositoriesOutputDirectory;

    @Parameter(property = "packageName", required = false)
    private String packageName;

    @Parameter(property = "generateSchema", required = false, defaultValue = "true")
    private boolean generateSchema;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void execute() throws MojoExecutionException {
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

        try {
            // Parse the schema.

            Schema schema = parserExecutor.parse(this.schema, GenericSchemaRules.INSTANCE);

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
        } catch (SchemaException e) {
            throw new MojoExecutionException("Cannot generate code", e);
        }
    }
}
