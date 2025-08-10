package com.rct.util;

import com.rct.model.RestResponse;
import com.rct.service.RestClientService;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class LoadTestRunner {

 public static class LoadTestConfig {
  private String url;
  private String method;
  private Map<String, String> headers;
  private String body;
  private int concurrentUsers;
  private int requestsPerUser;
  private int rampUpTimeSeconds;
  private int testDurationSeconds;

  public LoadTestConfig(String url, String method, Map<String, String> headers, String body,
                        int concurrentUsers, int requestsPerUser, int rampUpTimeSeconds, int testDurationSeconds) {
   this.url = url;
   this.method = method;
   this.headers = headers;
   this.body = body;
   this.concurrentUsers = concurrentUsers;
   this.requestsPerUser = requestsPerUser;
   this.rampUpTimeSeconds = rampUpTimeSeconds;
   this.testDurationSeconds = testDurationSeconds;
  }

  // Getters
  public String getUrl() { return url; }
  public String getMethod() { return method; }
  public Map<String, String> getHeaders() { return headers; }
  public String getBody() { return body; }
  public int getConcurrentUsers() { return concurrentUsers; }
  public int getRequestsPerUser() { return requestsPerUser; }
  public int getRampUpTimeSeconds() { return rampUpTimeSeconds; }
  public int getTestDurationSeconds() { return testDurationSeconds; }
 }

 public static class LoadTestResult {
  private final PerformanceMetrics metrics;
  private final boolean completed;
  private final String errorMessage;

  public LoadTestResult(PerformanceMetrics metrics, boolean completed, String errorMessage) {
   this.metrics = metrics;
   this.completed = completed;
   this.errorMessage = errorMessage;
  }

  public PerformanceMetrics getMetrics() { return metrics; }
  public boolean isCompleted() { return completed; }
  public String getErrorMessage() { return errorMessage; }
 }

 public static LoadTestResult runLoadTest(LoadTestConfig config, LoadTestProgressCallback callback) {
  PerformanceMetrics metrics = new PerformanceMetrics();
  ExecutorService executorService = null;

  try {
   metrics.startTest();
   executorService = Executors.newFixedThreadPool(config.getConcurrentUsers());

   AtomicInteger completedUsers = new AtomicInteger(0);
   CountDownLatch latch = new CountDownLatch(config.getConcurrentUsers());

   // Calculate ramp-up delay per user
   long rampUpDelayMs = config.getRampUpTimeSeconds() > 0 ?
           (config.getRampUpTimeSeconds() * 1000L) / config.getConcurrentUsers() : 0;

   // Submit user tasks with ramp-up delay
   for (int i = 0; i < config.getConcurrentUsers(); i++) {
    final int userIndex = i;
    final long delay = i * rampUpDelayMs;

    executorService.submit(() -> {
     try {
      if (delay > 0) {
       Thread.sleep(delay);
      }

      executeUserRequests(config, metrics, userIndex, callback);
      completedUsers.incrementAndGet();

      if (callback != null) {
       callback.onUserCompleted(userIndex, completedUsers.get(), config.getConcurrentUsers());
      }

     } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
     } finally {
      latch.countDown();
     }
    });
   }

   // Wait for test completion or timeout
   boolean completed = latch.await(config.getTestDurationSeconds() + config.getRampUpTimeSeconds() + 30,
           TimeUnit.SECONDS);

   metrics.endTest();

   if (callback != null) {
    callback.onTestCompleted(metrics);
   }

   return new LoadTestResult(metrics, completed, completed ? null : "Test timed out");

  } catch (Exception e) {
   metrics.endTest();
   return new LoadTestResult(metrics, false, "Test failed: " + e.getMessage());

  } finally {
   if (executorService != null) {
    executorService.shutdown();
    try {
     if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
      executorService.shutdownNow();
     }
    } catch (InterruptedException e) {
     executorService.shutdownNow();
     Thread.currentThread().interrupt();
    }
   }
  }
 }

 private static void executeUserRequests(LoadTestConfig config, PerformanceMetrics metrics,
                                         int userIndex, LoadTestProgressCallback callback) {
  RestClientService service = new RestClientService();

  for (int i = 0; i < config.getRequestsPerUser(); i++) {
   long startTime = System.nanoTime();
   boolean success = false;
   int statusCode = 0;
   String error = null;

   try {
    RestResponse response = service.sendRequest(
            config.getMethod(),
            config.getUrl(),
            config.getHeaders(),
            new HashMap<>(),
            config.getBody()
    );

    statusCode = response.getStatusCode();
    success = statusCode >= 200 && statusCode < 300;

   } catch (Exception e) {
    error = e.getMessage();
    statusCode = 0;
   }

   long endTime = System.nanoTime();
   long responseTime = (endTime - startTime) / 1_000_000; // Convert to milliseconds

   metrics.recordRequest(responseTime, statusCode, success, error);

   if (callback != null) {
    callback.onRequestCompleted(userIndex, i + 1, config.getRequestsPerUser(),
            responseTime, statusCode, success);
   }
  }
 }

 public interface LoadTestProgressCallback {
  void onUserCompleted(int userIndex, int completedUsers, int totalUsers);
  void onRequestCompleted(int userIndex, int completedRequests, int totalRequests,
                          long responseTime, int statusCode, boolean success);
  void onTestCompleted(PerformanceMetrics metrics);
 }
}
