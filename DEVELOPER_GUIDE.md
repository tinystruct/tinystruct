# tinystruct framework developer guide

## 1. Introduction

### Philosophy
The **tinystruct** framework is designed with a "Simple thinking, Better design" philosophy. It aims to provide a lightweight, high-performance environment for Java development where CLI and Web applications are treated as equal citizens.

### Architecture Overview
- **Zero Boilerplate**: No `main()` method required in your applications.
- **Unified Design**: The same application logic can be invoked via CLI or HTTP.
- **Minimal Configuration**: Convention over configuration, with an emphasis on transparency.
- **High Performance**: Optimized for low latency and high throughput.

---

## 2. Getting Started

### Prerequisites
- **JDK 17** or higher.
- **Maven** for dependency management.

### Installation
Add the following dependency to your `pom.xml`:

```xml
<dependency>
  <groupId>org.tinystruct</groupId>
  <artifactId>tinystruct</artifactId>
  <version>1.7.17</version>
</dependency>
```

Alternatively, use the [tinystruct-archetype](https://github.com/tinystruct/tinystruct-archetype) to bootstrap a new project.

---

## 3. Core Concepts

### Applications
In tinystruct, every module is an `Application`. To create one, extend `org.tinystruct.AbstractApplication`.

```java
import org.tinystruct.AbstractApplication;
import org.tinystruct.system.annotation.Action;

public class HelloApplication extends AbstractApplication {

    @Override
    public void init() {
        // Initialization logic (e.g., setting up resources)
    }

    @Override
    public String version() {
        return "1.0.0";
    }

    @Action("hello")
    public String sayHello() {
        return "Hello, Tinystruct!";
    }
}
```

### Actions and Routing
The `@Action` annotation maps URI paths or CLI commands to Java methods.

- **Path Mapping**: `@Action("praise")` handles `dispatcher praise` or `/?q=praise`.
- **Modes**: Specify if an action is restricted to CLI or specific HTTP methods.
  ```java
  @Action(value = "hello", mode = Mode.HTTP_POST)
  public String handlePost() { ... }
  ```
- **Arguments**: Methods can accept parameters directly. For complex HTTP interactions, you can include `Request` and `Response` as parameters.
  ```java
  @Action("greet")
  public String greet(String name) {
      return "Hello, " + name;
  }

  @Action("download")
  public void download(Request<?, ?> request, Response<?, ?> response) {
      // Use request and response directly for custom logic
  }
  ```

### Context Management
The `Context` object provides access to request-specific data, including CLI options and HTTP attributes.

```java
@Action("echo")
public String echo() {
    Object words = getContext().getAttribute("--words");
    return words != null ? words.toString() : "No words provided";
}
```

---

## 4. Execution Modes

### CLI Mode (The Dispatcher)
The `bin/dispatcher` tool is the entry point for CLI execution.

- **Check Version**: `bin/dispatcher --version`
- **Execute Action**: `bin/dispatcher hello`
- **Pass Arguments**: `bin/dispatcher greet/James` or `bin/dispatcher echo --words "Praise the Lord"`

### Web Mode (HTTP Server)
Tinystruct includes a built-in lightweight HTTP server. To start it:

```bash
bin/dispatcher start --import org.tinystruct.system.HttpServer
```

Access your actions via:
`http://localhost:8080/?q=hello`

---

## 5. Data & Templating

### Database Integration
Tinystruct provides built-in support for various databases (H2, MySQL, SQLite, SQLServer).

1. **Configure in `application.properties`**:
   ```properties
   driver=org.h2.Driver
   database.url=jdbc:h2:~/test
   database.user=sa
   database.password=
   ```
2. **Usage**: Use the `generate` command to create POJOs and use the internal data layer to interact with the database.

### Session Management
For web applications, session management is transparent. You can access session data via the `Context`:

```java
public String login() {
    getContext().getSession().setAttribute("user", "James");
    return "Logged in";
}
```

### JSON Handling
For JSON serialization and parsing, use the built-in `org.tinystruct.data.component.Builder` class instead of external libraries like Gson or Jackson.

```java
// Serialization
Builder builder = new Builder();
builder.put("status", "success");
builder.put("data", someObject);
String json = builder.toString();

// Parsing
builder.parse(jsonString);
Object value = builder.get("status");
```

### application.properties
Configuration is managed in `src/main/resources/application.properties`. Key properties include:
- `driver`: Database driver.
- `database.url`: JDBC URL.
- `default.home.page`: The default action to trigger on the root URL.

### Variables and Templates
Tinystruct supports dynamic content through a variable-based templating system.

1. **Set a Variable**:
   ```java
   setVariable("name", "World");
   ```
2. **Template Usage**: Variables are replaced in `.view` or HTML files using the `[%name%]` or similar syntax (depending on the specific template parser).

---

## 6. Advanced Topics

### Built-in CLI Commands
The dispatcher provides several utility commands:
- `generate`: POJO object generator for database tables.
- `sql-execute`: Run SQL statements directly.
- `install`: Install external packages.

### Custom Exception Handling
Extending `ApplicationException` or `ApplicationRuntimeException` allows for structured error reporting across both CLI and Web modes.

---

## 7. Networking & Integration

### HTTP Client (URLRequest & HTTPHandler)
To make outbound HTTP requests, use `org.tinystruct.net.URLRequest` and `org.tinystruct.net.handlers.HTTPHandler`.

```java
URL url = new URL("https://api.example.com/data");
URLRequest request = new URLRequest(url);
request.setMethod("POST")
       .setHeader("Content-Type", "application/json")
       .setBody("{\"key\":\"value\"}");

HTTPHandler handler = new HTTPHandler();
String responseBody = handler.handleRequest(request).getBody();
```

---

## 8. Best Practices
1. **Granular Applications**: Break logic into smaller, focused applications.
2. **Standard Interfaces**: Leverage `init()` for setup rather than constructors.
3. **Mode Awareness**: Use `Mode` in `@Action` to ensure security (e.g., restricted CLI-only tools).
