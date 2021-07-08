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
import java.util.List;
import java.util.Map;

public class Realm implements Serializable {
  private String name;
  private String url;
  private String appSource;
  private List<UserStorage> userStorages;
  private Map<String, String> properties = new HashMap<>();
  private String readerType;
  private String writerType;

  public String getName() {
    return this.name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<UserStorage> getUserStorages() {
    return this.userStorages;
  }

  public void setUserStorages(List<UserStorage> userStorages) {
    this.userStorages = userStorages;
  }

  public String getUrl() {
    return this.url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getAppSource() {
    return appSource;
  }

  public void setAppSource(String appSource) {
    this.appSource = appSource;
  }

  public void setProperties(Map<String, String> properties) {
    this.properties = properties;
  }

  public void addProperty(String name, String value) {
    this.properties.put(name, value);
  }

  public Map<String, String> getProperties() {
    return properties;
  }

  @Override
  public String toString() {
    return "Realm [appSource="
        + appSource
        + ", name="
        + name
        + ", url="
        + url
        + ", userStorages="
        + userStorages
        + "]";
  }

  public String getReaderType() {
    return readerType;
  }

  public void setReaderType(String readerType) {
    this.readerType = readerType;
  }

  public String getWriterType() {
    return writerType;
  }

  public void setWriterType(String writerType) {
    this.writerType = writerType;
  }
}
