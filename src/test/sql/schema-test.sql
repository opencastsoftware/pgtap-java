BEGIN;

SELECT plan(12);

SELECT has_table(
        'test',
        'conditions',
        'The test schema has the conditions table'
    );

SELECT columns_are(
        'test',
        'conditions',
        ARRAY ['time', 'location', 'device', 'temperature', 'humidity'],
        'The conditions table has the expected columns'
    );

-- time column
SELECT col_not_null(
        'test',
        'conditions',
        'time',
        'conditions.time column has NOT NULL constraint'
    );

SELECT col_type_is(
        'test',
        'conditions',
        'time',
        'TIMESTAMPTZ',
        'conditions.time column has type TIMESTAMPTZ'
    );

-- location column
SELECT col_not_null(
        'test',
        'conditions',
        'location',
        'conditions.location column has NOT NULL constraint'
    );

SELECT col_type_is(
        'test',
        'conditions',
        'location',
        'TEXT',
        'conditions.location column has type TEXT'
    );

-- device column
SELECT col_not_null(
        'test',
        'conditions',
        'device',
        'conditions.device column has NOT NULL constraint'
    );

SELECT col_type_is(
        'test',
        'conditions',
        'device',
        'TEXT',
        'conditions.device column has type TEXT'
    );

-- temperature column
SELECT col_is_null(
        'test',
        'conditions',
        'temperature',
        'conditions.temperature column is nullable'
    );

SELECT col_type_is(
        'test',
        'conditions',
        'temperature',
        'DOUBLE PRECISION',
        'conditions.temperature column has type DOUBLE PRECISION'
    );

-- humidity column
SELECT col_is_null(
        'test',
        'conditions',
        'humidity',
        'conditions.humidity column is nullable'
    );

SELECT col_type_is(
        'test',
        'conditions',
        'humidity',
        'DOUBLE PRECISION',
        'conditions.humidity column has type DOUBLE PRECISION'
    );

SELECT *
FROM finish();

ROLLBACK;