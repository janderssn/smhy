package smhy.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ClassPathResource;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceCopyUtilsTest {

    @TempDir
    Path tempDir;

    // -----------------------------------------------------------------------
    // findAssetRoot (private) — tested via reflection
    // -----------------------------------------------------------------------

    private String invokeFind(String path) throws Exception {
        Method m = ResourceCopyUtils.class.getDeclaredMethod("findAssetRoot", String.class);
        m.setAccessible(true);
        return (String) m.invoke(null, path);
    }

    @Test
    void findAssetRoot_stripsEverythingUpToAssets_jarPath() throws Exception {
        assertThat(invokeFind("jar:file:/app.jar!/assets/smhy.css"))
                .isEqualTo("smhy.css");
    }

    @Test
    void findAssetRoot_stripsEverythingUpToAssets_absolutePath() throws Exception {
        assertThat(invokeFind("/home/user/assets/sub/file.png"))
                .isEqualTo("sub/file.png");
    }

    @Test
    void findAssetRoot_noAssetsSegment_returnsFullPath() throws Exception {
        // When there is no "/assets/" in the path the regex finds nothing to strip
        assertThat(invokeFind("just-a-file.css")).isEqualTo("just-a-file.css");
    }

    // -----------------------------------------------------------------------
    // copyDirectory(String pattern, Path target)
    // -----------------------------------------------------------------------

    @Test
    void copyDirectoryWithPattern_copiesAtLeastOneAssetFile() throws Exception {
        ResourceCopyUtils.copyDirectory("classpath*:assets/**", tempDir);

        try (Stream<Path> files = Files.walk(tempDir)) {
            long count = files.filter(Files::isRegularFile).count();
            assertThat(count).isGreaterThan(0);
        }
    }

    @Test
    void copyDirectoryWithPattern_copiesKnownCssFile() throws Exception {
        ResourceCopyUtils.copyDirectory("classpath*:assets/smhy.css", tempDir);

        assertThat(tempDir.resolve("smhy.css")).exists();
    }

    // -----------------------------------------------------------------------
    // copyDirectory(Resource resource, Path target) — ClassPathResource branch
    // -----------------------------------------------------------------------

    @Test
    void copyDirectoryWithClassPathResource_delegatesViaPatternAndCopiesFiles() throws Exception {
        ClassPathResource resource = new ClassPathResource("assets/smhy.css");

        ResourceCopyUtils.copyDirectory(resource, tempDir);

        // The ClassPathResource path "assets/smhy.css" is forwarded as "classpath*:assets/smhy.css"
        assertThat(tempDir.resolve("smhy.css")).exists();
    }
}
