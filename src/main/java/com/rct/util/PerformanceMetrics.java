package com.rct.util;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class PerformanceMetrics {
 private final AtomicInteger totalRequests = new AtomicInteger(0);
 private final AtomicInteger successfulRequests = new AtomicInteger(0);
 private final AtomicInteger failedRequests = new AtomicInteger(0);
 private final AtomicLong totalResponseTime = new AtomicLong(0);
 private final AtomicLong minResponseTime = new AtomicLong(Long.MAX_VALUE);
 private final AtomicLong maxResponseTime = new AtomicLong(0);

 private final List<Long> responseTimes = Collections.synchronizedList(new ArrayList<>());
 private final Map<Integer, AtomicInteger> statusCodeCounts = new ConcurrentHashMap<>();
 private final Map<String, AtomicInteger> errorCounts = new ConcurrentHashMap<>();

 private long testStartTime;
 private long testEndTime;

 public void startTest() {
 testStartTime = System.currentTimeMillis();
 reset();
 }

 public void endTest() {
 testEndTime = System.currentTimeMillis();
 }

 public void recordRequest(long responseTime, int statusCode, boolean success, String error) {
 totalRequests.incrementAndGet();
 totalResponseTime.addAndGet(responseTime);
 responseTimes.add(responseTime);

 // Update min/max response times
 minResponseTime.updateAndGet(current -> Math.min(current, responseTime));
 maxResponseTime.updateAndGet(current -> Math.max(current, responseTime));

 // Count status codes
 statusCodeCounts.computeIfAbsent(statusCode, k -> new AtomicInteger(0)).incrementAndGet();

 if (success) {
 successfulRequests.incrementAndGet();
 } else {
 failedRequests.incrementAndGet();
 if (error != null) {
 errorCounts.computeIfAbsent(error, k -> new AtomicInteger(0)).incrementAndGet();
 }
 }
 }

 public void reset() {
 totalRequests.set(0);
 successfulRequests.set(0);
 failedRequests.set(0);
 totalResponseTime.set(0);
 minResponseTime.set(Long.MAX_VALUE);
 maxResponseTime.set(0);
 responseTimes.clear();
 statusCodeCounts.clear();
 errorCounts.clear();
 }

 // Getters for metrics
 public int getTotalRequests() { return totalRequests.get(); }
 public int getSuccessfulRequests() { return successfulRequests.get(); }
 public int getFailedRequests() { return failedRequests.get(); }
 public double getSuccessRate() {
 int total = getTotalRequests();
 return total > 0 ? (double) getSuccessfulRequests() / total * 100 : 0;
  }

 public long getAverageResponseTime() {
 int total = getTotalRequests();
 return total > 0 ? totalResponseTime.get() / total : 0;
 }

 public long getMinResponseTime() {
 long min = minResponseTime.get();
 return min == Long.MAX_VALUE ? 0 : min;
 }

 public long getMaxResponseTime() { return maxResponseTime.get(); }

 public long getTestDuration() {
 return testEndTime > testStartTime ? testEndTime - testStartTime :
 System.currentTimeMillis() - testStartTime;
 }

 public double getRequestsPerSecond() {
 long duration = getTestDuration() / 1000;
 return duration > 0 ? (double) getTotalRequests() / duration : 0;
 }

 public long getPercentile(double percentile) {
 if (responseTimes.isEmpty()) return 0;

 List<Long> sorted = new ArrayList<>(responseTimes);
 Collections.sort(sorted);

 int index = (int) Math.ceil(sorted.size() * percentile / 100) - 1;
 index = Math.max(0, Math.min(index, sorted.size() - 1));

 return sorted.get(index);
 }

 public Map<Integer, Integer> getStatusCodeDistribution() {
 Map<Integer, Integer> distribution = new HashMap<>();
 for (Map.Entry<Integer, AtomicInteger> entry : statusCodeCounts.entrySet()) {
 distribution.put(entry.getKey(), entry.getValue().get());
 }
 return distribution;
 }

 public Map<String, Integer> getErrorDistribution() {
 Map<String, Integer> distribution = new HashMap<>();
 for (Map.Entry<String, AtomicInteger> entry : errorCounts.entrySet()) {
 distribution.put(entry.getKey(), entry.getValue().get());
 }
 return distribution;
 }

 public String generateReport() {
 StringBuilder report = new StringBuilder();
 report.append("=== Performance Test Report ===\n");
 report.append(String.format("Test Duration: %d ms (%.2f seconds)\n",
  getTestDuration(), getTestDuration() / 1000.0));
 report.append(String.format("Total Requests: %d\n", getTotalRequests()));
 report.append(String.format("Successful: %d (%.2f%%)\n",
 getSuccessfulRequests(), getSuccessRate()));
 report.append(String.format("Failed: %d (%.2f%%)\n",
 getFailedRequests(), 100 - getSuccessRate()));
 report.append(String.format("Requests/Second: %.2f\n", getRequestsPerSecond()));

 report.append("\n=== Response Times ===\n");
 report.append(String.format("Average: %d ms\n", getAverageResponseTime()));
 report.append(String.format("Min: %d ms\n", getMinResponseTime()));
 report.append(String.format("Max: %d ms\n", getMaxResponseTime()));
 report.append(String.format("50th Percentile: %d ms\n", getPercentile(50)));
 report.append(String.format("95th Percentile: %d ms\n", getPercentile(95)));
 report.append(String.format("99th Percentile: %d ms\n", getPercentile(99)));

 report.append("\n=== Status Code Distribution ===\n");
 for (Map.Entry<Integer, Integer> entry : getStatusCodeDistribution().entrySet()) {
 report.append(String.format("Status %d: %d requests\n",
 entry.getKey(), entry.getValue()));
 }

 if (!errorCounts.isEmpty()) {
 report.append("\n=== Error Distribution ===\n");
  for (Map.Entry<String, Integer> entry : getErrorDistribution().entrySet()) {
 report.append(String.format("%s: %d occurrences\n",
 entry.getKey(), entry.getValue()));
 }
 }

 return report.toString();
 }
}
