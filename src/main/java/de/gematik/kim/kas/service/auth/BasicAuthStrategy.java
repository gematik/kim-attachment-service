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

package de.gematik.kim.kas.service.auth;

import de.gematik.kim.kas.service.AmClient;
import java.util.Base64;
import javax.naming.AuthenticationException;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("BasicAuth")
@Slf4j
public class BasicAuthStrategy implements AuthStrategy {

  @Autowired
  AmClient amClient;


  public void verifyAccount(HttpServletRequest request) throws AuthenticationException {
    String authString = request.getHeader("authorization");

    if (StringUtils.isBlank(authString)) {
      log.error("No authorization is set");
      throw new AuthenticationException("{message:\"No authorization-header set\"}");
    }
    if (!authString.startsWith("Basic ")) {
      log.error("No basic authorization");
      throw new AuthenticationException("{message:\"No basic authorization\"}");
    }
    String[] auth = new String(Base64.getDecoder().decode(authString.split(" ")[1])).split(":");
    if (auth.length != 2) {
      log.error("Authorization is set wrong. Invalid user password combination.");
      throw new AuthenticationException("{message:\"Authorization is set wrong\"}");
    }

    String username = auth[0];
    String password = auth[1];

    amClient.basicAuth(username, password);

    MDC.put(MAIL, username);
  }
}
