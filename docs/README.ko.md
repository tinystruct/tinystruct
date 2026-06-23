`"여호와여 주께서 하신 일이 어찌 그리 많은지요! 주께서 지혜로 그들을 다 지으셨으니 땅에는 주의 피조물이 가득합니다."`
***시편 104:24***

Language: [English](../README.md) | [Português (Brasil)](README.pt-BR.md) | [简体中文](README.zh-CN.md) | [繁體中文](README.zh-TW.md) | [日本語](README.ja.md) | [한국어](README.ko.md) | [Türkçe](README.tr.md) | [Русский](README.ru.md) | [Tiếng Việt](README.vi.md) | [ไทย](README.th.md) | [Deutsch](README.de.md) | [Español](README.es.md)

tinystruct 프레임워크
--
Java 개발을 위한 단순한 프레임워크입니다. 단순한 사고, 더 나은 설계, 쉬운 사용성, 좋은 성능을 지향합니다.

[![MvnRepository](https://badges.mvnrepository.com/badge/org.tinystruct/tinystruct/badge.svg?label=MvnRepository)](https://mvnrepository.com/artifact/org.tinystruct/tinystruct)
[![CodeQL](https://github.com/tinystruct/tinystruct/actions/workflows/codeql.yml/badge.svg)](https://github.com/tinystruct/tinystruct/actions/workflows/codeql.yml)

[![Star History Chart](https://api.star-history.com/svg?repos=tinystruct/tinystruct&type=Date)](https://www.star-history.com/#tinystruct/tinystruct&Date)

## 사전 요구 사항

- Java Development Kit (JDK) 17 이상
- 의존성 관리를 위한 Maven
- IntelliJ IDEA, Eclipse, VS Code 같은 텍스트 편집기 또는 IDE

tinystruct archetype으로 시작하기
--
이 archetype을 사용하면 tinystruct 기반 프로젝트를 빠르게 만들 수 있습니다: https://github.com/tinystruct/tinystruct-archetype

수동 설치 및 시작하기
--
* pom.xml에 의존성을 추가합니다.
```xml
<dependency>
  <groupId>org.tinystruct</groupId>
  <artifactId>tinystruct</artifactId>
  <version>1.7.27</version>
  <classifier>jar-with-dependencies</classifier> <!-- 선택 사항 -->
</dependency>
```

* Java에서 AbstractApplication을 확장합니다:

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

CLI 모드로 실행
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

HTTP 서버에서 실행
--
```tcsh
# bin/dispatcher start --import org.tinystruct.system.HttpServer 
```
아래 URL에 접근할 수 있습니다:

* <a href="http://localhost:8080/?q=say/Praise%20to%20the%20Lord!">http://localhost:8080/?q=say/Praise%20to%20the%20Lord! </a>
* <a href="http://localhost:8080/?q=praise">http://localhost:8080/?q=praise</a>

성능 테스트
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

평균 지연 시간 약 17.44ms의 낮은 지연으로 초당 **86,000**건 이상의 요청을 처리한다는 것은 endpoint가 높은 부하에서도 매우 효율적임을 보여줍니다. 이는 **tinystruct 프레임워크**의 성능과 효율을 보여주지만, 핵심은 숫자만이 아니라 그 뒤의 **철학**입니다.

### tinystruct 프레임워크가 현대적인 이유

1. **`main()` 메서드가 필요 없음**
   애플리케이션은 `bin/dispatcher` 같은 CLI 명령으로 바로 시작할 수 있습니다.

2. **CLI와 Web을 위한 통합 설계**
   **tinystruct**는 CLI와 Web을 동등하게 다루며 AI 작업, 스크립트 자동화, 하이브리드 애플리케이션에 적합합니다.

3. **내장 경량 HTTP 서버**
   Netty든 Tomcat이든 tinystruct는 서버 생명주기를 프레임워크 내부에 통합합니다.

4. **최소 설정 철학**
   설정은 필수 요소로 제한되며 과도한 bean, XML, YAML이 필요하지 않습니다.

5. **애너테이션 기반 라우팅**
   `@Action`은 명확하고 직관적인 라우팅 메커니즘을 제공합니다.

6. **성능 우선 아키텍처**
   오버헤드가 거의 없고, reflection 기반 bean scanning이나 불필요한 interceptor가 없습니다.

7. **복잡성 없이 개발자에게 제어권 제공**
   tinystruct는 **실제 비즈니스 로직**에 집중하도록 돕고, **투명하며**, **예측 가능하고**, **확장 가능**하게 설계되었습니다.

---

아키텍처
--
![tinystruct-framework-architecture](https://github.com/tinystruct/tinystruct/assets/3631818/288049b7-cefd-4442-b6d8-8624ae75cdc2)

라이선스
--

Apache License, Version 2.0에 따라 라이선스가 부여됩니다.
이 라이선스를 준수하지 않는 한 이 파일을 사용할 수 없습니다.
라이선스 사본은 다음에서 확인할 수 있습니다:

    http://www.apache.org/licenses/LICENSE-2.0

관련 법률이 요구하거나 서면으로 합의한 경우를 제외하고, 이 라이선스에 따라 배포되는 소프트웨어는 어떠한 보증이나 조건 없이 "있는 그대로" 제공됩니다.
