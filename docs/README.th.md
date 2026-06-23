`"ข้าแต่พระยาห์เวห์ พระราชกิจของพระองค์มีมากมายเพียงใด! พระองค์ทรงสร้างสิ่งทั้งปวงด้วยพระปัญญา แผ่นดินโลกเต็มไปด้วยสิ่งที่พระองค์ทรงสร้าง"`
***สดุดี 104:24***

Language: [English](../README.md) | [Português (Brasil)](README.pt-BR.md) | [简体中文](README.zh-CN.md) | [繁體中文](README.zh-TW.md) | [日本語](README.ja.md) | [한국어](README.ko.md) | [Türkçe](README.tr.md) | [Русский](README.ru.md) | [Tiếng Việt](README.vi.md) | [ไทย](README.th.md) | [Deutsch](README.de.md) | [Español](README.es.md)

เฟรมเวิร์ก tinystruct
--
เฟรมเวิร์กที่เรียบง่ายสำหรับการพัฒนา Java แนวคิดเรียบง่าย ออกแบบดีขึ้น ใช้งานง่าย และมีประสิทธิภาพดี

[![MvnRepository](https://badges.mvnrepository.com/badge/org.tinystruct/tinystruct/badge.svg?label=MvnRepository)](https://mvnrepository.com/artifact/org.tinystruct/tinystruct)
[![CodeQL](https://github.com/tinystruct/tinystruct/actions/workflows/codeql.yml/badge.svg)](https://github.com/tinystruct/tinystruct/actions/workflows/codeql.yml)

[![Star History Chart](https://api.star-history.com/svg?repos=tinystruct/tinystruct&type=Date)](https://www.star-history.com/#tinystruct/tinystruct&Date)

## ข้อกำหนดเบื้องต้น

- Java Development Kit (JDK) 17 หรือสูงกว่า
- Maven สำหรับจัดการ dependencies
- Text editor หรือ IDE เช่น IntelliJ IDEA, Eclipse หรือ VS Code

เริ่มต้นด้วย tinystruct archetype
--
คุณสามารถใช้ archetype นี้เพื่อสร้างโปรเจกต์ที่ใช้ tinystruct ได้อย่างรวดเร็ว: https://github.com/tinystruct/tinystruct-archetype

การติดตั้งแบบ manual และเริ่มต้นใช้งาน
--
* เพิ่ม dependency ใน pom.xml
```xml
<dependency>
  <groupId>org.tinystruct</groupId>
  <artifactId>tinystruct</artifactId>
  <version>1.7.27</version>
  <classifier>jar-with-dependencies</classifier> <!-- ไม่บังคับ -->
</dependency>
```

* Extend AbstractApplication ใน Java:

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

รันในโหมด CLI
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

รันใน HTTP server
--
```tcsh
# bin/dispatcher start --import org.tinystruct.system.HttpServer 
```
คุณสามารถเข้าถึง URL ต่อไปนี้:

* <a href="http://localhost:8080/?q=say/Praise%20to%20the%20Lord!">http://localhost:8080/?q=say/Praise%20to%20the%20Lord! </a>
* <a href="http://localhost:8080/?q=praise">http://localhost:8080/?q=praise</a>

ทดสอบประสิทธิภาพ
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

การรองรับมากกว่า **86,000** requests ต่อวินาที พร้อมค่า latency เฉลี่ยต่ำประมาณ 17.44ms แสดงว่า endpoint มีประสิทธิภาพสูงภายใต้โหลดหนัก สิ่งนี้แสดงพลังและประสิทธิภาพของ **tinystruct framework** แต่ไม่ใช่แค่ตัวเลขเท่านั้น ยังเกี่ยวกับ **ปรัชญา** เบื้องหลังด้วย

### อะไรทำให้ tinystruct framework ทันสมัย?

1. **ไม่ต้องมีเมธอด `main()`**
   แอปพลิเคชันสามารถเริ่มได้โดยตรงผ่านคำสั่ง CLI เช่น `bin/dispatcher`

2. **ออกแบบรวมกันสำหรับ CLI และ Web**
   **tinystruct** ให้ความสำคัญกับ CLI และ Web อย่างเท่าเทียม เหมาะกับงาน AI, script automation และแอปพลิเคชันแบบ hybrid

3. **มี HTTP server แบบ lightweight ในตัว**
   ไม่ว่าจะใช้ Netty หรือ Tomcat, tinystruct รวม lifecycle ของ server ไว้ใน framework

4. **แนวคิด configuration ขั้นต่ำ**
   Configuration ถูกจำกัดไว้เฉพาะส่วนจำเป็น ไม่ต้องมี beans, XML หรือ YAML มากเกินไป

5. **Routing ด้วย annotation**
   `@Action` ให้กลไก routing ที่ชัดเจนและเข้าใจง่าย

6. **สถาปัตยกรรมที่ให้ความสำคัญกับ performance**
   Overhead ต่ำมาก ไม่มี reflection-based bean scanning และไม่มี interceptor ที่ไม่จำเป็น

7. **ให้อิสระกับนักพัฒนาโดยไม่เพิ่มความซับซ้อน**
   tinystruct ช่วยให้นักพัฒนาโฟกัสกับ **business logic จริง** และออกแบบให้ **โปร่งใส**, **คาดเดาได้**, และ **ขยายได้**

---

สถาปัตยกรรม
--
![tinystruct-framework-architecture](https://github.com/tinystruct/tinystruct/assets/3631818/288049b7-cefd-4442-b6d8-8624ae75cdc2)

สัญญาอนุญาต
--

Licensed under the Apache License, Version 2.0.
คุณไม่สามารถใช้ไฟล์นี้ได้ เว้นแต่จะเป็นไปตาม license.
สามารถดูสำเนา license ได้ที่:

    http://www.apache.org/licenses/LICENSE-2.0

เว้นแต่กฎหมายที่เกี่ยวข้องกำหนดไว้หรือมีข้อตกลงเป็นลายลักษณ์อักษร ซอฟต์แวร์ที่แจกจ่ายภายใต้ license นี้ให้มาแบบ "AS IS" โดยไม่มีการรับประกันหรือเงื่อนไขใด ๆ
