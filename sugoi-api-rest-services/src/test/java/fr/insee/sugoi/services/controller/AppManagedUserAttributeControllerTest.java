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
package fr.insee.sugoi.services.controller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.insee.sugoi.commons.services.controller.technics.SugoiAdviceController;
import fr.insee.sugoi.core.configuration.GlobalKeysConfig;
import fr.insee.sugoi.core.model.ProviderResponse;
import fr.insee.sugoi.core.model.ProviderResponse.ProviderResponseStatus;
import fr.insee.sugoi.core.realm.RealmProvider;
import fr.insee.sugoi.core.service.PermissionService;
import fr.insee.sugoi.core.service.UserService;
import fr.insee.sugoi.model.Realm;
import fr.insee.sugoi.model.User;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@SpringBootTest(
    classes = {AppManagedUserAttributeController.class, SugoiAdviceController.class},
    properties = "spring.config.location=classpath:/permissions/test-regexp-permissions.properties")
@AutoConfigureMockMvc
@EnableWebMvc
public class AppManagedUserAttributeControllerTest {
  @Autowired MockMvc mockMvc;

  @MockBean private UserService userService;

  @MockBean private PermissionService permissionService;

  @MockBean private RealmProvider realmProvider;

  ObjectMapper objectMapper = new ObjectMapper();
  Realm realm;
  User user;

  @BeforeEach
  public void setup() {
    realm = new Realm();
    realm.setName("test");
    realm.addProperty(GlobalKeysConfig.APP_MANAGED_ATTRIBUTE_PATTERNS_LIST, "(.*)_$(application)");
    realm.addProperty(GlobalKeysConfig.APP_MANAGED_ATTRIBUTE_KEYS_LIST, "my-attribute-key");
  }

  @Test
  @WithMockUser(username = "reader_realm1", roles = "ADMIN_SUGOI")
  public void get200WhenAddCorrectAttributes() {
    try {

      Mockito.when(realmProvider.load(Mockito.anyString())).thenReturn(Optional.of(realm));
      Mockito.when(permissionService.getUserRealmAppManager(Mockito.any()))
          .thenReturn(List.of("*\\appA", "*\\appB"));
      Mockito.doReturn(true)
          .when(permissionService)
          .isWriter(Mockito.any(), Mockito.anyString(), Mockito.anyString());
      Mockito.doReturn(new ProviderResponse("", "requestId", ProviderResponseStatus.OK, null, null))
          .when(userService)
          .addAppManagedAttribute(
              Mockito.any(),
              Mockito.any(),
              Mockito.any(),
              Mockito.any(),
              Mockito.any(),
              Mockito.any());
      RequestBuilder requestBuilder =
          MockMvcRequestBuilders.patch(
                  "/realms/domaine1/storages/test/users/Toto/my-attribute-key/prop_role_appA")
              .contentType(MediaType.APPLICATION_JSON)
              .accept(MediaType.APPLICATION_JSON)
              .with(csrf());
      MockHttpServletResponse response = mockMvc.perform(requestBuilder).andReturn().getResponse();

      assertThat("Response must be 200 OK", response.getStatus(), is(204));

    } catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }

  @Test
  @WithMockUser
  public void get200WhenAddCorrectAttributesWhenAdminOrWriter() {
    try {

      Mockito.when(realmProvider.load(Mockito.anyString())).thenReturn(Optional.of(realm));
      Mockito.doReturn(true)
          .when(permissionService)
          .isWriter(Mockito.any(), Mockito.anyString(), Mockito.anyString());
      Mockito.doReturn(new ProviderResponse("", "requestId", ProviderResponseStatus.OK, null, null))
          .when(userService)
          .addAppManagedAttribute(
              Mockito.any(),
              Mockito.any(),
              Mockito.any(),
              Mockito.any(),
              Mockito.any(),
              Mockito.any());
      RequestBuilder requestBuilder =
          MockMvcRequestBuilders.patch(
                  "/realms/domaine1/storages/test/users/Toto/my-attribute-key/prop_role_appA")
              .contentType(MediaType.APPLICATION_JSON)
              .accept(MediaType.APPLICATION_JSON)
              .with(csrf());
      MockHttpServletResponse response = mockMvc.perform(requestBuilder).andReturn().getResponse();

      assertThat("Response must be 200 OK", response.getStatus(), is(204));

    } catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }

