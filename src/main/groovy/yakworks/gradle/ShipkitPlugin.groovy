package yakworks.gradle

import groovy.transform.CompileStatic;
import org.gradle.api.Plugin;
import org.gradle.api.Project
import org.shipkit.internal.gradle.bintray.BintrayReleasePlugin
import org.shipkit.internal.gradle.java.JavaBintrayPlugin
import org.shipkit.internal.gradle.java.PomContributorsPlugin;
import org.shipkit.internal.gradle.release.ShipkitBasePlugin
import org.shipkit.internal.gradle.release.TravisPlugin
import org.shipkit.internal.gradle.util.ProjectUtil;

/**
 * Continuous delivery for Java with Travis and Bintray.
 * Intended for root project of your Gradle project because it applies some configuration to 'allprojects'.
 * Adds plugins and tasks to setup automated releasing for a typical Java multi-project build.
 * <p>
 * Applies following plugins:
 *
 * <ul>
 *     <li>{@link ShipkitBasePlugin}</li>
 *     <li>{@link PomContributorsPlugin}</li>
 * </ul>
 *
 * Adds behavior:
 * <ul>
 *     <li>Applies {@link JavaBintrayPlugin} to all Java projects in a multi-project Gradle build
 *          (all projects that use Gradle's "java" plugin).</li>
 * </ul>
 */
@CompileStatic
public class ShipkitGroovyPlugin implements Plugin<Project> {

    public void apply(final Project project) {
        ProjectUtil.requireRootProject(project, this.getClass())
        //project.getPlugins().apply(ShipkitBasePlugin.class); see below for what this did
        //we don't apply the ShipkitBasePlugin because we don't wan't the TravisPlugin applied and instead want circle ci
        project.getPlugins().apply(CirclePlugin);
        project.getPlugins().apply(BintrayReleasePlugin)

        project.getPlugins().apply(PomContributorsPlugin)

        project.allprojects { Project subproject ->
            subproject.getPlugins().withId("java") {
                subproject.getPlugins().apply(JavaBintrayPlugin)
            }
        }
    }
}
