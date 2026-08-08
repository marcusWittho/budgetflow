--liquibase formatted sql

--changeset marcus.lw:001-create-organizations-table
CREATE TABLE organizations (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  name VARCHAR(255) NOT NULL,
  plan_id VARCHAR(50) DEFAULT 'free' CHECK (plan_id IN ('free', 'premium', 'enterprise')),
  subscription_id UUID,
  seats_available INT DEFAULT 5,
  features_enabled JSONB DEFAULT '[]'::jsonb,
  status VARCHAR(20) DEFAULT 'active' CHECK (status IN ('active', 'suspended', 'canceled')),
  created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_organizations_plan_id ON organizations(plan_id);
CREATE INDEX idx_organizations_subscription_id ON organizations(subscription_id);
--rollback DROP TABLE IF EXISTS organizations;
