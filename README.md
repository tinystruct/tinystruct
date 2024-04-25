
`"How many are your works, O LORD ! In wisdom you made them all; the earth is full of your creatures."`
***Psalms 104:24***

The tinystruct framework
--
A simple framework for Java development. Simple thinking, Better design, Easy to be used with better performance! 

Installation and Getting Started
--
* Add the dependency into your pom.xml.
```xml
<dependency>
  <groupId>org.tinystruct</groupId>
  <artifactId>tinystruct</artifactId>
  <version>1.2.6</version>
  <classifier>jar-with-dependencies</classifier>
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
        return "Praise to the Lord!";
    }

    @Action("say")
    public String say() throws ApplicationException {
        if (null != this.context.getAttribute("--words"))
            return this.context.getAttribute("--words").toString();

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
  /  /  /) (/ _)  /  /  (/ (  /  1.2.6
           /
```
```tcsh
$ bin/dispatcher --help
Usage: bin/dispatcher COMMAND [OPTIONS]
Commands: 
        download                Download a resource from other servers
        exec                    To execute native command(s)
        generate                POJO object generator
        install                 Install a package
        open                    Start a default browser to open the specific URL
        say                     Output words
        set                     Set system property
        sql-execute             Executes the given SQL statement, which may be an INSERT, UPDATE, or DELETE statement or an SQL statement that returns nothing, such as an SQL DDL statement.
        sql-query               Executes the given SQL statement, which returns a single ResultSet object.
        update                  Update for latest version

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
$ bin/dispatcher say/"Praise to the Lord"
Praise to the Lord
```
```tcsh
$ bin/dispatcher say --words Hello --import tinystruct.examples.example
Hello
```

Run it in a http server based on netty
--
```tcsh
# bin/dispatcher start --import org.tinystruct.system.NettyHttpServer 
```
You can access the below URLs:

* <a href="http://localhost:8080/?q=say/Praise%20to%20the%20Lord!">http://localhost:8080/?q=say/Praise%20to%20the%20Lord! </a>
* <a href="http://localhost:8080/?q=praise">http://localhost:8080/?q=praise</a>

Architecture
--
![tinystruct-framework-architecture](https://github.com/tinystruct/tinystruct/assets/3631818/fa4ba67c-c175-4c40-9434-e2931e998b4c)

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
