@rem ***************************************************************************
@rem Copyright  (c) 2017 James Mover Zhou
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
set "ROOT=%~dp0..\"
set "VERSION=0.7.9"
set "classpath=%ROOT%target\classes;%ROOT%lib\*;%ROOT%WEB-INF\lib\*;%ROOT%WEB-INF\classes;%classpath%"
@java -cp "%ROOT%target\classes;%ROOT%lib\*;%ROOT%lib\tinystruct-%VERSION%-jar-with-dependencies.jar;%ROOT%WEB-INF\lib\*;%ROOT%WEB-INF\classes;%USERPROFILE%\.m2\repository\org\tinystruct\tinystruct\%VERSION%\tinystruct-%VERSION%-jar-with-dependencies.jar" org.tinystruct.system.Dispatcher %*