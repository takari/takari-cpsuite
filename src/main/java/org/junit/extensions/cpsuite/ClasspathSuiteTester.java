/*
 * @author Johannes Link (business@johanneslink.net)
 * 
 * Published under Apache License, Version 2.0 (http://apache.org/licenses/LICENSE-2.0)
 */
package org.junit.extensions.cpsuite;

import java.lang.reflect.*;
import java.util.*;

import junit.framework.TestCase;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * ClassTester implementation to retrieve JUnit38 & 4.x test classes in the
 * classpath. You can specify if you want to include jar files in the search and
 * you can give a set of regex expression to specify the class names to include.
 * 
 */
public class ClasspathSuiteTester implements ClassTester {

	private final boolean searchInJars;
	private final SuiteType[] suiteTypes;
	private List<JavaStyleClassnameMatcher> positiveFilters;
	private List<JavaStyleClassnameMatcher> negationFilters;
	private final Class<?>[] baseTypes;
	private final Class<?>[] excludedBaseTypes;
	private final boolean useClasspathFromJars;

	/**
	 * @param searchInJars
	 *            Specify if you want to include jar files in the search
	 * @param filterPatterns
	 *            A set of regex expression to specify the class names to
	 *            include (included if any pattern matches); use null to include
	 *            all test classes in all packages.
	 * @param baseTypes
	 *            TODO
	 * @param types
	 */
	public ClasspathSuiteTester(boolean searchInJars, String[] filterPatterns, SuiteType[] suiteTypes, Class<?>[] baseTypes,
			Class<?>[] excludedBaseTypes, final boolean useClasspathFromJars) {
		this.searchInJars = searchInJars;
		this.positiveFilters = findPositiveFilters(filterPatterns);
		this.negationFilters = findNegationFilters(filterPatterns);
		this.suiteTypes = suiteTypes;
		this.baseTypes = baseTypes;
		this.excludedBaseTypes = excludedBaseTypes;
		this.useClasspathFromJars = useClasspathFromJars;
	}

	public boolean acceptClass(Class<?> clazz) {
		if (isInSuiteTypes(SuiteType.TEST_CLASSES)) {
			if (acceptTestClass(clazz)) {
				return true;
			}

		}
		if (isInSuiteTypes(SuiteType.JUNIT38_TEST_CLASSES)) {
			if (acceptJUnit38Test(clazz)) {
				return true;
			}
		}
		if (isInSuiteTypes(SuiteType.RUN_WITH_CLASSES)) {
			return acceptRunWithClass(clazz);
		}

		return false;
	}

	private boolean acceptJUnit38Test(Class<?> clazz) {
		if (isAbstractClass(clazz)) {
			return false;
		}
		if (hasExcludedBaseType(clazz)) {
			return false;
		}
		if (!hasCorrectBaseType(clazz)) {
			return false;
		}
		return TestCase.class.isAssignableFrom(clazz);
	}

	private boolean acceptRunWithClass(Class<?> clazz) {
		return clazz.isAnnotationPresent(RunWith.class);
	}

	private boolean isInSuiteTypes(SuiteType suiteType) {
		return Arrays.asList(suiteTypes).contains(suiteType);
	}

	private boolean acceptTestClass(Class<?> clazz) {
		if (isAbstractClass(clazz)) {
			return false;
		}
		if (hasExcludedBaseType(clazz)) {
			return false;
		}
		if (!hasCorrectBaseType(clazz)) {
			return false;
		}
		try {
			for (Method method : clazz.getMethods()) {
				if (method.getAnnotation(Test.class) != null) {
					return true;
				}
			}
		} catch (NoClassDefFoundError ignore) {
		}
		return false;
	}

	private boolean hasExcludedBaseType(Class<?> clazz) {
		for (Class<?> excludedBaseType : excludedBaseTypes) {
			if (excludedBaseType.isAssignableFrom(clazz)) {
				return true;
			}
		}
		return false;
	}

	private boolean hasCorrectBaseType(Class<?> clazz) {
		for (Class<?> baseType : baseTypes) {
			if (baseType.isAssignableFrom(clazz)) {
				return true;
			}
		}
		return false;
	}

	private boolean isAbstractClass(Class<?> clazz) {
		return (clazz.getModifiers() & Modifier.ABSTRACT) != 0;
	}

	public boolean acceptClassName(String className) {
		if (!acceptInPositiveFilers(className)) {
			return false;
		}
		return acceptInNegationFilters(className);
	}

	private boolean acceptInNegationFilters(String className) {
		for (JavaStyleClassnameMatcher pattern : negationFilters) {
			if (pattern.matches(className)) {
				return false;
			}
		}
		return true;
	}

	private boolean acceptInPositiveFilers(String className) {
		boolean isPositiveAccepted = positiveFilters.isEmpty();
		for (JavaStyleClassnameMatcher pattern : positiveFilters) {
			if (pattern.matches(className)) {
				isPositiveAccepted = true;
				break;
			} else {
				isPositiveAccepted = false;
			}
		}
		return isPositiveAccepted;
	}

	private List<JavaStyleClassnameMatcher> findPositiveFilters(String[] filterPatterns) {
		List<JavaStyleClassnameMatcher> filters = new ArrayList<JavaStyleClassnameMatcher>();
		if (filterPatterns != null) {
			for (String pattern : filterPatterns) {
				if (!pattern.startsWith("!")) {
					filters.add(new JavaStyleClassnameMatcher(pattern));
				}
			}
		}
		return filters;
	}

	private List<JavaStyleClassnameMatcher> findNegationFilters(String[] filterPatterns) {
		List<JavaStyleClassnameMatcher> filters = new ArrayList<JavaStyleClassnameMatcher>();
		for (String pattern : filterPatterns) {
			if (pattern.startsWith("!")) {
				filters.add(new JavaStyleClassnameMatcher(pattern.substring(1)));
			}
		}
		return filters;
	}

	public boolean acceptInnerClass() {
		return true;
	}

	public boolean searchInJars() {
		return searchInJars;
	}

	public List<JavaStyleClassnameMatcher> getPositiveClassnameFilters() {
		return positiveFilters;
	}

	public List<JavaStyleClassnameMatcher> getNegationClassnameFilters() {
		return negationFilters;
	}

	public SuiteType[] getSuiteTypes() {
		return suiteTypes;
	}

	public Class<?>[] getBaseTypes() {
		return baseTypes;
	}

	public Class<?>[] getExcludedBaseTypes() {
		return excludedBaseTypes;
	}

	@Override
  	public boolean useClasspathFromJars() {
		return this.useClasspathFromJars;
  	}
}
