---
name: tinystruct-dev
description: Expert guidance for developing with the tinystruct Java framework. Use this skill whenever working on the tinystruct codebase or any project built on tinystruct — including creating new Application classes, adding @Action-mapped routes, writing unit tests, working with ActionRegistry, setting up HTTP/CLI dual-mode handling, configuring the built-in HTTP server, using the event system, handling JSON with Builder, or debugging routing and context issues. Trigger this skill for any task involving tinystruct patterns, framework internals, developer conventions, and Server-Sent Events (SSE).
---

# Developer Skill - tinystruct framework

This skill captures the architecture, conventions, and patterns of the **tinystruct** Java framework — a lightweight, high-performance framework that treats CLI and HTTP as equal citizens, requiring no `main()` method and minimal configuration.

---

## Core Principle

**CLI and HTTP are equal citizens.** Every method annotated with `@Action` should ideally be runnable from both a terminal and a web browser without modification. This "dual-mode" capability is the core design philosophy of tinystruct.

---


## Core Architecture

### Key Abstractions

| Class/Interface | Role |
|---|---|
| `AbstractApplication` | Base class for all tinystruct applications. Extend this. |
| `@Action` annotation | Maps a method to a URI path (web) or command name (CLI). The single routing primitive. |
| `ActionRegistry` | Singleton that maps URL patterns to `Action` objects via regex. Never instantiate directly. |
| `Action` | Wraps a `MethodHandle` + regex pattern + priority + `Mode` for dispatch. |
| `Context` | Per-request state store. Access via `getContext()`. Holds CLI args and HTTP request/response. |
| `Dispatcher` | CLI entry point (`bin/dispatcher`). Reads `--import` to load applications. |
| `HttpServer` | Built-in HTTP server. Start with `bin/dispatcher start --import org.tinystruct.system.HttpServer`. |

### Package Map

```
org.tinystruct/
├── AbstractApplication.java      ← extend this
├── Application.java              ← interface
├── ApplicationException.java     ← checked exception
├── ApplicationRuntimeException.java ← unchecked exception
├── application/
│   ├── Action.java               ← runtime action wrapper
│   ├── ActionRegistry.java       ← singleton route registry
│   └── Context.java              ← request context
├── system/
│   ├── annotation/Action.java    ← @Action annotation + Mode enum
│   ├── Dispatcher.java           ← CLI dispatcher
│   ├── HttpServer.java           ← built-in HTTP server
│   ├── EventDispatcher.java      ← event bus
│   └── Settings.java             ← reads application.properties
├── data/component/Builder.java   ← JSON serialization (use instead of Gson/Jackson)
└── http/                         ← Request, Response, Constants
```

---

## Creating an Application

Every module is an `Application`. Extend `AbstractApplication`:

```java
package com.example;

import org.tinystruct.AbstractApplication;
import org.tinystruct.ApplicationException;
import org.tinystruct.http.Request;
import org.tinystruct.system.annotation.Action;
import org.tinystruct.system.annotation.Action.Mode;

public class HelloApp extends AbstractApplication {

    @Override
    public void init() {
        // One-time setup: set config, register resources.
        // Do NOT register actions here — use @Action annotation instead.
        this.setTemplateRequired(false); // skip .view template lookup if returning data directly
    }

    @Override
    public String version() {
        return "1.0.0";
    }

    // Handles: bin/dispatcher hello  AND  GET /?q=hello
    @Action("hello")
    public String hello() {
        return "Hello, tinystruct!";
    }

    // Path parameter: GET /?q=greet/James  OR  bin/dispatcher greet/James
    @Action("greet")
    public String greet(String name) {
        return "Hello, " + name + "!";
    }

    // HTTP-only POST handler
    @Action(value = "submit", mode = Mode.HTTP_POST)
    public String submit(Request request) throws ApplicationException {
        // Access raw request if needed
        return "Submitted";
    }
}
```

### `init()` Rules
- Called once when the application is loaded (via `setConfiguration()`).
- Use it for: setting up DB connections, configuring resource paths, calling `setTemplateRequired(false)`.
- **Do not** call `setAction()` here — use `@Action` annotation, which is processed automatically by `AnnotationProcessor`.

---

## @Action Annotation Reference

```java
@Action(
    value = "path/subpath",          // required: URI segment or CLI command
    description = "What it does",    // shown in --help output
    mode = Mode.DEFAULT,           // default: Mode.DEFAULT (both CLI + HTTP)
    example = "bin/dispatcher path/subpath/42"
)
public String myAction(int id) { ... }
```

