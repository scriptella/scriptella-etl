#!/usr/bin/env python3
"""Run the packaged JSON examples and check typed results (Python 3 only)."""
import argparse
import json
from decimal import Decimal
from pathlib import Path
import subprocess


def load(text):
    return json.loads(text, parse_float=Decimal, parse_int=Decimal)


def equal(actual, expected):
    # bool and Decimal must remain distinct (Python otherwise treats True == 1).
    if type(actual) is not type(expected):
        return False
    if isinstance(actual, dict):
        return actual.keys() == expected.keys() and all(
            equal(actual[key], expected[key]) for key in actual)
    if isinstance(actual, list):
        return len(actual) == len(expected) and all(
            equal(a, b) for a, b in zip(actual, expected))
    return actual == expected


def main():
    root = Path(__file__).resolve().parent
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--jar', type=Path, default=root / '../lib/scriptella.jar')
    args = parser.parse_args()
    jar = args.jar.resolve(strict=True)
    (root / 'output').mkdir(exist_ok=True)
    jobs = [
        ('json-to-db', 'imported-customers.json', 'json-to-db'),
        ('db-to-json', 'db-customers.json', 'db-to-json'),
        ('json-to-json', 'transformed-customers.json', 'json-to-json'),
        ('jsonl-to-jsonl', 'transformed-customers.jsonl', 'json-to-json'),
    ]
    for job, output, expected in jobs:
        output_path = root / 'output' / output
        if output_path.exists():
            output_path.unlink()
        result = subprocess.run(
            ['java', '-jar', str(jar), str(root / (job + '.etl.xml'))],
            text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
        if result.returncode:
            raise RuntimeError(f'{job} failed:\n{result.stdout}')
        text = output_path.read_bytes().decode('utf-8')
        if output.endswith('.jsonl'):
            if not text.endswith('\n') or '\r' in text:
                raise AssertionError(f'{job}: expected LF-terminated JSON Lines')
            actual = [load(line) for line in text.splitlines()]
        else:
            actual = load(text)
        expected_value = load((root / (expected + '.expected.json')).read_text(encoding='utf-8'))
        if not equal(actual, expected_value):
            raise AssertionError(f'{job}: output differs from {expected}.expected.json')
        print(f'{job}: typed output passed')


if __name__ == '__main__':
    main()
