`"主よ、あなたのみわざはなんと多いことでしょう。あなたはそれらをみな知恵をもって造られました。地はあなたの造られたもので満ちています。"`
***詩篇 104:24***

Language: [English](../README.md) | [Português (Brasil)](README.pt-BR.md) | [简体中文](README.zh-CN.md) | [繁體中文](README.zh-TW.md) | [日本語](README.ja.md) | [한국어](README.ko.md) | [Türkçe](README.tr.md) | [Русский](README.ru.md) | [Tiếng Việt](README.vi.md) | [ไทย](README.th.md) | [Deutsch](README.de.md) | [Español](README.es.md)

tinystruct フレームワーク
--
Java 開発のためのシンプルなフレームワークです。シンプルな考え方、より良い設計、使いやすさ、そして高い性能を目指しています。

[![MvnRepository](https://badges.mvnrepository.com/badge/org.tinystruct/tinystruct/badge.svg?label=MvnRepository)](https://mvnrepository.com/artifact/org.tinystruct/tinystruct)
[![CodeQL](https://github.com/tinystruct/tinystruct/actions/workflows/codeql.yml/badge.svg)](https://github.com/tinystruct/tinystruct/actions/workflows/codeql.yml)

[![Star History Chart](https://api.star-history.com/svg?repos=tinystruct/tinystruct&type=Date)](https://www.star-history.com/#tinystruct/tinystruct&Date)

## 前提条件

- Java Development Kit (JDK) 17 以上
- 依存関係管理のための Maven
- IntelliJ IDEA、Eclipse、VS Code などのテキストエディタまたは IDE

tinystruct archetype で始める
--
この archetype を使うと、tinystruct ベースのプロジェクトをすばやく作成できます: https://github.com/tinystruct/tinystruct-archetype

手動インストールと入門
--
* pom.xml に依存関係を追加します。
```xml
<dependency>
  <groupId>org.tinystruct</groupId>
  <artifactId>tinystruct</artifactId>
  <version>1.7.27</version>
  <classifier>jar-with-dependencies</classifier> <!-- 任意 -->
</dependency>
```

* Java で AbstractApplication を継承します:

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

CLI モードで実行
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

HTTP サーバーで実行
--
```tcsh
# bin/dispatcher start --import org.tinystruct.system.HttpServer 
```
以下の URL にアクセスできます:

* <a href="http://localhost:8080/?q=say/Praise%20to%20the%20Lord!">http://localhost:8080/?q=say/Praise%20to%20the%20Lord! </a>
* <a href="http://localhost:8080/?q=praise">http://localhost:8080/?q=praise</a>

性能テスト
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

平均レイテンシ約 17.44ms という低い遅延で **86,000** を超えるリクエストを毎秒処理できることは、このエンドポイントが高負荷下でも非常に効率的であることを示しています。これは **tinystruct フレームワーク** の性能と効率を示すものですが、重要なのは数値だけではなく、その背後にある**思想**です。

### tinystruct フレームワークが現代的な理由

1. **`main()` メソッドが不要**
   アプリケーションは `bin/dispatcher` などの CLI コマンドから直接起動できます。

2. **CLI と Web の統一設計**
   **tinystruct** は CLI と Web を同等に扱い、AI タスク、スクリプト自動化、ハイブリッドアプリケーションに適しています。

3. **軽量 HTTP サーバーを内蔵**
   Netty でも Tomcat でも、サーバーのライフサイクルをフレームワーク内に統合します。

4. **最小限の設定思想**
   設定は必要最小限で、過剰な bean、XML、YAML は不要です。

5. **アノテーションベースのルーティング**
   `@Action` により、明確で直感的なルーティングを実現します。

6. **性能優先のアーキテクチャ**
   オーバーヘッドはほぼなく、リフレクションベースの bean スキャンや不要な interceptor はありません。

7. **複雑さを増やさず開発者を支援**
   tinystruct は**実際のビジネスロジック**に集中できるように設計され、**透明**で**予測可能**かつ**拡張可能**です。

---

アーキテクチャ
--
![tinystruct-framework-architecture](https://github.com/user-attachments/assets/33dbdc21-4803-4b94-bf69-bee053cf558d)

ライセンス
--

Apache License, Version 2.0 に基づいてライセンスされています。
このライセンスに従う場合を除き、このファイルを使用することはできません。
ライセンスのコピーは以下で入手できます:

    http://www.apache.org/licenses/LICENSE-2.0

適用法令で要求される場合、または書面で合意された場合を除き、このライセンスの下で配布されるソフトウェアは「現状のまま」提供され、いかなる保証または条件もありません。
