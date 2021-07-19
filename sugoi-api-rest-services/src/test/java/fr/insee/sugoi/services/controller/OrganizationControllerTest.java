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
import fr.insee.sugoi.core.exceptions.OrganizationAlreadyExistException;
import fr.insee.sugoi.core.exceptions.OrganizationNotFoundException;
import fr.insee.sugoi.core.model.ProviderResponse;
import fr.insee.sugoi.core.model.ProviderResponse.ProviderResponseStatus;
import fr.insee.sugoi.core.service.OrganizationService;
import fr.insee.sugoi.model.Organization;
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
    classes = {OrganizationController.class, SugoiAdviceController.class},
    properties = "spring.config.location=classpath:/controller/application.properties")
@AutoConfigureMockMvc
@EnableWebMvc
public class OrganizationControllerTest {

  @Autowired MockMvc mockMvc;

  @MockBean private OrganizationService organizationService;

  ObjectMapper objectMapper = new ObjectMapper();
  Organization organization1, organization2, organization2Updated;
  PageResult<Organization> pageResult;

  @BeforeEach
  public void setup() {
    organization1 = new Organization();
    organization1.setIdentifiant("BigOrga");

    organization2 = new Organization();
    organization2.setIdentifiant("SimpleOrga");

    organization2Updated = new Organization();
    organization2Updated.setIdentifiant("SimpleOrga");
    organization2Updated.setOrganization(organization1);

    List<Organization> organizations = new ArrayList<>();
    organizations.add(organization1);
    organizations.add(organization2);
    pageResult = new PageResult<Organization>();
    pageResult.setResults(organizations);
    pageResult.setSearchToken("mySearchToken");
  }

  // Test read requests on good query

  @Test
  @WithMockUser
  public void retrieveAllOrganizations() {
    try {

      Mockito.when(
              organizationService.findByProperties(
                  Mockito.anyString(),
                  Mockito.isNull(),
                  Mockito.any(),
                  Mockito.any(),
                  Mockito.any()))
          .thenReturn(pageResult);

      RequestBuilder requestBuilder =
          MockMvcRequestBuilders.get("/realms/domaine1/organizations")
              .accept(MediaType.APPLICATION_JSON);
      MockHttpServletResponse response = mockMvc.perform(requestBuilder).andReturn().getResponse();
      TypeReference<PageResult<Organization>> mapType =
          new TypeReference<PageResult<Organization>>() {};
      PageResult<Organization> appRes =
          objectMapper.readValue(response.getContentAsString(), mapType);

      assertThat(
          "First element should be BigOrga",
          appRes.getResults().get(0).getIdentifiant(),
          is("BigOrga"));
      assertThat(
          "Second element should be SimpleOrga",
          appRes.getResults().get(1).getIdentifiant(),
          is("SimpleOrga"));
      assertThat("Response code should be 200", response.getStatus(), is(200));

    } catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }

  @Disabled
  @Test
  @WithMockUser
  public void shouldRetrieveSomeOrganizations() {}

  @Test
  @WithMockUser
  public void shouldGetOrganizationByID() {
    try {

      Mockito.when(organizationService.findById("domaine1", null, "BigOrga"))
          .thenReturn(Optional.of(organization1));

      RequestBuilder requestBuilder =
          MockMvcRequestBuilders.get("/realms/domaine1/organizations/BigOrga")
              .accept(MediaType.APPLICATION_JSON);

      MockHttpServletResponse response = mockMvc.perform(requestBuilder).andReturn().getResponse();
      Organization res = objectMapper.readValue(response.getContentAsString(), Organization.class);

      verify(organizationService).findById("domaine1", null, "BigOrga");
      assertThat("Organization returned should be BigOrga", res.getIdentifiant(), is("BigOrga"));

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
          MockMvcRequestBuilders.delete("/realms/domaine1/storages/toto/organizations/supprimemoi")
              .accept(MediaType.APPLICATION_JSON)
              .with(csrf());

      mockMvc.perform(requestBuilder).andReturn();
      verify(organizationService)
          .delete(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

    } catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }

  @Test
  @WithMockUser
  public void updateShouldCallUpdateServiceAndReturnNewApp() {
    try {

      Mockito.when(organizationService.findById("domaine1", null, "SimpleOrga"))
          .thenReturn(Optional.of(organization2))
          .thenReturn(Optional.of(organization2Updated));
      Mockito.doReturn(
              new ProviderResponse(
                  "", "requestId", ProviderResponseStatus.OK, organization2Updated, null))
          .when(organizationService)
          .update(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any());
      RequestBuilder requestBuilder =
          MockMvcRequestBuilders.put("/realms/domaine1/organizations/SimpleOrga")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(organization2Updated))
              .accept(MediaType.APPLICATION_JSON)
              .with(csrf());

      MockHttpServletResponse response = mockMvc.perform(requestBuilder).andReturn().getResponse();

      verify(organizationService)
          .update(Mockito.anyString(), Mockito.isNull(), Mockito.any(), Mockito.any());
      assertThat(
          "Should get updated organization",
          objectMapper
              .readValue(response.getContentAsString(), Organization.class)
              .getOrganization()
              .getIdentifiant(),
          is("BigOrga"));

      assertThat(
          "Should get location",
          response.getHeader("Location"),
          is("http://localhost/realms/domaine1/organizations/SimpleOrga"));

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
              new ProviderResponse("", "requestId", ProviderResponseStatus.OK, organization1, null))
          .when(organizationService)
          .create(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any());
      RequestBuilder requestBuilder =
          MockMvcRequestBuilders.post(
                  "/realms/domaine1/storages/Profil_domaine1_WebServiceLdap/organizations")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(organization1))
              .accept(MediaType.APPLICATION_JSON)
              .with(csrf());