### Mode Values
| Mode | When it triggers |
|---|---|
| `DEFAULT` | Both CLI and HTTP (GET, POST, etc.) |
| `CLI` | CLI dispatcher only |
| `HTTP_GET` | HTTP GET only |
| `HTTP_POST` | HTTP POST only |
| `HTTP_PUT` | HTTP PUT only |
| `HTTP_DELETE` | HTTP DELETE only |
| `HTTP_PATCH` | HTTP PATCH only |

> **Note:** You can map HTTP method names to `Mode` using `Action.Mode.fromName(String methodName)`. Unknown or null values return `Mode.DEFAULT`.

### Path Parameters
tinystruct automatically builds a regex from the method signature:

```java
@Action("user/{id}")
public String getUser(int id) { ... }
// → pattern: ^/?user/(-?\d+)$

@Action("search")
public String search(String query) { ... }
// → pattern: ^/?search/([^/]+)$
// → CLI: bin/dispatcher search/hello
// → HTTP: /?q=search/hello
```

Supported parameter types: `String`, `int/Integer`, `long/Long`, `float/Float`, `double/Double`, `boolean/Boolean`, `char/Character`, `short/Short`, `byte/Byte`, `Date` (parsed as `yyyy-MM-dd HH:mm:ss`).

### Accessing Request/Response

Include `Request` and/or `Response` as parameters — ActionRegistry automatically injects them from `Context`:

```java
@Action(value = "upload", mode = Mode.HTTP_POST)
public String upload(Request<?, ?> req, Response<?, ?> res) throws ApplicationException {
    // req.getParameter("file"), res.setHeader(...), etc.
    return "ok";
}
```

---

## Context and CLI Arguments

```java
@Action("echo")
public String echo() {
    // CLI: bin/dispatcher echo --words "Hello World"
    Object words = getContext().getAttribute("--words");
    if (words != null) return words.toString();
    return "No words provided";
}
```

CLI flags passed as `--key value` are stored in `Context` as `"--key"` → value.

---

## JSON Handling (use `Builder` and `Builders`, not Gson/Jackson)

The `Builder` class is used for JSON objects (`{}`), while the `Builders` class is used for JSON arrays (`[]`). **Always use `Builders` instead of `List<Builder>`** to avoid generic type erasure issues during JSON serialization.

```java
import org.tinystruct.data.component.Builder;
import org.tinystruct.data.component.Builders;

// 1. Serialize a Single Object
Builder response = new Builder();
response.put("status", "success");
response.put("count", 42);

// 2. Serialize a List of Objects using Builders
Builders dataList = new Builders();
for (MyModel item : myCollection) {
    Builder b = new Builder();
    b.put("id", item.getId());
    b.put("name", item.getName());
    dataList.add(b);
}
response.put("data", dataList);
return response.toString(); // {"status":"success","count":42,"data":[{"id":1,"name":"X"}]}

// 3. Parse a JSON Object
Builder parsedObj = new Builder();
parsedObj.parse(jsonString);
String status = parsedObj.get("status").toString();

// 4. Parse a JSON Array
Builders parsedArray = new Builders();
parsedArray.parse(jsonArrayString);
for (int i = 0; i < parsedArray.size(); i++) {
    Builder item = parsedArray.get(i);
    System.out.println(item.get("name"));
}
```

### Why use `Builder` and `Builders`?
- **Zero External Dependencies**: Keeps your application lean and fast.
- **Native Integration**: Works seamlessly with `AbstractApplication` result handling.
- **Performance**: Optimized for the specific data structures used within the framework.
- **Type Safety**: The framework natively understands how to serialize `Builders` to a JSON array `[]`, whereas `List<Builder>` can sometimes lead to runtime casting issues.

## Session Management (Web Mode)

The framework provides a pluggable architecture for HTTP sessions. By default, sessions are stored in memory (`MemorySessionRepository`). For clustered or stateless environments, you can switch to Redis natively.

Configure Redis sessions in `application.properties`:
```properties
default.session.repository=org.tinystruct.http.RedisSessionRepository
redis.host=127.0.0.1
redis.port=6379
```
To use sessions in your code, include `Request` as a parameter. The framework automatically injects the current request.

```java
import org.tinystruct.http.Request;

@Action(value = "login", mode = Mode.HTTP_POST)
public String login(Request<?, ?> request) {
    request.getSession().setAttribute("userId", "42");
    return "Logged in";
}

@Action("profile")
public String profile(Request<?, ?> request) {
    Object userId = request.getSession().getAttribute("userId");
    if (userId == null) return "Not logged in";
    return "User: " + userId;
}
```
---

## File Uploads (Multipart Data)

