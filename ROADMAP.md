# GitStats Roadmap

This roadmap starts from the current post-MVP baseline:

- Settings persistence is available for mode and excluded paths.
- Non-Git, empty repository, timeout, Git unavailable, and unexpected error states are surfaced in the UI.
- Table ergonomics include typed numeric columns, sorting, author filtering, summaries, copy, and CSV export.

## Near Term

### Repository And Branch Scope

Current stats run from `project.basePath`, which is simple but does not fully cover multi-root projects, nested repositories, submodules, or users who want to compare branches.

- Detect Git roots through the IntelliJ VCS/Git repository APIs.
- Add a repository selector when multiple Git roots are available.
- Add branch scope controls: current branch, all local branches, selected branch, or custom revision range.
- Include the active repository and branch scope in the summary/export output.
- Hide or soften the tool window state for projects without any Git roots.

### Author Drilldown

Detailed mode already retains commit and file-level data internally, but the UI currently presents only author-level rows.

- Open a details panel or secondary table when an author row is double-clicked.
- Show commits, dates, hashes, added/deleted lines, and modified files per author.
- Add actions to open a commit in the IDE Git Log or open a changed file.
- Support copying/exporting drilldown rows separately from the summary table.

### Author Identity Rules

Author names can vary across machines, accounts, and email addresses, which can split one contributor into multiple rows.

- Parse author email alongside author name.
- Support Git mailmap when available.
- Add local alias rules to merge authors.
- Add optional bot/service-account exclusion rules.
- Show merged identities in the drilldown so the aggregation remains auditable.

## Mid Term

### Metric Controls And Definitions

The plugin should make its counting rules explicit and configurable enough for teams to trust the numbers.

- Add toggles for merge commits, generated files, binary files, and renames.
- Distinguish modified-file events from unique modified files.
- Add net lines and churn metrics.
- Add per-extension, per-directory, or module-level breakdowns.
- Document how `git log --numstat` rows are interpreted.

### Trends And Period Comparison

The current table answers "who changed how much in this range"; users will also want to see movement over time.

- Add daily/weekly trend views for commits, added lines, deleted lines, and modified files.
- Add comparison against the previous equivalent period.
- Add time presets for today, yesterday, last week, last month, and custom saved ranges.
- Export trend data alongside the table data.

### Settings UX

The settings dialog works for basic input, but path and scope setup can be made friendlier.

- Add a project-relative path chooser for excluded paths.
- Validate excluded paths and highlight duplicates or paths outside the selected repository.
- Offer common defaults such as build output, generated sources, vendor dependencies, and lock files.
- Consider importing exclude suggestions from `.gitignore`.

## Long Term

### Large Repository Performance

Large repositories and wide date ranges can still be expensive even after command construction and parser improvements.

- Move refresh work to IntelliJ background tasks with progress and cancellation.
- Add cancellation when users refresh again, close the tool window, or change filters.
- Cache recent results by repository, branch scope, date range, mode, and exclusions.
- Stream parser results where possible so the UI can update progressively.
- Add benchmarks or fixture-based performance tests for large log output.

### Visualization

Once drilldown and trends exist, lightweight charts can make the tool more useful for review and reporting.

- Add trend charts for activity over time.
- Add author comparison charts for commits and line churn.
- Add directory or file-type breakdown charts.
- Keep charts optional and table-first so the tool remains fast and IDE-native.

### Quality And Compatibility

The current test surface covers settings, parser behavior, table formatting, and basic service states. Future work should keep widening the safety net as product scope grows.

- Keep README, Marketplace copy, screenshots, and in-product labels aligned as feature names change.
- Add command builder tests for branch/revision scopes and mailmap options.
- Add parser fixtures for renames, binary files, merge commits, author names with separators, and unusual paths.
- Add service tests for multiple repository roots once repository selection exists.
- Add focused Swing tests for drilldown actions and settings validation.
- Keep `check`, `buildPlugin`, and `verifyPlugin` green across the configured IDE range before releases.

## Non-Goals

- Do not add telemetry or upload Git history; stats should remain local to the user's machine.
- Do not replace the IDE Git Log; GitStats should summarize and link into existing IDE workflows.
- Do not optimize for organization-wide analytics before the single-project workflow is polished.
