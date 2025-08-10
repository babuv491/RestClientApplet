package com.rct.examples;

import com.rct.util.LoadTestRunner;
import com.rct.util.PerformanceMetrics;

import java.util.HashMap;
import java.util.Map;

/**
 * Example demonstrating how to use the LoadTestRunner programmatically
 */
public class PerformanceTestExample {

 public static void main(String[] args) {
  // Example 1: Simple GET request load test
  runSimpleGetTest();

  // Example 2: POST request with JSON body
  runPostJsonTest();

  // Example 3: High concurrency test
  runHighConcurrencyTest();
 }

 private static void runSimpleGetTest() {
  System.out.println("=== Simple GET Load Test ===");

  Map<String, String> headers = new HashMap<>();
  headers.put("Accept", "application/json");
  headers.put("User-Agent", "REST-Client-Pro/1.0");

  LoadTestRunner.LoadTestConfig config = new LoadTestRunner.LoadTestConfig(
          "https://jsonplaceholder.typicode.com/posts/1",
          "GET",
          headers,
          null,
          5, // 5 concurrent users
          10, // 10 requests per user
          2, // 2 seconds ramp-up
          30 // 30 seconds max duration
  );

  LoadTestRunner.LoadTestResult result = LoadTestRunner.runLoadTest(config,
          new SimpleProgressCallback("GET Test"));

  printResults(result);
 }

 private static void runPostJsonTest() {
  System.out.println("\n=== POST JSON Load Test ===");

  Map<String, String> headers = new HashMap<>();
  headers.put("Content-Type", "application/json");
  headers.put("Accept", "application/json");

  String jsonBody = "{\n" +
          " \"title\": \"Load Test Post\",\n" +
          " \"body\": \"This is a test post from load testing\",\n" +
          " \"userId\": 1\n" +
          "}";

  LoadTestRunner.LoadTestConfig config = new LoadTestRunner.LoadTestConfig(
          "https://jsonplaceholder.typicode.com/posts",
          "POST",
          headers,
          jsonBody,
          3, // 3 concurrent users
          5, // 5 requests per user
          1, // 1 second ramp-up
          20 // 20 seconds max duration
  );

  LoadTestRunner.LoadTestResult result = LoadTestRunner.runLoadTest(config,
          new SimpleProgressCallback("POST Test"));

  printResults(result);
 }

 private static void runHighConcurrencyTest() {
  System.out.println("\n=== High Concurrency Load Test ===");

  Map<String, String> headers = new HashMap<>();
  headers.put("Accept", "application/json");

  LoadTestRunner.LoadTestConfig config = new LoadTestRunner.LoadTestConfig(
          "https://httpbin.org/delay/1",
          "GET",
          headers,
          null,
          20, // 20 concurrent users
          5, // 5 requests per user
          5, // 5 seconds ramp-up
          60 // 60 seconds max duration
  );

  LoadTestRunner.LoadTestResult result = LoadTestRunner.runLoadTest(config,
          new SimpleProgressCallback("High Concurrency Test"));

  printResults(result);
 }

 private static void printResults(LoadTestRunner.LoadTestResult result) {
  if (result.isCompleted()) {
   System.out.println("✅Test completed successfully");
   System.out.println(result.getMetrics().generateReport());
  } else {
   System.out.println("❌Test failed: " + result.getErrorMessage());
   if (result.getMetrics().getTotalRequests() > 0) {
    System.out.println("Partial results:");
    System.out.println(result.getMetrics().generateReport());
   }
  }
  System.out.println("-".repeat(50));
 }

 private static class SimpleProgressCallback implements LoadTestRunner.LoadTestProgressCallback {
  private final String testName;
  private int lastReportedProgress = 0;

  public SimpleProgressCallback(String testName) {
   this.testName = testName;
  }

  @Override
  public void onUserCompleted(int userIndex, int completedUsers, int totalUsers) {
   int progress = (completedUsers * 100) / totalUsers;
   if (progress >= lastReportedProgress + 20) {
    System.out.printf("[%s] Users completed: %d/%d (%d%%)\n",
            testName, completedUsers, totalUsers, progress);
    lastReportedProgress = progress;
   }
  }

  @Override
  public void onRequestCompleted(int userIndex, int completedRequests, int totalRequests,
                                 long responseTime, int statusCode, boolean success) {
   // Optional: Log individual request completions
   // System.out.printf("User %d: Request %d/%d - %dms - %d %s\n",
   // userIndex, completedRequests, totalRequests, responseTime,
   // statusCode, success ? "✅" : "❌");
  }

  @Override
  public void onTestCompleted(PerformanceMetrics metrics) {
   System.out.printf("[%s] Test completed - %d requests in %.2f seconds\n",
           testName, metrics.getTotalRequests(),
           metrics.getTestDuration() / 1000.0);
  }
 }
}
