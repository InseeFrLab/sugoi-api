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
package fr.insee.sugoi.old.services.controller;

import fr.insee.sugoi.converter.mapper.OuganextSugoiMapper;
import fr.insee.sugoi.converter.ouganext.Organisation;
import fr.insee.sugoi.converter.ouganext.Organisations;
import fr.insee.sugoi.core.exceptions.OrganizationNotFoundException;
import fr.insee.sugoi.core.model.ProviderRequest;
import fr.insee.sugoi.core.model.ProviderResponse;
import fr.insee.sugoi.core.model.SugoiUser;
import fr.insee.sugoi.core.service.OrganizationService;
import fr.insee.sugoi.model.Organization;
import fr.insee.sugoi.model.paging.PageResult;
import fr.insee.sugoi.model.paging.PageableResult;
import fr.insee.sugoi.model.paging.SearchType;
import fr.insee.sugoi.old.services.model.ConverterDomainRealm;
import fr.insee.sugoi.old.services.model.RealmStorage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.ws.rs.core.Link;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/v1")
@SecurityRequirement(name = "basic")
@Tag(
    name = "[Deprecated] - Manage organizations",
    description = "Old endpoints to manage organizations")
public class OrganisationDomaineController {

  private OuganextSugoiMapper ouganextSugoiMapper = new OuganextSugoiMapper();

  @Autowired private OrganizationService organizationService;

  @Autowired private ConverterDomainRealm converterDomainRealm;

