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
  <version>1.7.19</version>
</dependency>
```

Alternatively, use the [tinystruct-archetype](https://github.com/tinystruct/tinystruct-archetype) to bootstrap a new project.

---

## 3. Core Concepts

### Applications
In tinystruct, every module is an `Application`. To create one, extend `org.tinystruct.AbstractApplication`.

```java
package com.example.app;

import org.tinystruct.AbstractApplication;
import org.tinystruct.ApplicationException;
import org.tinystruct.system.annotation.Action;
import org.tinystruct.system.annotation.Action.Mode;

public class HelloApplication extends AbstractApplication {

    @Override
    public void init() {
        // Initialization logic (e.g., setting up resources)
        // Note: Do NOT register actions here — use the @Action annotation instead.
        this.setTemplateRequired(false); // Skip .view template lookup if returning data directly
    }

    @Override
    public String version() {
        return "1.0.0";
    }

    // Handles: bin/dispatcher hello  AND  GET /?q=hello
    @Action("hello")
    public String sayHello() {
        return "Hello, tinystruct!";
    }

    // Path parameter: GET /?q=greet/James  OR  bin/dispatcher greet/James
    @Action("greet")
    public String greet(String name) {
        return "Hello, " + name + "!";
    }

    // HTTP-only POST handler
    @Action(value = "submit", mode = Mode.HTTP_POST)
    public String submit() throws ApplicationException {
        // Logic for handling submission
        return "Submitted successfully";
    }
}
```

### Actions and Routing
The `@Action` annotation maps URI paths or CLI commands to Java methods.

- **Path Mapping**: `@Action("praise")` handles `dispatcher praise` or `/?q=praise`.
- **Modes**: Specify if an action is restricted to CLI or specific HTTP methods.
- **Metadata**: Add descriptions and examples for CLI help generation.

  ```java
  @Action(
      value = "user/{id}",
      description = "Get a user by their ID",
      mode = Mode.HTTP_GET,
      example = "bin/dispatcher user/42"
  )
  public String getUser(int id) { 
      return "User ID: " + id;
  }
  ```

- **Arguments and Path Parameters**: Methods can accept parameters directly. tinystruct automatically builds a regex from the method signature for path parameters (e.g., `@Action("user/{id}")` -> `^/?user/(-?\d+)$`). For complex HTTP interactions, you can include `Request` and `Response` as parameters.
  ```java
  @Action(value = "upload", mode = Mode.HTTP_POST)
  public String upload(Request<?, ?> request, Response<?, ?> response) throws ApplicationException {
      // Use request and response directly for custom logic
      return "Upload handled";
  }
  ```

### Context Management
The `Context` object provides access to request-specific data, including CLI options and HTTP attributes.

```java
@Action("echo")
public String echo() {
    // Access CLI flags passed as `--words "Hello World"`
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
For web applications, session management is handled via the `Request` object. Include it as a parameter in your action method:

```java
import org.tinystruct.http.Request;

public String login(Request<?, ?> request) {
    request.getSession().setAttribute("user", "James");
    return "Logged in";
}
```

### JSON Handling
For JSON serialization and parsing, use the built-in `org.tinystruct.data.component.Builder` class instead of external libraries like Gson or Jackson.

```java
import org.tinystruct.data.component.Builder;

// Serialization
Builder builder = new Builder();
builder.put("status", "success");
builder.put("data", someObject);
String json = builder.toString();

// Parsing
Builder parsed = new Builder();
parsed.parse(jsonString);
String status = parsed.get("status").toString();
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

### Event Mechanism
Tinystruct supports an event-driven architecture to decouple components. To improve performance in asynchronous scenarios, event handlers can offload heavy processing to separate threads.

1. **Define an Event**: Implement `org.tinystruct.system.Event<T>`.
   ```java
   public class UserRegisterEvent implements Event<User> {
       private final User user;

       public UserRegisterEvent(User user) {
           this.user = user;
       }

       @Override
       public String getName() {
           return "user_register";
       }

       @Override
       public User getPayload() {
           return user;
       }
   }
   ```

2. **Dispatch an Event**:
   ```java
   EventDispatcher.getInstance().dispatch(new UserRegisterEvent(newUser));
   ```

3. **Asynchronous Handling**:
   To prevent blocking the main thread (e.g., during HTTP requests), handle events asynchronously using `CompletableFuture`.
   ```java
   EventDispatcher.getInstance().registerHandler(UserRegisterEvent.class, event -> {
       CompletableFuture.runAsync(() -> {
           // Heavy tasks: send email, update analytics, etc.
           sendWelcomeEmail(event.getPayload());
       });
   });
   ```

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
var response = handler.handleRequest(request);

// Always check the status code before using the response body
if (response.getStatusCode() == 200) {
    String responseBody = response.getBody();
    // Process the successful response
} else {
    // Handle the error (e.g., log response.getStatusCode())
}
```

---

## 8. Testing Patterns

For testing your applications, use JUnit 5. Since `ActionRegistry` is a singleton, you must manage its state carefully across tests.

```java
import org.junit.jupiter.api.*;
import org.tinystruct.system.Settings;

class HelloApplicationTest {

    private HelloApplication app;

    @BeforeEach
    void setUp() {
        app = new HelloApplication();
        // Setting configuration triggers init() and annotation processing
        app.setConfiguration(new Settings());
    }

    @Test
    void testHello() throws Exception {
        Object result = app.invoke("hello");
        Assertions.assertEquals("Hello, tinystruct!", result);
    }

    @Test
    void testGreet() throws Exception {
        Object result = app.invoke("greet", new Object[]{"James"});
        Assertions.assertEquals("Hello, James!", result);
    }
}
```

---

## 9. Common Pitfalls

| Problem | Fix |
|---|---|
| `ApplicationRuntimeException: template not found` | Call `setTemplateRequired(false)` in `init()` if you are returning data directly (e.g., for APIs). |
| Action not found at runtime | Make sure the class is imported via `--import` on the CLI or listed in `application.properties`. |
| Method not registered | Ensure the `@Action` annotation is on a `public` method — private/protected methods are ignored. |
| CLI arg not visible | Pass arguments with `--key value` syntax; access via `getContext().getAttribute("--key")`. |
| JSON using Gson/Jackson | Use `org.tinystruct.data.component.Builder` instead — it is the framework-native JSON library. |
| Two methods have the same path | Set explicit `mode` parameters (e.g., `Mode.HTTP_GET` vs `Mode.HTTP_POST`) to disambiguate. |

---

## 10. Best Practices
1. **Granular Applications**: Break logic into smaller, focused applications.
2. **Standard Interfaces**: Leverage `init()` for setup rather than constructors.
3. **Mode Awareness**: Use `Mode` in `@Action` to ensure security (e.g., restricted CLI-only tools).

---

## 11. Developer Tools

### Gemini CLI Skill
This project includes a specialized **Gemini CLI skill** located in `.agent/skills/tinystruct-dev/SKILL.md`. This skill provides expert guidance for developing with the tinystruct framework, covering architecture, routing, context, and more.

If you are using Gemini CLI, it will automatically recognize and utilize this skill to assist you with tinystruct-specific development tasks.
