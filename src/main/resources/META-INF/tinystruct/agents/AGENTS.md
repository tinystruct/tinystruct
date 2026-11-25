# AGENTS.md

This project is built on the **tinystruct** Java framework (version {%VERSION%}).
Write code the tinystruct way. Do not bring in Spring, Jakarta EE, Jackson or Gson patterns.

## Read first

Before writing or changing code, read the tinystruct skill:
`.agents/skills/tinystruct-patterns/SKILL.md`, and the topic files in its `references/` folder when you need detail
(routing, JSON, database, SSE, uploads, testing).

Online documentation: https://tinystruct.org/llms.txt

## Essentials

- Applications extend `org.tinystruct.AbstractApplication`. Do setup in `init()`, not in the constructor.
  API-only apps call `setTemplateRequired(false)` in `init()`.
- Routes and commands are `public` methods annotated with `@Action("path")`.
  An action without a `mode` works from both the CLI and HTTP. Use `mode = Mode.HTTP_POST`, `Mode.CLI`, etc. to restrict it.
- Never write a `main(String[] args)`. The entry point is `bin/dispatcher` (Windows: `bin\dispatcher.cmd`).
  If it is missing, generate it with `java -cp <tinystruct jar> org.tinystruct.system.Dispatcher --version`; never write it by hand.
- JSON: use `org.tinystruct.data.component.Builder` (object) and `Builders` (array), not Jackson or Gson.
- Persistence: POJOs extend `AbstractData`, mapped with `@Table`/`@Id`/`@Column` or an XML mapping file.
- Configuration lives in `src/main/resources/application.properties`; read it with `getConfiguration("key")`.

## Build, run and test

```bash
mvn compile                                   # compile to target/classes
mvn dependency:copy-dependencies -DoutputDirectory=lib   # put third-party jars on the dispatcher classpath
bin/dispatcher --help                          # list commands and actions
bin/dispatcher <action> [--key value]          # run an action from the CLI (fastest check)
bin/dispatcher start --import org.tinystruct.system.HttpServer --import <your.App>   # serve over HTTP
mvn test                                      # JUnit 5 tests
```

Verify every new transport-neutral action with `bin/dispatcher <action>` before testing it over HTTP.