Handling `multipart/form-data` uploads works uniformly across all servers (JDK HttpServer, Netty, Tomcat, Undertow). Use `request.getAttachments()` to access files.

```java
import org.tinystruct.data.FileEntity;

@Action(value = "upload", mode = Mode.HTTP_POST)
public String upload(Request<?, ?> request) throws ApplicationException {
    List<FileEntity> files = request.getAttachments();
    if (files != null) {
        for (FileEntity file : files) {
            System.out.println("Uploaded: " + file.getFilename());
            // file.getContent() provides the byte array
        }
    }
    return "Upload OK";
}
```
---

## Server-Sent Events (SSE)

`tinystruct` provides native support for Server-Sent Events (SSE) for real-time, one-way communication from server to client.

### How it Works
1. **Client Request**: A client initiates a connection with the `Accept: text/event-stream` header.
2. **Automatic Handling**: The built-in `HttpServer` detects this header and automatically handles the SSE lifecycle, including setting headers (`Connection: keep-alive`, `Cache-Control: no-cache`, etc.) and registering the client.
3. **Session Binding**: Connections are tracked by session ID in the `SSEPushManager`.

### Implementing an SSE Action
Define an `@Action` that returns an initial message or configuration. The framework will keep the connection open and register the client.

```java
import org.tinystruct.http.SSEPushManager;
import org.tinystruct.data.component.Builder;

@Action("sse/connect")
public String connect() {
    // Initial handshake message
    return "{\"type\":\"connect\",\"message\":\"Connected to SSE\"}";
}
```

### Pushing Data to Clients
Use `SSEPushManager` to send messages to specific clients or broadcast to everyone.

```java
// 1. Push to a specific session
String sessionId = getContext().getId();
Builder message = new Builder();
message.put("text", "Hello, user!");
SSEPushManager.getInstance().push(sessionId, message);

// 2. Broadcast to all connected clients
Builder broadcastMsg = new Builder();
broadcastMsg.put("event", "alert");
broadcastMsg.put("content", "System maintenance in 5 minutes");
SSEPushManager.getInstance().broadcast(broadcastMsg);
```

### Message Formatting
`SSEPushManager` automatically formats the `Builder` or `String` into valid SSE format:
- If `type` is `"connect"`, it sends `event: connect\ndata: Connected\n\n`.
- Otherwise, it sends `data: <JSON_STRING>\n\n`.

### Managing Connections
- **Registration**: Done automatically by the server when the action is invoked with the correct headers.
- **Removal**: Call `SSEPushManager.getInstance().remove(sessionId)` to close and remove a client.
- **Client IDs**: Access all active session IDs via `SSEPushManager.getInstance().getClientIds()`.


## Event System

```java
// 1. Define an event
public class OrderCreatedEvent implements org.tinystruct.system.Event<Order> {
    private final Order order;
    public OrderCreatedEvent(Order order) { this.order = order; }

    @Override public String getName() { return "order_created"; }
    @Override public Order getPayload() { return order; }
}

// 2. Register a handler (typically in init())
EventDispatcher.getInstance().registerHandler(OrderCreatedEvent.class, event -> {
    CompletableFuture.runAsync(() -> sendConfirmationEmail(event.getPayload()));
});

// 3. Dispatch
EventDispatcher.getInstance().dispatch(new OrderCreatedEvent(newOrder));
```
---

## Templates

If `templateRequired` is `true` (the default), `toString()` looks for a `.view` file:
- Location: `src/main/resources/themes/<ClassName>.view` (on classpath)
- Variables are interpolated using `{%variableName%}`

```java
// In your action method:
setVariable("username", "James");
setVariable("count", String.valueOf(42));
// The template file uses: {%username%} and {%count%}
```

To skip templates and return data directly (e.g., for APIs):
```java
@Override
public void init() {
    this.setTemplateRequired(false);
}
```

---

## Database Persistence & POJO Generation

tinystruct includes a built-in ORM-like data layer. Each database table is represented by a **POJO** (Plain Old Java Object) that extends `AbstractData`, paired with a **mapping XML file** that binds Java fields to database columns.

### Architecture Overview

