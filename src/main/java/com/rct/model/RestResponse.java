package com.rct.model;

import java.util.Map;

public class RestResponse {
 private int statusCode;
 private String statusText;
 private String body;
 private Map<String, String> headers;
 private long responseTime;

 public RestResponse() {}

 public RestResponse(int statusCode, String statusText, String body, Map<String, String> headers) {
 this.statusCode = statusCode;
 this.statusText = statusText;
 this.body = body;
 this.headers = headers;
 }

 public int getStatusCode() { return statusCode; }
 public void setStatusCode(int statusCode) { this.statusCode = statusCode; }

 public String getStatusText() { return statusText; }
 public void setStatusText(String statusText) { this.statusText = statusText; }

 public String getBody() { return body; }
 public void setBody(String body) { this.body = body; }

 public Map<String, String> getHeaders() { return headers; }
 public void setHeaders(Map<String, String> headers) { this.headers = headers; }

 public long getResponseTime() { return responseTime; }
 public void setResponseTime(long responseTime) { this.responseTime = responseTime; }

 @Override
 public String toString() {
 return "RestResponse{statusCode=" + statusCode + ", statusText='" + statusText +
 "', responseTime=" + responseTime + '}';
 }
}
