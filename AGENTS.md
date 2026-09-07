# Repository agent instructions

## Task completion

- After completing every task, review the task's changes, run the relevant verification, commit the completed work, and push the current branch to its configured remote.
- Do not leave completed task work uncommitted or unpushed.
- Keep each commit scoped to the task. Preserve unrelated user changes and do not include them unless the user explicitly asks.
- If a commit or push cannot be completed, report the exact blocker before handing the task back.
- After every task that changes the Android app or its build inputs, rebuild and verify the debug APK, then include a clickable link to the APK in the final response.
- Use the absolute workspace path for the APK link so it can be opened directly from the conversation.
