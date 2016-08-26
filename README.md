# ClasspathSuite

Eclipse (3.4 and below) does not have support for JUnit testing in a multi project setting. This little tool tackles the problem for JUnit4 test classes and suites.

Documentation:

*   [Getting it](#getting)
*   [How to use it](#howto)
*   [Release Notes](#changes)
*   [Open Issues](#issues)
*   [Report a bug or ask for a feature](mailto:cpsuite@johanneslink.net)
*   

### <a name="getting"></a> Getting it

ClassPathSuite is available via Maven:
````
<dependency>
    <groupId>io.takari.junit</groupId>
    <artifactId>takari-cpsuite</artifactId>
    <version>1.2.7</version>
</dependency>
````

### <a name="howto"></a>How to Use It

The mechanism is simple. Just create a new project in Eclipse and add all projects that contain tests you want to run to its build path. Now create a class like that:
		
```java
import org.junit.extensions.cpsuite.ClasspathSuite;
import org.junit.runner.RunWith;
@RunWith(ClasspathSuite.class)
public class MySuite {}
```

This will execute _all_ JUnit4 testclasses (those containing methods with the @Test annotation) in the projects classpath.

#### JAR Files

By default jar files are being ignored but you can include them in the search for tests by adding another annotation:

```java
import org.junit.extensions.cpsuite.ClasspathSuite.*;
...
@IncludeJars(true)
public class MySuite...
```

#### <a name="filtering"></a>Filtering

And you don't have to run all tests. Instead you can use another annotation to restrict the tests to run by regex expressions which will be applied onto the class name before adding a test class to the suite:

```java
import org.junit.extensions.cpsuite.ClasspathSuite.*;
...
@ClassnameFilters({"mytests.*", ".*Test"})
public class MySuite...
```

The filter patterns work disjunctively; if _any_ one filter matches, the class will be added to the suite of tests.

#### <a name="negationFilters"></a>Negation Filters

Excluding tests from the test suite can be done by regular expressions but that is very cumbersome to write and read. Therefore I added the feature to have filters that specify a regular expression to _exclude_ certain tests:

```java
@ClassnameFilters({"mytests.*", "!.*AllTests"})
```
Negation expressions are preceded by a "!". In the previous example all tests that match `"mytests.*"` will be run except those matching `".*AllTests"`.

You can have as many positive and negative filters as you like. The positve filters still work disjunctively whereas the negated filters will subtract all matching tests after the maximum set of tests to run has been determined. Having only negated filters starts with the full set of tests.

#### Abstract Test Classes

[ClasspathSuite](http://johanneslink.net/projects/cpsuite.jsp) solves another problem (bug?) in the JUnit 4 integration of Eclipse 3.2: test classes derived from an abstract test class which do not have test methods of their own are being ignored by Eclipse's test runner. When using `RunWith(ClasspathSuite.class)` you will catch those test classes as well.

#### <a name="suitetypes"></a>Running other RunWith-Suites

So far you would only run "normal" test classes. What about including other test suites that use JUnit's RunWith-Feature? Since version&nbsp;0.9.5 ClasspathSuites has another annotation in its bag that helps you around that problem: `@SuiteTypes(...)`. Look at the following example:

```java
import org.junit.extensions.cpsuite.ClasspathSuite.*;
import static org.junit.extensions.cpsuite.SuiteType.*;
...
@RunWith(ClasspathSuite.class)
@SuiteTypes(RUN_WITH_CLASSES)
public class AllRunWithSuites {}
```

This class will execute all test suites in the class path, i.e. classes using the `@RunWith` annotation. Normal test classes will not be run - unless they are in one of the test suites. If you want both test suites and test class being run use the following expression instead : 
		
```java
@SuiteTypes({RUN_WITH_CLASSES, TEST_CLASSES})
```		
Filtering works the same way it does without the `@SuiteTypes` annotation (see [above](#filtering)).

#### <a name="junit38"></a>Running JUnit 3.8 style tests

Since version&nbsp;1.1.0 ClasspathSuite can also be used to run tests in JUnit 3.8 style, using test classes that are derived from `junit.org.TestCase`. This feature is disabled by default and you have to tell ClasspathSuite that you want to consider JUnit 3.8 test cases as well:

```java
import org.junit.extensions.cpsuite.ClasspathSuite.*;
import static org.junit.extensions.cpsuite.SuiteType.*;
...
@RunWith(ClasspathSuite.class)
@SuiteTypes(JUNIT38_TEST_CLASSES)
public class AllJUnit38Tests {}
```

If you want to you can combine JUnit4 and JUnit38 style by specifying more than one suite type, e.g. by using "`@SuiteTypes({ JUNIT38_TEST_CLASSES, TEST_CLASSES })`".  Of course, [filtering](#filtering) works here as well.

#### <a name="baseTypeFilter"></a>Base Type Filter

If you want to restrict the execution of test classes to subclasses of a certain base class, you can do it this way:

```java
@BaseTypeFilter(MyBaseTestClass.class)
```

Use more than one base class if you like:

```java
@BaseTypeFilter({ MyBaseTest.class, YourBaseTest.class })
```

This works for both JUnit 4 and JUnit 3.8 test classes. The filter is _not_ applied on `RunWith` suites, though.

#### <a name="excludeBaseTypeFilter"></a>Exclude Base Type Filter

Similar to `BaseTypeFilter` you can specify one or more base classes whose children will be _excluded_ from a test run:

```java
@ExcludeBaseTypeFilter(MyBaseTestClass.class)
```

Exclude more than one base class if you like:

```java
@ExcludeBaseTypeFilter({ MyExBase.class, YourExbase.class })
```

`ExcludeBaseTypeFilter` takes precedence over `BaseTypeFilter`. The filter works for both JUnit 4 and JUnit 3.8 test classes, but it is _not_ applied on `RunWith` suites.

#### <a name="classpathProperty"></a>Classpath Property Annotation

If you want to use ClasspathSuite via Ant, you sometimes have the need to search tests in a different classpath than the standard one:

```java
@ClasspathProperty("my.class.path")
```

By default, the classpath that can be retrieved via `System.getProperty("java.class.path")` will be searched. Using this annotation allows you to point ClasspathSuite at the directories and jar files of your choice. 

In case the given property is not set, `java.class.path` is used as fallback value. This allows you to use the same annotated suite in both Ant and non-Ant context. 

#### <a name="beforeSuite"></a>BeforeSuite

You can annotate one or more methods with `@BeforeSuite` to indicate that they should be run _once_ before the full suite will be run. 

```java
@BeforeSuite
public static void init() {...}
```		

The signature must be `public static void methodName()`. An exception during the execution of an annotated method will stop the execution of the suite. The order in which those methods are run is not specified.

### <a name="issues"></a>Open Issues

*   ClasspathSuite does currently not work with Plugin-Tests (PDE Test). I should be looking into ways to resolve this, but currently I am not.
*   _Before_ version 1.2.0 beta you could not use `RUN_WITH_CLASSES` to collect other suites that use `@RunWith(ClasspathSuite.class)`. Now this is also possible.

### <a name="changes"></a>Release Notes

#### Version 1.2.6 <span class="date">2013-01-15</span>

*   Changed license to Apache License, Version 2.0
*   Tested with JUnit 4.11
*   Compiled with Java 6

#### Version 1.2.5 <span class="date">2009-06-25</span>

*   Some class instantiation problems in connection with GWT are being handled now.

#### Version 1.2.4 <span class="date">2009-06-03</span>

*   Added [@BeforeSuite](#beforeSuite)
*   Now the jars should _really_ be 1.5 compatible. I had messed it up in 1.2.3.

#### Version 1.2.3 <span class="date">2009-05-29</span>

*   Added [ExcludeBaseTypeFilter](#excludeBaseTypeFilter).
*   [BaseTypeFilter](#baseTypeFilter) accepts several classes now.
*   Tested with JUnit 4.6.
*   Changed source compatibility mode to 1.5.

#### Version 1.2.2 <span class="date">2009-03-07</span>

*   Added a fallback value to  the [ClasspathProperty annotation](#classpathProperty).

#### Version 1.2.1 <span class="date">2009-03-04</span>

*   Added [ClasspathProperty annotation](#classpathProperty).

#### Version 1.2.0 beta <span class="date">2009-03-02</span>

*   Changed internally to use JUnit 4.5 and its new runner builder mechanism.				It's _not working with JUnit 4.4 and below_ any more.
*   Added [BaseTypeFilter](#baseTypeFilter).
*   You can now run suites that use ClasspathSuite from other suites of the same kind.

#### Version 1.1.0 <span class="date">2008-01-18</span>

*   Added support for [JUnit 3.8 style](#junit38) tests. Thanks to Jeanne Boyarsky for suggesting				the feature and providing implementation hints.
*   Added support for JUnit 4.4.1. Thanks to Jeanne Boyarsky for providing the code fix.

#### Version 1.0.0 <span class="date">2007-05-17</span>

*   Added [negation filters](#negationFilters) which
				were suggested by Jason Hitt.
*   Additionally tested with JUnit 4.3.1.

#### Version 0.9.6 <span class="date">2007-02-01</span>

*   Fixed a bug that occurred when an empty directory in the classpath
				contained itself another empty directory. Thanx to David Saff who
				pointed that out and sent a test revealing the bug.

#### Version 0.9.5 <span class="date">2006-12-21</span>

*   Added the functionality to run other tests suites that
			use the `RunWith` attribute as well.			See [`@SuiteTypes`](#suitetypes).
*   Tested with JUnit 4.1 and 4.2.
*   Added more documentation to this website.

#### Version 0.9.1 <span class="date">2006-11-15</span>

*   Added GPL license information to all files.

Originally taken from <http://johanneslink.net/projects/cpsuite.jsp>
