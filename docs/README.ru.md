`"Как многочисленны дела Твои, Господи! Все соделал Ты премудро; земля полна Твоих созданий."`
***Псалом 103:24***

Language: [English](../README.md) | [Português (Brasil)](README.pt-BR.md) | [简体中文](README.zh-CN.md) | [繁體中文](README.zh-TW.md) | [日本語](README.ja.md) | [한국어](README.ko.md) | [Türkçe](README.tr.md) | [Русский](README.ru.md) | [Tiếng Việt](README.vi.md) | [ไทย](README.th.md) | [Deutsch](README.de.md) | [Español](README.es.md)

Фреймворк tinystruct
--
Простой фреймворк для разработки на Java. Простое мышление, лучший дизайн, удобство использования и хорошая производительность.

[![MvnRepository](https://badges.mvnrepository.com/badge/org.tinystruct/tinystruct/badge.svg?label=MvnRepository)](https://mvnrepository.com/artifact/org.tinystruct/tinystruct)
[![CodeQL](https://github.com/tinystruct/tinystruct/actions/workflows/codeql.yml/badge.svg)](https://github.com/tinystruct/tinystruct/actions/workflows/codeql.yml)

[![Star History Chart](https://api.star-history.com/svg?repos=tinystruct/tinystruct&type=Date)](https://www.star-history.com/#tinystruct/tinystruct&Date)

## Требования

- Java Development Kit (JDK) 17 или выше
- Maven для управления зависимостями
- Текстовый редактор или IDE, например IntelliJ IDEA, Eclipse или VS Code

Быстрый старт с archetype tinystruct
--
Этот archetype позволяет быстро создать проект на основе tinystruct: https://github.com/tinystruct/tinystruct-archetype

Ручная установка и начало работы
--
* Добавьте зависимость в pom.xml.
```xml
<dependency>
  <groupId>org.tinystruct</groupId>
  <artifactId>tinystruct</artifactId>
  <version>1.7.27</version>
  <classifier>jar-with-dependencies</classifier> <!-- Необязательно -->
</dependency>
```

* Наследуйте AbstractApplication в Java:

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

Запуск в режиме CLI
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

Запуск в HTTP-сервере
--
```tcsh
# bin/dispatcher start --import org.tinystruct.system.HttpServer 
```
Можно открыть следующие URL:

* <a href="http://localhost:8080/?q=say/Praise%20to%20the%20Lord!">http://localhost:8080/?q=say/Praise%20to%20the%20Lord! </a>
* <a href="http://localhost:8080/?q=praise">http://localhost:8080/?q=praise</a>

Тест производительности
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

Обработка более **86 000** запросов в секунду при низкой средней задержке около 17,44 мс показывает, что endpoint очень эффективен под высокой нагрузкой. Это демонстрирует мощность и эффективность **фреймворка tinystruct**, но дело не только в числах, а и в его **философии**.

### Что делает tinystruct современным?

1. **Не требуется метод `main()`**
   Приложения можно запускать напрямую через CLI-команды вроде `bin/dispatcher`.

2. **Единый дизайн для CLI и Web**
   **tinystruct** рассматривает CLI и Web как равноправные сценарии, что удобно для AI-задач, автоматизации скриптов и гибридных приложений.

3. **Встроенный легковесный HTTP-сервер**
   Netty или Tomcat интегрируются в жизненный цикл фреймворка.

4. **Минимальная конфигурация**
   Конфигурация сведена к необходимому минимуму, без избыточных beans, XML или YAML.

5. **Маршрутизация на основе аннотаций**
   `@Action` дает простой и понятный механизм маршрутизации.

6. **Архитектура с приоритетом производительности**
   Почти нет накладных расходов, нет reflection-based bean scanning и лишних interceptors.

7. **Контроль без лишней сложности**
   tinystruct помогает сосредоточиться на **реальной бизнес-логике** и остается **прозрачным**, **предсказуемым** и **расширяемым**.

---

Архитектура
--
![tinystruct-framework-architecture](https://github.com/tinystruct/tinystruct/assets/3631818/288049b7-cefd-4442-b6d8-8624ae75cdc2)

Лицензия
--

Лицензировано по Apache License, Version 2.0.
Вы не можете использовать этот файл иначе как в соответствии с лицензией.
Копию лицензии можно получить по адресу:

    http://www.apache.org/licenses/LICENSE-2.0

Если это не требуется законом или не согласовано письменно, программное обеспечение распространяется "КАК ЕСТЬ", без каких-либо гарантий или условий.
