`"¡Cuán numerosas son tus obras, oh SEÑOR! Con sabiduría las hiciste todas; la tierra está llena de tus criaturas."`
***Salmos 104:24***

Language: [English](../README.md) | [Português (Brasil)](README.pt-BR.md) | [简体中文](README.zh-CN.md) | [繁體中文](README.zh-TW.md) | [日本語](README.ja.md) | [한국어](README.ko.md) | [Türkçe](README.tr.md) | [Русский](README.ru.md) | [Tiếng Việt](README.vi.md) | [ไทย](README.th.md) | [Deutsch](README.de.md) | [Español](README.es.md)

El framework tinystruct
--
Un framework sencillo para el desarrollo en Java. Pensamiento simple, mejor diseño, fácil de usar y con buen rendimiento.

[![MvnRepository](https://badges.mvnrepository.com/badge/org.tinystruct/tinystruct/badge.svg?label=MvnRepository)](https://mvnrepository.com/artifact/org.tinystruct/tinystruct)
[![CodeQL](https://github.com/tinystruct/tinystruct/actions/workflows/codeql.yml/badge.svg)](https://github.com/tinystruct/tinystruct/actions/workflows/codeql.yml)

[![Star History Chart](https://api.star-history.com/svg?repos=tinystruct/tinystruct&type=Date)](https://www.star-history.com/#tinystruct/tinystruct&Date)

## Requisitos previos

- Java Development Kit (JDK) 17 o superior
- Maven para la gestión de dependencias
- Un editor de texto o IDE como IntelliJ IDEA, Eclipse o VS Code

Primeros pasos con el archetype de tinystruct
--
Puedes usar este archetype para crear rápidamente un proyecto basado en tinystruct: https://github.com/tinystruct/tinystruct-archetype

Instalación manual y primeros pasos
--
* Agrega la dependencia a tu pom.xml.
```xml
<dependency>
  <groupId>org.tinystruct</groupId>
  <artifactId>tinystruct</artifactId>
  <version>1.7.27</version>
  <classifier>jar-with-dependencies</classifier> <!-- Opcional -->
</dependency>
```

* Extiende AbstractApplication en Java:

```java
package tinystruct.examples;


import org.tinystruct.AbstractApplication;
import org.tinystruct.ApplicationException;
import org.tinystruct.system.annotation.Action;

public class example extends AbstractApplication {

    @Override
    public void init() {
        // TODO Auto-generated method stub
    }

    @Override
    public String version() {
        return "1.0";
    }

    @Action("praise")
    public String praise() {
        return "Praise the Lord!";
    }

    @Action("say")
    public String say() throws ApplicationException {
        if (null != getContext().getAttribute("--words"))
            return getContext().getAttribute("--words").toString();

        throw new ApplicationException("Could not find the parameter <i>words</i>.");
    }

    @Action("say")
    public String say(String words) {
        return words;
    }

    @Action(value = "hello", mode = Mode.HTTP_GET)
    public String helloGet() {
        return "GET";
    }

    @Action(value = "hello", mode = Mode.HTTP_POST)
    public String helloPost() {
        return "POST";
    }

}
```

Smalltalk: <a href="https://github.com/tinystruct/smalltalk">https://github.com/tinystruct/smalltalk</a>

Ejecutar en modo CLI
--
```tcsh
$ bin/dispatcher --version

  _/  '         _ _/  _     _ _/
  /  /  /) (/ _)  /  /  (/ (  /  1.7.27
           /
```
```tcsh
$ bin/dispatcher --help
Usage: bin/dispatcher COMMAND [OPTIONS]
A command line tool for tinystruct framework
Commands: 
        download        Download a resource from other servers
        exec            To execute native command(s)
        generate        POJO object generator
        install         Install a package
        maven-wrapper   Extract Maven Wrapper
        open            Start a default browser to open the specific URL
        say             Output words
        set             Set system property
        sql-execute     Executes the given SQL statement, which may be an INSERT, UPDATE, DELETE, or DDL statement
        sql-query       Executes the given SQL statement, which returns a single ResultSet object
        update          Update for latest version

Options: 
        --allow-remote-access   Allow to be accessed remotely
        --help                  Help command
        --host                  Host name / IP
        --import                Import application
        --logo                  Print logo
        --settings              Print settings
        --version               Print version

Run 'bin/dispatcher COMMAND --help' for more information on a command.
```
```tcsh
$ bin/dispatcher say/"Praise the Lord"
Praise the Lord
```
```tcsh
$ bin/dispatcher say --words Hello --import tinystruct.examples.example
Hello
```

Ejecutarlo en un servidor HTTP
--
```tcsh
# bin/dispatcher start --import org.tinystruct.system.HttpServer 
```
Puedes acceder a estas URL:

* <a href="http://localhost:8080/?q=say/Praise%20to%20the%20Lord!">http://localhost:8080/?q=say/Praise%20to%20the%20Lord! </a>
* <a href="http://localhost:8080/?q=praise">http://localhost:8080/?q=praise</a>

Prueba de rendimiento
--
```tcsh 
$ wrk -t12 -c400 -d30s "http://127.0.0.1:8080/?q=say/Praise the Lord!"

Running 30s test @ http://127.0.0.1:8080/?q=say/Praise the Lord!
12 threads and 400 connections

Thread Stats   Avg      Stdev     Max       +/- Stdev
Latency        17.44ms  33.42ms   377.73ms  88.98%
Req/Sec        7.27k    1.66k     13.55k    69.94%

2604473 requests in 30.02s, 524.09MB read

Requests/sec:  86753.98
Transfer/sec:  17.46MB
```

Procesar más de **86.000** solicitudes por segundo con baja latencia promedio, alrededor de 17,44 ms, indica que el endpoint es muy eficiente bajo carga intensa. Esto muestra la potencia y eficiencia del **framework tinystruct**, pero no se trata solo de números: también importa la **filosofía** detrás.

### ¿Qué hace moderno al framework tinystruct?

1. **No requiere método `main()`**
   Las aplicaciones pueden iniciarse directamente con comandos CLI como `bin/dispatcher`, sin código repetitivo.

2. **Diseño unificado para CLI y Web**
   **tinystruct** trata CLI y Web como ciudadanos de primera clase, ideal para tareas de IA, automatización de scripts y aplicaciones híbridas.

3. **Servidor HTTP ligero integrado**
   Ya sea Netty o Tomcat, tinystruct integra el ciclo de vida del servidor dentro del framework.

4. **Filosofía de configuración mínima**
   La configuración se limita a lo esencial, sin grandes cantidades de beans ni XML o YAML excesivos.

5. **Enrutamiento basado en anotaciones**
   `@Action` ofrece un mecanismo de enrutamiento claro e intuitivo.

6. **Arquitectura enfocada en rendimiento**
   Casi sin sobrecarga, sin escaneo de beans basado en reflexión ni interceptores innecesarios.

7. **Empoderamiento del desarrollador sin complejidad**
   tinystruct permite enfocarse en la **lógica de negocio real** con un diseño **transparente**, **predecible** y **extensible**.

---

Arquitectura
--
![tinystruct-framework-architecture](https://github.com/user-attachments/assets/33dbdc21-4803-4b94-bf69-bee053cf558d)

Licencia
--

Licenciado bajo Apache License, Version 2.0.
No puedes usar este archivo excepto de conformidad con la licencia.
Puedes obtener una copia en:

    http://www.apache.org/licenses/LICENSE-2.0

A menos que la ley aplicable lo exija o se acuerde por escrito, el software distribuido bajo la licencia se proporciona "TAL CUAL", sin garantías ni condiciones de ningún tipo.
