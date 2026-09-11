# Issue #61 — Public Scriptella Testcontainers Suite Plan

## Goal

Create `scriptella/scriptella-testcontainers`, a small public project that
answers:

> Can this Scriptella version perform a small but meaningful real ETL correctly
> against this database?

The initial suite covers PostgreSQL, MariaDB, Oracle Free, and Microsoft SQL
Server. It is a public real-database compatibility suite, not the complete
Scriptella certification matrix.

The terminology change from “smoke tests” to “database compatibility tests”
reflects what the existing test contract already verifies. It does not expand
the scope of Issue #61 or change the lightweight implementation strategy.

Phases and checkboxes are only for tracking progress and making interrupted
work easy to resume. They are not approval gates.

## Agreed scope

- [x] Keep the suite in a separate public repository named
  `scriptella-testcontainers`.
- [x] Use one Maven module, JUnit 5, and Testcontainers.
- [x] Depend on `scriptella-core` and `scriptella-drivers` through one
  `scriptella.version` Maven property.
- [x] Keep vendor JDBC drivers as explicit test dependencies.
- [x] Use checked-in Scriptella ETL XML fixtures.
- [x] Cover one reasonable current generally available version each of
  PostgreSQL, MariaDB, Oracle Free, and SQL Server.
- [x] Keep MySQL outside the initial public suite; it remains covered by the
  private certification suite.
- [x] Run databases sequentially on developer machines.
- [x] Prefer Colima's Docker runtime for local macOS development; any
  Testcontainers-compatible Docker runtime remains supported.
- [x] Use separate Linux CI jobs for the four databases.
- [x] Do not attempt to support SQL Server locally on Apple Silicon; Linux CI is
  the reliable environment for that target.

## Shared compatibility-test contract

Each database test should:

- [ ] Start a real database with Testcontainers.
- [ ] Create simple source and destination tables.
- [ ] Insert a few representative source rows containing an integer, an exact
  decimal, normal text, Unicode, `NULL`, and a whole-second timestamp without
  time-zone ambiguity.
- [ ] Run a real checked-in Scriptella ETL file using the documented Scriptella
  alias: `postgresql`, `mariadb`, `oracle`, or `mssql`.
- [ ] Query the source and copy the rows to the destination through Scriptella.
- [ ] Verify the persisted destination rows independently through the vendor
  JDBC driver, including the representative values above.
- [ ] Run one deliberately failing Scriptella transaction and verify through
  JDBC that its partial changes were rolled back.

The purpose is to catch obvious read/write, type-conversion, `NULL`, Unicode,
and transaction regressions. This is not intended to exhaustively test JDBC.
Small database-specific SQL and fixture differences are preferable to a custom
abstraction layer.

## Phase 1 — Bootstrap the repository

- [x] Initialize the separate local `scriptella-testcontainers` repository with
  the normal Scriptella license and a minimal `.gitignore`.
- [x] Add a single-module Maven build using the documented minimum Java version.
- [x] Add JUnit 5 and the Testcontainers BOM.
- [x] Add `scriptella-core` and `scriptella-drivers` using the same
  `${scriptella.version}` property.
- [x] Add explicit test dependencies for the PostgreSQL, MariaDB, Oracle, and
  SQL Server JDBC drivers and Testcontainers database modules.
- [x] Select and pin one normal supported database image version for each
  target, staying close to Scriptella 1.5 validation versions where practical.
- [x] Add a small test-resource layout for checked-in ETL XML and
  database-specific SQL.
- [x] Document how to override the Scriptella dependency, for example
  `mvn verify -Dscriptella.version=1.6-SNAPSHOT` after installing that snapshot
  from a local `scriptella-etl` checkout.

## Phase 2 — Implement the first lightweight database

- [x] Start with MariaDB or PostgreSQL, choosing whichever produces the simplest
  first working test.
- [x] Implement the shared compatibility-test contract with a checked-in ETL fixture.
- [x] Pass Testcontainers-generated connection information to Scriptella
  without fixed host ports.
- [x] Keep the container lifecycle shared for that database test class so the
  database is not restarted for every assertion.
- [x] Verify the copied data and rollback independently with the vendor JDBC
  driver.
- [x] Make the database runnable by itself with a short documented Maven
  command such as `mvn verify -Ddatabase=mariadb`.

