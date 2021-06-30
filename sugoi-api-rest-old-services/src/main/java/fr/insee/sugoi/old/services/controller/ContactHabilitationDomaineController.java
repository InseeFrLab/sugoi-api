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

import fr.insee.sugoi.converter.ouganext.Habilitations;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1")
@Tag(
    name = "[Deprecated] - Manage habilitations",
    description = "Old Enpoints to manage contact's habilitations")
@SecurityRequirement(name = "basic")
public class ContactHabilitationDomaineController {

  @Autowired private UserService userService;

  @Autowired private ConverterDomainRealm converterDomainRealm;

  /**
   * Retrieve all the habilitations of a contact
   *
   * @param identifiant id of the contact
   * @param domaine
   * @return OK with the list of habilitations
   */
  @PreAuthorize("@OldAuthorizeMethodDecider.isAtLeastConsultant(#domaine)")
  @GetMapping(
      value = "/{domaine}/contact/{id}/habilitations",
      produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
  @Operation(summary = "Retrieve all the habilitations of a contact", deprecated = true)
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "List of habilitations of the contact",
            content = {
              @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = Habilitations.class)),
              @Content(
                  mediaType = "application/xml",
                  schema = @Schema(implementation = Habilitations.class))
            })
      })
  public ResponseEntity<Habilitations> getHabilitation(
      @Parameter(description = "Contact's id to look for habilitations", required = true)
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
            new Habilitations(
                userService
                    .findById(
                        realmUserStorage.getRealm(), realmUserStorage.getUserStorage(), identifiant)
                    .orElseThrow(
                        () ->
                            new UserNotFoundException(
                                "Cannot find user " + identifiant + " in domaine " + domaine))
                    .getHabilitations()));
  }

  /**
   * Add habiltiations without properties on an application to a contact. Create the habilitation
   * application if it does not already exist.
   *
   * @param identifiant id of the contact
   * @param domaine
   * @param appName name of the app on which the habilitation applies
   * @param nomRoles roles to add on the app to the user
   * @return NO_CONTENT if creating went well
   */
  @PreAuthorize("@OldAuthorizeMethodDecider.isAtLeastGestionnaire(#domaine)")
  @PutMapping(
      value = "/{domaine}/contact/{id}/habilitations/{application}",
      produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
  @Operation(
      summary =
          "Add habiltiations without properties on an application to a contact. Create the habilitation application if it does not already exist.",
      deprecated = true)
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "204",
            description = "Successfully added habilitation",
            content = {
              @Content(mediaType = "application/json"),
              @Content(mediaType = "application/xml")
            })
      })
  public ResponseEntity<?> addHabilitations(
      @Parameter(description = "Contact's id to add habilitations", required = true)
          @PathVariable(name = "id", required = false)
          String identifiant,
      @Parameter(
              description = "Name of the domaine where the operation will be made",
              required = true)
          @PathVariable(name = "domaine", required = true)
          String domaine,
      @Parameter(description = "Name of the app on which the habilitation applies", required = true)
          @PathVariable(name = "application", required = true)
          String appName,
      @Parameter(description = "Name of role to add on the app to the user", required = true)
          @RequestParam(name = "role", required = true)
          List<String> nomRoles) {

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    List<String> roles =
        authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .map(String::toUpperCase)
            .collect(Collectors.toList());

    RealmStorage realmUserStorage = converterDomainRealm.getRealmForDomain(domaine);

    User user =
        userService
            .findById(realmUserStorage.getRealm(), realmUserStorage.getUserStorage(), identifiant)
            .orElseThrow(
                () ->
                    new UserNotFoundException(
                        "Cannot find user " + identifiant + " in domaine " + domaine));
    Habilitations habilitations = new Habilitations(user.getHabilitations());
    habilitations.addHabilitation(appName, nomRoles);
    user.setHabilitations(habilitations.convertSugoiHabilitation());
    userService.update(
        realmUserStorage.getRealm(),
        realmUserStorage.getUserStorage(),
        user,
        new ProviderRequest(new SugoiUser(authentication.getName(), roles), false, null, false));
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

  /**
   * Add habilitations of a specified role and application with one or more properties to a contact.
   * Role and application habilitation are created if they do not already exist.
   *
   * @param identifiant id of the contact
   * @param domaine
   * @param appName name of the application on which the habilitation apply
   * @param role role on which the habilitation apply
   * @param proprietes properties to add to the role of the application
   * @return NO_CONTENT if creating went well
   */
  @PreAuthorize("@OldAuthorizeMethodDecider.isAtLeastGestionnaire(#domaine)")
  @PutMapping(
      value = "/{domaine}/contact/{id}/habilitations/{application}/{role}",
      produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
  @Operation(
      summary =
          "Add habiltiations with properties on an application to a contact. Create the habilitation application if it does not already exist.",
      deprecated = true)
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "204",
            description = "Successfully added habilitation",
            content = {
              @Content(mediaType = "application/json"),
              @Content(mediaType = "application/xml")
            })
      })
  public ResponseEntity<?> addHabilitationsWithProperty(
      @Parameter(description = "Contact's id to add habilitations", required = true)
          @PathVariable(name = "id", required = false)
          String identifiant,
      @Parameter(
              description = "Name of the domaine where the operation will be made",
              required = true)
          @PathVariable(name = "domaine", required = true)
          String domaine,
      @Parameter(description = "Name of the app on which the habilitation applies", required = true)
          @PathVariable(name = "application", required = true)
          String appName,
      @Parameter(description = "Name of role to add on the app to the user", required = true)
          @PathVariable(name = "role", required = true)
          String role,
      @Parameter(description = "List of properties to add on the app to the user", required = true)
          @RequestParam(name = "propriete", required = true)
          List<String> proprietes) {

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    List<String> roles =
        authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .map(String::toUpperCase)
            .collect(Collectors.toList());

    RealmStorage realmUserStorage = converterDomainRealm.getRealmForDomain(domaine);

    User user =
        userService
            .findById(realmUserStorage.getRealm(), realmUserStorage.getUserStorage(), identifiant)
            .orElseThrow(
                () ->
                    new UserNotFoundException(
                        "Cannot find user " + identifiant + " in domaine " + domaine));
    Habilitations habilitations = new Habilitations(user.getHabilitations());
    habilitations.addHabilitations(appName, role, proprietes);
    user.setHabilitations(habilitations.convertSugoiHabilitation());
    userService.update(
        realmUserStorage.getRealm(),
        realmUserStorage.getUserStorage(),
        user,
        new ProviderRequest(new SugoiUser(authentication.getName(), roles), false, null, false));
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

  /**
   * Delete habilitations of a contact on an application.
   *
   * @param identifiant id of the contact
   * @param domaine
   * @param appName name of the app on which the habilitation applies
   * @param nomRoles roles to delete on the app
   * @return NO_CONTENT if deleting went well
   */
  @PreAuthorize("@OldAuthorizeMethodDecider.isAtLeastGestionnaire(#domaine)")
  @DeleteMapping(
      value = "/{domaine}/contact/{id}/habilitations/{application}",
      produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
  @Operation(summary = "Delete habilitations of a contact on an application.", deprecated = true)
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "204",
            description = "Successfully deleted habilitation",
            content = {
              @Content(mediaType = "application/json"),
              @Content(mediaType = "application/xml")
            })
      })
  public ResponseEntity<?> deleteHabilitations(
      @Parameter(description = "Contact's id to delete habilitations", required = true)
          @PathVariable(name = "id", required = true)
          String identifiant,
      @Parameter(
              description = "Name of the domaine where the operation will be made",
              required = true)
          @PathVariable(name = "domaine", required = true)
          String domaine,
      @Parameter(description = "Name of the app on which the habilitation applies", required = true)
          @PathVariable(name = "application", required = true)
          String appName,
      @Parameter(description = "List of roles to delete on the app for the user", required = true)
          @RequestParam(name = "role", required = true)
          List<String> nomRoles) {

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    List<String> roles =
        authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .map(String::toUpperCase)
            .collect(Collectors.toList());

    RealmStorage realmUserStorage = converterDomainRealm.getRealmForDomain(domaine);

    User user =
        userService
            .findById(realmUserStorage.getRealm(), realmUserStorage.getUserStorage(), identifiant)
            .orElseThrow(
                () ->
                    new UserNotFoundException(
                        "Cannot find user " + identifiant + " in domaine " + domaine));
    Habilitations habilitations = new Habilitations(user.getHabilitations());
    habilitations.removeHabilitation(appName, nomRoles);
    user.setHabilitations(habilitations.convertSugoiHabilitation());
    userService.update(
        realmUserStorage.getRealm(),
        realmUserStorage.getUserStorage(),
        user,
        new ProviderRequest(new SugoiUser(authentication.getName(), roles), false, null, false));
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

  /**
   * Delete habilitation properties of a specified role and application.
   *
   * @param identifiant id of the contact
   * @param domaine
   * @param appName name of the application on which the habilitation apply
   * @param role role on which the habilitation apply
   * @param proprietes properties to delete from the role of the application
   * @return NO_CONTENT if deleting went well
   */
  @PreAuthorize("@OldAuthorizeMethodDecider.isAtLeastGestionnaire(#domaine)")
  @DeleteMapping(
      value = "/{domaine}/contact/{id}/habilitations/{application}/{role}",
      produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
  @Operation(summary = "Delete habilitations of a contact on an application.", deprecated = true)
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "204",
            description = "Successfully deleted habilitation",
            content = {
              @Content(mediaType = "application/json"),
              @Content(mediaType = "application/xml")
            })
      })
  public ResponseEntity<?> deleteHabilitationsWithProperty(
      @Parameter(description = "Contact's id to delete habilitations", required = true)
          @PathVariable(name = "id", required = true)
          String identifiant,
      @Parameter(
              description = "Name of the domaine where the operation will be made",
              required = true)
          @PathVariable(name = "domaine", required = true)
          String domaine,
      @Parameter(description = "Name of the app on which the habilitation applies", required = true)
          @PathVariable(name = "application", required = true)
          String appName,
      @Parameter(description = "Name of role to delete on the app to the user", required = true)
          @RequestParam(name = "role", required = true)
          @PathVariable(name = "role", required = true)
          String nomRole,
      @Parameter(
              description = "List of habilitation to delete on the app for the user",
              required = true)
          @RequestParam(name = "propriete", required = true)
          List<String> proprietes) {

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    List<String> roles =
        authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .map(String::toUpperCase)
            .collect(Collectors.toList());

    RealmStorage realmUserStorage = converterDomainRealm.getRealmForDomain(domaine);

    User user =
        userService
            .findById(realmUserStorage.getRealm(), realmUserStorage.getUserStorage(), identifiant)
            .orElseThrow(
                () ->
                    new UserNotFoundException(
                        "Cannot find user " + identifiant + " in domaine " + domaine));
    Habilitations habilitations = new Habilitations(user.getHabilitations());
    habilitations.removeHabilitation(appName, nomRole, proprietes);
    user.setHabilitations(habilitations.convertSugoiHabilitation());
    userService.update(
        domaine,
        null,
        user,
        new ProviderRequest(new SugoiUser(authentication.getName(), roles), false, null, false));
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }
}
