# Scriptella

[![GitHub stars](https://img.shields.io/github/stars/scriptella/scriptella-etl?style=flat&logo=github)](https://github.com/scriptella/scriptella-etl/stargazers)
[![CI](https://github.com/scriptella/scriptella-etl/actions/workflows/ci.yml/badge.svg)](https://github.com/scriptella/scriptella-etl/actions/workflows/ci.yml)
[![Maven Central](https://img.shields.io/maven-central/v/org.scriptella/scriptella-core?logo=apachemaven)](https://central.sonatype.com/artifact/org.scriptella/scriptella-core)
[![GitHub release](https://img.shields.io/github/v/release/scriptella/scriptella-etl?logo=github)](https://github.com/scriptella/scriptella-etl/releases/latest)

[English](README.md) · [简体中文](README.zh-CN.md) · 한국어

Scriptella는 가볍고 XML로 구동되는 Java ETL 및 데이터베이스 마이그레이션 도구입니다. 독점 변환 언어나 그래픽 디자이너 없이 SQL과 스크립트 언어를 직접 사용해 데이터를 이동하고 변환할 수 있습니다.

## Scriptella를 선택하는 이유

* **SQL 우선:** 독점 변환 언어를 새로 배우지 않고 각 데이터 소스에 가장 알맞은 언어를 사용합니다.
* **가벼움:** 서버나 그래픽 디자이너 없이 하나의 구성 파일로 작업을 실행합니다.
* **자동화에 적합:** 마이그레이션과 데이터 작업을 버전 관리에 넣고 로컬, 빌드 파이프라인 또는 스케줄에 따라 실행합니다.

## 프로젝트 상태

최신 안정 릴리스는 [GitHub Releases](https://github.com/scriptella/scriptella-etl/releases)와 [Maven Central](https://central.sonatype.com/artifact/org.scriptella/scriptella-core)에서 받을 수 있습니다.

릴리스 세부 사항과 호환성 변경 내용은 [변경 로그](CHANGELOG.md)를 참고하세요.

## 요구 사항

현재 Scriptella 릴리스는 Java 17 이상이 필요합니다. Java 8 호환성이
필요한 경우 Scriptella 1.3을 사용하세요.

## Scriptella 받기

### 바이너리 배포판

[GitHub Releases](https://github.com/scriptella/scriptella-etl/releases) 또는 [https://scriptella.org/download.html](https://scriptella.org/download.html)에서 릴리스된 버전을 다운로드하세요.

바이너리 배포판의 압축을 풀고 해당 디렉터리로 이동한 다음 ETL 파일을 실행합니다.

```bash
java -jar scriptella.jar path/to/file.etl.xml
```

필요에 따라 연결의 `classpath` 속성으로 JDBC 드라이버와 다른 provider JAR을 추가하세요. [튜토리얼](https://scriptella.org/tutorial.html)과 [레퍼런스](https://scriptella.org/reference/)를 참고하면 됩니다.

### 실험적 curl 설치 프로그램

`curl`로 최신 Scriptella 릴리스를 설치합니다.

```bash
curl -fsSL https://scriptella.org/install.sh | sh
```

설치 프로그램은 Scriptella를 `${HOME}/.local/scriptella` 아래에 설치합니다. 기존 startup 파일에 `${HOME}/.local/scriptella/bin`을 추가하거나, 직접 추가할 수 있는 정확한 PATH 명령을 출력합니다. 보장되는 실행 명령은 패키지에 포함된 `scriptella.sh` launcher입니다.

```bash
scriptella.sh path/to/file.etl.xml
```

설치 프로그램은 Java를 설치하지 않습니다. Scriptella는 Java 17 이상이 필요합니다. 설치 프로그램이 startup 파일을 변경했다면 새 shell을 시작하거나 해당 파일을 다시 불러온 뒤 `scriptella.sh`를 사용하세요. 위의 수동 ZIP 설치와 `java -jar scriptella.jar`도 계속 사용할 수 있습니다.

### 빠른 시작

`scriptella.jar`와 같은 디렉터리에 `people.csv`를 만드세요.

```csv
id,name
1,Ada
2,Grace
```

그 디렉터리에 `csv-to-sql.etl.xml`도 만듭니다.

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

흐름은 소스 행에서 중첩된 작업으로 이어집니다.

1. `<query connection-id="input">`이 CSV 연결에 행을 요청합니다. CSV driver는 첫 줄을 헤더로 취급하고 나머지 각 줄에 대해 하나의 행을 생성합니다.
2. 중첩된 `<script connection-id="output">`은 생성된 각 행마다 한 번 실행됩니다. `connection-id`가 대상 연결을 선택하고, 중첩 구조가 현재 소스 행을 전달합니다.
3. `$id`와 `$name`은 현재 행의 값으로 확장됩니다. 이 예제는 텍스트를 출력하므로 치환을 의도적으로 사용합니다. 대상이 JDBC라면 `?id`와 `?name` 매개변수를 사용해 SQL 값을 안전하게 바인딩하세요.

해당 디렉터리에서 실행합니다.

```bash
java -jar scriptella.jar csv-to-sql.etl.xml
cat load.sql
```

생성된 `load.sql`에는 CSV 데이터 행마다 하나의 `INSERT` 문이 들어갑니다. 데이터베이스나 JDBC driver가 없어도 실행할 수 있습니다.

### 첫 마이그레이션

소스 `<query>` 안에 대상 `<script>`를 중첩하면 각 행을 변환하거나 복사할 수 있습니다. JDBC 매개변수 바인딩으로 데이터베이스에 직접 기록하는 완전한 MySQL-to-PostgreSQL 예제는 [docs/first-migration.md](docs/first-migration.md)를 참고하세요. [튜토리얼](https://scriptella.org/tutorial.html)에는 데이터베이스와 파일을 연동하는 추가 예제도 있습니다.

### 모듈과 drivers

Scriptella는 core 모듈과 선택적 provider 모듈로 나뉩니다.
`scriptella-core`에는 ETL 엔진, service-provider API, 범용 JDBC bridge가 들어 있습니다. 애플리케이션에서 원하는 vendor JDBC driver를 제공하면 사용할 수 있으며 vendor JAR은 Scriptella core에 포함되지 않습니다.

`scriptella-drivers`에는 데이터베이스 별칭, CSV, text, Velocity, Spring, mail 및 scripting provider를 비롯한 기본 어댑터 모음이 들어 있습니다. 이 모듈은 `scriptella-core`에 의존하지만 core는 drivers 모듈에 의존하지 않습니다. 서드파티 driver와 provider는 연결의 `classpath` 속성으로 별도 제공할 수 있습니다.

### Maven 좌표

배포된 artifact는 (1.2부터) group ID `org.scriptella`를 사용합니다. 현재
의존성 버전과 Maven 사용 예시는 [Maven Central](https://central.sonatype.com/artifact/org.scriptella/scriptella-core)과
[영문 README](README.md)에서 확인하세요. 대부분의 Maven 기반 JDBC 사용에서는
`scriptella-core`와 데이터베이스의 JDBC driver면 충분합니다. Scriptella의 기본
별칭이나 특수 provider가 필요할 때 `scriptella-drivers`를 추가하거나, 조립된
제품인 바이너리 배포판/all-in-one JAR을 사용하세요. 선택적 provider 런타임
라이브러리는 별도로 제공해야 할 수 있으므로 provider 문서를 확인하세요.

### 소스에서 빌드

Java 17+ 및 Maven 3.6+이 필요합니다.

```bash
mvn clean install
```

## 문서

* 웹사이트: [https://scriptella.org](https://scriptella.org)
* 레퍼런스: [https://scriptella.org/reference/](https://scriptella.org/reference/)
* API 문서: [https://scriptella.org/docs/api/](https://scriptella.org/docs/api/)
* Core 데이터베이스 호환성과 검증 대상: [docs/core-database-compatibility.md](docs/core-database-compatibility.md)
* 명령줄 사용 규약과 자동화 템플릿: [docs/cli-usage.md](docs/cli-usage.md)
* 릴리스 기록: [CHANGELOG.md](CHANGELOG.md)
* 유지보수 가이드: [docs/MAINTAINING.md](docs/MAINTAINING.md)

패키지 문서는 배포 아카이브의 `docs/` 아래에 포함될 수도 있습니다.

## 지원과 기여

* **버그와 기능:** [GitHub Issues](https://github.com/scriptella/scriptella-etl/issues)
* **토론:** [GitHub Discussions](https://github.com/scriptella/scriptella-etl/discussions)
* **지원:** [scriptella.org/support.html](https://scriptella.org/support.html); 상업 문의: [scriptella@gmail.com](mailto:scriptella@gmail.com)

호환성, 정확성 및 유지보수에 관한 범위가 명확한 issue 보고와 pull request를 환영합니다. 현재 프로젝트의 초점은 광범위한 기능 개발이 아닙니다.

## 라이선스

이 소프트웨어는 이 디렉터리의 `LICENSE` 파일에 명시된 조건(Apache License, Version 2.0)에 따라 라이선스됩니다.

Scriptella가 유용하다면 GitHub에서 프로젝트에 별표를 눌러 더 많은 개발자가 발견할 수 있도록 도와주세요.

Scriptella를 사용해 주셔서 감사합니다.

Scriptella Project Team  
[https://scriptella.org](https://scriptella.org)
