package no.trinnvis;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Generated;


/**
 * The report update message class. */
@JsonDeserialize(
    builder = ItemWithAdditionalProperties.ItemWithAdditionaPropertiesBuilder.class
)
@Generated("Generated from OpenApi")
public final class ItemWithAdditionalProperties {
    private final String name;

    private final String content;

    private final UUID employee;

    private final Set<Integer> pages;


    private final Set<String> accessControl;

    private Map<String, Object> additionalProperties;


    private ItemWithAdditionalProperties(ItemWithAdditionaPropertiesBuilder builder) {
        name = builder.name;
        content = builder.content;
        employee = builder.employee;
        pages = builder.pages;
        accessControl = builder.accessControl;
        additionalProperties = builder.additionalProperties; // TODO make immutable
    }

    public String getName() {
        return this.name;
    }

    public String getContent() {
        return this.content;
    }

    public UUID getEmployee() {
        return this.employee;
    }

    public Set<Integer> getPages() {
        return this.pages;
    }

    public Set<String> getAccessControl() {
        return this.accessControl;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return additionalProperties;
    }


    /**
     * Creates a new ItemWithAdditionaPropertiesBuilder.
     *
     * @returns the new ItemWithAdditionaPropertiesBuilder */
    public static ItemWithAdditionaPropertiesBuilder builder() {
        return new ItemWithAdditionaPropertiesBuilder();
    }

    /**
     * A builder class for creating instances of {@link ItemWithAdditionalProperties}
     */
    @JsonPOJOBuilder(
        withPrefix = ""
    )
    public static class ItemWithAdditionaPropertiesBuilder {
        private String name;

        private String content;

        private UUID employee;

        private Set<Integer> pages = new HashSet<>();

        private Set<String> accessControl = new HashSet<>();

        public ItemWithAdditionaPropertiesBuilder additionalProperties(final Map<String, Object> additionalProperties) {
            this.additionalProperties = additionalProperties;
            return this;
        }

        private Map<String, Object> additionalProperties = new HashMap<>();

        ItemWithAdditionaPropertiesBuilder() {
        }

        /**
         * Sets the name property for the new ItemWithAdditionaProperties.
         * @param name the name
         * @returns a reference to this ItemWithAdditionaPropertiesBuilder */
        public ItemWithAdditionaPropertiesBuilder name(final String name) {
            this.name=name;
            return this;
        }

        /**
         * Sets the content property for the new ItemWithAdditionaProperties.
         * @param content the content
         * @returns a reference to this ItemWithAdditionaPropertiesBuilder */
        public ItemWithAdditionaPropertiesBuilder content(final String content) {
            this.content=content;
            return this;
        }

        /**
         * Sets the employee property for the new ItemWithAdditionaProperties.
         * @param employee the employee
         * @returns a reference to this ItemWithAdditionaPropertiesBuilder */
        public ItemWithAdditionaPropertiesBuilder employee(final UUID employee) {
            this.employee=employee;
            return this;
        }

        /**
         * Sets the pages property for the new ItemWithAdditionaProperties.
         * @param pages the pages
         * @returns a reference to this ItemWithAdditionaPropertiesBuilder */
        public ItemWithAdditionaPropertiesBuilder pages(final Set<Integer> pages) {
            this.pages=pages;
            return this;
        }

        /**
         * Sets the accessControl property for the new ItemWithAdditionaProperties.
         * @param accessControl the accessControl
         * @returns a reference to this ItemWithAdditionaPropertiesBuilder */
        public ItemWithAdditionaPropertiesBuilder accessControl(final Set<String> accessControl) {
            this.accessControl=accessControl;
            return this;
        }

        @JsonAnySetter
        public ItemWithAdditionaPropertiesBuilder addSingleProperty(String key, Object value)
            throws IllegalArgumentException {

            /*
            MetricProperty metricProperty = MetricProperty.get(key);

            if (!metricProperty.validateValue(value)) {
                throw new IllegalArgumentException(value
                    + " was not of the type required by " + key + ". "
                    + metricProperty.getExpectedClass() + " expected.");
            }

            this.metricProperties.put(metricProperty, metricProperty
                .getExpectedClass().cast(value));
            */

            additionalProperties.put(key, value);
            return this;
        }

/*
        public Builder addProperties(
            Map<MetricProperty, Object> metricsProperties) {
            if (metricsProperties != null) {
                this.metricProperties.putAll(metricsProperties);
            }
            return this;
        }
*/

        /**
         * Creates a new ItemWithAdditionaProperties with all configuration options that have been specified by calling methods on this builder.
         * @returns the new ItemWithAdditionaProperties */
        public ItemWithAdditionalProperties build() {
            return new ItemWithAdditionalProperties(this);
        }
    }
}
