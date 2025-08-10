package com.rct.util;

import com.rct.manager.CollectionManager;
import java.util.Map;

public class CodeGenerator {

 public enum Language {
 JAVA("Java", "java"),
 PYTHON("Python", "py"),
 JAVASCRIPT("JavaScript", "js"),
 CURL("cURL", "sh"),
 CSHARP("C#", "cs"),
 GO("Go", "go"),
 TESTNG("TestNG Test", "java");

 private final String displayName;
 private final String extension;

 Language(String displayName, String extension) {
 this.displayName = displayName;
 this.extension = extension;
 }

 public String getDisplayName() { return displayName; }
 public String getExtension() { return extension; }
 }

 public static String generateCode(CollectionManager.SavedRequest request, Language language) {
 switch (language) {
 case JAVA: return generateJava(request);
 case PYTHON: return generatePython(request);
 case JAVASCRIPT: return generateJavaScript(request);
 case CURL: return generateCurl(request);
 case CSHARP: return generateCSharp(request);
 case GO: return generateGo(request);
 case TESTNG: return TestGenerator.generateRestAssuredTest(request);
 default: return "// Unsupported language";
 }
 }

 private static String generateJava(CollectionManager.SavedRequest request) {
 StringBuilder code = new StringBuilder();
 code.append("import java.net.http.*;\n");
 code.append("import java.net.URI;\n");
 code.append("import java.time.Duration;\n\n");

 code.append("public class ApiClient {\n");
 code.append(" public static void main(String[] args) throws Exception {\n");
 code.append(" HttpClient client = HttpClient.newBuilder()\n");
 code.append(" .connectTimeout(Duration.ofSeconds(30))\n");
 code.append(" .build();\n\n");

 code.append(" HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()\n");
 code.append(" .uri(URI.create(\"").append(request.getUrl()).append("\"))\n");
 code.append(" .timeout(Duration.ofSeconds(30))");

 // Add headers
 if (request.getHeaders() != null && !request.getHeaders().trim().isEmpty()) {
 String[] headerLines = request.getHeaders().split("\n");
 for (String line : headerLines) {
 String[] parts = line.split(":", 2);
 if (parts.length == 2) {
 code.append("\n .header(\"").append(parts[0].trim())
 .append("\", \"").append(parts[1].trim()).append("\")");
 }
 }
 }

 // Add method and body
 if (request.getBody() != null && !request.getBody().trim().isEmpty()) {
 code.append("\n .").append(request.getMethod()).append("(HttpRequest.BodyPublishers.ofString(\"")
 .append(request.getBody().replace("\"", "\\\"").replace("\n", "\\n"))
 .append("\"));\n\n");
 } else {
 code.append("\n .").append(request.getMethod()).append("(HttpRequest.BodyPublishers.noBody());\n\n");
 }

 code.append(" HttpResponse<String> response = client.send(\n");
 code.append(" requestBuilder.build(),\n");
 code.append(" HttpResponse.BodyHandlers.ofString());\n\n");

 code.append(" System.out.println(\"Status: \" + response.statusCode());\n");
 code.append(" System.out.println(\"Response: \" + response.body());\n");
 code.append(" }\n");
 code.append("}\n");

 return code.toString();
 }

