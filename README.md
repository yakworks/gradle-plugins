**DISCLAIMER:** This project tracks _yakworks_ and _9ci_ preferences for defaults but can easily be overridden

See it in action here https://github.com/yakworks/gorm-tools or in this project

NOTE: publish this gradle plugin `gw login` and `gw publishPlugins` 

Version 2.6+ for gradle5+

## What does it do?

It's optimized for:

* A Grails plugin or Groovy library that will be published to Artifactory

Its enforces a best pratice subproject layout.

## Usage

In the root project apply the shipyak plugin:

```groovy
apply plugin: 'yakworks.shipyak'
```

In a grails plugin

```groovy
apply plugin: "yakworks.grails-plugin"

```

In a groovy lib

```groovy
apply plugin: "yakworks.groovy-lib"

```

## Tasks

These are the main tasks the this adds or extends. Some are from shipyak and some are from shipkit etc 
and mentioned here if they are considered common and useful to the release process.

### Publishing Libs

These are used for development and overrides or quick publishing when needed. 
For doing a full release and CI better to use the appropriate release tasks .

- **snapshot** - publishes lib to your local maven on your computer
- **publish** - This the normal maven publish task but with easier config. 
  if snapshot=true in version.props then this will append SNAPSHOT to the version and use the maven.snapshotUrl to publish

### Releasing and Docs

Releasing and Docs have been moved into Make, see https://github.com/yakworks/bin

### Groovydocs

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

## build.yml

see here for the defaults

https://github.com/yakworks/gradle-plugins/blob/master/src/main/resources/configs/defaults.yml
