--liquibase formatted sql
--changeset leonardo:004-create-categories-table
-- Categoria de lancamento. Cada usuario tem a propria copia: ele renomeia
-- e arquiva a dele sem afetar ninguem.
CREATE TABLE categories (
  id         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  type       VARCHAR(20) NOT NULL CHECK (type IN ('income', 'expense', 'investment')),
  name       VARCHAR(120) NOT NULL,
  position   INT DEFAULT 0,
  archived   BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
  -- O user_id entra na unicidade: dois usuarios podem ter "Moradia".
  CONSTRAINT uq_categories_user_type_name UNIQUE (user_id, type, name)
);
CREATE INDEX idx_categories_user_id ON categories(user_id, type);
--rollback DROP TABLE IF EXISTS categories;
