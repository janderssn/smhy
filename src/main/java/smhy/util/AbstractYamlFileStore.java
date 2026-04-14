package smhy.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.SequenceNode;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.ParameterizedType;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;

@Slf4j
public abstract class AbstractYamlFileStore<T> implements FileStore<T> {

    public void save(Path outputPath, Collection<T> collection) {
        if (collection.isEmpty()) {
            return;
        }
        DumperOptions options = new DumperOptions();
        Representer representer = new Representer(options);
        representer.addClassTag(collection.stream().findFirst().map(Object::getClass).orElseThrow(), Tag.MAP);
        Yaml yaml = new Yaml(representer, options);
        try (FileWriter fileWriter = new FileWriter(outputPath.toFile())) {
            yaml.dump(collection, fileWriter);
        } catch (IOException e) {
            log.warn("unable to write conditions to yaml file: {}", outputPath, e);
        }
    }

    public Collection<T> load(Resource resource) {
        Constructor constructor = new ListConstructor<>(getClazz());
        Yaml yaml = new Yaml(constructor);
        try (InputStreamReader reader = new InputStreamReader(resource.getInputStream())) {
            return yaml.load(reader);
        } catch (IOException e) {
            log.warn("unable to read conditions from yaml file: {}", resource, e);
        }
        return Collections.emptyList();
    }

    public Collection<T> load(Path inputPath) {
        Constructor constructor = new ListConstructor<>(getClazz());
        Yaml yaml = new Yaml(constructor);
        try (FileReader fileReader = new FileReader(inputPath.toFile())) {
            return yaml.load(fileReader);
        } catch (IOException e) {
            log.warn("unable to read conditions from yaml file: {}", inputPath, e);
        }
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    private Class<T> getClazz() {
        return (Class<T>) ((ParameterizedType) getClass()
                .getGenericSuperclass()).getActualTypeArguments()[0];
    }

    private static class ListConstructor<T> extends Constructor {
        private final Class<T> clazz;

        public ListConstructor(final Class<T> clazz) {
            super(new LoaderOptions());
            this.clazz = clazz;
        }

        @Override
        protected Object constructObject(final Node node) {
            if (node instanceof SequenceNode && isRootNode(node)) {
                ((SequenceNode) node).setListType(clazz);
            }
            return super.constructObject(node);
        }

        private boolean isRootNode(final Node node) {
            return node.getStartMark().getIndex() == 0;
        }
    }
}
