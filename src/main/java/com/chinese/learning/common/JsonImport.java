package com.chinese.learning.common;

import jakarta.validation.Validator;
import java.util.*;
import org.springframework.stereotype.Component;
import tools.jackson.databind.*;

@Component
public class JsonImport {
  private final ObjectMapper mapper;
  private final Validator validator;

  public JsonImport(ObjectMapper mapper, Validator validator) {
    this.mapper = mapper;
    this.validator = validator;
  }

  public <T> List<T> parse(JsonNode root, String property, Class<T> type) {
    if (root == null
        || (!root.isArray() && (!root.isObject() || root.size() != 1 || !root.has(property))))
      throw ApiException.bad("Attendu : tableau ou objet contenant uniquement " + property);
    JsonNode array = root.isArray() ? root : root.get(property);
    if (array == null || !array.isArray() || array.size() > 5000)
      throw ApiException.bad("Tableau " + property + " requis (maximum 5000 entrées)");
    List<T> result = new ArrayList<>();
    List<String> errors = new ArrayList<>();
    int i = 0;
    for (JsonNode node : array) {
      try {
        T value = mapper.treeToValue(node, type);
        var violations = validator.validate(value);
        if (!violations.isEmpty())
          errors.add(
              "entrée "
                  + i
                  + ": "
                  + violations.stream()
                      .map(v -> v.getPropertyPath() + " " + v.getMessage())
                      .sorted()
                      .toList());
        else result.add(value);
      } catch (Exception e) {
        errors.add("entrée " + i + ": structure invalide");
      }
      i++;
    }
    if (!errors.isEmpty()) throw ApiException.bad(String.join("; ", errors));
    return result;
  }
}