 private static String generatePython(CollectionManager.SavedRequest request) {
 StringBuilder code = new StringBuilder();
 code.append("import requests\nimport json\n\n");

 code.append("def make_request():\n");
 code.append(" url = \"").append(request.getUrl()).append("\"\n");

 // Headers
 if (request.getHeaders() != null && !request.getHeaders().trim().isEmpty()) {
 code.append(" headers = {\n");
 String[] headerLines = request.getHeaders().split("\n");
 for (String line : headerLines) {
 String[] parts = line.split(":", 2);
 if (parts.length == 2) {
 code.append(" \"").append(parts[0].trim()).append("\": \"")
 .append(parts[1].trim()).append("\",\n");
 }
 }
 code.append(" }\n");
 } else {
 code.append(" headers = {}\n");
 }

 // Body
 if (request.getBody() != null && !request.getBody().trim().isEmpty()) {
 code.append(" data = '''").append(request.getBody()).append("'''\n");
 }

 code.append("\n response = requests.").append(request.getMethod().toLowerCase()).append("(\n");
 code.append(" url,\n");
 code.append(" headers=headers");

 if (request.getBody() != null && !request.getBody().trim().isEmpty()) {
 code.append(",\n data=data");
  }

 code.append("\n )\n\n");
 code.append(" print(f\"Status: {response.status_code}\")\n");
 code.append(" print(f\"Response: {response.text}\")\n");
 code.append(" return response\n\n");
 code.append("if __name__ == \"__main__\":\n");
 code.append(" make_request()\n");

 return code.toString();
 }

 private static String generateJavaScript(CollectionManager.SavedRequest request) {
 StringBuilder code = new StringBuilder();
 code.append("async function makeRequest() {\n");
 code.append(" const url = '").append(request.getUrl()).append("';\n");

 code.append(" const options = {\n");
 code.append(" method: '").append(request.getMethod()).append("',\n");

 // Headers
 if (request.getHeaders() != null && !request.getHeaders().trim().isEmpty()) {
 code.append(" headers: {\n");
 String[] headerLines = request.getHeaders().split("\n");
 for (String line : headerLines) {
 String[] parts = line.split(":", 2);
 if (parts.length == 2) {
 code.append(" '").append(parts[0].trim()).append("': '")
 .append(parts[1].trim()).append("',\n");
 }
 }
 code.append(" },\n");
 }

 // Body
 if (request.getBody() != null && !request.getBody().trim().isEmpty()) {
 code.append(" body: `").append(request.getBody()).append("`\n");
 }

 code.append(" };\n\n");
 code.append(" try {\n");
 code.append(" const response = await fetch(url, options);\n");
 code.append(" const data = await response.text();\n");
 code.append(" console.log('Status:', response.status);\n");
 code.append(" console.log('Response:', data);\n");
 code.append(" return { status: response.status, data };\n");
 code.append(" } catch (error) {\n");
 code.append(" console.error('Error:', error);\n");
 code.append(" throw error;\n");
 code.append(" }\n");
 code.append("}\n\n");
 code.append("makeRequest();\n");

 return code.toString();
 }

 private static String generateCurl(CollectionManager.SavedRequest request) {
 StringBuilder code = new StringBuilder();
 code.append("curl -X ").append(request.getMethod());
 code.append(" \\\n '").append(request.getUrl()).append("'");

  // Headers
 if (request.getHeaders() != null && !request.getHeaders().trim().isEmpty()) {
 String[] headerLines = request.getHeaders().split("\n");
 for (String line : headerLines) {
 String[] parts = line.split(":", 2);
 if (parts.length == 2) {
 code.append(" \\\n -H '").append(parts[0].trim()).append(": ")
 .append(parts[1].trim()).append("'");
 }
 }
 }

 // Body
 if (request.getBody() != null && !request.getBody().trim().isEmpty()) {
 code.append(" \\\n -d '").append(request.getBody().replace("'", "\\'")).append("'");
 }

 return code.toString();
 }

