--liquibase formatted sql
--changeset leonardo:006-create-accounts-table
-- De onde o dinheiro saiu ou entrou: Conta Corrente, Carteira, Poupanca.
CREATE TABLE accounts (
  id         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  name       VARCHAR(120) NOT NULL,
  archived   BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uq_accounts_user_name UNIQUE (user_id, name)
);
CREATE INDEX idx_accounts_user_id ON accounts(user_id);
--rollback DROP TABLE IF EXISTS accounts;
