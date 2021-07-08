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
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.insee.sugoi.commons.services.controller.technics.SugoiAdviceController;
import fr.insee.sugoi.core.exceptions.UserAlreadyExistException;
import fr.insee.sugoi.core.exceptions.UserNotFoundException;
import fr.insee.sugoi.core.model.ProviderResponse;
import fr.insee.sugoi.core.model.ProviderResponse.ProviderResponseStatus;
import fr.insee.sugoi.core.service.UserService;
import fr.insee.sugoi.model.User;
import fr.insee.sugoi.model.paging.PageResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@SpringBootTest(
    classes = {UserController.class, SugoiAdviceController.class},
    properties = "spring.config.location=classpath:/controller/application.properties")
@AutoConfigureMockMvc
@EnableWebMvc
public class UserControllerTest {

  @Autowired MockMvc mockMvc;

  @MockBean private UserService userService;

  ObjectMapper objectMapper = new ObjectMapper();
  User user1, user2, user1Updated;
  PageResult<User> pageResult;

  @BeforeEach
  public void setup() {
    user1 = new User();
    user1.setUsername("Toto");
    user1.setMail("toto@insee.fr");

    user2 = new User();
    user2.setUsername("Tata");
    user2.setMail("tata@insee.fr");

    user1Updated = new User();
    user1Updated.setUsername("Toto");
    user1Updated.setMail("new.toto@insee.fr");

    List<User> users = new ArrayList<>();
    users.add(user1);
    users.add(user2);
    pageResult = new PageResult<User>();
    pageResult.setResults(users);
    pageResult.setSearchToken("mySearchToken");
  }

  // Test read requests on good query

  @Test
  @WithMockUser
  public void retrieveAllUsers() {
    try {

      Mockito.when(
              userService.findByProperties(
                  Mockito.anyString(),
                  Mockito.isNull(),
                  Mockito.any(),
                  Mockito.any(),
                  Mockito.any()))
          .thenReturn(pageResult);

      RequestBuilder requestBuilder =
          MockMvcRequestBuilders.get("/realms/domaine1/users").accept(MediaType.APPLICATION_JSON);
      MockHttpServletResponse response = mockMvc.perform(requestBuilder).andReturn().getResponse();
      TypeReference<PageResult<User>> mapType = new TypeReference<PageResult<User>>() {};
      PageResult<User> appRes = objectMapper.readValue(response.getContentAsString(), mapType);

      assertThat(
          "First element should be Toto", appRes.getResults().get(0).getUsername(), is("Toto"));
      assertThat(
          "Toto should have mail toto@insee.fr",
          appRes.getResults().get(0).getMail(),
          is("toto@insee.fr"));
      assertThat(
          "Second element should be Tata", appRes.getResults().get(1).getUsername(), is("Tata"));
      assertThat(
          "Tata should have mail tata@insee.fr",
          appRes.getResults().get(1).getMail(),
          is("tata@insee.fr"));
      assertThat("Response code should be 200", response.getStatus(), is(200));

    } catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }

  @Disabled
  @Test
  @WithMockUser
  public void shouldRetrieveSomeUsers() {}

  @Test
  @WithMockUser
  public void shouldGetUserByID() {
    try {

      Mockito.when(userService.findById("domaine1", null, "Toto")).thenReturn(Optional.of(user1));

      RequestBuilder requestBuilder =
          MockMvcRequestBuilders.get("/realms/domaine1/users/Toto")
              .accept(MediaType.APPLICATION_JSON);

      MockHttpServletResponse response = mockMvc.perform(requestBuilder).andReturn().getResponse();
      User res = objectMapper.readValue(response.getContentAsString(), User.class);

      verify(userService).findById("domaine1", null, "Toto");
      assertThat("User returned should be Toto", res.getUsername(), is("Toto"));
      assertThat(
          "User returned should have toto@insee.fr as mail", res.getMail(), is("toto@insee.fr"));

    } catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }

  // Test write requests

