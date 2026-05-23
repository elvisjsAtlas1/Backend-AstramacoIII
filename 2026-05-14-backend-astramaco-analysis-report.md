# Code analysis
## backend-astramaco 
#### Version 0.0.1-SNAPSHOT 

**By: default**

*Date: 2026-05-14*

## Introduction
This document contains results of the code analysis of backend-astramaco

Backend Astramaco

## Configuration

- Quality Profiles
    - Names: Sonar way [Java]; Sonar way [XML]; 
    - Files: AZ4pIL494_yLgpjFZ88w.json; AZ4pIL8-4_yLgpjFZ9Iu.json; 


 - Quality Gate
    - Name: Sonar way
    - File: Sonar way.xml

## Synthesis

### Analysis Status

Reliability | Security | Security Review | Maintainability |
:---:|:---:|:---:|:---:
A | A | E | A |

### Quality gate status

| Quality Gate Status | OK |
|-|-|

Metric|Value
---|---
Reliability Rating on New Code|OK
Security Rating on New Code|OK
Maintainability Rating on New Code|OK


### Metrics

Coverage | Duplications | Comment density | Median number of lines of code per file | Adherence to coding standard |
:---:|:---:|:---:|:---:|:---:
0.0 % | 1.7 % | 1.2 % | 28.0 | 99.8 %

### Tests

Total | Success Rate | Skipped | Errors | Failures |
:---:|:---:|:---:|:---:|:---:
43 | 100.0 % | 0 | 0 | 0

### Detailed technical debt

Reliability|Security|Maintainability|Total
---|---|---|---
-|-|0d 0h 36min|0d 0h 36min


### Metrics Range

\ | Cyclomatic Complexity | Cognitive Complexity | Lines of code per file | Coverage | Comment density (%) | Duplication (%)
:---|:---:|:---:|:---:|:---:|:---:|:---:
Min | 0.0 | 0.0 | 5.0 | 0.0 | 0.0 | 0.0
Max | 101.0 | 40.0 | 1189.0 | 0.0 | 7.4 | 55.6

### Volume

Language|Number
---|---
Java|1189
XML|163
Total|1352


## Issues

### Issues count by severity and types

Type / Severity|INFO|MINOR|MAJOR|CRITICAL|BLOCKER
---|---|---|---|---|---
BUG|0|0|0|0|0
VULNERABILITY|0|0|0|0|0
CODE_SMELL|0|0|4|0|2


### Issues List

