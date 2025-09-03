package ru.hh.aiinterviewer.model.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import ru.hh.aiinterviewer.model.MessageRole;

@Converter(autoApply = true)
public class MessageRoleConverter implements AttributeConverter<MessageRole, String> {

  @Override
  public String convertToDatabaseColumn(MessageRole attribute) {
    if (attribute == null) {
      return null;
    }
    return attribute.toDatabaseValue();
  }

  @Override
  public MessageRole convertToEntityAttribute(String dbData) {
    if (dbData == null) {
      return null;
    }
    return MessageRole.fromDatabaseValue(dbData);
  }
}


