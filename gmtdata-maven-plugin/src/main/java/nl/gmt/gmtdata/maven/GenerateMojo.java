package nl.gmt.gmtdata.maven;

import nl.gmt.data.hibernate.generation.CodeGenerator;
import nl.gmt.data.hibernate.generation.GeneratorWriter;
import nl.gmt.data.migrate.SqlStatement;
import nl.gmt.data.schema.*;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.*;

@Mojo(name = "hibernate-model", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class GenerateMojo extends AbstractMojo {
    @Parameter(property = "schema", required = true)
    private File schema;

    @Parameter(property = "dbType", required = true)
    private String dbType;

    @Parameter(defaultValue = "${project.build.directory}/generated-sources/hibernate-model", property = "outputDir", required = true)
    private File outputDirectory;

    @Parameter(property = "generateRepositories", required = false, defaultValue = "false")
    private boolean generateRepositories;

    public void execute() throws MojoExecutionException {
        // Resolve the schema rules based on the database type.

        SchemaRules rules;

        switch (dbType.toLowerCase()) {
            case "sqlite":
                rules = new nl.gmt.data.migrate.sqlite.SchemaRules();
                break;

            case "mysql":
                rules = new nl.gmt.data.migrate.mysql.SchemaRules();
                break;

            default:
                throw new MojoExecutionException("Illegal dbType");
        }

        // Setup for parsing the schema.

        File schemaPath = this.schema.getAbsoluteFile();

        final File path = schemaPath.getParentFile();

        SchemaParserExecutor parserExecutor = new SchemaParserExecutor(new SchemaCallback() {
            @Override
            public InputStream loadFile(String schema) throws Exception {
                return new FileInputStream(new File(path, schema));
            }

            @Override
            public void serializeSql(Iterable<SqlStatement> statements) {
            }
        });

        try {
            // Parse the schema.

            Schema schema = parserExecutor.parse(schemaPath.getName(), rules);

            // Run the code generator.

            CodeGenerator generator = new CodeGenerator(schema);

            final File outputDir = getOutputDir(schema);

            generator.generate(new GeneratorWriter() {
                @Override
                public void writeFile(String fileName, String content) {
                    File target = new File(outputDir, fileName);

                    target.getParentFile().mkdirs();

                    try {
                        try (OutputStream os = new FileOutputStream(target)) {
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

    private File getOutputDir(Schema schema) {
        File outputDir = outputDirectory;

        for (String part : schema.getNamespace().split("\\.")) {
            outputDir = new File(outputDir, part);
        }

        return outputDir;
    }
}
