# intellij-platform-git-stats-plugin

![Build](https://github.com/zhensherlock/intellij-platform-git-stats-plugin/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/com.huayi.intellijplatform.gitstats.svg)](https://plugins.jetbrains.com/plugin/com.huayi.intellijplatform.gitstats)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/com.huayi.intellijplatform.gitstats.svg)](https://plugins.jetbrains.com/plugin/com.huayi.intellijplatform.gitstats)

[//]: # (## Template ToDo list)

[//]: # (- [x] Create a new [IntelliJ Platform Plugin Template][template] project.)

[//]: # (- [ ] Get familiar with the [template documentation][template].)

[//]: # (- [ ] Adjust the [pluginGroup]&#40;./gradle.properties&#41;, [plugin ID]&#40;./src/main/resources/META-INF/plugin.xml&#41; and [sources package]&#40;./src/main/kotlin&#41;.)

[//]: # (- [ ] Adjust the plugin description in `README` &#40;see [Tips][docs:plugin-description]&#41;)

[//]: # (- [ ] Review the [Legal Agreements]&#40;https://plugins.jetbrains.com/docs/marketplace/legal-agreements.html?from=IJPluginTemplate&#41;.)

[//]: # (- [ ] [Publish a plugin manually]&#40;https://plugins.jetbrains.com/docs/intellij/publishing-plugin.html?from=IJPluginTemplate&#41; for the first time.)

[//]: # (- [ ] Set the `PLUGIN_ID` in the above README badges.)

[//]: # (- [ ] Set the [Plugin Signing]&#40;https://plugins.jetbrains.com/docs/intellij/plugin-signing.html?from=IJPluginTemplate&#41; related [secrets]&#40;https://github.com/JetBrains/intellij-platform-plugin-template#environment-variables&#41;.)

[//]: # (- [ ] Set the [Deployment Token]&#40;https://plugins.jetbrains.com/docs/marketplace/plugin-upload.html?from=IJPluginTemplate&#41;.)

[//]: # (- [ ] Click the <kbd>Watch</kbd> button on the top of the [IntelliJ Platform Plugin Template][template] to be notified about releases containing new features and fixes.)

<!-- Plugin description -->
This plugin aims to help users better understand their code writing by counting the modifications of the source code in the project directory opened by the current IDE. It groups and counts the number of added lines of code, deleted lines of code, and modified files within a certain time period, and finally presents the results in a list form, allowing users to have a clear understanding of the overall code writing situation of their projects.

本插件旨在通过统计当前IDE打开的项目目录中的源代码修改情况，帮助用户更好地了解自己的代码编写情况。通过分组统计某个时间段内的添加代码行数、删除代码行数、修改文件数量，最后用列表形式展现，让用户清晰地了解自己项目的整体编写情况。
<!-- Plugin description end -->

## Installation

- Using IDE built-in plugin system:
  
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "intellij-platform-git-stats-plugin"</kbd> >
  <kbd>Install Plugin</kbd>
  
- Manually:

  Download the [latest release](https://github.com/zhensherlock/intellij-platform-git-stats-plugin/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>


---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
[docs:plugin-description]: https://plugins.jetbrains.com/docs/intellij/plugin-user-experience.html#plugin-description-and-presentation