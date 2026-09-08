# Repository agent instructions

## Task completion

- After completing every task, review the task's changes, run the relevant verification, commit the completed work, and push the current branch to its configured remote.
- Do not leave completed task work uncommitted or unpushed.
- Keep each commit scoped to the task. Preserve unrelated user changes and do not include them unless the user explicitly asks.
- If a commit or push cannot be completed, report the exact blocker before handing the task back.
- After every task that changes the Android app or its build inputs, rebuild and verify the debug APK, then include a clickable link to the APK in the final response.
- Use the absolute workspace path for the APK link so it can be opened directly from the conversation.

## Android visual design

- Do not use default platform `<Button>` styling on user-facing Keepers screens.
- Reuse the established controls: `gallery_primary_action` for primary actions,
  `gallery_filter_chip` for compact secondary actions, and the existing icon-button
  drawables for icon-only actions.
- Prefer sentence-case labels, rounded shapes, intentional spacing, and the existing
  charcoal, warm-gold, blue, and neutral colour palette.
- `activity_main.xml` is an advanced integration-test harness and is the only screen
  where plain platform buttons are acceptable.
- When adding or changing a user-facing action, add layout or behavior coverage that
  prevents accidental fallback to a stock Android button.
