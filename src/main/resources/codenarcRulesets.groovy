ruleset {
    ruleset('rulesets/basic.xml'){
        'EmptyClass' doNotApplyToFilesMatching: '.*Spec.groovy'
    }

    ruleset('rulesets/braces.xml') {
        'IfStatementBraces' enabled: false
    }

    ruleset('rulesets/concurrency.xml')

    ruleset('rulesets/convention.xml'){
        ['CouldBeElvis', 'NoDef', 'ParameterReassignment',
         'MethodReturnTypeRequired', 'CouldBeSwitchStatement', 'InvertedCondition', 'TrailingComma',
         'VariableTypeRequired', 'FieldTypeRequired', 'PublicMethodsBeforeNonPublicMethods','NoJavaUtilDate',
         'StaticFieldsBeforeInstanceFields', //FIXME this should be enabled
         'StaticMethodsBeforeInstanceMethods'
        ].each{
            "$it"(enabled:false)
        }

        //FIXME enable this
//        FieldTypeRequired {
//            doNotApplyToFilesMatching = ".*/grails-app/domain/.*|.*GrailsPlugin.groovy|.*Application.groovy|.*UrlMappings.groovy|.*BootStrap.groovy"
//        }
    }
    // convention
    // NoTabCharacter

    ruleset('rulesets/design.xml'){
        [
         'NestedForLoop', 'Instanceof',
         'AbstractClassWithoutAbstractMethod', 'PrivateFieldCouldBeFinal'
        ].each{
            "$it"(enabled:false)
        }
        //FIXME enable PrivateFieldCouldBeFinal
        BuilderMethodWithSideEffects{
            methodNameRegex = '(make.*|build.*)'
        }
    }

    //ruleset('rulesets/dry.xml') // doesn't do much for us

    //ruleset('rulesets/enhanced.xml')//FIXME try adding in the src to classpath so theese work

    ruleset('rulesets/exceptions.xml')

    ruleset('rulesets/formatting.xml'){
        LineLength(doNotApplyToFilesMatching: '.*Spec.groovy', length:160)

        ['BlockEndsWithBlankLine', 'BlockStartsWithBlankLine',
         'SpaceAfterCatch', 'SpaceAfterFor', 'SpaceAfterIf', 'SpaceAfterSwitch', 'SpaceAfterWhile',
         'SpaceAroundClosureArrow', 'SpaceAroundMapEntryColon', 'SpaceAroundOperator',
         'SpaceAfterOpeningBrace', 'SpaceAfterClosingBrace', 'SpaceBeforeOpeningBrace', 'SpaceBeforeClosingBrace',
         'TrailingWhitespace','ClassEndsWithBlankLine', 'ClassStartsWithBlankLine'
        ].each{
            "$it"(enabled:false)
        }
    }

    ruleset('rulesets/generic.xml')

    //ruleset('rulesets/grails.xml') //FIXME why not use this?

    ruleset('rulesets/groovyism.xml'){
        'GetterMethodCouldBeProperty' enabled:false
    }

    ruleset('rulesets/imports.xml'){
        MisorderedStaticImports(comesBefore:false)
        NoWildcardImports(ignoreStaticImports:true)
    }

    ruleset('rulesets/jdbc.xml')

    ruleset('rulesets/junit.xml'){
        ['JUnitPublicNonTestMethod', 'JUnitPublicProperty',
         'JUnitPublicNonTestMethod', 'ChainedTest'
        ].each{
            "$it"(enabled:false)
        }
    }

    ruleset('rulesets/logging.xml'){
        //exclude 'Println'
    }

    ruleset('rulesets/naming.xml'){
        'MethodName' doNotApplyToFilesMatching: '.*Spec.groovy'
        PropertyName {
            staticFinalRegex='[A-Z][a-zA-Z0-9_]*'
            ignorePropertyNames='_*'
        }
        FieldName {
            staticFinalRegex='[a-zA-Z][a-zA-Z0-9_]*'
            ignoreFieldNames='serialVersionUID,log,LOG'
        }
        VariableName {
            finalRegex='[a-zA-Z][a-zA-Z0-9_]*'
        }
        //exclude 'ConfusingMethodName'
        exclude 'FactoryMethodName'
    }

    ruleset('rulesets/security.xml'){
        exclude 'JavaIoPackageAccess'
        exclude 'FileCreateTempFile'
        exclude 'NonFinalPublicField'
    }

    ruleset('rulesets/serialization.xml'){
        exclude 'SerializableClassMustDefineSerialVersionUID'
    }

    ruleset('rulesets/size.xml'){
        'AbcMetric' doNotApplyToFilesMatching: '.*Spec.groovy'
        'MethodSize' doNotApplyToFilesMatching: '.*Spec.groovy'
        //'ParameterCount' maxParameters: 6
        exclude 'CrapMetric'
        MethodCount {
            doNotApplyToFilesMatching = '.*Spec.groovy'
            maxMethods = 40
        }
        ParameterCount(maxParameters:7)
        //exclude 'CyclomaticComplexity'
    }

    ruleset('rulesets/unnecessary.xml'){
        'UnnecessaryBooleanExpression' doNotApplyToFilesMatching: '.*Spec.groovy'
        'UnnecessaryObjectReferences' doNotApplyToFilesMatching: '.*Spec.groovy'
        exclude 'ConsecutiveStringConcatenation'
        exclude 'UnnecessaryBooleanExpression'
        exclude 'UnnecessaryGString'
        exclude 'UnnecessaryGetter'
        exclude 'UnnecessaryPublicModifier'
        exclude 'UnnecessaryReturnKeyword'
        exclude 'UnnecessarySetter'
        exclude 'UnnecessarySubstring'
        exclude 'UnnecessaryElseStatement'
        exclude 'UnnecessarySemicolon'
    }

    ruleset('rulesets/unused.xml'){
        UnusedPrivateField{
            ignoreFieldNames = 'serialVersionUID,log,LOG'
        }
        exclude 'UnusedMethodParameter' //FIXME this should be enabled
        exclude 'UnusedVariable'
    }

    ruleset('rulesets/codenarc-extra.xml') {
        CompileStatic  {
            doNotApplyToFilesMatching = ".*/src/test/.*|.*GrailsPlugin.groovy|.*Application.groovy"
        }
    }

    def getRule = { name ->
        //def ruleClass = org.codenarc.ruleregistry.RuleRegistryHolder.ruleRegistry?.getRuleClass(name)
        getRuleSet().rules.find{it.name==name}
    }

    /*@extCodenarcRulesets@*/
}
