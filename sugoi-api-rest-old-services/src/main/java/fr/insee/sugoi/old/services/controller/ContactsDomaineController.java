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
import fr.insee.sugoi.converter.ouganext.Contact;
import fr.insee.sugoi.converter.ouganext.Contacts;
import fr.insee.sugoi.converter.ouganext.Organisation;
import fr.insee.sugoi.core.model.ProviderRequest;
import fr.insee.sugoi.core.model.SugoiUser;
import fr.insee.sugoi.core.service.UserService;
import fr.insee.sugoi.model.Habilitation;
import fr.insee.sugoi.model.User;
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
import java.util.ArrayList;
import java.util.List;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/v1")
@Tag(name = "[Deprecated] - Gestion des contacts", description = "Old Enpoints to manage contacts")
@SecurityRequirement(name = "basic")
public class ContactsDomaineController {

  @Autowired private UserService userService;

  @Autowired private OuganextSugoiMapper ouganextSugoiMapper;

  @Autowired private ConverterDomainRealm converterDomainRealm;

  /**
   * Create a contact. The contact id is slug if filled and not already used. Otherwise the id is
   * generated.
   *
   * @param domaine
   * @param contact
   * @param slug filled in header Slug, the id of the contact if not already used
   * @return CREATED with the link of the created contact as a header
   */
  @PreAuthorize("@OldAuthorizeMethodDecider.isAtLeastGestionnaire(#domaine)")
  @PostMapping(
      value = "/{domaine}/contacts",
      consumes = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE},
      produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
  @Operation(
      summary =
          "Create a contact. The contact id is slug if filled and not already used. Otherwise the id is generated.",
      deprecated = true)
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "Contact successfully updated or created",
            content = {
              @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = Contact.class)),
              @Content(
                  mediaType = "application/xml",
                  schema = @Schema(implementation = Contact.class))
            })
      })
  public ResponseEntity<Contact> createContactIdentifiant(
      @Parameter(
              description = "Name of the domaine where the operation will be made",
              required = true)
          @PathVariable(name = "domaine", required = true)
          String domaine,
      @Parameter(description = "the id of the contact if not already used", required = false)
          @RequestHeader(value = "Slug", required = false)
          String slug,
      @Parameter(description = "Contact to create", required = true) @RequestBody Contact contact,
      Authentication authentication) {
    RealmStorage realmUserStorage = converterDomainRealm.getRealmForDomain(domaine);

    User sugoiUser = ouganextSugoiMapper.serializeToSugoi(contact, User.class);
    if (slug != null
        && userService
            .findById(realmUserStorage.getRealm(), realmUserStorage.getUserStorage(), slug)
            .isEmpty()) {
      sugoiUser.setUsername(slug);
    } else if (sugoiUser.getUsername() == null) {
      sugoiUser.setUsername(UUID.randomUUID().toString());
    }
    userService.create(
        realmUserStorage.getRealm(),
        realmUserStorage.getUserStorage(),
        sugoiUser,
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
        request.substring(0, request.lastIndexOf("/")) + "/contact/" + sugoiUser.getUsername();
    String resultLink =
        Link.fromUri(location)
            .rel("http://xml/insee.fr/schema/annuaire/Contact")
            .build()
            .toString();
    return ResponseEntity.status(HttpStatus.CREATED)
        .header("Location", location)
        .header("Link", resultLink)
        .build();
  }

  /**
   * Search contacts matching criteria.
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
   * @param habilitations
   * @param application
   * @param role
   * @param rolePropriete
   * @param resultatsDansBody if false, response is 204 and Http Response contains link headers to
   *     retrieve the resources
   * @param identifiantsSeuls if true, the contacts found will only have id value
   * @param certificat
   * @return OK and all the contacts in the body if resultatsDansBody or NO_CONTENT and the Link to
   *     the results as a header
   */
  @PreAuthorize("@OldAuthorizeMethodDecider.isAtLeastConsultant(#domaine)")
  @GetMapping(
      value = "/{domaine}/contacts",
      produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
  @Operation(summary = "Search contacts matching criteria.", deprecated = true)
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Contacts matching criterias",
            content = {
              @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = Contact[].class)),
              @Content(
                  mediaType = "application/xml",
                  schema = @Schema(implementation = Contact[].class))
            })
      })
  public ResponseEntity<?> getContacts(
      @Parameter(
              description = "Name of the domaine where the operation will be made",
              required = true)
          @PathVariable(name = "domaine", required = true)
          String domaine,
      @Parameter(description = "Identifiant of the contact", required = false)
          @RequestParam(name = "identifiant", required = false)
          String identifiant,
      @Parameter(description = "NomCommun of the contact", required = false)
          @RequestParam(name = "nomCommun", required = false)
          String nomCommun,
      @Parameter(description = "Description of the contact", required = false)
          @RequestParam(name = "description", required = false)
          String description,
      @Parameter(description = "Organization's id where the contact belongs ", required = false)
          @RequestParam(name = "organisationId", required = false)
          String organisationId,
      @Parameter(description = "Mail of the contact", required = false)
          @RequestParam(name = "mail", required = false)
          String mail,
      @Parameter(description = "Number of results to return", required = false)
          @RequestParam(name = "size", defaultValue = "20", required = false)
          int size,
      @Parameter(description = "Offset to apply to the search", required = false)
          @RequestParam(name = "start", defaultValue = "0", required = false)
          int offset,
      @Parameter(description = "Cookie to continue a previous search", required = false)
          @RequestParam(name = "searchCookie", required = false)
          String searchCookie,
      @Parameter(description = "Kind of search can be Et or Ou", required = false)
          @RequestParam(name = "typeRecherche", defaultValue = "et", required = false)
          String typeRecherche,
      @Parameter(description = "Habilitation which contact can have", required = false)
          @RequestParam(name = "habilitation", required = false)
          List<String> habilitations,
      @Parameter(description = "Application in which contact can be", required = false)
          @RequestParam(name = "application", required = false)
          String application,
      @Parameter(description = "Role which contact can have", required = false)
          @RequestParam(name = "role", required = false)
          String role,
      @Parameter(description = "Propriete which contact can have", required = false)
          @RequestParam(name = "rolePropriete", required = false)
          String rolePropriete,
      @Parameter(
              description =
                  "if false, response is 204 and Http Response contains link headers to retrieve the resources",
              required = false)
          @RequestParam(name = "body", defaultValue = "false")
          boolean resultatsDansBody,
      @Parameter(description = "Get only a list of identifiant", required = false)
          @RequestParam(name = "idOnly", defaultValue = "false")
          boolean identifiantsSeuls,
      @Parameter(description = "Find a contact by it's certificate", required = false)
          @RequestParam(name = "certificat", required = false)
          String certificat) {
    RealmStorage realmUserStorage = converterDomainRealm.getRealmForDomain(domaine);

    Contact searchContact = new Contact();
    searchContact.setIdentifiant(identifiant);
    searchContact.setNomCommun(nomCommun);
    searchContact.setDescription(description);
    if (organisationId != null) {
      Organisation searchOrganisation = new Organisation();
      searchOrganisation.setIdentifiant(organisationId);
      searchContact.setOrganisationDeRattachement(searchOrganisation);
    }
    searchContact.setAdresseMessagerie(mail);
    if (certificat != null) {
      searchContact.setCertificate(certificat.getBytes());
    }
    List<Habilitation> searchHabilitations = new ArrayList<>();
    if (habilitations != null) {
      searchHabilitations =
          habilitations.stream()
              .map(habilitation -> new Habilitation(habilitation))
              .collect(Collectors.toList());
    }
    if (application != null & role != null & rolePropriete != null) {
      Habilitation createdHabilitation = new Habilitation(application, role, rolePropriete);
      searchHabilitations.add(createdHabilitation);
    }
    User searchSugoiUser = ouganextSugoiMapper.serializeToSugoi(searchContact, User.class);
    searchSugoiUser.setHabilitations(searchHabilitations);

    PageableResult pageable = new PageableResult();
    if (searchCookie != null) {
      pageable.setSearchToken(searchCookie);
    }
    pageable.setOffset(offset);
    pageable.setSize(size);

    PageResult<User> foundUsers =
        userService.findByProperties(
            realmUserStorage.getRealm(),
            realmUserStorage.getUserStorage(),
            searchSugoiUser,
            pageable,
            SearchType.AND);

    HttpHeaders headers = new HttpHeaders();
    headers.add("X-Total-Size", String.valueOf(foundUsers.getTotalElements()));
    headers.add(
        "nextLocation",
        ServletUriComponentsBuilder.fromCurrentRequest()
            .replaceQueryParam("searchCookie", foundUsers.getSearchToken())
            .build()
            .toString());

    if (!resultatsDansBody) {
      headers.put(
          "Link",
          foundUsers.getResults().stream()
              .map(
                  user -> {
                    String request =
                        ServletUriComponentsBuilder.fromCurrentRequestUri().build().toString();
                    return Link.fromUri(
                            request.substring(0, request.lastIndexOf("/"))
                                + "/contact/"
                                + user.getUsername())
                        .rel("http://xml/insee.fr/schema/annuaire/Contact")
                        .build()
                        .toString();
                  })
              .collect(Collectors.toList()));
      return ResponseEntity.status(HttpStatus.NO_CONTENT).headers(headers).build();
    } else {
      Contacts contacts = new Contacts();
      if (identifiantsSeuls) {
        contacts
            .getListe()
            .addAll(
                foundUsers.getResults().stream()
                    .map(
                        user -> {
                          Contact contact = new Contact();
                          contact.setIdentifiant(user.getUsername());
                          return contact;
                        })
                    .collect(Collectors.toList()));
      } else {
        contacts
            .getListe()
            .addAll(
                foundUsers.getResults().stream()
                    .map(user -> ouganextSugoiMapper.serializeToOuganext(user, Contact.class))
                    .collect(Collectors.toList()));
      }
      return ResponseEntity.status(HttpStatus.OK).headers(headers).body(contacts);
    }
  }

  /**
   * Find contacts by certificate
   *
   * @param domaine
   * @param id id of the certificate
   * @return NOT IMPLEMENTED
   */
  @PreAuthorize("@OldAuthorizeMethodDecider.isAtLeastConsultant(#domaine)")
  @GetMapping(
      value = "/{domaine}/contacts/certificat/{id}",
      produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
  @Operation(summary = "Find contacts by certificate", deprecated = true)
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "501",
            description = "Not yet implemented",
            content = {
              @Content(mediaType = "application/json"),
              @Content(mediaType = "application/xml")
            })
      })
  public ResponseEntity<?> getAPartirDeIdCertificat(
      @Parameter(
              description = "Name of the domaine where the operation will be made",
              required = true)
          @PathVariable(name = "domaine", required = true)
          String domaine,
      @Parameter(description = " id of the certificate", required = true)
          @PathVariable(name = "id", required = true)
          String id) {
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
  }

  /**
   * Get the number of contacts in domaine.
   *
   * @param domaine
   * @return number of contacts in domaine
   */
  @PreAuthorize("@OldAuthorizeMethodDecider.isAtLeastConsultant(#domaine)")
  @GetMapping(
      value = "/{domaine}/contacts/size",
      produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
  @Operation(summary = "Get the number of contacts in domaine.", deprecated = true)
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "204",
            description = "Size of the domaine",
            content = {
              @Content(mediaType = "application/json"),
              @Content(mediaType = "application/xml")
            })
      })
  public ResponseEntity<?> getContactsSize(
      @Parameter(
              description = "Name of the domaine where the operation will be made",
              required = true)
          @PathVariable(name = "domaine", required = true)
          String domaine) {
    RealmStorage realmUserStorage = converterDomainRealm.getRealmForDomain(domaine);

    PageableResult pageable = new PageableResult();
    PageResult<User> foundUsers =
        userService.findByProperties(
            realmUserStorage.getRealm(),
            realmUserStorage.getUserStorage(),
            new User(),
            pageable,
            SearchType.AND);
    return ResponseEntity.status(HttpStatus.NO_CONTENT)
        .header("X-Total-Size", String.valueOf(foundUsers.getTotalElements()))
        .build();
  }
}
