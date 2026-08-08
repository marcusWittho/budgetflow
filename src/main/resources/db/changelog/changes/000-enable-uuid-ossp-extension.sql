--liquibase formatted sql

--changeset marcus.lw:000-enable-uuid-ossp-extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
--rollback DROP EXTENSION IF EXISTS "uuid-ossp";
