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

import fr.insee.sugoi.core.exceptions.ApplicationAlreadyExistException;
import fr.insee.sugoi.core.exceptions.ApplicationNotCreatedException;
import fr.insee.sugoi.core.exceptions.ApplicationNotFoundException;
import fr.insee.sugoi.core.model.ProviderRequest;
import fr.insee.sugoi.core.model.ProviderResponse;
import fr.insee.sugoi.model.Application;
import fr.insee.sugoi.model.paging.PageResult;
import fr.insee.sugoi.model.paging.PageableResult;
import java.util.Optional;

public interface ApplicationService {

  /**
   * Verify if the application doesn't already exist in the realm (by its name) and create if it
   * does'nt exist
   *
   * @param realm
   * @param application
   * @return The created application
   * @throws ApplicationAlreadyExistException if application already exist in realm
   * @throws ApplicationNotCreatedException if fail to create application
   */
  ProviderResponse create(String realm, Application application, ProviderRequest providerRequest);

  /**
   * if application exists, updates application, and if the application has groups, they are also
   * updated. Warning, this is not an atomic process (an app can be updated and not the groups).
   *
   * @param realm
   * @param application
   * @throws ApplicationNotFoundException if application doesn't exist in realm
   */
  ProviderResponse update(String realm, Application application, ProviderRequest providerRequest);

  /**
   * if application exists, deletes the application from the realm, all the groups from the
   * application are also deleted.
   *
   * @param realm
   * @param id
   * @throws ApplicationNotFoundException if application doesn't exist in realm
   */
  ProviderResponse delete(String realm, String id, ProviderRequest providerRequest);

  /**
   * Finds an application by its name
   *
   * @param realm
   * @param id
   * @return an Optional with the app found (with list of members for each groups)
   */
  Optional<Application> findById(String realm, String id);

  /**
   * Finds an application by some properties
   *
   * @param realm
   * @param storage
   * @param applicationFilter
   * @param pageableResult
   * @return a list of found applications (with list of members for each groups))
   */
  PageResult<Application> findByProperties(
      String realm, Application applicationFilter, PageableResult pageableResult);
}
