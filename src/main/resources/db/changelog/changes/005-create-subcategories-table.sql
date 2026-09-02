--liquibase formatted sql
--changeset leonardo:005-create-subcategories-table
-- Nao tem user_id proprio: herda o dono atraves da categoria pai.
CREATE TABLE subcategories (
  id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  category_id UUID NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
  name        VARCHAR(120) NOT NULL,
  -- Fixa ou variavel. Vinha da coluna "natureza" da planilha, que o app
  -- em Python nunca leu. Fica na subcategoria porque "Moradia" tem as
  -- duas: aluguel e fixo, manutencao e variavel. Nula em receita e
  -- investimento, onde a distincao nao faz sentido.
  nature      VARCHAR(20) CHECK (nature IN ('fixed', 'variable')),
  position    INT DEFAULT 0,
  archived    BOOLEAN DEFAULT FALSE,
  created_at  TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
  updated_at  TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uq_subcategories_category_name UNIQUE (category_id, name)
);
CREATE INDEX idx_subcategories_category_id ON subcategories(category_id);
--rollback DROP TABLE IF EXISTS subcategories;
