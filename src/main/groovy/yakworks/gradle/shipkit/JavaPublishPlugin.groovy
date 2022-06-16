/*
* Copyright 2021 Yak.Works - Licensed under the Apache License, Version 2.0 (the "License")
* You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
*/
package yakworks.gradle.shipkit

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.xml.QName

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.XmlProvider
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.bundling.Jar
import org.gradle.util.ClosureBackedAction

import yakworks.commons.ConfigMap
import yakworks.gradle.util.team.TeamMember

import static java.util.Arrays.asList
import static yakworks.gradle.GradleHelpers.prop
import static yakworks.gradle.util.team.TeamParser.parsePerson

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
@CompileStatic
class JavaPublishPlugin implements Plugin<Project> {

    private final static Logger LOG = Logging.getLogger(JavaPublishPlugin);

    public final static String JAVA_LIB_NAME = "javaLibrary";
    public final static String MAVEN_LOCAL_TASK = "publishJavaLibraryPublicationToMavenLocal";
    public static final String SNAPSHOT_TASK = "snapshot";

    ConfigMap config

    @CompileDynamic
    void apply(final Project project) {

        config = (ConfigMap)project.rootProject.findProperty('config')

        Task snapshotTask = project.tasks.create(SNAPSHOT_TASK) {
            dependsOn MAVEN_LOCAL_TASK
            description = "create local snapshot files."
            group = "Shipyak"
        }

        checkForSnapshotTask(project, snapshotTask);

        project.getPlugins().apply(JavaSourcesDocJarPlugin);
        project.getPlugins().apply("maven-publish");

        configurePublications(project, config)
        // so that we flesh out problems with maven publication during the build process
        // project.getTasks().getByName("build").dependsOn(MAVEN_LOCAL_TASK);
    }

    @CompileDynamic
    void configurePublications(Project project, ConfigMap config) {

        final Jar sourcesJar = (Jar) project.getTasks().getByName(JavaSourcesDocJarPlugin.SOURCES_JAR_TASK);
        final Jar javadocJar = (Jar) project.getTasks().getByName(JavaSourcesDocJarPlugin.JAVADOC_JAR_TASK);

        project.extensions.configure PublishingExtension, new ClosureBackedAction({
            repositories {
                maven {
                    url config.maven.publishUrl
                    credentials {
                        username config.maven.user
                        password config.maven.key
                    }
                }
            }
            publications {
                "${JAVA_LIB_NAME}"(MavenPublication) {
                    from(project.components.java)
                    artifact(sourcesJar)
                    artifact(javadocJar)
                    setArtifactId(project.getTasks().getByName("jar").getArchiveBaseName().getOrNull())
                    pom.withXml {
                        customizePom(project, config, it)
                    }
                }
            }
        })
    }

    void checkForSnapshotTask(Project proj, Task snapshotTask) {
        List<String> taskNames = proj.getGradle().getStartParameter().getTaskNames()
        if (taskNames.contains(SNAPSHOT_TASK)) {
            // if its not a snapshot version then make it one
            String ver = proj.getVersion().toString()
            if (!ver.endsWith("-SNAPSHOT")) {
                proj.version = "${ver}-SNAPSHOT";
            }
            prop(proj.rootProject, 'isSnapshot', true)
            // disable for now
            // def withName = { Task t -> ["javadoc", "groovydoc"].contains(t.getName())} as Spec<Task>
            // snapshotTask.getProject().getTasks().matching(withName("javadoc", "groovydoc")).all(doc -> {
            //     LOG.info("{} - disabled to speed up the 'snapshot' build", snapshotTask.getPath());
            //     doc.setEnabled(false);
            // });
        }
    }

    void customizePom(final Project project, final ConfigMap config, final XmlProvider xml) {
        String archivesBaseName = (String) project.getProperties().get("archivesBaseName");
        File contributorsFile = new File(project.getBuildDir(), "/shipkit/all-contributors.json")
        LOG.info("  Read project contributors from file: " + contributorsFile.getAbsolutePath());

        String projjDesc = project.description ?: project.rootProject.description

        final boolean isAndroidLibrary = project.getPlugins().hasPlugin("com.android.library");
        customizePom(xml.asNode(), config, archivesBaseName, projjDesc, isAndroidLibrary);
    }

    /**
     * Customizes pom xml based on the provide configuration and settings
     */
    @SuppressWarnings('ExplicitCallToGetAtMethod')
    void customizePom(Node root, ConfigMap config,
                             String projectName, String projectDescription,
                             boolean isAndroidLibrary) {
        //TODO: we need to conditionally append nodes because given node may already be on the root (issue 847)
        //TODO: all root.appendNode() need to be conditional
        root.appendNode("name", projectName);

        // if (!isAndroidLibrary && root.getAt(new QName("packaging")).isEmpty()) {
        //     root.appendNode("packaging", "jar");
        // }

        String repoLink = config['project']['repoUrl']
        root.appendNode("url", repoLink);

        if (projectDescription != null) {
            root.appendNode("description", projectDescription);
        }

        Node license = root.appendNode("licenses").appendNode("license");
        license.appendNode("name", config['license']);
        license.appendNode("url", repoLink + "/blob/master/LICENSE");
        license.appendNode("distribution", "repo");

        root.appendNode("scm").appendNode("url", repoLink + ".git");

        Node issues = root.appendNode("issueManagement");
        issues.appendNode("url", repoLink + "/issues");
        issues.appendNode("system", "GitHub issues");

        if (config['team']['developers']) {
            Node developers = root.appendNode("developers");
            for (String notation : config['team']['developers']) {
                TeamMember person = parsePerson(notation);
                Node d = developers.appendNode("developer");
                d.appendNode("id", person.gitHubUser);
                d.appendNode("name", person.name);
                d.appendNode("roles").appendNode("role", "Core developer");
                d.appendNode("url", "https://github.com/${person.gitHubUser}");
            }
        }
    }
}
