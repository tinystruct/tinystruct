
`"How many are your works, O LORD ! In wisdom you made them all; the earth is full of your creatures."`
***Psalms 104:24***

The tinystruct framework
--
A simple framework for Java development. Simple thinking, Better design, Easy to be used with better performance! 

[![Star History Chart](https://api.star-history.com/svg?repos=tinystruct/tinystruct&type=Date)](https://www.star-history.com/#tinystruct/tinystruct&Date)

## Prerequisites

- Java Development Kit (JDK) 17 or higher
- Maven (for dependency management)
- A text editor or IDE (IntelliJ IDEA, Eclipse, VS Code, etc.)

Installation and Getting Started
--
* Add the dependency into your pom.xml.
```xml
<dependency>
  <groupId>org.tinystruct</groupId>
  <artifactId>tinystruct</artifactId>
  <version>1.7.10</version>
  <classifier>jar-with-dependencies</classifier> <!-- Optional -->
</dependency>
```

* Extend the AbstractApplication in Java:

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

}

```
Smalltalk: <a href="https://github.com/tinystruct/smalltalk">https://github.com/tinystruct/smalltalk</a>

Execute in CLI mode
--
```tcsh
$ bin/dispatcher --version

  _/  '         _ _/  _     _ _/
  /  /  /) (/ _)  /  /  (/ (  /  1.7.10
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

Run it in a http server
--
```tcsh
# bin/dispatcher start --import org.tinystruct.system.HttpServer 
```
You can access the below URLs:

* <a href="http://localhost:8080/?q=say/Praise%20to%20the%20Lord!">http://localhost:8080/?q=say/Praise%20to%20the%20Lord! </a>
* <a href="http://localhost:8080/?q=praise">http://localhost:8080/?q=praise</a>

Performance Test
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
Handling over **86,000** requests per second with low average latency (~17.44ms), indicating the endpoint is highly efficient under heavy load. this shows the raw power and efficiency of the **tinystruct framework**. But it's not just about the performance numbers. It's about the **philosophy** behind it.

### What makes tinystruct framework modern?

1. **No `main()` method required**
   Applications can be started directly using CLI commands like `bin/dispatcher`, with no boilerplate code needed. This removes unnecessary ceremony from the development lifecycle.

2. **Unified design for CLI and Web**
   Unlike Spring Boot which is primarily web-centric, **tinystruct** treats CLI and Web as equal citizens. This makes it perfect for AI tasks, script automation, and hybrid applications — all from the same codebase.

3. **Built-in lightweight HTTP server**
   Whether it’s Netty or Tomcat, tinystruct integrates the server lifecycle inside the framework. There's no need for separate containers or complicated configuration files. Just import what you need and run.

4. **Minimal configuration philosophy**
   Configuration is minimized to the essentials. You don't need to wire up hundreds of beans, and there's no excessive XML or YAML involved. This improves developer productivity and reduces bugs.

5. **Annotation-based routing**
   The framework provides a clean and intuitive routing mechanism using `@Action`, eliminating the need for overly complex controller hierarchies.

6. **Performance-first architecture**
   There’s almost zero overhead. No reflection-based bean scanning, no auto-wiring maze, no unnecessary interceptors unless explicitly enabled. This translates into faster response times and smaller memory footprint.

7. **Developer empowerment without complexity**
   With tinystruct, developers are free to focus on **real business logic** rather than fighting with framework mechanics. It's designed to be **transparent**, **predictable**, and **extensible** — all without sacrificing control or performance.

---

Architecture
--
![tinystruct-framework-architecture](https://github.com/tinystruct/tinystruct/assets/3631818/288049b7-cefd-4442-b6d8-8624ae75cdc2)

License
--

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