 private static String generateCSharp(CollectionManager.SavedRequest request) {
 StringBuilder code = new StringBuilder();
 code.append("using System;\nusing System.Net.Http;\nusing System.Text;\nusing System.Threading.Tasks;\n\n");

 code.append("class Program\n{\n");
 code.append(" private static readonly HttpClient client = new HttpClient();\n\n");
 code.append(" static async Task Main(string[] args)\n {\n");
 code.append(" try\n {\n");
 code.append(" var response = await MakeRequest();\n");
 code.append(" Console.WriteLine($\"Status: {response.StatusCode}\");\n");
 code.append(" Console.WriteLine($\"Response: {await response.Content.ReadAsStringAsync()}\");\n");
 code.append(" }\n catch (Exception ex)\n {\n");
 code.append(" Console.WriteLine($\"Error: {ex.Message}\");\n");
 code.append(" }\n }\n\n");

 code.append(" static async Task<HttpResponseMessage> MakeRequest()\n {\n");
 code.append(" var request = new HttpRequestMessage\n {\n");
 code.append(" Method = HttpMethod.").append(capitalize(request.getMethod())).append(",\n");
 code.append(" RequestUri = new Uri(\"").append(request.getUrl()).append("\")\n");
 code.append(" };\n\n");

 // Headers
 if (request.getHeaders() != null && !request.getHeaders().trim().isEmpty()) {
 String[] headerLines = request.getHeaders().split("\n");
 for (String line : headerLines) {
 String[] parts = line.split(":", 2);
 if (parts.length == 2) {
 code.append(" request.Headers.Add(\"").append(parts[0].trim())
 .append("\", \"").append(parts[1].trim()).append("\");\n");
 }
 }
 }

 // Body
 if (request.getBody() != null && !request.getBody().trim().isEmpty()) {
 code.append(" request.Content = new StringContent(@\"")
 .append(request.getBody().replace("\"", "\"\""))
 .append("\", Encoding.UTF8, \"application/json\");\n");
 }

 code.append("\n return await client.SendAsync(request);\n");
 code.append(" }\n}\n");

 return code.toString();
 }

 private static String generateGo(CollectionManager.SavedRequest request) {
 StringBuilder code = new StringBuilder();
 code.append("package main\n\n");
 code.append("import (\n");
 code.append(" \"fmt\"\n");
 code.append(" \"io\"\n");
 code.append(" \"net/http\"\n");
 code.append(" \"strings\"\n");
 code.append(")\n\n");

 code.append("func main() {\n");
 code.append(" url := \"").append(request.getUrl()).append("\"\n");

 // Body
 if (request.getBody() != null && !request.getBody().trim().isEmpty()) {
 code.append(" payload := `").append(request.getBody()).append("`\n");
 code.append(" req, err := http.NewRequest(\"").append(request.getMethod())
 .append("\", url, strings.NewReader(payload))\n");
 } else {
 code.append(" req, err := http.NewRequest(\"").append(request.getMethod())
 .append("\", url, nil)\n");
 }

 code.append(" if err != nil {\n");
 code.append(" fmt.Printf(\"Error creating request: %v\\n\", err)\n");
 code.append(" return\n }\n\n");

 // Headers
 if (request.getHeaders() != null && !request.getHeaders().trim().isEmpty()) {
 String[] headerLines = request.getHeaders().split("\n");
 for (String line : headerLines) {
 String[] parts = line.split(":", 2);
 if (parts.length == 2) {
 code.append(" req.Header.Set(\"").append(parts[0].trim())
 .append("\", \"").append(parts[1].trim()).append("\")\n");
 }
 }
 }

 code.append("\n client := &http.Client{}\n");
 code.append(" resp, err := client.Do(req)\n");
 code.append(" if err != nil {\n");
 code.append(" fmt.Printf(\"Error making request: %v\\n\", err)\n");
 code.append(" return\n }\n");
 code.append(" defer resp.Body.Close()\n\n");

 code.append(" body, err := io.ReadAll(resp.Body)\n");
 code.append(" if err != nil {\n");
 code.append(" fmt.Printf(\"Error reading response: %v\\n\", err)\n");
 code.append(" return\n }\n\n");

 code.append(" fmt.Printf(\"Status: %s\\n\", resp.Status)\n");
 code.append(" fmt.Printf(\"Response: %s\\n\", string(body))\n");
 code.append("}\n");

 return code.toString();
 }

 private static String capitalize(String str) {
 if (str == null || str.isEmpty()) return str;
 return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
 }
}