## Phase 3 — Add the remaining databases

### Other lightweight database

- [x] Apply the same compatibility-test contract to the remaining PostgreSQL or MariaDB
  target.
- [x] Keep database-specific DDL or ETL fixture differences explicit and small.
- [x] Confirm both lightweight databases run sequentially as part of the full
  local suite.

### Oracle Free

- [x] Add a normal supported Oracle Free Testcontainers image and Oracle JDBC
  driver.
- [x] Adapt the simple tables and ETL fixture only where Oracle syntax requires
  it.
- [x] Pass the shared compatibility-test contract using the `oracle` Scriptella alias.

### Microsoft SQL Server

- [x] Add a normal supported Microsoft SQL Server Linux image and Microsoft JDBC
  driver.
- [x] Accept the image license in the standard Testcontainers-supported way.
- [x] Use test-only connection settings suitable for the local container.
- [x] Adapt the simple tables and ETL fixture only where SQL Server syntax
  requires it.
- [x] Pass the shared compatibility-test contract on Linux using the `mssql` Scriptella
  alias.
- [x] Document that SQL Server is not supported locally on Apple Silicon; do not
  add emulation or another workaround.

## Phase 4 — Add basic public CI

- [x] Publish the local repository as the public
  `scriptella/scriptella-testcontainers` repository.
- [x] Add one Linux GitHub Actions job per database so targets are easy to
  identify and may run in parallel.
- [x] Run each job against the repository's normal pinned stable Scriptella
  version.
- [x] Allow the workflow to receive or set a `scriptella.version` override for
  testing another version available from a configured Maven repository.
- [x] Add a simple current-source path that checks out `scriptella-etl`, installs
  its Maven artifacts in the same job, and runs the suite with that version.
  Use this path for unpublished development snapshots, which cannot resolve
  from a version override alone.
- [x] Keep the workflow self-contained and use standard Maven dependency caching
  only if it is useful.
- [x] Complete one successful public CI run across all four databases.

Do not add custom image caching, registries, artifact protocols, custom log
collection, self-hosted runners, or a detailed CI policy unless a concrete
problem later requires one.

The initial public workflows run all four databases in parallel. If runner
resource or duration constraints become a concrete problem, PostgreSQL and
MariaDB are the likely minimum always-on subset; Oracle Free and SQL Server may
then move to default-branch, manual, or another lower-frequency path. This is a
future fallback, not current scope.

The `scriptella-etl` workflow builds and installs the current source in each
job, checks out the public compatibility suite alongside it, and invokes the
same wrapper with the locally installed `1.6-SNAPSHOT` artifacts.

## Phase 5 — Finish minimal documentation

- [ ] Add a concise README explaining what the repository tests.
- [ ] List PostgreSQL, MariaDB, Oracle Free, and SQL Server as the initial
  targets.
- [ ] List Java, Maven, and a Testcontainers-compatible Docker runtime as
  prerequisites, with Colima documented as the preferred local macOS runtime.
- [ ] Document the Colima setup and Testcontainers environment variables needed
  for local database runs.
- [ ] Document the commands for one selected database and the complete
  sequential suite.
- [ ] Document the `scriptella.version` override and locally installed snapshot
  flow.
- [ ] Document the SQL Server and Apple Silicon limitation.
- [ ] Explain that this is a small public real-database compatibility suite rather than
  the complete private certification matrix.
- [ ] Mention that MySQL is covered separately by private Scriptella
  certification; do not imply MariaDB proves MySQL compatibility.
- [ ] Invite community contributions for additional concrete compatibility
  cases.
- [ ] Link the public repository from
  `scriptella-etl/docs/core-database-compatibility.md` and issue #61.

Once all four databases execute the shared contract locally or through public
Linux CI, the initial initiative is complete.

## Deferred unless a real need appears

- Additional database or version matrices.
- Broader JDBC type, performance, upgrade, or reliability testing.
- Docker Compose, custom images, registries, Kubernetes, or self-hosted
  runners.
- SQL Server emulation on Apple Silicon.
- Custom container logging, failure classification, CI artifact handling,
  or Docker-layer caching.
- Runtime, disk, memory, image-size, or repeated-run qualification.
- Formal acceptance gates, status-check policy, outage policy, or baseline
  evidence records.
- Detailed dependency-update procedures or certification infrastructure.
