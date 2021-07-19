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
import fr.insee.sugoi.core.exceptions.ApplicationAlreadyExistException;
import fr.insee.sugoi.core.exceptions.ApplicationNotFoundException;
import fr.insee.sugoi.core.model.ProviderResponse;
import fr.insee.sugoi.core.model.ProviderResponse.ProviderResponseStatus;
import fr.insee.sugoi.core.service.ApplicationService;
import fr.insee.sugoi.model.Application;
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
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@SpringBootTest(
    classes = {ApplicationController.class, SugoiAdviceController.class},
    properties = "spring.config.location=classpath:/controller/application.properties")
@AutoConfigureMockMvc
@EnableWebMvc
public class ApplicationControllerTest {

  @Autowired MockMvc mockMvc;

  @MockBean private ApplicationService applicationService;

  ObjectMapper objectMapper = new ObjectMapper();
  Application application1, application2, application1Updated;
  PageResult<Application> pageResult;

  @BeforeEach
  public void setup() {
    application1 = new Application();
    application1.setName("SuperAppli");
    application1.setOwner("Amoi");

    application2 = new Application();
    application2.setName("SuperAppli2");
    application2.setOwner("Amoi2");

    application1Updated = new Application();
    application1Updated.setName("SuperAppli");
    application1Updated.setOwner("NewOwner");

    List<Application> applications = new ArrayList<>();
    applications.add(application1);
    applications.add(application2);
    pageResult = new PageResult<Application>();
    pageResult.setResults(applications);
    pageResult.setSearchToken("mySearchToken");
  }

  // Test read requests on good query

  @Test
  @WithMockUser
  public void retrieveAllApplications() {
    try {

      Mockito.when(
              applicationService.findByProperties(
                  Mockito.anyString(), Mockito.any(), Mockito.any()))
          .thenReturn(pageResult);

      RequestBuilder requestBuilder =
          MockMvcRequestBuilders.get("/realms/domaine1/applications")
              .accept(MediaType.APPLICATION_JSON);
      MockHttpServletResponse response = mockMvc.perform(requestBuilder).andReturn().getResponse();
      TypeReference<PageResult<Application>> mapType =
          new TypeReference<PageResult<Application>>() {};
      PageResult<Application> appRes =
          objectMapper.readValue(response.getContentAsString(), mapType);

      assertThat(
          "First element should be SuperAppli",
          appRes.getResults().get(0).getName(),
          is("SuperAppli"));
      assertThat(
          "SuperAppli should have owner Amoi", appRes.getResults().get(0).getOwner(), is("Amoi"));
      assertThat(
          "Second element should be SuperAppli2",
          appRes.getResults().get(1).getName(),
          is("SuperAppli2"));
      assertThat(
          "SuperAppli2 should have owner Amoi2",
          appRes.getResults().get(1).getOwner(),
          is("Amoi2"));
      assertThat("Response code should be 200", response.getStatus(), is(200));

    } catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }

  @Disabled
  @Test
  @WithMockUser
  public void shouldRetrieveSomeApplications() {}

  @Test
  @WithMockUser
  public void shouldGetApplicationByID() {
    try {

      Mockito.when(applicationService.findById("domaine1", "SuperAppli"))
          .thenReturn(Optional.of(application1));

      RequestBuilder requestBuilder =
          MockMvcRequestBuilders.get("/realms/domaine1/applications/SuperAppli")
              .accept(MediaType.APPLICATION_JSON);

      MockHttpServletResponse response = mockMvc.perform(requestBuilder).andReturn().getResponse();
      Application res = objectMapper.readValue(response.getContentAsString(), Application.class);

      verify(applicationService).findById("domaine1", "SuperAppli");
      assertThat("Application returned should be SuperAppli", res.getName(), is("SuperAppli"));
      assertThat("Application returned should be owned by Amoi", res.getOwner(), is("Amoi"));

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

      RequestBuilder requestBuilder =
          MockMvcRequestBuilders.delete("/realms/domaine1/applications/supprimemoi")
              .accept(MediaType.APPLICATION_JSON)
              .with(csrf());
      Mockito.doReturn(new ProviderResponse("", "requestId", ProviderResponseStatus.OK, null, null))
          .when(applicationService)
          .delete(Mockito.any(), Mockito.any(), Mockito.any());
      mockMvc.perform(requestBuilder).andReturn();
      verify(applicationService).delete(Mockito.anyString(), Mockito.anyString(), Mockito.any());

    } catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }

  @Test
  @WithMockUser
  public void updateShouldCallUpdateServiceAndReturnNewApp() {
    try {

      Mockito.when(applicationService.findById("domaine1", "SuperAppli"))
          .thenReturn(Optional.of(application1Updated));
      Mockito.doReturn(
              new ProviderResponse(
                  "", "requestId", ProviderResponseStatus.OK, application1Updated, null))
          .when(applicationService)
          .update(Mockito.any(), Mockito.any(), Mockito.any());
      RequestBuilder requestBuilder =
          MockMvcRequestBuilders.put("/realms/domaine1/applications/SuperAppli")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(application1Updated))
              .accept(MediaType.APPLICATION_JSON)
              .with(csrf());

      MockHttpServletResponse response = mockMvc.perform(requestBuilder).andReturn().getResponse();

      verify(applicationService).update(Mockito.anyString(), Mockito.any(), Mockito.any());
      assertThat(
          "Should get updated application",
          objectMapper.readValue(response.getContentAsString(), Application.class).getOwner(),
          is("NewOwner"));

      assertThat(
          "Should get location",
          response.getHeader("Location"),
          is("http://localhost/realms/domaine1/applications/SuperAppli"));

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
              new ProviderResponse("", "requestId", ProviderResponseStatus.OK, application1, null))
          .when(applicationService)
          .create(Mockito.any(), Mockito.any(), Mockito.any());

