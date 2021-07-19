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
import fr.insee.sugoi.core.model.SugoiUser;
import fr.insee.sugoi.core.service.CredentialsService;
import fr.insee.sugoi.core.service.UserService;
import fr.insee.sugoi.model.User;
import fr.insee.sugoi.model.paging.PasswordChangeRequest;
import fr.insee.sugoi.model.paging.SendMode;
import fr.insee.sugoi.services.Utils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(
    name = "Manage credentials",
    description = "New endpoints to initialize, update or reset user password")
@RequestMapping(value = {"/v2", "/"})
@SecurityRequirements(
    value = {@SecurityRequirement(name = "oAuth"), @SecurityRequirement(name = "basic")})
public class CredentialsController {

  @Autowired private CredentialsService credentialsService;
  @Autowired private UserService userService;

  @PostMapping(path = {"/realms/{realm}/storages/{storage}/users/{id}/reinitPassword"})
  @Operation(summary = "Reinitialize the password of the user")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "204",
            description = "Operation done",
            content = {@Content(mediaType = "application/json")}),
        @ApiResponse(
            responseCode = "500",
            description = "Something went wrong",
            content = {@Content(mediaType = "application/json")})
      })
  @PreAuthorize("@NewAuthorizeMethodDecider.isPasswordManager(#realm,#storage)")
  public ResponseEntity<ProviderResponse> reinitPassword(
      @Parameter(description = "Password change request&", required = true) @RequestBody
          PasswordChangeRequest pcr,
      @Parameter(
              description = "Name of the realm where the operation will be made",
              required = true)
          @PathVariable("realm")
          String realm,
      @Parameter(
              description = "Name of the userStorage where the operation will be made",
              required = false)
          @PathVariable(value = "storage", required = false)
          String userStorage,
      @Parameter(description = "User's id to change password", required = true) @PathVariable("id")
          String id,
      @Parameter(description = "Way to send password", required = false)
          @RequestParam(value = "sendModes", required = false)
          List<SendMode> sendMode,
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
        credentialsService.reinitPassword(
            realm,
            userStorage,
            id,
            pcr,
            sendMode != null ? sendMode : new ArrayList<>(),
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

  @PostMapping(path = {"/realms/{realm}/users/{id}/reinitPassword"})
  @Operation(summary = "Reinitialize the password of the user")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "204",
            description = "Operation done",
            content = {@Content(mediaType = "application/json")}),
        @ApiResponse(
            responseCode = "500",
            description = "Something went wrong",
            content = {@Content(mediaType = "application/json")})
      })
  @PreAuthorize("@NewAuthorizeMethodDecider.isPasswordManager(#realm,#storage)")
  public ResponseEntity<ProviderResponse> reinitPassword(
      @Parameter(description = "Password change request&", required = true) @RequestBody
          PasswordChangeRequest pcr,
      @Parameter(
              description = "Name of the realm where the operation will be made",
              required = true)
          @PathVariable("realm")
          String realm,
      @Parameter(description = "User's id to change password", required = true) @PathVariable("id")
          String id,
      @Parameter(description = "Way to send password", required = false)
          @RequestParam(value = "sendModes", required = false)
          List<SendMode> sendMode,
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

    User user =
        userService
            .findById(realm, null, id)
            .orElseThrow(
                () -> new UserNotFoundException("Cannot find user " + id + " in realm " + realm));
    return reinitPassword(
        pcr,
        realm,
        (String) user.getMetadatas().get(GlobalKeysConfig.USERSTORAGE),
        id,
        sendMode,
        isAsynchronous,
        isUrgent,
        transactionId,
        authentication);
  }

  @PostMapping(path = {"/realms/{realm}/storages/{storage}/users/{id}/changePassword"})
  @Operation(summary = "Change user password with the new one provided")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "204",
            description = "Operation done",
            content = {@Content(mediaType = "application/json")}),
        @ApiResponse(
            responseCode = "500",
            description = "Something went wrong",
            content = {@Content(mediaType = "application/json")})
      })
  @PreAuthorize("@NewAuthorizeMethodDecider.isPasswordManager(#realm,#storage)")
  public ResponseEntity<ProviderResponse> changePassword(
      @Parameter(description = "Password change request", required = true) @RequestBody
          PasswordChangeRequest pcr,
      @Parameter(
              description = "Name of the realm where the operation will be made",
              required = true)
          @PathVariable("realm")
          String realm,
      @Parameter(
              description = "Name of the userStorage where the operation will be made",
              required = true)
          @PathVariable(value = "storage", required = true)
          String userStorage,
      @Parameter(description = "User's id to change password", required = true) @PathVariable("id")
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
        credentialsService.changePassword(
            realm,
            userStorage,
            id,
            pcr,
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

  @PostMapping(path = {"/realms/{realm}/users/{id}/changePassword"})
  @Operation(summary = "Change user password with the new one provided")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "204",
            description = "Operation done",
            content = {@Content(mediaType = "application/json")}),
        @ApiResponse(
            responseCode = "500",
            description = "Something went wrong",
            content = {@Content(mediaType = "application/json")})
      })
  @PreAuthorize("@NewAuthorizeMethodDecider.isPasswordManager(#realm,#storage)")
  public ResponseEntity<ProviderResponse> changePassword(
      @Parameter(description = "Password change request&", required = true) @RequestBody
          PasswordChangeRequest pcr,
      @Parameter(
              description = "Name of the realm where the operation will be made",
              required = true)
          @PathVariable("realm")
          String realm,
      @Parameter(description = "User's id to change password", required = true) @PathVariable("id")
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

    User user =
        userService
            .findById(realm, null, id)
            .orElseThrow(
                () -> new UserNotFoundException("Cannot find user " + id + " in realm " + realm));
    return changePassword(
        pcr,
        realm,
        (String) user.getMetadatas().get(GlobalKeysConfig.USERSTORAGE),
        isAsynchronous,
        isUrgent,
        transactionId,
        authentication);
  }

  @PostMapping(path = {"/realms/{realm}/storages/{storage}/users/{id}/initPassword"})
  @PreAuthorize("@NewAuthorizeMethodDecider.isPasswordManager(#realm,#storage)")
  @Operation(summary = "Initialize user's password with a random generated password")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "204",
            description = "Operation done",
            content = {@Content(mediaType = "application/json")}),
        @ApiResponse(
            responseCode = "500",
            description = "Something went wrong",
            content = {@Content(mediaType = "application/json")})
      })
  public ResponseEntity<ProviderResponse> initPassword(
      @Parameter(
              description = "Name of the realm where the operation will be made",
              required = true)
          @PathVariable("realm")
          String realm,
      @Parameter(
              description = "Name of the userStorage where the operation will be made",
              required = false)
          @PathVariable(value = "storage", required = false)
          String userStorage,
      @Parameter(description = "User's id to initialize password", required = true)
          @PathVariable("id")
          String id,
      @Parameter(description = "Password change request&", required = true) @RequestBody
          PasswordChangeRequest pcr,
      @Parameter(description = "Way to send password", required = false)
          @RequestParam(value = "sendModes", required = false)
          List<SendMode> sendMode,
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
        credentialsService.initPassword(
            realm,
            userStorage,
            id,
            pcr,
            sendMode != null ? sendMode : new ArrayList<>(),
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

  @PostMapping(path = {"/realms/{realm}/users/{id}/initPassword"})
  @PreAuthorize("@NewAuthorizeMethodDecider.isPasswordManager(#realm,#storage)")
  @Operation(summary = "Initialize user's password with a random generated password")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "204",
            description = "Operation done",
            content = {@Content(mediaType = "application/json")}),
        @ApiResponse(
            responseCode = "500",
            description = "Something went wrong",
            content = {@Content(mediaType = "application/json")})
      })
  public ResponseEntity<ProviderResponse> initPassword(
      @Parameter(
              description = "Name of the realm where the operation will be made",
              required = true)
          @PathVariable("realm")
          String realm,
      @Parameter(description = "User's id to initialize password", required = true)
          @PathVariable("id")
          String id,
      @Parameter(description = "Password change request&", required = true) @RequestBody
          PasswordChangeRequest pcr,
      @Parameter(description = "Way to send password", required = false)
          @RequestParam(value = "sendModes", required = false)
          List<SendMode> sendMode,
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

    User user =
        userService
            .findById(realm, null, id)
            .orElseThrow(
                () -> new UserNotFoundException("Cannot find user " + id + " in realm " + realm));
    return initPassword(
        realm,
        (String) user.getMetadatas().get(GlobalKeysConfig.USERSTORAGE),
        id,
        pcr,
        sendMode,
        isAsynchronous,
        isUrgent,
        transactionId,
        authentication);
  }

  @PostMapping(
      path = {"/realms/{realm}/storages/{storage}/users/{id}/validate-password"},
      consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE})
  @PreAuthorize("@NewAuthorizeMethodDecider.isPasswordManager(#realm,#storage)")
  @Operation(summary = "Check if provided password is the user's one")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Valid password",
            content = {@Content(mediaType = "application/json")}),
        @ApiResponse(
            responseCode = "401",
            description = "Invalid password",
            content = {@Content(mediaType = "application/json")})
      })
  public ResponseEntity<Void> validatePassword(
      @Parameter(
              description = "Name of the realm where the operation will be made",
              required = true)
          @PathVariable("realm")
          String realm,
      @Parameter(
              description = "Name of the userStorage where the operation will be made",
              required = false)
          @PathVariable(value = "storage", required = false)
          String userStorage,
      @Parameter(description = "User's id to validate password", required = true)
          @PathVariable("id")
          String id,
      @Parameter(
              description = "Map<String,String> containing the key password with user password",
              required = true)
          @RequestParam
          MultiValueMap<String, String> params) {

    if (params.containsKey("password")
        && credentialsService.validateCredential(
            realm, userStorage, id, params.getFirst("password"))) {
      return ResponseEntity.ok().build();
    } else {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
  }

  @PostMapping(
      path = {"/realms/{realm}/users/{id}/validate-password"},
      consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE})
  @PreAuthorize("@NewAuthorizeMethodDecider.isPasswordManager(#realm,#storage)")
  @Operation(summary = "Check if provided password is the user's one")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Valid password",
            content = {@Content(mediaType = "application/json")}),
        @ApiResponse(
            responseCode = "401",
            description = "Invalid password",
            content = {@Content(mediaType = "application/json")})
      })
  public ResponseEntity<Void> validatePassword(
      @Parameter(
              description = "Name of the realm where the operation will be made",
              required = true)
          @PathVariable("realm")
          String realm,
      @Parameter(description = "User's id to validate password", required = true)
          @PathVariable("id")
          String id,
      @Parameter(
              description = "Map<String,String> containing the key password with user password",
              required = true)
          @RequestParam
          MultiValueMap<String, String> params) {
    User user =
        userService
            .findById(realm, null, id)
            .orElseThrow(
                () -> new UserNotFoundException("Cannot find user " + id + " in realm " + realm));
    return validatePassword(
        realm, (String) user.getMetadatas().get(GlobalKeysConfig.USERSTORAGE), id, params);
  }
}
