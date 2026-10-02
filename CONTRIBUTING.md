# Contributing to GitStats

Thank you for helping improve `GitStats`. Contributions to code, tests, documentation, translations, and issue triage are welcome.

By participating, you agree to follow the project [Code of Conduct](CODE_OF_CONDUCT.md).

## Choose the right channel

- Use [GitHub Discussions](https://github.com/zhensherlock/intellij-platform-git-stats-plugin/discussions) for usage questions, development help, ideas, and showcases.
- Use [GitHub Issues](https://github.com/zhensherlock/intellij-platform-git-stats-plugin/issues/new/choose) for reproducible bugs, concrete feature proposals, and documentation problems.
- Search existing issues and discussions before opening a new one.

A bug report should include the IDE name and build, plugin version, operating system, Git version, calculation mode, active filters, and steps to reproduce the problem. Include the relevant IDE log excerpt when possible, but remove repository names, paths, author details, and other sensitive information before posting it.

## Development setup

This repository uses the Gradle Wrapper and requires JDK 21.

```bash
git clone https://github.com/zhensherlock/intellij-platform-git-stats-plugin.git
cd intellij-platform-git-stats-plugin
./gradlew check
```

Run the plugin in an IntelliJ IDEA sandbox:

```bash
./gradlew runIde
```

Sandbox logs are stored under `.intellijPlatform/sandbox/*/*/log/idea.log`. Test sandbox logs are stored in `log-test/idea.log`.

## Making a change

1. Create a focused branch from the current default branch.
2. Keep the change limited to one problem or feature.
3. Add or update tests for behavior changes.
4. Put user-visible text in `src/main/resources/messages/MyBundle.properties` instead of hardcoding it.
5. Update `README.md` or other documentation when user-visible behavior changes.
6. Add noteworthy user-facing changes under `## [Unreleased]` in `CHANGELOG.md`.
7. Use a [Conventional Commit](https://www.conventionalcommits.org/) message such as `fix: normalize excluded paths` or `feat: add branch filtering`.

Do not add a released-version heading or date to `CHANGELOG.md` as part of a normal contribution. The release workflow manages that step.

When changing Git history behavior, keep command arguments as a list: revision arguments belong before `--`, and pathspecs belong after it. Prefer IntelliJ Platform UI components and APIs over raw Swing equivalents when an appropriate platform API exists.

## Verification

Run the default checks before opening a pull request:

```bash
./gradlew check
```

Run the closest test first while developing. For example:

```bash
./gradlew test \
  --tests 'com.huayi.intellijplatform.gitstats.MyPluginTest.testExcludePathsAreNormalized' \
  --console=plain
```

For changes that affect packaging or platform compatibility, also run:

```bash
./gradlew buildPlugin
./gradlew verifyPlugin
```

`verifyPlugin` checks multiple IntelliJ Platform versions and may take longer than `check`. `buildPlugin` writes the installable ZIP to `build/distributions/`.

For visible tool window changes, launch the sandbox with `./gradlew runIde` and test the affected flow manually. Describe the IDE and operating system you tested in the pull request.

## Pull requests

- Link the related issue or discussion when one exists.
- Explain the problem, the chosen solution, and any tradeoffs.
- Include screenshots or recordings for visible UI changes.
- Include tests for new behavior and regressions where practical.
- Call out compatibility changes and migration steps explicitly.
- Keep unrelated formatting, dependency, generated-file, and release-version changes out of the pull request.
- Make sure CI passes and the pull request description is complete.

Maintainers may ask for a smaller reproduction, additional tests, or a narrower change before reviewing an implementation. This keeps reviews focused and makes future regressions easier to diagnose.
