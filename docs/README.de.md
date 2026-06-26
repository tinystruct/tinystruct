`"Wie zahlreich sind deine Werke, HERR! Du hast sie alle in Weisheit gemacht; die Erde ist voll von deinen Geschöpfen."`
***Psalm 104:24***

Language: [English](../README.md) | [Português (Brasil)](README.pt-BR.md) | [简体中文](README.zh-CN.md) | [繁體中文](README.zh-TW.md) | [日本語](README.ja.md) | [한국어](README.ko.md) | [Türkçe](README.tr.md) | [Русский](README.ru.md) | [Tiếng Việt](README.vi.md) | [ไทย](README.th.md) | [Deutsch](README.de.md) | [Español](README.es.md)

Das tinystruct Framework
--
Ein einfaches Framework für die Java-Entwicklung. Einfaches Denken, besseres Design, leicht zu verwenden und mit guter Performance.

[![MvnRepository](https://badges.mvnrepository.com/badge/org.tinystruct/tinystruct/badge.svg?label=MvnRepository)](https://mvnrepository.com/artifact/org.tinystruct/tinystruct)
[![CodeQL](https://github.com/tinystruct/tinystruct/actions/workflows/codeql.yml/badge.svg)](https://github.com/tinystruct/tinystruct/actions/workflows/codeql.yml)

[![Star History Chart](https://api.star-history.com/svg?repos=tinystruct/tinystruct&type=Date)](https://www.star-history.com/#tinystruct/tinystruct&Date)

## Voraussetzungen

- Java Development Kit (JDK) 17 oder höher
- Maven für die Abhängigkeitsverwaltung
- Ein Texteditor oder eine IDE wie IntelliJ IDEA, Eclipse oder VS Code

Schnellstart mit dem tinystruct Archetype
--
Mit diesem Archetype kannst du schnell ein tinystruct-basiertes Projekt erstellen: https://github.com/tinystruct/tinystruct-archetype

Manuelle Installation und Einstieg
--
* Füge die Abhängigkeit zu deiner pom.xml hinzu.
```xml
<dependency>
  <groupId>org.tinystruct</groupId>
  <artifactId>tinystruct</artifactId>
  <version>1.7.27</version>
  <classifier>jar-with-dependencies</classifier> <!-- Optional -->
</dependency>
```

* Erweitere AbstractApplication in Java:

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

Im CLI-Modus ausführen
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

In einem HTTP-Server ausführen
--
```tcsh
# bin/dispatcher start --import org.tinystruct.system.HttpServer 
```
Du kannst diese URLs aufrufen:

* <a href="http://localhost:8080/?q=say/Praise%20to%20the%20Lord!">http://localhost:8080/?q=say/Praise%20to%20the%20Lord! </a>
* <a href="http://localhost:8080/?q=praise">http://localhost:8080/?q=praise</a>

Performance-Test
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

Mehr als **86.000** Anfragen pro Sekunde bei niedriger durchschnittlicher Latenz von etwa 17,44 ms zeigen, dass der Endpunkt unter hoher Last sehr effizient ist. Das zeigt die rohe Leistung und Effizienz des **tinystruct Frameworks**. Es geht aber nicht nur um Zahlen, sondern auch um die **Philosophie** dahinter.

### Was macht das tinystruct Framework modern?

1. **Keine `main()`-Methode erforderlich**
   Anwendungen können direkt mit CLI-Befehlen wie `bin/dispatcher` gestartet werden, ohne Boilerplate-Code.

2. **Einheitliches Design für CLI und Web**
   **tinystruct** behandelt CLI und Web als gleichwertige Einsatzbereiche. Das passt gut zu KI-Aufgaben, Skriptautomatisierung und hybriden Anwendungen.

3. **Eingebauter leichtgewichtiger HTTP-Server**
   Ob Netty oder Tomcat: tinystruct integriert den Server-Lebenszyklus im Framework. Importieren, was du brauchst, und ausführen.

4. **Philosophie minimaler Konfiguration**
   Die Konfiguration bleibt auf das Wesentliche beschränkt. Keine Massen an Beans, kein übermäßiges XML oder YAML.

5. **Annotation-basiertes Routing**
   `@Action` bietet einen klaren und intuitiven Routing-Mechanismus ohne komplexe Controller-Hierarchien.

6. **Performance-orientierte Architektur**
   Sehr geringer Overhead, keine reflektionsbasierte Bean-Suche und keine unnötigen Interceptors, sofern sie nicht explizit aktiviert werden.

7. **Mehr Kontrolle ohne unnötige Komplexität**
   Entwickler konzentrieren sich auf **echte Geschäftslogik**. tinystruct ist **transparent**, **vorhersehbar** und **erweiterbar**.

---

Architektur
--
![tinystruct-framework-architecture](https://github.com/user-attachments/assets/33dbdc21-4803-4b94-bf69-bee053cf558d)

Lizenz
--

Lizenziert unter der Apache License, Version 2.0.
Du darfst diese Datei nur gemäß der Lizenz verwenden.
Eine Kopie der Lizenz erhältst du unter:

    http://www.apache.org/licenses/LICENSE-2.0

Sofern nicht gesetzlich vorgeschrieben oder schriftlich vereinbart, wird die Software unter der Lizenz ohne Gewährleistung oder Bedingungen jeglicher Art bereitgestellt.
