--liquibase formatted sql
--changeset leonardo:007-create-payment-methods-table
-- Como foi pago: Pix, Debito, Credito, Boleto.
-- Dimensao separada da conta: um Pix sai da Conta Corrente, uma compra no
-- Credito sai do Cartao. Guardar as duas permite responder "quanto saiu do
-- cartao" e "quanto foi via Pix", que sao perguntas diferentes.
CREATE TABLE payment_methods (
  id         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  name       VARCHAR(120) NOT NULL,
  archived   BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uq_payment_methods_user_name UNIQUE (user_id, name)
);
CREATE INDEX idx_payment_methods_user_id ON payment_methods(user_id);
--rollback DROP TABLE IF EXISTS payment_methods;
