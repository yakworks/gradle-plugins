/**
 * setup defs for codenarcIntegrationTest
 */
ruleset {
    ruleset('rulesets/basic.xml'){
        'EmptyClass' doNotApplyToFilesMatching: '.*Spec.groovy'
        'EmptyMethod' doNotApplyToFilesMatching: ".*Controller.groovy"
    }

    ruleset('rulesets/braces.xml') {
        'IfStatementBraces' enabled: false
    }

    ruleset('rulesets/comments.xml'){
        ['JavadocEmptyLastLine', 'JavadocMissingThrowsDescription',
         'JavadocMissingExceptionDescription', 'ClassJavadoc'].each{
            "$it"(enabled:false)
        }

        ClassJavadoc{
            doNotApplyToFilesMatching = ".*/src/test/.*|.*GrailsPlugin.groovy|.*Application.groovy"
        }
    }

    ruleset('rulesets/concurrency.xml')

    ruleset('rulesets/convention.xml'){
        ['CouldBeElvis', 'NoDef', 'ParameterReassignment',
         'MethodReturnTypeRequired', 'CouldBeSwitchStatement', 'InvertedCondition', 'TrailingComma',
         'VariableTypeRequired', 'FieldTypeRequired', 'PublicMethodsBeforeNonPublicMethods','NoJavaUtilDate',
         'StaticFieldsBeforeInstanceFields', //FIXME this should be enabled
         'StaticMethodsBeforeInstanceMethods', 'IfStatementCouldBeTernary',
         'ImplicitReturnStatement', 'ImplicitClosureParameter'
        ].each{
            "$it"(enabled:false)
        }
        CompileStatic  {
            doNotApplyToFilesMatching = ".*/src/test/.*|.*GrailsPlugin.groovy|.*Application.groovy"
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
         'AbstractClassWithoutAbstractMethod', 'PrivateFieldCouldBeFinal', 'ImplementationAsType'
        ].each{
            "$it"(enabled:false)
        }
        'ReturnsNullInsteadOfEmptyCollection' doNotApplyToFilesMatching: ".*Controller.groovy"
        //FIXME enable PrivateFieldCouldBeFinal
        BuilderMethodWithSideEffects{
            methodNameRegex = '(make.*|build.*)'
        }
    }

    // ruleset('rulesets/dry.xml'){
    //     ['DuplicateNumberLiteral', 'DuplicateStringLiteral'].each{
    //         "$it"(enabled:false)
    //     }
    // } // doesn't do much for us and we end up commenting out because of map constructors

    //ruleset('rulesets/enhanced.xml')//FIXME try adding in the src to classpath so theese work

    ruleset('rulesets/exceptions.xml'){
        // CatchException {
        //     //doNotApplyToFileNames = "*Controller.groovy"
        //     doNotApplyToClassNames = "*Controller"
        // }
        CatchException(enabled:false)
    }

    ruleset('rulesets/formatting.xml'){
        LineLength(doNotApplyToFilesMatching: '.*Spec.groovy', length:160)

        ['BlockEndsWithBlankLine', 'BlockStartsWithBlankLine',
         'SpaceAfterCatch', 'SpaceAfterFor', 'SpaceAfterIf', 'SpaceAfterSwitch', 'SpaceAfterWhile',
         'SpaceInsideParentheses', 'SpaceAfterMethodCallName',
         'SpaceAroundClosureArrow', 'SpaceAroundMapEntryColon', 'SpaceAroundOperator',
         'SpaceAfterOpeningBrace', 'SpaceAfterClosingBrace', 'SpaceBeforeOpeningBrace', 'SpaceBeforeClosingBrace',
         'TrailingWhitespace','ClassEndsWithBlankLine', 'ClassStartsWithBlankLine', 'ConsecutiveBlankLines'
        ].each{
            "$it"(enabled:false)
        }
    }

    ruleset('rulesets/generic.xml')

    //ruleset('rulesets/grails.xml') //FIXME why not use this?

    ruleset('rulesets/groovyism.xml'){
        'GetterMethodCouldBeProperty' enabled:false
        'ExplicitCallToAndMethod' enabled:false
        'ExplicitCallToOrMethod' enabled:false
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
        exclude 'MethodName'
        exclude 'ConfusingMethodName'
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
        'MethodSize' doNotApplyToFilesMatching: '.*Spec.groovy'
        //'ParameterCount' maxParameters: 6
        exclude 'CrapMetric'
        AbcMetric {
            doNotApplyToFilesMatching = '.*Spec.groovy'
            maxMethodAbcScore = 75
            maxClassAverageMethodAbcScore = 75
        }
        MethodCount {
            doNotApplyToClassNames = "*Controller"
            maxMethods = 50
        }
        ParameterCount(maxParameters:7)
        CyclomaticComplexity{
            maxMethodComplexity = 30
            maxClassAverageMethodComplexity = 30
        }
        ClassSize {
            maxLines = 750
        }
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
        exclude 'UnnecessaryObjectReferences'
        exclude 'UnnecessaryCast'
    }

    ruleset('rulesets/unused.xml'){
        UnusedPrivateField{
            ignoreFieldNames = 'serialVersionUID,log,LOG'
        }
        exclude 'UnusedMethodParameter' //FIXME this should be enabled
        exclude 'UnusedVariable'
    }
    //
    // ruleset('rulesets/codenarc-extra.xml') {
    //     CompileStatic  {
    //         doNotApplyToFilesMatching = ".*/src/test/.*|.*GrailsPlugin.groovy|.*Application.groovy"
    //     }
    // }

    def getRule = { name ->
        //def ruleClass = org.codenarc.ruleregistry.RuleRegistryHolder.ruleRegistry?.getRuleClass(name)
        getRuleSet().rules.find{it.name==name}
    }

    /*@extCodenarcRulesets@*/
}
