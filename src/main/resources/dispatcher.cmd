@rem ***************************************************************************
@rem Copyright  (c) 2023 James Mover Zhou
@rem
@rem Licensed under the Apache License, Version 2.0 (the "License");
@rem you may not use this file except in compliance with the License.
@rem You may obtain a copy of the License at
@rem
@rem    http:\\www.apache.org\licenses\LICENSE-2.0
@rem
@rem Unless required by applicable law or agreed to in writing, software
@rem distributed under the License is distributed on an "AS IS" BASIS,
@rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@rem See the License for the specific language governing permissions and
@rem limitations under the License.
@rem ***************************************************************************
@echo off

@REM Set the local Maven repository path for tinystruct.jar
set "MAVEN_REPO=%USERPROFILE%\.m2\repository\org\tinystruct\tinystruct"
@REM Consolidate classpath entries, initialize ROOT and VERSION
set "ROOT=%~dp0..\"
set "VERSION={%VERSION%}"

@REM Define the paths for tinystruct jars in the Maven repository
set "TINYSTRUCT_MAVEN_JAR=%MAVEN_REPO%\%VERSION%\tinystruct-%VERSION%.jar"
set "TINYSTRUCT_MAVEN_JAR_WITH_DEPS=%MAVEN_REPO%\%VERSION%\tinystruct-%VERSION%-jar-with-dependencies.jar"

REM Check which jar to use for extracting Maven Wrapper
if exist "%TINYSTRUCT_MAVEN_JAR_WITH_DEPS%" (
    set "JAR_PATH=%TINYSTRUCT_MAVEN_JAR_WITH_DEPS%"
) else (
    set "JAR_PATH=%TINYSTRUCT_MAVEN_JAR%"
)

@REM Check if any of the tinystruct jars exists in the local Maven repository
if not exist "%TINYSTRUCT_MAVEN_JAR%" if not exist "%TINYSTRUCT_MAVEN_JAR_WITH_DEPS%" (
    echo Error: Neither tinystruct.jar nor tinystruct-jar-with-dependencies.jar found in local Maven repository. Please ensure one of them is available.
    exit /B 1
)

@REM Check if JAVA_HOME is set and valid
if "%JAVA_HOME%" == "" (
    echo Error: JAVA_HOME not found in your environment. >&2
    echo Please set the JAVA_HOME variable in your environment to match the location of your Java installation. >&2
    exit /B 1
)

if not exist "%JAVA_HOME%\bin\java.exe" (
    echo Error: JAVA_HOME is set to an invalid directory. >&2
    echo JAVA_HOME = "%JAVA_HOME%" >&2
    echo Please set the JAVA_HOME variable in your environment to match the location of your Java installation. >&2
    exit /B 1
)

set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"

@REM Check if the Maven Wrapper is already available
if not exist "mvnw" (
    echo Maven Wrapper not found. Extracting from JAR...

    @REM Run Java code to extract the ZIP file from the JAR
    %JAVA_CMD% -cp "%JAR_PATH%" org.tinystruct.system.Dispatcher maven:wrapper --jar-file-path "%JAR_PATH%" --destination-dir "%ROOT%"

    if exist "%ROOT%\maven-wrapper.zip" (
        echo Maven wrapper ZIP extracted successfully.
        @REM Now unzip the Maven Wrapper files
        powershell -Command "Expand-Archive -Path '%ROOT%\maven-wrapper.zip' -DestinationPath '%ROOT%'"
        echo Maven wrapper setup completed.
    ) else (
        echo Error: Maven wrapper ZIP file not found in JAR.
        exit /B 1
    )
)

set "classpath=%ROOT%target\classes;%ROOT%lib\tinystruct-%VERSION%-jar-with-dependencies.jar;%ROOT%lib\tinystruct-%VERSION%.jar;%ROOT%lib\*;%ROOT%WEB-INF\lib\*;%ROOT%WEB-INF\classes;%USERPROFILE%\.m2\repository\org\tinystruct\tinystruct\%VERSION%\tinystruct-%VERSION%-jar-with-dependencies.jar;%USERPROFILE%\.m2\repository\org\tinystruct\tinystruct\%VERSION%\tinystruct-%VERSION%.jar"

@REM Run Java application
%JAVA_CMD% -cp "%classpath%" org.tinystruct.system.Dispatcher %*