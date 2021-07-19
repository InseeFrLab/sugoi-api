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
package fr.insee.sugoi.core.service.impl;

import fr.insee.sugoi.core.model.SugoiUser;
import fr.insee.sugoi.core.service.PermissionService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang.text.StrSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PermissionServiceImpl implements PermissionService {

  @Value("${fr.insee.sugoi.api.regexp.role.reader:}")
  private List<String> regexpReaderList;

  @Value("${fr.insee.sugoi.api.regexp.role.writer:}")
  private List<String> regexpWriterList;

  @Value("${fr.insee.sugoi.api.regexp.role.admin:}")
  private List<String> adminRoleList;

  @Value("${fr.insee.sugoi.api.regexp.role.password.manager:}")
  private List<String> passwordManagerRoleList;

  @Value("${fr.insee.sugoi.api.regexp.role.application.manager:}")
  private List<String> applicationManagerRoleList;

  public static final Logger logger = LoggerFactory.getLogger(PermissionService.class);

  @Override
  public boolean isReader(SugoiUser sugoiUser, String realm, String userStorage) {
    List<String> searchRoleList =
        getSearchRoleList(sugoiUser, realm, userStorage, null, regexpReaderList);
    return checkIfUserGetRoles(sugoiUser, searchRoleList)
        || isWriter(sugoiUser, realm, userStorage)
        || isApplicationManager(sugoiUser, realm);
  }

  @Override
  public boolean isPasswordManager(SugoiUser sugoiUser, String realm, String userStorage) {
    List<String> searchRoleList =
        getSearchRoleList(sugoiUser, realm, userStorage, null, passwordManagerRoleList);
    return checkIfUserGetRoles(sugoiUser, searchRoleList);
  }

  @Override
  public boolean isApplicationManager(
      SugoiUser sugoiUser, String realm, String userStorage, String application) {
    List<String> searchRoleList =
        getSearchRoleList(sugoiUser, realm, userStorage, application, applicationManagerRoleList);
    return checkIfUserGetRoles(sugoiUser, searchRoleList);
  }

  @Override
  public boolean isApplicationManager(SugoiUser sugoiUser, String realm) {
    List<String> searchRoleList =
        getSearchRoleList(sugoiUser, realm, "*", "*", applicationManagerRoleList);
    return checkIfUserGetRoles(sugoiUser, searchRoleList);
  }

  @Override
  public boolean isWriter(SugoiUser sugoiUser, String realm, String userStorage) {
    List<String> searchRoleList =
        getSearchRoleList(sugoiUser, realm, userStorage, null, regexpWriterList);
    return checkIfUserGetRoles(sugoiUser, searchRoleList) || isAdmin(sugoiUser);
  }

  @Override
  public boolean isAdmin(SugoiUser sugoiUser) {
    return checkIfUserGetRoles(sugoiUser, adminRoleList);
  }

  private boolean checkIfUserGetRoles(SugoiUser sugoiUser, List<String> rolesSearch) {
    logger.debug("Checking if user is in : {}", rolesSearch);
    List<String> roles =
        sugoiUser.getRoles().stream().map(String::toUpperCase).collect(Collectors.toList());
    logger.debug("User roles: {}", roles);
    for (String roleSearch : rolesSearch) {
      logger.trace(roleSearch);
      if (roles.contains(roleSearch.toUpperCase())) {
        return true;
      }
      for (String role : roles) {
        if (role.toUpperCase().matches(roleSearch.replaceAll("\\*", ".*").toUpperCase())) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public List<String> getUserRealmReader(SugoiUser sugoiUser) {
    return getUserRightList(sugoiUser, regexpReaderList);
  }

  @Override
  public List<String> getUserRealmWriter(SugoiUser sugoiUser) {
    return getUserRightList(sugoiUser, regexpWriterList);
  }

  @Override
  public List<String> getUserRealmPasswordManager(SugoiUser sugoiUser) {
    return getUserRightList(sugoiUser, passwordManagerRoleList);
  }

  @Override
  public List<String> getUserRealmAppManager(SugoiUser sugoiUser) {
    return getUserRightList(sugoiUser, applicationManagerRoleList);
  }

  private List<String> getUserRightList(SugoiUser sugoiUser, List<String> regexpListToSearch) {
    List<String> searchRoleList =
        getSearchRoleList(
                sugoiUser,
                "(?<realm>.*)",
                "(?<userStorage>.*)",
                "(?<application>.*)",
                regexpListToSearch)
            .stream()
            .map(String::toUpperCase)
            .collect(Collectors.toList());
    List<String> roles =
        sugoiUser.getRoles().stream()
            .map(String::toUpperCase)
            .map(
                role -> {
                  for (String searchRole : searchRoleList) {
                    Pattern p = Pattern.compile(searchRole);
                    Matcher m = p.matcher(role);
                    if (m.matches()) {
                      String realm = "";
                      String userStorage = "";
                      String application = "";
                      try {
                        realm = m.group("REALM");
                      } catch (Exception e) {
                      }
                      try {
                        userStorage = "_" + m.group("USERSTORAGE");
                      } catch (Exception e) {
                      }
                      try {
                        if (m.group("APPLICATION") != null) {
                          if (realm.equals("")) {
                            realm = "*";
                            userStorage = "_*";
                          }
                          application = "\\" + m.group("APPLICATION");
                        }
                      } catch (Exception e) {
                      }
                      String res = realm + userStorage + application;
                      return res;
                    }
                  }
                  return null;
                })
            .filter(role -> role != null)
            .collect(Collectors.toList());
    return roles;
  }

  private List<String> getSearchRoleList(
      SugoiUser sugoiUser,
      String realm,
      String userStorage,
      String application,
      List<String> regexpList) {
    Map<String, String> valueMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    valueMap.put("realm", realm.toUpperCase());
    if (userStorage != null) {
      valueMap.put("userStorage", userStorage.toUpperCase());
    }
    if (application != null) {
      valueMap.put("application", application);
    }
    return regexpList.stream()
        .map(regexp -> StrSubstitutor.replace(regexp, valueMap, "$(", ")"))
        .collect(Collectors.toList());
  }

  @Override
  public List<String> getAllowedAttributePattern(
      SugoiUser sugoiUser, String realm, String storage, String pattern) {
    List<String> appRightsOfUser =
        getUserRealmAppManager(sugoiUser).stream()
            .map(app -> app.split("\\\\")[1])
            .collect(Collectors.toList());
    // Look for regexp of attribute value allowed
    List<String> regexpAttributesAllowed = new ArrayList<>();
    for (String appRight : appRightsOfUser) {
      Map<String, String> valueMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
      valueMap.put("application", appRight);
      valueMap.put("realm", realm);
      valueMap.put("storage", storage);
      regexpAttributesAllowed.add(
          StrSubstitutor.replace(pattern, valueMap, "$(", ")").toUpperCase());
    }
    return regexpAttributesAllowed;
  }

  @Override
  public boolean isValidAttributeAccordingAttributePattern(
      SugoiUser sugoiUser, String realm, String storage, String pattern, String attribute) {
    List<String> regexpAttributesAllowed =
        getAllowedAttributePattern(sugoiUser, realm, storage, pattern);
    // Check if attribute match with allowed pattern
    for (String regexpAttributeAllowed : regexpAttributesAllowed) {
      if (attribute.toUpperCase().matches(regexpAttributeAllowed)) {
        return true;
      }
    }
    return false;
  }
}
