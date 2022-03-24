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

package de.gematik.kim.kas.service;

import de.gematik.kim.kas.am_api.KasAuthApi;
import de.gematik.kim.kas.exceptions.NotEnoughSpaceException;
import de.gematik.kim.kas.model.RemainingQuotaInfo;
import java.util.Objects;
import javax.naming.AuthenticationException;
import javax.naming.CommunicationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;

@Service
@Slf4j
@RequiredArgsConstructor
public class AmClient {

  private final KasAuthApi kasAuthApi;

  public void basicAuth(String username, String password) throws AuthenticationException {
    try {
      ResponseEntity<Void> response = kasAuthApi.basicAuthWithHttpInfo(username, password);
      if (!response.getStatusCode().is2xxSuccessful()) {
        throw new AuthenticationException("Authorization failed for " + username);
      }
    } catch (Exception ex) {
      log.error("Authorization failed for {}", username);
      throw new AuthenticationException("Authorization failed for " + username);
    }
  }

  public long checkAndGetRemainingQuota(long size, String username) throws NotEnoughSpaceException {
    ResponseEntity<RemainingQuotaInfo> response;
    try {
      response = kasAuthApi.addResourceWithHttpInfo(username, String.valueOf(size));
    } catch (HttpServerErrorException ex) {
      if (ex.getRawStatusCode() == HttpStatus.INSUFFICIENT_STORAGE.value()) {
        log.error("Not enough space left. {} bytes needed but {} left", size, ex.getResponseBodyAsString());
        throw new NotEnoughSpaceException(
            "Could not save file for " + username + " because quota is exceeded. Only "
                + ex.getResponseBodyAsString() + " bytes left but " + size + " needed");
      } else {
        throw ex;
      }
    } catch (Exception ex) {
      log.error("Internal error: ", ex);
      throw new RuntimeException("Internal error while requesting quota");
    }
    Long remainingQuota = Objects.requireNonNull(response.getBody()).getRemainingQuota();
    log.info("{} left for {} ", remainingQuota, username);
    return remainingQuota;
  }

  public String releaseQuota(long size, String username) throws CommunicationException {
    ResponseEntity<RemainingQuotaInfo> response;
    try {
      response = kasAuthApi.releaseResourceWithHttpInfo(username, String.valueOf(size));
    } catch (Exception ex) {
      log.error("Communication to accountmanager broken. ", ex);
      throw new CommunicationException("Could not release space for " + username);
    }
    Long remainingQuota = Objects.requireNonNull(response.getBody()).getRemainingQuota();
    log.info("Storage released for {}. New remain quota = {}", username, remainingQuota);
    return String.valueOf(remainingQuota);
  }
}
