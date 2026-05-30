#!/bin/bash
set -e

echo "Waiting for Cassandra..."
until cqlsh "$CASSANDRA_HOST" "$CASSANDRA_PORT" -e "describe cluster" > /dev/null 2>&1; do
  sleep 3
done

echo "Running Cassandra schema init..."

cqlsh "$CASSANDRA_HOST" "$CASSANDRA_PORT" \
  -e "CREATE KEYSPACE IF NOT EXISTS \"$CASSANDRA_KEYSPACE\" WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};"

cqlsh "$CASSANDRA_HOST" "$CASSANDRA_PORT" \
  -e "CREATE TABLE IF NOT EXISTS \"$CASSANDRA_KEYSPACE\".event_reactions (
        event_id   text,
        created_by text,
        like_value tinyint,
        created_at timestamp,
        PRIMARY KEY (event_id, created_by)
      );"

cqlsh "$CASSANDRA_HOST" "$CASSANDRA_PORT" \
  -e "CREATE INDEX IF NOT EXISTS ON \"$CASSANDRA_KEYSPACE\".event_reactions (like_value);"

cqlsh "$CASSANDRA_HOST" "$CASSANDRA_PORT" \
  -e "CREATE INDEX IF NOT EXISTS ON \"$CASSANDRA_KEYSPACE\".event_reactions (created_by);"

echo "Done!"