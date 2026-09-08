# Scriptella

[![GitHub stars](https://img.shields.io/github/stars/scriptella/scriptella-etl?style=flat&logo=github)](https://github.com/scriptella/scriptella-etl/stargazers)
[![CI](https://github.com/scriptella/scriptella-etl/actions/workflows/ci.yml/badge.svg)](https://github.com/scriptella/scriptella-etl/actions/workflows/ci.yml)
[![Maven Central](https://img.shields.io/maven-central/v/org.scriptella/scriptella-core?logo=apachemaven)](https://central.sonatype.com/artifact/org.scriptella/scriptella-core)
[![GitHub release](https://img.shields.io/github/v/release/scriptella/scriptella-etl?logo=github)](https://github.com/scriptella/scriptella-etl/releases/latest)

[English](README.md) · 简体中文 · [한국어](README.ko.md)

Scriptella 是一个轻量级、由 XML 驱动的 Java ETL 和数据库迁移工具。你可以直接使用 SQL 和脚本语言移动、转换数据，无需专有的转换语言或图形化设计器。

## 为什么选择 Scriptella？

* **SQL 优先：** 针对每个数据源使用最合适的语言，不必学习专有的转换语言。
* **轻量级：** 只需一个配置文件即可运行作业，无需服务器或图形化设计器。
* **适合自动化：** 将迁移和数据作业纳入版本控制，在本地、构建流水线或计划任务中运行。

## 项目状态

最新稳定版本可从 [GitHub Releases](https://github.com/scriptella/scriptella-etl/releases) 和 [Maven Central](https://central.sonatype.com/artifact/org.scriptella/scriptella-core) 获取。

发布详情和兼容性变更请参阅[变更日志](CHANGELOG.md)。

## 要求

当前 Scriptella 版本需要 Java 17 或更高版本。需要 Java 8 兼容性时，请使用
Scriptella 1.3。

## 获取 Scriptella

### 二进制发行版

从 [GitHub Releases](https://github.com/scriptella/scriptella-etl/releases) 或 [https://scriptella.org/download.html](https://scriptella.org/download.html) 下载已发布的版本。

解压二进制发行版，进入解压后的目录并运行 ETL 文件：

```bash
java -jar scriptella.jar path/to/file.etl.xml
```

按需通过连接的 `classpath` 属性添加 JDBC 驱动和其他 provider JAR。请参阅[教程](https://scriptella.org/tutorial.html)和[参考文档](https://scriptella.org/reference/)。

### 实验性的 curl 安装程序

使用 `curl` 安装最新的 Scriptella 版本：

```bash
curl -fsSL https://scriptella.org/install.sh | sh
```

安装程序会将 Scriptella 放在 `${HOME}/.local/scriptella` 下。它会向现有的启动文件添加 `${HOME}/.local/scriptella/bin`，或者输出可手动添加的确切 PATH 命令。保证可用的命令是随包提供的 `scriptella.sh` 启动器：

```bash
scriptella.sh path/to/file.etl.xml
```

安装程序不会安装 Java；Scriptella 要求 Java 17 或更高版本。安装程序更新启动文件后，请启动新 shell 或重新加载该文件，再使用 `scriptella.sh`。上面的手动 ZIP 安装和 `java -jar scriptella.jar` 仍然可用。

### 快速开始

在 `scriptella.jar` 旁创建 `people.csv`：

```csv
id,name
1,Ada
2,Grace
```

然后在同一目录创建 `csv-to-sql.etl.xml`：

```xml
<!DOCTYPE etl SYSTEM "http://scriptella.org/dtd/etl.dtd">
<etl>
    <connection id="input" driver="csv" url="people.csv"/>
    <connection id="output" driver="text" url="load.sql"/>

    <query connection-id="input">
        <script connection-id="output">
            INSERT INTO people (id, name) VALUES ($id, '$name');
        </script>
    </query>
</etl>
```

执行流程是从源数据行到嵌套操作：

1. `<query connection-id="input">` 向 CSV 连接请求数据行。CSV driver 将第一行视为表头，并为之后的每一行生成一个数据行。
2. 嵌套的 `<script connection-id="output">` 对每个生成的数据行执行一次。它的 `connection-id` 选择目标；嵌套关系提供当前源数据行。
3. `$id` 和 `$name` 会展开为当前数据行中的值。本例写入文本，因此这种替换是有意的。目标为 JDBC 时，请使用 `?id` 和 `?name` 参数安全地绑定 SQL 值。

在该目录中运行：

```bash
java -jar scriptella.jar csv-to-sql.etl.xml
cat load.sql
```

生成的 `load.sql` 会为每个 CSV 数据行包含一条 `INSERT` 语句。整个过程不需要数据库或 JDBC driver。

### 首次迁移

将目标 `<script>` 嵌套在源 `<query>` 中，即可转换或复制每一行数据，这正是 Scriptella 的强大之处。完整的 MySQL 到 PostgreSQL 示例会通过 JDBC 参数绑定直接写入数据库，请参阅 [docs/first-migration.md](docs/first-migration.md)。[教程](https://scriptella.org/tutorial.html)还提供了更多数据库和文件集成示例。

### 模块和 drivers

Scriptella 分为 core 模块和可选的 provider 模块。
`scriptella-core` 包含 ETL 引擎、service-provider API 和通用 JDBC bridge。应用程序提供任意厂商的 JDBC driver 即可使用；厂商 JAR 不包含在 Scriptella core 中。

`scriptella-drivers` 包含一组有明确取舍的内置适配器，包括数据库别名、CSV、text、Velocity、Spring、mail 和 scripting provider。它依赖 `scriptella-core`，但 core 不依赖 drivers 模块。第三方 driver 和 provider 可以通过连接的 `classpath` 属性单独提供。

### Maven 坐标

已发布的 artifact 使用 group ID `org.scriptella`（从 1.2 开始）。当前依赖版本和
Maven 使用示例请参阅 [Maven Central](https://central.sonatype.com/artifact/org.scriptella/scriptella-core)
和[英文 README](README.md)。对于大多数基于 Maven 的 JDBC 使用场景，`scriptella-core`
配合数据库的 JDBC driver 即已足够。需要 Scriptella 内置别名或专用 provider 时再添加
`scriptella-drivers`，也可以使用二进制发行版或 all-in-one JAR 获取组装后的产品。
可选 provider 的运行时库仍可能需要单独提供；请参阅 provider 文档。

### 从源代码构建

需要 Java 17+ 和 Maven 3.6+：

```bash
mvn clean install
```

## 文档

* 网站：[https://scriptella.org](https://scriptella.org)
* 参考文档：[https://scriptella.org/reference/](https://scriptella.org/reference/)
* API 文档：[https://scriptella.org/docs/api/](https://scriptella.org/docs/api/)
* Core 数据库兼容性和验证目标：[docs/core-database-compatibility.md](docs/core-database-compatibility.md)
* 命令行使用约定和自动化模板：[docs/cli-usage.md](docs/cli-usage.md)
* 发布历史：[CHANGELOG.md](CHANGELOG.md)
* 维护者指南：[docs/MAINTAINING.md](docs/MAINTAINING.md)

打包文档也可能位于发行档案的 `docs/` 目录中。

## 支持和贡献

* **Bug 和功能：** [GitHub Issues](https://github.com/scriptella/scriptella-etl/issues)
* **讨论：** [GitHub Discussions](https://github.com/scriptella/scriptella-etl/discussions)
* **支持：** [scriptella.org/support.html](https://scriptella.org/support.html)；商业咨询：[scriptella@gmail.com](mailto:scriptella@gmail.com)

欢迎针对兼容性、正确性和维护工作的 pull request 以及范围明确的 issue 报告。目前项目重点不是广泛的功能开发。

## 许可

本软件遵循本目录中名为 `LICENSE` 的文件所载条款（Apache License, Version 2.0）。

如果 Scriptella 对你有帮助，欢迎在 GitHub 上为项目点个 star，帮助更多开发者发现它。

感谢你使用 Scriptella。

Scriptella Project Team  
[https://scriptella.org](https://scriptella.org)
