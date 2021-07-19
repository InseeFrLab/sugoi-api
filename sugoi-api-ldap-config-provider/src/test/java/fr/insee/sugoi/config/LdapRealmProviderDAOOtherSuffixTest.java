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
package fr.insee.sugoi.config;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

import fr.insee.sugoi.core.exceptions.RealmNotFoundException;
import fr.insee.sugoi.model.Realm;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ldap.embedded.EmbeddedLdapAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(classes = {EmbeddedLdapAutoConfiguration.class, LdapRealmProviderDAOImpl.class})
@TestPropertySource(locations = "classpath:/application-other-suffix.properties")
public class LdapRealmProviderDAOOtherSuffixTest {

  @Autowired LdapRealmProviderDAOImpl ldapRealmProviderDAOImpl;

  @Test
  public void ldapRealmDifferentNamePattern() {
    Realm realm =
        ldapRealmProviderDAOImpl
            .load("domainesugoi")
            .orElseThrow(
                () -> new RealmNotFoundException("The realm " + "test" + " doesn't exist "));
    assertThat(
        "Default userstorage should have usersource",
        realm.getUserStorages().get(0).getUserSource(),
        is("ou=contacts_sugoi,ou=clients_domaine1,o=insee,c=fr"));
  }
}
