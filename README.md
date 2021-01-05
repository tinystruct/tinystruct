
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
  <version>0.1.3</version>
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
  /  /  /) (/ _)  /  /  (/ (  /  0.1.3
           /
```
```tcsh
$ bin/dispatcher --help
Usage:	dispatcher [--attributes] [actions[/args...]...]
	where attributes include any custom attributes those defined in context 
	or keypair parameters are going to be passed by context,
 	such as: 
	--http.proxyHost=127.0.0.1 or --http.proxyPort=3128 or --param=value
	
$ bin/dispatcher say/"Praise to the Lord"
Praise to the Lord
```


Run it in a servlet container
--
```tcsh
# bin/dispatcher --start-server --import-applications=org.tinystruct.system.TomcatServer
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
