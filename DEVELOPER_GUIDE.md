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
  <version>1.7.21</version>
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

## 5. HTTP Features

### Session Management
Tinystruct provides a pluggable architecture for HTTP session management via the `SessionManager` and `SessionRepository` interfaces.

- **Default Behavior**: By default, sessions are stored in memory using `MemorySessionRepository`.
- **Redis Integration**: For clustered or stateless deployments, you can easily switch to a Redis-backed session repository.
  
To configure Redis sessions, update your `application.properties`:
```properties
default.session.repository=org.tinystruct.http.RedisSessionRepository
redis.host=127.0.0.1
redis.port=6379
# redis.password=yourpassword
```
Session configuration applies universally across all supported server modules (JDK HttpServer, Netty, Tomcat, Undertow).

### File Uploads (Multipart Data)
Handling file uploads is supported out of the box. When a `multipart/form-data` request is received, the framework automatically parses it. You can access uploaded files using `request.getAttachments()`:

```java
@Action(value = "upload", mode = Mode.HTTP_POST)
public String upload(Request<?, ?> request, Response<?, ?> response) throws ApplicationException {
    List<FileEntity> files = request.getAttachments();
    if (files != null) {
        for (FileEntity file : files) {
            System.out.println("Uploaded: " + file.getFilename());
            // Save the file to disk
            byte[] fileData = file.get();
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(new java.io.File("/tmp/" + file.getFilename()))) {
                fos.write(fileData);
            } catch (java.io.IOException e) {
                throw new ApplicationException(e.getMessage(), e);
            }
        }
    }
    return "Upload successful!";
}
```
This generic multipart handler works identically across the built-in JDK HTTP server and specialized server adapters like Undertow and Tomcat.

---

## 6. Data & Templating

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

## 7. Advanced Topics

### Distributed Locking
For clustered environments where you need to synchronize tasks across multiple nodes, use `DistributedLock` or `DistributedRedisLock`.

```java
import org.tinystruct.valve.DistributedRedisLock;
import org.tinystruct.valve.Lock;

public void processCriticalTask() {
    Lock lock = new DistributedRedisLock("my-global-lock-id");
    lock.lock();
    try {
        // Critical section logic here
    } finally {
        lock.unlock();
    }
}
```

### Server-Sent Events (SSE) & Streaming
Tinystruct provides built-in support for SSE to push real-time updates from server to client.

1. **Register the client connection**:
```java
@Action(value = "stream", mode = Mode.HTTP_GET)
public void stream(Request<?, ?> request, Response<?, ?> response) {
    String sessionId = request.getSession().getId();
    SSEPushManager.getInstance().register(sessionId, response);
}
```

2. **Push messages to the client**:
```java
Builder message = new Builder();
message.put("event", "update");
message.put("data", "Process completed!");
SSEPushManager.getInstance().push(sessionId, message);
```

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

## 8. Networking & Integration

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


### HTTP Client Async Patterns
For high-performance or non-blocking I/O, `HTTPHandler` supports asynchronous requests returning a `CompletableFuture`.

```java
URL url = new URL("https://api.example.com/data");
URLRequest request = new URLRequest(url);
HTTPHandler handler = new HTTPHandler();

CompletableFuture<URLResponse> future = handler.handleRequestAsync(request);
future.thenAccept(response -> {
    if (response.getStatusCode() == 200) {
        System.out.println("Async Data: " + response.getBody());
    }
}).exceptionally(ex -> {
    ex.printStackTrace();
    return null;
});
```

### Model Context Protocol (MCP) Integration
Tinystruct natively supports the Model Context Protocol (MCP), enabling AI model interactions, tool discovery, and prompt handling.

1. **Creating an MCP Server**:
Extend `MCPServer` and register your tools and prompts.
```java
import org.tinystruct.mcp.MCPServer;
import org.tinystruct.mcp.tools.CalculatorTool;

public class MyMCPServer extends MCPServer {
    @Override
    public void init() {
        super.init();
        this.registerTool(new CalculatorTool());
    }
}
```

2. **Connecting as a Client**:
Use `MCPClient` to connect to remote MCP servers, execute tools, and retrieve resources via JSON-RPC.
```java
MCPClient client = new MCPClient("http://localhost:8004", "auth-token");
client.connect();

Map<String, Object> params = new HashMap<>();
params.put("a", 10);
params.put("b", 20);
Object result = client.executeResource("calculator/add", params);
client.disconnect();
```

---

## 9. Testing Patterns

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

## 10. Common Pitfalls

| Problem | Fix |
|---|---|
| `ApplicationRuntimeException: template not found` | Call `setTemplateRequired(false)` in `init()` if you are returning data directly (e.g., for APIs). |
| Action not found at runtime | Make sure the class is imported via `--import` on the CLI or listed in `application.properties`. |
| Method not registered | Ensure the `@Action` annotation is on a `public` method — private/protected methods are ignored. |
| CLI arg not visible | Pass arguments with `--key value` syntax; access via `getContext().getAttribute("--key")`. |
| JSON using Gson/Jackson | Use `org.tinystruct.data.component.Builder` instead — it is the framework-native JSON library. |
| Two methods have the same path | Set explicit `mode` parameters (e.g., `Mode.HTTP_GET` vs `Mode.HTTP_POST`) to disambiguate. |

---

## 11. Best Practices
1. **Granular Applications**: Break logic into smaller, focused applications.
2. **Standard Interfaces**: Leverage `init()` for setup rather than constructors.
3. **Mode Awareness**: Use `Mode` in `@Action` to ensure security (e.g., restricted CLI-only tools).

---

## 12. Developer Tools

### Gemini CLI Skill
This project includes a specialized **Gemini CLI skill** located in `.agent/skills/tinystruct-dev/SKILL.md`. This skill provides expert guidance for developing with the tinystruct framework, covering architecture, routing, context, and more.

If you are using Gemini CLI, it will automatically recognize and utilize this skill to assist you with tinystruct-specific development tasks.
