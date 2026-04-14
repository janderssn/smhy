package smhy;

import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Test classes annotated with {@code @IntegrationTest} only runs if the spring profile {@code itest} is active.
 * The test class will additionally be marked with {@code SpringBootTest}.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest
@EnabledIfSystemProperty(named = "spring.profiles.active", matches = "itest")
public @interface IntegrationTest {
}
