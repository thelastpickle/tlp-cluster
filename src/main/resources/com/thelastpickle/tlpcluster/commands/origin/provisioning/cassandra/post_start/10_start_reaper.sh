cqlsh $(hostname) -e "CREATE KEYSPACE IF NOT EXISTS reaper_db with replication = {'class':'SimpleStrategy', 'replication_factor':3}"
service cassandra-reaper start
