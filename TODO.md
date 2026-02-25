# TODO

1. rename all mcp tpo IDs
1. GENERATOR. ensure some projects show delay, are not started or are finished closed.
2. add better error handling in api.
3. add ability to edit or delete a worklog.
4. remove default test password from AbstractApi.
5. make sure getAll will filter via alc in db, not in memory.
6. need ability to delete a task/story.
7. in case a user wants to remove himself from the ACL, we automatically add him back. you cannot remove yourself from
   ACL.

## Feature

1- Create Sprint Dialog that allows creation of Product, Version, Feature.

# Bugs

1. add gantt buffer calculation.
2. editing estimation will not change gantt chart task duration.
3. new task at the end will not take over last user assignment.
4. sometimes adding a story and two tasks will add additionally one task.
5. changing assignment must also change hidden dependencies
6. some tests fail with java.awt.HeadlessException.
7. users are retired by their name instead of their email address.
8. LocationDialog not showing errors in dialog.
9. AvailabilityTest.userSecurity() generates several exceptions on server side that the test does not catch.
10. gantt resource leveling fails sometimes with circular dependency error.
11. fix resource leveling not handling dependency to later story.
12. gantt calendar too light.
13. gantt calendar should be using sprint calendar.
14. some ai filter test fail all the time, as the tests are vague.
15. fix none humanized version of setMultiSelectComboBoxValue.
16. TaskGrid user colors are fake.

# Failing Tests

# Next Tooling Ideas

Based on the latest trends and developer experiences in 2025-2026, the best default Model Context Protocol (MCP) tools
to provide are those that offer immediate productivity gains, such as
file management, code exploration, and web search capabilities. These tools allow the AI to move from chatting to
active, contextual work.
Here are the best default MCP tools to consider:

1. Filesystem Server (Essential)

   Why: This is the most crucial, foundational tool. It allows the AI to read, write, and manage files in a designated
   local directory.
   Use Case: Reading codebase, creating documentation, or writing scripts.

2. GitHub/GitLab Server (Development)

   Why: For software development, the ability to interact with repositories is essential for context-aware coding.
   Use Case: Searching issues, reviewing pull requests, and checking repo structures.

3. Puppeteer/Browser (Web Interaction)

   Why: A "must-have" for frontend testing and web-related tasks, allowing the model to browse the web.
   Use Case: Testing UI components, debugging websites, or gathering real-time information.

4. Sequential Thinking (Logic & Reasoning)

   Why: Helps the AI manage complex, multi-step problems, which reduces errors and hallucinations.
   Use Case: Breaking down large architectural tasks or complex coding logic.

5. Search Engines (Tavily/Brave/Kagi)

   Why: Provides up-to-date information, bypassing the knowledge cutoff of the underlying LLM.
   Use Case: Looking up the latest documentation, API changes, or research data.

6. Database/API Connectors (PostgreSQL/SQLite/Supabase)

   Why: Enables the AI to directly interact with data, turning natural language into SQL queries.
   Use Case: Data analysis, schema inspection, or quick data manipulation.

Best Practices for Defaulting

    Keep it Simple: Start with the Filesystem and Search tools, then add others as needed.
    Human in the Loop: Always ensure there is a confirmation step for any actions that modify files or external data (e.g., file writes, API calls).
    Fine-Grained Tools: Design tools to do one thing well (e.g., read_file instead of a generic file_manager). 

These tools, when properly configured, allow AI agents (like those in Cursor, Windsurf, or Claude Desktop) to act as
autonomous, effective partners.

# Tools best practice

Best practices for defining Model Context Protocol (MCP) tools focus on clarity, security, and atomic functionality to
ensure AI agents can effectively discover and use them. Key practices include
using descriptive snake_case names, providing detailed, example-rich JSON Schema definitions for parameters, keeping
tools focused on single, atomic operations, and robustly handling errors.
Here are the best practices for defining MCP tools:
Tool Design and Definition

    Clear, Descriptive Naming: Use snake_case for tool names (e.g., get_user_data rather than getUserData). Names should be concise yet explicitly describe the action, as agents rely on these to select the right tool.
    Atomic Operations: Keep tools focused on a single, specific task rather than combining multiple steps into one.
    Comprehensive Documentation: Write thorough descriptions for both the tool and its parameters to guide the LLM on how to use them, including examples in the tool description.
    Structured Results: Return consistent JSON objects to ensure the model can parse the output reliably.
    Avoid Over-Translation: Do not directly convert every API endpoint to a tool. Instead, design tools around the user's intent to reduce unnecessary, multi-step orchestration. 

Parameter and Schema Best Practices

    Detailed JSON Schema: Use rigorous JSON Schema definitions for all tool parameters.
    Identify Requirements: Clearly mark parameters as required or optional.
    Flatten Arguments: Avoid deeply nested JSON structures in arguments, as this can confuse models. 

Security and Reliability

    Input Validation: Never assume the client sends valid data; sanitize and validate all inputs.
    Least-Privilege Access: Ensure tools only have the permissions necessary to perform their specific function.
    Error Handling: Implement robust error handling and return meaningful error messages, rather than letting the server crash.
    Timeouts and Rate Limiting: Implement timeouts and consider rate limiting for resource-intensive operations to protect the system. 

Implementation Tips

    Use stderr for Logging: For STDIO-based servers, never use console.log() for logging, as it breaks JSON-RPC communication. Use console.error() instead.
    Progress Reporting: Use built-in progress reporting for long-running operations.
    Version Dynamically: Do not hardcode versions; read them dynamically. 