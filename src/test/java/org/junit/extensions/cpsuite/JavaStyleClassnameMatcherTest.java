package org.junit.extensions.cpsuite;

import static org.junit.Assert.*;

import org.junit.Test;

public final class JavaStyleClassnameMatcherTest {
	@Test
	public void testSingleWildcard() {
		JavaStyleClassnameMatcher matcher = new JavaStyleClassnameMatcher(
				"com.example.*Test");
		assertTrue(matcher.matches("com.example.FooTest"));
		assertFalse(matcher.matches("com.example.Foo"));
		assertFalse(matcher.matches("com.example.foo.FooTest"));
	}

	@Test
	public void testDoubleWildcard() {
		JavaStyleClassnameMatcher matcher = new JavaStyleClassnameMatcher(
				"com.example.**Test");
		assertTrue(matcher.matches("com.example.FooTest"));
		assertFalse(matcher.matches("com.example.Foo"));
		assertTrue(matcher.matches("com.example.foo.FooTest"));
	}

}
