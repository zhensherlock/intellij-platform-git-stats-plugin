# Repository Instructions

## Commands

- Use the Gradle wrapper with JDK 21; CI uses Zulu 21.
- Run the plugin in an IDE sandbox with `./gradlew runIde`.
- Main local verification is `./gradlew check`; CI runs `./gradlew buildPlugin`, `./gradlew check`, and `./gradlew verifyPlugin` as separate jobs.
- Run one focused test with `./gradlew test --tests 'com.huayi.intellijplatform.gitstats.MyPluginTest.testExcludePathsAreNormalized' --console=plain` and replace the method/class filter as needed.
- `./gradlew buildPlugin` writes `build/distributions/GitStats-<version>.zip`.
- `verifyPlugin` checks the IDEs configured in `build.gradle.kts`: IC from `platformVersion` plus IDEA `2026.1.3`; it can be slower than `check`.
- JetBrains sandbox logs are under `.intellijPlatform/sandbox/*/*/log/idea.log`; test sandbox logs use `log-test/idea.log`.

## Project Shape

- This is a single-module Kotlin IntelliJ Platform plugin, not a multi-package repo.
- `src/main/resources/META-INF/plugin.xml` registers the `Git Stats` tool window, notification group, resource bundle, and required `Git4Idea` dependency.
- `src/main/kotlin/com/huayi/intellijplatform/gitstats/toolWindow/GitStatsWindowFactory.kt` owns the Swing tool window UI, date/author filters, background refresh, and result rendering.
- `src/main/kotlin/com/huayi/intellijplatform/gitstats/services/GitStatsService.kt` validates the project/repo, maps settings/date ranges to `GitStatsResult`, and delegates Git history work to `GitUtils`.
- `GitUtils` does not use JGit; it gets the IDE-configured Git executable from `Git4Idea`, builds `git log --numstat` commands via `GitLogCommandBuilder`, executes through `CommandRunner`, and parses through `GitLogParser`.
- Table display details live in `src/main/kotlin/com/huayi/intellijplatform/gitstats/toolWindow/StatsTable*`; settings persistence lives in project-level `GitStatsSettingsService`.
- User-visible strings live in `src/main/resources/messages/MyBundle.properties`; keep new UI labels there instead of hardcoding them.

## UI Guidelines

- Prefer IntelliJ Platform native UI APIs and components for plugin UI, such as `JB*` components, `DialogWrapper`, `Messages`, `FileChooserFactory`, `ActionSystem`, and `JBPopupFactory`, instead of raw Swing widgets when an IntelliJ-native equivalent exists.
- Keep UI behavior consistent with IDE themes, spacing, hover states, keyboard shortcuts, and accessibility expectations.

## IntelliJ/Gradle Gotchas

- Plugin version, target platform, and supported build range live in `gradle.properties`, not in `plugin.xml`.
- Current compatibility config is `platformVersion=2024.2.6`, `pluginSinceBuild=242`, and `pluginUntilBuild=261.*`.
- The IntelliJ Gradle plugin derives the built descriptor version/build range from Gradle config; do not duplicate `<version>` or `<idea-version>` in source `plugin.xml`.
- `build.gradle.kts` depends on bundled `Git4Idea`, and `plugin.xml` declares `Git4Idea`; keep both in sync if Git integration changes.
- Kotlin stdlib bundling is disabled with `kotlin.stdlib.default.dependency=false`.
- Gradle configuration cache and build cache are enabled; prefer cache-friendly task wiring when editing build scripts.

## Git Stats Behavior

- Excluded paths are newline-delimited, trimmed, deduplicated, and normalized from `\` to `/` in `SettingModel`.
- Stored settings use mode ids `fast_summary` and `detailed`; `GitStatsSettingsService` normalizes old labels like `Fast Summary`/`Detailed` for compatibility.
- `GitLogCommandBuilder` passes Git pathspecs as argument-list entries after `--`, e.g. `.`, `:(exclude)path`; do not join the command into a shell string or add shell quoting.
- `Fast Summary` mode omits commit count and sorts by added lines; `Detailed` mode includes commit counts and per-commit parsing.
- Git repo checks time out after 10 seconds; Git log commands time out after 60 seconds and redirect stderr into stdout before parsing.

## Tests and Workflow

- Existing tests extend `BasePlatformTestCase`, so focused tests still prepare the IntelliJ test sandbox.
- Swing component tests should run UI mutations on the EDT; `MyPluginTest.runOnEdt` shows the current pattern.
- `CLAUDE.md` delegates shared guidance back to this file; keep common agent instructions here.
- `settings.gradle.kts` installs a `commit-msg` hook that enforces Conventional Commits; allowed types include `release`.
- Release notes come from `CHANGELOG.md`: build CI creates a draft release from `./gradlew getChangelog --unreleased`, and release CI may patch `CHANGELOG.md` from the GitHub release body before `publishPlugin`.
