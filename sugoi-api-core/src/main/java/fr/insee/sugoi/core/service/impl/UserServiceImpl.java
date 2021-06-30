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
import fr.insee.sugoi.core.exceptions.UserNotFoundException;
import fr.insee.sugoi.core.model.ProviderRequest;
import fr.insee.sugoi.core.model.ProviderResponse;
import fr.insee.sugoi.core.model.ProviderResponse.ProviderResponseStatus;
import fr.insee.sugoi.core.realm.RealmProvider;
import fr.insee.sugoi.core.seealso.SeeAlsoService;
import fr.insee.sugoi.core.service.UserService;
import fr.insee.sugoi.core.store.ReaderStore;
import fr.insee.sugoi.core.store.StoreProvider;
import fr.insee.sugoi.model.Realm;
import fr.insee.sugoi.model.User;
import fr.insee.sugoi.model.UserStorage;
import fr.insee.sugoi.model.paging.PageResult;
import fr.insee.sugoi.model.paging.PageableResult;
import fr.insee.sugoi.model.paging.SearchType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

  @Autowired private StoreProvider storeProvider;

  @Autowired private RealmProvider realmProvider;

  @Autowired private SugoiEventPublisher sugoiEventPublisher;

  @Autowired(required = false)
  private SeeAlsoService seeAlsoService;

  protected static final Logger logger = LogManager.getLogger(UserServiceImpl.class);

  @Override
  public ProviderResponse create(
      String realm, String storage, User user, ProviderRequest providerRequest) {
    try {
      ProviderResponse response =
          storeProvider.getWriterStore(realm, storage).createUser(user, providerRequest);
      sugoiEventPublisher.publishCustomEvent(
          realm,
          storage,
          SugoiEventTypeEnum.CREATE_USER,
          Map.ofEntries(Map.entry(EventKeysConfig.USER, user)));

      // TODO Must be done here because at the provider level we doesn't have the
      // readerstore
      if (!providerRequest.isAsynchronousAllowed()
          && response.getStatus().equals(ProviderResponseStatus.OK)) {
        response.setEntity(findById(realm, storage, user.getUsername()).get());
      }

      return response;
    } catch (Exception e) {
      sugoiEventPublisher.publishCustomEvent(
          realm,
          storage,
          SugoiEventTypeEnum.CREATE_USER_ERROR,
          Map.ofEntries(
              Map.entry(EventKeysConfig.USER, user),
              Map.entry(EventKeysConfig.ERROR, e.toString())));
      throw e;
    }
  }

  @Override
  public ProviderResponse update(
      String realm, String storage, User user, ProviderRequest providerRequest) {

    try {
      ProviderResponse response =
          storeProvider.getWriterStore(realm, storage).updateUser(user, providerRequest);
      sugoiEventPublisher.publishCustomEvent(
          realm,
          storage,
          SugoiEventTypeEnum.UPDATE_USER,
          Map.ofEntries(Map.entry(EventKeysConfig.USER, user)));
      if (!providerRequest.isAsynchronousAllowed()
          && response.getStatus().equals(ProviderResponseStatus.OK)) {
        response.setEntity(findById(realm, storage, user.getUsername()).get());
      }
      return response;
    } catch (Exception e) {
      sugoiEventPublisher.publishCustomEvent(
          realm,
          storage,
          SugoiEventTypeEnum.UPDATE_USER_ERROR,
          Map.ofEntries(
              Map.entry(EventKeysConfig.USER, user),
              Map.entry(EventKeysConfig.ERROR, e.toString())));
      throw e;
    }
  }

  @Override
  public ProviderResponse delete(
      String realmName, String storage, String id, ProviderRequest providerRequest) {
    try {
      ProviderResponse response =
          storeProvider.getWriterStore(realmName, storage).deleteUser(id, providerRequest);
      sugoiEventPublisher.publishCustomEvent(
          realmName,
          storage,
          SugoiEventTypeEnum.DELETE_USER,
          Map.ofEntries(Map.entry(EventKeysConfig.USER_ID, id)));
      return response;
    } catch (Exception e) {
      sugoiEventPublisher.publishCustomEvent(
          realmName,
          storage,
          SugoiEventTypeEnum.DELETE_USER_ERROR,
          Map.ofEntries(
              Map.entry(EventKeysConfig.USER_ID, id),
              Map.entry(EventKeysConfig.ERROR, e.toString())));
      if (e instanceof UserNotFoundException) {
        throw (UserNotFoundException) e;
      } else {
        throw e;
      }
    }
  }

  @Override
  public Optional<User> findById(String realmName, String storage, String id) {
    User user = null;
    try {
      if (id != null) {
        Realm realm = realmProvider.load(realmName);
        if (storage != null) {
          user = storeProvider.getReaderStore(realmName, storage).getUser(id);
          user.addMetadatas(GlobalKeysConfig.REALM, realmName.toLowerCase());
          user.addMetadatas(GlobalKeysConfig.USERSTORAGE, storage.toLowerCase());
        } else {
          for (UserStorage us : realm.getUserStorages()) {
            try {
              user = storeProvider.getReaderStore(realmName, us.getName()).getUser(id);
              user.addMetadatas(GlobalKeysConfig.REALM, realmName);
              user.addMetadatas(GlobalKeysConfig.USERSTORAGE, us.getName());
              break;
            } catch (Exception e) {
              logger.debug(
                  "Error when trying to find user "
                      + id
                      + " on realm "
                      + realmName
                      + " and userstorage "
                      + us
                      + " error "
                      + e.getMessage());
            }
          }
        }
        if (seeAlsoService != null
            && realm.getProperties().containsKey(GlobalKeysConfig.SEEALSO_ATTRIBUTES)) {
          String[] seeAlsosAttributes =
              realm
                  .getProperties()
                  .get(GlobalKeysConfig.SEEALSO_ATTRIBUTES)
                  .replace(" ", "")
                  .split(",");
          List<String> seeAlsos = new ArrayList<>();
          for (String seeAlsoAttribute : seeAlsosAttributes) {
            Object seeAlsoAttributeValue = user.getAttributes().get(seeAlsoAttribute);
            if (seeAlsoAttributeValue instanceof String) {
              seeAlsos.add((String) seeAlsoAttributeValue);
            } else if (seeAlsoAttributeValue instanceof List) {
              ((List<?>) seeAlsoAttributeValue)
                  .forEach(seeAlso -> seeAlsos.add(seeAlso.toString()));
            }
          }
          for (String seeAlso : seeAlsos) {
            seeAlsoService.decorateWithSeeAlso(user, seeAlso);
          }
        }
        sugoiEventPublisher.publishCustomEvent(
            realmName,
            storage,
            SugoiEventTypeEnum.FIND_USER_BY_ID,
            Map.ofEntries(Map.entry(EventKeysConfig.USER_ID, id)));
      }
      return Optional.ofNullable(user);
    } catch (Exception e) {
      sugoiEventPublisher.publishCustomEvent(
          realmName,
          storage,
          SugoiEventTypeEnum.FIND_USER_BY_ID_ERROR,
          Map.ofEntries(
              Map.entry(EventKeysConfig.USER_ID, id),
              Map.entry(EventKeysConfig.ERROR, e.toString())));
      return Optional.ofNullable(user);
    }
  }

  @Override
  public PageResult<User> findByProperties(
      String realm,
      String storage,
      User userProperties,
      PageableResult pageable,
      SearchType typeRecherche) {

    PageResult<User> result = new PageResult<>();
    result.setPageSize(pageable.getSize());
    try {
      if (storage != null) {
        result =
            storeProvider
                .getReaderStore(realm, storage)
                .searchUsers(userProperties, pageable, typeRecherche.name());
        result
            .getResults()
            .forEach(
                user -> {
                  user.addMetadatas(EventKeysConfig.REALM, realm);
                  user.addMetadatas(EventKeysConfig.USERSTORAGE, storage);
                });
      } else {
        Realm r = realmProvider.load(realm);
        for (UserStorage us : r.getUserStorages()) {
          ReaderStore readerStore =
              storeProvider.getStoreForUserStorage(realm, us.getName()).getReader();
          PageResult<User> temResult =
              readerStore.searchUsers(userProperties, pageable, typeRecherche.name());
          temResult
              .getResults()
              .forEach(
                  user -> {
                    user.addMetadatas(EventKeysConfig.REALM, realm);
                    user.addMetadatas(EventKeysConfig.USERSTORAGE, us.getName());
                  });
          result.getResults().addAll(temResult.getResults());
          result.setTotalElements(
              temResult.getTotalElements() == -1
                  ? temResult.getTotalElements()
                  : result.getTotalElements() + temResult.getTotalElements());
          result.setSearchToken(temResult.getSearchToken());
          result.setHasMoreResult(temResult.isHasMoreResult());
          if (result.getResults().size() >= result.getPageSize()) {
            sugoiEventPublisher.publishCustomEvent(
                realm,
                storage,
                SugoiEventTypeEnum.FIND_USERS,
                Map.ofEntries(
                    Map.entry(EventKeysConfig.USER_PROPERTIES, userProperties),
                    Map.entry(EventKeysConfig.PAGEABLE, pageable),
                    Map.entry(EventKeysConfig.TYPE_RECHERCHE, typeRecherche)));
            return result;
          }
          pageable.setSize(pageable.getSize() - result.getTotalElements());
        }
      }

    } catch (Exception e) {
      sugoiEventPublisher.publishCustomEvent(
          realm,
          storage,
          SugoiEventTypeEnum.FIND_USERS_ERROR,
          Map.ofEntries(
              Map.entry(EventKeysConfig.USER_PROPERTIES, userProperties),
              Map.entry(EventKeysConfig.PAGEABLE, pageable),
              Map.entry(EventKeysConfig.TYPE_RECHERCHE, typeRecherche),
              Map.entry(EventKeysConfig.ERROR, e.toString())));
      throw new RuntimeException("Erreur lors de la récupération des utilisateurs", e);
    }
    sugoiEventPublisher.publishCustomEvent(
        realm,
        storage,
        SugoiEventTypeEnum.FIND_USERS,
        Map.ofEntries(
            Map.entry(EventKeysConfig.USER_PROPERTIES, userProperties),
            Map.entry(EventKeysConfig.PAGEABLE, pageable),
            Map.entry(EventKeysConfig.TYPE_RECHERCHE, typeRecherche)));
    return result;
  }

  @Override
  public ProviderResponse addAppManagedAttribute(
      String realm,
      String storage,
      String userId,
      String attributeKey,
      String attribute,
      ProviderRequest providerRequest) {
    try {
      ProviderResponse response =
          storeProvider
              .getWriterStore(realm, storage)
              .addAppManagedAttribute(userId, attributeKey, attribute, providerRequest);
      sugoiEventPublisher.publishCustomEvent(
          realm,
          storage,
          SugoiEventTypeEnum.ADD_APP_MANAGED_ATTRIBUTES,
          Map.ofEntries(
              Map.entry(EventKeysConfig.ATTRIBUTE_KEY, attributeKey),
              Map.entry(EventKeysConfig.ATTRIBUTE_VALUE, attribute),
              Map.entry(EventKeysConfig.USER_ID, userId)));
      return response;
    } catch (Exception e) {
      sugoiEventPublisher.publishCustomEvent(
          realm,
          storage,
          SugoiEventTypeEnum.ADD_APP_MANAGED_ATTRIBUTES_ERROR,
          Map.ofEntries(
              Map.entry(EventKeysConfig.ATTRIBUTE_KEY, attributeKey),
              Map.entry(EventKeysConfig.ATTRIBUTE_VALUE, attribute),
              Map.entry(EventKeysConfig.USER_ID, userId),
              Map.entry(EventKeysConfig.ERROR, e.toString())));
      throw e;
    }
  }

  @Override
  public ProviderResponse deleteAppManagedAttribute(
      String realm,
      String storage,
      String userId,
      String attributeKey,
      String attribute,
      ProviderRequest providerRequest) {
    try {
      ProviderResponse response =
          storeProvider
              .getWriterStore(realm, storage)
              .deleteAppManagedAttribute(userId, attributeKey, attribute, providerRequest);
      sugoiEventPublisher.publishCustomEvent(
          realm,
          storage,
          SugoiEventTypeEnum.DELETE_APP_MANAGED_ATTRIBUTES,
          Map.ofEntries(
              Map.entry(EventKeysConfig.ATTRIBUTE_KEY, attributeKey),
              Map.entry(EventKeysConfig.ATTRIBUTE_VALUE, attribute),
              Map.entry(EventKeysConfig.USER_ID, userId)));
      return response;
    } catch (Exception e) {
      sugoiEventPublisher.publishCustomEvent(
          realm,
          storage,
          SugoiEventTypeEnum.DELETE_APP_MANAGED_ATTRIBUTES_ERROR,
          Map.ofEntries(
              Map.entry(EventKeysConfig.ATTRIBUTE_KEY, attributeKey),
              Map.entry(EventKeysConfig.ATTRIBUTE_VALUE, attribute),
              Map.entry(EventKeysConfig.USER_ID, userId),
              Map.entry(EventKeysConfig.ERROR, e.toString())));
      throw e;
    }
  }
}
