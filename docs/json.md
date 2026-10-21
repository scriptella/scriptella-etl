# JSON driver (next release)

The current source build supports streaming JSON and JSON Lines input and typed
JSON object templates for output. This driver is not included in Scriptella 1.6.
Use `driver="json"` with separate connections for input and output.

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
can exhaust memory. Output opens on the first invocation only. Zero source rows
leave destinations untouched or absent. Output is nontransactional; failures
can leave partial files. Check execution success before consuming exports.

For the complete reference, open `docs/api/scriptella/driver/json/package-summary.html`
in the binary ZIP, or `drivers/src/java/scriptella/driver/json/package.html` in
the source tree. It covers encoding, selection paths, typed mappings, numeric
precision, missing/null values, memory limits and failure behavior.

For Maven applications, add `org.scriptella:scriptella-drivers` at the matching
Scriptella version and explicitly include `com.fasterxml.jackson.core:jackson-core:2.22.4`
plus any JDBC driver used by the job. Jackson is an optional provider dependency;
the Ant binary and examples archives already bundle its intact JAR and notices.
