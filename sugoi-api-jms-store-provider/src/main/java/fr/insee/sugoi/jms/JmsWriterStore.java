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
package fr.insee.sugoi.jms;

import fr.insee.sugoi.core.model.ProviderRequest;
import fr.insee.sugoi.core.model.ProviderResponse;
import fr.insee.sugoi.core.model.ProviderResponse.ProviderResponseStatus;
import fr.insee.sugoi.core.store.WriterStore;
import fr.insee.sugoi.jms.model.BrokerResponse;
import fr.insee.sugoi.jms.utils.JmsAtttributes;
import fr.insee.sugoi.jms.utils.Method;
import fr.insee.sugoi.jms.writer.JmsWriter;
import fr.insee.sugoi.model.Application;
import fr.insee.sugoi.model.Group;
import fr.insee.sugoi.model.Organization;
import fr.insee.sugoi.model.Realm;
import fr.insee.sugoi.model.User;
import fr.insee.sugoi.model.UserStorage;
import fr.insee.sugoi.model.paging.PasswordChangeRequest;
import fr.insee.sugoi.model.paging.SendMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jms.JmsException;

public class JmsWriterStore implements WriterStore {

  private JmsWriter jmsWriter;

  private String queueRequestName;
  private String queueResponseName;
  private String queueUrgentRequestName;
  private String queueUrgentResponseName;

  private Realm realm;

  private UserStorage userStorage;

  public JmsWriterStore(
      JmsWriter jmsWriter,
      String queueRequestName,
      String queueResponseName,
      String queueUrgentRequestName,
      String queueUrgentResponseName,
      Realm realm,
      UserStorage userStorage) {
    this.realm = realm;
    this.userStorage = userStorage;
    this.jmsWriter = jmsWriter;
    this.queueRequestName = queueRequestName;
    this.queueResponseName = queueResponseName;
    this.queueUrgentRequestName = queueUrgentRequestName;
    this.queueUrgentResponseName = queueUrgentResponseName;
  }

  @Override
  public ProviderResponse deleteUser(String id, ProviderRequest providerRequest) {
    Map<String, Object> params = new HashMap<>();
    params.put(JmsAtttributes.USER_ID, id);
    params.put(JmsAtttributes.REALM, realm.getName());
    params.put(JmsAtttributes.USER_STORAGE, userStorage.getName());
    return CheckAndSend(Method.DELETE_USER, params, id, providerRequest);
  }

  @Override
  public ProviderResponse createUser(User user, ProviderRequest providerRequest) {
    Map<String, Object> params = new HashMap<>();
    params.put(JmsAtttributes.USER, user);
    params.put(JmsAtttributes.REALM, realm.getName());
    params.put(JmsAtttributes.USER_STORAGE, userStorage.getName());
    return CheckAndSend(Method.CREATE_USER, params, user.getUsername(), providerRequest);
  }

  @Override
  public ProviderResponse updateUser(User updatedUser, ProviderRequest providerRequest) {
    Map<String, Object> params = new HashMap<>();
    params.put(JmsAtttributes.USER, updatedUser);
    params.put(JmsAtttributes.REALM, realm.getName());
    params.put(JmsAtttributes.USER_STORAGE, userStorage.getName());
    return CheckAndSend(Method.UPDATE_USER, params, updatedUser.getUsername(), providerRequest);
  }

  @Override
  public ProviderResponse deleteGroup(
      String appName, String groupName, ProviderRequest providerRequest) {
    Map<String, Object> params = new HashMap<>();
    params.put(JmsAtttributes.APP_NAME, appName);
    params.put(JmsAtttributes.GROUP_NAME, groupName);
    params.put(JmsAtttributes.REALM, realm.getName());
    params.put(JmsAtttributes.USER_STORAGE, userStorage.getName());
    return CheckAndSend(Method.DELETE_GROUP, params, groupName, providerRequest);
  }

