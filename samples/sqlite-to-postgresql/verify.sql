-- Run independently with psql -X -v ON_ERROR_STOP=1 -f verify.sql.
-- Compare against constants, not a second Scriptella query.
DO $$
BEGIN
    IF EXISTS (
        WITH expected(id, big_value, amount, label, note) AS (VALUES
            (1, 9007199254740993::bigint, 123.125::numeric, 'Hello café — 東京 😀'::text, NULL::text),
            (2, 9223372036854775807::bigint, -0.5::numeric, 'naïve Ελληνικά', ''),
            (3, '-9223372036854775808'::bigint, 0::numeric, '', 'ordinary text'),
            (4, NULL::bigint, NULL::numeric, NULL::text, NULL::text)
        )
        (SELECT * FROM sqlite_records EXCEPT ALL SELECT * FROM expected)
        UNION ALL
        (SELECT * FROM expected EXCEPT ALL SELECT * FROM sqlite_records)
    ) THEN
        RAISE EXCEPTION 'SQLite migration values differ from the expected fixture';
    END IF;
END $$;
SELECT 'PASS: exact integers, common numerics, Unicode, NULL and empty text' AS result;
