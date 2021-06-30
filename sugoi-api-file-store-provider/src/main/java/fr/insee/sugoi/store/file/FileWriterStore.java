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
package fr.insee.sugoi.store.file;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.insee.sugoi.core.model.ProviderRequest;
import fr.insee.sugoi.core.model.ProviderResponse;
import fr.insee.sugoi.core.model.ProviderResponse.ProviderResponseStatus;
import fr.insee.sugoi.core.store.WriterStore;
import fr.insee.sugoi.model.Application;
import fr.insee.sugoi.model.Group;
import fr.insee.sugoi.model.Organization;
import fr.insee.sugoi.model.User;
import fr.insee.sugoi.model.paging.PasswordChangeRequest;
import fr.insee.sugoi.model.paging.SendMode;
import fr.insee.sugoi.store.file.configuration.FileKeysConfig;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;

public class FileWriterStore implements WriterStore {

  @Autowired ResourceLoader resourceLoader;

  private Map<String, String> config;
  private ObjectMapper mapper = new ObjectMapper();
  FileReaderStore fileReaderStore;

  public FileWriterStore(Map<String, String> generateConfig) {
    this.config = generateConfig;
    this.fileReaderStore = new FileReaderStore(generateConfig);
  }

  @Override
  public ProviderResponse deleteUser(String id, ProviderRequest providerRequest) {
    fileReaderStore.setResourceLoader(resourceLoader);
    User user = fileReaderStore.getUser(id);
    user.getGroups()
        .forEach(
            group -> deleteUserFromGroup(group.getAppName(), group.getName(), id, providerRequest));
    deleteResourceFile(config.get(FileKeysConfig.USER_SOURCE), id);
    ProviderResponse response = new ProviderResponse();
    response.setEntityId(id);
    response.setStatus(ProviderResponseStatus.OK);
    return response;
  }

  @Override
  public ProviderResponse createUser(User user, ProviderRequest providerRequest) {
    try {
      createResourceFile(
          config.get(FileKeysConfig.USER_SOURCE),
          user.getUsername(),
          mapper.writeValueAsString(user));
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Error mapping user" + user.getUsername(), e);
    }
    ProviderResponse response = new ProviderResponse();
    response.setEntityId(user.getUsername());
    response.setStatus(ProviderResponseStatus.OK);
    return response;
  }

  @Override
  public ProviderResponse updateUser(User updatedUser, ProviderRequest providerRequest) {
    try {
      updateResourceFile(
          config.get(FileKeysConfig.USER_SOURCE),
          updatedUser.getUsername(),
          mapper.writeValueAsString(updatedUser));
      ProviderResponse response = new ProviderResponse();
      response.setEntityId(updatedUser.getUsername());
      response.setStatus(ProviderResponseStatus.OK);
      return response;
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Error mapping user" + updatedUser.getUsername(), e);
    }
  }

  @Override
  public ProviderResponse deleteGroup(
      String appName, String groupName, ProviderRequest providerRequest) {
    fileReaderStore.setResourceLoader(resourceLoader);
    Application application = fileReaderStore.getApplication(appName);
    if (application != null) {
      application.getGroups().removeIf(group -> group.getName().equalsIgnoreCase(groupName));
      updateApplication(application, providerRequest);
      ProviderResponse response = new ProviderResponse();
      response.setEntityId(appName);
      response.setStatus(ProviderResponseStatus.OK);
      return response;
    } else {
      throw new RuntimeException("Application " + appName + " doesn't exist");
    }
  }

