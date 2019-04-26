# gradle-defaults
Inspired and initially forked from https://github.com/ajoberstar/gradle-defaults
**DISCLAIMER:** This project tracks _yakworks_ and _9ci_ preferences for defaults but can easily be overridden

It will follow SemVer for breaking changes (prior to 1.0.0, minors may include breaking changes).

See it in action here https://github.com/yakworks/gorm-tools or in this project

## What does it do?

It's optimized for:

* A Groovy library that will be published to Bintray
* A Grails plugin that will be published to Bintray

Its enforces a best pratice subproject layout.

## Usage

In the root project apply the defaults plugin:

```groovy
plugins {
  id 'org.yakworks.defaults' version '1.0.0'
}
```

In a grails plugin

```groovy
apply plugin: "yakworks.grails-plugin"
```
## Tasks

These are the main tasks the this adds or extends. Some are from shipyak and some are from shipkit etc 
and mentioned here if they are considered common and useful to the release process.

### Publishing Libs

These are used for development and overrides or quick publishing when needed. 
For doing a full release and CI better to use the appropriate release tasks .

- **snapshot** - publishes lib to your local maven on your computer
- **publish** - Not For Bintray. This the normal maven publish task but with easier config. 
  if snapshot=true in version.props then this will append SNAPSHOT to the version and use the maven.snapshotUrl to publish
- **bintrayUpload** - If using Bintray then this is what creates and uploads lib. Bintray will reject it if snapshot=true 

### Releasing

These do a full release, and trys to be intelligent and automatically running checks/tests
updating changelogs, incrementing version.properties tagging, 
generatings docs and then publishing to appropriate repo. 

- **publishRelease** - mostly for testing, bascically just fires **performRelease** unless snapshot=true, then it just publishes lib and docs.
- **ciPublish** - generally doing a release should be done through a CI and this is the one you want to call. 
  Setup CI to call this task as it forks for checks and will do a diff to check if only docs changed. 
  Will fire **releaseNeeded**, **ciReleasePrepare** and then **performRelease**. 
  If snapshot=true in version then it takes care of sorting out the proper repo for lib publising as well as where to put generated docs
- **ciCheck** - TODO

### Docs

- **publishDocs** : fires **mkdocsBuild** to build the docs and then fires gitPublishPush to publish to githubs ghpages.
  if snapshot=true then is places docs in a snapshot subdir under gh-pages
- **mkdocsBuild** : Runs the mkdocs (python) command to generate the docs in build dir. Will fire **mkdocsPrepare**
- **mkdocsPrepare** : Usefull for debugging. Copies docs to build and runs the replace to tokens to get it ready for build.
- **updateReadmeVersions** : updates the README.md to update the version to the latest in version.properties
- **groovydocMerge** : builds the groovy docs. will merge all the publishable lib's groovydocs into one. 

## Codenarc and Overrides

This provides an opinionated ruleset an can be seen in the src/main/resources/codenarcRulesets.groovy
to override do something like the following in build.gradle to turn off.
```
//codenarc overrides for yakworks ruleset defaults
ext.codenarcRuleset= '''
  getRule('MethodParameterTypeRequired').enabled = false 
  getRule('Println').enabled = false
'''
```

## config.yml or shipyak.yml


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


