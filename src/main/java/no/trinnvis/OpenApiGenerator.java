package no.trinnvis;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.common.base.CaseFormat;
import com.google.common.base.Strings;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import io.swagger.oas.models.OpenAPI;
import io.swagger.oas.models.Operation;
import io.swagger.oas.models.PathItem;
import io.swagger.oas.models.media.ArraySchema;
import io.swagger.oas.models.media.DateSchema;
import io.swagger.oas.models.media.DateTimeSchema;
import io.swagger.oas.models.media.ObjectSchema;
import io.swagger.oas.models.media.Schema;
import io.swagger.oas.models.media.StringSchema;
import io.swagger.oas.models.media.UUIDSchema;
import io.swagger.parser.OpenAPIParser;
import io.swagger.parser.models.SwaggerParseResult;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Generated;
import javax.lang.model.element.Modifier;

public class OpenApiGenerator {

    private final File output;
    private final File input;
    private final String destinationPackage = "no.trinnvis.dabih.api";
    private Map<String, ClassName> types = new HashMap<>();
    private Map<String, StringSchema> enums = new HashMap<>();
    private OpenAPI openAPI;

    OpenApiGenerator(final File output, final File input) {
        this.output = output;
        this.input = input;
        types.put("uuid", ClassName.get(UUID.class));
        types.put("string", ClassName.get(String.class));
        types.put("date", ClassName.get(LocalDate.class));
        types.put("local-date-time", ClassName.get(LocalDateTime.class));
        types.put("date-time", ClassName.get(ZonedDateTime.class));
        types.put("integer", ClassName.get(Integer.class));
        types.put("boolean", ClassName.get(Boolean.class));
    }

    private static <T, E> Optional<T> getKeyByValue(Map<T, E> map, E value) {
        return map.entrySet()
            .stream()
            .filter(entry -> Objects.equals(entry.getValue(), value))
            .map(Map.Entry::getKey)
            .findFirst();
    }

    void execute() {

        SwaggerParseResult result = new OpenAPIParser().readLocation(input.toURI().toString(), null, null);

        openAPI = result.getOpenAPI();
        handleTypes(openAPI.getComponents().getSchemas());

        writeEnums();

        writeResources();

    }

    private void writeResources() {


        Tree<String> forest = new Tree<>("forest");
        Tree<String> current = forest;

        for (String tree : openAPI.getPaths().keySet()) {
            Tree<String> root = current;

            for (String data : tree.split("/")) {
                current = current.child(data);
            }

            current = root;
        }

        forest.accept(new PrintIndentedVisitor(0));




        Set<String> prefixes = new HashSet<>();

        openAPI.getPaths().entrySet()
            .stream()
            .sorted(Comparator.comparing(e -> e.getKey()))
            .forEach((e) -> {

                String[] r = e.getKey().split("/");
                prefixes.add(r[1]);

                //    System.out.println(e.getKey() + " " );
            });

        prefixes.forEach(e -> {
            System.out.println(e);
            try {
                writeResourceApi(e);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        });

        //openAPI.getTags().forEach(tag -> {
        //    System.out.println(tag);
        //});
    }

    private void writeResourceApi(final String e) throws IOException {

        TypeSpec.Builder builder = TypeSpec.classBuilder(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, e) + "Api")
            .addJavadoc("The $L class.", CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, e).replaceAll("_", " "))
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addAnnotation(AnnotationSpec.builder(javax.ws.rs.Path.class).addMember("value", "\"/$L\"", e).build());



        writeResourceApiOperations(builder, e);

        JavaFile javaFile = JavaFile.builder(destinationPackage + ".resources", builder.build())
            .skipJavaLangImports(true)
            .build();

