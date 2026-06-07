# Repository Instructions

## Commands

- Use the Gradle wrapper with JDK 21; CI uses Zulu 21.
- Run the plugin in an IDE sandbox with `./gradlew runIde`.
- Main local verification is `./gradlew check`; CI also runs `./gradlew buildPlugin` and `./gradlew verifyPlugin` as separate jobs.
- Run one test with `./gradlew test --tests 'com.huayi.intellijplatform.gitstats.MyPluginTest.testExcludePathsAreNormalized' --console=plain` and replace the method/class filter as needed.
- `./gradlew buildPlugin` writes `build/distributions/GitStats-<version>.zip`.
- `verifyPlugin` checks the IDEs configured in `build.gradle.kts`: IC from `platformVersion` plus IDEA `2026.1.3`; it can be slower than `check`.
- JetBrains sandbox logs are under `.intellijPlatform/sandbox/*/*/log/idea.log`; test sandbox logs use `log-test/idea.log`.

## Project Shape

- This is a single-module Kotlin IntelliJ Platform plugin, not a multi-package repo.
- `src/main/resources/META-INF/plugin.xml` registers one tool window, `Git Stats`, backed by `GitStatsWindowFactory`.
- `GitStatsWindowFactory` owns the Swing UI, date picker, refresh action, and settings action.
- `GitStatsService` converts UI date ranges into table models and delegates Git history work to `GitUtils`.
- `GitUtils` does not use JGit; it shells out to the IDE-configured Git executable from `Git4Idea` and parses `git log --numstat`.
- User-visible strings live in `src/main/resources/messages/MyBundle.properties`; keep new UI labels there instead of hardcoding them.

## IntelliJ/Gradle Gotchas

- Plugin version, target platform, and supported build range live in `gradle.properties`, not in `plugin.xml`.
- Current compatibility config is `platformVersion=2024.2.6`, `pluginSinceBuild=242`, and `pluginUntilBuild=261.*`.
- `patchPluginXml` injects `<version>`, `<idea-version>`, and changelog notes into the built descriptor; do not duplicate those in source `plugin.xml`.
- `build.gradle.kts` depends on bundled `Git4Idea`, and `plugin.xml` declares `Git4Idea`; keep both in sync if Git integration changes.
- Kotlin stdlib bundling is disabled with `kotlin.stdlib.default.dependency=false`.
- Gradle configuration cache and build cache are enabled; prefer cache-friendly task wiring when editing build scripts.

## Git Stats Behavior

- Excluded paths are newline-delimited, trimmed, deduplicated, and normalized from `\` to `/` in `SettingModel`.
- `GitUtils` converts exclusions to Git pathspecs like `:(exclude)path`; be careful with shell quoting when changing command construction.
- `Fast Summary` mode omits commit count and sorts by added lines; `Detailed` mode includes commit counts and per-commit parsing.
- Git command execution has a 60-second timeout and merges stderr into stdout before parsing.

## Tests and Workflow

- Existing tests extend `BasePlatformTestCase`, so focused tests still prepare the IntelliJ test sandbox.
- Swing component tests should run UI mutations on the EDT; `MyPluginTest.runOnEdt` shows the current pattern.
- `settings.gradle.kts` installs a `commit-msg` hook that enforces Conventional Commits; allowed types include `release`.
- Release notes come from `CHANGELOG.md`: build CI creates a draft release from `./gradlew getChangelog --unreleased`, and release CI may patch `CHANGELOG.md` from the GitHub release body before `publishPlugin`.
