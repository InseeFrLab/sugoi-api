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
package fr.insee.sugoi.core.store;

import fr.insee.sugoi.core.exceptions.InvalidPasswordException;
import fr.insee.sugoi.core.model.ProviderRequest;
import fr.insee.sugoi.core.model.ProviderResponse;
import fr.insee.sugoi.model.Application;
import fr.insee.sugoi.model.Group;
import fr.insee.sugoi.model.Organization;
import fr.insee.sugoi.model.User;
import fr.insee.sugoi.model.paging.PasswordChangeRequest;
import fr.insee.sugoi.model.paging.SendMode;
import java.util.List;

/** Writer stores are responsible for all operations modifying the underlying store. */
public interface WriterStore {

  /**
   * Create the user in the store.
   *
   * @param user
   * @return the user as it has been passed (address location migth have been added).
   */
  ProviderResponse createUser(User user, ProviderRequest providerRequest);

  /**
   * Replace the user with the same id by the updatedUser in the store.
   *
   * @param updatedUser
   * @return A provider response
   */
  ProviderResponse updateUser(User updatedUser, ProviderRequest providerRequest);

  /**
   * Delete the user id in the store.
   *
   * @param organizationId
   */
  ProviderResponse deleteUser(String id, ProviderRequest providerRequest);

  /**
   * Create the organization in the store.
   *
   * @param organization
   * @throws UnsupportedOperationException if the configuration for organizations is not set on the
   *     UserStorage.
   * @return the organization as it has been passed (address location migth have been added).
   */
  ProviderResponse createOrganization(Organization organization, ProviderRequest providerRequest);

  /**
   * Replace the organization with the same id by the updatedOrganization in the store.
   *
   * @param updatedOrganization
   * @throws UnsupportedOperationException if the configuration for organizations is not set on the
   *     UserStorage.
   * @return the updatedOrganization as it has been passed (address location migth have been added).
   */
  ProviderResponse updateOrganization(
      Organization updatedOrganization, ProviderRequest providerRequest);

  /**
   * Delete the organization id in the store.
   *
   * @param organizationId
   * @throws UnsupportedOperationException if the configuration for organizations is not set on the
   *     UserStorage.
   */
  ProviderResponse deleteOrganization(String organizationId, ProviderRequest providerRequest);

  /**
   * Create the application in the store. If the application contains groups, add it but do not add
   * the group members.
   *
   * @param application
   * @throws UnsupportedOperationException if the configuration for applications is not set for the
   *     Realm.
   * @return the application as it has been passed
   */
  ProviderResponse createApplication(Application application, ProviderRequest providerRequest);

  /**
   * Replace the application with the same id by the updatedApplication in the store. Groups that do
   * not exist are removed, groups for which something changes but without changing the name are
   * modified but with users unchanged and groups which id doesn't exist yet are created without
   * users.
   *
   * @param updatedApplication
   * @throws UnsupportedOperationException if the configuration for applications is not set for the
   *     Realm.
   * @return the updateApplication as it has been passed
   */
  ProviderResponse updateApplication(
      Application updatedApplication, ProviderRequest providerRequest);

  /**
   * Delete the application applicationName in the store.
   *
   * @throws UnsupportedOperationException if the configuration for applications is not set on the
   *     Realm.
   * @param applicationName
   */
  ProviderResponse deleteApplication(String applicationName, ProviderRequest providerRequest);

  /**
   * Create the group in the application appName. Users are not added.
   *
   * @param appName
   * @param group
   * @throws UnsupportedOperationException if the configuration for applications or groups are not
   *     set.
   * @return the group as it has been passed
   */
  ProviderResponse createGroup(String appName, Group group, ProviderRequest providerRequest);

  /**
   * Replace the group of same name in the application appName by updatedGroup. Users are unchanged.
   *
   * @param appName
   * @param updatedGroup
   * @throws UnsupportedOperationException if the configuration for applications or groups are not
   *     set.
   * @return the group as it has been passed.
   */
  ProviderResponse updateGroup(String appName, Group updatedGroup, ProviderRequest providerRequest);

  /**
   * Delete the group groupName of the application appName in the store. Users remain unchanged.
   *
   * @param appName
   * @param groupName
   * @throws UnsupportedOperationException if the configuration for applications or groups are not
   *     set.
   */
  ProviderResponse deleteGroup(String appName, String groupName, ProviderRequest providerRequest);

  /**
   * Add a user to the group groupName in the application appName. The user might not exist on the
   * realm.
   *
   * @param appName
   * @param groupName
   * @param userId
   * @throws UnsupportedOperationException if the configuration for applications or groups is not
   *     set.
   */
  ProviderResponse addUserToGroup(
      String appName, String groupName, String userId, ProviderRequest providerRequest);

  /**
   * Delete the user userId from the group groupName in the application appName.
   *
   * @param appName
   * @param groupName
   * @param userId
   * @throws UnsupportedOperationException if the configuration for applications or groups is not
   *     set.
   */
  ProviderResponse deleteUserFromGroup(
      String appName, String groupName, String userId, ProviderRequest providerRequest);

  /**
   * Set the password of user to initPassword. If password already exist, changes it.
   *
   * @param user
   * @param initPassword, password to set to the user, cannot be null
   * @param pcr not used
   * @param sendModes not used
   */
  ProviderResponse initPassword(
      String user,
      String initPassword,
      PasswordChangeRequest pcr,
      List<SendMode> sendModes,
      ProviderRequest providerRequest);

  /**
   * Set the password of user to generatedPassword. Same behaviour than initPassword.
   *
   * @param user
   * @param generatedPassword password to set to the user, cannot be null
   * @param pcr not used
   * @param sendModes not used
   */
  ProviderResponse reinitPassword(
      String userId,
      String generatedPassword,
      PasswordChangeRequest pcr,
      List<SendMode> sendModes,
      ProviderRequest providerRequest);

  /**
   * Change the user password from oldPassword to newPassword. If user do not have password
   * oldPassword should be set to null to be changed.
   *
   * @param user
   * @param oldPassword
   * @param newPassword
   * @param pcr not used
   * @throws InvalidPasswordException if oldPassword doesn't match the actual password.
   */
  ProviderResponse changePassword(
      String user,
      String oldPassword,
      String newPassword,
      PasswordChangeRequest pcr,
      ProviderRequest providerRequest);

  /**
   * Add the attribute to the app-managed-attribute-key in the store
   *
   * @param userId
   * @param attribute
   */
  ProviderResponse addAppManagedAttribute(
      String userId, String attributeKey, String attributeValue, ProviderRequest providerRequest);

  /**
   * Delete the attribute value from the app-managed-attribute-key in the store
   *
   * @param userId
   * @param attribute
   */
  ProviderResponse deleteAppManagedAttribute(
      String userId, String attributeKey, String attributeValue, ProviderRequest providerRequest);
}
