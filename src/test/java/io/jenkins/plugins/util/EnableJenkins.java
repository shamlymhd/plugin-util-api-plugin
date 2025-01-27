package io.jenkins.plugins.util;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * JUnit 5 meta annotation providing {@link org.jvnet.hudson.test.JenkinsRule JenkinsRule} integration.
 *
 * <p>
 * Test methods using the rule extension need to accept it by {@link org.jvnet.hudson.test.JenkinsRule JenkinsRule}
 * parameter; each test case gets a new rule object.
 * </p>
 * Annotating a <em>class</em> provides access for all of its tests. Unrelated test cases can omit the parameter.
 *
 * <blockquote><pre>
 * &#64;EnableJenkins
 * class ExampleJUnit5Test {
 *     &#64;Test
 *     public void example(JenkinsRule r) {
 *         // use 'r' ...
 *     }
 *
 *     &#64;Test
 *     public void exampleNotUsingRule() {
 *         // ...
 *     }
 * }
 * </pre></blockquote>
 * <p>
 * Annotating a <i>method</i> limits access to the method.
 * </p>
 * <blockquote><pre>
 * class ExampleJUnit5Test {
 *
 *     &#64;EnableJenkins
 *     &#64;Test
 *     public void example(JenkinsRule r) {
 *         // use 'r' ...
 *     }
 * }
 * </pre></blockquote>
 *
 * @see JenkinsExtension
 * @see org.junit.jupiter.api.extension.ExtendWith
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ExtendWith(JenkinsExtension.class)
public @interface EnableJenkins {
}
