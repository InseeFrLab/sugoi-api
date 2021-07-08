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

import fr.insee.sugoi.core.exceptions.ApplicationNotFoundException;
import fr.insee.sugoi.core.model.ProviderRequest;
import fr.insee.sugoi.core.model.ProviderResponse;
import fr.insee.sugoi.core.model.ProviderResponse.ProviderResponseStatus;
import fr.insee.sugoi.core.model.SugoiUser;
import fr.insee.sugoi.core.service.ApplicationService;
import fr.insee.sugoi.model.Application;
import fr.insee.sugoi.model.paging.PageResult;
import fr.insee.sugoi.model.paging.PageableResult;
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
@Tag(
    name = "Manage applications",
    description = "New endpoints to create, update, delete, or find application")
@RequestMapping(value = {"/v2", "/"})
@SecurityRequirements(
    value = {@SecurityRequirement(name = "oAuth"), @SecurityRequirement(name = "basic")})
public class ApplicationController {

  @Autowired private ApplicationService applicationService;

  @GetMapping(
      path = {"/realms/{realm}/applications"},
      produces = {MediaType.APPLICATION_JSON_VALUE})
  @Operation(summary = "Search applications by parameter")
  @PreAuthorize("@NewAuthorizeMethodDecider.isReader(#realm,#storage)")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Applications found according to parameter",
            content = {
              @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = PageResult.class))
            })
      })
  public ResponseEntity<PageResult<Application>> getApplications(
      @Parameter(
              description = "Name of the realm where the operation will be made",
              required = true)
          @PathVariable("realm")
          String realm,
      @Parameter(description = "Expected size of result", required = false)
          @RequestParam(name = "size", defaultValue = "20")
          int size,
      @Parameter(description = "Token to continue a previous search", required = false)
          @RequestParam(name = "searchToken", required = false)
          String searchCookie,
      @Parameter(description = "Offset to apply when searching", required = false)
          @RequestParam(name = "offset", required = false, defaultValue = "0")
          int offset,
      @Parameter(description = "Name of the app searched", required = false)
          @RequestParam(value = "name", required = false)
          String name,
      @Parameter(description = "Name of the owner of the app searched", required = false)
          @RequestParam(value = "owner", required = false)
          String owner) {

    Application applicationFilter = new Application();
    applicationFilter.setName(name);
    applicationFilter.setOwner(owner);

    PageableResult pageableResult = new PageableResult(size, offset, searchCookie);

    PageResult<Application> foundApplications =
        applicationService.findByProperties(realm, applicationFilter, pageableResult);

    if (foundApplications.isHasMoreResult()) {
      URI location =
          ServletUriComponentsBuilder.fromCurrentRequest()
              .replaceQueryParam("searchToken", foundApplications.getSearchToken())
              .build()
              .toUri();
      return ResponseEntity.status(HttpStatus.OK)
          .header(HttpHeaders.LOCATION, location.toString())
          .body(foundApplications);
    } else {
      return ResponseEntity.status(HttpStatus.OK).body(foundApplications);
    }
  }

  @PostMapping(
      value = {"/realms/{realm}/applications"},
      consumes = {MediaType.APPLICATION_JSON_VALUE},
      produces = {MediaType.APPLICATION_JSON_VALUE})
  @Operation(summary = "Create application")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "Application created",
            content = {
              @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = Application.class))
            }),
        @ApiResponse(
            responseCode = "409",
            description = "Application already exist",
            content = {@Content(mediaType = "application/json")})
      })
  @PreAuthorize("@NewAuthorizeMethodDecider.isWriter(#realm,#storage)")
  public ResponseEntity<Application> createApplication(
      @Parameter(
              description = "Name of the realm where the operation will be made",
              required = true)
          @PathVariable("realm")
          String realm,
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
      @Parameter(description = "Application to create", required = true) @RequestBody
          Application application) {

    ProviderResponse response =
        applicationService.create(
            realm,
            application,
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
            .path("/" + response.getEntityId())
            .build()
            .toUri();
    return ResponseEntity.status(Utils.convertStatusTHttpStatus(response, true, false))
        .location(response.getStatus().equals(ProviderResponseStatus.OK) ? location : null)
        .header("X-SUGOI-TRANSACTION-ID", response.getRequestId())
        .header("X-SUGOI-REQUEST-STATUS", response.getStatus().toString())
        .body(response.getEntity() != null ? (Application) response.getEntity() : null);
  }

  @PutMapping(
      value = {"/realms/{realm}/applications/{applicationName}"},
      consumes = {MediaType.APPLICATION_JSON_VALUE},
      produces = {MediaType.APPLICATION_JSON_VALUE})
  @Operation(summary = "Update application")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Application updated",
            content = {
              @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = Application.class))
            }),
        @ApiResponse(
            responseCode = "404",
            description = "Application does'nt exist",
            content = {@Content(mediaType = "application/json")})
      })
  @PreAuthorize("@NewAuthorizeMethodDecider.isAppManager(#realm,#applicationName)")
  public ResponseEntity<Application> updateApplication(
      @Parameter(
              description = "Name of the realm where the operation will be made",
              required = true)
          @PathVariable("realm")
          String realm,
      @Parameter(description = "Id of the app to update", required = true)
          @PathVariable("applicationName")
          String applicationName,
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
      @Parameter(description = "Application to update", required = true) @RequestBody
          Application application) {

    if (!application.getName().equals(applicationName)) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }

    ProviderResponse response =
        applicationService.update(
            realm,
            application,
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

    URI location = ServletUriComponentsBuilder.fromCurrentRequest().build().toUri();

    return ResponseEntity.status(Utils.convertStatusTHttpStatus(response, false, false))
        .location(location)
        .header("X-SUGOI-TRANSACTION-ID", response.getRequestId())
        .header("X-SUGOI-REQUEST-STATUS", response.getStatus().toString())
        .body(response.getEntity() != null ? (Application) response.getEntity() : null);
  }

  @DeleteMapping(
      value = {"/realms/{realm}/applications/{id}"},
      produces = {MediaType.APPLICATION_JSON_VALUE})
  @Operation(summary = "Delete application")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "204",
            description = "Application deleted",
            content = {@Content(mediaType = "application/json")}),
        @ApiResponse(
            responseCode = "404",
            description = "Application does'nt exist",
            content = {@Content(mediaType = "application/json")})
      })
  @PreAuthorize("@NewAuthorizeMethodDecider.isWriter(#realm,#storage)")
  public ResponseEntity<ProviderResponse> deleteApplication(
      @Parameter(
              description = "Name of the realm where the operation will be made",
              required = true)
          @PathVariable("realm")
          String realm,
      @Parameter(description = "Application's id to delete", required = false) @PathVariable("id")
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
        applicationService.delete(
            realm,
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

  @GetMapping(
      path = {"/realms/{realm}/applications/{name}"},
      produces = {MediaType.APPLICATION_JSON_VALUE})
  @Operation(summary = "Get application by name")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Application found according to parameter",
            content = {
              @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = Application.class))
            }),
        @ApiResponse(
            responseCode = "404",
            description = "Application does'nt exist",
            content = {@Content(mediaType = "application/json")})
      })
  @PreAuthorize("@NewAuthorizeMethodDecider.isReader(#realm,#userStorage)")
  public ResponseEntity<Application> getApplicationByName(
      @Parameter(
              description = "Name of the realm where the operation will be made",
              required = true)
          @PathVariable("realm")
          String realm,
      @Parameter(description = "Name of the app searched", required = true) @PathVariable("name")
          String name) {
    Application application =
        applicationService
            .findById(realm, name)
            .orElseThrow(
                () ->
                    new ApplicationNotFoundException(
                        "Application " + name + " not found in realm " + realm));
    return ResponseEntity.status(HttpStatus.OK).body(application);
  }
}
