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

import fr.insee.sugoi.core.configuration.GlobalKeysConfig;
import fr.insee.sugoi.core.event.configuration.EventKeysConfig;
import fr.insee.sugoi.core.event.model.SugoiEventTypeEnum;
import fr.insee.sugoi.core.event.publisher.SugoiEventPublisher;
import fr.insee.sugoi.core.exceptions.GroupNotFoundException;
import fr.insee.sugoi.core.exceptions.UserNotFoundException;
import fr.insee.sugoi.core.model.ProviderRequest;
import fr.insee.sugoi.core.model.ProviderResponse;
import fr.insee.sugoi.core.model.ProviderResponse.ProviderResponseStatus;
import fr.insee.sugoi.core.service.GroupService;
import fr.insee.sugoi.core.service.UserService;
import fr.insee.sugoi.core.store.StoreProvider;
import fr.insee.sugoi.model.Group;
import fr.insee.sugoi.model.User;
import fr.insee.sugoi.model.paging.PageResult;
import fr.insee.sugoi.model.paging.PageableResult;
import fr.insee.sugoi.model.paging.SearchType;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GroupServiceImpl implements GroupService {

  @Autowired private StoreProvider storeProvider;

  @Autowired private SugoiEventPublisher sugoiEventPublisher;

  @Autowired private UserService userService;

  @Override
  public ProviderResponse create(
      String realm, String appName, Group group, ProviderRequest providerRequest) {
    try {
      ProviderResponse response =
          storeProvider.getWriterStore(realm).createGroup(appName, group, providerRequest);
      sugoiEventPublisher.publishCustomEvent(
          realm,
          null,
          SugoiEventTypeEnum.CREATE_GROUP,
          Map.ofEntries(
              Map.entry(EventKeysConfig.GROUP, group),
              Map.entry(EventKeysConfig.APPLICATION_NAME, appName)));
      if (!providerRequest.isAsynchronousAllowed()
          && response.getStatus().equals(ProviderResponseStatus.OK)) {
        response.setEntity(findById(realm, appName, group.getName()).get());
      }
      return response;
    } catch (Exception e) {
      sugoiEventPublisher.publishCustomEvent(
          realm,
          null,
          SugoiEventTypeEnum.CREATE_GROUP_ERROR,
          Map.ofEntries(
              Map.entry(EventKeysConfig.GROUP, group),
              Map.entry(EventKeysConfig.APPLICATION_NAME, appName),
              Map.entry(EventKeysConfig.ERROR, e.toString())));
      throw e;
    }
  }

  @Override
  public ProviderResponse delete(
      String realm, String appName, String id, ProviderRequest providerRequest) {
    try {
      ProviderResponse response =
          storeProvider.getWriterStore(realm).deleteGroup(appName, id, providerRequest);
      sugoiEventPublisher.publishCustomEvent(
          realm,
          null,
          SugoiEventTypeEnum.DELETE_GROUP,
          Map.ofEntries(Map.entry(EventKeysConfig.GROUP_ID, id)));
      return response;
    } catch (Exception e) {
      sugoiEventPublisher.publishCustomEvent(
          realm,
          null,
          SugoiEventTypeEnum.DELETE_GROUP_ERROR,
          Map.ofEntries(
              Map.entry(EventKeysConfig.GROUP_ID, id),
              Map.entry(EventKeysConfig.ERROR, e.toString())));
      if (e instanceof GroupNotFoundException) {
        throw (GroupNotFoundException) e;
      } else {
        throw e;
      }
    }
  }

  @Override
  public Optional<Group> findById(String realm, String appName, String id) {
    try {

      Group group = storeProvider.getReaderStore(realm).getGroup(appName, id);
      sugoiEventPublisher.publishCustomEvent(
          realm,
          null,
          SugoiEventTypeEnum.FIND_GROUP_BY_ID,
          Map.ofEntries(
              Map.entry(EventKeysConfig.GROUP_ID, id),
              Map.entry(EventKeysConfig.APPLICATION_NAME, appName)));
      return Optional.ofNullable(group);
    } catch (Exception e) {
      sugoiEventPublisher.publishCustomEvent(
          realm,
          null,
          SugoiEventTypeEnum.FIND_GROUP_BY_ID_ERROR,
          Map.ofEntries(
              Map.entry(EventKeysConfig.GROUP_ID, id),
              Map.entry(EventKeysConfig.APPLICATION_NAME, appName),
              Map.entry(EventKeysConfig.ERROR, e.toString())));
      return Optional.empty();
    }
  }

  @Override
  public PageResult<Group> findByProperties(
      String realm, String appName, Group groupFilter, PageableResult pageableResult) {
    try {

      PageResult<Group> groups =
          storeProvider
              .getReaderStore(realm)
              .searchGroups(appName, groupFilter, pageableResult, SearchType.AND.name());
      sugoiEventPublisher.publishCustomEvent(
          realm,
          null,
          SugoiEventTypeEnum.FIND_GROUPS,
          Map.ofEntries(
              Map.entry(EventKeysConfig.APPLICATION_NAME, appName),
              Map.entry(EventKeysConfig.GROUP_FILTER, groupFilter)));
      return groups;
    } catch (Exception e) {
      sugoiEventPublisher.publishCustomEvent(
          realm,
          null,
          SugoiEventTypeEnum.FIND_GROUPS_ERROR,
          Map.ofEntries(
              Map.entry(EventKeysConfig.APPLICATION_NAME, appName),
              Map.entry(EventKeysConfig.GROUP_FILTER, groupFilter),
              Map.entry(EventKeysConfig.ERROR, e.toString())));
      throw e;
    }
  }

  @Override
  public ProviderResponse update(
      String realm, String appName, Group group, ProviderRequest providerRequest) {
    try {
      ProviderResponse response =
          storeProvider.getWriterStore(realm).updateGroup(appName, group, providerRequest);
      sugoiEventPublisher.publishCustomEvent(
          realm,
          null,
          SugoiEventTypeEnum.UPDATE_GROUP,
          Map.ofEntries(
              Map.entry(EventKeysConfig.GROUP, group),
              Map.entry(EventKeysConfig.APPLICATION_NAME, appName)));
      if (!providerRequest.isAsynchronousAllowed()
          && response.getStatus().equals(ProviderResponseStatus.OK)) {
        response.setEntity(findById(realm, appName, group.getName()).get());
      }
      return response;
    } catch (Exception e) {
      sugoiEventPublisher.publishCustomEvent(
          realm,
          null,
          SugoiEventTypeEnum.UPDATE_GROUP,
          Map.ofEntries(
              Map.entry(EventKeysConfig.GROUP, group),
              Map.entry(EventKeysConfig.APPLICATION_NAME, appName),
              Map.entry(EventKeysConfig.ERROR, e.toString())));
      throw e;
    }
  }

  // TODO attention il manque le userStorage du userId a ajouter
  @Override
  public ProviderResponse addUserToGroup(
      String realm,
      String storage,
      String userId,
      String appName,
      String groupName,
      ProviderRequest providerRequest) {
    try {
      if (storage == null) {
        User user =
            userService
                .findById(realm, null, userId)
                .orElseThrow(() -> new UserNotFoundException("message"));
        storage = (String) user.getAttributes().get(GlobalKeysConfig.USERSTORAGE);
      }
      ProviderResponse response =
          storeProvider
              .getWriterStore(realm, storage)
              .addUserToGroup(appName, groupName, userId, providerRequest);
      sugoiEventPublisher.publishCustomEvent(
          realm,
          storage,
          SugoiEventTypeEnum.ADD_USER_TO_GROUP,
          Map.ofEntries(
              Map.entry(EventKeysConfig.USER, userId),
              Map.entry(EventKeysConfig.APPLICATION_NAME, appName),
              Map.entry(EventKeysConfig.GROUP_NAME, groupName)));
      return response;
    } catch (Exception e) {
      sugoiEventPublisher.publishCustomEvent(
          realm,
          storage,
          SugoiEventTypeEnum.ADD_USER_TO_GROUP_ERROR,
          Map.ofEntries(
              Map.entry(EventKeysConfig.USER, userId),
              Map.entry(EventKeysConfig.APPLICATION_NAME, appName),
              Map.entry(EventKeysConfig.GROUP_NAME, groupName),
              Map.entry(EventKeysConfig.ERROR, e.toString())));
      throw e;
    }
  }

  // TODO attention il manque le userStorage du userId a ajouter

  @Override
  public ProviderResponse deleteUserFromGroup(
      String realm,
      String storage,
      String userId,
      String appName,
      String groupName,
      ProviderRequest providerRequest) {
    try {
      if (storage == null) {
        User user =
            userService
                .findById(realm, null, userId)
                .orElseThrow(() -> new UserNotFoundException("message"));
        storage = (String) user.getAttributes().get(GlobalKeysConfig.USERSTORAGE);
      }
      ProviderResponse response =
          storeProvider
              .getWriterStore(realm)
              .deleteUserFromGroup(appName, groupName, userId, providerRequest);
      sugoiEventPublisher.publishCustomEvent(
          realm,
          null,
          SugoiEventTypeEnum.DELETE_USER_FROM_GROUP,
          Map.ofEntries(
              Map.entry(EventKeysConfig.USER, userId),
              Map.entry(EventKeysConfig.APPLICATION_NAME, appName),
              Map.entry(EventKeysConfig.GROUP_NAME, groupName)));
      return response;
    } catch (Exception e) {
      sugoiEventPublisher.publishCustomEvent(
          realm,
          null,
          SugoiEventTypeEnum.DELETE_USER_FROM_GROUP,
          Map.ofEntries(
              Map.entry(EventKeysConfig.USER, userId),
              Map.entry(EventKeysConfig.APPLICATION_NAME, appName),
              Map.entry(EventKeysConfig.GROUP_NAME, groupName),
              Map.entry(EventKeysConfig.ERROR, e.toString())));
      throw e;
    }
  }
}
