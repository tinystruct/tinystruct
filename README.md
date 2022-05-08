
"How many are your works, O LORD ! In wisdom you made them all; the earth is full of your creatures."
Psalms 104:24

The tinystruct framework
--
A simple framework for Java development. Simple thinking, Better design, Easy to be used to have better performance! 

Installation and Getting Started
--
* Add the dependency into your pom.xml.
```xml
<dependency>
  <groupId>org.tinystruct</groupId>
  <artifactId>tinystruct</artifactId>
  <version>0.3.8</version>
  <classifier>jar-with-dependencies</classifier>
</dependency>
```

* Extend the AbstractApplication in Java:

```java
import org.tinystruct.AbstractApplication;

public class Example extends AbstractApplication {

    @Override
    public void init() {
        // TODO Auto-generated method stub
        this.setAction("praise", "praise");
        this.setAction("say", "say");
        this.setAction("smile", "smile");
    }

    @Override
    public String version() {
        return "1.0";
    }
    
    public String praise() {
        return "Praise to the Lord!";
    }

    public String say() {
        if(null != this.context.getParameter("words"))
        return this.context.getParameter("words").toString();

        return "Invalid parameter(s).";
    }

    public String say(String words) {
        return words;
    }
    
    public String smile() {
        return ":)";
    }

}
```

Execute in CLI mode
--
```tcsh
$ bin/dispatcher --version

  _/  '         _ _/  _     _ _/
  /  /  /) (/ _)  /  /  (/ (  /  0.3.8
           /
```
```tcsh
$ bin/dispatcher --help
Usage: bin/dispatcher COMMAND [OPTIONS]
Commands: 
        download        Download a resource from other servers
        exec            To execute native command(s)
        install         Install a package
        say             Output words
        set             Set system property
        update          Update for latest version

Options: 
        --help          Help command
        --import        Import application
        --logo          Print logo
        --settings      Print settings
        --version       Print version

Run 'bin/dispatcher COMMAND --help' for more information on a command.
	
$ bin/dispatcher say/"Praise to the Lord"
Praise to the Lord
```


Run it in a http server based on netty
--
```tcsh
# bin/dispatcher start --import org.tinystruct.system.NettyHttpServer
```


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
