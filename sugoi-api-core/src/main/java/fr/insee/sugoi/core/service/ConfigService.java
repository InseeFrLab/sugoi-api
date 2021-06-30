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
package fr.insee.sugoi.core.service;

import fr.insee.sugoi.core.exceptions.RealmAlreadyExistException;
import fr.insee.sugoi.core.exceptions.RealmNotCreatedException;
import fr.insee.sugoi.core.exceptions.RealmNotFoundException;
import fr.insee.sugoi.core.model.ProviderRequest;
import fr.insee.sugoi.core.model.ProviderResponse;
import fr.insee.sugoi.model.Realm;
import java.util.List;
import java.util.Optional;

/** Managing realm configuration */
public interface ConfigService {

  /**
   * Finds a realm by its name by using the RealmProvider Bean The result is cached.
   *
   * @param name
   * @return an optional of the Realm
   */
  Optional<Realm> getRealm(String name);

  /**
   * Finds all realms using the RealmProvider bean. The resulting list is cached.
   *
   * @return a list of all realms
   */
  List<Realm> getRealms();

  /**
   * [NotYetImplemented] Check if the realm exists (by name) and delete realm using the name
   *
   * @throws RealmNotFoundException if realmName doesn't match with the name of an existing realm
   */
  ProviderResponse deleteRealm(String realmName, ProviderRequest providerRequest);

  /**
   * [NotYetImplemented] Check if the realm exists and update realm
   *
   * @throws RealmNotFoundException if realm doesn't match (by name) with an existing realm
   */
  ProviderResponse updateRealm(Realm realm, ProviderRequest providerRequest);

  /**
   * [NotYetImplemented] Check if a realm with the same name already exists, and creates it if it
   * doesn't exist
   *
   * @throws RealmAlreadyExistException if realm already exists
   * @throws RealmNotCreatedException if fail to create realm
   */
  ProviderResponse createRealm(Realm realm, ProviderRequest providerRequest);
}