```
┌──────────────────────────────────────────────────────────┐
│  Your POJO (e.g. User.java)                              │
│  extends AbstractData                                     │
│  ┌──────────────────────────────────────────────────────┐ │
│  │ setData(Row row)   ← hydrate from DB result          │ │
│  │ setXxx() / getXxx() ← field accessors                │ │
│  │ toString()         ← JSON serialization              │ │
│  └──────────────────────────────────────────────────────┘ │
├──────────────────────────────────────────────────────────┤
│  AbstractData                                             │
│  ┌──────────────────────────────────────────────────────┐ │
│  │ append()       → INSERT                              │ │
│  │ appendAndGetId() → INSERT + return generated ID      │ │
│  │ update()       → UPDATE                              │ │
│  │ delete()       → DELETE                              │ │
│  │ findAll()      → SELECT *                            │ │
│  │ findOneById()  → SELECT WHERE id=?                   │ │
│  │ findWith(where, params) → SELECT WHERE ...           │ │
│  │ find(SQL, params) → raw SQL query                    │ │
│  └──────────────────────────────────────────────────────┘ │
├──────────────────────────────────────────────────────────┤
│  Mapping.java  ← reads .map.xml, builds Field metadata   │
├──────────────────────────────────────────────────────────┤
│  Repository (MySQL / SQLServer / SQLite / H2)             │
│  ← actual JDBC execution                                 │
└──────────────────────────────────────────────────────────┘
```

### Key Classes

| Class | Role |
|---|---|
| `AbstractData` | Base class for all POJOs. Provides CRUD + query methods. |
| `Data` | Interface defining the data contract (`append`, `update`, `delete`, `find*`). |
| `Mapping` | Reads `.map.xml` files and builds `Field` metadata for `AbstractData`. |
| `MappingManager` | Caches parsed mapping documents (singleton). |
| `Repository` | Database-specific JDBC implementation (MySQL, SQLServer, SQLite, H2). |
| `Generator` | Interface for POJO + mapping file generators. |
| `MySQLGenerator`, `MSSQLGenerator`, `SQLiteGenerator`, `H2Generator` | Concrete generators per database type. |
| `FieldType` | Maps SQL column types to Java types. |
| `Condition` | Fluent SQL query builder for `SELECT` statements. |
| `Row` | Represents a single database result row. |
| `Table` | Represents a collection of `Row` objects (query result set). |
| `DatabaseOperator` | Low-level JDBC wrapper for raw SQL execution. |

### Generating POJOs from Database Tables (CLI)

The `generate` command introspects a live database table and produces **two files** per table:

1. **Java POJO** — extends `AbstractData`, with getters/setters, `setData(Row)`, and `toString()`
2. **Mapping XML** — `ClassName.map.xml` that maps fields to columns

#### Prerequisites

Configure your database connection in `application.properties`:
```properties
driver=com.mysql.cj.jdbc.Driver
database.url=jdbc:mysql://localhost:3306/mydb
database.user=root
database.password=secret
```

#### Running the Generator

```bash
# Interactive mode — prompts for table names, path, and imports
bin/dispatcher generate

# Non-interactive — specify tables directly
bin/dispatcher generate --tables users

# Multiple tables at once (semicolon-delimited)
bin/dispatcher generate --tables "users;orders;products"
```

The interactive prompts will ask:
1. **Table name(s)** — semicolon-delimited (e.g. `users;orders`)
2. **Base path** — where to place Java files (default: auto-detected from `default.apps.package` or `src/main/java/custom/objects`)

#### Automatic Package Imports
Generators automatically detect the types used in your table and include the required Java imports in the POJO. This includes:
- `java.time.LocalDateTime` (for `DATETIME`, `TIMESTAMP`, `DATETIME2`)
- `java.util.Date` (for `DATE`)
- `java.sql.Timestamp` (if explicitly mapped)
- `java.sql.Time` (for `TIME`)

#### What Gets Generated

For a table `users` with columns `id INT AUTO_INCREMENT`, `username VARCHAR(50)`, `email VARCHAR(100)`, `created_at DATETIME`:

**Java POJO** — `src/main/java/com/example/objects/User.java`:
```java
package com.example.objects;
import java.io.Serializable;
import java.time.LocalDateTime;

import org.tinystruct.data.component.Row;
import org.tinystruct.data.component.AbstractData;

public class User extends AbstractData implements Serializable {
    /**
     * Auto Generated Serial Version UID
     */
    private static final long serialVersionUID = 123456789L;

    private String username;
    private String email;
    private LocalDateTime createdAt;

    // Id getter — reads from AbstractData.Id field
    public Integer getId() {
        return Integer.parseInt(this.Id.toString());
    }

    public void setUsername(String username) {
        this.username = this.setFieldAsString("username", username);
    }

    public String getUsername() {
        return this.username;
    }

    public void setEmail(String email) {
        this.email = this.setFieldAsString("email", email);
    }

    public String getEmail() {
        return this.email;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = this.setFieldAsLocalDateTime("createdAt", createdAt);
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    @Override
    public void setData(Row row) {
        if (row.getFieldInfo("id") != null)
            this.setId(row.getFieldInfo("id").intValue());
        if (row.getFieldInfo("username") != null)
            this.setUsername(row.getFieldInfo("username").stringValue());
        if (row.getFieldInfo("email") != null)
            this.setEmail(row.getFieldInfo("email").stringValue());
        if (row.getFieldInfo("created_at") != null)
            this.setCreatedAt(row.getFieldInfo("created_at").localDateTimeValue());
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("{");
        buffer.append("\"Id\":" + this.getId());
        buffer.append(",\"username\":\"" + this.getUsername() + "\"");
        buffer.append(",\"email\":\"" + this.getEmail() + "\"");
        buffer.append(",\"createdAt\":\"" + this.getCreatedAt() + "\"");
        buffer.append("}");
        return buffer.toString();
    }
}
```

