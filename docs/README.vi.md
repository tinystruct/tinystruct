`"Lạy Đức Giê-hô-va, công việc Ngài nhiều biết bao! Ngài đã dựng nên tất cả bằng sự khôn ngoan; đất đầy dẫy tạo vật của Ngài."`
***Thi Thiên 104:24***

Language: [English](../README.md) | [Português (Brasil)](README.pt-BR.md) | [简体中文](README.zh-CN.md) | [繁體中文](README.zh-TW.md) | [日本語](README.ja.md) | [한국어](README.ko.md) | [Türkçe](README.tr.md) | [Русский](README.ru.md) | [Tiếng Việt](README.vi.md) | [ไทย](README.th.md) | [Deutsch](README.de.md) | [Español](README.es.md)

Framework tinystruct
--
Một framework đơn giản cho phát triển Java. Tư duy đơn giản, thiết kế tốt hơn, dễ sử dụng và có hiệu năng tốt.

[![MvnRepository](https://badges.mvnrepository.com/badge/org.tinystruct/tinystruct/badge.svg?label=MvnRepository)](https://mvnrepository.com/artifact/org.tinystruct/tinystruct)
[![CodeQL](https://github.com/tinystruct/tinystruct/actions/workflows/codeql.yml/badge.svg)](https://github.com/tinystruct/tinystruct/actions/workflows/codeql.yml)

[![Star History Chart](https://api.star-history.com/svg?repos=tinystruct/tinystruct&type=Date)](https://www.star-history.com/#tinystruct/tinystruct&Date)

## Yêu cầu

- Java Development Kit (JDK) 17 trở lên
- Maven để quản lý phụ thuộc
- Trình soạn thảo văn bản hoặc IDE như IntelliJ IDEA, Eclipse hoặc VS Code

Bắt đầu với tinystruct archetype
--
Bạn có thể dùng archetype này để nhanh chóng tạo dự án dựa trên tinystruct: https://github.com/tinystruct/tinystruct-archetype

Cài đặt thủ công và bắt đầu
--
* Thêm dependency vào pom.xml.
```xml
<dependency>
  <groupId>org.tinystruct</groupId>
  <artifactId>tinystruct</artifactId>
  <version>1.7.27</version>
  <classifier>jar-with-dependencies</classifier> <!-- Tùy chọn -->
</dependency>
```

* Kế thừa AbstractApplication trong Java:

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

Chạy ở chế độ CLI
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

Chạy trong HTTP server
--
```tcsh
# bin/dispatcher start --import org.tinystruct.system.HttpServer 
```
Bạn có thể truy cập các URL sau:

* <a href="http://localhost:8080/?q=say/Praise%20to%20the%20Lord!">http://localhost:8080/?q=say/Praise%20to%20the%20Lord! </a>
* <a href="http://localhost:8080/?q=praise">http://localhost:8080/?q=praise</a>

Kiểm thử hiệu năng
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

Xử lý hơn **86.000** request mỗi giây với độ trễ trung bình thấp, khoảng 17,44 ms, cho thấy endpoint rất hiệu quả dưới tải cao. Điều này thể hiện sức mạnh và hiệu quả của **framework tinystruct**, nhưng không chỉ là con số; còn là **triết lý** phía sau nó.

### Điều gì làm tinystruct hiện đại?

1. **Không cần phương thức `main()`**
   Ứng dụng có thể khởi động trực tiếp bằng lệnh CLI như `bin/dispatcher`.

2. **Thiết kế thống nhất cho CLI và Web**
   **tinystruct** xem CLI và Web là hai môi trường ngang hàng, phù hợp cho tác vụ AI, tự động hóa script và ứng dụng lai.

3. **HTTP server nhẹ tích hợp sẵn**
   Dù dùng Netty hay Tomcat, tinystruct tích hợp vòng đời server vào framework.

4. **Triết lý cấu hình tối thiểu**
   Cấu hình chỉ giữ những phần cần thiết, không cần quá nhiều bean, XML hoặc YAML.

5. **Routing dựa trên annotation**
   `@Action` cung cấp cơ chế routing rõ ràng và trực quan.

6. **Kiến trúc ưu tiên hiệu năng**
   Gần như không có overhead, không quét bean bằng reflection và không có interceptor không cần thiết.

7. **Trao quyền cho lập trình viên mà không phức tạp**
   tinystruct giúp tập trung vào **logic nghiệp vụ thật**, với thiết kế **minh bạch**, **dễ dự đoán** và **có thể mở rộng**.

---

Kiến trúc
--
![tinystruct-framework-architecture](https://github.com/tinystruct/tinystruct/assets/3631818/288049b7-cefd-4442-b6d8-8624ae75cdc2)

Giấy phép
--

Được cấp phép theo Apache License, Version 2.0.
Bạn chỉ được sử dụng tệp này theo đúng giấy phép.
Bạn có thể lấy bản sao giấy phép tại:

    http://www.apache.org/licenses/LICENSE-2.0

Trừ khi luật hiện hành yêu cầu hoặc có thỏa thuận bằng văn bản, phần mềm được phân phối theo giấy phép này được cung cấp "NGUYÊN TRẠNG", không có bảo đảm hoặc điều kiện dưới bất kỳ hình thức nào.
