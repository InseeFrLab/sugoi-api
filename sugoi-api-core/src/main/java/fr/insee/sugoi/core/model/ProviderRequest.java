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

public class ProviderRequest implements Serializable {

  private SugoiUser sugoiUser;
  private boolean asynchronousAllowed;
  private String transactionId;
  private boolean urgent;

  public ProviderRequest() {}

  public ProviderRequest(SugoiUser sugoiUser, boolean asynchronousAllowed, String transactionId) {
    this.sugoiUser = sugoiUser;
    this.asynchronousAllowed = asynchronousAllowed;
    this.transactionId = transactionId;
  }

  public ProviderRequest(
      SugoiUser sugoiUser, boolean asynchronousAllowed, String transactionId, boolean isUrgent) {
    this.sugoiUser = sugoiUser;
    this.asynchronousAllowed = asynchronousAllowed;
    this.transactionId = transactionId;
    this.urgent = isUrgent;
  }

  public SugoiUser getSugoiUser() {
    return this.sugoiUser;
  }

  public void setSugoiUser(SugoiUser sugoiUser) {
    this.sugoiUser = sugoiUser;
  }

  public boolean isAsynchronousAllowed() {
    return this.asynchronousAllowed;
  }

  public void setAsynchronousAllowed(boolean asynchronousAllowed) {
    this.asynchronousAllowed = asynchronousAllowed;
  }

  public String getTransactionId() {
    return this.transactionId;
  }

  public void setTransactionId(String transactionId) {
    this.transactionId = transactionId;
  }

  public boolean isUrgent() {
    return this.urgent;
  }

  public void setIsUrgent(boolean isUrgent) {
    this.urgent = isUrgent;
  }
}