**Mapping XML** — `src/main/resources/com/example/objects/User.map.xml`:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<mapping>
  <class name="User" table="users">
    <id name="Id" column="id" increment="true" generate="false" length="11" type="int"/>
    <property name="username" column="username" length="50" type="varchar"/>
    <property name="email" column="email" length="100" type="varchar"/>
    <property name="createdAt" column="created_at" length="0" type="datetime"/>
  </class>
</mapping>
```

> **Important:** The mapping XML file path mirrors the POJO's package structure but under `src/main/resources/` instead of `src/main/java/`. The `Mapping` class loads it from the classpath using `getClass().getResourceAsStream("/" + classPath + className + ".map.xml")`.

### Writing a POJO Manually

If you need to create a POJO without the generator (e.g. for a new table that doesn't exist yet), follow this pattern:

```java
package com.example.objects;

import java.io.Serializable;
import org.tinystruct.data.component.Row;
import org.tinystruct.data.component.AbstractData;

public class Product extends AbstractData implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private double price;
    private int quantity;

    // Id getter — type depends on your DB column type
    // Use Integer for INT, Long for BIGINT, String for VARCHAR/UUID
    public Integer getId() {
        return Integer.parseInt(this.Id.toString());
    }

    public void setName(String name) {
        this.name = this.setFieldAsString("name", name);
    }

    public String getName() {
        return this.name;
    }

    public void setPrice(double price) {
        this.price = this.setFieldAsDouble("price", price);
    }

    public double getPrice() {
        return this.price;
    }

    public void setQuantity(int quantity) {
        this.quantity = this.setFieldAsInt("quantity", quantity);
    }

    public int getQuantity() {
        return this.quantity;
    }

    @Override
    public void setData(Row row) {
        if (row.getFieldInfo("id") != null)
            this.setId(row.getFieldInfo("id").intValue());
        if (row.getFieldInfo("name") != null)
            this.setName(row.getFieldInfo("name").stringValue());
        if (row.getFieldInfo("price") != null)
            this.setPrice(row.getFieldInfo("price").doubleValue());
        if (row.getFieldInfo("quantity") != null)
            this.setQuantity(row.getFieldInfo("quantity").intValue());
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("{");
        buffer.append("\"Id\":" + this.getId());
        buffer.append(",\"name\":\"" + this.getName() + "\"");
        buffer.append(",\"price\":" + this.getPrice());
        buffer.append(",\"quantity\":" + this.getQuantity());
        buffer.append("}");
        return buffer.toString();
    }
}
```

And the corresponding mapping file `src/main/resources/com/example/objects/Product.map.xml`:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<mapping>
  <class name="Product" table="products">
    <id name="Id" column="id" increment="true" generate="false" length="11" type="int"/>
    <property name="name" column="name" length="100" type="varchar"/>
    <property name="price" column="price" length="0" type="double"/>
    <property name="quantity" column="quantity" length="11" type="int"/>
  </class>
</mapping>
```

### Mapping XML Reference

#### `<id>` Element (Primary Key)

| Attribute | Description |
|---|---|
| `name` | Java field name (always `Id` for the primary key) |
| `column` | Database column name |
| `type` | SQL type (e.g. `int`, `bigint`, `varchar`) |
| `length` | Column length (e.g. `11`, `36`) |
| `increment` | `true` if AUTO_INCREMENT / IDENTITY; `false` otherwise |
| `generate` | `true` to auto-generate UUID (for non-auto-increment string IDs); `false` otherwise |

> **Rule:** If `increment="true"`, the DB generates the ID. If `generate="true"` and `type` is not `int`, a UUID is generated by the framework.

#### `<property>` Element (Regular Column)

| Attribute | Description |
|---|---|
| `name` | Java field name (camelCase) |
| `column` | Database column name (e.g. `created_at`) |
| `type` | SQL type |
| `length` | Column length |

