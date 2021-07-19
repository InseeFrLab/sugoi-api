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

import fr.insee.sugoi.core.configuration.GlobalKeysConfig;
import fr.insee.sugoi.core.exceptions.UserNotFoundException;
import fr.insee.sugoi.core.model.ProviderRequest;
import fr.insee.sugoi.core.model.ProviderResponse;
import fr.insee.sugoi.core.model.ProviderResponse.ProviderResponseStatus;
import fr.insee.sugoi.core.model.SugoiUser;
import fr.insee.sugoi.core.service.UserService;
import fr.insee.sugoi.model.Habilitation;
import fr.insee.sugoi.model.Organization;
import fr.insee.sugoi.model.User;
import fr.insee.sugoi.model.paging.PageResult;
import fr.insee.sugoi.model.paging.PageableResult;
import fr.insee.sugoi.model.paging.SearchType;
import fr.insee.sugoi.services.Utils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;
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
@Tag(name = "Manage users", description = "New endpoints to create update delete and find users")
@RequestMapping(value = {"/v2", "/"})
@SecurityRequirements(
    value = {@SecurityRequirement(name = "oAuth"), @SecurityRequirement(name = "basic")})
public class UserController {

  @Autowired private UserService userService;

  @GetMapping(
      path = {"/realms/{realm}/storages/{storage}/users"},
      produces = {MediaType.APPLICATION_JSON_VALUE})
  @Operation(summary = "Search users according to parameters, paginate working")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Users found according to parameter",
            content = {
              @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = PageResult.class))
            })
      })
  @PreAuthorize("@NewAuthorizeMethodDecider.isReader(#realm,#storage)")
  public ResponseEntity<?> getUsers(
      @Parameter(
              description = "Name of the realm where the operation will be made",
              required = true)
          @PathVariable("realm")
          String realm,
      @Parameter(
              description = "Name of the userStorage where the operation will be made",
              required = false)
          @PathVariable(name = "storage", required = false)
          String storage,
      @Parameter(description = "User's identifiant of user to search ", required = false)
          @RequestParam(name = "identifiant", required = false)
          String identifiant,
      @Parameter(description = "User's mail of user to search ", required = false)
          @RequestParam(name = "mail", required = false)
          String mail,
      @Parameter(description = "User's commun name of user to search ", required = false)
          @RequestParam(name = "nomCommun", required = false)
          String nomCommun,
      @Parameter(description = "User's description", required = false)
          @RequestParam(name = "description", required = false)
          String description,
      @Parameter(description = "User rattached organization", required = false)
          @RequestParam(name = "organisationId", required = false)
          String organisationId,
      @Parameter(description = "Expected size of result", required = false)
          @RequestParam(name = "size", defaultValue = "20")
          int size,
      @Parameter(description = "Offset to apply when searching", required = false)
          @RequestParam(name = "offset", required = false, defaultValue = "0")
          int offset,
      @Parameter(description = "Token to continue a previous search", required = false)
          @RequestParam(name = "searchToken", required = false)
          String searchCookie,
      @Parameter(description = "Search type can be OR or AND ", required = false)
          @RequestParam(name = "typeRecherche", defaultValue = "AND", required = true)
          SearchType typeRecherche,
      @Parameter(description = "User's habilitations of user to search ", required = false)
          @RequestParam(name = "habilitation", required = false)
          List<String> habilitations,
      @Parameter(description = "User's application of user to search ", required = false)
          @RequestParam(name = "application", required = false)
          String application) {

    // set the user which will serve as a model to retrieve the matching users
    User searchUser = new User();
    searchUser.setUsername(identifiant);
    searchUser.setLastName(nomCommun);
    searchUser.setMail(mail);
    if (organisationId != null) {
      Organization organizationSearch = new Organization();
      organizationSearch.setIdentifiant(organisationId);
      searchUser.setOrganization(organizationSearch);
    }
    if (habilitations != null) {
      habilitations.forEach(
          habilitationName -> searchUser.addHabilitation(new Habilitation(habilitationName)));
    }

    // set the page to maintain the search request pagination
    PageableResult pageable = new PageableResult(size, offset, searchCookie);

    PageResult<User> foundUsers =
        userService.findByProperties(realm, storage, searchUser, pageable, typeRecherche);
    if (foundUsers.isHasMoreResult()) {
      URI location =
          ServletUriComponentsBuilder.fromCurrentRequest()
              .replaceQueryParam("searchToken", foundUsers.getSearchToken())
              .build()
              .toUri();
      return ResponseEntity.status(HttpStatus.OK)
          .header(HttpHeaders.LOCATION, location.toString())
          .body(foundUsers);
    } else {
      return ResponseEntity.status(HttpStatus.OK).body(foundUsers);
    }
  }

  @GetMapping(
      path = {"/realms/{realm}/users"},
      produces = {MediaType.APPLICATION_JSON_VALUE})
  @Operation(summary = "Search users according to parameters, paginate could not work")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Users found according to parameters",
            content = {
              @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = PageResult.class))
            })
      })
  @PreAuthorize("@NewAuthorizeMethodDecider.isReader(#realm,#storage)")
  public ResponseEntity<?> getUsers(
      @Parameter(
              description = "Name of the realm where the operation will be made",
              required = true)
          @PathVariable("realm")
          String realm,
      @Parameter(description = "User's identifiant of user to search ", required = false)
          @RequestParam(name = "identifiant", required = false)
          String identifiant,
      @Parameter(description = "User's mail of user to search ", required = false)
          @RequestParam(name = "mail", required = false)
          String mail,
      @Parameter(description = "User's commun name of user to search ", required = false)
          @RequestParam(name = "nomCommun", required = false)
          String nomCommun,
      @Parameter(description = "User's description", required = false)
          @RequestParam(name = "description", required = false)
          String description,
      @Parameter(description = "User rattached organization", required = false)
          @RequestParam(name = "organisationId", required = false)
          String organisationId,
      @Parameter(description = "Expected size of result", required = false)
          @RequestParam(name = "size", defaultValue = "20")
          int size,
      @Parameter(description = "Offset to apply when searching", required = false)
          @RequestParam(name = "offset", required = false, defaultValue = "0")
          int offset,
      @Parameter(description = "Token to continue a previous search", required = false)
          @RequestParam(name = "searchToken", required = false)
          String searchCookie,
      @Parameter(description = "Search type can be OR or AND ", required = false)
          @RequestParam(name = "typeRecherche", defaultValue = "AND", required = true)
          SearchType typeRecherche,
      @Parameter(description = "User's habilitations of user to search ", required = false)
          @RequestParam(name = "habilitation", required = false)
          List<String> habilitations,
      @Parameter(description = "User's application of user to search ", required = false)
          @RequestParam(name = "application", required = false)
          String application) {
    return getUsers(
        realm,
        null,
        identifiant,
        mail,
        nomCommun,
        description,
        organisationId,
        size,
        offset,
        searchCookie,
        typeRecherche,
        habilitations,
        application);
  }

  @PostMapping(
      value = {"/realms/{realm}/storages/{storage}/users"},
      consumes = {MediaType.APPLICATION_JSON_VALUE},
      produces = {MediaType.APPLICATION_JSON_VALUE})
  @Operation(summary = "Create user")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "User created",
            content = {
              @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = User.class))
            }),
        @ApiResponse(
            responseCode = "409",
            description = "User already exist",
            content = {
              @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = User.class))
            })
      })
  @PreAuthorize("@NewAuthorizeMethodDecider.isWriter(#realm,#storage)")
  public ResponseEntity<?> createUsers(
      @Parameter(
              description = "Name of the realm where the operation will be made",
              required = true)
          @PathVariable("realm")
          String realm,
      @Parameter(
              description = "Name of the userStorage where the operation will be made",
              required = false)
          @PathVariable(name = "storage", required = false)
          String storage,
      @Parameter(description = "Allowed asynchronous request", required = false)
          @RequestHeader(name = "X-SUGOI-ASYNCHRONOUS-ALLOWED-REQUEST", defaultValue = "false")
          boolean isAsynchronous,
      @Parameter(description = "Make request prioritary", required = false)
          @RequestHeader(name = "X-SUGOI-URGENT-REQUEST", defaultValue = "false")
          boolean isUrgent,
      @Parameter(description = "Transaction Id", required = false)
          @RequestHeader(name = "X-SUGOI-TRANSACTION-ID", required = false)
          String transactionId,
      Authentication authentication,
      @Parameter(description = "User to create", required = true) @RequestBody User user) {

    ProviderResponse response =
        userService.create(
            realm,
            storage,
            user,
            new ProviderRequest(
                new SugoiUser(
                    authentication.getName(),
                    authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .map(String::toUpperCase)
                        .collect(Collectors.toList())),
                isAsynchronous,
                transactionId,
                isUrgent));
    URI location =
        ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/" + user.getUsername())
            .build()
            .toUri();
    return ResponseEntity.status(Utils.convertStatusTHttpStatus(response, true, false))
        .location(response.getStatus().equals(ProviderResponseStatus.OK) ? location : null)
        .header("X-SUGOI-TRANSACTION-ID", response.getRequestId())
        .header("X-SUGOI-REQUEST-STATUS", response.getStatus().toString())
        .body(response.getEntity() != null ? (User) response.getEntity() : null);
  }

  @PutMapping(
      value = {"/realms/{realm}/storages/{storage}/users/{id}"},
      consumes = {MediaType.APPLICATION_JSON_VALUE},
      produces = {MediaType.APPLICATION_JSON_VALUE})
  @Operation(summary = "Update user")
  @PreAuthorize("@NewAuthorizeMethodDecider.isWriter(#realm,#storage)")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "User updated",
            content = {
              @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = User.class))
            }),
        @ApiResponse(
            responseCode = "404",
            description = "User not found",
            content = {@Content(mediaType = "application/json")}),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid combinaison of id between body and path",
            content = {@Content(mediaType = "application/json")})
      })
  public ResponseEntity<?> updateUsers(
      @Parameter(
              description = "Name of the realm where the operation will be made",
              required = true)
          @PathVariable("realm")
          String realm,
      @Parameter(
              description = "Name of the userStorage where the operation will be made",
              required = false)
          @PathVariable(name = "storage", required = false)
          String storage,
      @Parameter(description = "User's id to update", required = true) @PathVariable("id")
          String id,
      @Parameter(description = "Allowed asynchronous request", required = false)
          @RequestHeader(name = "X-SUGOI-ASYNCHRONOUS-ALLOWED-REQUEST", defaultValue = "false")
          boolean isAsynchronous,
      @Parameter(description = "Make request prioritary", required = false)
          @RequestHeader(name = "X-SUGOI-URGENT-REQUEST", defaultValue = "false")
          boolean isUrgent,
      @Parameter(description = "Transaction Id", required = false)
          @RequestHeader(name = "X-SUGOI-TRANSACTION-ID", required = false)
          String transactionId,
      Authentication authentication,
      @Parameter(description = "User to update", required = true) @RequestBody User user) {

    if (!user.getUsername().equals(id)) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }

    ProviderResponse response =
        userService.update(
            realm,
            storage,
            user,
            new ProviderRequest(
                new SugoiUser(
                    authentication.getName(),
                    authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .map(String::toUpperCase)
                        .collect(Collectors.toList())),
                isAsynchronous,
                null,
                isUrgent));
    URI location = ServletUriComponentsBuilder.fromCurrentRequest().build().toUri();
    return ResponseEntity.status(Utils.convertStatusTHttpStatus(response, false, false))
        .header(HttpHeaders.LOCATION, location.toString())
        .header("X-SUGOI-TRANSACTION-ID", response.getRequestId())
        .header("X-SUGOI-REQUEST-STATUS", response.getStatus().toString())
        .body(response.getEntity() != null ? (User) response.getEntity() : null);
  }

  @PutMapping(
      value = {"/realms/{realm}/users/{id}"},
      consumes = {MediaType.APPLICATION_JSON_VALUE},
      produces = {MediaType.APPLICATION_JSON_VALUE})
  @Operation(summary = "Update user")
  @PreAuthorize("@NewAuthorizeMethodDecider.isWriter(#realm,#storage)")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "User updated",
            content = {
              @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = User.class))
            }),
        @ApiResponse(
            responseCode = "404",
            description = "User not found",
            content = {@Content(mediaType = "application/json")}),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid combinaison of id between body and path",
            content = {@Content(mediaType = "application/json")})
      })
  public ResponseEntity<?> updateUsers(
      @Parameter(
              description = "Name of the realm where the operation will be made",
              required = true)
          @PathVariable("realm")
          String realm,
      @Parameter(description = "User's id to update", required = true) @PathVariable("id")
          String id,
      @Parameter(description = "Allowed asynchronous request", required = false)
          @RequestHeader(name = "X-SUGOI-ASYNCHRONOUS-ALLOWED-REQUEST", defaultValue = "false")
          boolean isAsynchronous,
      @Parameter(description = "Make request prioritary", required = false)
          @RequestHeader(name = "X-SUGOI-URGENT-REQUEST", defaultValue = "false")
          boolean isUrgent,
      @Parameter(description = "Transaction Id", required = false)
          @RequestHeader(name = "X-SUGOI-TRANSACTION-ID", required = false)
          String transactionId,
      Authentication authentication,
      @Parameter(description = "User to update", required = true) @RequestBody User user) {

    if (!user.getUsername().equals(id)) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }
    User foundUser =
        userService
            .findById(realm, null, id)
            .orElseThrow(
                () -> new UserNotFoundException("Cannot find user " + id + " in realm " + realm));
    return updateUsers(
        realm,
        (String) foundUser.getMetadatas().get(GlobalKeysConfig.USERSTORAGE),
        id,
        isAsynchronous,
        isUrgent,
        transactionId,
        authentication,
        user);
  }

  @DeleteMapping(
      value = {"/realms/{realm}/storages/{storage}/users/{id}"},
      produces = {MediaType.APPLICATION_JSON_VALUE})
  @Operation(summary = "Delete user")
  @PreAuthorize("@NewAuthorizeMethodDecider.isWriter(#realm,#storage)")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "204",
            description = "User deleted",
            content = {
              @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = User.class))
            }),
        @ApiResponse(
            responseCode = "404",
            description = "User not found",
            content = {@Content(mediaType = "application/json")})
      })
  public ResponseEntity<String> deleteUsers(
      @Parameter(
              description = "Name of the realm where the operation will be made",
              required = true)
          @PathVariable("realm")
          String realm,
      @Parameter(
              description = "Name of the userStorage where the operation will be made",
              required = false)
          @PathVariable(name = "storage", required = false)
          String storage,
      @Parameter(description = "User's id to delete", required = true) @PathVariable("id")
          String id,
      @Parameter(description = "Allowed asynchronous request", required = false)
          @RequestHeader(name = "X-SUGOI-ASYNCHRONOUS-ALLOWED-REQUEST", defaultValue = "false")
          boolean isAsynchronous,
      @Parameter(description = "Make request prioritary", required = false)
          @RequestHeader(name = "X-SUGOI-URGENT-REQUEST", defaultValue = "false")
          boolean isUrgent,
      @Parameter(description = "Transaction Id", required = false)
          @RequestHeader(name = "X-SUGOI-TRANSACTION-ID", required = false)
          String transactionId,
      Authentication authentication) {

    ProviderResponse response =
        userService.delete(
            realm,
            storage,
            id,
            new ProviderRequest(
                new SugoiUser(
                    authentication.getName(),
                    authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .map(String::toUpperCase)
                        .collect(Collectors.toList())),
                isAsynchronous,
                transactionId,
                isUrgent));
    return ResponseEntity.status(Utils.convertStatusTHttpStatus(response, false, true))
        .header("X-SUGOI-TRANSACTION-ID", response.getRequestId())
        .header("X-SUGOI-REQUEST-STATUS", response.getStatus().toString())
        .build();
  }

  @DeleteMapping(
      value = {"/realms/{realm}/users/{id}"},
      produces = {MediaType.APPLICATION_JSON_VALUE})
  @Operation(summary = "Delete user")
  @PreAuthorize("@NewAuthorizeMethodDecider.isWriter(#realm,#storage)")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "204",
            description = "User deleted",
            content = {
              @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = User.class))
            }),
        @ApiResponse(
            responseCode = "404",
            description = "User not found",
            content = {@Content(mediaType = "application/json")})
      })
  public ResponseEntity<String> deleteUsers(
      @Parameter(
              description = "Name of the realm where the operation will be made",
              required = true)
          @PathVariable("realm")
          String realm,
      @Parameter(description = "User's id to delete", required = true) @PathVariable("id")
          String id,
      @Parameter(description = "Allowed asynchronous request", required = false)
          @RequestHeader(name = "X-SUGOI-ASYNCHRONOUS-ALLOWED-REQUEST", defaultValue = "false")
          boolean isAsynchronous,
      @Parameter(description = "Make request prioritary", required = false)
          @RequestHeader(name = "X-SUGOI-URGENT-REQUEST", defaultValue = "false")
          boolean isUrgent,
      @Parameter(description = "Transaction Id", required = false)
          @RequestHeader(name = "X-SUGOI-TRANSACTION-ID", required = false)
          String transactionId,
      Authentication authentication) {

    User foundUser =
        userService
            .findById(realm, null, id)
            .orElseThrow(
                () -> new UserNotFoundException("Cannot find user " + id + " in realm " + realm));
    return deleteUsers(
        realm,
        (String) foundUser.getMetadatas().get(GlobalKeysConfig.USERSTORAGE),
        isAsynchronous,
        isUrgent,
        transactionId,
        authentication);
  }

  @GetMapping(
      path = {"/realms/{realm}/storages/{storage}/users/{username}"},
      produces = {MediaType.APPLICATION_JSON_VALUE})
  @Operation(summary = "Get user by username")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "User found according to parameters",
            content = {
              @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = User.class))
            })
      })
  @PreAuthorize("@NewAuthorizeMethodDecider.isReader(#realm,#storage)")
  public ResponseEntity<User> getUserByUsername(
      @Parameter(
              description = "Name of the realm where the operation will be made",
              required = true)
          @PathVariable("realm")
          String realm,
      @Parameter(
              description = "Name of the userStorage where the operation will be made",
              required = false)
          @PathVariable(name = "storage", required = false)
          String storage,
      @Parameter(description = "Username to search", required = true) @PathVariable("username")
          String id) {
    User user =
        userService
            .findById(realm, storage, id)
            .orElseThrow(
                () -> new UserNotFoundException("Cannot find user " + id + " in realm " + realm));
    return ResponseEntity.status(HttpStatus.OK).body(user);
  }

  @GetMapping(
      path = {"/realms/{realm}/users/{username}"},
      produces = {MediaType.APPLICATION_JSON_VALUE})
  @Operation(summary = "Get user by username")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "User found according to parameters",
            content = {
              @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = User.class))
            })
      })
  @PreAuthorize("@NewAuthorizeMethodDecider.isReader(#realm,#storage)")
  public ResponseEntity<User> getUserByUsername(
      @Parameter(
              description = "Name of the realm where the operation will be made",
              required = true)
          @PathVariable("realm")
          String realm,
      @Parameter(description = "Username to search", required = true) @PathVariable("username")
          String id) {
    return getUserByUsername(realm, null, id);
  }
}
