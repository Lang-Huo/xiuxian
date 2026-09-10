@echo off
rem 自带 Maven 启动器（Windows，绕过本机可能损坏的 mvn 脚本）
rem 用法：mvnw.cmd <maven 参数>，如 mvnw.cmd spring-boot:run
setlocal
set SCRIPT_DIR=%~dp0
set MVN_HOME=%SCRIPT_DIR%..\tooling\apache-maven-3.9.6
for /f "delims=" %%i in ('dir /b "%MVN_HOME%\boot\plexus-classworlds-*.jar"') do set BOOT_JAR=%MVN_HOME%\boot\%%i
if not exist "%BOOT_JAR%" (
  echo 未找到 tooling\apache-maven-3.9.6，请确认目录结构。 >&2
  exit /b 1
)
java -classpath "%BOOT_JAR%" -Dmaven.home="%MVN_HOME%" -Dmaven.multiModuleProjectDirectory="%SCRIPT_DIR%" -Dclassworlds.conf="%MVN_HOME%\bin\m2.conf" org.codehaus.plexus.classworlds.launcher.Launcher %*
