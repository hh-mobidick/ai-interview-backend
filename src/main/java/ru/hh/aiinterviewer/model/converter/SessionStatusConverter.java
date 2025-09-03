package ru.hh.aiinterviewer.model.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import ru.hh.aiinterviewer.model.SessionStatus;

@Converter(autoApply = true)
public class SessionStatusConverter implements AttributeConverter<SessionStatus, String> {

  @Override
  public String convertToDatabaseColumn(SessionStatus attribute) {
    if (attribute == null) {
      return null;
    }
    return attribute.toDatabaseValue();
  }

  @Override
  public SessionStatus convertToEntityAttribute(String dbData) {
    if (dbData == null) {
      return null;
    }
    return SessionStatus.fromDatabaseValue(dbData);
  }
}


