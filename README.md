# gradle-defaults
Inspired and initially forked from https://github.com/ajoberstar/gradle-defaults
**DISCLAIMER:** This project tracks _yakworks_ preferences for defaults. It will follow SemVer for breaking changes (prior to 1.0.0, minors may include breaking changes).


## What does it do?

It's optimized for:

* A Groovy library that will be published to Bintray
* A Grails plugin that will be published to Bintray

Its enforces a best pratice subproject layout.

## Usage

In the root project apply the defaults plugin:

```groovy
plugins {
  id 'org.yakworks.defaults' version '0.1.0'
}
```

In a grails plugin

```groovy
apply plugin: "yakworks.grails-plugin"
```

## Details

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
- `org.ajoberstar.semver-vcs-grgit` applied

Tasks in the root project:

- **mkdocsBuild**
- **update-readme-versions**
- **groovydocMerge**

In all publishable projects:

- `codenarc`
- `com.diffplug.spotless` applied
  - Expects a license header in `<root project>/gradle/HEADER`. Will be applied to Java and Groovy files.
  - Groovy code formatted
  - Gradle files formatted for 2 space indents, trailing space removed, and newlines at the end of files.
- ordering rules:
  - `clean` runs before everything else
  - all `publishing` tasks run after `build`


### Overriding Defaults
if you want to change how spotelss is setup
`spotless.formats.remove 'groovyGradle'` and then setup as normal or
just change a specific setting such as `spotless.formats.groovyGradle.target('gradle/*.gradle', 'build.gradle')`


