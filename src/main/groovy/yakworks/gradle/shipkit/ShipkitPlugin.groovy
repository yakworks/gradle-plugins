package yakworks.gradle.shipkit

import com.jfrog.bintray.gradle.BintrayExtension
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.internal.ClosureBackedAction
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.shipkit.internal.gradle.bintray.BintrayReleasePlugin
import org.shipkit.internal.gradle.configuration.ShipkitConfigurationPlugin
import org.shipkit.internal.gradle.git.GitPlugin
import org.shipkit.internal.gradle.java.JavaBintrayPlugin
import org.shipkit.internal.gradle.java.JavaLibraryPlugin
import org.shipkit.internal.gradle.java.JavaPublishPlugin
import org.shipkit.internal.gradle.java.PomContributorsPlugin
import org.shipkit.internal.gradle.release.ReleasePlugin
import org.shipkit.internal.gradle.util.ProjectUtil
import yakworks.commons.ConfigMap
import yakworks.commons.Pogo
import yakworks.gradle.DefaultsPlugin
import yakworks.gradle.DocmarkPlugin
import yakworks.gradle.config.YamlConfigPlugin

/**
 * Continuous delivery for Java/Groovy/Grails with CirclePlugin and Bintray.
 * Intended for root project of your Gradle project because it applies some configuration to 'allprojects'.
 * Adds plugins and tasks to setup automated releasing for a typical Java/Groovy/Grails multi-project build.
 */
@CompileStatic
class ShipkitPlugin implements Plugin<Project> {
    private final static Logger LOG = Logging.getLogger(ShipkitPlugin)

    public void apply(final Project project) {
        ProjectUtil.requireRootProject(project, this.getClass())
        project.plugins.with {
            apply(ConfigYakPlugin)
            apply(CircleReleasePlugin)
            apply(DefaultsPlugin)
        }
        ConfigMap config = project.plugins.apply(YamlConfigPlugin).config

        boolean isBintray = config['bintray.enabled']

        if(isBintray){
            project.plugins.apply(BintrayReleasePlugin)
        } else{
            project.plugins.apply(MavenRepoReleasePlugin)
        }
        project.plugins.apply(PomContributorsPlugin)

        project.allprojects { Project prj ->

            prj.plugins.withType(JavaLibraryPlugin) {
                if(isBintray){
                    prj.plugins.apply(JavaBintrayPlugin)
                    configBintray(prj, config)

                    prj.plugins.withId("yakworks.grails-plugin") {
                        configGrailsBintray(prj)
                    }
                } else{
                    //JavaBintrayPlugin takes care of these above
                    prj.plugins.apply(JavaPublishPlugin)
                    //prj.plugins.apply(ComparePublicationsPlugin) //TODO fix this
                }

                if (prj['isSnapshot'] || !isBintray) {
                    LOG.lifecycle("Setting up publish maven Repo to ${prj['mavenPublishUrl']} because one of these is true\n" +
                        " - isSnapshot: " + prj['isSnapshot'] + ", (!isBintray): " + !isBintray + "\n" )
                    setupPublishRepo(prj)
                }

                cleanDepsInPom(prj)
                wireUpDocPublishing(project)
            }
        }
    }

    @CompileDynamic
    void setupPublishRepo(Project project){
        project.extensions.configure PublishingExtension, new ClosureBackedAction( {
            repositories {
                maven {
                    url project.mavenPublishUrl
                    credentials {
                        username project.findProperty("mavenRepoUser")
                        password project.findProperty("mavenRepoKey")
                    }
                }
            }
        })
    }

    //Sets dependendsOn and wires up so gitPush will take into account the README updates and the Mkdocs will get run after a release
    void wireUpDocPublishing(Project project){
        final Task updateReadme = project.tasks.getByName(DocmarkPlugin.UPDATE_README_TASK)
        final File rmeFile = project.file('README.md')
        GitPlugin.registerChangesForCommitIfApplied([rmeFile], 'README.md versions', updateReadme)

        final Task performRelease = project.tasks.getByName(ReleasePlugin.PERFORM_RELEASE_TASK)
        boolean enableDocsPublish = project.hasProperty('enableDocsPublish')? Boolean.valueOf(project['enableDocsPublish'].toString()) : true
        if(enableDocsPublish) {
            String gitPublishDocsTaskName = 'gitPublishPush'
            if (project.hasProperty(ShipkitConfigurationPlugin.DRY_RUN_PROPERTY)) {
                gitPublishDocsTaskName = 'gitPublishCopy'
            }
            //LOG.lifecycle("gitPublishDocsTaskName $gitPublishDocsTaskName" )
            final Task gitPublishDocsTask = project.tasks.getByName(gitPublishDocsTaskName)
            gitPublishDocsTask.mustRunAfter(GitPlugin.GIT_PUSH_TASK)
            performRelease.dependsOn(gitPublishDocsTask)
        } else {
            //LOG.lifecycle("Doc will not be published as enableDocsPublish is false" )
        }
    }

    /**
     * Taken from GrailsCentralPublishGradlePlugin in grails-core. its the 'org.grails.grails-plugin-publish'
     * Cleans up dependencies without versions and removes the bom dependencyManagement stuff and adds the grails-plugin.xml artefact
     */
    //FIXME I don't think this is how it should be done.
    @CompileDynamic
    private void cleanDepsInPom(Project project) {
        project.plugins.withType(MavenPublishPlugin) {
            project.extensions.configure PublishingExtension, new ClosureBackedAction( {
                publications {
                    javaLibrary(MavenPublication) {
                        artifact getGrailsPluginArtifact(project)
                        pom.withXml {
                            Node pomNode = asNode()
                            if (pomNode.dependencyManagement) {
                                pomNode.dependencyManagement[0].replaceNode {}
                            }
                            pomNode.dependencies.dependency.findAll {
                                it.version.text().isEmpty()
                            }.each {
                                it.replaceNode {}
                            }
                        }
                    }
                }
            })
        }
    }

    /**
     * sets up the bintray task defualts
     */
    @CompileDynamic
    private void configBintray(Project project, ConfigMap config) {
        project.afterEvaluate { prj ->
            final BintrayExtension bintray = project.getExtensions().getByType(BintrayExtension.class)
            binConfig.evalAll() //make sure StringTemplates are evaluated
            bintray.user = onfig['bintray.user']
            bintray.key = onfig['bintray.key']

            final BintrayExtension.PackageConfig pkg = bintray.getPkg()
            Pogo.merge(pkg,config['bintray.pkg'])
            Pogo.merge(pkg.version.gpg,config['bintray.pkg.version.gpg'])
            Pogo.merge(pkg.version.mavenCentralSync,config['bintray.pkg.version.mavenCentralSync'])

            pkg.name = prj.name
            pkg.version.name = prj.version
        }
    }

    private void configGrailsBintray(Project project) {
        project.afterEvaluate { prj ->
            final BintrayExtension bintray = project.getExtensions().getByType(BintrayExtension.class)
            bintray.pkg.version.attributes = ["grails-plugin": "${prj['group']}:${prj['name']}".toString()]
        }
    }

    @CompileDynamic
    protected Map<String, String> getGrailsPluginArtifact(Project project) {
        def directory
        try {
            directory = project.sourceSets.main.groovy.outputDir
        } catch (Exception e) {
            directory = project.sourceSets.main.output.classesDir
        }
        [source: "${directory}/META-INF/grails-plugin.xml".toString(),
         classifier: "plugin",
         extension: 'xml']
    }

}
