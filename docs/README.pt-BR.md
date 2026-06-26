`"Quantas são as tuas obras, SENHOR! Fizeste todas elas com sabedoria; a terra está cheia das tuas criaturas."`
***Salmos 104:24***

Language: [English](../README.md) | [Português (Brasil)](README.pt-BR.md) | [简体中文](README.zh-CN.md) | [繁體中文](README.zh-TW.md) | [日本語](README.ja.md) | [한국어](README.ko.md) | [Türkçe](README.tr.md) | [Русский](README.ru.md) | [Tiếng Việt](README.vi.md) | [ไทย](README.th.md) | [Deutsch](README.de.md) | [Español](README.es.md)

O framework tinystruct
--
Um framework simples para desenvolvimento Java. Pensamento simples, melhor design, fácil de usar e com bom desempenho.

[![MvnRepository](https://badges.mvnrepository.com/badge/org.tinystruct/tinystruct/badge.svg?label=MvnRepository)](https://mvnrepository.com/artifact/org.tinystruct/tinystruct)
[![CodeQL](https://github.com/tinystruct/tinystruct/actions/workflows/codeql.yml/badge.svg)](https://github.com/tinystruct/tinystruct/actions/workflows/codeql.yml)

[![Star History Chart](https://api.star-history.com/svg?repos=tinystruct/tinystruct&type=Date)](https://www.star-history.com/#tinystruct/tinystruct&Date)

## Pré-requisitos

- Java Development Kit (JDK) 17 ou superior
- Maven para gerenciamento de dependências
- Um editor de texto ou IDE como IntelliJ IDEA, Eclipse ou VS Code

Começando com o archetype do tinystruct
--
Você pode usar este archetype para criar rapidamente um projeto baseado em tinystruct: https://github.com/tinystruct/tinystruct-archetype

Instalação manual e primeiros passos
--
* Adicione a dependência ao seu pom.xml.
```xml
<dependency>
  <groupId>org.tinystruct</groupId>
  <artifactId>tinystruct</artifactId>
  <version>1.7.27</version>
  <classifier>jar-with-dependencies</classifier> <!-- Opcional -->
</dependency>
```

* Estenda AbstractApplication em Java:

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

Executar em modo CLI
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

Executar em um servidor HTTP
--
```tcsh
# bin/dispatcher start --import org.tinystruct.system.HttpServer 
```
Você pode acessar as URLs abaixo:

* <a href="http://localhost:8080/?q=say/Praise%20to%20the%20Lord!">http://localhost:8080/?q=say/Praise%20to%20the%20Lord! </a>
* <a href="http://localhost:8080/?q=praise">http://localhost:8080/?q=praise</a>

Teste de desempenho
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

Processar mais de **86.000** requisições por segundo com baixa latência média, cerca de 17,44 ms, indica que o endpoint é altamente eficiente sob carga pesada. Isso mostra a força e a eficiência do **framework tinystruct**, mas não se trata apenas dos números: trata-se também da **filosofia** por trás dele.

### O que torna o framework tinystruct moderno?

1. **Não requer método `main()`**
   Aplicações podem ser iniciadas diretamente com comandos CLI como `bin/dispatcher`, sem código repetitivo.

2. **Design unificado para CLI e Web**
   **tinystruct** trata CLI e Web como recursos igualmente importantes, ideal para tarefas de IA, automação de scripts e aplicações híbridas.

3. **Servidor HTTP leve integrado**
   Seja Netty ou Tomcat, tinystruct integra o ciclo de vida do servidor ao framework.

4. **Filosofia de configuração mínima**
   A configuração fica limitada ao essencial, sem excesso de beans, XML ou YAML.

5. **Roteamento baseado em anotações**
   `@Action` fornece um mecanismo de roteamento limpo e intuitivo.

6. **Arquitetura orientada a desempenho**
   Quase sem overhead, sem varredura de beans baseada em reflexão e sem interceptadores desnecessários.

7. **Mais controle sem complexidade**
   tinystruct permite focar na **lógica de negócio real** com um design **transparente**, **previsível** e **extensível**.

---

Arquitetura
--
![tinystruct-framework-architecture](https://github.com/user-attachments/assets/33dbdc21-4803-4b94-bf69-bee053cf558d)

Licença
--

Licenciado sob a Apache License, Version 2.0.
Você não pode usar este arquivo exceto em conformidade com a licença.
Você pode obter uma cópia em:

    http://www.apache.org/licenses/LICENSE-2.0

Salvo exigência legal ou acordo por escrito, o software distribuído sob a licença é fornecido "NO ESTADO EM QUE SE ENCONTRA", sem garantias ou condições de qualquer tipo.
