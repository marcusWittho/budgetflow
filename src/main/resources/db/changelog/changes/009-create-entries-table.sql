--liquibase formatted sql
--changeset leonardo:009-create-entries-table
-- O lancamento. Tabela central do sistema.
--
-- amount e NUMERIC, nunca ponto flutuante: a planilha sobrevivia com float
-- porque o Excel arredondava na exibicao, mas aqui a soma acontece no banco.
--
-- Guardamos category_id E subcategory_id de proposito. E redundante, mas
-- subcategoria e opcional e quase toda agregacao agrupa por categoria —
-- assim ela nao precisa de join extra nem de tratar nulo.
CREATE TABLE entries (
  id                 UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id            UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  entry_date         DATE NOT NULL,
  category_id        UUID NOT NULL REFERENCES categories(id),
  subcategory_id     UUID REFERENCES subcategories(id),
  account_id         UUID REFERENCES accounts(id),
  payment_method_id  UUID REFERENCES payment_methods(id),
  description        VARCHAR(300) NOT NULL,
  amount             NUMERIC(19,2) NOT NULL CHECK (amount > 0),
  status             VARCHAR(20) NOT NULL CHECK (status IN ('paid', 'pending')),
  notes              TEXT,
  group_id           UUID REFERENCES entry_groups(id) ON DELETE CASCADE,
  installment_number INT,
  created_at         TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
  updated_at         TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
  -- Ou o lancamento e avulso (sem grupo e sem numero de parcela), ou
  -- pertence a um grupo e tem numero. Nao existe meio caminho.
  CONSTRAINT ck_entries_installment CHECK (
    (group_id IS NULL AND installment_number IS NULL)
    OR (group_id IS NOT NULL AND installment_number >= 1)
  )
);
-- O user_id vem primeiro em todo indice: o banco usa da esquerda para a
-- direita, e voce sempre filtra por usuario antes de qualquer outra coisa.
CREATE INDEX idx_entries_user_date ON entries(user_id, entry_date DESC);
CREATE INDEX idx_entries_category_id ON entries(category_id, entry_date);
CREATE INDEX idx_entries_group_id ON entries(group_id) WHERE group_id IS NOT NULL;
CREATE INDEX idx_entries_user_pending ON entries(user_id, entry_date) WHERE status = 'pending';
--rollback DROP TABLE IF EXISTS entries;