### AbstractData Setter Methods

Use these in your setter methods to keep the internal field state synchronized with the mapping:

| Method | Java Type | Example |
|---|---|---|
| `setFieldAsString(name, value)` | `String` | `this.name = this.setFieldAsString("name", name);` |
| `setFieldAsInt(name, value)` | `int` | `this.age = this.setFieldAsInt("age", age);` |
| `setFieldAsLong(name, value)` | `long` | `this.count = this.setFieldAsLong("count", count);` |
| `setFieldAsDouble(name, value)` | `double` | `this.price = this.setFieldAsDouble("price", price);` |
| `setFieldAsFloat(name, value)` | `float` | `this.rate = this.setFieldAsFloat("rate", rate);` |
| `setFieldAsBoolean(name, value)` | `boolean` | `this.active = this.setFieldAsBoolean("active", active);` |
| `setFieldAsDate(name, value)` | `Date` | `this.dob = this.setFieldAsDate("dob", dob);` |
| `setFieldAsLocalDateTime(name, value)` | `LocalDateTime` | `this.createdAt = this.setFieldAsLocalDateTime("createdAt", createdAt);` |
| `setFieldAsTimestamp(name, value)` | `Timestamp` | `this.updatedAt = this.setFieldAsTimestamp("updatedAt", updatedAt);` |
| `setFieldAsByteArray(name, value)` | `byte[]` | `this.data = this.setFieldAsByteArray("data", data);` |

### CRUD Operations

```java
// --- CREATE ---
User user = new User();
user.setUsername("james");
user.setEmail("james@example.com");
user.setCreatedAt(LocalDateTime.now());

// Insert and let DB assign the ID
user.append();

// Or insert and retrieve the generated ID
Object generatedId = user.appendAndGetId();

// --- READ ---
User user = new User();
user.setId(42);
user.findOneById();  // Populates all fields via setData()
System.out.println(user.getUsername()); // "james"

// Find by a specific column
User user = new User();
user.findOneByKey("email", "james@example.com");

// Find all
User user = new User();
Table allUsers = user.findAll();
for (Row row : allUsers) {
    User u = new User();
    u.setData(row);
    System.out.println(u.getUsername());
}

// --- UPDATE ---
User user = new User();
user.setId(42);
user.findOneById();
user.setEmail("newemail@example.com");
user.update();

// --- DELETE ---
User user = new User();
user.setId(42);
user.delete();
```

### Querying with Conditions

```java
// Find with a WHERE clause
User user = new User();
Table results = user.findWith("username LIKE ?", new Object[]{"%jam%"});

// Using Condition builder for complex queries
User user = new User();
Condition condition = new Condition();
condition.setRequestFields("id,username,email");
Table results = user.find(
    condition.select("`users`").and("email LIKE ?").orderBy("id DESC"),
    new Object[]{"%@example.com"}
);

// Select specific fields only
User user = new User();
user.setRequestFields("id,username");
Table results = user.findAll();

// Order by
User user = new User();
user.orderBy(new String[]{"created_at DESC"});
Table results = user.findAll();
```

### Raw SQL with DatabaseOperator

For queries that don't fit the POJO model:

```java
import org.tinystruct.data.DatabaseOperator;

try (DatabaseOperator operator = new DatabaseOperator()) {
    // SELECT query
    ResultSet rs = operator.query("SELECT COUNT(*) as cnt FROM users WHERE active = ?", new Object[]{true});
    if (rs.next()) {
        System.out.println("Active users: " + rs.getInt("cnt"));
    }

    // INSERT/UPDATE/DELETE
    int affected = operator.update("UPDATE users SET active = ? WHERE last_login < ?",
        new Object[]{false, "2024-01-01"});
}
```

### SQL Type → Java Type Mapping (FieldType)

| SQL Type | Java Type | FieldType Constant |
|---|---|---|
| `INT`, `INTEGER`, `TINYINT`, `SMALLINT` | `int` | `FieldType.INT`, `FieldType.INTEGER`, `FieldType.TINYINT`, `FieldType.SMALLINT` |
| `BIGINT` | `long` | `FieldType.BIGINT` |
| `FLOAT`, `REAL` | `float` | `FieldType.FLOAT`, `FieldType.REAL` |
| `DOUBLE`, `NUMERIC`, `DECIMAL` | `double` | `FieldType.DOUBLE`, `FieldType.NUMERIC`, `FieldType.DECIMAL` |
| `VARCHAR`, `TEXT`, `LONGTEXT`, `LONGVARCHAR`, `ENUM`, `SET`, `JSON`, `CHARACTER VARYING` | `String` | `FieldType.VARCHAR`, `FieldType.TEXT`, etc. |
| `BIT`, `BOOLEAN` | `boolean` | `FieldType.BIT`, `FieldType.BOOLEAN` |
| `DATE` | `Date` | `FieldType.DATE` |
| `DATETIME`, `DATETIME2`, `SMALLDATETIME` | `LocalDateTime` | `FieldType.DATETIME`, `FieldType.DATETIME2`, `FieldType.SMALLDATETIME` |
| `TIMESTAMP` | `LocalDateTime` | `FieldType.TIMESTAMP` |
| `BLOB` | `byte[]` | `FieldType.BLOB` |

