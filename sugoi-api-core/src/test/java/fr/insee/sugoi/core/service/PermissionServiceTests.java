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
package fr.insee.sugoi.core.service;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import fr.insee.sugoi.core.configuration.GlobalKeysConfig;
import fr.insee.sugoi.core.model.SugoiUser;
import fr.insee.sugoi.core.realm.RealmProvider;
import fr.insee.sugoi.core.service.impl.PermissionServiceImpl;
import fr.insee.sugoi.model.Realm;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(
    classes = PermissionServiceImpl.class,
    properties = "spring.config.location=classpath:/permissions/test-regexp-permissions.properties")
public class PermissionServiceTests {

  @MockBean private RealmProvider realmProvider;

  @Autowired PermissionService permissions;

  private Realm realm;

  @BeforeEach
  public void setup() {
    realm = new Realm();
    realm.setName("test");
    realm.addProperty(GlobalKeysConfig.APP_MANAGED_ATTRIBUTE_PATTERNS_LIST, "(.*)_$(application)");
    realm.addProperty(GlobalKeysConfig.APP_MANAGED_ATTRIBUTE_KEYS_LIST, "my-attribute-key");
  }

  @Test
  public void testAdminRegexp() {
    SugoiUser sugoiUser = new SugoiUser("admin", List.of("role_Admin_Sugoi"));
    assertThat("User is admin", permissions.isAdmin(sugoiUser), is(true));
  }

  @Test
  public void testReaderRegexp() {
    SugoiUser sugoiUser = new SugoiUser("reader_realm1", List.of("role_Reader_realm1_sugoi"));

    assertThat("User can read realm1", permissions.isReader(sugoiUser, "realm1", ""), is(true));
    assertThat(
        "User cannot write realm1", permissions.isWriter(sugoiUser, "realm1", ""), is(false));
    assertThat("User cannot read realm2", permissions.isReader(sugoiUser, "realm2", ""), is(false));
  }

  @Test
  public void testWriterRegexp() {
    SugoiUser sugoiUser = new SugoiUser("writer_realm1", List.of("role_Writer_realm1_sugoi"));
    assertThat("User can read realm1", permissions.isReader(sugoiUser, "realm1", ""), is(true));
    assertThat("User can write realm1", permissions.isWriter(sugoiUser, "realm1", ""), is(true));
    assertThat("User cannot read realm2", permissions.isReader(sugoiUser, "realm2", ""), is(false));
  }

  @Test
  public void testUserStorageNull() {
    try {
      SugoiUser sugoiUser = new SugoiUser("writer_realm1", List.of("role_Writer_realm1_sugoi"));
      assertThat(
          "No nullPointerException", permissions.isReader(sugoiUser, "realm1", null), is(true));
    } catch (Exception e) {
      fail();
    }
  }

  @Test
  public void testAppManager() {
    try {
      SugoiUser sugoiUser =
          new SugoiUser("appmanager_realm1_appli1", List.of("role_Asi_realm1_appli1"));
      assertThat(
          "user is app manager for appli1 in realm1",
          permissions.isApplicationManager(sugoiUser, "realm1", null, "appli1"),
          is(true));
      assertThat(
          "user is reader in realm1", permissions.isReader(sugoiUser, "realm1", null), is(true));
    } catch (Exception e) {
      fail();
    }
  }

  @Test
  public void testAppManagerWithoutRealmInRoleName() {
    try {
      SugoiUser sugoiUser = new SugoiUser("appmanager_appli1", List.of("role_Asi_appli1"));
      assertThat(
          "user is app manager for appli1 in realm1",
          permissions.isApplicationManager(sugoiUser, "realm1", null, "appli1"),
          is(true));
      assertThat(
          "user is reader in realm1", permissions.isReader(sugoiUser, "realm1", null), is(true));
      assertThat(
          "user is app manager for appli1 in realm2",
          permissions.isApplicationManager(sugoiUser, "realm2", null, "appli1"),
          is(true));
      assertThat(
          "user is reader in realm2", permissions.isReader(sugoiUser, "realm2", null), is(true));

    } catch (Exception e) {
      fail();
    }
  }

  @Test
  public void testAdminWildcard() {
    SugoiUser sugoiUser = new SugoiUser("admin_wildcard", List.of("role_nimportequoi_admin"));
    try {
      assertThat("user is admin sugoi", permissions.isAdmin(sugoiUser), is(true));
    } catch (Exception e) {
      fail();
    }
  }

  @Test
  public void testgetAllowedAttributePattern() {
    try {
      Mockito.when(realmProvider.load(Mockito.any())).thenReturn(Optional.of(realm));
      SugoiUser sugoiUser =
          new SugoiUser("admin_wildcard", List.of("role_ASI_realm1_app1", "role_ASI_app2"));
      List<String> allowedAttributesPattern =
          permissions.getAllowedAttributePattern(
              sugoiUser,
              "toto",
              "tata",
              realm
                  .getProperties()
                  .get(GlobalKeysConfig.APP_MANAGED_ATTRIBUTE_KEYS_LIST)
                  .split(",")[0]);
      assertThat("allowed Attributes Pattern", allowedAttributesPattern.size(), is(2));
    } catch (Exception e) {
      fail();
    }
  }
}
