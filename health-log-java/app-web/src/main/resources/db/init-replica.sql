DO $$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'replica_user') THEN
    CREATE ROLE replica_user WITH REPLICATION LOGIN PASSWORD 'postgres';
  END IF;
END
$$;