  @Test
  @WithMockUser
  public void deleteShouldCallDeleteService() {
    try {

      Mockito.doReturn(new ProviderResponse("", "requestId", ProviderResponseStatus.OK, null, null))
          .when(userService)
          .delete(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

      Mockito.when(userService.findById(Mockito.anyString(), Mockito.isNull(), Mockito.anyString()))
          .thenReturn(Optional.of(user1))
          .thenReturn(Optional.of(user1));

      RequestBuilder requestBuilder =
          MockMvcRequestBuilders.delete("/realms/domaine1/storages/toto/users/supprimemoi")
              .accept(MediaType.APPLICATION_JSON)
              .with(csrf());

      mockMvc.perform(requestBuilder).andReturn();
      verify(userService).delete(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

    } catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }

  @Test
  @WithMockUser
  public void updateShouldCallUpdateServiceAndReturnNewApp() {
    try {

      Mockito.when(userService.findById(Mockito.anyString(), Mockito.any(), Mockito.any()))
          .thenReturn(Optional.of(user1))
          .thenReturn(Optional.of(user1Updated));

      Mockito.doReturn(
              new ProviderResponse("", "requestId", ProviderResponseStatus.OK, user1Updated, null))
          .when(userService)
          .update(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any());

      RequestBuilder requestBuilder =
          MockMvcRequestBuilders.put("/realms/domaine1/storages/domaine1/users/Toto")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(user1Updated))
              .accept(MediaType.APPLICATION_JSON)
              .with(csrf());

      MockHttpServletResponse response = mockMvc.perform(requestBuilder).andReturn().getResponse();

      verify(userService).update(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any());
      assertThat(
          "Should get updated user",
          objectMapper.readValue(response.getContentAsString(), User.class).getMail(),
          is("new.toto@insee.fr"));

      assertThat(
          "Should get location",
          response.getHeader("Location"),
          is("http://localhost/realms/domaine1/storages/domaine1/users/Toto"));

    } catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }

  @Test
  @WithMockUser
  public void postShouldCallPostServiceAndReturnNewApp() {

    try {
      Mockito.doReturn(
              new ProviderResponse("", "requestId", ProviderResponseStatus.OK, user1, null))
          .when(userService)
          .create(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any());
      RequestBuilder requestBuilder =
          MockMvcRequestBuilders.post(
                  "/realms/domaine1/storages/Profil_domaine1_WebServiceLdap/users")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(user1))
              .accept(MediaType.APPLICATION_JSON)
              .with(csrf());

      MockHttpServletResponse response = mockMvc.perform(requestBuilder).andReturn().getResponse();
      verify(userService)
          .create(Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.any());
      assertThat(
          "Should get new user",
          objectMapper.readValue(response.getContentAsString(), User.class).getUsername(),
          is("Toto"));

    } catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }

  // Test location headers
  @Test
  @WithMockUser
  public void getNextLocationInSearchResponse() {
    try {

      pageResult.setHasMoreResult(true);

      Mockito.when(
              userService.findByProperties(
                  Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
          .thenReturn(pageResult);
      RequestBuilder requestBuilder =
          MockMvcRequestBuilders.get("/realms/domaine1/users?size=2")
              .accept(MediaType.APPLICATION_JSON);

      assertThat(
          "Location header gives next page",
          mockMvc.perform(requestBuilder).andReturn().getResponse().getHeader("Location"),
          is("http://localhost/realms/domaine1/users?size=2&searchToken=mySearchToken"));

    } catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }

  @Test
  @WithMockUser
  public void getObjectLocationInUserCreationResponse() {
    try {

      Mockito.when(
              userService.create(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any()))
          .thenReturn(
              new ProviderResponse(
                  user1.getUsername(), null, ProviderResponseStatus.OK, null, null));

      RequestBuilder requestBuilder =
          MockMvcRequestBuilders.post(
                  "/realms/domaine1/storages/Profil_domaine1_WebServiceLdap/users")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(user1))
              .accept(MediaType.APPLICATION_JSON)
              .with(csrf());

      assertThat(
          "Location header gives get uri",
          mockMvc.perform(requestBuilder).andReturn().getResponse().getHeader("Location"),
          is(
              "http://localhost/realms/domaine1/storages/Profil_domaine1_WebServiceLdap/users/Toto"));

    } catch (Exception e1) {
      e1.printStackTrace();
      fail();
    }
  }

  // Test response codes on error
  @Test
  public void get401OnCreateUserWhenNotAuhtenticated() {
    try {

      RequestBuilder requestBuilder =
          MockMvcRequestBuilders.post(
                  "/realms/domaine1/storages/Profil_domaine1_WebServiceLdap/users")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(user1))
              .accept(MediaType.APPLICATION_JSON)
              .with(csrf());

      assertThat(
          "Should respond 401",
          mockMvc.perform(requestBuilder).andReturn().getResponse().getStatus(),
          is(401));

    } catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }

