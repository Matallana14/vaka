@rem Gradle wrapper launcher para Windows
@if "%DEBUG%"=="" @echo off
@rem
set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.\
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%

set CLASSPATH=%APP_HOME%gradle\wrapper\gradle-wrapper.jar
java -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
