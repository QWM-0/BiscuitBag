# BiscuitBag（饼干袋）

一个基于 Kotlin Multiplatform + Compose Multiplatform 的 Android 阅读追踪应用。以"饼干屑"（Breadcrumb）为最小阅读单位（每段一个），帮助用户追踪书籍阅读进度。

## 技术栈

| 类别 | 技术 |
|------|------|
| 语言 | Kotlin 2.4.10 |
| 平台 | Android（Kotlin Multiplatform，当前仅有 androidTarget） |
| UI 框架 | Compose Multiplatform 1.11.1 + Material 3 |
| 数据库 | SQLDelight 2.3.2（SQLite，AndroidSqliteDriver） |
| 导航 | Navigation Compose（`org.jetbrains.androidx.navigation:navigation-compose`） |
| 构建 | Gradle (Kotlin DSL)，AGP 8.7.3 |
| JVM 目标 | 17 |

关键依赖：`kotlinx-coroutines`、`kotlinx-serialization-json`、`kotlinx-datetime`、`androidx-lifecycle`。

## 项目结构

```
BiscuitBag/
├── build.gradle.kts            # 根构建文件：声明所有插件（不 apply）
├── settings.gradle.kts         # 项目设置，模块名 "BiscuitBag"，含 :composeApp
├── gradle.properties           # JVM 参数、AndroidX、代理配置
├── gradle/libs.versions.toml   # 版本目录（version catalog）
└── composeApp/
    ├── build.gradle.kts        # 模块构建：KMP + Android + SQLDelight 配置
    └── src/
        ├── commonMain/
        │   ├── kotlin/com/biscuitbag/
        │   │   ├── App.kt                          # 导航图（NavHost），路由定义
        │   │   ├── data/repository/
        │   │   │   └── BiscuitBagRepository.kt     # 数据仓库层，封装所有数据库操作
        │   │   ├── database/
        │   │   │   └── DatabaseDriverFactory.kt    # expect 声明
        │   │   └── ui/
        │   │       ├── screens/
        │   │       │   ├── BookListScreen.kt       # 书库列表
        │   │       │   ├── BookEditScreen.kt       # 添加/编辑书籍
        │   │       │   ├── ChapterListScreen.kt    # 章节列表
        │   │       │   ├── ChapterEditScreen.kt    # 添加/编辑章节
        │   │       │   ├── ReadingScreen.kt        # 阅读界面（饼干屑网格）
        │   │       │   └── StatsScreen.kt          # 阅读统计
        │   │       └── viewmodel/
        │   │           ├── BookListViewModel.kt
        │   │           ├── BookEditViewModel.kt
        │   │           ├── ChapterListViewModel.kt
        │   │           ├── ChapterEditViewModel.kt
        │   │           ├── ReadingViewModel.kt
        │   │           └── StatsViewModel.kt
        │   └── sqldelight/com/biscuitbag/
        │       └── BiscuitBag.sq                   # SQLDelight 表定义和命名查询
        └── androidMain/
            ├── AndroidManifest.xml
            ├── res/values/{themes,strings}.xml
            └── kotlin/com/biscuitbag/
                ├── MainActivity.kt                 # Android Activity 入口
                ├── database/
                │   └── DatabaseDriverFactory.kt    # actual 实现（AndroidSqliteDriver）
                └── import/
                    └── EpubImporter.kt             # EPUB 解析与导入
```

## 架构

- **导航**：单 Activity (`MainActivity`) 内使用 `NavHost` 手动管理路由，共 8 条路由（见 `App.kt`）。
- **状态管理**：每个 Screen 对应一个 ViewModel，使用 `MutableStateFlow` + `collectAsState()` 驱动 UI。ViewModel 持有 `BiscuitBagRepository` 引用。
- **数据层**：`BiscuitBagRepository` 封装所有 SQLDelight 查询操作，直接暴露同步方法（非 Flow，除 `getAllBooks` 外）。实体类（`BookEntity`、`ChapterEntity`、`BreadcrumbEntity`、`ReadingRecordEntity`）定义在 repository 文件中。
- **数据库**：4 张表 — `Book`、`Chapter`、`Breadcrumb`、`ReadingRecord`。表定义和命名 SQL 查询均写在 `BiscuitBag.sq` 中，SQLDelight 编译时生成类型安全的 Kotlin 访问代码。
- **平台抽象**：`DatabaseDriverFactory` 使用 KMP `expect`/`actual` 模式，common 声明接口，android 提供 `AndroidSqliteDriver` 实现。

