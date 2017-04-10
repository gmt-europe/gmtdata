package nl.gmt.data.postgres2sqlserver;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.ParserProperties;

public class Arguments {
    @Option(name = "-f", aliases = {"--from"}, required = true, usage = "Source Postgres connection string")
    private String from;

    @Option(name = "-t", aliases = {"--to"}, required = true, usage = "Destination SQL Server connection string")
    private String to;

    @Option(name = "-s", aliases = {"--schema"}, required = true, usage = "Path to database schema")
    private String schema;

    public Arguments(String... args) throws CmdLineException {
        CmdLineParser parser = new CmdLineParser(this, ParserProperties.defaults().withUsageWidth(80));
        try {
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            parser.printUsage(System.err);

            throw e;
        }
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public String getSchema() {
        return schema;
    }
}
