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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@JsonInclude(Include.NON_NULL)
public class User implements Serializable {

  private String lastName;

  private String firstName;
  private String mail;
  private String username;
  private byte[] certificate;
  private Organization organization;

  private List<Group> groups = new ArrayList<>();
  private List<Habilitation> habilitations = new ArrayList<>();

  private Map<String, String> address = new HashMap<>();
  private Map<String, Object> metadatas = new HashMap<>();
  private Map<String, Object> attributes = new HashMap<>();

  public User() {}

  public User(String username) {
    this.username = username;
  }

  public List<Habilitation> getHabilitations() {
    return this.habilitations;
  }

  public void addHabilitation(Habilitation habilitation) {
    this.habilitations.add(habilitation);
  }

  public void setHabilitations(List<Habilitation> habilitations) {
    this.habilitations = habilitations;
  }

  public String getLastName() {
    return this.lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public String getFirstName() {
    return this.firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public String getMail() {
    return this.mail;
  }

  public void setMail(String mail) {
    this.mail = mail;
  }

  public String getUsername() {
    return this.username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public byte[] getCertificate() {
    return this.certificate;
  }

  public void setCertificate(byte[] certificate) {
    this.certificate = certificate;
  }

  public List<Group> getGroups() {
    return this.groups;
  }

  public void setGroups(List<Group> groups) {
    this.groups = groups;
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

  public void setAttributes(Map<String, Object> attributes) {
    this.attributes = attributes;
  }

  public void setMetadatas(Map<String, Object> metadatas) {
    this.metadatas = metadatas;
  }

  public void removeFromHabilitations(String appName, String role, String property) {
    this.habilitations =
        this.habilitations.stream()
            .filter(
                habilitation ->
                    !(habilitation.getApplication().equals(appName)
                        && habilitation.getRole().equals(role)
                        && habilitation.getProperty().equals(property)))
            .collect(Collectors.toList());
  }

  public void removeFromHabilitations(String habilitationId) {
    this.habilitations =
        this.habilitations.stream()
            .filter(habilitation -> !(habilitation.getId().equals(habilitationId)))
            .collect(Collectors.toList());
  }
}
