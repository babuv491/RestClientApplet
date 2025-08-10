package com.rct.util;

import com.rct.manager.CollectionManager;
import java.util.Map;

public class TestGenerator {

 public static String generateRestAssuredTest(CollectionManager.SavedRequest request) {
 StringBuilder test = new StringBuilder();

 // Package and imports
 test.append("package com.api.tests;\n\n");
 test.append("import io.restassured.RestAssured;\n");
 test.append("import io.restassured.response.Response;\n");
 test.append("import org.testng.annotations.*;\n");
 test.append("import org.testng.Assert;\n");
 test.append("import static io.restassured.RestAssured.*;\n");
 test.append("import static org.hamcrest.Matchers.*;\n\n");

 // Class name based on request name
 String className = sanitizeClassName(request.getName()) + "Test";
 test.append("public class ").append(className).append(" {\n\n");

 // Setup method
 String baseUri = extractBaseUri(request.getUrl());
 test.append(" @BeforeMethod\n");
 test.append(" public void setUp() {\n");
  test.append(" RestAssured.baseURI = \"").append(baseUri).append("\";\n");
 test.append(" RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();\n");
 test.append(" }\n\n");

 // Generate all test methods
 generatePositiveTest(test, request);
 generateNegativeTests(test, request);
 generatePerformanceTest(test, request);
 generateSecurityTests(test, request);
 generateDataValidationTests(test, request);

 test.append("}\n");
 return test.toString();
 }

 private static String sanitizeClassName(String name) {
 if (name == null || name.trim().isEmpty()) {
 return "ApiTest";
 }
 // Remove special characters and make it a valid Java class name
 String sanitized = name.replaceAll("[^a-zA-Z0-9]", "");
 if (sanitized.isEmpty()) {
 return "ApiTest";
 }
 // Ensure it starts with uppercase
 return Character.toUpperCase(sanitized.charAt(0)) + sanitized.substring(1);
 }

 private static String sanitizeMethodName(String name) {
 if (name == null || name.trim().isEmpty()) {
 return "ApiRequest";
 }
 // Remove special characters and make it a valid Java method name
 String sanitized = name.replaceAll("[^a-zA-Z0-9]", "");
 if (sanitized.isEmpty()) {
 return "ApiRequest";
 }
 // Ensure it starts with uppercase for test method
 return Character.toUpperCase(sanitized.charAt(0)) + sanitized.substring(1);
 }

 private static String extractBaseUri(String url) {
 if (url == null || url.trim().isEmpty()) {
 return "http://localhost:8080";
 }

 try {
 java.net.URL urlObj = new java.net.URL(url);
 return urlObj.getProtocol() + "://" + urlObj.getHost() +
 (urlObj.getPort() != -1 ? ":" + urlObj.getPort() : "");
 } catch (Exception e) {
 // Fallback: extract manually
 if (url.contains("://")) {
 String[] parts = url.split("://", 2);
 if (parts.length == 2) {
 String[] hostParts = parts[1].split("/", 2);
 return parts[0] + "://" + hostParts[0];
 }
 }
 return "http://localhost:8080";
 }
 }

 private static String extractEndpoint(String url) {
 if (url == null || url.trim().isEmpty()) {
 return "/";
 }

 try {
 java.net.URL urlObj = new java.net.URL(url);
 String path = urlObj.getPath();
 String query = urlObj.getQuery();

 if (path == null || path.isEmpty()) {
 path = "/";
 }

 // Don't include query parameters in endpoint as they're handled separately
 return path;
 } catch (Exception e) {
 // Fallback: extract manually
 if (url.contains("://")) {
 String[] parts = url.split("://", 2);
 if (parts.length == 2) {
 String[] pathParts = parts[1].split("/", 2);
 if (pathParts.length == 2) {
 String pathWithQuery = "/" + pathParts[1];
 // Remove query parameters
 if (pathWithQuery.contains("?")) {
 return pathWithQuery.split("\\?")[0];
  }
 return pathWithQuery;
 }
 }
 }
 return "/";
 }
 }

 private static boolean isJsonRequest(CollectionManager.SavedRequest request) {
 if (request.getHeaders() != null) {
 String headers = request.getHeaders().toLowerCase();
 return headers.contains("application/json") || headers.contains("content-type: application/json");
 }

 if (request.getBody() != null && !request.getBody().trim().isEmpty()) {
 String body = request.getBody().trim();
 return body.startsWith("{") || body.startsWith("[");
 }

 return false;
 }