  @Override
  public ProviderResponse createGroup(
      String appName, Group group, ProviderRequest providerRequest) {
    Map<String, Object> params = new HashMap<>();
    params.put(JmsAtttributes.APP_NAME, appName);
    params.put(JmsAtttributes.GROUP, group);
    params.put(JmsAtttributes.REALM, realm.getName());
    params.put(JmsAtttributes.USER_STORAGE, userStorage.getName());
    return CheckAndSend(Method.CREATE_GROUP, params, group.getName(), providerRequest);
  }

  @Override
  public ProviderResponse updateGroup(
      String appName, Group updatedGroup, ProviderRequest providerRequest) {
    Map<String, Object> params = new HashMap<>();
    params.put(JmsAtttributes.APPLICATION, appName);
    params.put(JmsAtttributes.GROUP, updatedGroup);
    params.put(JmsAtttributes.REALM, realm.getName());
    params.put(JmsAtttributes.USER_STORAGE, userStorage.getName());
    return CheckAndSend(Method.UPDATE_GROUP, params, updatedGroup.getName(), providerRequest);
  }

  @Override
  public ProviderResponse deleteOrganization(String name, ProviderRequest providerRequest) {
    Map<String, Object> params = new HashMap<>();
    params.put(JmsAtttributes.ORGANIZATION_NAME, name);
    params.put(JmsAtttributes.REALM, realm.getName());
    params.put(JmsAtttributes.USER_STORAGE, userStorage.getName());
    return CheckAndSend(Method.DELETE_ORGANIZATION, params, name, providerRequest);
  }

  @Override
  public ProviderResponse createOrganization(
      Organization organization, ProviderRequest providerRequest) {
    Map<String, Object> params = new HashMap<>();
    params.put(JmsAtttributes.ORGANIZATION, organization);
    params.put(JmsAtttributes.REALM, realm.getName());
    params.put(JmsAtttributes.USER_STORAGE, userStorage.getName());
    return CheckAndSend(
        Method.CREATE_ORGANIZATION, params, organization.getIdentifiant(), providerRequest);
  }

  @Override
  public ProviderResponse updateOrganization(
      Organization updatedOrganization, ProviderRequest providerRequest) {
    Map<String, Object> params = new HashMap<>();
    params.put(JmsAtttributes.ORGANIZATION, updatedOrganization);
    params.put(JmsAtttributes.REALM, realm.getName());
    params.put(JmsAtttributes.USER_STORAGE, userStorage.getName());
    return CheckAndSend(
        Method.UPDATE_ORGANIZATION, params, updatedOrganization.getIdentifiant(), providerRequest);
  }

  @Override
  public ProviderResponse deleteUserFromGroup(
      String appName, String groupName, String userId, ProviderRequest providerRequest) {
    Map<String, Object> params = new HashMap<>();
    params.put(JmsAtttributes.APP_NAME, appName);
    params.put(JmsAtttributes.GROUP_NAME, groupName);
    params.put(JmsAtttributes.USER_ID, userId);
    params.put(JmsAtttributes.REALM, realm.getName());
    params.put(JmsAtttributes.USER_STORAGE, userStorage.getName());
    return CheckAndSend(Method.DELETE_USER_FROM_GROUP, params, groupName, providerRequest);
  }

  @Override
  public ProviderResponse addUserToGroup(
      String appName, String groupName, String userId, ProviderRequest providerRequest) {
    Map<String, Object> params = new HashMap<>();
    params.put(JmsAtttributes.APP_NAME, appName);
    params.put(JmsAtttributes.GROUP_NAME, groupName);
    params.put(JmsAtttributes.USER_ID, userId);
    params.put(JmsAtttributes.REALM, realm.getName());
    params.put(JmsAtttributes.USER_STORAGE, userStorage.getName());
    return CheckAndSend(Method.ADD_USER_TO_GROUP, params, groupName, providerRequest);
  }

