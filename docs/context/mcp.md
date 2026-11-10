# MCP server (draft)
Purpose: let an AI assistant (Claude Desktop/Code, Copilot) query and act on opsdesk, mirroring the owner's work MCP server.
- Transport: see `open-questions.md` 2 (HTTP `/mcp` in the app, or stdio).
- Tools (draft): `list_services(stage?)`, `get_service_status(name)`, `list_open_incidents(severity?)`, `create_incident(service, severity, summary)`, `search_runbooks(query)` (uses `ai` retriever).
- Tool descriptions are written for the model: what, when to use, inputs, output shape.
- How to try: MCP Inspector + a `.mcp.json` example for Claude Code (to add).
