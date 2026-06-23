`"耶和華啊，你所造的何其多！都是你用智慧造成的；遍地滿了你的豐富。"`
***詩篇 104:24***

Language: [English](../README.md) | [Português (Brasil)](README.pt-BR.md) | [简体中文](README.zh-CN.md) | [繁體中文](README.zh-TW.md) | [日本語](README.ja.md) | [한국어](README.ko.md) | [Türkçe](README.tr.md) | [Русский](README.ru.md) | [Tiếng Việt](README.vi.md) | [ไทย](README.th.md) | [Deutsch](README.de.md) | [Español](README.es.md)

tinystruct 框架
--
一個用於 Java 開發的簡潔框架。理念簡單、設計更好、易於使用，並具備良好的效能。

[![MvnRepository](https://badges.mvnrepository.com/badge/org.tinystruct/tinystruct/badge.svg?label=MvnRepository)](https://mvnrepository.com/artifact/org.tinystruct/tinystruct)
[![CodeQL](https://github.com/tinystruct/tinystruct/actions/workflows/codeql.yml/badge.svg)](https://github.com/tinystruct/tinystruct/actions/workflows/codeql.yml)

[![Star History Chart](https://api.star-history.com/svg?repos=tinystruct/tinystruct&type=Date)](https://www.star-history.com/#tinystruct/tinystruct&Date)

## 前置需求

- Java Development Kit (JDK) 17 或更高版本
- Maven，用於相依性管理
- 文字編輯器或 IDE，例如 IntelliJ IDEA、Eclipse 或 VS Code

使用 tinystruct archetype 快速開始
--
你可以使用這個 archetype 快速建立 tinystruct 專案：https://github.com/tinystruct/tinystruct-archetype

手動安裝與入門
--
* 將相依性加入 pom.xml。
```xml
<dependency>
  <groupId>org.tinystruct</groupId>
  <artifactId>tinystruct</artifactId>
  <version>1.7.27</version>
  <classifier>jar-with-dependencies</classifier> <!-- 選用 -->
</dependency>
```

* 在 Java 中繼承 AbstractApplication：

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

Smalltalk：<a href="https://github.com/tinystruct/smalltalk">https://github.com/tinystruct/smalltalk</a>

以 CLI 模式執行
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

在 HTTP 伺服器中執行
--
```tcsh
# bin/dispatcher start --import org.tinystruct.system.HttpServer 
```
你可以存取以下 URL：

* <a href="http://localhost:8080/?q=say/Praise%20to%20the%20Lord!">http://localhost:8080/?q=say/Praise%20to%20the%20Lord! </a>
* <a href="http://localhost:8080/?q=praise">http://localhost:8080/?q=praise</a>

效能測試
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

在較低平均延遲約 17.44ms 下，每秒處理超過 **86,000** 個請求，表示該端點在高負載下仍非常高效。這展現了 **tinystruct 框架** 的原始效能與效率，但重點不只是數字，也在於背後的**設計哲學**。

### tinystruct 框架為什麼現代？

1. **不需要 `main()` 方法**
   應用程式可以直接透過 `bin/dispatcher` 等 CLI 命令啟動，不需要樣板程式碼。

2. **CLI 與 Web 的統一設計**
   **tinystruct** 將 CLI 與 Web 視為同等重要，適合 AI 任務、腳本自動化與混合型應用。

3. **內建輕量級 HTTP 伺服器**
   不論使用 Netty 或 Tomcat，tinystruct 都將伺服器生命週期整合到框架中。

4. **最小化設定理念**
   設定僅保留必要部分，不需要大量 bean，也不需要過度的 XML 或 YAML。

5. **基於註解的路由**
   `@Action` 提供清晰直觀的路由機制。

6. **效能優先架構**
   幾乎沒有額外開銷，沒有基於反射的 bean 掃描，也沒有非必要的攔截器。

7. **賦能開發者且保持簡單**
   tinystruct 讓開發者專注於**真實業務邏輯**，並保持**透明**、**可預測**、**可擴充**。

---

架構
--
![tinystruct-framework-architecture](https://github.com/tinystruct/tinystruct/assets/3631818/288049b7-cefd-4442-b6d8-8624ae75cdc2)

授權
--

本專案依 Apache License, Version 2.0 授權。
除非符合該授權，否則不得使用本檔案。
你可以在以下位置取得授權副本：

    http://www.apache.org/licenses/LICENSE-2.0

除非法律要求或書面同意，依本授權散布的軟體均以「原樣」提供，不附帶任何明示或暗示的擔保或條件。
