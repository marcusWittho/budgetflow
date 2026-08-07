package com.lwv.budgetflow.domain.converter;

import com.lwv.budgetflow.domain.enums.OrganizationStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class OrganizationStatusConverter implements AttributeConverter<OrganizationStatus, String> {

  @Override
  public String convertToDatabaseColumn(OrganizationStatus attribute) {
    return attribute == null ? null : attribute.name().toLowerCase();
  }

  @Override
  public OrganizationStatus convertToEntityAttribute(String dbData) {
    return dbData == null ? null : OrganizationStatus.valueOf(dbData.toUpperCase());
  }
}
