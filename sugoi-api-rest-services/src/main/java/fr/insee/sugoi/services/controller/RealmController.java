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

import fr.insee.sugoi.core.exceptions.RealmNotFoundException;
import fr.insee.sugoi.core.model.ProviderRequest;
import fr.insee.sugoi.core.model.ProviderResponse;
import fr.insee.sugoi.core.model.SugoiUser;
import fr.insee.sugoi.core.service.ConfigService;
import fr.insee.sugoi.model.Realm;
import fr.insee.sugoi.services.Utils;
import fr.insee.sugoi.services.decider.AuthorizeMethodDecider;
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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
@RequestMapping(value = {"/v2", "/"})
@Tag(
    name = "Manage Realms and storages",
    description = "New Endpoints to create, update, delete and find realms")
@SecurityRequirements(
    value = {@SecurityRequirement(name = "oAuth"), @SecurityRequirement(name = "basic")})
public class RealmController {

  @Autowired ConfigService configService;

  @Autowired
  @Qualifier("NewAuthorizeMethodDecider")
  AuthorizeMethodDecider authorizeService;

  @GetMapping(value = "/realms")
  @Operation(summary = "Get realms where you have rights")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "realm found",
            content = {
              @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = Realm[].class))
            })
      })
  public ResponseEntity<List<Realm>> getRealms(
      @Parameter(description = "Id of the realm to search", required = false)
          @RequestParam(name = "id", required = false)
          String id,
      Authentication authentication) {
    List<Realm> realms = new ArrayList<>();
    List<Realm> realmsFiltered = new ArrayList<>();

    if (id != null) {
      Realm retrievedRealm =
          configService
              .getRealm(id)
              .orElseThrow(() -> new RealmNotFoundException("Realm " + id + " not found"));
      if (retrievedRealm != null) {
        realms.add(retrievedRealm);
      }
    } else {
      realms.addAll(configService.getRealms());
    }

    // Filter realm before sending if user not admin
    for (Realm realm : realms) {
      if (realm.getUserStorages().stream()
              .filter(us -> authorizeService.isReader(realm.getName(), us.getName()))
              .count()
          > 0) realmsFiltered.add(realm);
    }
    return new ResponseEntity<List<Realm>>(realmsFiltered, HttpStatus.OK);
  }

  @PostMapping(
      value = "/realms",
      consumes = {MediaType.APPLICATION_JSON_VALUE},
      produces = {MediaType.APPLICATION_JSON_VALUE})
  @Operation(summary = "Create realm")
  @PreAuthorize("@NewAuthorizeMethodDecider.isAdmin()")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "409",
            description = "Realm already exist",
            content = {@Content(mediaType = "application/json")}),
        @ApiResponse(
            responseCode = "201",
            description = "Realm created",
            content = {
              @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = Realm.class))
            })
      })
  public ResponseEntity<Realm> createRealm(
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
      @RequestBody Realm realm) {
    ProviderResponse response =
        configService.createRealm(
            realm,
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
    return ResponseEntity.status(Utils.convertStatusTHttpStatus(response, true, false))
        .location(createRealmURI(realm.getName()))
        .header("X-SUGOI-TRANSACTION-ID", response.getRequestId())
        .header("X-SUGOI-REQUEST-STATUS", response.getStatus().toString())
        .body(response.getEntity() != null ? (Realm) response.getEntity() : null);
  }

  // TODO According to the status send by provider does http code must change ?
  @PutMapping(
      value = "/realms/{id}",
      consumes = {MediaType.APPLICATION_JSON_VALUE},
      produces = {MediaType.APPLICATION_JSON_VALUE})
  @PreAuthorize("@NewAuthorizeMethodDecider.isAdmin()")
  @Operation(summary = "Update realm")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "400",
            description = "Bad request",
            content = {@Content(mediaType = "application/json")}),
        @ApiResponse(
            responseCode = "404",
            description = "Realm not found",
            content = {@Content(mediaType = "application/json")}),
        @ApiResponse(
            responseCode = "200",
            description = "Realm updated",
            content = {
              @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = Realm.class))
            })
      })
  public ResponseEntity<Realm> updateRealm(
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
      @RequestBody Realm realm,
      @PathVariable("id") String id) {
    if (!realm.getName().equalsIgnoreCase(id)) {
      return ResponseEntity.badRequest().build();
    }
    ProviderResponse response =
        configService.updateRealm(
            realm,
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
    return ResponseEntity.status(Utils.convertStatusTHttpStatus(response, false, false))
        .location(createRealmURI(realm.getName()))
        .header("X-SUGOI-TRANSACTION-ID", response.getRequestId())
        .header("X-SUGOI-REQUEST-STATUS", response.getStatus().toString())
        .body(response.getEntity() != null ? (Realm) response.getEntity() : null);
  }

  @DeleteMapping(
      value = "/realms/{id}",
      produces = {MediaType.APPLICATION_JSON_VALUE})
  @Operation(summary = "Delete Realm")
  @PreAuthorize("@NewAuthorizeMethodDecider.isAdmin()")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "404",
            description = "Realm not found",
            content = {@Content(mediaType = "application/json")}),
        @ApiResponse(
            responseCode = "204",
            description = "Realm deleted",
            content = {@Content(mediaType = "application/json")})
      })
  public ResponseEntity<ProviderResponse> deleteRealm(
      @PathVariable("id") String id,
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
        configService.deleteRealm(
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

  private URI createRealmURI(String realmName) {
    return ServletUriComponentsBuilder.fromCurrentRequest().path("/" + realmName).build().toUri();
  }
}
