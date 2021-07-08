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
package fr.insee.sugoi.core.realm;

import fr.insee.sugoi.core.exceptions.RealmNotFoundException;
import fr.insee.sugoi.core.model.ProviderRequest;
import fr.insee.sugoi.core.model.ProviderResponse;
import fr.insee.sugoi.model.Realm;
import fr.insee.sugoi.model.UserStorage;
import java.util.List;
import java.util.Optional;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

/**
 * This is called to fetch realms configuration.
 *
 * <p>All implementations need to declare a value for the property `fr.insee.sugoi.config.type` like
 * this :
 *
 * <pre>
 * &#64;ConditionalOnProperty(
 *  value = "fr.insee.sugoi.realm.config.type",
 *  havingValue = "<value>",
 *  matchIfMissing = false)
 * </pre>
 */
public interface RealmProvider {

  /**
   * Load the realm. Even if the returned object is cached, Implementations should take care of not
   * solliciting too much resources as this is called often.
   *
   * @param realmName, the realm name to return (case insensitive)
   * @return the realm found
   * @throws RealmNotFoundException
   */
  @Cacheable(value = "Realm", key = "#realmName")
  public Optional<Realm> load(String realmName) throws RealmNotFoundException;

  /**
   * Find an userStorage by name. Default implementation is to call 'load(realmName) and browse
   * through all user storage.
   *
   * @param realmName : the realm to search into (case insensitive)
   * @param userStorageName : the name of the user storage wanted (case insensitive)
   * @return the user storage found
   * @throws RealmNotFoundException
   */
  public default UserStorage loadUserStorageByUserStorageName(
      String realmName, String userStorageName) throws RealmNotFoundException {
    Realm r =
        load(realmName)
            .orElseThrow(
                () -> new RealmNotFoundException("The realm " + realmName + "doesn't exist"));
    if (r != null) {

      for (UserStorage us : r.getUserStorages()) {
        if (us.getName().equalsIgnoreCase(userStorageName)) {
          return us;
        }
      }
    }

    throw new RealmNotFoundException(String.format("No user Storage %s found", userStorageName));
  }

  /**
   * Find an user storage by his name. As we have no guarantee that each user storage accross all
   * realms have an unique name, the returned user storage is the first found with this name.
   *
   * <p>Default implementation is to browse through realms and user storages and teturn the first
   * found with this name (case insensitive)
   *
   * <p>There is no guarantee that the same user storage is returned everytime with the same call
   *
   * @param userStorageName to find
   * @return the first user storage found.
   * @throws RealmNotFoundException
   */
  public default Realm loadRealmByUserStorageName(String userStorageName)
      throws RealmNotFoundException {
    for (Realm r : findAll()) {
      for (UserStorage us : r.getUserStorages()) {
        if (us.getName().equalsIgnoreCase(userStorageName)) {
          return r;
        }
      }
    }
    throw new RealmNotFoundException(
        String.format("No Realm found for user Storage %s", userStorageName));
  }

  /**
   * Load all realms. Even if the returned List of object is cached, Implementations should take
   * care of not solliciting too much resources as this is called often.
   *
   * @return List<Realm>, the realm list
   */
  @Cacheable("Realms")
  public List<Realm> findAll();

  @CacheEvict(
      value = {"Realms", "Realm"},
      allEntries = true)
  public ProviderResponse createRealm(Realm realm, ProviderRequest providerRequest);

  @CacheEvict(
      value = {"Realms", "Realm"},
      allEntries = true)
  public ProviderResponse updateRealm(Realm realm, ProviderRequest providerRequest);

  @CacheEvict(
      value = {"Realms", "Realm"},
      allEntries = true)
  public ProviderResponse deleteRealm(String realmName, ProviderRequest providerRequest);
}
