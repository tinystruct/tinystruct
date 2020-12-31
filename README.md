
"How many are your works, O LORD ! In wisdom you made them all; the earth is full of your creatures."
Psalms 104:24

tinystruct
---
A simple framework for Java development. Simple is hard, Complex is easy. Better thinking, better design.

To execute it in CLI mode
---
```tcsh
$ bin/dispatcher --version

  _/  '         _ _/  _     _ _/
  /  /  /) (/ _)  /  /  (/ (  /  2.0
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
---
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