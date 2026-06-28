# OJP Workspace Rules & EverOS Integration Guidelines

This project utilizes EverOS for persistent developer memory retrieval and collection. All agent runs within this workspace MUST adhere to the following rules:

## 1. Pre-Task Action: Memory Retrieval
Before writing code or executing subtasks (e.g., modifying services, refactoring SQL components), you must recall relevant historical developer cases:
- Execute memory search using the embedded python helper:
  ```bash
  python C:/Users/007sl/.gemini/skills/ncode-memory-everos/scripts/everos_helper.py search --user-id "user_007" --query "[core_problem_description]"
  ```
- Incorporate any recalled episodes, profiles, or agent skills into your context as Few-Shot examples to improve output quality.

## 2. Post-Task Action: Memory Consolidation
Upon successfully resolving a task (compiles successfully, tests pass, dev server running):
- Consolidate your conversation trajectory into an OpenAI formatted JSON messages array and save it to a temporary file (e.g., `E:/a8-turbo/trajectory.json`).
- Upload this trajectory to EverOS to consolidate developer knowledge:
  ```bash
  python C:/Users/007sl/.gemini/skills/ncode-memory-everos/scripts/everos_helper.py record --user-id "user_007" --session-id "[your_conversation_id]" --file "E:/a8-turbo/trajectory.json"
  ```
- **CRITICAL**: Delete the temporary file `E:/a8-turbo/trajectory.json` immediately after a successful upload to keep the workspace clean.

## 3. Architectural Design & Alternative Selection Rules (架构选型避坑透明性红线)
- **CRITICAL**: When proposing any technical alternatives, framework selections, or architectural decisions, you **MUST proactively and explicitly detail the potential constraints, edge cases, underlying limitations, and failure traps of each proposed option in the first response**.
- You must never present a simplified ideal version of an option without analyzing its real-world implementation cost (e.g., networking requirements, performance degradation, data drift, or lock-in risks). Preventing silent selection failures is of paramount importance.