  @Test
  @WithMockUser(username = "reader_realm1", roles = "ASI_SUGOI")
  public void get200WhenAddCorrectAttributesWhenAppManager() {
    try {

      Mockito.when(
              permissionService.isWriter(Mockito.any(), Mockito.anyString(), Mockito.anyString()))
          .thenReturn(false);
      Mockito.doReturn(true)
          .when(permissionService)
          .isValidAttributeAccordingAttributePattern(
              Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
      Mockito.doReturn(new ProviderResponse("", "requestId", ProviderResponseStatus.OK, null, null))
          .when(userService)
          .addAppManagedAttribute(
              Mockito.any(),
              Mockito.any(),
              Mockito.any(),
              Mockito.any(),
              Mockito.any(),
              Mockito.any());
      Mockito.when(realmProvider.load(Mockito.anyString())).thenReturn(Optional.of(realm));
      Mockito.when(
              permissionService.getAllowedAttributePattern(
                  Mockito.any(), Mockito.anyString(), Mockito.any(), Mockito.anyString()))
          .thenReturn(List.of("(.*)_appA", "(.*)_appB"));
      RequestBuilder requestBuilder =
          MockMvcRequestBuilders.patch(
                  "/realms/domaine1/storages/test/users/Toto/my-attribute-key/prop_role_appA")
              .contentType(MediaType.APPLICATION_JSON)
              .accept(MediaType.APPLICATION_JSON)
              .with(csrf());
      MockHttpServletResponse response = mockMvc.perform(requestBuilder).andReturn().getResponse();

      assertThat("Response must be 200 OK", response.getStatus(), is(204));

    } catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }

  @Test
  @WithMockUser(username = "reader_realm1", roles = "ASI_SUGOI")
  public void get403WhenAddIncorrectAttributes() {
    try {

      Mockito.when(
              permissionService.isWriter(Mockito.any(), Mockito.anyString(), Mockito.anyString()))
          .thenReturn(false);
      Mockito.when(realmProvider.load(Mockito.anyString())).thenReturn(Optional.of(realm));
      Mockito.when(
              permissionService.getAllowedAttributePattern(
                  Mockito.any(), Mockito.anyString(), Mockito.any(), Mockito.anyString()))
          .thenReturn(List.of("(.*)_appA", "(.*)_appB"));
      RequestBuilder requestBuilder =
          MockMvcRequestBuilders.patch(
                  "/realms/domaine1/storages/test/users/Toto/my-attribute-key2/prop_role_appA")
              .contentType(MediaType.APPLICATION_JSON)
              .accept(MediaType.APPLICATION_JSON)
              .with(csrf());
      MockHttpServletResponse response = mockMvc.perform(requestBuilder).andReturn().getResponse();

      assertThat("Response must be 403", response.getStatus(), is(403));

    } catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }

  @Test
  @WithMockUser(username = "reader_realm1", roles = "ASI_SUGOI")
  public void get403WhenNoRightIncorrectAttributes() {
    try {

      Mockito.when(
              permissionService.isWriter(Mockito.any(), Mockito.anyString(), Mockito.anyString()))
          .thenReturn(false);
      Mockito.when(realmProvider.load(Mockito.anyString())).thenReturn(Optional.of(realm));
      Mockito.when(
              permissionService.getAllowedAttributePattern(
                  Mockito.any(), Mockito.anyString(), Mockito.any(), Mockito.anyString()))
          .thenReturn(List.of("(.*)_sugoi"));
      RequestBuilder requestBuilder =
          MockMvcRequestBuilders.patch(
                  "/realms/domaine1/storages/test/users/Toto/my-attribute-key/prop_role_appA")
              .contentType(MediaType.APPLICATION_JSON)
              .accept(MediaType.APPLICATION_JSON)
              .with(csrf());
      MockHttpServletResponse response = mockMvc.perform(requestBuilder).andReturn().getResponse();

      assertThat("Response must be 403", response.getStatus(), is(403));

    } catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }
}
