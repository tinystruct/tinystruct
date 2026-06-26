`"耶和华啊，你所造的何其多！都是你用智慧造成的；遍地满了你的丰富。"`
***诗篇 104:24***

Language: [English](../README.md) | [Português (Brasil)](README.pt-BR.md) | [简体中文](README.zh-CN.md) | [繁體中文](README.zh-TW.md) | [日本語](README.ja.md) | [한국어](README.ko.md) | [Türkçe](README.tr.md) | [Русский](README.ru.md) | [Tiếng Việt](README.vi.md) | [ไทย](README.th.md) | [Deutsch](README.de.md) | [Español](README.es.md)

tinystruct 框架
--
一个用于 Java 开发的简单框架。理念简洁，设计更优，易于使用，并具备更好的性能。

[![MvnRepository](https://badges.mvnrepository.com/badge/org.tinystruct/tinystruct/badge.svg?label=MvnRepository)](https://mvnrepository.com/artifact/org.tinystruct/tinystruct)
[![CodeQL](https://github.com/tinystruct/tinystruct/actions/workflows/codeql.yml/badge.svg)](https://github.com/tinystruct/tinystruct/actions/workflows/codeql.yml)

[![Star History Chart](https://api.star-history.com/svg?repos=tinystruct/tinystruct&type=Date)](https://www.star-history.com/#tinystruct/tinystruct&Date)

## 前置条件

- Java Development Kit (JDK) 17 或更高版本
- Maven（用于依赖管理）
- 文本编辑器或 IDE（IntelliJ IDEA、Eclipse、VS Code 等）

使用 tinystruct archetype 快速开始
--
你可以按照这个 archetype 快速创建基于 tinystruct 的项目：https://github.com/tinystruct/tinystruct-archetype

手动安装与入门
--
* 将依赖添加到你的 pom.xml。
```xml
<dependency>
  <groupId>org.tinystruct</groupId>
  <artifactId>tinystruct</artifactId>
  <version>1.7.27</version>
  <classifier>jar-with-dependencies</classifier> <!-- 可选 -->
</dependency>
```

* 在 Java 中继承 AbstractApplication：

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

以 CLI 模式执行
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

在 HTTP 服务器中运行
--
```tcsh
# bin/dispatcher start --import org.tinystruct.system.HttpServer 
```
你可以访问以下 URL：

* <a href="http://localhost:8080/?q=say/Praise%20to%20the%20Lord!">http://localhost:8080/?q=say/Praise%20to%20the%20Lord! </a>
* <a href="http://localhost:8080/?q=praise">http://localhost:8080/?q=praise</a>

性能测试
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
在较低平均延迟（约 17.44ms）下每秒处理超过 **86,000** 个请求，说明该端点在高负载下依然非常高效。这展示了 **tinystruct 框架** 的原始性能与效率。但重点并不只是性能数字，还在于它背后的**设计哲学**。

### tinystruct 框架为什么是现代化的？

1. **不需要 `main()` 方法**
   应用可以直接通过 `bin/dispatcher` 等 CLI 命令启动，无需样板代码。这减少了开发生命周期中不必要的形式化步骤。

2. **CLI 与 Web 的统一设计**
   不同于主要面向 Web 的 Spring Boot，**tinystruct** 将 CLI 与 Web 视为同等重要的一等能力。这使它非常适合 AI 任务、脚本自动化和混合型应用，并且可以共用同一套代码库。

3. **内置轻量级 HTTP 服务器**
   无论使用 Netty 还是 Tomcat，tinystruct 都把服务器生命周期集成在框架内部。无需单独的容器，也不需要复杂的配置文件。只需导入所需组件即可运行。

4. **最小化配置理念**
   配置被压缩到必要范围。你不需要装配大量 bean，也不会陷入过度的 XML 或 YAML 配置。这能提升开发效率，并减少错误。

5. **基于注解的路由**
   框架通过 `@Action` 提供清晰直观的路由机制，避免过于复杂的控制器层级。

6. **性能优先的架构**
   框架开销几乎为零。没有基于反射的 bean 扫描，没有自动装配迷宫，也不会启用非必要的拦截器，除非你明确开启。这会带来更快的响应时间和更小的内存占用。

7. **赋能开发者，同时保持简单**
   使用 tinystruct，开发者可以专注于**真实业务逻辑**，而不是与框架机制纠缠。它被设计为**透明**、**可预测**、**可扩展**，同时不牺牲控制力和性能。

---

架构
--
![tinystruct-framework-architecture](https://github.com/user-attachments/assets/33dbdc21-4803-4b94-bf69-bee053cf558d)

许可证
--

本项目基于 Apache License, Version 2.0（“License”）授权；
除非符合该许可证，否则你不得使用本文件。
你可以在以下地址获取许可证副本：

    http://www.apache.org/licenses/LICENSE-2.0

除非适用法律要求或书面同意，按该许可证分发的软件均按“原样”提供，
不附带任何明示或暗示的担保或条件。
有关许可证下权限和限制的具体条款，请参阅该许可证。
