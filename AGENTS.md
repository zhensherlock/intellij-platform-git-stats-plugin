# Repository Instructions

## Commands

- Use `./gradlew` with JDK 21; CI uses Zulu 21 and the wrapper is Gradle 9.5.0.
- Run the plugin in an IDE sandbox with `./gradlew runIde`.
- Default local verification is `./gradlew check`.
- CI runs `./gradlew buildPlugin`, `./gradlew check`, and `./gradlew verifyPlugin` as separate jobs.
- Run one focused test with `./gradlew test --tests 'com.huayi.intellijplatform.gitstats.MyPluginTest.testExcludePathsAreNormalized' --console=plain`; replace the filter as needed.
- `./gradlew buildPlugin` writes `build/distributions/GitStats-<version>.zip`.
- `verifyPlugin` checks IC from `platformVersion` plus IDEA `2026.1.3`; expect it to be slower than `check`.
- IDE sandbox logs are under `.intellijPlatform/sandbox/*/*/log/idea.log`; test sandbox logs use `log-test/idea.log`.

## Project Shape

- This is a single-module Kotlin IntelliJ Platform plugin.
- `src/main/resources/META-INF/plugin.xml` registers the `Git Stats` tool window, notification group, resource bundle, and required `Git4Idea` dependency.
- `GitStatsWindowFactory.kt` owns the tool window shell, action-based filter toolbar, background refresh threading, and result rendering.
- `GitStatsService.kt` validates the project/repository, maps filters/settings to `GitStatsResult`, and delegates Git history work to `GitUtils`.
- `GitUtils` uses the IDE-configured Git executable from `Git4Idea`; it builds argument lists with `GitLogCommandBuilder`, executes through `CommandRunner`, and parses with `GitLogParser`. It does not use JGit.
- Table display/copy/export behavior lives in `toolWindow/StatsTable*` and `TableSnapshot.kt`.
- Settings persistence is project-level `GitStatsSettingsService`, stored in the workspace file.
- User-visible strings live in `src/main/resources/messages/MyBundle.properties`; add UI labels there instead of hardcoding.

## IntelliJ and Gradle Gotchas

- Plugin version, target platform, and supported build range live in `gradle.properties`, not `plugin.xml`.
- Current compatibility config is `platformVersion=2024.2.6`, `pluginSinceBuild=242`, and `pluginUntilBuild=262.*`.
- The IntelliJ Gradle plugin derives descriptor version/build range from Gradle config; do not add `<version>` or `<idea-version>` to source `plugin.xml`.
- Keep bundled `Git4Idea` in `build.gradle.kts` and the `Git4Idea` dependency in `plugin.xml` in sync.
- Kotlin stdlib bundling is disabled with `kotlin.stdlib.default.dependency=false`.
- Gradle configuration cache and build cache are enabled; avoid task wiring that breaks them.
- Prefer IntelliJ-native UI components/APIs (`JB*`, `DialogWrapper`, `Messages`, `FileChooserFactory`, `ActionSystem`, `JBPopupFactory`) over raw Swing equivalents when available.

## Git Stats Behavior

- Excluded paths are newline-delimited, trimmed, deduplicated, and normalized from `\` to `/` in `SettingModel`.
- Include path filters are normalized by `PathFilterPaths`; absolute paths, Windows drive paths, parent traversal, and raw Git magic pathspecs are rejected.
- Stored settings use mode ids `fast_summary` and `detailed`; `GitStatsSettingsService` normalizes legacy labels like `Fast Summary`/`Detailed`.
- `GitLogCommandBuilder` must return argument-list entries, never a shell string. Revision args go before `--`; pathspecs go after `--`.
- Pathspecs default to `.` when no include paths are selected; include paths precede `:(exclude)path` entries.
- Branch scopes include current branch, `HEAD`, all local branches via `--branches`, selected local/remote refs, and a validated custom revision range.
- Fast Summary mode omits commit count and sorts by added lines; Detailed mode includes commit counts and per-commit parsing.
- Git repo checks time out after 10 seconds; Git log commands time out after 60 seconds and redirect stderr into stdout before parsing.

## Tests and Workflow

- Tests extend `BasePlatformTestCase`, so even focused tests prepare the IntelliJ test sandbox.
- Swing component tests should mutate UI on the EDT; reuse `MyPluginTest.runOnEdt`.
- `settings.gradle.kts` installs a `commit-msg` hook that enforces Conventional Commits; `release` is an allowed type.
- `CLAUDE.md` delegates shared guidance back to this file; keep common agent instructions here.

## Release Workflow

- For a new plugin release, update `version` in `gradle.properties` and write notes under `## [Unreleased]` in `CHANGELOG.md`.
- Do not manually add `## [x.y.z] - YYYY-MM-DD` during the pre-release/version-bump commit.
- Keep the `[Unreleased]` compare link pointing from the latest released tag to `HEAD`, for example `v0.8.0...HEAD`.
- The build workflow reads Gradle `version`, runs `./gradlew getChangelog --unreleased`, and creates a draft GitHub release named `v$VERSION`.
- Publishing or prereleasing that draft triggers release CI: it checks out the tag, may run `patchChangelog` from the release body, runs `publishPlugin`, uploads the distribution, then opens the changelog-update PR.
- If release CI fails only while creating the changelog-update PR after publishing/upload succeeded, create that branch/PR manually instead of rerunning the whole release job.
