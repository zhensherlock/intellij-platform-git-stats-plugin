<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# intellij-platform-git-stats-plugin Changelog

## [Unreleased]

- ✨ Support IDE builds through 262

## [0.9.0] - 2026-06-14

- ✨ Add branch scope filtering for current branch, all branches, and custom ranges
- ✨ Add include path filtering alongside exclude-path support
- ✨ Move Git Stats filters into action-based toolbar popups
- 🐛 Stabilize filter chip labels and hover state when filters change or are cleared
- 🧹 Tighten toolbar spacing and refresh plugin docs, roadmap, and release automation guidance

## [0.8.0] - 2026-06-07

- ✨ Add date range filtering for Git Stats refreshes
- ✨ Add author filtering and row actions to the stats table
- ✨ Persist stats mode and excluded paths, including support for multiple exclude entries
- ✨ Improve Git command execution state handling and result feedback
- 🐛 Treat empty Git repositories as empty results instead of an error
- 🐛 Keep the refresh button loading state in sync
- 🧹 Extract mode, date, and table parsing into dedicated domain utilities
- 🧹 Refresh README, icons, and repository workflow documentation

## [0.7.0] - 2026-06-06

- ✨ Migrate to IntelliJ Platform Gradle Plugin 2.x
- ✨ Update the build baseline to Java 21 and IntelliJ Platform 2024.2
- ✨ Support IDE builds from 242 through 261
- 🧹 Modernize GitHub Actions, Gradle Wrapper, and run configurations
- 🧹 Remove obsolete Qodana, Kover, and UI test template scaffolding

## [0.6.2] - 2025-05-13

**2025.05.13**

- ✨ Support 2025.1

## [0.6.1] - 2024-11-19

**2024.11.19**

- 🐛 Fixed error basePath
- 🐛 Fixed label width

## [0.6.0] - 2024-11-17

**2024.11.17**

- ✨ Add git exclude path
- ✨ Support 2024.3

## [0.5.0] - 2024-09-18

**2024.09.18**

- ✨ Support 2024.2

## [0.4.0] - 2024-05-29

**2024.05.29**

- ✨ Support UTF-8

## [0.0.2] - 2023-05-26

**2023.05.26**

- ✨ Add advanced mode
- ✨ 添加高级模式

## [0.0.1] - 2023-05-15

### Added

- Displays the code statistics table

[Unreleased]: https://github.com/zhensherlock/intellij-platform-git-stats-plugin/compare/0.9.0...HEAD
[0.9.0]: https://github.com/zhensherlock/intellij-platform-git-stats-plugin/compare/0.8.0...0.9.0
[0.8.0]: https://github.com/zhensherlock/intellij-platform-git-stats-plugin/compare/0.7.0...0.8.0
[0.7.0]: https://github.com/zhensherlock/intellij-platform-git-stats-plugin/compare/0.6.2...0.7.0
[0.6.2]: https://github.com/zhensherlock/intellij-platform-git-stats-plugin/compare/0.6.1...0.6.2
[0.6.1]: https://github.com/zhensherlock/intellij-platform-git-stats-plugin/compare/0.6.0...0.6.1
[0.6.0]: https://github.com/zhensherlock/intellij-platform-git-stats-plugin/compare/0.5.0...0.6.0
[0.5.0]: https://github.com/zhensherlock/intellij-platform-git-stats-plugin/compare/0.4.0...0.5.0
[0.4.0]: https://github.com/zhensherlock/intellij-platform-git-stats-plugin/compare/0.0.2...0.4.0
[0.0.2]: https://github.com/zhensherlock/intellij-platform-git-stats-plugin/compare/0.0.1...0.0.2
[0.0.1]: https://github.com/zhensherlock/intellij-platform-git-stats-plugin/commits/0.0.1
[//]: #
