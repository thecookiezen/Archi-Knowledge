# Archiledger

**Give your AI assistant a persistent memory and the power to build knowledge graphs.**

Archiledger is a specialized **Knowledge Graph** that serves as a **RAG (Retrieval-Augmented Generation)** system, equipped with a naive **vector search** implementation. It is exposed as a [Model Context Protocol (MCP)](https://modelcontextprotocol.io/) server to enable LLM-based assistants to store, connect, and recall information using a graph database. Whether you need a personal memory bank that persists across conversations or want to analyze codebases and documents into structured knowledge graphs, Archiledger provides the infrastructure to make your AI truly remember.

> **⚠️ Disclaimer:** This server currently implements **no authentication** mechanisms. Additionally, it relies on an **embedded graph database** (or in-memory storage) which is designed and optimized for **local development and testing environments only**. It is **not recommended for production use** in its current state.

## Why Archiledger?

LLMs are powerful, but they forget everything the moment a conversation ends. This creates frustrating experiences:

- **Repeating yourself** — Telling your assistant the same preferences, project context, or decisions over and over
- **Lost insights** — Valuable analysis from one session isn't available in the next
- **No connected thinking** — Information lives in silos without relationships between concepts

Archiledger solves this by giving your AI a **graph-based memory**:

| Problem | Archiledger Solution |
|---------|---------------------|
| Context resets every conversation | Persistent notes that survive restarts |
| Flat, disconnected notes | Typed links between atomic notes (Zettelkasten principle) |
| No way to categorize knowledge | Tags and keywords on every note |
| No temporal awareness | ISO-8601 timestamps on every note |
| No relevance signal | Retrieval count tracks frequently accessed notes |
| Keyword search limits | **Vector search** finds semantically similar notes |
| Hard to explore large knowledge bases | Graph traversal via typed `LINKED_TO` relationships |

The Zettelkasten model is powerful because each note is atomic, self-contained, and linked. When your AI can traverse these typed connections, it can provide richer context and discover non-obvious relationships.

## Features

- **Knowledge Graph**: Atomic MemoryNotes connected by typed links.
- **MCP Tools**:
  - **Note Management**:
    - `create_notes`: Create one or more memory notes with content, keywords, tags, and optional links.
    - `get_note`: Retrieve a specific note by ID. Increments the retrieval counter for relevance tracking.
    - `get_notes_by_tag`: Find all notes with a given tag (e.g., `architecture`, `decision`, `bug`).
    - `delete_notes`: Delete notes by their IDs, including associated links and embeddings.
  - **Link Management**:
    - `add_links`: Add typed links between notes (e.g., `DEPENDS_ON`, `RELATED_TO`, `CONTRADICTS`).
    - `delete_links`: Remove typed links between notes.
  - **Graph Exploration**:
    - `read_graph`: Read the entire knowledge graph. Returns all notes and their links.
    - `get_linked_notes`: Find all notes directly connected to a given note.
    - `get_all_tags`: List all unique tags currently used across notes.
    - `search_notes`: Semantic similarity search across all note content using vector embeddings.

## Known Limitations & Performance Characteristics

> **⚠️ Important:** Application is designed for local development, personal use, and small-to-medium datasets. Review the following limitations before using in production-like scenarios.

| Limitation | Impact | Notes/Mitigation |
|------------|--------|------------------|
| **Embedded LadybugDB** | Single-process database with limited concurrency | Suitable for small datasets (<100k notes). |
| **No authentication** | All operations are unauthenticated | Intended for local/trusted environments only. |
| **Heap-limited operations** | Large graph reads (`read_graph`) may OOM | Increase heap (`-Xmx`) or use pagination for large datasets. |

### Performance Expectations (Embedded LadybugDB)

Based on load testing with 512MB heap:

| Operation | Throughput | Notes |
|-----------|------------|-------|
| Note creation | ~50-100 ops/sec | Using Cypher inserts |
| Link creation | ~30-60 ops/sec | Depends on graph connectivity |
| Note lookup by ID | <10ms | Direct index lookup |
| Similarity search | O(n) | Scales linearly with note count |

> **💡 Tip:** For load testing see [LOAD_TESTING.md](./LOAD_TESTING.md).

## Architecture

- **Domain Layer**: Contains the core domain model (`MemoryNote`, `MemoryNoteId`, `NoteLink`). Defines the repository port (`MemoryNoteRepository`).
- **Application Layer**: Orchestrates the domain logic using services (`MemoryNoteService`). Handles retrieval count tracking and embedding generation.
- **Infrastructure Layer**:
  - **Persistence**:
    - `InMemoryMemoryNoteRepository`: Thread-safe in-memory implementation (default profile).
    - `LadybugMemoryNoteRepository`: LadybugDB graph database implementation (activates with `ladybugdb` profile). Notes are stored as graph nodes, links as `LINKED_TO` relationships.
  - **Vector Search / Storage**:
    - Note: Generating embeddings from text is always handled by **Spring AI's ONNX Model** (downloading a local embedding model like `all-minilm-l6-v2` to process text into float arrays).
    - `LadybugEmbeddingsService`: Uses LadybugDB's native vector extension — arrays stored efficiently on disk and indexed with HNSW for ultra-fast approximate nearest neighbor matching.
    - `LadybugVectorExtensionInitializer`: Installs/loads the vector extension and creates the HNSW index.
  - **MCP**: Acts as the primary adapter, exposing memory tools via the `McpToolAdapter`.

## Prerequisites

- Java 21 or higher
- Maven

## Building

```bash
mvn clean package
```

## Running

The server uses streamable HTTP transport by default on port **8080**.

### Default (In-Memory)
```bash
java -jar mcp/target/archiledger-server-0.0.1-SNAPSHOT.jar
```

### With LadybugDB (Embedded)
This mode runs LadybugDB inside the application process.

**Transient (Data lost on restart):**
```bash
java -Dspring.profiles.active=ladybugdb -jar mcp/target/archiledger-server-0.0.1-SNAPSHOT.jar
```

**Persistent (Data saved to file):**
Set the `ladybugdb.data-dir` property to a directory path.
```bash
java -Dspring.profiles.active=ladybugdb \
     -Dladybugdb.data-dir=./ladybugdb-data \
     -jar mcp/target/archiledger-server-0.0.1-SNAPSHOT.jar
```

> **💡 Tip: Viewing the Graph with Ladybug BugScope**
>
> When using embedded Neo4j, you can visualize your graph using [Ladybug BugScope](https://github.com/LadybugDB/bugscope). 
> 1. Open BugScope (default: http://localhost:8080) and connect using the Ladybug data directory URI.
> 2. Run Cypher queries like `MATCH (n) RETURN n` to explore your knowledge graph.

### Running with Docker

The Docker image supports configurable data persistence.

**Transient (Data lost when container stops):**
```bash
docker run -p 8080:8080 registry.hub.docker.com/thecookiezen/archiledger:latest
```

**Persistent (Data saved to host filesystem):**
Mount a local directory to `/data/ladybugdb` inside the container:
```bash
docker run -p 8080:8080 -v /path/to/local/ladybugdb-data:/data/ladybugdb registry.hub.docker.com/thecookiezen/archiledger:latest
```

**Custom data directory (Optional):**
Override the default data directory path using the `LADYBUGDB_DATA_DIR` environment variable:
```bash
docker run -p 8080:8080 \
  -e LADYBUGDB_DATA_DIR=/custom/data/path \
  -v /path/to/local/data:/custom/data/path \
  registry.hub.docker.com/thecookiezen/archiledger:latest
```

#### Docker Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `LADYBUGDB_DATA_DIR` | `/data/ladybugdb` | Directory where LadybugDB stores its data |

> **💡 Note:** The data directory at `/data/ladybugdb` (or your custom path) must be writable by the container user (UID 100, `spring` user). If you encounter permission errors, ensure your host directory has appropriate permissions:
> ```bash
> mkdir -p /path/to/local/ladybugdb-data
> chmod 777 /path/to/local/ladybugdb-data  # or chown to UID 100
> ```

## Configuration

Configuration is located in `src/main/resources/application.properties`.

```properties
spring.ai.mcp.server.name=archiledger-server
spring.ai.mcp.server.version=1.0.0
spring.ai.mcp.server.protocol=STREAMABLE
server.port=8080
```

### Vector Storage & Indexing Provider

Generating vector embeddings (the math part) is handled by Spring AI via an ONNX model.

| Property | Values | Default | Description |
|----------|--------|---------|-------------|
| `ladybugdb.extension-dir` | directory path | `~/.lbug/extensions` | Custom directory for LadybugDB extension cache |

Uses LadybugDB's native vector extension — hands off the generated float arrays to LadybugDB which stores them on disk and utilizes an advanced HNSW spatial index for instant semantic queries over millions of nodes.

## MCP Client Connection

Once the server is running, MCP clients can connect via:
- **Streamable HTTP Endpoint**: `http://localhost:8080/mcp`

---

## Usage with LLM

This MCP server can be used with LLM-based assistants (like GitHub Copilot, Gemini CLI, or other MCP-compatible clients) for various knowledge management scenarios. Below are two primary use cases with example instructions.

### Use Case 1: Memory Bank

Use the knowledge graph as a persistent memory bank. The LLM stores atomic pieces of knowledge as notes, tags them for categorization, and links related notes for richer context.

```markdown
# Memory Bank Instructions

You have access to a knowledge graph MCP server. Use it to store and retrieve atomic notes across conversations.

## Core Behaviors

### Proactive Memory Storage
When the user shares important information, store it as an atomic note:
- **Preferences**: User's coding style, preferred tools, naming conventions
- **Decisions**: Architecture decisions, technology choices, rejected alternatives
- **Context**: Project goals, constraints, team information
- **Tasks**: Ongoing work, blockers, next steps

### Tagging Notes
Use tags for categorization (a note can have multiple tags):
- `preference` - User preferences and settings
- `decision` - Important decisions with rationale
- `context` - Project or domain context
- `task` - Work items and their status
- `observation` - General notes and observations
- `person` - Team members and stakeholders

### Creating Notes
When storing information:
1. Give the note a descriptive ID (e.g., `java-naming-convention`, `db-migration-decision`)
2. Write focused content (one idea per note — the Zettelkasten atomicity principle)
3. Add relevant keywords for search
4. Set appropriate tags for categorization
5. Link to related notes using typed links

### Recalling Notes
At the start of each conversation:
1. Use `read_graph` to get an overview of stored knowledge
2. Use `search_notes` to find semantically relevant notes
3. Use `get_notes_by_tag` to retrieve notes by category
4. Reference stored decisions and preferences in your responses

### Linking Notes
Link related notes for better context using typed links:
- `RELATES_TO` - General relationship
- `DEPENDS_ON` - Dependency relationship
- `AFFECTS` - One thing impacts another
- `PART_OF` - Component/container relationship
- `SUPERSEDES` - Replaces previous decision/approach
- `CONTRADICTS` - Conflicts with another note
```

---

### Use Case 2: Codebase/Document Analysis

Use the knowledge base to build a structured knowledge base from a codebase or document corpus. Each code concept becomes an atomic note, linked to related concepts.

```markdown
# Codebase Knowledge Graph Builder

You have access to a memory MCP server. Use it to create atomic knowledge notes from the codebase for architecture documentation, onboarding, and investigation.

## Analysis Workflow

### Phase 1: High-Level Structure
Start by mapping the overall architecture:
1. Identify major modules, packages, or services
2. Create a note for each architectural component
3. Link notes with `DEPENDS_ON`, `CONTAINS`, or `USES` links

### Phase 2: Deep Dive
For each component, create detailed notes:
1. Key classes, interfaces, and their responsibilities
2. Important functions and their purposes
3. Data models and their relationships
4. External integrations and APIs

### Phase 3: Cross-Cutting Concerns
Document patterns that span multiple components:
1. Design patterns in use
2. Shared utilities and helpers
3. Configuration and environment handling
4. Error handling strategies

## Tags for Code Analysis

Use these tags on notes:
- `module` - Top-level packages, services, or bounded contexts
- `component` - Major classes, interfaces, or subsystems
- `function` - Important functions or methods
- `model` - Data models, DTOs, entities
- `pattern` - Design patterns in use
- `config` - Configuration classes or files
- `api` - External or internal API endpoints
- `dependency` - External libraries or services

## Link Types for Code

Use these relation types when linking notes:
- `DEPENDS_ON` - Class/module depends on another
- `IMPLEMENTS` - Implements an interface or contract
- `EXTENDS` - Inherits from another class
- `USES` - Utilizes another component
- `CALLS` - Function calls another function
- `CONTAINS` - Package contains class, class contains method
- `PRODUCES` - Creates or emits events/messages
- `CONSUMES` - Handles events/messages

## Querying for Investigation

Use the graph for code investigation:

1. **Find dependencies**: Get a note and examine its links
2. **Impact analysis**: Follow `DEPENDS_ON` links to find affected components
3. **Understand data flow**: Trace `CALLS`, `PRODUCES`, `CONSUMES` links
4. **Onboarding**: Search by `module` tag, then explore linked `component` notes

## Best Practices

1. **One idea per note** — follow the Zettelkasten atomicity principle
2. **Include file paths** in content or keywords for easy navigation
3. **Document "why"** not just "what" — capture design rationale
4. **Update incrementally** — add notes as you explore
5. **Link generously** — typed links are what make the graph valuable
```

---

### MCP Server Configuration for LLM Clients

Configure your LLM client to connect to the Archiledger MCP server. Below are examples for common clients.

#### Gemini CLI (`settings.json`)

```json
{
  "mcpServers": {
    "archiledger": {
      "httpUrl": "http://localhost:8080/mcp"
    }
  }
}
```

#### VSCode / GitHub Copilot (`settings.json`)

```json
{
  "servers": {
    "archiledger": {
      "type": "http",
      "url": "http://localhost:8080/mcp"
    }
  }
}
```

#### Antigravity

```json
{
  "mcpServers": {
      "archiledger": {
          "serverUrl": "http://localhost:8080/mcp"
      }
  }
}
```

### Docker Container Tips for MCP Clients

1. **Persistent Data**: Always mount a volume (`-v`) to preserve your knowledge graph across container restarts.

2. **Container Lifecycle**: Run the container separately with `-d` (detached mode).

3. **Port Conflicts**: If port 8080 is in use, map to a different host port (e.g., `-p 9090:8080`) and update the URL accordingly.

4. **Named Containers**: Use `--name archiledger` to easily manage the container:
   ```bash
   docker stop archiledger && docker rm archiledger
   ```

5. **Check Container Logs**: Debug connection issues with:
   ```bash
   docker logs archiledger
   ```
