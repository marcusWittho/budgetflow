package com.lwv.budgetflow.auth.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PlanTypeConverter implements AttributeConverter<PlanType, String> {

  @Override
  public String convertToDatabaseColumn(PlanType attribute) {
    return attribute == null ? null : attribute.name().toLowerCase();
  }

  @Override
  public PlanType convertToEntityAttribute(String dbData) {
    return dbData == null ? null : PlanType.valueOf(dbData.toUpperCase());
  }
}
