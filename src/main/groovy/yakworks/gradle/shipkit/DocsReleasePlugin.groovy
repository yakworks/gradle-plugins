/*
* Copyright 2019 Yak.Works - Licensed under the Apache License, Version 2.0 (the "License")
* You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
*/
package yakworks.gradle.shipkit

import groovy.transform.CompileStatic
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.shipkit.internal.gradle.bintray.BintrayReleasePlugin
import org.shipkit.internal.gradle.configuration.ShipkitConfigurationPlugin
import org.shipkit.internal.gradle.git.GitPlugin
import org.shipkit.internal.gradle.java.PomContributorsPlugin
import org.shipkit.internal.gradle.release.ReleasePlugin
import org.shipkit.internal.gradle.util.ProjectUtil
import yakworks.commons.ConfigMap
import yakworks.gradle.DefaultsPlugin
import yakworks.gradle.DocmarkPlugin

/**
 * Wires up the README updates and the docs build and publishing to the performRelease task
 */
@CompileStatic
class DocsReleasePlugin implements Plugin<Project> {
    private final static Logger LOG = Logging.getLogger(DocsReleasePlugin)

    ConfigMap config

    public void apply(final Project project) {
        ProjectUtil.requireRootProject(project, this.getClass())

        config = project.plugins.apply(ConfigYakPlugin).config

        Task updateReadme = project.tasks.getByName(DocmarkPlugin.UPDATE_README_TASK)
        File rmeFile = project.file('README.md')
        GitPlugin.registerChangesForCommitIfApplied([rmeFile], 'README.md versions', updateReadme)

        // if docs are enabled and mkdocs.yml file exists
        // then wire up dependency chains so performRelease fires the docs build and publish
        if(project.file('mkdocs.yml').exists() && config['docs.enabled']) {
            String gitPublishDocsTaskName = DocmarkPlugin.PUBLISH_DOCS_TASK
            if (project.hasProperty(ShipkitConfigurationPlugin.DRY_RUN_PROPERTY)) {
                gitPublishDocsTaskName = DocmarkPlugin.GH_COPY_DOCS_TASK
            }
            //LOG.lifecycle("gitPublishDocsTaskName $gitPublishDocsTaskName" )
            final Task gitPublishDocsTask = project.tasks.getByName(gitPublishDocsTaskName)
            gitPublishDocsTask.mustRunAfter(GitPlugin.GIT_PUSH_TASK)
            Task performRelease = project.tasks.getByName(ReleasePlugin.PERFORM_RELEASE_TASK)
            performRelease.dependsOn(gitPublishDocsTask)
        }
    }

}
