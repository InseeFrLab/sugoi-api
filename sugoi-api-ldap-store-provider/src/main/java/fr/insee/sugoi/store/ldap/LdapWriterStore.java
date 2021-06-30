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
package fr.insee.sugoi.store.ldap;

import com.unboundid.ldap.sdk.AddRequest;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.DeleteRequest;
import com.unboundid.ldap.sdk.ExtendedResult;
import com.unboundid.ldap.sdk.LDAPConnectionPool;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.Modification;
import com.unboundid.ldap.sdk.ModificationType;
import com.unboundid.ldap.sdk.ModifyRequest;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.extensions.PasswordModifyExtendedRequest;
import com.unboundid.util.SubtreeDeleter;
import fr.insee.sugoi.core.event.configuration.EventKeysConfig;
import fr.insee.sugoi.core.exceptions.AppManagedAttributeException;
import fr.insee.sugoi.core.exceptions.ApplicationAlreadyExistException;
import fr.insee.sugoi.core.exceptions.ApplicationNotFoundException;
import fr.insee.sugoi.core.exceptions.GroupAlreadyExistException;
import fr.insee.sugoi.core.exceptions.GroupNotFoundException;
import fr.insee.sugoi.core.exceptions.InvalidPasswordException;
import fr.insee.sugoi.core.exceptions.OrganizationAlreadyExistException;
import fr.insee.sugoi.core.exceptions.OrganizationNotFoundException;
import fr.insee.sugoi.core.exceptions.StoragePolicyNotMetException;
import fr.insee.sugoi.core.exceptions.UserAlreadyExistException;
import fr.insee.sugoi.core.exceptions.UserNotFoundException;
import fr.insee.sugoi.core.model.ProviderRequest;
import fr.insee.sugoi.core.model.ProviderResponse;
import fr.insee.sugoi.core.model.ProviderResponse.ProviderResponseStatus;
import fr.insee.sugoi.core.store.WriterStore;
import fr.insee.sugoi.ldap.utils.LdapFactory;
import fr.insee.sugoi.ldap.utils.config.LdapConfigKeys;
import fr.insee.sugoi.ldap.utils.mapper.AddressLdapMapper;
import fr.insee.sugoi.ldap.utils.mapper.ApplicationLdapMapper;
import fr.insee.sugoi.ldap.utils.mapper.GroupLdapMapper;
import fr.insee.sugoi.ldap.utils.mapper.OrganizationLdapMapper;
import fr.insee.sugoi.ldap.utils.mapper.UserLdapMapper;
import fr.insee.sugoi.model.Application;
import fr.insee.sugoi.model.Group;
import fr.insee.sugoi.model.Organization;
import fr.insee.sugoi.model.User;
import fr.insee.sugoi.model.paging.PasswordChangeRequest;
import fr.insee.sugoi.model.paging.SendMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class LdapWriterStore extends LdapStore implements WriterStore {

  private LDAPConnectionPool ldapPoolConnection;

  private UserLdapMapper userLdapMapper;
  private OrganizationLdapMapper organizationLdapMapper;
  private GroupLdapMapper groupLdapMapper;
  private ApplicationLdapMapper applicationLdapMapper;
  private AddressLdapMapper addressLdapMapper;

  private LdapReaderStore ldapReaderStore;

  public LdapWriterStore(Map<String, String> config) {
    try {
      this.ldapPoolConnection = LdapFactory.getConnectionPoolAuthenticated(config);
      this.config = config;
      userLdapMapper = new UserLdapMapper(config);
      organizationLdapMapper = new OrganizationLdapMapper(config);
      groupLdapMapper = new GroupLdapMapper(config);
      applicationLdapMapper = new ApplicationLdapMapper(config);
      addressLdapMapper = new AddressLdapMapper(config);
      ldapReaderStore = new LdapReaderStore(config);
    } catch (LDAPException e) {
      throw new RuntimeException(e);
    }
  }

  /** Delete a user and its address */
  @Override
  public ProviderResponse deleteUser(String id, ProviderRequest providerRequest) {
    User currentUser = ldapReaderStore.getUser(id);
    if (currentUser != null) {
      try {
        currentUser
            .getGroups()
            .forEach(
                group ->
                    deleteUserFromGroup(group.getAppName(), group.getName(), id, providerRequest));
        if (currentUser.getAddress().get("id") != null) {
          deleteAddress(currentUser.getAddress().get("id"));
        }
        DeleteRequest dr = new DeleteRequest(getUserDN(id));
        ldapPoolConnection.delete(dr);
        ProviderResponse response = new ProviderResponse();
        response.setStatus(ProviderResponseStatus.OK);
        response.setEntityId(id);
        return response;
      } catch (LDAPException e) {
        throw new RuntimeException("Failed to delete user " + id, e);
      }
    }
    throw new UserNotFoundException(
        "Cannot find user "
            + id
            + " in realm "
            + config.get(LdapConfigKeys.REALM_NAME)
            + " and userstorage "
            + config.get(LdapConfigKeys.USERSTORAGE_NAME));
  }

  /**
   * Create a user in ldap. If the user has an address, a ldap resource address is generated with a
   * random value as id An organization link can be created but may not exist
   */
  @Override
  public ProviderResponse createUser(User user, ProviderRequest providerRequest) {
    if (ldapReaderStore.getUser(user.getUsername()) == null) {
      try {
        if (user.getAddress() != null && user.getAddress().size() > 0) {
          UUID addressUuid = createAddress(user.getAddress());
          user.addAddress("id", addressUuid.toString());
        }
        AddRequest userAddRequest =
            new AddRequest(getUserDN(user.getUsername()), userLdapMapper.mapToAttributes(user));
        ldapPoolConnection.add(userAddRequest);
      } catch (LDAPException e) {
        throw new RuntimeException(
            "Failed to create user. Provider message : " + e.getMessage(), e);
      }
      ProviderResponse response = new ProviderResponse();
      response.setStatus(ProviderResponseStatus.OK);
      response.setEntityId(user.getUsername());
      return response;
    }
    throw new UserAlreadyExistException(
        "User "
            + user.getUsername()
            + " already exist in realm "
            + config.get(LdapConfigKeys.REALM_NAME)
            + "and userstorage "
            + config.get(LdapConfigKeys.USERSTORAGE_NAME));
  }

  /** Update the ldap properties of a user. Current user is read to retrieve user address link */
  @Override
  public ProviderResponse updateUser(User updatedUser, ProviderRequest providerRequest) {
    if (ldapReaderStore.getUser(updatedUser.getUsername()) != null) {
      try {
        if (updatedUser != null) {
          User currentUser = ldapReaderStore.getUser(updatedUser.getUsername());
          if (currentUser == null) {
            throw new UserNotFoundException("User is not found on this user storage");
          }
          if (updatedUser.getAddress() != null && updatedUser.getAddress().size() > 0) {
            if (currentUser.getAddress() != null && currentUser.getAddress().containsKey("id")) {
              updateAddress(currentUser.getAddress().get("id"), updatedUser.getAddress());
            } else {
              Map<String, String> newAddress = new HashMap<>();
              newAddress.put("id", createAddress(updatedUser.getAddress()).toString());
              updatedUser.setAddress(newAddress);
              createAddress(updatedUser.getAddress());
            }
          }
          ModifyRequest mr =
              new ModifyRequest(
                  getUserDN(updatedUser.getUsername()), userLdapMapper.createMods(updatedUser));
          ldapPoolConnection.modify(mr);
        }
      } catch (LDAPException e) {
        throw new RuntimeException("Failed to update user while writing to LDAP", e);
      }
      ProviderResponse response = new ProviderResponse();
      response.setStatus(ProviderResponseStatus.OK);
      response.setEntityId(updatedUser.getUsername());
      return response;
    }
    throw new UserNotFoundException(
        "User "
            + updatedUser.getUsername()
            + " doesn't exist in realm "
            + config.get(LdapConfigKeys.REALM_NAME)
            + "and userstorage "
            + config.get(LdapConfigKeys.USERSTORAGE_NAME));
  }

  @Override
  public ProviderResponse deleteGroup(
      String appName, String groupName, ProviderRequest providerRequest) {
    try {
      if (ldapReaderStore.getGroup(appName, groupName) == null) {
        throw new GroupNotFoundException(
            String.format(
                "Group %s doesn't exist in application %s in realm %s",
                groupName, appName, config.get(LdapConfigKeys.REALM_NAME)));
      } else {
        DeleteRequest dr = new DeleteRequest(getGroupDN(appName, groupName));
        ldapPoolConnection.delete(dr);
        ProviderResponse response = new ProviderResponse();
        response.setStatus(ProviderResponseStatus.OK);
        response.setEntityId(groupName);
        return response;
      }
    } catch (LDAPException e) {
      throw new RuntimeException("Failed to delete group " + groupName, e);
    }
  }

  @Override
  public ProviderResponse createGroup(
      String appName, Group group, ProviderRequest providerRequest) {
    try {
      if (ldapReaderStore.getGroup(appName, group.getName()) != null) {
        throw new GroupAlreadyExistException(
            String.format(
                "Group %s already exist in application %s in realm %s",
                group.getName(), appName, config.get(LdapConfigKeys.REALM_NAME)));
      }

      if (matchGroupWildcardPattern(appName, group.getName())) {
        // check if parent entry exist
        if (ldapPoolConnection.getEntry(getGroupSource(appName)) == null) {
          AddRequest groupsAR =
              new AddRequest(
                  getGroupSource(appName),
                  new Attribute("objectClass", "top", "organizationalUnit"));
          ldapPoolConnection.add(groupsAR);
        }
        AddRequest ar =
            new AddRequest(
                getGroupDN(appName, group.getName()), groupLdapMapper.mapToAttributes(group));
        ldapPoolConnection.add(ar);
      } else {
        throw new StoragePolicyNotMetException("Group pattern won't match");
      }
    } catch (LDAPException e) {
      throw new RuntimeException("Failed to create group " + group.getName(), e);
    }
    ProviderResponse response = new ProviderResponse();
    response.setStatus(ProviderResponseStatus.OK);
    response.setEntityId(group.getName());
    return response;
  }

  @Override
  public ProviderResponse updateGroup(
      String appName, Group updatedGroup, ProviderRequest providerRequest) {
    try {
      if (ldapReaderStore.getGroup(appName, updatedGroup.getName()) == null) {
        throw new GroupNotFoundException(
            String.format(
                "Group %s doesn't exist in application %s in realm %s",
                updatedGroup.getName(), appName, config.get(LdapConfigKeys.REALM_NAME)));
      } else {
        ModifyRequest mr =
            new ModifyRequest(
                getGroupDN(appName, updatedGroup.getName()),
                groupLdapMapper.createMods(updatedGroup));
        ldapPoolConnection.modify(mr);
      }
    } catch (LDAPException e) {
      throw new RuntimeException(
          "Failed to update group " + updatedGroup.getName() + " while writing to LDAP", e);
    }
    ProviderResponse response = new ProviderResponse();
    response.setStatus(ProviderResponseStatus.OK);
    response.setEntityId(updatedGroup.getName());
    return response;
  }

  /** Delete an organization and its address */
  @Override
  public ProviderResponse deleteOrganization(String name, ProviderRequest providerRequest) {
    if (ldapReaderStore.getOrganization(name) != null) {
      try {
        Organization currentOrganization = ldapReaderStore.getOrganization(name);
        if (currentOrganization.getAddress().get("id") != null) {
          deleteAddress(currentOrganization.getAddress().get("id"));
        }
        DeleteRequest dr = new DeleteRequest(getOrganizationDN(name));
        ldapPoolConnection.delete(dr);
        ProviderResponse response = new ProviderResponse();
        response.setStatus(ProviderResponseStatus.OK);
        response.setEntityId(name);
        return response;
      } catch (LDAPException e) {
        throw new RuntimeException("Failed to delete organisation " + name, e);
      }
    }
    throw new OrganizationNotFoundException(
        "Cannot find organization "
            + name
            + " in realm "
            + config.get(LdapConfigKeys.REALM_NAME)
            + " in userstorage "
            + config.get(LdapConfigKeys.USERSTORAGE_NAME));
  }

  /**
   * Create an organization in ldap. If the organization has an address, a ldap resource address is
   * generated with a random value as id An organization link can be created but may not exist
   */
  @Override
  public ProviderResponse createOrganization(
      Organization organization, ProviderRequest providerRequest) {
    if (ldapReaderStore.getOrganization(organization.getIdentifiant()) == null) {
      try {
        if (organization.getAddress() != null && organization.getAddress().size() > 0) {
          UUID addressUuid = createAddress(organization.getAddress());
          organization.addAddress("id", addressUuid.toString());
        }
        AddRequest ar =
            new AddRequest(
                getOrganizationDN(organization.getIdentifiant()),
                organizationLdapMapper.mapToAttributes(organization));
        ldapPoolConnection.add(ar);
      } catch (LDAPException e) {
        throw new RuntimeException(
            "Failed to create organization " + organization.getIdentifiant(), e);
      }
      ProviderResponse response = new ProviderResponse();
      response.setStatus(ProviderResponseStatus.OK);
      response.setEntityId(organization.getIdentifiant());
      return response;
    }
    throw new OrganizationAlreadyExistException(
        "Organization "
            + organization.getIdentifiant()
            + " already exist in realm "
            + config.get(LdapConfigKeys.REALM_NAME)
            + " userstorage "
            + config.get(LdapConfigKeys.USERSTORAGE_NAME));
  }

  /**
   * Update the ldap properties of a organization. Current organization is read to retrieve address
   * link
   */
  @Override
  public ProviderResponse updateOrganization(
      Organization updatedOrganization, ProviderRequest providerRequest) {
    if (ldapReaderStore.getOrganization(updatedOrganization.getIdentifiant()) != null) {

      try {
        Organization currentOrganization =
            ldapReaderStore.getOrganization(updatedOrganization.getIdentifiant());
        if (updatedOrganization.getAddress() != null
            && updatedOrganization.getAddress().size() > 0) {
          if (currentOrganization.getAddress().containsKey("id")) {
            updateAddress(
                currentOrganization.getAddress().get("id"), updatedOrganization.getAddress());
          } else {
            Map<String, String> newAddress = new HashMap<>();
            newAddress.put("id", createAddress(updatedOrganization.getAddress()).toString());
            updatedOrganization.setAddress(newAddress);
          }
        }
        ModifyRequest mr =
            new ModifyRequest(
                getOrganizationDN(updatedOrganization.getIdentifiant()),
                organizationLdapMapper.createMods(updatedOrganization));
        ldapPoolConnection.modify(mr);
      } catch (LDAPException e) {
        throw new RuntimeException(
            "Failed to update organization "
                + updatedOrganization.getIdentifiant()
                + "while writing to LDAP",
            e);
      }
      ProviderResponse response = new ProviderResponse();
      response.setStatus(ProviderResponseStatus.OK);
      response.setEntityId(updatedOrganization.getIdentifiant());
      return response;
    }
    throw new OrganizationNotFoundException(
        "Cannot find organization "
            + updatedOrganization.getIdentifiant()
            + " in realm "
            + config.get(LdapConfigKeys.REALM_NAME)
            + " in userstorage "
            + config.get(LdapConfigKeys.USERSTORAGE_NAME));
  }

  @Override
  public ProviderResponse deleteUserFromGroup(
      String appName, String groupName, String userId, ProviderRequest providerRequest) {
    if (ldapReaderStore.getGroup(appName, groupName) == null) {
      throw new GroupNotFoundException(
          "Cannot find group "
              + groupName
              + " in app "
              + appName
              + " in realm "
              + config.get(LdapConfigKeys.REALM_NAME));
    }
    if (ldapReaderStore.getUser(userId) == null) {
      throw new UserNotFoundException(
          "Cannot find user with id "
              + userId
              + " in realm "
              + config.get(LdapConfigKeys.REALM_NAME));
    }
    try {
      ModifyRequest mr =
          new ModifyRequest(
              getGroupDN(appName, groupName),
              new Modification(ModificationType.DELETE, "uniqueMember", getUserDN(userId)));
      ldapPoolConnection.modify(mr);
      ProviderResponse response = new ProviderResponse();
      response.setStatus(ProviderResponseStatus.OK);
      response.setEntityId(userId);
      return response;
    } catch (LDAPException e) {
      if (!e.getResultCode().equals(ResultCode.NO_SUCH_ATTRIBUTE)) {
        throw new RuntimeException("Failed to remove user to group " + groupName, e);
      }
      throw new RuntimeException(e.getMessage());
    }
  }

  @Override
  public ProviderResponse addUserToGroup(
      String appName, String groupName, String userId, ProviderRequest providerRequest) {
    if (ldapReaderStore.getGroup(appName, groupName) == null) {
      throw new GroupNotFoundException(
          "Cannot find group "
              + groupName
              + " in app "
              + appName
              + " in realm "
              + config.get(LdapConfigKeys.REALM_NAME));
    }
    if (ldapReaderStore.getUser(userId) == null) {
      throw new UserNotFoundException(
          "Cannot find user with id "
              + userId
              + " in realm "
              + config.get(LdapConfigKeys.REALM_NAME));
    }
    try {
      ModifyRequest mr =
          new ModifyRequest(
              getGroupDN(appName, groupName),
              new Modification(ModificationType.ADD, "uniqueMember", getUserDN(userId)));
      ldapPoolConnection.modify(mr);
      ProviderResponse response = new ProviderResponse();
      response.setStatus(ProviderResponseStatus.OK);
      response.setEntityId(userId);
      return response;
    } catch (LDAPException e) {
      if (!e.getResultCode().equals(ResultCode.ATTRIBUTE_OR_VALUE_EXISTS)) {
        throw new RuntimeException("Failed to add user to group " + groupName, e);
      }
      throw new RuntimeException(e.getMessage());
    }
  }

  @Override
  public ProviderResponse reinitPassword(
      String userId,
      String generatedPassword,
      PasswordChangeRequest pcr,
      List<SendMode> sendMode,
      ProviderRequest providerRequest) {

    Modification mod =
        new Modification(ModificationType.REPLACE, "userPassword", generatedPassword);
    User user = ldapReaderStore.getUser(userId);

    if (user == null) {
      throw new UserNotFoundException(
          "User " + userId + " not found in realm" + config.get(LdapConfigKeys.REALM_NAME));
    }

    try {
      ldapPoolConnection.modify(
          "uid=" + user.getUsername() + "," + config.get(LdapConfigKeys.USER_SOURCE), mod);
      changePasswordResetStatus(
          userId,
          (pcr.getProperties() != null
                  && pcr.getProperties().get(EventKeysConfig.MUST_CHANGE_PASSWORD) != null)
              ? Boolean.parseBoolean(pcr.getProperties().get(EventKeysConfig.MUST_CHANGE_PASSWORD))
              : false,
          providerRequest);
      ProviderResponse response = new ProviderResponse();
      response.setStatus(ProviderResponseStatus.OK);
      response.setEntityId(user.getUsername());
      return response;
    } catch (LDAPException e) {
      throw new RuntimeException("Failed to reinit password for user " + user.getUsername(), e);
    }
  }

  @Override
  public ProviderResponse initPassword(
      String userId,
      String password,
      PasswordChangeRequest pcr,
      List<SendMode> sendMode,
      ProviderRequest providerRequest) {
    Modification mod = new Modification(ModificationType.REPLACE, "userPassword", password);
    User user = ldapReaderStore.getUser(userId);
    if (user == null) {
      throw new UserNotFoundException(
          "User " + userId + " not found in realm" + config.get(LdapConfigKeys.REALM_NAME));
    }

    try {

      ldapPoolConnection.modify(
          "uid=" + user.getUsername() + "," + config.get(LdapConfigKeys.USER_SOURCE), mod);
      ProviderResponse response = new ProviderResponse();
      response.setStatus(ProviderResponseStatus.OK);
      response.setEntityId(user.getUsername());
      return response;
    } catch (LDAPException e) {
      throw new RuntimeException("Failed to init password for user " + user.getUsername(), e);
    }
  }

  @Override
  public ProviderResponse changePassword(
      String userId,
      String oldPassword,
      String newPassword,
      PasswordChangeRequest pcr,
      ProviderRequest providerRequest) {
    User user = ldapReaderStore.getUser(userId);

    if (user == null) {
      throw new UserNotFoundException(
          "User " + userId + " not found in realm" + config.get(LdapConfigKeys.REALM_NAME));
    }
    try {
      PasswordModifyExtendedRequest pmer =
          new PasswordModifyExtendedRequest(
              "uid=" + user.getUsername() + "," + config.get(LdapConfigKeys.USER_SOURCE),
              oldPassword,
              newPassword);
      ExtendedResult result = ldapPoolConnection.processExtendedOperation(pmer);

      if (result.getResultCode().intValue() == ResultCode.INVALID_CREDENTIALS_INT_VALUE) {
        throw new InvalidPasswordException("Old password is not correct");
      } else if (result.getResultCode().intValue() != 0) {
        throw new RuntimeException("Unexpected error when changing password");
      }
    } catch (NumberFormatException | LDAPException e) {
      e.printStackTrace();
    }
    ProviderResponse response = new ProviderResponse();
    response.setStatus(ProviderResponseStatus.OK);
    response.setEntityId(user.getUsername());
    return response;
  }

  private ProviderResponse changePasswordResetStatus(
      String userId, boolean isReset, ProviderRequest providerRequest) {
    Modification mod =
        new Modification(
            ModificationType.REPLACE, "pwdReset", Boolean.toString(isReset).toUpperCase());
    User user = ldapReaderStore.getUser(userId);

    if (user == null) {
      throw new UserNotFoundException(
          "User " + userId + " not found in realm" + config.get(LdapConfigKeys.REALM_NAME));
    }

    try {
      ldapPoolConnection.modify(
          "uid=" + user.getUsername() + "," + config.get(LdapConfigKeys.USER_SOURCE), mod);
      ProviderResponse response = new ProviderResponse();
      response.setStatus(ProviderResponseStatus.OK);
      response.setEntityId(user.getUsername());
      return response;
    } catch (LDAPException e) {
      throw new RuntimeException(e.getMessage());
    }
  }

  /** Create a ldap ressource application and all the depending groups */
  @Override
  public ProviderResponse createApplication(
      Application application, ProviderRequest providerRequest) {
    if (ldapReaderStore.getApplication(application.getName()) == null) {
      try {
        AddRequest ar =
            new AddRequest(
                getApplicationDN(application.getName()),
                applicationLdapMapper.mapToAttributes(application));
        ldapPoolConnection.add(ar);
        AddRequest groupsAR =
            new AddRequest(
                getGroupSource(application.getName()),
                new Attribute("objectClass", "top", "organizationalUnit"));
        ldapPoolConnection.add(groupsAR);
        application.getGroups().stream()
            .forEach(group -> createGroup(application.getName(), group, providerRequest));
      } catch (LDAPException e) {
        throw new RuntimeException("Failed to create application" + application.getName(), e);
      }
      ProviderResponse response = new ProviderResponse();
      response.setStatus(ProviderResponseStatus.OK);
      response.setEntityId(application.getName());
      return response;
    }
    throw new ApplicationAlreadyExistException(
        "Application "
            + application.getName()
            + " already exist in realm "
            + config.get(LdapConfigKeys.REALM_NAME));
  }

  @Override
  public ProviderResponse updateApplication(
      Application updatedApplication, ProviderRequest providerRequest) {
    if (ldapReaderStore.getApplication(updatedApplication.getName()) != null) {
      try {
        ModifyRequest mr =
            new ModifyRequest(
                getApplicationDN(updatedApplication.getName()),
                applicationLdapMapper.createMods(updatedApplication));
        ldapPoolConnection.modify(mr);
        List<Group> alreadyExistingGroups =
            ldapReaderStore.getApplication(updatedApplication.getName()).getGroups();
        for (Group existingGroup : alreadyExistingGroups) {
          Optional<Group> optionalGroup =
              updatedApplication.getGroups().stream()
                  .filter(group -> group.getName().equalsIgnoreCase(existingGroup.getName()))
                  .findFirst();
          if (optionalGroup.isPresent()) {
            updateGroup(updatedApplication.getName(), optionalGroup.get(), providerRequest);
          } else {
            deleteGroup(updatedApplication.getName(), existingGroup.getName(), providerRequest);
          }
        }
        for (Group updatedGroup : updatedApplication.getGroups()) {
          if (alreadyExistingGroups.stream()
              .allMatch(group -> !group.getName().equalsIgnoreCase(updatedGroup.getName())))
            createGroup(updatedApplication.getName(), updatedGroup, providerRequest);
        }

      } catch (LDAPException e) {
        throw new RuntimeException(
            "Failed to update application "
                + updatedApplication.getName()
                + "while writing to LDAP",
            e);
      }
      ProviderResponse response = new ProviderResponse();
      response.setStatus(ProviderResponseStatus.OK);
      response.setEntityId(updatedApplication.getName());
      return response;
    }
    throw new ApplicationNotFoundException(
        "Application "
            + updatedApplication.getName()
            + " doesn't exist in realm "
            + config.get(LdapConfigKeys.REALM_NAME));
  }

  /** Delete application branch */
  @Override
  public ProviderResponse deleteApplication(
      String applicationName, ProviderRequest providerRequest) {
    if (ldapReaderStore.getApplication(applicationName) != null) {
      try {
        // DeleteRequest dr = new DeleteRequest(getApplicationDN(applicationName));
        // ldapPoolConnection.delete(dr);
        (new SubtreeDeleter()).delete(ldapPoolConnection, getApplicationDN(applicationName));
      } catch (LDAPException e) {
        throw new RuntimeException("Failed to delete application " + applicationName, e);
      }
      ProviderResponse response = new ProviderResponse();
      response.setStatus(ProviderResponseStatus.OK);
      response.setEntityId(applicationName);
      return response;
    }
    throw new ApplicationNotFoundException(
        "Application "
            + applicationName
            + " doesn't exist in realm "
            + config.get(LdapConfigKeys.REALM_NAME));
  }

  @Override
  public ProviderResponse addAppManagedAttribute(
      String userId, String attributeKey, String attributeValue, ProviderRequest providerRequest) {
    if (ldapReaderStore.getUser(userId) != null) {
      try {
        ModifyRequest modifyAttributeRequest =
            new ModifyRequest(
                getUserDN(userId),
                new Modification(ModificationType.ADD, attributeKey, attributeValue));
        ldapPoolConnection.modify(modifyAttributeRequest);
        ProviderResponse response = new ProviderResponse();
        response.setStatus(ProviderResponseStatus.OK);
        response.setEntityId(userId);
        return response;
      } catch (LDAPException e) {
        throw new RuntimeException(
            "Failed to update user attribute "
                + attributeKey
                + " with value "
                + attributeValue
                + " while writing to LDAP",
            e);
      }
    }
    throw new UserNotFoundException(
        "Cannot find user "
            + userId
            + " in realm "
            + config.get(LdapConfigKeys.REALM_NAME)
            + " and userstorage "
            + config.get(LdapConfigKeys.USERSTORAGE_NAME));
  }

  @Override
  public ProviderResponse deleteAppManagedAttribute(
      String userId, String attributeKey, String attributeValue, ProviderRequest providerRequest) {
    if (ldapReaderStore.getUser(userId) != null) {

      try {
        ModifyRequest modifyAttributeRequest =
            new ModifyRequest(
                getUserDN(userId),
                new Modification(ModificationType.DELETE, attributeKey, attributeValue));
        ldapPoolConnection.modify(modifyAttributeRequest);
        ProviderResponse response = new ProviderResponse();
        response.setStatus(ProviderResponseStatus.OK);
        response.setEntityId(userId);
        return response;
      } catch (LDAPException e) {
        if (e.getResultCode().equals(ResultCode.NO_SUCH_ATTRIBUTE)) {
          throw new AppManagedAttributeException("Cannot delete, attribute not found", e);
        }
        throw new RuntimeException(
            "Failed to update user attribute "
                + attributeKey
                + " with value "
                + attributeValue
                + " while writing to LDAP",
            e);
      }
    }
    throw new UserNotFoundException(
        "Cannot find user "
            + userId
            + " in realm "
            + config.get(LdapConfigKeys.REALM_NAME)
            + " and userstorage "
            + config.get(LdapConfigKeys.USERSTORAGE_NAME));
  }

  /**
   * create an ldap object address with a random id
   *
   * @param address
   * @return chosen id
   * @throws LDAPException
   */
  private UUID createAddress(Map<String, String> address) throws LDAPException {
    UUID addressUUID = UUID.randomUUID();
    AddRequest addressAddRequest =
        new AddRequest(
            getAddressDN(addressUUID.toString()), addressLdapMapper.mapToAttributes(address));
    ldapPoolConnection.add(addressAddRequest);
    return addressUUID;
  }

  private void updateAddress(String id, Map<String, String> newAddress) throws LDAPException {
    ModifyRequest modifyRequest =
        new ModifyRequest(getAddressDN(id), addressLdapMapper.createMods(newAddress));
    ldapPoolConnection.modify(modifyRequest);
  }

  private void deleteAddress(String id) throws LDAPException {
    DeleteRequest deleteRequest = new DeleteRequest(getAddressDN(id));
    ldapPoolConnection.delete(deleteRequest);
  }
}
