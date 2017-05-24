package no.trinnvis;

import com.google.common.base.CaseFormat;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.beans.ConstructorProperties;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Generated;
import javax.lang.model.element.Modifier;
import org.raml.v2.api.RamlModelBuilder;
import org.raml.v2.api.RamlModelResult;
import org.raml.v2.api.model.common.ValidationResult;
import org.raml.v2.api.model.v10.api.Api;
import org.raml.v2.api.model.v10.datamodel.ArrayTypeDeclaration;
import org.raml.v2.api.model.v10.datamodel.ObjectTypeDeclaration;
import org.raml.v2.api.model.v10.datamodel.TypeDeclaration;

public class RamlGenerator {

    private final File output;
    private Map<String, ClassName> types = new HashMap<>();

    RamlGenerator(final File output) {
        this.output = output;
        types.put("uuid", ClassName.get(UUID.class));
        types.put("string", ClassName.get(String.class));
        types.put("date-only", ClassName.get(LocalDate.class));
        types.put("integer", ClassName.get(Integer.class));
        types.put("boolean", ClassName.get(Boolean.class));
    }

    void execute() {

        RamlModelResult ramlModelResult = new RamlModelBuilder().buildApi("src/main/raml/api.raml");
        if (ramlModelResult.hasErrors()) {
            for (ValidationResult validationResult : ramlModelResult.getValidationResults()) {
                System.out.println(validationResult.getMessage());
            }
        } else {
            Api api = ramlModelResult.getApiV10();

            List<TypeDeclaration> types = api.types();
            handleTypes(types);

            api.uses().forEach(l -> handleTypes(l.types()));

        }
    }

