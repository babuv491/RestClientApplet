package com.rct.util;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CurlImporter {

 public static class CurlRequest {
 private String method = "GET";
 private String url;
 private Map<String, String> headers = new HashMap<>();
 private String body = "";

 public String getMethod() { return method; }
 public void setMethod(String method) { this.method = method; }

 public String getUrl() { return url; }
 public void setUrl(String url) { this.url = url; }
 
 public Map<String, String> getHeaders() { return headers; }
 public void setHeaders(Map<String, String> headers) { this.headers = headers; }

 public String getBody() { return body; }
 public void setBody(String body) { this.body = body; }
 }

 public static CurlRequest parseCurl(String curlCommand) {
 CurlRequest request = new CurlRequest();

 // Clean up multi-line format by removing line continuations
 String cleanedCommand = curlCommand.replaceAll("\\\\\\s*\\n\\s*", " ").trim();

 // Extract method
 Pattern methodPattern = Pattern.compile("-X\\s+([A-Z]+)");
 Matcher methodMatcher = methodPattern.matcher(cleanedCommand);
 if (methodMatcher.find()) {
 request.setMethod(methodMatcher.group(1));
 }

 // Extract URL (improved pattern to handle quoted URLs)
 Pattern urlPattern = Pattern.compile("curl\\s+(?:-X\\s+[A-Z]+\\s+)?['\"]([^'\"]+)['\"]|curl\\s+(?:-X\\s+[A-Z]+\\s+)?([^\\s]+)");
 Matcher urlMatcher = urlPattern.matcher(cleanedCommand);
 if (urlMatcher.find()) {
 String url = urlMatcher.group(1) != null ? urlMatcher.group(1) : urlMatcher.group(2);
 request.setUrl(url);
 }

 // Extract headers
 Pattern headerPattern = Pattern.compile("-H\\s+['\"]([^'\"]+)['\"]");
 Matcher headerMatcher = headerPattern.matcher(cleanedCommand);
 while (headerMatcher.find()) {
  String header = headerMatcher.group(1);
 String[] parts = header.split(":", 2);
 if (parts.length == 2) {
 request.getHeaders().put(parts[0].trim(), parts[1].trim());
 }
 }

 // Extract body
 Pattern bodyPattern = Pattern.compile("-d\\s+['\"]([^'\"]*)['\"]");
 Matcher bodyMatcher = bodyPattern.matcher(cleanedCommand);
 if (bodyMatcher.find()) {
 String body = bodyMatcher.group(1);
 // Unescape common escape sequences
 body = body.replace("\\n", "\n").replace("\\\"", "\"");
 request.setBody(body);
 }

 return request;
 }
}
