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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import fr.insee.sugoi.core.exceptions.RealmNotFoundException;
import fr.insee.sugoi.model.Realm;
import fr.insee.sugoi.model.UserStorage;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ldap.embedded.EmbeddedLdapAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(classes = {EmbeddedLdapAutoConfiguration.class, LdapRealmProviderDAOImpl.class})
@TestPropertySource(locations = "classpath:/application-write.properties")
public class LdapRealmProviderWriteTest {

  @Autowired LdapRealmProviderDAOImpl ldapRealmProviderDAOImpl;

  @Test
  public void addNewRealmWithOneUserStorageTest() {
    Realm realmToAdd = new Realm();
    realmToAdd.setName("toadd");
    realmToAdd.setUrl("localhost");
    UserStorage uniqueUserStorage = new UserStorage();
    uniqueUserStorage.setUserSource("ou=SSM,o=insee,c=fr");
    uniqueUserStorage.setOrganizationSource("ou=organisations,ou=clients_domaine2,o=insee,c=fr");
    uniqueUserStorage.addProperty("group_filter_pattern", "toto");
    realmToAdd.setUserStorages(List.of(uniqueUserStorage));
    assertThrows(
        RealmNotFoundException.class,
        () ->
            ldapRealmProviderDAOImpl
                .load("toadd")
                .orElseThrow(
                    () -> new RealmNotFoundException("The realm " + "test" + " doesn't exist ")),
        "Realm should not exist");

    ldapRealmProviderDAOImpl.createRealm(realmToAdd, null);
    Realm retrievedRealm =
        ldapRealmProviderDAOImpl
            .load("toadd")
            .orElseThrow(
                () -> new RealmNotFoundException("The realm " + "test" + " doesn't exist "));
    assertThat("Realm should be present", retrievedRealm.getName(), is("toadd"));
    assertThat("Realm should have an url", retrievedRealm.getUrl(), is("localhost"));
    assertThat(
        "Realm should have a usersource",
        retrievedRealm.getUserStorages().get(0).getUserSource(),
        is("ou=SSM,o=insee,c=fr"));
    assertThat(
        "Realm should have an organizationsource",
        retrievedRealm.getUserStorages().get(0).getOrganizationSource(),
        is("ou=organisations,ou=clients_domaine2,o=insee,c=fr"));
    assertThat(
        "Realm should have a groupfilterpattern",
        retrievedRealm.getUserStorages().get(0).getProperties().get("group_filter_pattern"),
        is("toto"));
  }

  @Test
  public void addNewRealmWithTwoUserStoragesTest() {
    Realm realmToAdd = new Realm();
    realmToAdd.setName("multistorage");
    realmToAdd.setUrl("localhost");
    UserStorage userStorage1 = new UserStorage();
    userStorage1.setName("first");
    userStorage1.setUserSource("ou=SSM,o=insee,c=fr");
    userStorage1.setOrganizationSource("ou=organisations,ou=clients_domaine2,o=insee,c=fr");
    userStorage1.addProperty("group_filter_pattern", "toto");
    UserStorage userStorage2 = new UserStorage();
    userStorage2.setName("second");
    userStorage2.setUserSource("ou=SSM,o=insee,c=fr");
    userStorage2.setOrganizationSource("ou=organisations,ou=clients_domaine2,o=insee,c=fr");
    userStorage2.addProperty("group_filter_pattern", "toto");
    List<UserStorage> userStorages = new ArrayList<>();
    userStorages.add(userStorage1);
    userStorages.add(userStorage2);
    realmToAdd.setUserStorages(userStorages);
    assertThrows(
        RealmNotFoundException.class,
        () ->
            ldapRealmProviderDAOImpl
                .load("multistorage")
                .orElseThrow(
                    () -> new RealmNotFoundException("The realm " + "test" + " doesn't exist ")),
        "Realm should not exist");
    ldapRealmProviderDAOImpl.createRealm(realmToAdd, null);
    Realm retrievedRealm =
        ldapRealmProviderDAOImpl
            .load("multistorage")
            .orElseThrow(
                () -> new RealmNotFoundException("The realm " + "test" + " doesn't exist "));
    assertThat("Realm should be present", retrievedRealm.getName(), is("multistorage"));
    assertThat(
        "Realm should have two userstorages", retrievedRealm.getUserStorages().size(), is(2));
    assertThat(
        "First realm has usersource",
        retrievedRealm.getUserStorages().get(0).getUserSource(),
        is("ou=SSM,o=insee,c=fr"));
    assertThat(
        "Realm should have a groupfilterpattern",
        retrievedRealm.getUserStorages().get(0).getProperties().get("group_filter_pattern"),
        is("toto"));
  }

  @Test
  public void deleteRealmWithOneStorageTest() {
    assertThat(
        "Realm should be present",
        ldapRealmProviderDAOImpl.load("todelete").get(),
        is(not(nullValue())));
    ldapRealmProviderDAOImpl.deleteRealm("todelete", null);
    assertThrows(
        RealmNotFoundException.class,
        () ->
            ldapRealmProviderDAOImpl
                .load("todelete")
                .orElseThrow(
                    () ->
                        new RealmNotFoundException("The realm " + "todelete" + " doesn't exist ")),
        "todelete should be deleted");
  }

  @Test
  public void deleteRealmWithMultipleStoragesTest() {
    assertThat(
        "Realm should be present",
        ldapRealmProviderDAOImpl.load("todeletemulti").get(),
        is(not(nullValue())));
    ldapRealmProviderDAOImpl.deleteRealm("todeletemulti", null);
    assertThrows(
        RealmNotFoundException.class,
        () ->
            ldapRealmProviderDAOImpl
                .load("todeletemulti")
                .orElseThrow(
                    () -> new RealmNotFoundException("The realm " + "test" + " doesn't exist ")),
        "todeletemulti should be deleted");
  }

  @Test
  public void changeRealmUserSourceTest() {
    Realm realmToModify =
        ldapRealmProviderDAOImpl
            .load("tomodify")
            .orElseThrow(
                () -> new RealmNotFoundException("The realm " + "test" + " doesn't exist "));
    UserStorage userStorage = realmToModify.getUserStorages().get(0);
    userStorage.setUserSource("ou=contacts,ou=clients_domaine2,o=insee,c=fr");
    ldapRealmProviderDAOImpl.updateRealm(realmToModify, null);
    assertThat(
        "User source should change to domaine2",
        ldapRealmProviderDAOImpl
            .load("tomodify")
            .orElseThrow(
                () -> new RealmNotFoundException("The realm " + "test" + " doesn't exist "))
            .getUserStorages()
            .get(0)
            .getUserSource(),
        is("ou=contacts,ou=clients_domaine2,o=insee,c=fr"));
  }

  @Test
  public void changeRealmUrlTest() {
    Realm realmToModify =
        ldapRealmProviderDAOImpl
            .load("tomodify")
            .orElseThrow(
                () -> new RealmNotFoundException("The realm " + "test" + " doesn't exist "));
    realmToModify.setUrl("new_url");
    ldapRealmProviderDAOImpl.updateRealm(realmToModify, null);
    assertThat(
        "Url should have changed",
        ldapRealmProviderDAOImpl
            .load("tomodify")
            .orElseThrow(
                () -> new RealmNotFoundException("The realm " + "test" + " doesn't exist "))
            .getUrl(),
        is("new_url"));
  }
}