 private static void generatePositiveTest(StringBuilder test, CollectionManager.SavedRequest request) {
 String methodName = "test" + sanitizeMethodName(request.getName()) + "Success";
 test.append(" @Test(priority = 1)\n");
 test.append(" public void ").append(methodName).append("() {\n");

 buildBasicRequest(test, request);
 addSuccessAssertions(test, request);

 test.append(" }\n\n");
 }

 private static void generateNegativeTests(StringBuilder test, CollectionManager.SavedRequest request) {
 // Invalid endpoint test
 test.append(" @Test(priority = 2)\n");
 test.append(" public void test").append(sanitizeMethodName(request.getName())).append("InvalidEndpoint() {\n");
 test.append(" given()\n");
 addHeaders(test, request);
 test.append(" .when()\n");
 test.append(" .").append(request.getMethod().toLowerCase()).append("(\"/invalid-endpoint\")\n");
 test.append(" .then()\n");
 test.append(" .statusCode(404);\n");
 test.append(" }\n\n");

 // Unauthorized test
 test.append(" @Test(priority = 2)\n");
 test.append(" public void test").append(sanitizeMethodName(request.getName())).append("Unauthorized() {\n");
 test.append(" given()\n");
 test.append(" .when()\n");
 test.append(" .").append(request.getMethod().toLowerCase()).append("(\"").append(extractEndpoint(request.getUrl())).append("\")\n");
 test.append(" .then()\n");
 test.append(" .statusCode(anyOf(equalTo(401), equalTo(403)));\n");
 test.append(" }\n\n");

 // Invalid JSON body test
 if (hasJsonBody(request)) {
 test.append(" @Test(priority = 2)\n");
 test.append(" public void test").append(sanitizeMethodName(request.getName())).append("InvalidJson() {\n");
 test.append(" given()\n");
 addHeaders(test, request);
 test.append(" .body(\"{invalid json}\")\n");
 test.append(" .when()\n");
 test.append(" .").append(request.getMethod().toLowerCase()).append("(\"").append(extractEndpoint(request.getUrl())).append("\")\n");
 test.append(" .then()\n");
 test.append(" .statusCode(400);\n");
 test.append(" }\n\n");
 }
 }

 private static void generatePerformanceTest(StringBuilder test, CollectionManager.SavedRequest request) {
 test.append(" @Test(priority = 3)\n");
 test.append(" public void test").append(sanitizeMethodName(request.getName())).append("Performance() {\n");

 buildBasicRequest(test, request);
 test.append(" .time(lessThan(2000L))\n");
 test.append(" .extract().response();\n\n");

 test.append(" Assert.assertTrue(response.getTime() < 2000, \n");
 test.append(" \"Response time exceeded 2 seconds: \" + response.getTime() + \"ms\");\n");
 test.append(" }\n\n");
 }

 private static void generateSecurityTests(StringBuilder test, CollectionManager.SavedRequest request) {
 // SQL Injection test
 test.append(" @Test(priority = 4)\n");
 test.append(" public void test").append(sanitizeMethodName(request.getName())).append("SqlInjection() {\n");
 test.append(" given()\n");
 addHeaders(test, request);
 test.append(" .queryParam(\"id\", \"1' OR '1'='1\")\n");
 test.append(" .when()\n");
 test.append(" .").append(request.getMethod().toLowerCase()).append("(\"").append(extractEndpoint(request.getUrl())).append("\")\n");
 test.append(" .then()\n");
 test.append(" .statusCode(not(equalTo(500)))\n");
 test.append(" .body(not(containsString(\"SQL\")));\n");
 test.append(" }\n\n");

 // XSS test
 test.append(" @Test(priority = 4)\n");
 test.append(" public void test").append(sanitizeMethodName(request.getName())).append("XssProtection() {\n");
 test.append(" given()\n");
 addHeaders(test, request);
 test.append(" .queryParam(\"search\", \"<script>alert('xss')</script>\")\n");
 test.append(" .when()\n");
 test.append("  .").append(request.getMethod().toLowerCase()).append("(\"").append(extractEndpoint(request.getUrl())).append("\")\n");
 test.append(" .then()\n");
 test.append(" .statusCode(not(equalTo(500)))\n");
 test.append(" .body(not(containsString(\"<script>\")));\n");
 test.append(" }\n\n");
 }