      RequestBuilder requestBuilder =
          MockMvcRequestBuilders.post("/realms/domaine1/applications")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(application1))
              .accept(MediaType.APPLICATION_JSON)
              .with(csrf());

      MockHttpServletResponse response = mockMvc.perform(requestBuilder).andReturn().getResponse();
      verify(applicationService).create(Mockito.any(), Mockito.any(), Mockito.any());
      assertThat(
          "Should get new application",
          objectMapper.readValue(response.getContentAsString(), Application.class).getName(),
          is("SuperAppli"));

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
              applicationService.findByProperties(
                  Mockito.anyString(), Mockito.any(), Mockito.any()))
          .thenReturn(pageResult);
      RequestBuilder requestBuilder =
          MockMvcRequestBuilders.get("/realms/domaine1/applications?size=2")
              .accept(MediaType.APPLICATION_JSON);

      assertThat(
          "Location header gives next page",
          mockMvc.perform(requestBuilder).andReturn().getResponse().getHeader("Location"),
          is("http://localhost/realms/domaine1/applications?size=2&searchToken=mySearchToken"));

    } catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }

  @Test
  @WithMockUser
  public void getObjectLocationInApplicationCreationResponse() {
    try {

      Mockito.when(applicationService.create(Mockito.anyString(), Mockito.any(), Mockito.any()))
          .thenReturn(
              new ProviderResponse(
                  application1.getName(), null, ProviderResponseStatus.OK, application1, null));

      RequestBuilder requestBuilder =
          MockMvcRequestBuilders.post("/realms/domaine1/applications")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(application1))
              .accept(MediaType.APPLICATION_JSON)
              .with(csrf());

      assertThat(
          "Location header gives get uri",
          mockMvc.perform(requestBuilder).andReturn().getResponse().getHeader("Location"),
          is("http://localhost/realms/domaine1/applications/SuperAppli"));

    } catch (Exception e1) {
      e1.printStackTrace();
      fail();
    }
  }

  // Test response codes on error
  @Test
  public void get401OnCreateApplicationWhenNotAuhtenticated() {
    try {

      RequestBuilder requestBuilder =
          MockMvcRequestBuilders.post("/domaine1/applications")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(application1))
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
  public void get401OnDeleteApplicationWhenNotAuhtenticated() {
    try {

      RequestBuilder requestBuilder =
          MockMvcRequestBuilders.delete("/realms/domaine1/applications/supprimemoi")
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
  public void get401OnUpdateApplicationWhenNotAuhtenticated() {
    try {

      RequestBuilder requestBuilder =
          MockMvcRequestBuilders.put("/realms/domaine1/applications/SuperAppli")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(application1))
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
  public void get409WhenCreatingAlreadyExistingApplication() {
    try {

      Mockito.when(applicationService.create(Mockito.anyString(), Mockito.any(), Mockito.any()))
          .thenThrow(
              new ApplicationAlreadyExistException(
                  "Application "
                      + application1.getName()
                      + " already exist in realm "
                      + "domaine1"));

      RequestBuilder requestBuilder =
          MockMvcRequestBuilders.post("/realms/domaine1/applications")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(application1))
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
  public void get404WhenNoApplicationIsFoundWhenGetById() {
    try {

      Mockito.when(applicationService.findById("domaine1", "dontexist"))
          .thenReturn(Optional.empty());

      RequestBuilder requestBuilder =
          MockMvcRequestBuilders.get("/realms/domaine1/applications/dontexist")
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
  public void get400WhenNoApplicationIdDoesntMatchBody() {
    try {

      RequestBuilder requestBuilder =
          MockMvcRequestBuilders.put("/realms/domaine1/applications/dontexist")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(application1))
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
  public void get404WhenNoApplicationIsFoundWhenUpdate() {
    try {
      Mockito.doThrow(new ApplicationNotFoundException(""))
          .when(applicationService)
          .update(Mockito.anyString(), Mockito.any(), Mockito.any());
      RequestBuilder requestBuilder =
          MockMvcRequestBuilders.put("/realms/domaine1/applications/SuperAppli")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(application1))
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
  public void get404WhenNoApplicationIsFoundWhenDelete() {
    try {

      Mockito.doThrow(new ApplicationNotFoundException(""))
          .when(applicationService)
          .delete(Mockito.anyString(), Mockito.anyString(), Mockito.any());

      RequestBuilder requestBuilder =
          MockMvcRequestBuilders.delete("/realms/domaine1/applications/dontexist")
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
}
