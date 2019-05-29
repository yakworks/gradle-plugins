**DISCLAIMER:** This project tracks _yakworks_ and _9ci_ preferences for defaults but can easily be overridden

See it in action here https://github.com/yakworks/gorm-tools or in this project

Its built on top of the awesome shipkit. Has out defaults and makes the automated releasing, tagging, docs generation and deployemnt easier.

## What does it do?

It's optimized for:

* A Grails plugin or Groovy library that will be published to Bintray or an Artifactory

Its enforces a best pratice subproject layout.

## Usage

In the root project apply the defaults plugin:

```groovy
plugins {
  id 'org.yakworks.defaults' version '1.1.3'
}
```

In a grails plugin

```groovy
apply plugin: "yakworks.grails-plugin"

```

or

```
buildscript {
  repositories {
    maven {
      url "https://plugins.gradle.org/m2/"
    }
  }
  dependencies {
    classpath "org.yakworks:gradle-plugins:1.1.13"
  }
}

apply plugin: "yakworks.shipyak"
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

## config.yml or build.yml

This add a yaml config. These are the defaults.

```
 # Can contain standard yaml pointers using the & and * and
 # strings can also use GString syntax is it starts with $.
 # available bound properties are this `config` and `project`.
 # Helper methods are property(prop), findProperty(prop)
 # and findAnyProperty(prop) which looks in env and system.properties.
 
 title: # short title used for javadocs and documentation
 description: # REQUIRED
 author: 9ci Inc # REQUIRED author for docs, copyright, and spotless licenseHeader too. Can be legal Org name or user name
 license: &License Apache-2.0 # REQUIRED will be used in the docs, pom and bintray upload
 # the pointer to the docs for this project
 websiteUrl: &SiteUrl ${"https://" + config.github.owner + ".github.io/" + config.github.repo}
 
 # everything before the first separator will get set on the project property
 # or project.ext and standards for gradle.properties should be followed.
 # everything after the first separator will in the project.config
 
 git:
   releasableBranchRegex: 'master|release/.+'   # matches 'master', 'release/2.x', 'release/3.x', etc.
   tagPrefix: v # so that tags are "v1.0", "v2.3.4"
   commitMessagePostfix: '[ci skip]' # when shipkit commits, this postfix can have ci skip it preferably
   # git config so it shows the user in the commits
   config:
     user: BuildBot
     email: <9cibot@9ci.com>
   releasable:
     branchRegex: 'master|release/.+'
     diff: # gets set to either travisDiff or circleDiff depending on autodetection of CI, can manually set this for local builds and testing
     circleDiff: 'git diff --name-only $(echo "$CIRCLE_COMPARE_URL" | rev | cut -d/ -f1 | rev)'
     travisDiff: 'git diff --name-only $TRAVIS_COMMIT_RANGE' # for Travis no parsing env var is needed.
     #grepDocs: ${config.git.releasable.diff +  ' | grep -E xxx'}
     grepDocs: ${config.git.releasable.circleDiff +  ' | grep -E (README\\.md|mkdocs\\.yml|docs/)' }
     grepRelease: ${config.git.releasable.diff +  ' | grep --invert-match -E (README\\.md|mkdocs\\.yml|docs/|src/examples/|src/test/)' }
 
 github:
   fullName: # REQUIRED (example: yakworks/gradle-plugins) if none specified then will try to find from git config --get remote.origin.url
   owner: ${config.github.fullName.split('/')[0]} # example: yakworks
   repo: ${config.github.fullName.split('/')[1]} # example: gradle-plugins
   repoUrl: &GithubUrl ${'https://github.com/' + config.github.fullName}
   issuesUrl: &IssuesUrl ${config.github.repoUrl + '/issues'}
   #DO NOT set and checkin your token in yml here. Use a env var or gradle.properties
   writeAuthToken: '${System.getenv("GRGIT_USER") ?: System.getenv("GH_WRITE_TOKEN") ?: project.findProperty("gitHubToken")}'
   readOnlyAuthToken: '${System.getenv("GRGIT_USER") ?: System.getenv("GH_WRITE_TOKEN") ?: project.findProperty("gitHubToken") ?: "e7fe8fcdd6ffed5c38498c4c79b2a68e6f6ed1bb"}'
 
 team:
   # keys here should be github user and name should match github name.
   developers: # REQUIRED List in form githubUser: Full Name ex: ['basejump: Joshua Burnett', 'jimbob: Jim Bob']
   contributors: # Other contribs , List same format as developers
   ignoredContributors: ['9cibot'] # list of names or logins to ignore, mostly for changlog. good for robot/CI user's commits when build/deploying
 
 releaseNotes:
   file: docs/release-notes.md
 
 # see docs here https://github.com/bintray/gradle-bintray-plugin. the yml supported options are below
 # this gets setup for each publishable project. If this is enabled then its assumed this is the primary place to publish
 bintray:
   enabled: true # set this to false to turn off
   #Note: properties such as BINTRAY_USER should be setup as system env variables, normally the case for CI
   # or the camelCase variant can be put in ~/.gradle/gradle.properties or even assigned in ext props.
   #DO NOT set or checkin your token in yml here and push to github, in case that wasn't obvious
   user: ${findAnyProperty("BINTRAY_USER")} # REQUIRED findAny searches for BINTRAY_USER env and then looks for bintrayUser
   key: ${props.findAny("BINTRAY_KEY")} # REQUIRED findAny searches for BINTRAY_KEY env and then looks for bintrayKey
   pkg:
     repo: # REQUIRED ex: grails-plugins
     userOrg: ${config.github.owner} # -> the github owner by default
     name: # this will get set from each subprojects name during runtime
     websiteUrl: *SiteUrl
     issueTrackerUrl: *IssuesUrl
     vcsUrl: *GithubUrl
     licenses: [ *License ]
     publicDownloadNumbers: true
     githubRepo: ${config.github.fullName} #Optional Github repository
     githubReleaseNotesFile: README.md #Github readme file
     version:
       name: ${project.version}
       gpg:
         sign: false
       mavenCentralSync:
         sync: false
 # normal maven setting can be configured if not publishing to bintray and using an maven repo such as artifactory
 # If using bintray for primary and a maven repo for snapshots then just setup user/key and the repoUrl here.
 # if not using bintray then set enabled= false on it and setup repoUrl and a snapshotUrl if they differ.
 # Currently not suported to have 2 different credentials
 maven:
   user: '${props.findAny("MAVEN_REPO_USER")}' # see notes above about env variables like this
   key: '${props.findAny("MAVEN_REPO_KEY")}'
   publishUrl: # NOT normally set in config.yml. this plugin will assign it either repoUrl or snapshotUrl depending on whether snapshot is true in version.properties
   repoUrl: 'http://repo.9ci.com/grails-plugins'
   snapshotUrl: # set this when not using bintray and you have a different url for snapshots
 
 #spotless helps keep your code clean and spotless.
 spotless:
   enabled: true
   # default license header file to use
   licenseHeader: ${'/* Copyright \$YEAR. ' + config.author + '. Licensed under the Apache License, Version 2.0 */'}
   # licenseHeaderFile: # or you specify the file name of header file to use
   groovy:
     licenseHeader: $config.spotless.licenseHeader
     indentWithSpaces: 4
     trimTrailingWhitespace: true
     endWithNewline: true
     includes:
       - 'src/main/groovy/**/*.groovy'
       - 'grails-app/**/*.groovy'
       - 'src/test/groovy/**/*.groovy'
       - 'src/integration-test/groovy/**/*.groovy'
     excludes:
       - '**/*.java'
       - '**/conf/**/*.groovy'
       # - 'grails-app/init/**/*.groovy'
   groovyGradle:
     indentWithSpaces: 2
     trimTrailingWhitespace: true
     endWithNewline: true
     includes:
       - '**/*.gradle'
       - 'build.gradle'
       - 'gradle/*.gradle'
 
 codenarc:
   toolVersion: 1.1.13
   config: prj.yakworks.getCodenarcRuleSet()
   reportFormat: 'html'
   maxPriority1Violations: 0
   maxPriority2Violations: 2
   maxPriority3Violations: 4
   main:
     ignoreFailures: false
     excludes: # make this a list of excludes is wanted
   test:
     enabled: false # if false will exclude all files by adding a exclude: '**/*', runs much faster with lots of tests
     ignoreFailures: true
     excludes:
 # the mkdocs engine
 docs:
   enabled: true

```