## 数据库

- 文件：`BiscuitBag.sq`（SQLDelight 格式）
- 生成的包名：`com.biscuitbag.database`
- 数据库名：`biscuitbag.db`
- 表：
  - `Book` — 书籍（id, title, author, totalPages, type, coverPath, createdAt）
  - `Chapter` — 章节（id, bookId FK, chapterNumber, title, paragraphCount, createdAt）
  - `Breadcrumb` — 饼干屑（id, chapterId FK, paragraphIndex, isRead, readAt）
  - `ReadingRecord` — 每日阅读记录（id, date UNIQUE, breadcrumbsCompleted）

## 关键约定

1. 书籍标题：type=0（书本）时自动包裹 `《》`，ViewModel 的 `save()` 方法处理；编辑时 `unwrapTitle()` 去掉 `《》` 再填入输入框。
2. 章节目录：留空自动填充 `第N章`，N 为 `max(chapterNumber) + 1`。
3. 饼干屑数量：支持"按页数估算"（每页×4段）和手动输入两种方式。
4. 导航路由参数：`bookId` 和 `chapterId` 均使用 `NavType.LongType`。
5. Entity 类定义为 data class，放置于 `BiscuitBagRepository.kt` 文件底部，并有 SQLDelight 生成类型的私有扩展函数 `toEntity()`。
6. 导入路由（`"import"`）：composable 中直接调用 `onImportEpub?.invoke()` 并 `popBackStack()`。
7. EPUB 导入在 `MainActivity` 中处理（非 Composable），导入完成后调用 `recreate()` 刷新界面。

## 构建和运行

编译需要 **JDK 17+**。如果系统默认 Java 版本过低（如 Java 8），需显式指定 `JAVA_HOME`：

```bash
# 用 JDK 17 构建 debug APK
JAVA_HOME="D:/programming/jdk-17.0.3.7-hotspot" ./gradlew assembleDebug

# 安装到设备
./gradlew installDebug

# 清理构建
./gradlew clean
```

APK 产物路径：`composeApp/build/outputs/apk/debug/composeApp-debug.apk`

## CI / CD

项目配置了 GitHub Actions 自动构建，文件：`.github/workflows/build.yml`

| 触发方式 | 说明 |
|---------|------|
| `push` 到 `main` 分支 | 自动构建 |
| `pull_request` 到 `main` | 自动构建 |
| `workflow_dispatch`（手动） | 在 GitHub 仓库 Actions 页面手动触发 |

构建产物（APK）会上传为 Artifact，可在构建完成后下载。

### 发布 APK

另有发布工作流 `.github/workflows/release.yml`，两种触发方式：

| 触发方式 | 用法 |
|---------|------|
| 打 tag 推送 | `git tag v1.0.0 && git push --tags` |
| 手动触发 | GitHub Actions → **Release APK** → **Run workflow** → 填版本号 |

发布后 APK 会上传到 GitHub Releases 页面，国内用户可将下载链接中的 `github.com` 替换为 `hub.fastgit.org` 加速。

## 测试

项目当前没有任何单元测试或 UI 测试。添加测试时：
- 公共代码测试放在 `composeApp/src/commonTest/`
- Android 仪器化测试放在 `composeApp/src/androidTest/`
- Gradle 中需添加对应 test 依赖

## 代码风格

- Kotlin 官方编码风格（`kotlin.code.style=official`）
- 包命名：`com.biscuitbag.<layer>`
- 注释使用中文
- UI 字符串使用中文硬编码在 Composable 中（未使用 strings.xml 资源）
- 状态管理遵循：ViewModel 持有 `MutableStateFlow`，暴露为 `StateFlow`，Screen 通过 `collectAsState()` 订阅

## 防火墙/代理

`gradle.properties` 中配有机场代理（HTTP/HTTPS 127.0.0.1:7897）。在其他网络环境下可能需要注释掉这些配置。
