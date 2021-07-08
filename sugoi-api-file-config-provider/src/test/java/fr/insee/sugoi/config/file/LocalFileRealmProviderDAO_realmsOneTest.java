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

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

import fr.insee.sugoi.core.exceptions.RealmNotFoundException;
import fr.insee.sugoi.core.realm.RealmProvider;
import fr.insee.sugoi.model.Realm;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(classes = LocalFileRealmProviderDAO.class)
@TestPropertySource(
    properties = "fr.insee.sugoi.realm.config.local.path=classpath:/realms-one.json")
public class LocalFileRealmProviderDAO_realmsOneTest {

  @Autowired RealmProvider localFileConfig;

  @Test
  public void shouldBeLocalFileRealmProvider() {
    assertThat(
        "RealmProvider should be an instance of LocalFileRealmProviderDAO",
        localFileConfig,
        isA(LocalFileRealmProviderDAO.class));
  }

  @Test
  public void shouldHaveOnlyOneRealm() {
    assertThat("There should be only one realm", localFileConfig.findAll().size(), is(1));
  }

  @Test
  public void shouldFetchTestRealm() {
    assertThat(
        "We should have a realm test",
        localFileConfig
            .load("test")
            .orElseThrow(
                () -> new RealmNotFoundException("The realm " + "test" + " doesn't exist "))
            .getName(),
        is("test"));
    assertThat(
        "We should have a realm TeSt",
        localFileConfig
            .load("TeSt")
            .orElseThrow(
                () -> new RealmNotFoundException("The realm " + "test" + " doesn't exist "))
            .getName(),
        is("test"));
  }

  @Test
  public void shouldHaveTwoUserstorages() {
    Realm realm =
        localFileConfig
            .load("test")
            .orElseThrow(
                () -> new RealmNotFoundException("The realm " + "realm" + " doesn't exist "));
    assertThat(
        "We should have a userstorage default",
        realm.getUserStorages().stream()
            .anyMatch(userstorage -> userstorage.getName().equals("default")));
    assertThat(
        "We should have a userstorage other",
        realm.getUserStorages().stream()
            .anyMatch(userstorage -> userstorage.getName().equals("other")));
  }
}
