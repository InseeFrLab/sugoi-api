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
package fr.insee.sugoi.model;

import java.io.Serializable;

public class Habilitation implements Serializable {
  private String application;
  private String role;
  private String property;

  public Habilitation() {}

  public Habilitation(String application, String role, String property) {
    this.application = application;
    this.role = role;
    this.property = property;
  }

  // Habilitation can be defined by a String of type [property_]role_application
  public Habilitation(String habilitationID) {
    String[] splitHabilitation = habilitationID.split("_");
    if (splitHabilitation.length == 2) {
      this.role = splitHabilitation[0];
      this.application = splitHabilitation[1];
    } else if (splitHabilitation.length == 3) {
      this.property = splitHabilitation[0];
      this.role = splitHabilitation[1];
      this.application = splitHabilitation[2];
    }
  }

  public String getApplication() {
    return this.application;
  }

  public void setApplication(String application) {
    this.application = application;
  }

  public String getRole() {
    return this.role;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public String getProperty() {
    return this.property;
  }

  public void setProperty(String property) {
    this.property = property;
  }

  public String getId() {
    return this.property != null
        ? this.property + "_" + this.role + "_" + this.application
        : this.role + "_" + this.application;
  }
}