    private void handleTypes(final List<TypeDeclaration> types) {
        types.forEach(t -> {
            addToTypeMap(t, "no.trinnvis.dabih.api");
        });

        types.forEach(t -> {
            System.out.println("" + t.displayName().value());

            try {
                writeModelType(t, "no.trinnvis.dabih.api");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void addToTypeMap(TypeDeclaration t, String s) {
        if (!types.containsKey(t.name())) {
            ClassName className = ClassName.get(s, t.name());
            types.put(t.name(), className);
        }
    }

    private void writeModelType(TypeDeclaration t, String packageName) throws IOException {
        TypeSpec.Builder builder = TypeSpec.classBuilder(t.name())
            .addJavadoc("The $L class.", CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, t.name()).replaceAll("_", " "))
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL);

        TypeSpec.Builder builderBuilder = TypeSpec.classBuilder(t.name() + "Builder")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addJavadoc("A builder class for creating instances of {@link $L}\n", t.name())
            .addMethod(MethodSpec.constructorBuilder().build());

        if (t.description() != null) {

            builder.addJavadoc(t.description().value());
        }

        MethodSpec.Builder constructorBuilder = MethodSpec.constructorBuilder()
            .addModifiers(Modifier.PUBLIC)
            .addJavadoc("Public constructor required by Jackson. Use builder in code.");

        List<String> constructorParameterNames = new ArrayList<>();

        if (t instanceof ObjectTypeDeclaration) {

            ObjectTypeDeclaration object = (ObjectTypeDeclaration) t;

            object.properties().forEach(p -> {

                TypeName type = findClass(p.type(), p);

                constructorParameterNames.add(p.name());

                addField(builder, p, type);
                addBuilderField(builderBuilder, p, type);

                addGetMethod(builder, p, type);

                TypeName builderType = ClassName.get(packageName, t.name(), t.name() + "Builder");

                addBuilderMethod(builderBuilder, p, type, builderType, t.name());

                constructorBuilder
                    .addParameter(type, p.name())
                    .addJavadoc("@param $L the $L property\n", p.name(), p.name())
                    .addStatement("this.$N = $N", p.name(), p.name());
            });

        }

        MethodSpec buildMethod = MethodSpec.methodBuilder("build")

            .addModifiers(Modifier.PUBLIC)
            .addJavadoc("Creates a new $L with all configuration options that have been specified by calling methods on this builder.\n", t.name())
            .addJavadoc("@returns the new $L", t.name())
            .returns(ClassName.get(packageName, t.name()))
            .addStatement("return new " + t.name() + "(" + constructorParameterNames.stream().collect(Collectors.joining(", ")) + ")")

            .build();

        builderBuilder.addMethod(buildMethod);

        String result = constructorParameterNames.stream()
            .map((s) -> "\"" + s + "\"")
            .collect(Collectors.joining(", "));

        final AnnotationSpec annotationSpec = AnnotationSpec.builder(ConstructorProperties.class)
            .addMember("value", "{" + result + "}")
            .build();
        MethodSpec constructor = constructorBuilder
            .addAnnotation(annotationSpec)
            .build();

        MethodSpec builderMethod = MethodSpec.methodBuilder("builder")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addJavadoc("Creates a new $L.\n\n", t.name() + "Builder")
            .addJavadoc("@returns the new $L", t.name() + "Builder")
            .returns(ClassName.get(packageName, t.name(), t.name() + "Builder"))
            .addStatement("return new " + t.name() + "Builder()")
            .build();

        TypeSpec spec = builder
            .addMethod(constructor)
            .addMethod(builderMethod)
            .addType(builderBuilder.build())
            .addAnnotation(AnnotationSpec.builder(Generated.class).addMember("value", "$S", "Generated from RAML").build())
            .build();

        JavaFile javaFile = JavaFile.builder(packageName, spec)
            .skipJavaLangImports(true)
            .build();

        javaFile.writeTo(Paths.get(output.getAbsolutePath(),"src/main/java"));
    }

    private TypeName findClass(String type, TypeDeclaration p) {
        if (types.containsKey(type)) {
            return types.get(type);

        }

        if ("array".equals(type)) {
            ArrayTypeDeclaration declaration = (ArrayTypeDeclaration) p;
            TypeName itemType = findClass(declaration.items().type(), declaration.items());
            ClassName set = ClassName.get("java.util", "Set");
            return ParameterizedTypeName.get(set, itemType);
        }

        throw new IllegalArgumentException(type);
    }
    private String capitalize(String input) {
        return input.substring(0,  1).toUpperCase() + input.substring(1);
    }

    private void addGetMethod(TypeSpec.Builder builder, TypeDeclaration p, TypeName type) {

        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("get" + capitalize(p.name()))
            .returns(type)
            .addModifiers(Modifier.PUBLIC)
            .addStatement("return this." + p.name());
        if (p.description() != null) {
            methodBuilder.addJavadoc(p.description().value());
        }
        MethodSpec get = methodBuilder
            .build();

        builder.addMethod(get);
    }

    private void addBuilderMethod(TypeSpec.Builder builder, TypeDeclaration p, TypeName type, TypeName builderType, String name) {

        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(p.name())
            .returns(builderType)
            .addModifiers(Modifier.PUBLIC)
            .addJavadoc( "Sets the $L property for the new $L.\n", p.name(), name)
            .addJavadoc( "@param $L the $L\n", p.name(), p.name())
            .addJavadoc( "@returns a reference to this $T", builderType)
            .addParameter(type, p.name(), Modifier.FINAL)
            .addStatement("this." + p.name() + "=" + p.name())
            .addStatement("return this");

        if (p.description() != null) {
            methodBuilder.addJavadoc(p.description().value());
        }

        MethodSpec get = methodBuilder
            .build();

        builder.addMethod(get);
    }

    private void addField(TypeSpec.Builder builder, TypeDeclaration p, TypeName type) {
        FieldSpec.Builder fieldBuilder = FieldSpec.builder(type, p.name())
            .addModifiers(Modifier.PRIVATE, Modifier.FINAL);

        FieldSpec field = fieldBuilder
            .build();

        builder.addField(field);
    }

    private void addBuilderField(TypeSpec.Builder builder, TypeDeclaration p, TypeName type) {
        FieldSpec.Builder fieldBuilder = FieldSpec.builder(type, p.name())
            .addModifiers(Modifier.PRIVATE);

        FieldSpec field = fieldBuilder
            .build();

        builder.addField(field);
    }

    private void printType(TypeDeclaration declaration) {
        if (declaration instanceof ObjectTypeDeclaration) {

            ObjectTypeDeclaration object = (ObjectTypeDeclaration) declaration;

            object.properties().forEach(t -> {
                System.out.println("  " + t.displayName().value());
            });

        }
    }
}
