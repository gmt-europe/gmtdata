package nl.gmt.data.postgres2sqlserver;

import nl.gmt.data.DataException;
import org.kohsuke.args4j.CmdLineException;

public class App {
    public static void main(String[] args) throws DataException {
        Arguments arguments;
        try {
            arguments = new Arguments(args);
        } catch (CmdLineException e) {
            return;
        }

        try {
            new Transferer(arguments).transfer();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
