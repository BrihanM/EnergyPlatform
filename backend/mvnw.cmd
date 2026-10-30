@echo off
setlocal
set WRAPPER_JAR=%~dp0.mvn\wrapper\maven-wrapper.jar
if not exist "%WRAPPER_JAR%" (
  echo Maven Wrapper jar not found: %WRAPPER_JAR%
  exit /b 1
)
set MAVEN_PROJECTBASEDIR=%~dp0.
java "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" -classpath "%WRAPPER_JAR%" org.apache.maven.wrapper.MavenWrapperMain %*
exit /b %ERRORLEVEL%
