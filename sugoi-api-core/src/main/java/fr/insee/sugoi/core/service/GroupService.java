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

import fr.insee.sugoi.core.exceptions.GroupAlreadyExistException;
import fr.insee.sugoi.core.exceptions.GroupNotCreatedException;
import fr.insee.sugoi.core.exceptions.GroupNotFoundException;
import fr.insee.sugoi.core.exceptions.UserNotFoundException;
import fr.insee.sugoi.core.model.ProviderRequest;
import fr.insee.sugoi.core.model.ProviderResponse;
import fr.insee.sugoi.model.Group;
import fr.insee.sugoi.model.paging.PageResult;
import fr.insee.sugoi.model.paging.PageableResult;
import java.util.Optional;

public interface GroupService {

  /**
   * Check if the group exists in the app (by name) and create it if it doesn't exist
   *
   * @param realm
   * @param appName
   * @param group
   * @return the created group
   * @throws GroupAlreadyExistException if the group already exist
   * @throws GroupNotCreatedException if fail to create group
   */
  ProviderResponse create(
      String realm, String appName, Group group, ProviderRequest providerRequest);

  /**
   * Check if the group exists in the app (by name) and update it
   *
   * @param realm
   * @param appName
   * @param group
   * @throws GroupNotFoundException if group doesn't exist in app
   */
  ProviderResponse update(
      String realm, String appName, Group group, ProviderRequest providerRequest);

  /**
   * Check if the group exists in the app (by name) and delete it
   *
   * @param realm
   * @param appName
   * @param id
   * @throws GroupNotFoundException if group doesn't exist in app
   */
  ProviderResponse delete(String realm, String appName, String id, ProviderRequest providerRequest);

  /**
   * Find a group by its id in an application
   *
   * @param realm
   * @param appName
   * @param id
   * @return an optional with the group found
   */
  Optional<Group> findById(String realm, String appName, String id);

  /**
   * Find a groups matching criterias
   *
   * @param realm
   * @param appName
   * @param groupFilter
   * @param pageableResult
   * @return a list of all groups matching criterias
   */
  PageResult<Group> findByProperties(
      String realm, String appName, Group groupFilter, PageableResult pageableResult);

  /**
   * Check if user and group exist and add the user to the group
   *
   * @param realm
   * @param userId
   * @param appName
   * @param groupName
   * @throws GroupNotFoundException if group doesn't exist in app
   * @throws UserNotFoundException if user doesn't exist realm
   */
  ProviderResponse addUserToGroup(
      String realm,
      String storage,
      String userId,
      String appName,
      String groupName,
      ProviderRequest providerRequest);

  /**
   * Check if user and group exist and remove the user from the group
   *
   * @param realm
   * @param userId
   * @param appName
   * @param groupName
   * @throws GroupNotFoundException if group doesn't exist in app
   * @throws UserNotFoundException if user doesn't exist realm
   */
  ProviderResponse deleteUserFromGroup(
      String realm,
      String storage,
      String userId,
      String appName,
      String groupName,
      ProviderRequest providerRequest);
}
