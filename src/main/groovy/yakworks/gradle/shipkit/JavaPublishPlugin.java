package yakworks.gradle.shipkit;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.bundling.Jar;

import yakworks.commons.ConfigMap;
import yakworks.gradle.util.GradleDSLHelper;
import yakworks.gradle.util.PomCustomizer;
import yakworks.gradle.util.StringUtil;
import yakworks.gradle.util.TaskMaker;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Arrays.asList;

/**
 * Publishing java libraries using 'maven-publish' plugin.
 * Intended to be applied in individual Java submodule.
 * Applies following plugins and tasks and configures them:
 *
 * <ul>
 *     <li>{@link JavaSourcesDocJarPlugin}</li>
 *     <li>maven-publish</li>
 * </ul>
 *
 * Other features:
 * <ul>
 *     <li>Configures Gradle's publications to publish java library</li>
 *     <li>Configures 'build' task to depend on 'publishJavaLibraryToMavenLocal'
 *          to flesh out publication issues during the build</li>
 *     <li>Configures 'snapshot' task to depend on 'publishJavaLibraryToMavenLocal'</li>
 * </ul>
 */
public class JavaPublishPlugin implements Plugin<Project> {

    private final static Logger LOG = Logging.getLogger(JavaPublishPlugin.class);

    public final static String PUBLICATION_NAME = "javaLibrary";
    public final static String POM_TASK = "generatePomFileFor" + StringUtil.capitalize(PUBLICATION_NAME) + "Publication";
    public final static String MAVEN_LOCAL_TASK = "publish" + StringUtil.capitalize(PUBLICATION_NAME) + "PublicationToMavenLocal";
    public static final String SNAPSHOT_TASK = "snapshot";

    public void apply(final Project project) {

        ConfigMap config  = project.getRootProject().getPlugins().apply(YamlConfigShipYakPlugin.class).getConfig();

        Task snapshotTask = TaskMaker.task(project, SNAPSHOT_TASK, t -> {
            t.setDescription("create local snapshot files.");
        });
        snapshotTask.dependsOn(MAVEN_LOCAL_TASK);
        configureSnapshotTask(snapshotTask, project.getGradle().getStartParameter().getTaskNames());

        project.getPlugins().apply(JavaSourcesDocJarPlugin.class);
        project.getPlugins().apply("maven-publish");

        final Jar sourcesJar = (Jar) project.getTasks().getByName(JavaSourcesDocJarPlugin.SOURCES_JAR_TASK);
        final Jar javadocJar = (Jar) project.getTasks().getByName(JavaSourcesDocJarPlugin.JAVADOC_JAR_TASK);

        GradleDSLHelper.publications(project, publications -> {
            MavenPublication p = publications.create(PUBLICATION_NAME, MavenPublication.class, publication -> {
                publication.from(project.getComponents().getByName("java"));
                publication.artifact(sourcesJar);
                publication.artifact(javadocJar);
                publication.setArtifactId(((Jar) project.getTasks().getByName("jar")).getArchiveBaseName().getOrNull());
                PomCustomizer.customizePom(project, config, publication);
            });
            LOG.info("{} - configured '{}' publication", project.getPath(), p.getArtifactId());
        });

        //so that we flesh out problems with maven publication during the build process
        project.getTasks().getByName("build").dependsOn(MAVEN_LOCAL_TASK);
    }

    static boolean configureSnapshotTask(Task snapshotTask, List<String> taskNames) {
        boolean isSnapshot = taskNames.contains(SNAPSHOT_TASK);
        if (isSnapshot) {
            snapshotTask.getProject().getTasks().matching(withName("javadoc", "groovydoc")).all(doc -> {
                LOG.info("{} - disabled to speed up the 'snapshot' build", snapshotTask.getPath());
                doc.setEnabled(false);
            });
        }
        return isSnapshot;
    }

    public static Spec<Task> withName(final String... names) {
        Set<String> namesSet = new HashSet<>(asList(names));
        return t -> namesSet.contains(t.getName());
    }
}
