# JSON and JSON Lines examples (next release)

These examples require the JSON driver in the current source build; Scriptella
1.6 does not include it. Build the examples archive with `ant examples`, then
unzip it and change to its `json/` directory. Java 17 or newer is required.
The archive includes Scriptella, Jackson Core and H2 in `../lib/`; the H2
connections declare their JDBC classpath explicitly. No database server is needed.

**Dynamic JSON values use unquoted `?{expression}`, including strings.**
For example, `"id": ?{customer_id}` emits a typed value; `"id": "$customer_id"`,
`"id": "${customer_id}"` and `"id": "?{customer_id}"` emit literal strings.
The driver handles quoting and escaping. Dollar substitution remains available
in supported configuration, such as `url="${output.file}"` or
`<include href="${template}.json"/>`; it does not interpolate JSON strings.
The same rule applies to JSON Lines.

```sh
mkdir -p output
java -jar ../lib/scriptella.jar json-to-db.etl.xml
java -jar ../lib/scriptella.jar db-to-json.etl.xml
java -jar ../lib/scriptella.jar json-to-json.etl.xml
java -jar ../lib/scriptella.jar jsonl-to-jsonl.etl.xml
```

| Job | Demonstrates | Output | Expected values |
| --- | --- | --- | --- |
| [json-to-db.etl.xml](json-to-db.etl.xml) | Nested array selection, JDBC bindings, array indexes and literal dotted keys; read back stored H2 rows before closing the in-memory database | `output/imported-customers.json` | [json-to-db.expected.json](json-to-db.expected.json) |
| [db-to-json.etl.xml](db-to-json.etl.xml) | Seed H2 and export ordered rows using typed nested mappings | `output/db-customers.json` | [db-to-json.expected.json](db-to-json.expected.json) |
| [json-to-json.etl.xml](json-to-json.etl.xml) | Rename fields and copy structured lists from [customers.json](customers.json) | `output/transformed-customers.json` | [json-to-json.expected.json](json-to-json.expected.json) |
| [jsonl-to-jsonl.etl.xml](jsonl-to-jsonl.etl.xml) | Transform [customers.jsonl](customers.jsonl), inferring format from both filenames | `output/transformed-customers.jsonl` | The same transformation objects, one compact object per line |

All paths resolve relative to the ETL file. Each job runs independently.
Generated files under `output/` are excluded from the examples and source ZIPs,
so running these jobs before packaging does not add their results to either archive.
Output files are overwritten when the first record is emitted. For an optional
repeatable check of all four jobs and their typed results, run `python3 verify.py`
(Python 3 is required only for this optional acceptance helper and
`ant test-distribution`; it is not a Scriptella runtime dependency). Scriptella
performs all loading, database population, transformations and exports. The
helper only invokes the jobs and verifies their results. It creates `output/` and
compares structures and exact decimal values, allowing whitespace and decimal
scale to differ. `python3 verify.py --jar /path/to/scriptella.jar` selects a
separate binary distribution while retaining the example's bundled H2 dependency.

The source query `response.customers` selects an array using a simple path,
not JSONPath. Each selected record is exposed as `row`. Existing expressions
access `row.customer.name`, `row.items[0].price` and `row['external.code']`.
An empty standard JSON query reads a root array or a single root object. JSON
Lines queries must be empty and read one complete value per physical line.
An explicit connection property `format=json` or `format=jsonl` overrides
filename inference; `.jsonl` and `.ndjson` select JSON Lines otherwise.

Output scripts are JSON object templates with unquoted `?{expression}` values.
Strings, numbers, booleans, null, maps and lists retain their types. Never quote
a placeholder or construct escaped JSON through text substitution. Placeholder
text inside a JSON string remains literal. Dates require explicit conversion;
arbitrary Java objects are unsupported. Both formats use the same templates.

Input is streamed one record at a time; memory includes the current record
(and physical line for JSON Lines), not the whole document. Very large records
can still exhaust memory. Parsing preserves integers and decimal precision;
expression arithmetic and JDBC conversions may change it. These fixtures use
explicit nulls; real inputs may also omit fields.

For optional map fields, use `row.get('note')` and
`text:ifNull(row.get('note'), 'No note')`; use `row.containsKey('note')` to
distinguish a missing key from explicit null. Guard a missing or null parent
before accessing nested fields. See the
[JSON guide](https://github.com/scriptella/scriptella-etl/blob/master/docs/json.md) for
complete optional-field examples and JDBC timestamp conversion templates,
including timezone and precision considerations.

> [!WARNING]
> **Zero source rows leave existing output untouched**, and missing destinations
> remain absent; standard JSON does not automatically produce `[]`. For scheduled
> exports, use a fresh staging path for each run, check execution success, and
> explicitly handle an absent file as an empty result before publishing.
> Output is nontransactional and can be partial on failure, even when it is valid
> JSON or complete JSON Lines. Never publish output from a failed execution.

A handled mapping error still prevents successful writer commit. Commit
finalizes and closes output before execution succeeds.

The complete driver reference at `drivers/src/java/scriptella/driver/json/package.html`
in the source tree covers properties, paths, supported values, parser limits and
failure behavior. In the binary ZIP, open
`docs/api/scriptella/driver/json/package-summary.html` for that reference.
