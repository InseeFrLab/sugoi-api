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
import java.util.HashMap;
import java.util.Map;

public class Organization implements Serializable {
  private String identifiant;
  private byte[] gpgkey;
  private Organization organization;

  private Map<String, String> address = new HashMap<>();
  private Map<String, Object> metadatas = new HashMap<>();
  private Map<String, Object> attributes = new HashMap<>();

  public byte[] getGpgkey() {
    return this.gpgkey;
  }

  public void setGpgkey(byte[] gpgkey) {
    this.gpgkey = gpgkey;
  }

  public Map<String, String> getAddress() {
    return this.address;
  }

  public void addAddress(String name, String value) {
    this.address.put(name, value);
  }

  public void setAddress(Map<String, String> address) {
    this.address = address;
  }

  public Map<String, Object> getMetadatas() {
    return this.metadatas;
  }

  public void addMetadatas(String name, Object value) {
    this.metadatas.put(name, value);
  }

  public Map<String, Object> getAttributes() {
    return this.attributes;
  }

  public void addAttributes(String name, Object value) {
    this.attributes.put(name, value);
  }

  public Organization getOrganization() {
    return this.organization;
  }

  public void setOrganization(Organization organization) {
    this.organization = organization;
  }

  public String getIdentifiant() {
    return this.identifiant;
  }

  public void setIdentifiant(String identifiant) {
    this.identifiant = identifiant;
  }

  public void setAttributes(Map<String, Object> attributes) {
    this.attributes = attributes;
  }

  public void setMetadatas(Map<String, Object> metadatas) {
    this.metadatas = metadatas;
  }
}
