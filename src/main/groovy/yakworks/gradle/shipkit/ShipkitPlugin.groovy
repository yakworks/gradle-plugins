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
 * Use ShipyakPlugin
 */
@Deprecated
@CompileStatic
class ShipkitPlugin implements Plugin<Project> {

    public void apply(final Project project) {
        project.plugins.apply(ShipyakPlugin)
    }

}
