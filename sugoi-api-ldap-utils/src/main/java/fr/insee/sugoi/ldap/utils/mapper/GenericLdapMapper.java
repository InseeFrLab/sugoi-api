/*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package fr.insee.sugoi.ldap.utils.mapper;

import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.Modification;
import fr.insee.sugoi.core.exceptions.LdapMappingConfigurationException;
import fr.insee.sugoi.ldap.utils.LdapUtils;
import fr.insee.sugoi.ldap.utils.config.LdapConfigKeys;
import fr.insee.sugoi.model.Group;
import fr.insee.sugoi.model.Habilitation;
import fr.insee.sugoi.model.Organization;
import fr.insee.sugoi.model.User;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class GenericLdapMapper {

  @SuppressWarnings("unchecked")
  public static <ReturnType> ReturnType mapLdapAttributesToObject(
      Collection<Attribute> attributes,
      Class<ReturnType> returnClazz,
      Map<String, String> config,
      Map<String, String> mapping) {
    try {
      ReturnType mappedEntity = returnClazz.getDeclaredConstructor().newInstance();
      for (Entry<String, String> mappingDefinition : mapping.entrySet()) {
        try {
          String[] splitedMappingDefinition = mappingDefinition.getValue().split(",");
          String attributeLdapName = splitedMappingDefinition[0];
          String mappingType = splitedMappingDefinition[1];
          String fieldToSetName = mappingDefinition.getKey();
          List<String> correspondingAttributes = new ArrayList<>();
          attributes.stream()
              .filter(attribute -> attributeLdapName.equalsIgnoreCase(attribute.getName()))
              .forEach(
                  attribute ->
                      correspondingAttributes.addAll(Arrays.asList(attribute.getValues())));
          if (correspondingAttributes.size() > 0) {
            if (fieldToSetName.contains(".")) {
              String[] splitedFieldName = fieldToSetName.split("\\.");
              String mapToModifyName = splitedFieldName[0];
              String keyToModify = splitedFieldName[1];
              Field modelField = mappedEntity.getClass().getDeclaredField(mapToModifyName);
              modelField.setAccessible(true);
              Map<String, Object> map = (Map<String, Object>) modelField.get(mappedEntity);
              map.put(
                  keyToModify,
                  transformAttributeToSugoi(mappingType, correspondingAttributes, config));
            } else {
              Field modelField =
                  mappedEntity.getClass().getDeclaredField(mappingDefinition.getKey());
              modelField.setAccessible(true);
              modelField.set(
                  mappedEntity,
                  transformAttributeToSugoi(mappingType, correspondingAttributes, config));
            }
          }
        } catch (Exception e) {
          throw new LdapMappingConfigurationException(
              "Error occured while mapping attribute to Ldap. Must be caused by the configuration "
                  + mappingDefinition.getKey()
                  + ":"
                  + mappingDefinition.getValue()
                  + " for entity "
                  + returnClazz.getName(),
              e);
        }
      }
      return mappedEntity;
    } catch (InstantiationException
        | IllegalAccessException
        | IllegalArgumentException
        | InvocationTargetException
        | NoSuchMethodException
        | SecurityException e) {
      throw new RuntimeException("Exception while getting the entity " + returnClazz.getName(), e);
    }
  }

  @SuppressWarnings("unchecked")
  public static <SugoiType> List<Attribute> mapObjectToLdapAttributes(
      SugoiType entity,
      Class<SugoiType> entityClazz,
      Map<String, String> config,
      Map<String, String> mapping,
      List<String> objectClasses) {
    List<Attribute> attributes = new ArrayList<>();
    if (objectClasses != null && !objectClasses.isEmpty()) {
      attributes.add(new Attribute("objectClass", objectClasses));
    }
    // Exception where else is needed ? not needed if modification step

    for (Entry<String, String> mappingDefinition : mapping.entrySet()) {
      try {
        String[] splitedMappingDefinition = mappingDefinition.getValue().split(",");
        String attributeLdapName = splitedMappingDefinition[0];
        String mappingType = splitedMappingDefinition[1];
        String readonlyStatus = splitedMappingDefinition[2];
        String fieldToSetName = mappingDefinition.getKey();
        if (!readonlyStatus.equalsIgnoreCase("ro")) {
          if (fieldToSetName.contains(".")) {
            String[] splitedFieldName = fieldToSetName.split("\\.");
            String mapToModifyName = splitedFieldName[0];
            String keyToModify = splitedFieldName[1];
            Field modelField = entity.getClass().getDeclaredField(mapToModifyName);
            modelField.setAccessible(true);
            Map<String, Object> map = (Map<String, Object>) modelField.get(entity);
            if (map != null && map.containsKey(keyToModify)) {
              Object sugoiValue = map.get(keyToModify);
              if (sugoiValue != null) {
                attributes.addAll(
                    transformSugoiToAttribute(mappingType, attributeLdapName, sugoiValue, config));
              }
            }
          } else {
            Field sugoiField = entity.getClass().getDeclaredField(fieldToSetName);
            sugoiField.setAccessible(true);
            Object sugoiValue = sugoiField.get(entity);
            if (sugoiValue != null) {
              attributes.addAll(
                  transformSugoiToAttribute(mappingType, attributeLdapName, sugoiValue, config));
            }
          }
        }
      } catch (Exception e) {
        throw new LdapMappingConfigurationException(
            "Error occured while mapping attribute to Ldap. Must be caused by the configuration "
                + mappingDefinition.getKey()
                + ":"
                + mappingDefinition.getValue()
                + " for entity "
                + entityClazz.getName(),
            e);
      }
    }
    return attributes;
  }

  private static Object transformAttributeToSugoi(
      String type, List<String> attr, Map<String, String> config) {
    switch (type.toUpperCase()) {
      case "STRING":
        return attr.get(0);
      case "ORGANIZATION":
        Organization orga = new Organization();
        orga.setIdentifiant(LdapUtils.getNodeValueFromDN(attr.get(0)));
        return orga;
      case "ADDRESS":
        Map<String, String> address = new HashMap<>();
        address.put("id", LdapUtils.getNodeValueFromDN(attr.get(0)));
        return address;
      case "LIST_HABILITATION":
        return attr.stream()
            .filter(
                attributeValue ->
                    attributeValue.split("_").length == 2 || attributeValue.split("_").length == 3)
            .map(attributeValue -> new Habilitation(attributeValue))
            .collect(Collectors.toList());
      case "LIST_USER":
        return attr.stream()
            .map(attributeValue -> new User(LdapUtils.getNodeValueFromDN(attributeValue)))
            .collect(Collectors.toList());
      case "LIST_GROUP":
        return attr.stream()
            .map(
                attributeValue -> {
                  Pattern pattern =
                      Pattern.compile(
                          config
                              .get(LdapConfigKeys.GROUP_SOURCE_PATTERN)
                              .replace("{appliname}", "(.*)"));
                  Matcher matcher =
                      pattern.matcher(attributeValue.substring(attributeValue.indexOf(",") + 1));
                  if (matcher.matches()) {
                    return new Group(
                        matcher.group(1), LdapUtils.getNodeValueFromDN(attributeValue));
                  } else {
                    return null;
                  }
                })
            .collect(Collectors.toList());
      case "LIST_STRING":
        return attr.stream().collect(Collectors.toList());
      default:
        return null;
    }
  }

  @SuppressWarnings("unchecked")
  private static List<Attribute> transformSugoiToAttribute(
      String type, String ldapAttributeName, Object sugoiValue, Map<String, String> config) {
    switch (type.toUpperCase()) {
      case "STRING":
        if ((String) sugoiValue != "") {
          return List.of(new Attribute(ldapAttributeName, (String) sugoiValue));
        } else {
          return List.of();
        }
      case "ORGANIZATION":
        return List.of(
            new Attribute(
                ldapAttributeName,
                String.format(
                    "%s=%s,%s",
                    // TODO should be a param
                    "uid",
                    //
                    ((Organization) sugoiValue).getIdentifiant(),
                    config.get(LdapConfigKeys.ORGANIZATION_SOURCE))));
      case "ADDRESS":
        if (((Map<String, String>) sugoiValue).containsKey("id")
            && config.get(LdapConfigKeys.ADDRESS_SOURCE) != null) {
          return List.of(
              new Attribute(
                  ldapAttributeName,
                  String.format(
                      "%s=%s,%s",
                      // TODO should be a param
                      "l",
                      //
                      ((Map<String, String>) sugoiValue).get("id"),
                      config.get(LdapConfigKeys.ADDRESS_SOURCE))));
        } else return List.of();
      case "LIST_HABILITATION":
        return ((List<Habilitation>) sugoiValue)
            .stream()
                .filter(
                    habilitation ->
                        habilitation.getApplication() != null && habilitation.getRole() != null)
                .map(habilitation -> new Attribute(ldapAttributeName, habilitation.getId()))
                .collect(Collectors.toList());
      case "LIST_USER":
        return List.of();
      case "LIST_GROUP":
        return ((List<Group>) sugoiValue)
            .stream()
                .map(
                    group ->
                        new Attribute(
                            ldapAttributeName,
                            String.format(
                                // TODO should be a param
                                "cn",
                                //
                                group.getName(),
                                config.get(LdapConfigKeys.APP_SOURCE))))
                .collect(Collectors.toList());
      case "LIST_STRING":
        return ((List<String>) sugoiValue)
            .stream()
                .map(value -> new Attribute(ldapAttributeName, value))
                .collect(Collectors.toList());
      default:
        List.of();
    }
    return List.of();
  }

  public static <O> List<Modification> createMods(
      O entity, Class<O> propertiesClazz, Map<String, String> config, Map<String, String> mapping) {
    return LdapUtils.convertAttributesToModifications(
        // Modification => no need to specify object classes
        mapObjectToLdapAttributes(entity, propertiesClazz, config, mapping, null));
  }
}
