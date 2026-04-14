package smhy.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;

@Slf4j
@UtilityClass
public class ResourceCopyUtils {

    public static void copyDirectory(Resource resource, final Path target) throws IOException {
        if (resource instanceof ClassPathResource) {
            copyDirectory("classpath*:%s".formatted(((ClassPathResource) resource).getPath()), target);
        } else {
            FileUtils.copyDirectory(resource.getFile(), target.toFile());
        }
    }

    // https://gist.github.com/vorburger/1062678
    public static void copyDirectory(String pattern, final Path target) throws IOException {
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources(pattern);
        for (Resource resource : resources) {
            if (resource.exists() && resource.isReadable() && resource.contentLength() > 0) {
                URL url = resource.getURL();
                Path destination = target.resolve(findAssetRoot(url.getPath()));
                FileUtils.copyURLToFile(url, destination.toFile());
                log.debug("Copied {} to {}", url, destination);
            } else {
                log.warn("Unable to copy resource {}: {}", resource, resource.getDescription());
            }
        }
    }

    private static String findAssetRoot(String path) {
        return path.replaceAll(".*/assets/", "");
    }
}




