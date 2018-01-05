package no.trinnvis;

import java.io.File;
import org.junit.Test;

public class OpenApiGeneratorTest {
    @Test
    public void testGenerator() {
        final File output = new File("test/output");
        final File input = new File("test/openapi.yaml");

        System.out.println(input.getAbsolutePath());
        OpenApiGenerator generator = new OpenApiGenerator(output, input);
        generator.execute();

    }

}