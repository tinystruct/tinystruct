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

@REM Consolidate classpath entries, initialize ROOT and VERSION
set "ROOT=%~dp0..\"
set "VERSION=1.4.4"
set "classpath=%ROOT%target\classes;%ROOT%lib\tinystruct-%VERSION%-jar-with-dependencies.jar;%ROOT%lib\tinystruct-%VERSION%.jar;%ROOT%lib\*;%ROOT%WEB-INF\lib\*;%ROOT%WEB-INF\classes;%USERPROFILE%\.m2\repository\org\tinystruct\tinystruct\%VERSION%\tinystruct-%VERSION%-jar-with-dependencies.jar;%USERPROFILE%\.m2\repository\org\tinystruct\tinystruct\%VERSION%\tinystruct-%VERSION%.jar"

@REM Run Java application
%JAVA_CMD% -cp "%classpath%" org.tinystruct.system.Dispatcher %*
