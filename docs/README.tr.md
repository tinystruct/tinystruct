`"Ne çok eserin var, ya RAB! Hepsini bilgelikle yaptın; yeryüzü yarattıklarınla dolu."`
***Mezmurlar 104:24***

Language: [English](../README.md) | [Português (Brasil)](README.pt-BR.md) | [简体中文](README.zh-CN.md) | [繁體中文](README.zh-TW.md) | [日本語](README.ja.md) | [한국어](README.ko.md) | [Türkçe](README.tr.md) | [Русский](README.ru.md) | [Tiếng Việt](README.vi.md) | [ไทย](README.th.md) | [Deutsch](README.de.md) | [Español](README.es.md)

tinystruct framework
--
Java geliştirme için basit bir framework. Basit düşünce, daha iyi tasarım, kolay kullanım ve iyi performans.

[![MvnRepository](https://badges.mvnrepository.com/badge/org.tinystruct/tinystruct/badge.svg?label=MvnRepository)](https://mvnrepository.com/artifact/org.tinystruct/tinystruct)
[![CodeQL](https://github.com/tinystruct/tinystruct/actions/workflows/codeql.yml/badge.svg)](https://github.com/tinystruct/tinystruct/actions/workflows/codeql.yml)

[![Star History Chart](https://api.star-history.com/svg?repos=tinystruct/tinystruct&type=Date)](https://www.star-history.com/#tinystruct/tinystruct&Date)

## Gereksinimler

- Java Development Kit (JDK) 17 veya üzeri
- Bağımlılık yönetimi için Maven
- IntelliJ IDEA, Eclipse veya VS Code gibi bir metin editörü ya da IDE

tinystruct archetype ile başlangıç
--
tinystruct tabanlı bir projeyi hızlıca oluşturmak için bu archetype kullanılabilir: https://github.com/tinystruct/tinystruct-archetype

Elle kurulum ve başlangıç
--
* Bağımlılığı pom.xml dosyana ekle.
```xml
<dependency>
  <groupId>org.tinystruct</groupId>
  <artifactId>tinystruct</artifactId>
  <version>1.7.27</version>
  <classifier>jar-with-dependencies</classifier> <!-- İsteğe bağlı -->
</dependency>
```

* Java'da AbstractApplication sınıfını genişlet:

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

CLI modunda çalıştırma
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

HTTP sunucusunda çalıştırma
--
```tcsh
# bin/dispatcher start --import org.tinystruct.system.HttpServer 
```
Aşağıdaki URL'lere erişebilirsin:

* <a href="http://localhost:8080/?q=say/Praise%20to%20the%20Lord!">http://localhost:8080/?q=say/Praise%20to%20the%20Lord! </a>
* <a href="http://localhost:8080/?q=praise">http://localhost:8080/?q=praise</a>

Performans testi
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

Düşük ortalama gecikmeyle, yaklaşık 17,44 ms, saniyede **86.000** üzerinde istek işlemek endpoint'in ağır yük altında çok verimli olduğunu gösterir. Bu, **tinystruct framework**'ün gücünü ve verimliliğini gösterir; ancak mesele yalnızca sayılar değil, arkasındaki **felsefedir**.

### tinystruct framework'ü modern yapan nedir?

1. **`main()` metodu gerekmez**
   Uygulamalar `bin/dispatcher` gibi CLI komutlarıyla doğrudan başlatılabilir.

2. **CLI ve Web için birleşik tasarım**
   **tinystruct**, CLI ve Web'i eşit önemde görür; yapay zeka görevleri, betik otomasyonu ve hibrit uygulamalar için uygundur.

3. **Yerleşik hafif HTTP sunucusu**
   Netty veya Tomcat kullanılsa da tinystruct sunucu yaşam döngüsünü framework içinde yönetir.

4. **Minimum yapılandırma felsefesi**
   Yapılandırma temel ihtiyaçlarla sınırlıdır; fazla bean, XML veya YAML gerekmez.

5. **Annotation tabanlı yönlendirme**
   `@Action`, temiz ve sezgisel bir yönlendirme mekanizması sunar.

6. **Performans öncelikli mimari**
   Neredeyse sıfır ek yük, refleksiyon tabanlı bean taraması yok ve gereksiz interceptor yoktur.

7. **Karmaşıklık olmadan geliştirici kontrolü**
   tinystruct, **gerçek iş mantığına** odaklanmayı sağlar; **şeffaf**, **öngörülebilir** ve **genişletilebilir** olacak şekilde tasarlanmıştır.

---

Mimari
--
![tinystruct-framework-architecture](https://github.com/user-attachments/assets/33dbdc21-4803-4b94-bf69-bee053cf558d)

Lisans
--

Apache License, Version 2.0 kapsamında lisanslanmıştır.
Bu dosyayı lisansa uygun olmadıkça kullanamazsın.
Lisansın bir kopyasını şu adresten alabilirsin:

    http://www.apache.org/licenses/LICENSE-2.0

Geçerli yasalar gerektirmedikçe veya yazılı olarak kabul edilmedikçe, lisans kapsamında dağıtılan yazılım herhangi bir garanti veya koşul olmadan "OLDUĞU GİBİ" sağlanır.
