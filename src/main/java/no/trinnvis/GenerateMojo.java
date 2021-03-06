package no.trinnvis;

import java.io.File;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import org.apache.maven.project.MavenProject;

/**
 * Says "Hi" to the user.
 *
 *
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class GenerateMojo extends AbstractMojo
{
    /**
     * Location of the output directory.
     */
    @Parameter(name = "output",
        property = "swagger.codegen.maven.plugin.output",
        defaultValue = "${project.build.directory}/generated-sources/openapi")
    private File output;
    /**
     * Location of the input specification.
     */
    @Parameter(name = "input",
        property = "swagger.codegen.maven.plugin.input",
        defaultValue = "${project.basedir}/src/main/openapi/openapi.yaml")
    private File input;

    /**
     * The project being built.
     */
    @Parameter(readonly = true, required = true, defaultValue = "${project}")
    private MavenProject project;


    public void execute() throws MojoExecutionException
    {
        getLog().info( "Generate API model from open api definition" );
        getLog().info( "Input " + input.getAbsolutePath() );
        getLog().info( "Output " + output.getAbsolutePath() );

        OpenApiGenerator generator = new OpenApiGenerator(output, input);
        generator.execute();

        final String sourceFolder =  "src/main/java";

        String sourceJavaFolder = output.toString() + "/" + sourceFolder;
        project.addCompileSourceRoot(sourceJavaFolder);

    }
}