  @Override
  public ProviderResponse reinitPassword(
      String userId,
      String password,
      PasswordChangeRequest pcr,
      List<SendMode> sendModes,
      ProviderRequest providerRequest) {
    Map<String, Object> params = new HashMap<>();
    params.put(JmsAtttributes.USER_ID, userId);
    params.put(JmsAtttributes.REALM, realm.getName());
    params.put(JmsAtttributes.USER_STORAGE, userStorage.getName());
    params.put(JmsAtttributes.PASSWORD, password);
    params.put(JmsAtttributes.PASSWORD_CHANGE_REQUEST, pcr);
    params.put(JmsAtttributes.SEND_MODE, sendModes);
    return CheckAndSend(Method.REINIT_PASSWORD, params, userId, providerRequest);
  }

  @Override
  public ProviderResponse initPassword(
      String userId,
      String password,
      PasswordChangeRequest pcr,
      List<SendMode> sendModes,
      ProviderRequest providerRequest) {
    Map<String, Object> params = new HashMap<>();
    params.put(JmsAtttributes.USER_ID, userId);
    params.put(JmsAtttributes.PASSWORD, password);
    params.put(JmsAtttributes.REALM, realm.getName());
    params.put(JmsAtttributes.USER_STORAGE, userStorage.getName());
    params.put(JmsAtttributes.PASSWORD_CHANGE_REQUEST, pcr);
    params.put(JmsAtttributes.SEND_MODE, sendModes);
    return CheckAndSend(Method.INIT_PASSWORD, params, userId, providerRequest);
  }

  @Override
  public ProviderResponse createApplication(
      Application application, ProviderRequest providerRequest) {
    Map<String, Object> params = new HashMap<>();
    params.put(JmsAtttributes.APPLICATION, application);
    params.put(JmsAtttributes.REALM, realm.getName());
    params.put(JmsAtttributes.USER_STORAGE, userStorage.getName());
    return CheckAndSend(Method.CREATE_APPLICATION, params, application.getName(), providerRequest);
  }

  @Override
  public ProviderResponse updateApplication(
      Application updatedApplication, ProviderRequest providerRequest) {
    Map<String, Object> params = new HashMap<>();
    params.put(JmsAtttributes.APPLICATION, updatedApplication);
    params.put(JmsAtttributes.REALM, realm.getName());
    params.put(JmsAtttributes.USER_STORAGE, userStorage.getName());
    return CheckAndSend(
        Method.UPDATE_APPLICATION, params, updatedApplication.getName(), providerRequest);
  }

  @Override
  public ProviderResponse deleteApplication(
      String applicationName, ProviderRequest providerRequest) {
    Map<String, Object> params = new HashMap<>();
    params.put(JmsAtttributes.APP_NAME, applicationName);
    params.put(JmsAtttributes.REALM, realm.getName());
    params.put(JmsAtttributes.USER_STORAGE, userStorage.getName());
    return CheckAndSend(Method.DELETE_APPLICATION, params, applicationName, providerRequest);
  }

  @Override
  public ProviderResponse changePassword(
      String userId,
      String oldPassword,
      String newPassword,
      PasswordChangeRequest pcr,
      ProviderRequest providerRequest) {
    Map<String, Object> params = new HashMap<>();
    params.put(JmsAtttributes.USER_ID, userId);
    params.put(JmsAtttributes.NEW_PASSWORD, newPassword);
    params.put(JmsAtttributes.OLD_PASSWORD, oldPassword);
    params.put(JmsAtttributes.REALM, realm.getName());
    params.put(JmsAtttributes.USER_STORAGE, userStorage.getName());
    params.put(JmsAtttributes.PASSWORD_CHANGE_REQUEST, pcr);
    return CheckAndSend(Method.CHANGE_PASSWORD, params, userId, providerRequest);
  }

  @Override
  public ProviderResponse addAppManagedAttribute(
      String userId, String attributeKey, String attribute, ProviderRequest providerRequest) {
    Map<String, Object> params = new HashMap<>();
    params.put(JmsAtttributes.USER_ID, userId);
    params.put(JmsAtttributes.ATTRIBUTE_KEY, attributeKey);
    params.put(JmsAtttributes.ATTRIBUTE_VALUE, attribute);
    return CheckAndSend(Method.ADD_APP_MANAGED_ATTRIBUTE, params, userId, providerRequest);
  }

