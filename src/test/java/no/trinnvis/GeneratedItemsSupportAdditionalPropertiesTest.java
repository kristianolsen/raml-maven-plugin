package no.trinnvis;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Test;

public class GeneratedItemsSupportAdditionalPropertiesTest {

    @Test
    public void testDeserializeAdditionalProperties() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        ItemWithAdditionalProperties item = mapper.readValue(getResourceFileAsString("itemWithAdditionalProperties.json"), ItemWithAdditionalProperties.class);
        assertThat(item.getAdditionalProperties().get("extraString"), is("extra 1"));
    }

    /**
     * Reads given resource file as a string.
     *
     * @param resourceFileName the path to the resource file
     * @return the file's contents or null if the file could not be opened
     */
    public String getResourceFileAsString(String resourceFileName) {
        InputStream is = getClass().getClassLoader().getResourceAsStream(resourceFileName);
        if (is != null) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            return reader.lines().collect(Collectors.joining(System.lineSeparator()));
        }
        return null;
    }

    @Test
    public void testSerializeAdditionalProperties() throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        Map<String, Object> properties = new HashMap<>();
        properties.put("test1", "test 1 value");
        ItemWithAdditionalProperties item = ItemWithAdditionalProperties.builder()
            .additionalProperties(properties)
            .build();
        String json = mapper.writeValueAsString(item);
        assertThat(json, is("{\"name\":null,\"content\":null,\"employee\":null,\"pages\":[],\"accessControl\":[],\"test1\":\"test 1 value\"}"));
    }


}
