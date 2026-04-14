package smhy.util;

import java.nio.file.Path;
import java.util.Collection;

public interface FileStore<T> {
    void save(Path outputPath, Collection<T> collection);
    Collection<T> load(Path inputPath);
}

