package com.lwv.budgetflow.auth.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class UserStatusConverter implements AttributeConverter<UserStatus, String> {

  @Override
  public String convertToDatabaseColumn(UserStatus attribute) {
    return attribute == null ? null : attribute.name().toLowerCase();
  }

  @Override
  public UserStatus convertToEntityAttribute(String dbData) {
    return dbData == null ? null : UserStatus.valueOf(dbData.toUpperCase());
  }
}