### Using POJOs in an Application

```java
import org.tinystruct.AbstractApplication;
import org.tinystruct.ApplicationException;
import org.tinystruct.system.annotation.Action;
import com.example.objects.User;
import org.tinystruct.data.component.Builder;
import org.tinystruct.data.component.Builders;
import org.tinystruct.data.component.Row;
import org.tinystruct.data.component.Table;

public class UserApp extends AbstractApplication {

    @Override
    public void init() {
        this.setTemplateRequired(false);
    }

    @Override
    public String version() {
        return "1.0.0";
    }

    @Action("api/users")
    public String listUsers() throws ApplicationException {
        User user = new User();
        Table allUsers = user.findAll();

        Builders list = new Builders();
        for (Row row : allUsers) {
            User u = new User();
            u.setData(row);

            Builder b = new Builder();
            b.put("id", u.getId());
            b.put("username", u.getUsername());
            b.put("email", u.getEmail());
            list.add(b);
        }

        Builder response = new Builder();
        response.put("users", list);
        return response.toString();
    }

    @Action("api/user")
    public String getUser(int id) throws ApplicationException {
        User user = new User();
        user.setId(id);
        user.findOneById();
        return user.toString();
    }
}
```

### File Placement Rules

| File Type | Location Pattern | Example |
|---|---|---|
| Java POJO | `src/main/java/<package-path>/ClassName.java` | `src/main/java/com/example/objects/User.java` |
| Mapping XML | `src/main/resources/<package-path>/ClassName.map.xml` | `src/main/resources/com/example/objects/User.map.xml` |

> **Critical:** The mapping XML **must** mirror the POJO's package path under `src/main/resources/`. If the POJO is at `src/main/java/com/example/objects/User.java`, the mapping must be at `src/main/resources/com/example/objects/User.map.xml`. The framework uses `getClass().getResourceAsStream("/" + classPath + className + ".map.xml")` to locate it.

### Generator Naming Conventions

- Table names are **singularized** automatically (e.g. `users` → `User`, `order_items` → `OrderItem`)
- Column names with underscores are converted to **camelCase** (e.g. `created_at` → `createdAt`)
- The `setData()` method references the **original database column name** (e.g. `row.getFieldInfo("created_at")`)
- The mapping XML `<property name="...">` uses the **camelCase** Java field name
- The mapping XML `<property column="...">` uses the **original database column name**

---

## Configuration (`application.properties`)

Located at `src/main/resources/application.properties`:

```properties
# Database
driver=org.h2.Driver
database.url=jdbc:h2:~/mydb
database.user=sa
database.password=

# Server
default.home.page=hello        # default action for /?q= (root URL)
server.port=8080

# Locale
default.language=en_US
```

Access config values in your application:
```java
String port = this.getConfiguration("server.port");
```

---

## Running the Application

```bash
# CLI mode
bin/dispatcher hello
bin/dispatcher greet/James
bin/dispatcher echo --words "Hello" --import com.example.HelloApp

# HTTP server (listens on :8080 by default)
bin/dispatcher start --import org.tinystruct.system.HttpServer
# Then: http://localhost:8080/?q=hello

# Generate POJO from DB table
bin/dispatcher generate --table users

# Run SQL
bin/dispatcher sql-query "SELECT * FROM users" --import org.tinystruct.system.Dispatcher
```

---

## Networking: Outbound HTTP

To make outbound HTTP requests from your application, use `URLRequest` and `HTTPHandler`.

```java
import org.tinystruct.net.URLRequest;
import org.tinystruct.net.handlers.HTTPHandler;
import java.net.URL;

URL url = new URL("https://api.example.com/data");
URLRequest request = new URLRequest(url);
request.setMethod("POST")
       .setHeader("Content-Type", "application/json")
       .setBody("{\"key\":\"value\"}");

HTTPHandler handler = new HTTPHandler();
var response = handler.handleRequest(request);

// Always check the status code before using the response body
if (response.getStatusCode() == 200) {
    String body = response.getBody();
    // Process success
} else {
    // Handle error (e.g., response.getStatusCode())
}
```