        javaFile.writeTo(Paths.get(output.getAbsolutePath(), "src/main/java"));

    }

    private void writeResourceApiOperations(final TypeSpec.Builder builder, final String e) {
        writeDirectOperations(builder, e, CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, ""), "");
        writeIndirectOperations(builder, e);
        writeIndirectOperationsWithPathParameter(builder, e);
    }

    private void writeIndirectOperations(final TypeSpec.Builder builder, final String e) {
        openAPI.getPaths().entrySet()
            .stream()
            .filter(item -> {
                String[] x = item.getKey().split(("/"));

                return x.length == 3 && x[1].equals(e) && !x[2].contains("{");
            }).forEach(item -> {
            String[] x = item.getKey().split(("/"));

            writeDirectOperations(builder, item.getKey().substring(1), CaseFormat.LOWER_HYPHEN.to(CaseFormat.UPPER_CAMEL, x[2]), x[2]);
        });
    }


    private void writeIndirectOperationsWithPathParameter(final TypeSpec.Builder builder, final String e) {
        openAPI.getPaths().entrySet()
            .stream()
            .filter(item -> {
                String[] x = item.getKey().split(("/"));

                return x.length == 3 && x[1].equals(e) && x[2].contains("{");
            }).forEach(item -> {
            String[] x = item.getKey().split(("/"));


            String name = x[2].substring(1, x[2].length()-1);

            name = name.replace("+","");

            final String method = "get" + CaseFormat.LOWER_HYPHEN.to(CaseFormat.UPPER_CAMEL, name);
            MethodSpec.Builder builder1 = MethodSpec.methodBuilder(method)
                .addModifiers(Modifier.PUBLIC);

            final String path = "/" + x[2];
            if (!Strings.isNullOrEmpty(path)) {
                builder1.addAnnotation(AnnotationSpec.builder(javax.ws.rs.Path.class).addMember("value", "\"$L\"", path).build());
            }


            MethodSpec spec = builder1
                .returns(String.class)
                .addStatement("return \"subresource\"")
                .build();

            builder.addMethod(spec);
        });
    }


    private void writeDirectOperations(final TypeSpec.Builder builder, final String e, final String Suffix, final String path) {
        final PathItem x = openAPI.getPaths().get("/" + e);
        if (x != null) {
            writeOperation(builder, x.getGet(), "get" + Suffix, javax.ws.rs.GET.class, path);
            writeOperation(builder, x.getPost(), "post" + Suffix, javax.ws.rs.POST.class, path);
            writeOperation(builder, x.getPut(), "put" + Suffix, javax.ws.rs.PUT.class, path);
            writeOperation(builder, x.getDelete(), "delete" + Suffix, javax.ws.rs.DELETE.class, path);
        }
    }

    private void writeOperation(final TypeSpec.Builder builder, final Operation operation, final String method, Class<?> methodClass, final String path) {

        if (operation != null) {

            MethodSpec.Builder builder1 = MethodSpec.methodBuilder(method)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(methodClass);


            if (!Strings.isNullOrEmpty(path)) {
                builder1.addAnnotation(AnnotationSpec.builder(javax.ws.rs.Path.class).addMember("value", "\"/$L\"", path).build());
            }


            MethodSpec spec = builder1
                .returns(String.class)
                .addStatement("return \"x\"")
                .build();

            builder.addMethod(spec);

        }

    }

    private void writeEnums() {
        enums.forEach((k, v) -> {
            try {
                writeEnum(k, v);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void writeEnum(final String name, final StringSchema declaration) throws IOException {

        TypeSpec.Builder builder = TypeSpec.enumBuilder(name)
            .addJavadoc("The $L.", CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, name).replaceAll("_", " "))
            .addModifiers(Modifier.PUBLIC);

        if (declaration.getDescription() != null) {

            builder.addJavadoc(declaration.getDescription());
        }

        declaration.getEnum().forEach(builder::addEnumConstant);

        CodeBlock block = CodeBlock.builder()
            .beginControlFlow("if (string == null || string.isEmpty())")
            .addStatement("return null")
            .nextControlFlow("else")
            .addStatement("return " + name + ".valueOf(string)")
            .endControlFlow()
            .build();

        MethodSpec ofMethod = MethodSpec.methodBuilder("of")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addParameter(String.class, "string")
            //.addJavadoc("Creates a new $L.\n\n", t.name() + "Builder")
            //.addJavadoc("@returns the new $L", t.name() + "Builder")
            .returns(ClassName.get(destinationPackage, name))
            .addCode(block)
            .build();

        MethodSpec toStringMethod = MethodSpec.methodBuilder("toString")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override.class)
            .returns(String.class)
            .addStatement("return this.name()")
            .build();

        TypeSpec spec = builder
            .addMethod(ofMethod)
            .addMethod(toStringMethod)
            .addAnnotation(AnnotationSpec.builder(Generated.class).addMember("value", "$S", "Generated from OpenApi").build())
            .build();

        JavaFile javaFile = JavaFile.builder(destinationPackage, spec)
            .skipJavaLangImports(true)
            .build();

        javaFile.writeTo(Paths.get(output.getAbsolutePath(), "src/main/java"));

    }

    private void handleTypes(final Map<String, Schema> types) {
        types.forEach((key, value) -> {
            addToTypeMap(key, destinationPackage);
        });

        types.forEach((key, value) -> {

            System.out.println("" + key);
            try {
                writeModelType(key, value, destinationPackage);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void addToTypeMap(String key, String s) {
        if (!types.containsKey(key)) {
            ClassName className = ClassName.get(s, key);
            types.put(key, className);
        }
    }

    private void writeModelType(String name, final Schema value, String packageName) throws IOException {
        TypeSpec.Builder builder = TypeSpec.classBuilder(name)
            .addJavadoc("The $L class.", CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, name).replaceAll("_", " "))
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL);

        TypeSpec.Builder builderBuilder = TypeSpec.classBuilder(name + "Builder")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addJavadoc("A builder class for creating instances of {@link $L}\n", name)
            .addMethod(MethodSpec.constructorBuilder().build());

        if (value.getDescription() != null) {

            builder.addJavadoc(value.getDescription());
        }

        MethodSpec.Builder constructorBuilder = MethodSpec.constructorBuilder()
            .addModifiers(Modifier.PRIVATE);

        constructorBuilder.addParameter(ClassName.get(packageName, name, name + "Builder"), "builder");

        List<String> constructorParameterNames = new ArrayList<>();

        if (value.getType().equals("object")) {

            ObjectSchema object = (ObjectSchema) value;

            object.getProperties().forEach((k, p) -> {

                TypeName type = findClass(k, p);

                constructorParameterNames.add(k);

                addField(builder, p, type, k);
                addBuilderField(builderBuilder, p, type, k);

                addGetMethod(builder, p, type, k);

                TypeName builderType = ClassName.get(packageName, name, name + "Builder");

                addBuilderMethod(builderBuilder, p, type, builderType, name, k);

                constructorBuilder
                    .addStatement("$N = builder.$N", k, k);
            });

        }

        MethodSpec buildMethod = MethodSpec.methodBuilder("build")
            .addModifiers(Modifier.PUBLIC)
            .addJavadoc("Creates a new $L with all configuration options that have been specified by calling methods on this builder.\n", name)
            .addJavadoc("@returns the new $L", name)
            .returns(ClassName.get(packageName, name))
            .addStatement("return new " + name + "(this)")
            .build();

        builderBuilder.addMethod(buildMethod);

        final AnnotationSpec annotationSpec = AnnotationSpec.builder(JsonPOJOBuilder.class)
            .addMember("withPrefix", "$S", "")
            .build();

        builderBuilder.addAnnotation(annotationSpec);

        final AnnotationSpec annotationSpecForClass = AnnotationSpec.builder(JsonDeserialize.class)
            .addMember("builder", "$L.$L.class", name, name + "Builder")
            .build();

        MethodSpec builderMethod = MethodSpec.methodBuilder("builder")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addJavadoc("Creates a new $L.\n\n", name + "Builder")
            .addJavadoc("@returns the new $L", name + "Builder")
            .returns(ClassName.get(packageName, name, name + "Builder"))
            .addStatement("return new " + name + "Builder()")
            .build();

        TypeSpec spec = builder
            .addAnnotation(annotationSpecForClass)
            .addMethod(constructorBuilder.build())
            .addMethod(builderMethod)
            .addType(builderBuilder.build())
            .addAnnotation(AnnotationSpec.builder(Generated.class).addMember("value", "$S", "Generated from OpenApi").build())
            .build();

        JavaFile javaFile = JavaFile.builder(packageName, spec)
            .skipJavaLangImports(true)
            .build();

        javaFile.writeTo(Paths.get(output.getAbsolutePath(), "src/main/java"));
    }

    private TypeName findClass(String type, Schema p) {
        if (p instanceof StringSchema) {
            StringSchema stringTypeDeclaration = (StringSchema) p;
            if (stringTypeDeclaration.getEnum() != null) {
                System.out.println(type + " is enum " + stringTypeDeclaration.getEnum());

                String name = capitalize(type) + "Enum";

                if (!enums.containsKey(name)) {
                    System.out.println(name + " is defined");
                    enums.put(name, stringTypeDeclaration);

                }

                return ClassName.get(destinationPackage, name);

            } else if (stringTypeDeclaration.getFormat() != null) {
                if (types.containsKey(stringTypeDeclaration.getFormat())) {
                    return types.get(stringTypeDeclaration.getFormat());
                }
            }
        }

        if (p instanceof DateSchema) {
            DateSchema schema = (DateSchema) p;
            return types.get(schema.getFormat());
        }

        if (p instanceof DateTimeSchema) {
            DateTimeSchema schema = (DateTimeSchema) p;
            return types.get(schema.getFormat());
        }

        if (p instanceof UUIDSchema) {
            UUIDSchema schema = (UUIDSchema) p;
            return types.get(schema.getFormat());
        }

        if (p != dereference(p)) {
            return findClass(type, dereference(p));
        }

        Optional<String> schemaName = getKeyByValue(openAPI.getComponents().getSchemas(), p);
        if (schemaName.isPresent() && types.containsKey(schemaName.get())) {
            return types.get(schemaName.get());
        }

        if (types.containsKey(p.getType())) {
            return types.get(p.getType());
        }

        if (p instanceof ArraySchema) {
            ArraySchema declaration = (ArraySchema) p;

            Schema subSchema = dereference(declaration.getItems());

            TypeName itemType = findClass(subSchema.getTitle(), subSchema);
            ClassName set = ClassName.get("java.util", "Set");
            return ParameterizedTypeName.get(set, itemType);
        }

        return ClassName.get(Object.class);
//        throw new IllegalArgumentException(type);
    }

    private Schema dereference(final Schema schema) {

        if (schema.get$ref() != null) {
            String[] n = schema.get$ref().split("/");
            return openAPI.getComponents().getSchemas().get(n[n.length - 1]);
        } else {
            return schema;
        }

    }

    private String capitalize(String input) {
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }

    private void addGetMethod(TypeSpec.Builder builder, Schema p, TypeName type, String propertyName) {

        String prefix = type.equals(ClassName.get(Boolean.class)) ? "is" : "get";
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(prefix + capitalize(propertyName))
            .returns(type)
            .addModifiers(Modifier.PUBLIC)
            .addStatement("return this." + propertyName);
        if (p.getDescription() != null) {
            methodBuilder.addJavadoc(p.getDescription());
        }
        MethodSpec get = methodBuilder
            .build();

        builder.addMethod(get);
    }

    private void addBuilderMethod(TypeSpec.Builder builder, Schema p, TypeName type, TypeName builderType, String name, String propertyName) {

        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(propertyName)
            .returns(builderType)
            .addModifiers(Modifier.PUBLIC)
            .addJavadoc("Sets the $L property for the new $L.\n", propertyName, name)
            .addJavadoc("@param $L the $L\n", propertyName, propertyName)
            .addJavadoc("@returns a reference to this $T", builderType)
            .addParameter(type, propertyName, Modifier.FINAL)
            .addStatement("this." + propertyName + "=" + propertyName)
            .addStatement("return this");

        if (p.getDescription() != null) {
            methodBuilder.addJavadoc(p.getDescription());
        }

        MethodSpec get = methodBuilder
            .build();

        builder.addMethod(get);
    }

    private void addField(TypeSpec.Builder builder, Schema p, TypeName type, final String propertyName) {
        FieldSpec.Builder fieldBuilder = FieldSpec.builder(type, propertyName)
            .addModifiers(Modifier.PRIVATE, Modifier.FINAL);

        FieldSpec field = fieldBuilder
            .build();

        builder.addField(field);
    }

    private void addBuilderField(TypeSpec.Builder builder, Schema p, TypeName type, String propertyName) {
        FieldSpec.Builder fieldBuilder = FieldSpec.builder(type, propertyName)
            .addModifiers(Modifier.PRIVATE);

        if (p instanceof ArraySchema) {
            fieldBuilder
                .initializer("new $T<>()", HashSet.class);
        } else if (p instanceof StringSchema) {
            StringSchema stringTypeDeclaration = (StringSchema) p;
            if (stringTypeDeclaration.getEnum() != null) {

                if (stringTypeDeclaration.getDefault() != null) {
                    fieldBuilder
                        .initializer("$T.$L", type, stringTypeDeclaration.getDefault());
                }
            }
        }

        FieldSpec field = fieldBuilder
            .build();

        builder.addField(field);
    }
/*
    private void printType(TypeDeclaration declaration) {
        if (declaration instanceof ObjectTypeDeclaration) {

            ObjectTypeDeclaration object = (ObjectTypeDeclaration) declaration;

            object.properties().forEach(t -> {
                System.out.println("  " + t.displayName().value());
            });

        }
    }
    */
}
