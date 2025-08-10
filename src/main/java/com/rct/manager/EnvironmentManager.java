package com.rct.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.rct.util.LogManager;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EnvironmentManager {
 private static final String ENVIRONMENTS_FILE = "environments.json";
 private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{([^}]+)\\}\\}");

 private Map<String, Environment> environments;
 private String activeEnvironment;
 private ObjectMapper objectMapper;
 private List<EnvironmentChangeListener> listeners;

 public static class Environment {
 private String name;
 private Map<String, String> variables;
 private boolean isActive;

 public Environment() {
 this.variables = new HashMap<>();
 }

 public Environment(String name) {
 this.name = name;
 this.variables = new HashMap<>();
 this.isActive = false;
 }

 // Getters and setters
 public String getName() { return name; }
 public void setName(String name) { this.name = name; }
 public Map<String, String> getVariables() { return variables; }
 public void setVariables(Map<String, String> variables) { this.variables = variables; }
 public boolean isActive() { return isActive; }
 public void setActive(boolean active) { isActive = active; }

 public void addVariable(String key, String value) {
 variables.put(key, value);
 }

 public void removeVariable(String key) {
 variables.remove(key);
 }

 @Override
 public String toString() {
 return name + (isActive ? " (Active)" : "");
 }
 }

 public interface EnvironmentChangeListener {
 void onEnvironmentChanged(String environmentName);
 void onVariablesUpdated();
 }

 public EnvironmentManager() {
 this.environments = new HashMap<>();
 this.objectMapper = new ObjectMapper();
 this.listeners = new ArrayList<>();
 loadEnvironments();

 // Create default environment if none exist
 if (environments.isEmpty()) {
 createDefaultEnvironment();
 }
 }

 private void createDefaultEnvironment() {
 Environment defaultEnv = new Environment("Default");
 defaultEnv.addVariable("BASE_URL", "https://api.example.com");
 defaultEnv.addVariable("API_KEY", "your-api-key-here");
 defaultEnv.addVariable("TIMEOUT", "30000");
 environments.put("Default", defaultEnv);
 setActiveEnvironment("Default");
 saveEnvironments();
 }
 
 public void addEnvironment(String name) {
 if (!environments.containsKey(name)) {
 environments.put(name, new Environment(name));
 saveEnvironments();
 notifyVariablesUpdated();
 }
 }

 public void removeEnvironment(String name) {
 if (environments.containsKey(name) && environments.size() > 1) {
 environments.remove(name);
 if (name.equals(activeEnvironment)) {
 // Set first available environment as active
 setActiveEnvironment(environments.keySet().iterator().next());
 }
 saveEnvironments();
 notifyVariablesUpdated();
 }
 }

 public void duplicateEnvironment(String sourceName, String newName) {
 Environment source = environments.get(sourceName);
 if (source != null && !environments.containsKey(newName)) {
 Environment duplicate = new Environment(newName);
 duplicate.setVariables(new HashMap<>(source.getVariables()));
  environments.put(newName, duplicate);
 saveEnvironments();
 notifyVariablesUpdated();
 }
 }

 public void setActiveEnvironment(String name) {
 if (environments.containsKey(name)) {
 // Deactivate all environments
 environments.values().forEach(env -> env.setActive(false));
 // Activate selected environment
 environments.get(name).setActive(true);
 this.activeEnvironment = name;
 saveEnvironments();
 notifyEnvironmentChanged(name);
 }
 }

 public Environment getActiveEnvironment() {
 return activeEnvironment != null ? environments.get(activeEnvironment) : null;
 }

 public Environment getEnvironment(String name) {
 return environments.get(name);
 }

 public Set<String> getEnvironmentNames() {
 return new TreeSet<>(environments.keySet());
 }

 public Map<String, Environment> getAllEnvironments() {
 return new HashMap<>(environments);
 }

 public void updateVariable(String environmentName, String key, String value) {
 Environment env = environments.get(environmentName);
 if (env != null) {
 if (value == null || value.trim().isEmpty()) {
 env.removeVariable(key);
 } else {
  env.addVariable(key, value.trim());
 }
 saveEnvironments();
 notifyVariablesUpdated();
 }
 }

 public void removeVariable(String environmentName, String key) {
 Environment env = environments.get(environmentName);
 if (env != null) {
 env.removeVariable(key);
 saveEnvironments();
 notifyVariablesUpdated();
 }
 }

 public String resolveVariables(String text) {
 if (text == null || activeEnvironment == null) {
 return text;
 }

 Environment env = environments.get(activeEnvironment);
 if (env == null) {
 return text;
 }

 String resolved = text;
 Matcher matcher = VARIABLE_PATTERN.matcher(text);

 while (matcher.find()) {
 String variableName = matcher.group(1);
 String variableValue = env.getVariables().get(variableName);

 if (variableValue != null) {
 resolved = resolved.replace("{{" + variableName + "}}", variableValue);
 }
 }

 return resolved;
 }

 public List<String> findVariablesInText(String text) {
 List<String> variables = new ArrayList<>();
 if (text != null) {
 Matcher matcher = VARIABLE_PATTERN.matcher(text);
 while (matcher.find()) {
 String variableName = matcher.group(1);
 if (!variables.contains(variableName)) {
 variables.add(variableName);
 }
 }
 }
 return variables;
 }

 public boolean hasUnresolvedVariables(String text) {
 if (text == null || activeEnvironment == null) {
 return false;
 }

 Environment env = environments.get(activeEnvironment);
 if (env == null) {
 return false;
 }

 List<String> variables = findVariablesInText(text);
 for (String variable : variables) {
 if (!env.getVariables().containsKey(variable)) {
 return true;
 }
 }
 return false;
 }

 public void addListener(EnvironmentChangeListener listener) {
 listeners.add(listener);
 }

 public void removeListener(EnvironmentChangeListener listener) {
 listeners.remove(listener);
 }

 private void notifyEnvironmentChanged(String environmentName) {
 for (EnvironmentChangeListener listener : listeners) {
 try {
 listener.onEnvironmentChanged(environmentName);
 } catch (Exception e) {
 LogManager.getInstance().log("Error notifying environment change listener: " + e.getMessage());
 }
 }
 }

 private void notifyVariablesUpdated() {
 for (EnvironmentChangeListener listener : listeners) {
 try {
 listener.onVariablesUpdated();
 } catch (Exception e) {
 LogManager.getInstance().log("Error notifying variables update listener: " + e.getMessage());
 }
 }
 }

 private void loadEnvironments() {
 File file = getEnvironmentsFile();
 if (file.exists()) {
 try {
 TypeFactory typeFactory = objectMapper.getTypeFactory();
 Map<String, Environment> loadedEnvironments = objectMapper.readValue(file,
 typeFactory.constructMapType(HashMap.class, String.class, Environment.class));
 
 this.environments = loadedEnvironments;

 // Find active environment
 for (Map.Entry<String, Environment> entry : environments.entrySet()) {
 if (entry.getValue().isActive()) {
 this.activeEnvironment = entry.getKey();
 break;
 }
 }

 LogManager.getInstance().log("Loaded " + environments.size() + " environments from file");
 } catch (IOException e) {
 LogManager.getInstance().log("Error loading environments: " + e.getMessage());
 this.environments = new HashMap<>();
 }
 }
 }

 private void saveEnvironments() {
 try {
 objectMapper.writerWithDefaultPrettyPrinter().writeValue(getEnvironmentsFile(), environments);
 LogManager.getInstance().log("Saved " + environments.size() + " environments to file");
 } catch (IOException e) {
 LogManager.getInstance().log("Error saving environments: " + e.getMessage());
 }
 }

 private File getEnvironmentsFile() {
 return new File(com.rct.util.FileManager.getEnvironmentsFile());
 }

 public void exportEnvironment(String environmentName, File file) throws IOException {
 Environment env = environments.get(environmentName);
 if (env != null) {
 objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, env);
 }
 }

 public void importEnvironment(File file) throws IOException {
 Environment env = objectMapper.readValue(file, Environment.class);
 if (env.getName() == null || env.getName().trim().isEmpty()) {
  env.setName("Imported Environment");
 }

 // Ensure unique name
 String baseName = env.getName();
 int counter = 1;
 while (environments.containsKey(env.getName())) {
 env.setName(baseName + " (" + counter + ")");
 counter++;
 }

 environments.put(env.getName(), env);
 saveEnvironments();
 notifyVariablesUpdated();
 }
}