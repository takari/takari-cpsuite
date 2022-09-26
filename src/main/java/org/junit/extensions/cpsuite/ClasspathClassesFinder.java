/*
 * @author Johannes Link (business@johanneslink.net)
 * 
 * Published under Apache License, Version 2.0 (http://apache.org/licenses/LICENSE-2.0)
 */
package org.junit.extensions.cpsuite;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Utility class to find classes within the class path, both inside and outside
 * of jar files. Inner and anonymous classes are not being considered in the
 * first place.
 * 
 * It's originally evolved out of ClassPathTestCollector in JUnit 3.8.1
 */
public class ClasspathClassesFinder implements ClassesFinder
{

	private static final int CLASS_SUFFIX_LENGTH = ".class".length();
	private static final String FALLBACK_CLASSPATH_PROPERTY = "java.class.path";

	private final ClassTester tester;

	private final String classpathProperty;

	private final boolean excludeDuplicated;

	public ClasspathClassesFinder(ClassTester tester, String classpathProperty, boolean excludeDuplicated) {
		this.tester = tester;
		this.classpathProperty = classpathProperty;
		this.excludeDuplicated = excludeDuplicated;
	}

	public List<Class<?>> find() {
		return findClassesInClasspath(getClasspath());
	}

	private String getClasspath() {
		String classPath = System.getProperty(getClasspathProperty());
		if (classPath == null)
			classPath = System.getProperty(FALLBACK_CLASSPATH_PROPERTY);
		return classPath;
	}

	private List<Class<?>> findClassesInClasspath(String classPath) {
		return findClassesInRoots(splitClassPath(classPath));
	}

	private List<Class<?>> findClassesInRoots(List<String> roots) {
		List<Class<?>> classes = new ArrayList<Class<?>>(100);
		Set<String> classNames = null; // by default no hashmap for efficiency
		if (excludeDuplicated) classNames = new HashSet<String>(100);
		for (String root : roots) {
			gatherClassesInRoot(new File(root), classes, classNames);
		}
		return classes;
	}

	private void gatherClassesInRoot(File classRoot, List<Class<?>> classes, Set<String> classNames) {
		Iterable<String> relativeFilenames = new NullIterator<String>();
        if (tester.acceptClassRoot(classRoot.getAbsolutePath())) {
            if (tester.searchInJars() && isJarFile(classRoot)) {
                try {
                    relativeFilenames = new JarFilenameIterator(classRoot);
                } catch (IOException e) {
                    // Don't iterate unavailable ja files
                    e.printStackTrace();
                }
            }
            else if (classRoot.isDirectory()) {
                relativeFilenames = new RecursiveFilenameIterator(classRoot);
            }
        }
		gatherClasses(classes, relativeFilenames, classNames);
	}

	private boolean isJarFile(File classRoot) {
		return classRoot.getName().endsWith(".jar") || classRoot.getName().endsWith(".JAR");
	}

	private void gatherClasses(List<Class<?>> classes, Iterable<String> filenamesIterator, Set<String> classNames) {
		for (String fileName : filenamesIterator) {
			if (!isClassFile(fileName)) {
				continue;
			}
			String className = classNameFromFile(fileName);
			if (!tester.acceptClassName(className)) {
				continue;
			}
			if (!tester.acceptInnerClass() && isInnerClass(className)) {
				continue;
			}
			try {
				Class<?> clazz = Class.forName(className, false, getClass().getClassLoader());
				if (clazz == null || clazz.isLocalClass() || clazz.isAnonymousClass() || hasDuplicatedClassName(classNames, clazz)) {
					continue;
				}
				if (tester.acceptClass(clazz)) {
					classes.add(clazz);
					addClassName(classNames, clazz.getName());
				}
			} catch (ClassNotFoundException cnfe) {
				// ignore not instantiable classes
			} catch (NoClassDefFoundError ncdfe) {
				// ignore not instantiable classes
			} catch (ExceptionInInitializerError ciie) {
				// ignore not instantiable classes
			} catch (UnsatisfiedLinkError ule) {
				// ignore not instantiable classes
			}
		}
	}

	private void addClassName(Set<String> classNames, String className) {
		if (classNames != null) { // only check if hashmap initialized
			classNames.add(className);
		}
	}

	private boolean hasDuplicatedClassName(Set<String> classNames, Class<?> clazz) {
		return classNames != null && classNames.contains(clazz.getName());
	}

	private boolean isInnerClass(String className) {
		return className.contains("$");
	}

	private boolean isClassFile(String classFileName) {
		return classFileName.endsWith(".class");
	}

	private List<String> splitClassPath(String classPath) {
		final String separator = System.getProperty("path.separator");
		return Arrays.asList(classPath.split(separator));
	}

	private String classNameFromFile(String classFileName) {
		// convert /a/b.class to a.b
		String s = replaceFileSeparators(cutOffExtension(classFileName));
		if (s.startsWith("."))
			return s.substring(1);
		return s;
	}

	private String replaceFileSeparators(String s) {
		String result = s.replace(File.separatorChar, '.');
		if (File.separatorChar != '/') {
			// In Jar-Files it's always '/'
			result = result.replace('/', '.');
		}
		return result;
	}

	private String cutOffExtension(String classFileName) {
		return classFileName.substring(0, classFileName.length() - CLASS_SUFFIX_LENGTH);
	}

	public ClassTester getTester() {
		return tester;
	}

	public String getClasspathProperty() {
		return classpathProperty;
	}

}
