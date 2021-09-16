/*
 * @author Johannes Link (business@johanneslink.net)
 * 
 * Published under Apache License, Version 2.0 (http://apache.org/licenses/LICENSE-2.0)
 */
package org.junit.extensions.cpsuite;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.jar.*;

/**
 * Utility class to find classes within the class path, both inside and outside
 * of jar files. Inner and anonymous classes are not being considered in the
 * first place.
 * 
 * It's originally evolved out of ClassPathTestCollector in JUnit 3.8.1
 */
public class ClasspathClassesFinder implements ClassesFinder {

	private static final int CLASS_SUFFIX_LENGTH = ".class".length();
	private static final String FALLBACK_CLASSPATH_PROPERTY = "java.class.path";

	private final ClassTester tester;

	private final String classpathProperty;

	public ClasspathClassesFinder(ClassTester tester, String classpathProperty) {
		this.tester = tester;
		this.classpathProperty = classpathProperty;
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

	private List<Class<?>> findClassesInClasspath(final String classPath) {
    	List<String> classpathRoots = splitClassPath(classPath);
    	if (this.tester.useClasspathFromJars()) {
      		classpathRoots = findAndAddClasspathFromJars(classpathRoots);
    	}
    	return findClassesInRoots(classpathRoots);
	}

	private List<String> findAndAddClasspathFromJars(final List<String> classpathRoots)
	{
		List<String> allRoots = new ArrayList<>(100);
		allRoots.addAll(classpathRoots);
  		
		for (String root : classpathRoots) {
			File classpathFile = new File(root);
			if (isJarFile(classpathFile)) {
				Manifest manifest = null;
				try (URLClassLoader cl = new URLClassLoader(new URL[] { classpathFile.toURI().toURL() })) {
					URL url = cl.findResource("META-INF/MANIFEST.MF");
					if (url != null) {
						try (InputStream inputStream = url.openStream()) {
							manifest = new Manifest(inputStream);
						}
					}
				} catch (IOException e) {
					// ignore errors and just continue (manifest will be null, so this entry is just ignored)
				}
				if (manifest != null) {
					Attributes attr = manifest.getMainAttributes();
					String classPath = attr.getValue("Class-Path");
					if (classPath != null) {
						Collection<? extends String> segments = splitClassPathFromManifest(classPath);
						for (String segment : segments) {
							File relativeFile = new File(classpathFile.getParentFile(), segment);
							try	{
								allRoots.add(relativeFile.getCanonicalPath());
							} catch (IOException e)	{
								// just ignore the failure
							}
						}
					}
				}
			}
		}
		return allRoots;
	}

	private Collection<? extends String> splitClassPathFromManifest(final String classPath) {
		return Arrays.asList(classPath.split(" "));
	}
	
	private List<Class<?>> findClassesInRoots(List<String> roots) {
		List<Class<?>> classes = new ArrayList<Class<?>>(100);
		for (String root : roots) {
			gatherClassesInRoot(new File(root), classes);
		}
		return classes;
	}

	private void gatherClassesInRoot(File classRoot, List<Class<?>> classes) {
		Iterable<String> relativeFilenames = new NullIterator<String>();
		if (tester.searchInJars() && isJarFile(classRoot)) {
			try {
				relativeFilenames = new JarFilenameIterator(classRoot);
			} catch (IOException e) {
				// Don't iterate unavailable ja files
				e.printStackTrace();
			}
		} else if (classRoot.isDirectory()) {
			relativeFilenames = new RecursiveFilenameIterator(classRoot);
		}
		gatherClasses(classes, relativeFilenames);
	}

	private boolean isJarFile(File classRoot) {
		return classRoot.getName().endsWith(".jar") || classRoot.getName().endsWith(".JAR");
	}

	private void gatherClasses(List<Class<?>> classes, Iterable<String> filenamesIterator) {
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
				if (clazz == null || clazz.isLocalClass() || clazz.isAnonymousClass()) {
					continue;
				}
				if (tester.acceptClass(clazz)) {
					classes.add(clazz);
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
