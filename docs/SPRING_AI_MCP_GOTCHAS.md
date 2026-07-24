_Referenced from [CLAUDE.md](../CLAUDE.md) — Spring AI 2.0 / MCP breaking-change gotchas._

# Spring AI 2.0 / MCP gotchas — verify against the 2.0 reference docs

Spring AI 2.0 is a breaking rewrite. **Most pre-2026 tutorials and much of your prior training show
stale imports and dead APIs.** When you touch AI/MCP code, treat the official 2.0 reference as the
source of truth and assume snippets found elsewhere are wrong until proven otherwise.

Known breaking points (all verified for 2.0.x):

- **MCP annotations moved into core:** `@McpTool`, `@McpResource`, `@McpPrompt`,
  `@McpToolParam` live in **`org.springframework.ai.mcp.annotation`** — *not* the old
  `org.springaicommunity.mcp.*`. Code importing the community package won't compile.
- **Spring MCP transports moved into Spring AI:** they're under **`org.springframework.ai.mcp`**, no
  longer in the MCP Java SDK (`io.modelcontextprotocol.sdk`).
- **Jackson 3:** package is **`tools.jackson.*`**, not `com.fasterxml.jackson.*`. Use `JsonMapper`
  (via `JacksonUtils.getDefaultJsonMapper()` / the new `JsonHelper`) where you need direct JSON access.
- **Options classes are immutable, builder-only.** Setters were removed. Use
  `AnthropicChatOptions.builder().model(...).maxTokens(...).temperature(...).build()`. No
  `setTemperature(...)`.
- **Tool error contract:** throw **`ToolExecutionException`** with a model-readable message from an
  `@McpTool` method. A plain `RuntimeException` bypasses the error processor and can halt the agent
  loop.
- **MCP servers validate tool args against the JSON schema by default** (2.0). Make sure every tool's
  params carry accurate schemas; malformed/missing args now return a structured error to the client.
- **Anthropic module = thin adapter over `com.anthropic:anthropic-java`.** For anything the
  `AnthropicChatModel` abstraction doesn't expose (beta endpoints, files API), drop to the SDK client
  directly — don't reimplement it.
- Enable the MCP annotation scanner in config (`spring.ai.mcp.server.annotation-scanner.enabled`);
  a `@Component` with annotated methods is enough — no manual `ToolCallback` bean wiring.
- **Google GenAI starter has two modes** — free Gemini Developer API (api-key only) vs paid Vertex
  (`project-id`/`location`); mixing them yields confusing 400 auth errors. Multiple providers coexist via
  `spring.ai.model.chat` + qualified beans. See [MODEL_ROUTING.md](MODEL_ROUTING.md).

Reference docs (fetch these rather than relying on memory):
`https://docs.spring.io/spring-ai/reference/` and the 2.0 upgrade notes.
