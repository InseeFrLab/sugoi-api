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
import fr.insee.sugoi.converter.ouganext.InfoFormattage;
import fr.insee.sugoi.core.exceptions.UserNotFoundException;
import fr.insee.sugoi.core.model.ProviderRequest;
import fr.insee.sugoi.core.model.SugoiUser;
import fr.insee.sugoi.core.service.UserService;
import fr.insee.sugoi.model.User;
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
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1")
@Tag(name = "[Deprecated] - Manage contacts", description = "Old Enpoints to manage contact")
@SecurityRequirement(name = "basic")
public class ContactDomaineController {

  @Autowired private UserService userService;

  @Autowired private OuganextSugoiMapper ouganextSugoiMapper;

  @Autowired private ConverterDomainRealm converterDomainRealm;

  /**
   * Update or create a contact.
   *
   * @param domaine realm of the request
   * @param id id of the contact to update, will replace the id of the contact body parameter
   * @param creation if true a non existent contact to update is created
   * @param contact
   * @return OK with the updated or created contact
   */
  @PreAuthorize("@OldAuthorizeMethodDecider.isAtLeastGestionnaire(#domaine)")
  @PutMapping(
      value = "/{domaine}/contact/{id}",
      consumes = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE},
      produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
  @Operation(summary = "Create or modify a contact", deprecated = true)
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Contact successfully updated or created",
            content = {
              @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = Contact.class)),
              @Content(
                  mediaType = "application/xml",
                  schema = @Schema(implementation = Contact.class))
            }),
        @ApiResponse(
            responseCode = "404",
            description = "Contact to update not found",
            content = {
              @Content(mediaType = "application/json"),
              @Content(mediaType = "application/xml")
            })
      })
  public ResponseEntity<Contact> createOrModifyContact(
      @Parameter(description = "Contact to create or modify", required = true) @RequestBody
          Contact contact,
      @Parameter(description = "Contact's id to modify", required = false)
          @PathVariable(name = "id", required = true)
          String identifiant,
      @Parameter(
              description = "Name of the domaine where the operation will be made",
              required = true)
          @PathVariable(name = "domaine", required = true)
          String domaine,
      @Parameter(
              description =
                  "Boolean indicates whether the contact must be created if not already exist",
              required = false)
          @RequestParam(name = "creation")
          boolean creation,
      Authentication authentication) {
    RealmStorage realmUserStorage = converterDomainRealm.getRealmForDomain(domaine);
    contact.setIdentifiant(identifiant);
    User sugoiUser = ouganextSugoiMapper.serializeToSugoi(contact, User.class);

    if (creation) {
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
      return ResponseEntity.status(HttpStatus.OK)
          .body(
              ouganextSugoiMapper.serializeToOuganext(
                  userService
                      .findById(
                          realmUserStorage.getRealm(),
                          realmUserStorage.getUserStorage(),
                          identifiant)
                      .get(),
                  Contact.class));
    }

    userService.update(
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
    return ResponseEntity.status(HttpStatus.OK)
        .body(
            ouganextSugoiMapper.serializeToOuganext(
                userService
                    .findById(
                        realmUserStorage.getRealm(), realmUserStorage.getUserStorage(), identifiant)
                    .get(),
                Contact.class));
  }

  /**
   * Get a contact by its identifiant
   *
   * @param identifiant
   * @param domaine
   * @return Ok and the first contact matching the identifiant
   */
  @PreAuthorize("@OldAuthorizeMethodDecider.isAtLeastConsultant(#domaine)")
  @GetMapping(
      value = "/{domaine}/contact/{id}",
      produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
  @Operation(summary = "Get a contact by its identifiant", deprecated = true)
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Contact found",
            content = {
              @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = Contact.class)),
              @Content(
                  mediaType = "application/xml",
                  schema = @Schema(implementation = Contact.class))
            })
      })
  public ResponseEntity<Contact> getContactDomaine(
      @Parameter(description = "Contact's id to search", required = true)
          @PathVariable(name = "id", required = true)
          String identifiant,
      @Parameter(
              description = "Name of the domaine where the operation will be made",
              required = true)
          @PathVariable(name = "domaine", required = true)
          String domaine) {
    RealmStorage realmUserStorage = converterDomainRealm.getRealmForDomain(domaine);

    return ResponseEntity.status(HttpStatus.OK)
        .body(
            ouganextSugoiMapper.serializeToOuganext(
                userService
                    .findById(
                        realmUserStorage.getRealm(), realmUserStorage.getUserStorage(), identifiant)
                    .orElseThrow(
                        () ->
                            new UserNotFoundException(
                                "User " + identifiant + " not found in realm " + domaine)),
                Contact.class));
  }

  /**
   * Delete a contact by its identifiant
   *
   * @param identifiant
   * @param domaine
   * @return No content if success, not found if the contact doesn't exist
   */
  @PreAuthorize("@OldAuthorizeMethodDecider.isAtLeastGestionnaire(#domaine)")
  @DeleteMapping(
      value = "/{domaine}/contact/{id}",
      produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
  @Operation(summary = "Delete a contact by its identifiant", deprecated = true)
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Contact deleted",
            content = {
              @Content(mediaType = "application/json"),
              @Content(mediaType = "application/xml")
            }),
        @ApiResponse(
            responseCode = "404",
            description = "Contact to delete not found",
            content = {
              @Content(mediaType = "application/json"),
              @Content(mediaType = "application/xml")
            })
      })
  public ResponseEntity<Contact> deleteContact(
      @Parameter(description = "Contact's id to search", required = true) @PathVariable("id")
          String identifiant,
      @Parameter(
              description = "Name of the domaine where the operation will be made",
              required = true)
          @PathVariable("domaine")
          String domaine,
      Authentication authentication) {
    RealmStorage realmUserStorage = converterDomainRealm.getRealmForDomain(domaine);

    userService.delete(
        realmUserStorage.getRealm(),
        realmUserStorage.getUserStorage(),
        identifiant,
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
   * Send the login of a contact using its mail address
   *
   * @param identifiant id of the contact
   * @param domaine
   * @param modeEnvoiStrings can only be "mail"
   * @param infoEnvoi mail formating information
   * @return NOT_IMPLEMENTED
   */
  @PreAuthorize("@OldAuthorizeMethodDecider.isAtLeastGestionnaire(#domaine)")
  @PostMapping(
      value = "/{domaine}/contact/{id}/login",
      consumes = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE},
      produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
  @Operation(summary = "Send identifiant to a contact", deprecated = true)
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "Not yet implemented",
            content = {
              @Content(mediaType = "application/json"),
              @Content(mediaType = "application/xml")
            })
      })
  public ResponseEntity<?> envoiLogin(
      @Parameter(description = "Contact's id to search", required = true)
          @PathVariable(name = "id", required = true)
          String identifiant,
      @Parameter(
              description = "Name of the domaine where the operation will be made",
              required = true)
          @PathVariable(name = "domaine", required = true)
          String domaine,
      @Parameter(description = "Way to send login can be mail or letter", required = false)
          @RequestParam(name = "modeEnvoi", required = false)
          List<String> modeEnvoiStrings,
      @Parameter(description = "Other infos to send login", required = true) @RequestBody
          InfoFormattage infoEnvoi) {
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
  }
}
