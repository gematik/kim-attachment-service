/*
 * Copyright (c) 2022 gematik GmbH
 * 
 * Licensed under the Apache License, Version 2.0 (the License);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.gematik.kim.kas.api;

import de.gematik.kim.kas.filter.BaseAuthFilter;
import de.gematik.kim.kas.service.auth.AuthStrategy;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/${gematik.kim.kas.path-prefix}/${gematik.kim.kas.version}")
@Slf4j
public class AuthToggleController {

  @Autowired
  private BaseAuthFilter baseAuthFilter;

  @Autowired
  @Qualifier("BasicAuth")
  private AuthStrategy basicAuth;
  @Autowired
  @Qualifier("NoAuth")
  private AuthStrategy noAuth;

  /**
   * Changes the authentication behaviour
   */
  @Operation(summary = "Switch between BasicAuth and NoAuth",
      method = "switchAuth()",
      tags = {"Switch Auth"})
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Switched ", content = {
          @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)}),
      @ApiResponse(responseCode = "500", description = "Internal server error")})
  @PostMapping(value = "/switchAuth")
  public ResponseEntity<String> toggleAuth() {

    baseAuthFilter.toggleAuth();
    return new ResponseEntity<>(String.format("{\"new_auth_strategy\": \"%s\"}",
        baseAuthFilter.getAuthStrategy().getClass().getName()),
        HttpStatus.OK);
  }
}
