--liquibase formatted sql
--changeset leonardo:008-create-entry-groups-table
-- Amarra as parcelas de uma compra ou as mensalidades de uma assinatura.
-- Na planilha isso era a coluna "grupo", com codigos P001 e R001.
CREATE TABLE entry_groups (
  id                 UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id            UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  code               VARCHAR(16) NOT NULL,
  kind               VARCHAR(20) NOT NULL CHECK (kind IN ('installment', 'recurring')),
  total_installments INT,
  label              VARCHAR(200),
  active             BOOLEAN DEFAULT TRUE,
  created_at         TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
  updated_at         TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uq_entry_groups_user_code UNIQUE (user_id, code),
  -- Parcelamento tem total conhecido; assinatura nao tem fim previsto.
  -- O CHECK garante que os dois casos nao se misturam.
  CONSTRAINT ck_entry_groups_total CHECK (
    (kind = 'installment' AND total_installments IS NOT NULL AND total_installments >= 1)
    OR (kind = 'recurring' AND total_installments IS NULL)
  )
);
CREATE INDEX idx_entry_groups_user_id ON entry_groups(user_id);
--rollback DROP TABLE IF EXISTS entry_groups;
