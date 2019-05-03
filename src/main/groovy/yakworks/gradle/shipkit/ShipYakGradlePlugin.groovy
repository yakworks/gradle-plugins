package yakworks.gradle.shipkit


import groovy.transform.CompileStatic
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.shipkit.internal.gradle.release.GradlePortalReleasePlugin
import org.shipkit.internal.gradle.util.ProjectUtil

/**
 * yakworks.gradle-plugin , the shipkit one wont work as it depends on travis. This will work for circle.
 */
@CompileStatic
class ShipYakGradlePlugin implements Plugin<Project> {

    public void apply(final Project project) {
        ProjectUtil.requireRootProject(project, this.getClass());
        project.plugins.apply(ShippablePlugin)
        project.plugins.apply('groovy')
        project.plugins.apply(GradlePortalReleasePlugin);
    }

}
