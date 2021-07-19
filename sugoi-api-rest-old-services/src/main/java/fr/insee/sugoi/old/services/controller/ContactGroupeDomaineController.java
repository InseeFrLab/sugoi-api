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
import fr.insee.sugoi.converter.ouganext.Groupe;
import fr.insee.sugoi.core.exceptions.GroupNotFoundException;
import fr.insee.sugoi.core.exceptions.UserNotFoundException;
import fr.insee.sugoi.core.model.ProviderRequest;
import fr.insee.sugoi.core.model.SugoiUser;
import fr.insee.sugoi.core.service.GroupService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1")
@Tag(name = "[Deprecated] - Manage groups", description = "Old Enpoints to manage contact's group")
@SecurityRequirement(name = "basic")
public class ContactGroupeDomaineController {

  @Autowired private UserService userService;
  @Autowired private GroupService groupService;

  @Autowired private OuganextSugoiMapper ouganextSugoiMapper;
  @Autowired private ConverterDomainRealm converterDomainRealm;

  /**
   * Get the groups of a contact
   *
   * @param identifiant
   * @param domaine
   * @return OK with the list of groups
   */
  @PreAuthorize("@OldAuthorizeMethodDecider.isAtLeastConsultant(#domaine)")
  @GetMapping(
      value = "/{domaine}/contact/{id}/groupes",
      produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
  @Operation(summary = "Get the groups of a contact", deprecated = true)
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Contact successfully updated or created",
            content = {
              @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = Groupe.class)),
              @Content(
                  mediaType = "application/xml",
                  schema = @Schema(implementation = Groupe.class))
            })
      })
  public ResponseEntity<List<Groupe>> getGroups(
      @Parameter(description = "Contact to search groups", required = true)
          @PathVariable(name = "id", required = true)
          String identifiant,
      @Parameter(
              description = "Name of the domaine where the operation will be made",
              required = true)
          @PathVariable(name = "domaine", required = true)
          String domaine) {
    RealmStorage realmUserStorage = converterDomainRealm.getRealmForDomain(domaine);

    User user =
        userService
            .findById(realmUserStorage.getRealm(), realmUserStorage.getUserStorage(), identifiant)
            .orElseThrow(
                () ->
                    new UserNotFoundException(
                        "Cannot find user " + identifiant + " in domaine " + domaine));
    return ResponseEntity.status(HttpStatus.OK)
        .body(
            user.getGroups().stream()
                .map(group -> ouganextSugoiMapper.serializeToOuganext(group, Groupe.class))
                .collect(Collectors.toList()));
  }

  /**
   * Add a contact in an application group
   *
   * @param identifiant id of the contact to add to the group
   * @param domaine
   * @param nomAppli name of the application of the group
   * @param nomGroupe group name
   * @return NO_CONTENT if contact was added to the group, CONFLICT if user already in group
   */
  @PreAuthorize("@OldAuthorizeMethodDecider.isAtLeastGestionnaire(#domaine)")
  @PutMapping(
      value = "/{domaine}/contact/{id}/groupes/{nomappli}/{nomgroupe}",
      produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
  @Operation(summary = "Add a contact in an application group", deprecated = true)
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "204",
            description = "Contact successfully added to group",
            content = {
              @Content(mediaType = "application/json"),
              @Content(mediaType = "application/xml")
            }),
        @ApiResponse(
            responseCode = "404",
            description = "Group or contact not found",
            content = {
              @Content(mediaType = "application/json"),
              @Content(mediaType = "application/xml")
            }),
        @ApiResponse(
            responseCode = "409",
            description = "Contact already in group",
            content = {
              @Content(mediaType = "application/json"),
              @Content(mediaType = "application/xml")
            })
      })
  public ResponseEntity<String> addToGroup(
      @Parameter(description = "Contact's id to add to group", required = true)
          @PathVariable(name = "id", required = true)
          String identifiant,
      @Parameter(
              description = "Name of the domaine where the operation will be made",
              required = true)
          @PathVariable(name = "domaine", required = true)
          String domaine,
      @Parameter(description = "Name of the application of the group", required = true)
          @PathVariable(name = "nomappli", required = true)
          String nomAppli,
      @Parameter(description = "Group name", required = true)
          @PathVariable(name = "nomgroupe", required = true)
          String nomGroupe,
      Authentication authentication) {
    RealmStorage realmUserStorage = converterDomainRealm.getRealmForDomain(domaine);

    groupService
        .findById(realmUserStorage.getRealm(), nomAppli, nomGroupe)
        .orElseThrow(
            () ->
                new GroupNotFoundException(
                    "Cannot find group "
                        + nomGroupe
                        + " in app "
                        + nomAppli
                        + "in domaine "
                        + domaine));
    User user =
        userService
            .findById(realmUserStorage.getRealm(), realmUserStorage.getUserStorage(), identifiant)
            .orElseThrow(
                () ->
                    new UserNotFoundException(
                        "Cannot find user " + identifiant + " in domaine " + domaine));
    if (user.getGroups() != null
        && user.getGroups().stream()
            .anyMatch(g -> g.getName().equals(nomGroupe) && g.getAppName().equals(nomAppli))) {
      return new ResponseEntity<String>("Contact already in group", HttpStatus.CONFLICT);
    } else {
      groupService.addUserToGroup(
          realmUserStorage.getRealm(),
          realmUserStorage.getUserStorage(),
          identifiant,
          nomAppli,
          nomGroupe,
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
      return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
  }

  /**
   * Delete a user from a group
   *
   * @param identifiant user to delete from the group
   * @param domaine
   * @param nomAppli name of the application of the group
   * @param nomGroupe name of the group in which the user will be added
   * @return NO_CONTENT if deletion occured, CONFLICT if user doesn't not belong to the group
   */
  @PreAuthorize("@OldAuthorizeMethodDecider.isAtLeastGestionnaire(#domaine)")
  @DeleteMapping(
      value = "{domaine}/contact/{id}/groupes/{nomappli}/{nomgroupe}",
      produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
  @Operation(summary = "Add a contact in an application group", deprecated = true)
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "204",
            description = "Contact successfully deleted from group",
            content = {
              @Content(mediaType = "application/json"),
              @Content(mediaType = "application/xml")
            }),
        @ApiResponse(
            responseCode = "404",
            description = "Group or contact not found",
            content = {
              @Content(mediaType = "application/json"),
              @Content(mediaType = "application/xml")
            }),
        @ApiResponse(
            responseCode = "409",
            description = "Contact not in group",
            content = {
              @Content(mediaType = "application/json"),
              @Content(mediaType = "application/xml")
            })
      })
  public ResponseEntity<String> removeFromGroups(
      @Parameter(description = "Contact's id to delete from group", required = true)
          @PathVariable(name = "id", required = true)
          String identifiant,
      @Parameter(
              description = "Name of the domaine where the operation will be made",
              required = true)
          @PathVariable(name = "domaine", required = true)
          String domaine,
      @Parameter(description = "Name of the application of the group", required = true)
          @PathVariable(name = "nomappli", required = true)
          String nomAppli,
      @Parameter(description = "Group name", required = true)
          @PathVariable(name = "nomgroupe", required = true)
          String nomGroupe,
      Authentication authentication) {
    RealmStorage realmUserStorage = converterDomainRealm.getRealmForDomain(domaine);

    User user =
        userService
            .findById(realmUserStorage.getRealm(), realmUserStorage.getUserStorage(), identifiant)
            .orElseThrow(
                () ->
                    new UserNotFoundException(
                        "Cannot find user " + identifiant + " in domaine " + domaine));
    if (user.getGroups() != null
        && user.getGroups().stream()
            .anyMatch(
                group ->
                    group.getAppName().equals(nomAppli) && group.getName().equals(nomGroupe))) {
      groupService.deleteUserFromGroup(
          realmUserStorage.getRealm(),
          realmUserStorage.getUserStorage(),
          identifiant,
          nomAppli,
          nomGroupe,
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
      return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    } else {
      return new ResponseEntity<>("Contact doesn't belong to group", HttpStatus.CONFLICT);
    }
  }
}