  @Override
  public ProviderResponse deleteAppManagedAttribute(
      String userId, String attributeKey, String attribute, ProviderRequest providerRequest) {
    Map<String, Object> params = new HashMap<>();
    params.put(JmsAtttributes.USER_ID, userId);
    params.put(JmsAtttributes.ATTRIBUTE_KEY, attributeKey);
    params.put(JmsAtttributes.ATTRIBUTE_VALUE, attribute);
    params.put(JmsAtttributes.REALM, realm.getName());
    params.put(JmsAtttributes.USER_STORAGE, userStorage.getName());
    return CheckAndSend(Method.DELETE_APP_MANAGED_ATTRIBUTE, params, userId, providerRequest);
  }

  private ProviderResponse CheckAndSend(
      String method, Map<String, Object> params, String entityId, ProviderRequest providerRequest) {
    ProviderResponse response = new ProviderResponse();

    // Perhaps use 2 jms template, one with a "reasonable" timeout for synchronous
    // requeste,
    // one without any timout, for ckeing status
    // => the timeout is defined only at jmsTemplate level, not at the receive
    // method

    // Check status
    if (providerRequest.getTransactionId() != null
        && !providerRequest.getTransactionId().isEmpty()) {
      try {
        // We dont't care of the jmstemplate when checking for response we just care
        // about the queue name
        BrokerResponse br =
            jmsWriter.checkResponseInQueueSynchronous(
                providerRequest.isUrgent() ? queueUrgentResponseName : queueResponseName,
                providerRequest.getTransactionId());
        if (br == null) {
          response.setStatus(ProviderResponseStatus.PENDING);
          response.setRequestId(providerRequest.getTransactionId());
          return response;
        }
        response = br.getProviderResponse();
        if (response.getStatus() == ProviderResponseStatus.OK) {
          response.setStatus(ProviderResponseStatus.ACCEPTED);
        } else if (response.getStatus() == ProviderResponseStatus.KO) {
          throw response.getException();
        }
      } catch (JmsException e) {
        response.setStatus(ProviderResponseStatus.PENDING);
        response.setRequestId(providerRequest.getTransactionId());
      }
      return response;
    }

    // Send Request
    params.put(JmsAtttributes.PROVIDER_REQUEST, providerRequest);
    String correlationId =
        providerRequest.isAsynchronousAllowed()
            ? jmsWriter.writeRequestInQueueAsynchronous(
                providerRequest.isUrgent() ? queueUrgentRequestName : queueRequestName,
                method,
                params)
            : jmsWriter.writeRequestInQueueSynchronous(
                providerRequest.isUrgent() ? queueUrgentRequestName : queueRequestName,
                method,
                params);

    // IF asynchronous : status must be requested in another request
    if (providerRequest.isAsynchronousAllowed()) {
      response.setEntityId(entityId);
      response.setRequestId(correlationId);
      response.setStatus(ProviderResponseStatus.REQUESTED);
    }
    // IF synchronous
    else {
      try {
        BrokerResponse br =
            jmsWriter.checkResponseInQueueSynchronous(
                providerRequest.isUrgent() ? queueUrgentResponseName : queueResponseName,
                correlationId);
        // warn : br is null if no response in time

        if (br == null) {
          response.setStatus(ProviderResponseStatus.PENDING);
          response.setRequestId(providerRequest.getTransactionId());
          return response;
        }

        response = br.getProviderResponse();

        if (response.getStatus() == ProviderResponseStatus.OK) {
          response.setStatus(ProviderResponseStatus.ACCEPTED);
        } else if (response.getStatus() == ProviderResponseStatus.KO) {
          throw response.getException();
        }
      } catch (JmsException e) {
        response.setStatus(ProviderResponseStatus.REQUESTED);
        response.setRequestId(correlationId);
      }
    }

    return response;
  }
}
