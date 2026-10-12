/*
--liquibase formatted sql

--changeset file-converter:003-debezium-connect-grant splitStatements:false
DO $$
BEGIN
    EXECUTE format(
        'GRANT CONNECT ON DATABASE %I TO debezium',
        current_database()
    );
END
$$;

--changeset file-converter:004-debezium-outbox-grants
GRANT USAGE ON SCHEMA converter TO debezium;
GRANT SELECT ON TABLE converter.outbox TO debezium;

--changeset file-converter:005-debezium-publication splitStatements:false
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_publication
        WHERE pubname = 'file_converter_publication'
    ) THEN
        EXECUTE
            'CREATE PUBLICATION file_converter_publication
             FOR TABLE converter.outbox';

    ELSIF NOT EXISTS (
        SELECT 1
        FROM pg_publication_tables
        WHERE pubname = 'file_converter_publication'
          AND schemaname = 'converter'
          AND tablename = 'outbox'
    ) THEN
        EXECUTE
            'ALTER PUBLICATION file_converter_publication
             ADD TABLE converter.outbox';
    END IF;
END
$$;*/