---


## Testing Patterns

Use JUnit 5. ActionRegistry is a singleton — reset or use fresh state carefully in tests.

```java
import org.junit.jupiter.api.*;
import org.tinystruct.application.ActionRegistry;

class MyAppTest {

    private MyApp app;

    @BeforeEach
    void setUp() {
        app = new MyApp();
        // Set a minimal configuration to trigger init() and annotation processing
        Settings config = new Settings();
        app.setConfiguration(config);
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

For `ActionRegistry` unit tests, follow the pattern in:
`src/test/java/org/tinystruct/application/ActionRegistryTest.java`

### HTTP Integration Testing
For full integration tests involving the built-in HTTP server and method mode verification, see:
`src/test/java/org/tinystruct/system/HttpServerHttpModeTest.java`

**Key Pattern:**
1. Start `HttpServer` in a background thread.
2. Use `ApplicationManager.call("start", context, Action.Mode.CLI)` to boot the server.
3. Wait for the port to be open using a `Socket` before sending requests.
4. Use `URLRequest` and `HTTPHandler` to perform actual HTTP requests and verify responses.

---

## Red Flags & Common Pitfalls

| Symptom / Problem | Fix |
|---|---|
| Using `Gson` or `Jackson` | **Violation.** Use `org.tinystruct.data.component.Builder` for native JSON. |
| `ApplicationRuntimeException: template not found` | Call `setTemplateRequired(false)` in `init()` if you return data directly. |
| `@Action` on private/protected method | **Ignored.** Actions MUST be `public` to be registered. |
| Hardcoding `main()` method | **Anti-pattern.** Use `bin/dispatcher` for execution. |
| Direct `ActionRegistry` usage | **Avoid.** Let the framework handle routing via annotations. |
| Action not found at runtime | Make sure the class is imported via `--import` or listed in `application.properties`. |
| CLI arg not visible | Pass with `--key value` syntax; access via `getContext().getAttribute("--key")`. **Do not** use `{key}` path parameters for optional flags. |
| Two methods same path, wrong one fires | Set explicit `mode` (e.g., `HTTP_GET` vs `HTTP_POST`) to disambiguate. |

---

## Best Practices

1. **Granular Applications**: Break logic into smaller, focused applications rather than one monolithic class.
2. **Setup in `init()`**: Leverage the `init()` method for application setup (config, DB) rather than the constructor.
3. **Mode Awareness**: Use the `Mode` parameter in `@Action` to restrict sensitive tools to `CLI` only or specific HTTP methods.
4. **Context over Params**: For optional CLI flags, use `getContext().getAttribute("--flag")` rather than adding parameters to the method signature.
5. **Asynchronous Events**: For heavy tasks triggered by events (e.g. sending email), use `CompletableFuture.runAsync()` inside the event handler to keep the request/response cycle fast.

---


## Reference Files

Read these when you need deeper context beyond what's in this skill:

- `DEVELOPER_GUIDE.md` — full developer guide with extended examples; read for complex multi-module or advanced routing scenarios
- `README.md` — quick start and architecture diagram; read for project setup or onboarding questions
- `src/main/java/org/tinystruct/AbstractApplication.java` — complete base class; read when working with lifecycle hooks or unfamiliar inherited methods
- `src/main/java/org/tinystruct/system/annotation/Action.java` — annotation definition + `Mode` enum; read for edge cases around routing modes
- `src/main/java/org/tinystruct/application/ActionRegistry.java` — routing engine internals; read when debugging                                                                                                              ng route resolution or priority conflicts
- `src/test/java/org/tinystruct/application/ActionRegistryTest.java` — registry test examples; read when writing                                                                                                              ng tests involving `ActionRegistry`
- `src/test/java/org/tinystruct/system/HttpServerHttpModeTest.java` — HTTP mode and server integration test patterns; read when testing HTTP actions or server lifecycle
- `src/main/java/org/tinystruct/data/component/AbstractData.java` — base POJO class with all CRUD and query methods; read when working with data persistence
- `src/main/java/org/tinystruct/data/Mapping.java` — mapping XML parser; read when debugging field mapping or `.map.xml` issues
- `src/main/java/org/tinystruct/data/tools/MySQLGenerator.java` — reference POJO generator implementation; read when understanding generated code structure
- `src/main/java/org/tinystruct/data/component/FieldType.java` — SQL-to-Java type mappings; read when adding support for new column types
- `src/main/java/org/tinystruct/data/component/Condition.java` — fluent SQL query builder; read when constructing complex queries