  /**
   * Update or create an organisation.
   *
   * @param domaine realm of the request
   * @param id id of the organisation to update, will replace the id of the organization body
   *     parameter
   * @param creation if true a non existent organisation to update is created
   * @param organisation
   * @return OK with the updated or created organisation
   */
  @PreAuthorize("@OldAuthorizeMethodDecider.isAtLeastGestionnaire(#domaine)")
  @PutMapping(
      value = "/{domaine}/organisation/{id}",
      consumes = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE},
      produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
  @Operation(summary = "Update or create an organisation", deprecated = true)
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Organization successfully updated or created",
            content = {
              @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = Organisation.class)),
              @Content(
                  mediaType = "application/xml",
                  schema = @Schema(implementation = Organisation.class))
            }),
        @ApiResponse(
            responseCode = "404",
            description = "Organization to update not found",
            content = {
              @Content(mediaType = "application/json"),
              @Content(mediaType = "application/xml")
            })
      })
  public ResponseEntity<Organisation> createOrModifyOrganisation(
      @Parameter(
              description = "Name of the domaine where the operation will be made",
              required = true)
          @PathVariable(name = "domaine", required = true)
          String domaine,
      @Parameter(description = "Organization's id to modify", required = false)
          @PathVariable(name = "id", required = true)
          String id,
      @Parameter(
              description =
                  "Boolean indicates whether the organization must be created if not already exist",
              required = false)
          @RequestParam(name = "creation", required = false)
          boolean creation,
      @Parameter(description = "Organization to create or modify", required = true) @RequestBody
          Organisation organisation,
      Authentication authentication) {
    RealmStorage realmUserStorage = converterDomainRealm.getRealmForDomain(domaine);

    organisation.setIdentifiant(id);
    Organization sugoiOrganization =
        ouganextSugoiMapper.serializeToSugoi(organisation, Organization.class);

    if (creation) {
      ProviderResponse response =
          organizationService.create(
              realmUserStorage.getRealm(),
              realmUserStorage.getUserStorage(),
              sugoiOrganization,
              new ProviderRequest(
                  new SugoiUser(
                      authentication.getName(),
                      authentication.getAuthorities().stream()
                          .map(GrantedAuthority::getAuthority)
                          .map(String::toUpperCase)
                          .collect(Collectors.toList())),
                  false,
                  null,
                  false));
      return ResponseEntity.status(HttpStatus.OK)
          .body(
              ouganextSugoiMapper.serializeToOuganext(
                  organizationService.findById(
                      realmUserStorage.getRealm(),
                      realmUserStorage.getUserStorage(),
                      response.getEntityId()),
                  Organisation.class));
    }

    ProviderResponse response =
        organizationService.update(
            realmUserStorage.getRealm(),
            realmUserStorage.getUserStorage(),
            sugoiOrganization,
            new ProviderRequest(
                new SugoiUser(
                    authentication.getName(),
                    authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .map(String::toUpperCase)
                        .collect(Collectors.toList())),
                false,
                null,
                false));
    return ResponseEntity.status(HttpStatus.OK)
        .body(
            ouganextSugoiMapper.serializeToOuganext(
                organizationService
                    .findById(
                        realmUserStorage.getRealm(),
                        realmUserStorage.getUserStorage(),
                        response.getEntityId())
                    .get(),
                Organisation.class));
  }

  /**
   * Get an organization of the domaine by its identifiant
   *
   * @param domaine
   * @param id
   * @return Ok with the desired organization
   */
  @PreAuthorize("@OldAuthorizeMethodDecider.isAtLeastConsultant(#domaine)")
  @GetMapping(
      value = "/{domaine}/organisation/{id}",
      produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
  @Operation(summary = "Get an organization with its identifiant", deprecated = true)
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Organization found",
            content = {
              @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = Organisation.class)),
              @Content(
                  mediaType = "application/xml",
                  schema = @Schema(implementation = Organisation.class))
            })
      })
  public ResponseEntity<Organisation> getOrganisationDomaine(
      @Parameter(
              description = "Name of the domaine where the operation will be made",
              required = true)
          @PathVariable(name = "domaine", required = true)
          String domaine,
      @Parameter(description = "Organization's id to find", required = true)
          @PathVariable(name = "id", required = true)
          String id) {
    RealmStorage realmUserStorage = converterDomainRealm.getRealmForDomain(domaine);

    return ResponseEntity.status(HttpStatus.OK)
        .body(
            ouganextSugoiMapper.serializeToOuganext(
                organizationService
                    .findById(realmUserStorage.getRealm(), realmUserStorage.getUserStorage(), id)
                    .orElseThrow(
                        () ->
                            new OrganizationNotFoundException(
                                "Organization " + id + " not found in realm " + domaine)),
                Organisation.class));
  }

  /**
   * Delete an organization according the id
   *
   * @param domaine
   * @param id
   * @return NO_CONTENT and delete the organization
   */
  @PreAuthorize("@OldAuthorizeMethodDecider.isAtLeastGestionnaire(#domaine)")
  @DeleteMapping(
      value = "/{domaine}/organisation/{id}",
      produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
  @Operation(summary = "Delete an organization by its identifiant", deprecated = true)
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "204",
            description = "Organization deleted",
            content = {
              @Content(mediaType = "application/json"),
              @Content(mediaType = "application/xml")
            }),
        @ApiResponse(
            responseCode = "404",
            description = "Organization not found",
            content = {
              @Content(mediaType = "application/json"),
              @Content(mediaType = "application/xml")
            })
      })
  public ResponseEntity<?> deleteOrganisation(
      @Parameter(
              description = "Name of the domaine where the operation will be made",
              required = true)
          @PathVariable(name = "domaine", required = true)
          String domaine,
      @Parameter(description = "Organization's id to delete", required = true)
          @PathVariable(name = "id", required = true)
          String id,
      Authentication authentication) {
    RealmStorage realmUserStorage = converterDomainRealm.getRealmForDomain(domaine);

    organizationService.delete(
        realmUserStorage.getRealm(),
        realmUserStorage.getUserStorage(),
        id,
        new ProviderRequest(
            new SugoiUser(
                authentication.getName(),
                authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .map(String::toUpperCase)
                    .collect(Collectors.toList())),
            false,
            null,
            false));
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

  /**
   * Create an organisation. The organisation id is slug if filled and not already used. Otherwise
   * the id is generated.
   *
   * @param domaine
   * @param organisation
   * @param slug filled in header Slug, the id of the organisation if not already used
   * @return CREATED with the created organisation link as a link header and as a location header
   */
  @PreAuthorize("@OldAuthorizeMethodDecider.isAtLeastGestionnaire(#domaine)")
  @PostMapping(
      value = "{domaine}/organisations",
      consumes = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE},
      produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
  @Operation(
      summary =
          "Create an organisation. The organisation id is slug if filled and not already used. Otherwise the id is generated.",
      deprecated = true)
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "Organization created",
            content = {
              @Content(mediaType = "application/json"),
              @Content(mediaType = "application/xml")
            })
      })
  public ResponseEntity<?> createOrganisation(
      @Parameter(
              description = "Name of the domaine where the operation will be made",
              required = true)
          @PathVariable(name = "domaine", required = true)
          String domaine,
      @Parameter(description = "Organization to create", required = true) @RequestBody
          Organisation organisation,
      @Parameter(
              description = "filled in header Slug, the id of the organisation if not already used",
              required = false)
          @RequestHeader(value = "Slug", required = false)
          String slug,
      Authentication authentication) {
    RealmStorage realmUserStorage = converterDomainRealm.getRealmForDomain(domaine);

    Organization sugoiOrganization =
        ouganextSugoiMapper.serializeToSugoi(organisation, Organization.class);
    if (slug != null
        && organizationService
            .findById(realmUserStorage.getRealm(), realmUserStorage.getUserStorage(), slug)
            .isEmpty()) {
      sugoiOrganization.setIdentifiant(slug);
    } else if (sugoiOrganization.getIdentifiant() == null) {
      sugoiOrganization.setIdentifiant(UUID.randomUUID().toString());
    }
    organizationService.create(
        realmUserStorage.getRealm(),
        realmUserStorage.getUserStorage(),
        sugoiOrganization,
        new ProviderRequest(
            new SugoiUser(
                authentication.getName(),
                authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .map(String::toUpperCase)
                    .collect(Collectors.toList())),
            false,
            null,
            false));
    String request = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toString();
    String location =
        request.substring(0, request.lastIndexOf("/"))
            + "/organisation/"
            + sugoiOrganization.getIdentifiant();
    String resultLink =
        Link.fromUri(location)
            .rel("http://xml/insee.fr/schema/annuaire/Organisation")
            .build()
            .toString();
    return ResponseEntity.status(HttpStatus.CREATED)
        .header("Location", location)
        .header("Link", resultLink)
        .build();
  }

  /**
   * Search organizations matching criteria.
   *
   * @param domaine realm of the request
   * @param identifiant
   * @param nomCommun
   * @param description
   * @param organisationId
   * @param mail
   * @param size number of results to return
   * @param offset
   * @param searchCookie a cookie to browse several pages
   * @param typeRecherche "ET or "OU" to decide if all criteria must be respected or only one
   * @param resultatsDansBody if false, response is 204 and Http Response contains link headers to
   *     retrieve the resources
   * @param identifiantsSeuls if true, the contacts found will only have id value
   * @param certificat
   * @return OK and all the Organisation in the body if resultatsDansBody or NO_CONTENT and the Link
   *     to the results as a header
   */
  @PreAuthorize("@OldAuthorizeMethodDecider.isAtLeastConsultant(#domaine)")
  @GetMapping(
      value = "/{domaine}/organisations",
      produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
  @Operation(summary = "Search organizations matching criteria.", deprecated = true)
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Organization found",
            content = {
              @Content(mediaType = "application/json"),
              @Content(mediaType = "application/xml")
            })
      })
  public ResponseEntity<?> getOrganisations(
      @Parameter(
              description = "Name of the domaine where the operation will be made",
              required = true)
          @PathVariable(name = "domaine", required = true)
          String domaine,
      @Parameter(description = "Identifiant of the organization", required = false)
          @RequestParam(name = "identifiant", required = false)
          String identifiant,
      @Parameter(description = "NomCommun of the organization", required = false)
          @RequestParam(name = "nomCommun", required = false)
          String nomCommun,
      @Parameter(description = "Description of the organization", required = false)
          @RequestParam(name = "description", required = false)
          String description,
      @Parameter(
              description = "Organization's id where the organization belongs ",
              required = false)
          @RequestParam(name = "organisationId", required = false)
          String organisationId,
      @Parameter(description = "Mail of the organization", required = false)
          @RequestParam(name = "mail", required = false)
          String mail,
      @Parameter(description = "Number of results to return", required = false)
          @RequestParam(name = "size", defaultValue = "20")
          int size,
      @Parameter(description = "Offset to apply to the search", required = false)
          @RequestParam(name = "start", defaultValue = "0")
          int offset,
      @Parameter(description = "Cookie to continue a previous search", required = false)
          @RequestParam(name = "searchCookie", required = false)
          String searchCookie,
      @Parameter(description = "Kind of search can be Et or Ou", required = false)
          @RequestParam(name = "typeRecherche", defaultValue = "et")
          String typeRecherche,
      @Parameter(
              description =
                  "if false, response is 204 and Http Response contains link headers to retrieve the resources",
              required = false)
          @RequestParam(name = "body", defaultValue = "false")
          boolean resultatsDansBody,
      @Parameter(
              description = "if true, the contacts found will only have id value",
              required = false)
          @RequestParam(name = "idOnly", defaultValue = "false")
          boolean identifiantsSeuls,
      @Parameter(description = "Find a organization by it's certificate", required = false)
          @RequestParam(name = "certificat", required = false)
          String certificat) {
    RealmStorage realmUserStorage = converterDomainRealm.getRealmForDomain(domaine);

    Organisation searchOrganisation = new Organisation();
    searchOrganisation.setIdentifiant(identifiant);
    searchOrganisation.setNomCommun(nomCommun);
    searchOrganisation.setDescription(description);
    if (organisationId != null) {
      Organisation subOrganisation = new Organisation();
      subOrganisation.setIdentifiant(organisationId);
      searchOrganisation.setOrganisationDeRattachement(subOrganisation);
    }
    searchOrganisation.setAdresseMessagerie(mail);
    if (certificat != null) {
      searchOrganisation.setCleDeChiffrement(certificat.getBytes());
    }
    Organization searchSugoiOrganization =
        ouganextSugoiMapper.serializeToSugoi(searchOrganisation, Organization.class);

    PageableResult pageable = new PageableResult();
    if (searchCookie != null) {
      pageable.setSearchToken(searchCookie);
    }
    pageable.setOffset(offset);
    pageable.setSize(size);

    PageResult<Organization> foundOrganizations =
        organizationService.findByProperties(
            realmUserStorage.getRealm(),
            realmUserStorage.getUserStorage(),
            searchSugoiOrganization,
            pageable,
            SearchType.AND);

    HttpHeaders headers = new HttpHeaders();
    headers.add("X-Total-Size", String.valueOf(foundOrganizations.getTotalElements()));
    headers.add(
        "nextLocation",
        ServletUriComponentsBuilder.fromCurrentRequest()
            .replaceQueryParam("searchCookie", foundOrganizations.getSearchToken())
            .build()
            .toString());
    if (!resultatsDansBody) {
      headers.put(
          "Link",
          foundOrganizations.getResults().stream()
              .map(
                  organization -> {
                    String request =
                        ServletUriComponentsBuilder.fromCurrentRequestUri().build().toString();
                    return Link.fromUri(
                            request.substring(0, request.lastIndexOf("/"))
                                + "/organisation/"
                                + organization.getIdentifiant())
                        .rel("http://xml/insee.fr/schema/annuaire/Contact")
                        .build()
                        .toString();
                  })
              .collect(Collectors.toList()));
      return ResponseEntity.status(HttpStatus.NO_CONTENT).headers(headers).build();
    } else {
      Organisations organisations = new Organisations();
      if (identifiantsSeuls) {
        organisations
            .getListe()
            .addAll(
                foundOrganizations.getResults().stream()
                    .map(
                        organization -> {
                          Organisation organisation = new Organisation();
                          organisation.setIdentifiant(organization.getIdentifiant());
                          return organisation;
                        })
                    .collect(Collectors.toList()));
      } else {
        organisations
            .getListe()
            .addAll(
                foundOrganizations.getResults().stream()
                    .map(
                        organization ->
                            ouganextSugoiMapper.serializeToOuganext(
                                organization, Organisation.class))
                    .collect(Collectors.toList()));
      }
      return ResponseEntity.status(HttpStatus.OK).headers(headers).body(organisations);
    }
  }
}
