# gradle-defaults
Inspired and initially forked from https://github.com/ajoberstar/gradle-defaults
**DISCLAIMER:** This project tracks _yakworks_ preferences for defaults. It will follow SemVer for breaking changes (prior to 1.0.0, minors may include breaking changes).

See it in action here https://github.com/yakworks/gorm-tools

## What does it do?

It's optimized for:

* A Groovy library that will be published to Bintray
* A Grails plugin that will be published to Bintray

Its enforces a best pratice subproject layout.

## Usage

In the root project apply the defaults plugin:

```groovy
plugins {
  id 'org.yakworks.defaults' version '0.1.3'
}
```

In a grails plugin

```groovy
apply plugin: "yakworks.grails-plugin"
```

## gradle.properties or ext props

```
  # author for docs and copyright, used in licenseHeader too. Can be legal Org name or user name
author=9ci Inc
license=Apache-2.0

#### Publishing settings GitHub/Bintray/Artifactory/Maven
group=org.yakworks
gitHubSlug=yakworks/gradle-plugins
 # default matches 'master', 'release/2.x', 'release/3.x', etc.
gitReleasableBranch="master|release/.+" 
gitConfigUser=9cibot
gitConfigEmail=9cibot@9ci.com
# bintray, don't set these to use standard mavenPublish settings
bintrayOrg=9ci
bintrayRepo=grails-plugins
bintrayUser=
bintrayKey=

mavenPublishUrl=http://repo.9ci.com/oss-snapshots
mavenPublishUser=should go in gradle.properties or set as env var MAVEN_PUBLISH_USER
mavenPublishKey=should go in gradle.properties 
# mavenSnapshotUrl= set this if different than the mavenPublishUrl

developers = {"basejump": "Joshua Burnett", "snimavat": "Sudhir Nimavat", "alexeyzvegintcev": "Alexey"}

```

What happens when the following plugins are applied:

### Always

Applied in the root project:

- `com.energizedwork.idea-project-components`
- `org.ajoberstar.grgit`
- `org.ajoberstar.git-publish`
  - Publishes to `gh-pages` branch
  - Publishes content from:
    - "build/mkdocs/site" to `/`
    - `groovydoc` task to `/api` from all projects
    - if version is snapshot then it puts it all under `/snapshots/

Tasks in the root project:

- **mkdocsBuild**
- **update-readme-versions**
- **groovydocMerge**

In all "publishable" projects:

- `codenarc`
- `com.diffplug.spotless` applied
  - Expects a license header in `<root project>/gradle/HEADER`. Will be applied to Java and Groovy files.
  - Groovy code formatted
  - Gradle files formatted for 2 space indents, trailing space removed, and newlines at the end of files.
- ordering rules:
  - `clean` runs before everything else
  - all `publishing` tasks run after `build`


### Overriding Defaults
if you want to change how spotless is setup
`spotless.formats.remove 'groovyGradle'` and then setup as normal or
just change a specific setting such as `spotless.formats.groovyGradle.target('gradle/*.gradle', 'build.gradle')`