  @Override
  public ProviderResponse createGroup(
      String appName, Group appGroup, ProviderRequest providerRequest) {
    fileReaderStore.setResourceLoader(resourceLoader);
    Application application = fileReaderStore.getApplication(appName);
    if (application != null) {
      if (application.getGroups() == null) {
        application.setGroups(new ArrayList<>());
      }
      if (!application.getGroups().stream()
          .anyMatch(group -> group.getName().equalsIgnoreCase(appGroup.getName()))) {
        appGroup.setAppName(appName);
        application.getGroups().add(appGroup);
        updateApplication(application, providerRequest);
        ProviderResponse response = new ProviderResponse();
        response.setEntityId(appGroup.getName());
        response.setStatus(ProviderResponseStatus.OK);
        return response;
      } else {
        throw new RuntimeException("Group " + appGroup.getName() + " already exists in " + appName);
      }
    } else {
      throw new RuntimeException("Application " + appName + " doesn't exist");
    }
  }

  @Override
  public ProviderResponse updateGroup(
      String appName, Group updatedGroup, ProviderRequest providerRequest) {
    fileReaderStore.setResourceLoader(resourceLoader);
    Application application = fileReaderStore.getApplication(appName);
    if (application != null) {
      if (application.getGroups() != null
          && application.getGroups().stream()
              .anyMatch(
                  filterGroup -> filterGroup.getName().equalsIgnoreCase(updatedGroup.getName()))) {

        application
            .getGroups()
            .removeIf(
                filterGroup -> filterGroup.getName().equalsIgnoreCase(updatedGroup.getName()));
        application.getGroups().add(updatedGroup);
        updateApplication(application, providerRequest);
        ProviderResponse response = new ProviderResponse();
        response.setEntityId(updatedGroup.getName());
        response.setStatus(ProviderResponseStatus.OK);
        return response;
      } else {
        throw new RuntimeException(
            "Group " + updatedGroup.getName() + " doesn't exist in " + appName);
      }
    } else {
      throw new RuntimeException("Application " + appName + " doesn't exist");
    }
  }

  @Override
  public ProviderResponse deleteOrganization(String name, ProviderRequest providerRequest) {
    if (config.get(FileKeysConfig.ORGANIZATION_SOURCE) != null) {
      deleteResourceFile(config.get(FileKeysConfig.ORGANIZATION_SOURCE), name);
      ProviderResponse response = new ProviderResponse();
      response.setEntityId(name);
      response.setStatus(ProviderResponseStatus.OK);
      return response;
    } else {
      throw new UnsupportedOperationException(
          "organizations feature not configured for this storage");
    }
  }

  @Override
  public ProviderResponse createOrganization(
      Organization organization, ProviderRequest providerRequest) {
    if (config.get(FileKeysConfig.ORGANIZATION_SOURCE) != null) {
      try {
        createResourceFile(
            config.get(FileKeysConfig.ORGANIZATION_SOURCE),
            organization.getIdentifiant(),
            mapper.writeValueAsString(organization));
      } catch (JsonProcessingException e) {
        throw new RuntimeException(
            "Error mapping organization " + organization.getIdentifiant(), e);
      }
      ProviderResponse response = new ProviderResponse();
      response.setEntityId(organization.getIdentifiant());
      response.setStatus(ProviderResponseStatus.OK);
      return response;
    } else {
      throw new UnsupportedOperationException(
          "organizations feature not configured for this storage");
    }
  }

  @Override
  public ProviderResponse updateOrganization(
      Organization updatedOrganization, ProviderRequest providerRequest) {
    if (config.get(FileKeysConfig.ORGANIZATION_SOURCE) != null) {
      try {
        updateResourceFile(
            config.get(FileKeysConfig.ORGANIZATION_SOURCE),
            updatedOrganization.getIdentifiant(),
            mapper.writeValueAsString(updatedOrganization));
        ProviderResponse response = new ProviderResponse();
        response.setEntityId(updatedOrganization.getIdentifiant());
        response.setStatus(ProviderResponseStatus.OK);
        return response;
      } catch (JsonProcessingException e) {
        throw new RuntimeException(
            "Error mapping organization" + updatedOrganization.getIdentifiant(), e);
      }
    } else {
      throw new UnsupportedOperationException(
          "organizations feature not configured for this storage");
    }
  }

