# GitStats Roadmap

## Next

### Repository Scope And Report Context

- [ ] Detect Git roots through the IntelliJ VCS/Git repository APIs instead of relying only on the project base path.
- [ ] Add a repository selector when multiple Git roots are available.
- [ ] Include the active repository, branch scope, date range, include paths, and exclusions in the summary/export output.
- [ ] Hide or soften the tool window state for projects without any Git roots.

### Author Drilldown

- [ ] Open a details panel or secondary table when an author row is double-clicked.
- [ ] Show commits, dates, hashes, added/deleted lines, and modified files per author.
- [ ] Add actions to open a commit in the IDE Git Log or open a changed file.
- [ ] Support copying/exporting drilldown rows separately from the summary table.

### Author Identity Rules

- [ ] Parse author email alongside author name.
- [ ] Support Git mailmap when available.
- [ ] Add local alias rules to merge authors.
- [ ] Add optional bot/service-account exclusion rules.
- [ ] Show merged identities in the drilldown so the aggregation remains auditable.

### Settings UX

- [ ] Add a project-relative path chooser for excluded paths.
- [ ] Validate excluded paths and highlight duplicates or paths outside the selected repository.
- [ ] Offer common exclude presets such as build output, generated sources, vendor dependencies, and lock files.
- [ ] Import exclude suggestions from `.gitignore`.
- [ ] Add saved views for reusable combinations of repository, branch scope, date range, include paths, mode, and exclusions.

## Later

### Metrics And Breakdowns

- [ ] Add toggles for merge commits, generated files, binary files, and renames.
- [ ] Distinguish modified-file events from unique modified files.
- [ ] Add net lines and churn metrics.
- [ ] Add per-extension, per-directory, or module-level breakdowns.
- [ ] Add top changed files and high-churn file lists.
- [ ] Add optional grouping by package/source root for JVM projects.

### Trends And Comparison

- [ ] Add daily/weekly trend views for commits, added lines, deleted lines, and modified files.
- [ ] Add comparison against the previous equivalent period.
- [ ] Add additional time presets such as today, yesterday, last week, and last month, plus custom saved ranges.
- [ ] Export trend data alongside the table data.
- [ ] Highlight unusually large spikes compared with the previous period.

### Reporting

- [ ] Add Markdown export for sharing stats in issues, pull requests, or release notes.
- [ ] Include filters, mode, repository, branch scope, and generated time in exported files.
- [ ] Add a copy action for summary-only output.
- [ ] Add a lightweight snapshot history for comparing recent generated reports.

### Large Repository Performance

- [ ] Move refresh work to IntelliJ background tasks with progress and cancellation.
- [ ] Add cancellation when users refresh again, close the tool window, or change filters.
- [ ] Cache recent results by repository, branch scope, date range, mode, and exclusions.
- [ ] Stream parser results where possible so the UI can update progressively.
- [ ] Add benchmarks or fixture-based performance tests for large log output.

### Visualization

- [ ] Add trend charts for activity over time.
- [ ] Add author comparison charts for commits and line churn.
- [ ] Add directory or file-type breakdown charts.
- [ ] Keep charts optional and table-first so the tool remains fast and IDE-native.

### Quality And Compatibility

- [ ] Keep README, Marketplace copy, screenshots, and in-product labels aligned as feature names change.
- [ ] Expand command builder tests when repository selection, export metadata, or mailmap options are added.
- [ ] Add parser fixtures for renames, binary files, merge commits, author names with separators, and unusual paths.
- [ ] Add service tests for multiple repository roots once repository selection exists.
- [ ] Add focused Swing tests for drilldown actions and settings validation.
- [ ] Keep `check`, `buildPlugin`, and `verifyPlugin` green across the configured IDE range before releases.