 private static void generateDataValidationTests(StringBuilder test, CollectionManager.SavedRequest request) {
 if (request.getMethod().equals("GET")) {
 // Response schema validation
 test.append(" @Test(priority = 5)\n");
 test.append(" public void test").append(sanitizeMethodName(request.getName())).append("ResponseSchema() {\n");

 buildBasicRequest(test, request);
 if (isJsonRequest(request)) {
 test.append(" .body(\"$\", instanceOf(Object.class))\n");
 }
 test.append(" .extract().response();\n");
 test.append(" }\n\n");
 }

 // Large payload test for POST/PUT
 if (request.getMethod().equals("POST") || request.getMethod().equals("PUT")) {
 test.append(" @Test(priority = 5)\n");
 test.append(" public void test").append(sanitizeMethodName(request.getName())).append("LargePayload() {\n");
 test.append(" String largePayload = \"{\\\"data\\\": \\\"\" + \"x\".repeat(10000) + \"\\\"}\";\n");
 test.append(" given()\n");
 addHeaders(test, request);
 test.append(" .body(largePayload)\n");
 test.append(" .when()\n");
 test.append(" .").append(request.getMethod().toLowerCase()).append("(\"").append(extractEndpoint(request.getUrl())).append("\")\n");
 test.append(" .then()\n");
 test.append(" .statusCode(anyOf(equalTo(200), equalTo(201), equalTo(413)));\n");
 test.append(" }\n\n");
 }
 }

 private static void buildBasicRequest(StringBuilder test, CollectionManager.SavedRequest request) {
 test.append(" Response response = given()\n");
 addHeaders(test, request);
 addQueryParams(test, request);
 addBody(test, request);
 test.append(" .when()\n");
 test.append(" .").append(request.getMethod().toLowerCase()).append("(\"").append(extractEndpoint(request.getUrl())).append("\")\n");
 test.append(" .then()\n");
 }

 private static void addHeaders(StringBuilder test, CollectionManager.SavedRequest request) {
 if (request.getHeaders() != null && !request.getHeaders().trim().isEmpty()) {
 String[] headerLines = request.getHeaders().split("\n");
 for (String line : headerLines) {
 String[] parts = line.split(":", 2);
 if (parts.length == 2) {
 test.append(" .header(\"").append(parts[0].trim())
 .append("\", \"").append(parts[1].trim()).append("\")\n");
 }
 }
 }
 }

 private static void addQueryParams(StringBuilder test, CollectionManager.SavedRequest request) {
 if (request.getParams() != null && !request.getParams().trim().isEmpty()) {
 String[] paramLines = request.getParams().split("\n");
 for (String line : paramLines) {
 if (line.trim().isEmpty()) continue;
 String[] parts = line.split("=", 2);
 if (parts.length == 2) {
 test.append(" .queryParam(\"").append(parts[0].trim())
 .append("\", \"").append(parts[1].trim()).append("\")\n");
 }
 }
 }
 }

 private static void addBody(StringBuilder test, CollectionManager.SavedRequest request) {
 if (request.getBody() != null && !request.getBody().trim().isEmpty()) {
 test.append(" .body(\"\"\"").append("\n");
 test.append(" ").append(request.getBody().replace("\"", "\\\"")).append("\n");
 test.append(" \"\"\")\n");
 }
 }

 private static void addSuccessAssertions(StringBuilder test, CollectionManager.SavedRequest request) {
 test.append(" .statusCode(anyOf(equalTo(200), equalTo(201), equalTo(204)))\n");
 test.append(" .time(lessThan(5000L))\n");

 if (isJsonRequest(request)) {
 test.append(" .contentType(containsString(\"application/json\"))\n");
 }

 addResponseAssertions(test, request.getMethod());

 test.append(" .extract().response();\n\n");

 test.append(" Assert.assertTrue(response.getStatusCode() >= 200 && response.getStatusCode() < 300);\n");
 test.append(" Assert.assertNotNull(response.getBody());\n");

 if (request.getMethod().equals("GET")) {
 test.append(" Assert.assertTrue(response.getTime() < 3000);\n");
 }
 }

 private static boolean hasJsonBody(CollectionManager.SavedRequest request) {
 return request.getBody() != null && !request.getBody().trim().isEmpty() &&
 (request.getBody().trim().startsWith("{") || request.getBody().trim().startsWith("["));
 }

 private static void addResponseAssertions(StringBuilder test, String method) {
 switch (method.toUpperCase()) {
 case "GET":
 test.append(" .body(\"size()\", greaterThanOrEqualTo(0))\n");
 break;
 case "POST":
 test.append(" .statusCode(anyOf(equalTo(200), equalTo(201)))\n");
 break;
 case "PUT":
 test.append(" .statusCode(anyOf(equalTo(200), equalTo(204)))\n");
 break;
 case "DELETE":
 test.append("  .statusCode(anyOf(equalTo(200), equalTo(204), equalTo(404)))\n");
 break;
 case "PATCH":
 test.append(" .statusCode(anyOf(equalTo(200), equalTo(204)))\n");
 break;
 }
 }
}