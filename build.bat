@echo off
echo Building REST Client Pro standalone executable...
echo.

REM Check if Maven is available
where mvn >nul 2>nul
if %ERRORLEVEL% equ 0 (
 echo Building standalone JAR with all dependencies...
 call mvn clean package -DskipTests
 if %ERRORLEVEL% neq 0 (
 echo Build failed!

 exit /b 1
 )
 echo.
 echo Build successful!
 echo Executable JAR created: target\rest-client-pro.jar
 echo.
 echo To run: java -jar target\rest-client-pro.jar
 echo.
) else (
 echo Maven not found. Please install Maven to build the project.
 echo.
 echo Alternative: Use your IDE to build the project with Maven.
)

