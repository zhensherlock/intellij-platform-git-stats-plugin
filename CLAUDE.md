# CLAUDE.md - GitStats IntelliJ Plugin

Use `AGENTS.md` as the primary project instruction file.

When working in a subdirectory, also follow the nearest nested `AGENTS.md` before making changes.

## Claude-Specific Behavior

- Prefer small, reviewable changes.
- Before large refactors, summarize the impact and likely touched IntelliJ plugin areas.
- After code changes, run the most relevant Gradle checks from `AGENTS.md`; default to `./gradlew check`.
- Keep shared repo guidance in `AGENTS.md`; keep this file as a lightweight Claude entrypoint.