Name|Description|Type|Severity|Number
---|---|---|---|---
Tests should include assertions|A test case without assertions ensures only that no exceptions are thrown. Beyond basic runnability, it ensures nothing about the behavior of the <br /> code under test. <br /> This rule raises an exception when no assertions from any of the following known frameworks are found in a test: <br />  <br />    AssertJ  <br />    Awaitility  <br />    EasyMock  <br />    Eclipse Vert.x  <br />    Fest 1.x and 2.x  <br />    Hamcrest  <br />    JMock  <br />    JMockit  <br />    JUnit  <br />    Mockito  <br />    Rest-assured 2.x, 3.x and 4.x  <br />    RxJava 1.x and 2.x  <br />    Selenide  <br />    Spring’s org.springframework.test.web.servlet.ResultActions.andExpect() and <br />   org.springframework.test.web.servlet.ResultActions.andExpectAll()  <br />    Truth Framework  <br />    WireMock  <br />  <br /> Furthermore, as new or custom assertion frameworks may be used, the rule can be parametrized to define specific methods that will also be <br /> considered as assertions. No issue will be raised when such methods are found in test cases. The parameter value should have the following format <br /> &lt;FullyQualifiedClassName&gt;#&lt;MethodName&gt;, where MethodName can end with the wildcard character. For constructors, <br /> the pattern should be &lt;FullyQualifiedClassName&gt;#&lt;init&gt;. <br /> Example:&nbsp;com.company.CompareToTester#compare*,com.company.CustomAssert#customAssertMethod,com.company.CheckVerifier#&lt;init&gt;. <br /> Noncompliant Code Example <br />  <br /> @Test <br /> public void testDoSomething() {  // Noncompliant <br />   MyClass myClass = new MyClass(); <br />   myClass.doSomething(); <br /> } <br />  <br /> Compliant Solution <br /> Example when com.company.CompareToTester#compare* is used as parameter to the rule. <br />  <br /> import com.company.CompareToTester; <br />  <br /> @Test <br /> public void testDoSomething() { <br />   MyClass myClass = new MyClass(); <br />   assertNull(myClass.doSomething());  // JUnit assertion <br />   assertThat(myClass.doSomething()).isNull();  // Fest assertion <br /> } <br />  <br /> @Test <br /> public void testDoSomethingElse() { <br />   MyClass myClass = new MyClass(); <br />   new CompareToTester().compareWith(myClass);  // Compliant - custom assertion method defined as rule parameter <br />   CompareToTester.compareStatic(myClass);  // Compliant <br /> } <br /> |CODE_SMELL|BLOCKER|2
JUnit assertTrue/assertFalse should be simplified to the corresponding dedicated assertion|Testing equality or nullness with JUnit’s assertTrue() or assertFalse() should be simplified to the corresponding <br /> dedicated assertion. <br /> Noncompliant Code Example <br />  <br /> Assert.assertTrue(a.equals(b)); <br /> Assert.assertTrue(a == b); <br /> Assert.assertTrue(a == null); <br /> Assert.assertTrue(a != null); <br /> Assert.assertFalse(a.equals(b)); <br />  <br /> Compliant Solution <br />  <br /> Assert.assertEquals(a, b); <br /> Assert.assertSame(a, b); <br /> Assert.assertNull(a); <br /> Assert.assertNotNull(a); <br /> Assert.assertNotEquals(a, b); <br /> |CODE_SMELL|MAJOR|3
Similar tests should be grouped in a single Parameterized test|When multiple tests differ only by a few hardcoded values they should be refactored as a single "parameterized" test. This reduces the chances of <br /> adding a bug and makes them more readable. Parameterized tests exist in most test frameworks (JUnit, TestNG, etc…?). <br /> The right balance needs of course to be found. There is no point in factorizing test methods when the parameterized version is a lot more complex <br /> than initial tests. <br /> This rule raises an issue when at least 3 tests could be refactored as one parameterized test with less than 4 parameters. Only test methods which <br /> have at least one duplicated statement are considered. <br /> Noncompliant Code Example <br /> with JUnit 5 <br />  <br /> import static org.junit.jupiter.api.Assertions.assertEquals; <br />  <br /> import org.junit.jupiter.api.Test; <br />  <br /> public class AppTest <br /> { <br />     @Test <br />     void test_not_null1() {  // Noncompliant. The 3 following tests differ only by one hardcoded number. <br />       setupTax(); <br />       assertNotNull(getTax(1)); <br />     } <br />  <br />     @Test <br />     void test_not_null2() { <br />       setupTax(); <br />       assertNotNull(getTax(2)); <br />     } <br />  <br />     @Test <br />     void test_not_nul3l() { <br />       setupTax(); <br />       assertNotNull(getTax(3)); <br />     } <br />  <br />     @Test <br />     void testLevel1() {  // Noncompliant. The 3 following tests differ only by a few hardcoded numbers. <br />         setLevel(1); <br />         runGame(); <br />         assertEquals(playerHealth(), 100); <br />     } <br />  <br />     @Test <br />     void testLevel2() {  // Similar test <br />         setLevel(2); <br />         runGame(); <br />         assertEquals(playerHealth(), 200); <br />     } <br />  <br />     @Test <br />     void testLevel3() {  // Similar test <br />         setLevel(3); <br />         runGame(); <br />         assertEquals(playerHealth(), 300); <br />     } <br /> } <br />  <br /> Compliant Solution <br />  <br /> import static org.junit.jupiter.api.Assertions.assertEquals; <br />  <br /> import org.junit.jupiter.params.ParameterizedTest; <br /> import org.junit.jupiter.params.provider.CsvSource; <br />  <br /> public class AppTest <br /> { <br />  <br />    @ParameterizedTest <br />    @ValueSource(ints = {1, 2, 3}) <br />    void test_not_null(int arg) { <br />      setupTax(); <br />      assertNotNull(getTax(arg)); <br />    } <br />  <br />     @ParameterizedTest <br />     @CsvSource({ <br />         "1, 100", <br />         "2, 200", <br />         "3, 300", <br />     }) <br />     void testLevels(int level, int health) { <br />         setLevel(level); <br />         runGame(); <br />         assertEquals(playerHealth(), health); <br />     } <br /> } <br />  <br /> See <br />  <br />    Modern Best Practices for Testing in Java - <br />   Philipp Hauer  <br />    JUnit 5 documentation - Parameterized tests <br />    <br />    Writing Parameterized Tests With JUnit 4  <br />    TestNG documentation - Parameters  <br /> |CODE_SMELL|MAJOR|1


## Security Hotspots

### Security hotspots count by category and priority

Category / Priority|LOW|MEDIUM|HIGH
---|---|---|---
LDAP Injection|0|0|0
Object Injection|0|0|0
Server-Side Request Forgery (SSRF)|0|0|0
XML External Entity (XXE)|0|0|0
Insecure Configuration|2|0|0
XPath Injection|0|0|0
Authentication|0|0|0
Weak Cryptography|0|0|0
Denial of Service (DoS)|0|0|0
Log Injection|0|0|0
Cross-Site Request Forgery (CSRF)|0|0|1
Open Redirect|0|0|0
Permission|0|0|0
SQL Injection|0|0|0
Encryption of Sensitive Data|0|0|0
Traceability|0|0|0
Buffer Overflow|0|0|0
File Manipulation|0|0|0
Code Injection (RCE)|0|0|0
Cross-Site Scripting (XSS)|0|0|0
Command Injection|0|0|0
Path Traversal Injection|0|0|0
HTTP Response Splitting|0|0|0
Others|0|0|0


### Security hotspots

Category|Name|Priority|Severity|Count
---|---|---|---|---
Insecure Configuration|Having a permissive Cross-Origin Resource Sharing policy is security-sensitive|LOW|MINOR|2
Cross-Site Request Forgery (CSRF)|Disabling CSRF protections is security-sensitive|HIGH|CRITICAL|1
