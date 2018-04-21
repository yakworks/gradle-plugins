ruleset {
    ruleset('rulesets/basic.xml'){
        'EmptyClass' doNotApplyToFilesMatching: '.*Spec.groovy'
    }

    ruleset('rulesets/braces.xml'){
        exclude 'IfStatementBraces' //FIXME add a new rule that does if statment braces unless its a single line
    }

    ruleset('rulesets/concurrency.xml')

    ruleset('rulesets/convention.xml'){
        ['CouldBeElvis', 'NoDef', 'ParameterReassignment', 'MethodParameterTypeRequired',
         'MethodReturnTypeRequired', 'CouldBeSwitchStatement', 'InvertedCondition', 'TrailingComma',
         'VariableTypeRequired'
        ].each{
            exclude it
        }
    }
    // convention
    // NoTabCharacter

    ruleset('rulesets/design.xml'){
        [
         'NestedForLoop', 'Instanceof',
         'AbstractClassWithoutAbstractMethod'
        ].each{
            exclude it
        }
        BuilderMethodWithSideEffects{
            methodNameRegex = '(make.*|build.*)'
        }
    }

    //ruleset('rulesets/dry.xml') // doesn't do much for us

    //ruleset('rulesets/enhanced.xml')//FIXME try adding in the src to classpath so theese work

    ruleset('rulesets/exceptions.xml')

    // ruleset('rulesets/formatting.xml')
    ruleset('rulesets/formatting.xml'){
        LineLength(doNotApplyToFilesMatching: '.*Spec.groovy', length:160)
        //'BlockEndsWithBlankLine' doNotApplyToFilesMatching: '*Spec.groovy'

        ['ClassJavadoc', //FIXME this should be enabled
         'BlockEndsWithBlankLine', 'BlockStartsWithBlankLine',
         'SpaceAfterCatch', 'SpaceAfterFor', 'SpaceAfterIf', 'SpaceAfterSwitch', 'SpaceAfterWhile',
         'SpaceAroundClosureArrow', 'SpaceAroundMapEntryColon', 'SpaceAroundOperator',
         'SpaceAfterOpeningBrace', 'SpaceAfterClosingBrace', 'SpaceBeforeOpeningBrace', 'SpaceBeforeClosingBrace',
         'TrailingWhitespace'
        ].each{
            exclude it
        }
    }

    ruleset('rulesets/generic.xml')

    //ruleset('rulesets/grails.xml')

    ruleset('rulesets/groovyism.xml'){
        exclude 'GetterMethodCouldBeProperty'
    }

    ruleset('rulesets/imports.xml'){
        MisorderedStaticImports(comesBefore:false)
    }

    ruleset('rulesets/jdbc.xml')

    ruleset('rulesets/junit.xml'){
        exclude 'JUnitPublicNonTestMethod'
        exclude 'JUnitPublicProperty'
        exclude 'JUnitPublicNonTestMethod'
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
            staticFinalRegex='[A-Z][a-zA-Z0-9_]*'
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
        //exclude 'UnnecessaryDotClass' //FIXME this should be enabled
        exclude 'UnnecessarySetter' //FIXME this should be enabled
        exclude 'UnnecessarySubstring'
        exclude 'UnnecessaryElseStatement'
    }

    ruleset('rulesets/unused.xml'){
        exclude 'UnusedMethodParameter' //FIXME this should be enabled
        exclude 'UnusedVariable' //FIXME this should be enabled
    }

    ruleset('rulesets/codenarc-extra.xml') {
        CompileStatic  {
            doNotApplyToFilesMatching = ".*/src/test/.*|.*GrailsPlugin.groovy|.*Application.groovy"
        }
    }
}
