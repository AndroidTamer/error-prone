/*
 * Copyright 2013 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.errorprone.matchers;

import static com.google.errorprone.matchers.ChildMultiMatcher.MatchType.ANY;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.annotations;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.hasAnnotation;
import static com.google.errorprone.matchers.Matchers.hasAnnotationOnAnyOverriddenMethod;
import static com.google.errorprone.matchers.Matchers.hasArgumentWithValue;
import static com.google.errorprone.matchers.Matchers.hasMethod;
import static com.google.errorprone.matchers.Matchers.isSubtypeOf;
import static com.google.errorprone.matchers.Matchers.methodHasParameters;
import static com.google.errorprone.matchers.Matchers.methodHasVisibility;
import static com.google.errorprone.matchers.Matchers.methodIsNamed;
import static com.google.errorprone.matchers.Matchers.methodNameStartsWith;
import static com.google.errorprone.matchers.Matchers.methodReturns;
import static com.google.errorprone.matchers.Matchers.nestingKind;
import static com.google.errorprone.matchers.Matchers.not;
import static com.google.errorprone.suppliers.Suppliers.VOID_TYPE;
import static javax.lang.model.element.NestingKind.TOP_LEVEL;

import com.google.errorprone.VisitorState;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ClassType;
import com.sun.tools.javac.tree.JCTree;

import java.util.Arrays;
import java.util.Collection;

import javax.lang.model.element.Modifier;

/**
 * Matchers for code patterns which appear to be JUnit-based tests.
 * @author alexeagle@google.com (Alex Eagle)
 * @author eaftan@google.com (Eddie Aftandillian)
 */
public class JUnitMatchers {
  public static final String JUNIT4_TEST_ANNOTATION = "org.junit.Test";
  public static final String JUNIT_BEFORE_ANNOTATION = "org.junit.Before";
  public static final String JUNIT_AFTER_ANNOTATION = "org.junit.After";
  public static final String JUNIT_BEFORE_CLASS_ANNOTATION = "org.junit.BeforeClass";
  public static final String JUNIT_AFTER_CLASS_ANNOTATION = "org.junit.AfterClass";
  private static final String JUNIT3_TEST_CASE_CLASS = "junit.framework.TestCase";
  private static final String JUNIT4_RUN_WITH_ANNOTATION = "org.junit.runner.RunWith";
  private static final String JUNIT4_IGNORE_ANNOTATION = "org.junit.Ignore";


  public static final Matcher<MethodTree> hasJUnitAnnotation = anyOf(
      /* @Test, @Before, and @After are inherited by methods that override a base method with the
       * annotation.  @BeforeClass and @AfterClass can only be applied to static methods, so they
       * cannot be inherited. */
      hasAnnotationOnAnyOverriddenMethod(JUNIT4_TEST_ANNOTATION),
      hasAnnotationOnAnyOverriddenMethod(JUNIT_BEFORE_ANNOTATION),
      hasAnnotationOnAnyOverriddenMethod(JUNIT_AFTER_ANNOTATION),
      hasAnnotation(JUNIT_BEFORE_CLASS_ANNOTATION),
      hasAnnotation(JUNIT_AFTER_CLASS_ANNOTATION));

  public static final Matcher<MethodTree> hasJUnit4BeforeAnnotations = anyOf(
      hasAnnotationOnAnyOverriddenMethod(JUNIT_BEFORE_ANNOTATION),
      hasAnnotation(JUNIT_BEFORE_CLASS_ANNOTATION));

  public static final Matcher<MethodTree> hasJUnit4AfterAnnotations = anyOf(
      hasAnnotationOnAnyOverriddenMethod(JUNIT_AFTER_ANNOTATION),
      hasAnnotation(JUNIT_AFTER_CLASS_ANNOTATION));

  /**
   * Matches a class that inherits from TestCase.
   */
  public static final Matcher<ClassTree> isTestCaseDescendant =
      isSubtypeOf(JUNIT3_TEST_CASE_CLASS);

  /**
   * Match a class which appears to be missing a @RunWith annotation.
   *
   * Matches if:
   * 1) The class does not have a JUnit 4 @RunWith annotation.
   * 2) The class is concrete.
   * 3) The class is a top-level class.
   */
  public static final Matcher<ClassTree> isConcreteClassWithoutRunWith = allOf(
      not(hasAnnotation(JUNIT4_RUN_WITH_ANNOTATION)),
      not(Matchers.<ClassTree>hasModifier(Modifier.ABSTRACT)),
      nestingKind(TOP_LEVEL));

  /**
   * Match a class which has one or more methods with a JUnit 4 @Test annotation.
   */
  public static final Matcher<ClassTree> hasJUnit4TestCases =
      hasMethod(hasAnnotationOnAnyOverriddenMethod(JUNIT4_TEST_ANNOTATION));

