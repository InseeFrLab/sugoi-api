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
package fr.insee.sugoi.jms.model;

import java.util.HashMap;
import java.util.Map;

public class BrokerRequest {

  private String method;
  // maybe too generic, we always have a provider request, a realm , a userstorage
  private Map<String, Object> methodParams = new HashMap<>();
  private String correlationId;

  public BrokerRequest(String method, Map<String, Object> methodParams) {
    this.method = method;
    this.methodParams = methodParams;
  }

  public BrokerRequest() {}

  public String getMethod() {
    return this.method;
  }

  public void setMethod(String method) {
    this.method = method;
  }

  public Map<String, Object> getmethodParams() {
    return this.methodParams;
  }

  public void setmethodParams(String name, Object value) {
    this.methodParams.put(name, value);
  }

  public String getCorrelationId() {
    return correlationId;
  }

  public void setCorrelationId(String correlationId) {
    this.correlationId = correlationId;
  }
}
