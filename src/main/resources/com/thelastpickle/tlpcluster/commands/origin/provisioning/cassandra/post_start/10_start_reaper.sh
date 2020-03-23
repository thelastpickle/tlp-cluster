cqlsh $(hostname) -e "CREATE KEYSPACE IF NOT EXISTS reaper_db with replication = {'class':'SimpleStrategy', 'replication_factor':3}"
cqlsh $(hostname) -e "CREATE TABLE IF NOT EXISTS reaper_db.schema_migration(applied_successful boolean, version int, script_name varchar, script text, executed_at timestamp, PRIMARY KEY (applied_successful, version))"
cqlsh $(hostname) -e "CREATE TABLE IF NOT EXISTS reaper_db.schema_migration_leader(keyspace_name text, leader uuid, took_lead_at timestamp, leader_hostname text, PRIMARY KEY (keyspace_name))"
service cassandra-reaper start