  /** Group membership information is removed from the user object and from the group object */
  @Override
  public ProviderResponse deleteUserFromGroup(
      String appName, String groupName, String userId, ProviderRequest providerRequest) {
    fileReaderStore.setResourceLoader(resourceLoader);
    Application application = fileReaderStore.getApplication(appName);
    if (application != null) {
      Group group =
          application.getGroups() != null
              ? application.getGroups().stream()
                  .filter(filterGroup -> filterGroup.getName().equalsIgnoreCase(groupName))
                  .findFirst()
                  .orElse(null)
              : null;
      if (group != null) {
        User user = fileReaderStore.getUser(userId);
        user.getGroups()
            .removeIf(
                groupFilter ->
                    groupFilter.getAppName().equalsIgnoreCase(appName)
                        && groupFilter.getName().equalsIgnoreCase(groupName));
        updateUser(user, providerRequest);
        if (group.getUsers() != null) {
          try {
            group
                .getUsers()
                .removeIf(userFilter -> userFilter.getUsername().equalsIgnoreCase(userId));
            updateResourceFile(
                config.get(FileKeysConfig.APP_SOURCE),
                application.getName(),
                mapper.writeValueAsString(application));
          } catch (JsonProcessingException e) {
            throw new RuntimeException("Error mapping application " + application.getName(), e);
          }
        }
        ProviderResponse response = new ProviderResponse();
        response.setEntityId(userId);
        response.setStatus(ProviderResponseStatus.OK);
        return response;
      } else {
        throw new RuntimeException("Group " + groupName + " doesn't exist in " + appName);
      }
    } else {
      throw new RuntimeException("Application " + appName + " doesn't exist");
    }
  }

  /**
   * Group membership information is added on the user object and on the group object, user's groups
   * are simplified groups to avoid information loop
   */
  @Override
  public ProviderResponse addUserToGroup(
      String appName, String groupName, String userId, ProviderRequest providerRequest) {
    fileReaderStore.setResourceLoader(resourceLoader);
    Application application = fileReaderStore.getApplication(appName);
    if (application != null) {
      Group group =
          application.getGroups() != null
              ? application.getGroups().stream()
                  .filter(filterGroup -> filterGroup.getName().equalsIgnoreCase(groupName))
                  .findFirst()
                  .orElse(null)
              : null;
      if (group != null) {
        User user = fileReaderStore.getUser(userId);
        if (user != null) {
          try {
            if (user.getGroups() == null) {
              user.setGroups(new ArrayList<>());
            }
            user.getGroups().add(new Group(appName, groupName));
            updateUser(user, new ProviderRequest(null, false, null));
            if (group.getUsers() == null) {
              group.setUsers(new ArrayList<>());
            }
            group.getUsers().add(user);
            updateResourceFile(
                config.get(FileKeysConfig.APP_SOURCE),
                application.getName(),
                mapper.writeValueAsString(application));
            ProviderResponse response = new ProviderResponse();
            response.setEntityId(userId);
            response.setStatus(ProviderResponseStatus.OK);
            return response;
          } catch (JsonProcessingException e) {
            throw new RuntimeException("Error mapping application " + application.getName(), e);
          }
        } else {
          throw new RuntimeException("User " + userId + " not found");
        }
      } else {
        throw new RuntimeException("Group " + groupName + " doesn't exist in " + appName);
      }
    } else {
      throw new RuntimeException("Application " + appName + " doesn't exist");
    }
  }

  @Override
  public ProviderResponse reinitPassword(
      String userId,
      String generatedPassword,
      PasswordChangeRequest pcr,
      List<SendMode> sendMode,
      ProviderRequest providerRequest) {
    throw new UnsupportedOperationException("Password actions are not supported on file storage");
  }

  @Override
  public ProviderResponse initPassword(
      String userId,
      String password,
      PasswordChangeRequest pcr,
      List<SendMode> sendMode,
      ProviderRequest providerRequest) {
    throw new UnsupportedOperationException("Password actions are not supported on file storage");
  }