  /**
   * Match a class which appears to be a JUnit 3 test class.
   *
   * Matches if:
   * 1) The class does inherit from TestCase.
   * 2) The class does not have a JUnit 4 @RunWith annotation.
   * 3) The class is concrete.
   * 4) This class is a top-level class.
   */
  public static final Matcher<ClassTree> isJUnit3TestClass = allOf(
      isTestCaseDescendant,
      isConcreteClassWithoutRunWith);

  /**
   * Match a method which appears to be a JUnit 3 test case.
   *
   * Matches if:
   * 1) The method's name begins with "test".
   * 2) The method has no parameters.
   * 3) The method is public.
   * 4) The method returns void
   */
  public static final Matcher<MethodTree> isJunit3TestCase = allOf(
      methodNameStartsWith("test"),
      methodHasParameters(),
      Matchers.<MethodTree>hasModifier(Modifier.PUBLIC),
      methodReturns(VOID_TYPE)
  );

  /**
   * Match a method which appears to be a JUnit 3 setUp method
   *
   * Matches if:
   * 1) The method is named "setUp"
   * 2) The method has no parameters
   * 3) The method is a public or protected instance method that is not abstract
   * 4) The method returns void
   */
  public static final Matcher<MethodTree> looksLikeJUnit3SetUp = allOf(
      methodIsNamed("setUp"),
      methodHasParameters(),
      anyOf(
          methodHasVisibility(MethodVisibility.Visibility.PUBLIC),
          methodHasVisibility(MethodVisibility.Visibility.PROTECTED)
      ),
      not(Matchers.<MethodTree>hasModifier(Modifier.ABSTRACT)),
      not(Matchers.<MethodTree>hasModifier(Modifier.STATIC)),
      methodReturns(VOID_TYPE)
  );

  /**
   * Match a method which appears to be a JUnit 3 tearDown method
   *
   * Matches if:
   * 1) The method is named "tearDown"
   * 2) The method has no parameters
   * 3) The method is a public or protected instance method that is not abstract
   * 4) The method returns void
   */
  public static final Matcher<MethodTree> looksLikeJUnit3TearDown = allOf(
      methodIsNamed("tearDown"),
      methodHasParameters(),
      anyOf(
          methodHasVisibility(MethodVisibility.Visibility.PUBLIC),
          methodHasVisibility(MethodVisibility.Visibility.PROTECTED)
      ),
      not(Matchers.<MethodTree>hasModifier(Modifier.ABSTRACT)),
      not(Matchers.<MethodTree>hasModifier(Modifier.STATIC)),
      methodReturns(VOID_TYPE)
  );

  /**
   * Matches a method annotated with @Test but not @Ignore.
   */
  public static final Matcher<MethodTree> wouldRunInJUnit4 = allOf(
      hasAnnotationOnAnyOverriddenMethod(JUNIT4_TEST_ANNOTATION),
      not(hasAnnotationOnAnyOverriddenMethod(JUNIT4_IGNORE_ANNOTATION)));

  public static class JUnit4TestClassMatcher implements Matcher<ClassTree> {

    /**
     * A list of test runners that this matcher should look for in the @RunWith annotation.
     * Subclasses of the test runners are also matched.
     */
    private static final Collection<String> TEST_RUNNERS = Arrays.asList(
        "org.mockito.runners.MockitoJUnitRunner",
        "org.junit.runners.BlockJUnit4ClassRunner");

    /**
     * Matches an argument of type Class<T>, where T is a subtype of one of the test runners listed
     * in the TEST_RUNNERS field.
     *
     * TODO(user): Support checking for an annotation that tells us whether this test runner
     * expects tests to be annotated with @Test.
     */
    private static final Matcher<ExpressionTree> isJUnit4TestRunner =
        new Matcher<ExpressionTree>() {
      @Override
      public boolean matches(ExpressionTree t, VisitorState state) {
        Type type = ((JCTree) t).type;
        // Expect a class type.
        if (!(type instanceof ClassType)) {
          return false;
        }
        // Expect one type argument, the type of the JUnit class runner to use.
        com.sun.tools.javac.util.List<Type> typeArgs = ((ClassType) type).getTypeArguments();
        if (typeArgs.size() != 1) {
          return false;
        }
        Type runnerType = typeArgs.get(0);
        for (String testRunner : TEST_RUNNERS) {
          Symbol parent = state.getSymbolFromString(testRunner);
          if (parent == null) {
            continue;
          }
          if (runnerType.tsym.isSubClass(parent, state.getTypes())) {
            return true;
          }
        }
        return false;
      }
    };

    private static final Matcher<ClassTree> isJUnit4TestClass = allOf(
        not(isTestCaseDescendant),
        annotations(ANY, hasArgumentWithValue("value", isJUnit4TestRunner)));

    @Override
    public boolean matches(ClassTree classTree, VisitorState state) {
      return isJUnit4TestClass.matches(classTree, state);
    }
  }
}
