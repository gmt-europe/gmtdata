package nl.gmt.data.hibernate.generation;

import java.io.File;

public interface GeneratorWriter {
    void writeFile(File fileName, String content, boolean overwrite);
}