      MockHttpServletResponse response = mockMvc.perform(requestBuilder).andReturn().getResponse();
      verify(organizationService)
          .create(Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.any());
      System.out.println(response.getContentAsString());
      assertThat(
          "Should get new organization",
          objectMapper
              .readValue(response.getContentAsString(), Organization.class)
              .getIdentifiant(),
          is("BigOrga"));

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
              organizationService.findByProperties(
                  Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
          .thenReturn(pageResult);
      RequestBuilder requestBuilder =
          MockMvcRequestBuilders.get("/realms/domaine1/organizations?size=2")
              .accept(MediaType.APPLICATION_JSON);

      assertThat(
          "Location header gives next page",
          mockMvc.perform(requestBuilder).andReturn().getResponse().getHeader("Location"),
          is("http://localhost/realms/domaine1/organizations?size=2&searchToken=mySearchToken"));

    } catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }

  @Test
  @WithMockUser
  public void getObjectLocationInOrganizationCreationResponse() {
    try {

      Mockito.when(organizationService.findById(Mockito.any(), Mockito.any(), Mockito.any()))
          .thenThrow(OrganizationNotFoundException.class)
          .thenReturn(Optional.of(organization1));
      Mockito.when(
              organizationService.create(
                  Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any()))
          .thenReturn(
              new ProviderResponse(
                  organization1.getIdentifiant(), null, ProviderResponseStatus.OK, null, null));

      RequestBuilder requestBuilder =
          MockMvcRequestBuilders.post(
                  "/realms/domaine1/storages/Profil_domaine1_WebServiceLdap/organizations")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(organization1))
              .accept(MediaType.APPLICATION_JSON)
              .with(csrf());

      assertThat(
          "Location header gives get uri",
          mockMvc.perform(requestBuilder).andReturn().getResponse().getHeader("Location"),
          is(
              "http://localhost/realms/domaine1/storages/Profil_domaine1_WebServiceLdap/organizations/BigOrga"));

    } catch (Exception e1) {
      e1.printStackTrace();
      fail();
    }
  }

  // Test response codes on error
  @Test
  public void get401OnCreateOrganizationWhenNotAuhtenticated() {
    try {

      RequestBuilder requestBuilder =
          MockMvcRequestBuilders.post("/realms/domaine1/organizations")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(organization1))
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
  public void get401OnDeleteOrganizationWhenNotAuhtenticated() {
    try {

      RequestBuilder requestBuilder =
          MockMvcRequestBuilders.delete("/realms/domaine1/organizations/supprimemoi")
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
  public void get401OnUpdateOrganizationWhenNotAuhtenticated() {
    try {

      RequestBuilder requestBuilder =
          MockMvcRequestBuilders.put("/realms/domaine1/organizations/BigOrga")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(organization1))
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
  public void get409WhenCreatingAlreadyExistingOrganization() {
    try {

      Mockito.when(
              organizationService.create(
                  Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
          .thenThrow(new OrganizationAlreadyExistException(""));

      RequestBuilder requestBuilder =
          MockMvcRequestBuilders.post(
                  "/realms/domaine1/storages/Profil_domaine1_WebServiceLdap/organizations")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(organization1))
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
  public void get404WhenNoOrganizationIsFoundWhenGetById() {
    try {

      Mockito.when(organizationService.findById("domaine1", null, "dontexist"))
          .thenReturn(Optional.empty());

      RequestBuilder requestBuilder =
          MockMvcRequestBuilders.get("/realms/domaine1/organizations/dontexist")
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
  public void get400WhenNoOrganizationIdDoesntMatchBody() {
    try {

      RequestBuilder requestBuilder =
          MockMvcRequestBuilders.put("/realms/domaine1/organizations/dontexist")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(organization1))
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
  public void get404WhenNoOrganizationIsFoundWhenUpdate() {
    try {

      Mockito.doThrow(new OrganizationNotFoundException(""))
          .when(organizationService)
          .update(Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.any());
      RequestBuilder requestBuilder =
          MockMvcRequestBuilders.put("/realms/domaine1/organizations/BigOrga")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(organization1))
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
  public void get404WhenNoOrganizationIsFoundWhenDelete() {
    try {

      Mockito.doThrow(new OrganizationNotFoundException(""))
          .when(organizationService)
          .delete(Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.any());

      RequestBuilder requestBuilder =
          MockMvcRequestBuilders.delete("/realms/domaine1/organizations/dontexist")
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
