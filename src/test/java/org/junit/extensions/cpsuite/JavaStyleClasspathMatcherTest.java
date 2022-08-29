package org.junit.extensions.cpsuite;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class JavaStyleClasspathMatcherTest {
	@Test
	public void testSingleWildcard() {
		JavaStyleClasspathMatcher matcher = new JavaStyleClasspathMatcher(
				"project/module/*Test");
		assertTrue(matcher.matches("project/module/FooTest"));
		assertFalse(matcher.matches("project/module/Foo"));
		assertFalse(matcher.matches("project/module/foo/FooTest"));
	}

	@Test
	public void testDoubleWildcard() {
		JavaStyleClasspathMatcher matcher = new JavaStyleClasspathMatcher(
				"project/module/**Test");
		assertTrue(matcher.matches("project/module/FooTest"));
		assertFalse(matcher.matches("project/module/Foo"));
		assertTrue(matcher.matches("project/module/foo/FooTest"));
	}
}
