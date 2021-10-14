/*
* Copyright 2021 Yak.Works - Licensed under the Apache License, Version 2.0 (the "License")
* You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
*/
package yakworks.gradle.util

import groovy.transform.CompileStatic
import groovy.xml.QName

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.XmlProvider
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.publish.maven.MavenPublication

import yakworks.commons.ConfigMap
import yakworks.gradle.util.team.TeamMember

import static yakworks.gradle.util.BuildConventions.contributorsFile
import static yakworks.gradle.util.team.TeamParser.parsePerson

/**
 * Customizes the pom file. Intended to be used with Gradle's 'maven-publish' plugin.
 */
@CompileStatic
class PomCustomizer {

    private static final Logger LOG = Logging.getLogger(PomCustomizer);

    /**
     * Customizes the pom. The method requires following properties on root project to function correctly:
     */
    public static void customizePom(final Project project, final ConfigMap config, final MavenPublication publication) {
        publication.getPom().withXml(new Action<XmlProvider>() {
            public void execute(XmlProvider xml) {
                String archivesBaseName = (String) project.getProperties().get("archivesBaseName");
                File contributorsFile = contributorsFile(project);
                LOG.info("  Read project contributors from file: " + contributorsFile.getAbsolutePath());

                final boolean isAndroidLibrary = project.getPlugins().hasPlugin("com.android.library");
                customizePom(xml.asNode(), config, archivesBaseName, project.getDescription(), isAndroidLibrary);
            }
        });
    }

    /**
     * Customizes pom xml based on the provide configuration and settings
     */
    static void customizePom(Node root, ConfigMap config,
                             String projectName, String projectDescription,
                             boolean isAndroidLibrary) {
        //TODO: we need to conditionally append nodes because given node may already be on the root (issue 847)
        //TODO: all root.appendNode() need to be conditional
        root.appendNode("name", projectName);

        if (!isAndroidLibrary && root.getAt(new QName("packaging")).isEmpty()) {
            root.appendNode("packaging", "jar");
        }

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
