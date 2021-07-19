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
package fr.insee.sugoi.core.model;

import java.io.Serializable;

public class ProviderResponse implements Serializable {

  ProviderResponseStatus status;
  String requestId;
  String entityId;
  RuntimeException exception;
  Object entity;

  public ProviderResponse(
      String entityId,
      String requestId,
      ProviderResponseStatus status,
      Object entity,
      RuntimeException e) {
    this.entityId = entityId;
    this.requestId = requestId;
    this.status = status;
    this.exception = e;
    this.entity = entity;
  }

  public ProviderResponse() {}

  public enum ProviderResponseStatus {
    // Request is executed and result is effective at once
    OK,
    // Request has failed
    KO,
    // Request is registered but could eventually be refused or fail (Request have
    // been send in broker queue)
    REQUESTED,
    // When cannot determine the status of a request (User ask for an entity not in
    // the broker queue)
    PENDING,
    // Request is executed with success but could be effective later on (Response
    // have been take from the broker cue)
    ACCEPTED;
  }

  public ProviderResponseStatus getStatus() {
    return status;
  }

  public void setStatus(ProviderResponseStatus status) {
    this.status = status;
  }

  public String getEntityId() {
    return entityId;
  }

  public void setEntityId(String entityId) {
    this.entityId = entityId;
  }

  public String getRequestId() {
    return requestId;
  }

  public void setRequestId(String requestId) {
    this.requestId = requestId;
  }

  public RuntimeException getException() {
    return exception;
  }

  public void setException(RuntimeException exception) {
    this.exception = exception;
  }

  public Object getEntity() {
    return this.entity;
  }

  public void setEntity(Object entity) {
    this.entity = entity;
  }
}