  @Override
  public ProviderResponse createApplication(
      Application application, ProviderRequest providerRequest) {
    if (config.get(FileKeysConfig.APP_SOURCE) != null) {
      try {
        application.getGroups().forEach(group -> group.setUsers(null));
        createResourceFile(
            config.get(FileKeysConfig.APP_SOURCE),
            application.getName(),
            mapper.writeValueAsString(application));
        ProviderResponse response = new ProviderResponse();
        response.setEntityId(application.getName());
        response.setStatus(ProviderResponseStatus.OK);
        return response;
      } catch (JsonProcessingException e) {
        throw new RuntimeException("Error mapping application " + application.getName(), e);
      }
    } else {
      throw new UnsupportedOperationException("Applications feature not configured for this realm");
    }
  }

  @Override
  public ProviderResponse updateApplication(
      Application updatedApplication, ProviderRequest providerRequest) {
    if (config.get(FileKeysConfig.APP_SOURCE) != null) {
      try {
        fileReaderStore.setResourceLoader(resourceLoader);
        for (Group updatedGroup : updatedApplication.getGroups()) {
          Group existingGroup =
              fileReaderStore.getGroup(updatedApplication.getName(), updatedGroup.getName());
          if (existingGroup != null) {
            updatedGroup.setUsers(existingGroup.getUsers());
          }
        }
        updateResourceFile(
            config.get(FileKeysConfig.APP_SOURCE),
            updatedApplication.getName(),
            mapper.writeValueAsString(updatedApplication));
        ProviderResponse response = new ProviderResponse();
        response.setEntityId(updatedApplication.getName());
        response.setStatus(ProviderResponseStatus.OK);
        return response;
      } catch (JsonProcessingException e) {
        throw new RuntimeException("Error mapping application" + updatedApplication.getName(), e);
      }
    } else {
      throw new UnsupportedOperationException("Applications feature not configured for this realm");
    }
  }

  @Override
  public ProviderResponse deleteApplication(
      String applicationName, ProviderRequest providerRequest) {
    if (config.get(FileKeysConfig.APP_SOURCE) != null) {
      deleteResourceFile(config.get(FileKeysConfig.APP_SOURCE), applicationName);
      ProviderResponse response = new ProviderResponse();
      response.setEntityId(applicationName);
      response.setStatus(ProviderResponseStatus.OK);
      return response;
    } else {
      throw new UnsupportedOperationException("Applications feature not configured for this realm");
    }
  }

  @Override
  public ProviderResponse changePassword(
      String userId,
      String oldPassword,
      String newPassword,
      PasswordChangeRequest pcr,
      ProviderRequest providerRequest) {
    throw new NotImplementedException();
  }

  @Override
  public ProviderResponse addAppManagedAttribute(
      String userId, String attributeKey, String attribute, ProviderRequest providerRequest) {
    throw new NotImplementedException();
  }

  @Override
  public ProviderResponse deleteAppManagedAttribute(
      String userId, String attributeKey, String attribute, ProviderRequest providerRequest) {
    throw new NotImplementedException();
  }

  private void createResourceFile(String source, String resourceName, String resourceValue) {
    try {
      File file = resourceLoader.getResource(source + resourceName).getFile();
      file.createNewFile();
      updateResourceFile(source, resourceName, resourceValue);
    } catch (IOException e) {
      throw new RuntimeException("Error creating new resource file", e);
    }
  }

  private void deleteResourceFile(String source, String resourceName) {
    try {
      File file = resourceLoader.getResource(source + resourceName).getFile();
      file.delete();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void updateResourceFile(String source, String resourceName, String resourceValue) {
    try {
      File file = resourceLoader.getResource(source + resourceName).getFile();
      FileWriter fWriter = new FileWriter(file);
      fWriter.write(resourceValue);
      fWriter.close();
    } catch (IOException e) {
      throw new RuntimeException("Error writing in file", e);
    }
  }
}
