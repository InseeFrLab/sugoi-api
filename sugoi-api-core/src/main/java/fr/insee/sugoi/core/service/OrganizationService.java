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

import fr.insee.sugoi.core.exceptions.OrganizationAlreadyExistException;
import fr.insee.sugoi.core.exceptions.OrganizationNotCreatedException;
import fr.insee.sugoi.core.exceptions.OrganizationNotFoundException;
import fr.insee.sugoi.core.model.ProviderRequest;
import fr.insee.sugoi.core.model.ProviderResponse;
import fr.insee.sugoi.model.Organization;
import fr.insee.sugoi.model.paging.PageResult;
import fr.insee.sugoi.model.paging.PageableResult;
import fr.insee.sugoi.model.paging.SearchType;
import java.util.Optional;

public interface OrganizationService {

  /**
   * Check if the organization exists in the realm (by name) and create it if it doesn't exists
   *
   * @param realm
   * @param storage
   * @param organization
   * @return created Organization
   * @throws OrganizationAlreadyExistException if an organization with the same name already exist
   * @throws OrganizationNotCreatedException if the organization is not found after create
   */
  ProviderResponse create(
      String realm, String storage, Organization organization, ProviderRequest providerRequest);

  /**
   * Check if the organization exists in the realm (by name) and delete it if it exists
   *
   * @param realm
   * @param storage
   * @param id
   * @throws OrganizationNotFoundException if the organization is not found
   */
  ProviderResponse delete(String realm, String storage, String id, ProviderRequest providerRequest);

  /**
   * Find an organization by its name in the realm
   *
   * @param realm
   * @param storage
   * @param id
   * @return an optional of organization
   */
  Optional<Organization> findById(String realm, String storage, String id);

  /**
   * Find organizations by criterias in the realm
   *
   * @param realm
   * @param storage
   * @param organizationFilter
   * @param pageableResult
   * @param typeRecherche
   * @return
   */
  PageResult<Organization> findByProperties(
      String realm,
      String storage,
      Organization organizationFilter,
      PageableResult pageableResult,
      SearchType typeRecherche);

  /**
   * Check if the organization exist in the realm (by name) and update it if it exist
   *
   * @param realm
   * @param storage
   * @param organization
   * @throws OrganizationNotFoundException if the organization is not found
   */
  ProviderResponse update(
      String realm, String storage, Organization organization, ProviderRequest providerRequest);
}
