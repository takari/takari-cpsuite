/*
 * @author Johannes Link (business@johanneslink.net)
 * 
 * Published under Apache License, Version 2.0 (http://apache.org/licenses/LICENSE-2.0)
 */
package org.junit.extensions.cpsuite;

public class ClasspathFinderFactory implements ClassesFinderFactory
{
	public ClassesFinder create(boolean searchInJars, String[] filterPatterns, String[] classpathFilterPatterns, SuiteType[] suiteTypes, Class<?>[] baseTypes,
			Class<?>[] excludedBaseTypes, String classpathProperty, boolean excludeDuplicated) {
		ClassTester tester = new ClasspathSuiteTester(searchInJars, filterPatterns, classpathFilterPatterns, suiteTypes, baseTypes, excludedBaseTypes);
		return new ClasspathClassesFinder(tester, classpathProperty, excludeDuplicated);
	}

}
