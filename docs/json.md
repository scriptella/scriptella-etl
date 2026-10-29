# JSON driver

**Upcoming in 1.7** — available in source builds; not included in the current stable release.

The JSON driver supports streaming JSON and JSON Lines input and typed JSON
object templates for output. Use `driver="json"` with separate connections for
input and output.

## Use typed placeholders for JSON values

**Use unquoted `?{expression}` for every dynamic JSON value, including strings.**
The driver evaluates the expression and serializes its result with the correct
JSON type and escaping:

```text
{
    "id": ?{customer_id},
    "name": ?{customer_name},
    "active": ?{active},
    "items": ?{row.items},
    "source": "customer-import"
}
```

With a numeric `customer_id` of 123:

| Template | Output | Meaning |
| --- | --- | --- |
| `"id": ?{customer_id}` | `"id": 123` | Evaluated, typed value |
| `"id": "$customer_id"` | `"id": "$customer_id"` | Literal string |
| `"id": "${customer_id}"` | `"id": "${customer_id}"` | Literal string |
| `"id": "?{customer_id}"` | `"id": "?{customer_id}"` | Literal string |

A string result is quoted and escaped automatically; numbers, booleans, null,
maps and lists retain their types. Do not put quotes around a placeholder or
interpolate variables inside a JSON string. To construct a dynamic string,
evaluate the whole string expression in one unquoted `?{...}` placeholder.
This applies equally to JSON and JSON Lines.

Dollar substitution still applies in configuration that supports it, such as
`url="${output.file}"` and `<include href="${template}.json"/>`, and in other
drivers' text substitution contexts. It does not interpolate JSON template
strings. Inside `?{...}`, reference variables directly, such as `customer_id`
or `row.customer.name`.

## Optional fields and null values

JSON objects are maps. Use `row.get('note')` for an optional field: both a
missing key and an explicit JSON `null` return null. Use
`row.containsKey('note')` when you need to distinguish them. A default value
can be supplied with `text:ifNull`:

```text
{
    "note": ?{row.get('note')},
    "notePresent": ?{row.containsKey('note')},
    "displayNote": ?{text:ifNull(row.get('note'), 'No note')},
    "customerName": ?{row.get('customer') == null ? null : row.get('customer').get('name')}
}
```

For input `{"note": null, "customer": null}`, this emits:

```json
{"note": null, "notePresent": true, "displayNote": "No note", "customerName": null}
```

For input `{}`, the result is the same except that `notePresent` is false.
The nested expression checks the optional parent before accessing its child;
it also returns null when `customer` is an object without a `name` key. It
assumes a present, non-null `customer` is an object. A non-null note, including
an empty string, is retained by `text:ifNull`. Null-valued output fields are
written as `null`, not omitted. These expressions assume each `row` is an object.

## Export JDBC dates and timestamps

JDBC dates and timestamps must be converted explicitly to a supported JSON
value. In a database query, alias the timestamp column as `"created_at"` and
use this output template:

```text
{
    "createdAt": ?{created_at == null ? null : date:format(created_at, 'yyyy-MM-dd HH:mm:ss.SSSXXX')},
    "createdAtEpochMillis": ?{created_at == null ? null : created_at.getTime()}
}
```

For a timestamp representing `2026-10-21T12:34:56.123Z`, with the JVM timezone
set to UTC, the result is:

```json
{"createdAt": "2026-10-21 12:34:56.123Z", "createdAtEpochMillis": 1792586096123}
```

Both fields become JSON null for a SQL NULL. `date:format` uses the JVM's
default timezone; choose it explicitly for reproducible exports, for example
`java -Duser.timezone=UTC -jar ../lib/scriptella.jar db-to-json.etl.xml`.
The timezone used when JDBC interprets a timestamp without a timezone also
depends on the database and driver configuration. The pattern above includes
an offset and milliseconds; `getTime()` returns numeric milliseconds since
the Unix epoch. Both examples discard any submillisecond timestamp precision.

## Run the examples

Build the binary and examples ZIPs with `ant zip examples`, then extract the
examples ZIP and follow `json/README.md`. It includes independent JSON-to-H2,
H2-to-JSON, JSON-to-JSON and JSON Lines jobs, with all runtime dependencies in
`lib/`. Run from the extracted `json/` directory:

```sh
mkdir -p output
java -jar ../lib/scriptella.jar json-to-db.etl.xml
java -jar ../lib/scriptella.jar db-to-json.etl.xml
java -jar ../lib/scriptella.jar json-to-json.etl.xml
java -jar ../lib/scriptella.jar jsonl-to-jsonl.etl.xml
```

An empty standard JSON query reads a root array or one root object; a simple
path such as `response.customers` selects a nested array. Records are exposed
as typed `row` values. Output scripts contain one JSON object template per
invocation, such as `{"id": ?{row.id}, "items": ?{row.items}}`. Placeholders
occupy whole values and retain types; do not quote them.

The `format=json` or `format=jsonl` connection property overrides filename
inference. Otherwise `.jsonl` and `.ndjson` select JSON Lines; all other names
use standard JSON. JSON Lines queries must be empty. Each input line is one
complete value; each output invocation writes one compact object plus LF.

Reading streams the document, retaining the current record; very large records
can exhaust memory. Output opens on the first invocation only.

> [!WARNING]
> **Zero source rows do not clear an existing export.** If no output script runs,
> an existing destination remains untouched and a missing destination remains
> absent. Standard JSON does not automatically produce `[]`. A successful job
> can therefore leave a previous run's data at the destination.
>
> For scheduled exports, write to a fresh, run-specific staging path and publish
> it only after execution succeeds. If no file was created, apply an explicit
> empty-result policy: publish `[]` for standard JSON, an empty file for JSON
> Lines, or report that no export was produced. Do not reuse an old staging file.
>
> Output is nontransactional; failures can leave partial files, even valid JSON
> or complete JSON Lines. Check execution success before publishing or consuming
> exports.

For the complete reference, open `docs/api/scriptella/driver/json/package-summary.html`
in the binary ZIP, or `drivers/src/java/scriptella/driver/json/package.html` in
the source tree. It covers encoding, selection paths, typed mappings, numeric
precision, missing/null values, memory limits and failure behavior.

For Maven applications, add `org.scriptella:scriptella-drivers` at the matching
Scriptella version and explicitly include `com.fasterxml.jackson.core:jackson-core:2.22.4`
plus any JDBC driver used by the job. Jackson is an optional provider dependency;
the Ant binary and examples archives already bundle its intact JAR and notices.
