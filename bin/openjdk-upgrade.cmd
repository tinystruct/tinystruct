@echo off
set "ROOT=%~dp0..\"
CALL %ROOT%\bin\dispatcher.cmd download --url https://download.java.net/java/GA/jdk17/0d483333a00540d886896bac774ff48b/35/GPL/openjdk-17_windows-x64_bin.zip
powershell -Command "Expand-Archive -Path '%ROOT%\java\GA\jdk17\0d483333a00540d886896bac774ff48b\35\GPL\openjdk-17_windows-x64_bin.zip' -DestinationPath '%ROOT%'"
del /F /Q "%ROOT%\java\GA\jdk17\0d483333a00540d886896bac774ff48b\35\GPL\openjdk-17_windows-x64_bin.zip"
rmdir /s /q "%ROOT%\java\"
setx JAVA_HOME "%ROOT%\jdk-17"

for /f "tokens=3*" %%A in ('reg query HKCU\Environment /v PATH 2^>nul') do set "OLD_PATH=%%A %%B"
set "NEW_PATH=%OLD_PATH%;%ROOT%\jdk-17\bin"
setx PATH "%NEW_PATH%"

echo OpenJDK17 has installed successfully.