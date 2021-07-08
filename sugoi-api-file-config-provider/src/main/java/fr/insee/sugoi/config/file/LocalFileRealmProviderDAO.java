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
package fr.insee.sugoi.config.file;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.insee.sugoi.core.model.ProviderRequest;
import fr.insee.sugoi.core.model.ProviderResponse;
import fr.insee.sugoi.core.model.ProviderResponse.ProviderResponseStatus;
import fr.insee.sugoi.core.realm.RealmProvider;
import fr.insee.sugoi.model.Realm;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
    value = "fr.insee.sugoi.realm.config.type",
    havingValue = "local",
    matchIfMissing = true)
public class LocalFileRealmProviderDAO implements RealmProvider {

  @Value("${fr.insee.sugoi.realm.config.local.path:classpath:/realms.json}")
  private String localFilePath;

  @Value("${fr.insee.sugoi.store.defaultReader:}")
  private String defaultReader;

  @Value("${fr.insee.sugoi.store.defaultWriter:}")
  private String defaultWriter;

  @Autowired ResourceLoader resourceLoader;

  private List<Realm> realms;
  private ObjectMapper mapper = new ObjectMapper();

  @Override
  public Optional<Realm> load(String realmName) {
    if (realms == null) {
      realms = findAll();
    }
    return realms.stream().filter(r -> r.getName().equalsIgnoreCase(realmName)).findFirst();
  }

  @Override
  public List<Realm> findAll() {
    try {
      Resource realmsResource = resourceLoader.getResource(localFilePath);
      if (!realmsResource.exists()) {
        throw new IllegalArgumentException("No resources found in " + localFilePath);
      }
      InputStream is = realmsResource.getInputStream();
      realms = mapper.readValue(is, new TypeReference<List<Realm>>() {});
      realms.stream()
          .forEach(
              realm -> {
                if (realm.getReaderType() == null) {
                  realm.setReaderType(defaultReader);
                }
                if (realm.getWriterType() == null) {
                  realm.setWriterType(defaultReader);
                }
              });
    } catch (Exception e) {
      throw new IllegalArgumentException("No resources found in " + localFilePath, e);
    }
    return realms;
  }

  @Override
  public ProviderResponse createRealm(Realm realm, ProviderRequest providerRequest) {
    realms.add(realm);
    overwriteConfig(realms);
    ProviderResponse response = new ProviderResponse();
    response.setStatus(ProviderResponseStatus.OK);
    response.setEntityId(realm.getName());
    return response;
  }

  @Override
  public ProviderResponse updateRealm(Realm realm, ProviderRequest providerRequest) {
    Realm realmToModify =
        realms.stream()
            .filter(r -> r.getName().equalsIgnoreCase(realm.getName()))
            .findFirst()
            .orElse(null);
    if (realmToModify != null) {
      realmToModify = realm;
    }
    ProviderResponse response = new ProviderResponse();
    response.setStatus(ProviderResponseStatus.OK);
    response.setEntityId(realm.getName());
    return response;
  }

  @Override
  public ProviderResponse deleteRealm(String realmName, ProviderRequest providerRequest) {
    overwriteConfig(
        realms.stream()
            .filter(realm -> !realm.getName().equalsIgnoreCase(realmName))
            .collect(Collectors.toList()));
    ProviderResponse response = new ProviderResponse();
    response.setStatus(ProviderResponseStatus.OK);
    response.setEntityId(realmName);
    return response;
  }

  private void overwriteConfig(List<Realm> realmsToWrite) {
    try {
      FileWriter fWriter = new FileWriter(resourceLoader.getResource(localFilePath).getFile());
      fWriter.write(mapper.writeValueAsString(realmsToWrite));
      fWriter.close();
      realms = findAll();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