  @Test
  public void get401OnDeleteUserWhenNotAuhtenticated() {
    try {

      RequestBuilder requestBuilder =
          MockMvcRequestBuilders.delete("/realms/domaine1/users/supprimemoi")
              .accept(MediaType.APPLICATION_JSON)
              .with(csrf());

      assertThat(
          "Should respond 401",
          mockMvc.perform(requestBuilder).andReturn().getResponse().getStatus(),
          is(401));

    } catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }

  @Test
  public void get401OnUpdateUserWhenNotAuhtenticated() {
    try {

      RequestBuilder requestBuilder =
          MockMvcRequestBuilders.put("/realms/domaine1/users/Toto")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(user1))
              .accept(MediaType.APPLICATION_JSON)
              .with(csrf());

      assertThat(
          "Should respond 401",
          mockMvc.perform(requestBuilder).andReturn().getResponse().getStatus(),
          is(401));

    } catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }

  @Test
  @WithMockUser
  public void get409WhenCreatingAlreadyExistingUser() {
    try {

      Mockito.doThrow(new UserAlreadyExistException(""))
          .when(userService)
          .create(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any());

      RequestBuilder requestBuilder =
          MockMvcRequestBuilders.post(
                  "/realms/domaine1/storages/Profil_domaine1_WebServiceLdap/users")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(user1))
              .accept(MediaType.APPLICATION_JSON)
              .with(csrf());

      assertThat(
          "Should respond 409",
          mockMvc.perform(requestBuilder).andReturn().getResponse().getStatus(),
          is(409));

    } catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }

  @Test
  @WithMockUser
  public void get404WhenNoUserIsFoundWhenGetById() {
    try {

      Mockito.when(userService.findById(Mockito.anyString(), Mockito.isNull(), Mockito.anyString()))
          .thenReturn(Optional.empty());

      RequestBuilder requestBuilder =
          MockMvcRequestBuilders.get("/realms/domaine1/users/dontexist")
              .accept(MediaType.APPLICATION_JSON);

      assertThat(
          "Should respond 404",
          mockMvc.perform(requestBuilder).andReturn().getResponse().getStatus(),
          is(404));

    } catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }

  @Test
  @WithMockUser
  public void get400WhenNoUserIdDoesntMatchBody() {
    try {

      RequestBuilder requestBuilder =
          MockMvcRequestBuilders.put("/realms/domaine1/users/dontexist")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(user1))
              .accept(MediaType.APPLICATION_JSON)
              .with(csrf());
      assertThat(
          "Should respond 404",
          mockMvc.perform(requestBuilder).andReturn().getResponse().getStatus(),
          is(400));

    } catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }

  @Test
  @WithMockUser
  public void get404WhenNoUserIsFoundWhenUpdate() {
    try {

      Mockito.doThrow(new UserNotFoundException(""))
          .when(userService)
          .update(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any());
      RequestBuilder requestBuilder =
          MockMvcRequestBuilders.put("/realms/domaine1/users/Toto")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(user1))
              .accept(MediaType.APPLICATION_JSON)
              .with(csrf());

      assertThat(
          "Should respond 404",
          mockMvc.perform(requestBuilder).andReturn().getResponse().getStatus(),
          is(404));

    } catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }

  @Test
  @WithMockUser
  public void get404WhenNoUserIsFoundWhenDelete() {
    try {

      Mockito.doThrow(new UserNotFoundException(""))
          .when(userService)
          .update(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any());

      RequestBuilder requestBuilder =
          MockMvcRequestBuilders.delete("/realms/domaine1/users/dontexist")
              .accept(MediaType.APPLICATION_JSON)
              .with(csrf());

      assertThat(
          "Should respond 404",
          mockMvc.perform(requestBuilder).andReturn().getResponse().getStatus(),
          is(HttpStatus.NOT_FOUND.value()));

    } catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }
}
