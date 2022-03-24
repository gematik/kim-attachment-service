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

package de.gematik.kim.kas.filter;

import de.gematik.kim.kas.service.auth.AuthStrategy;
import de.gematik.kim.kas.service.auth.BasicAuthStrategy;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@Slf4j
public class BaseAuthFilter implements HandlerInterceptor {


  private AuthStrategy authStrategy;

  @Autowired
  @Qualifier("BasicAuth")
  private AuthStrategy basicAuth;
  @Autowired
  @Qualifier("NoAuth")
  private AuthStrategy noAuth;


  @Value("${gematik.kim.kas.version}")
  private String version;
  @Value("${gematik.kim.kas.path-prefix}")
  private String prefix;

  @Value("${gematik.kim.kas.use-auth-initial:true}")
  private boolean auth;

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object object)
      throws IOException {
    if (!request.getRequestURI().endsWith("/" + prefix + "/" + version + "/attachment")) {
      return true;
    }
    try {
      authStrategy.verifyAccount(request);
    } catch (Exception ex) {
      response.sendError(401, "Authorization failed");
      response.getWriter().write("{message:\"" + ex.getMessage() + "\"}");
      response.getWriter().flush();
      return false;
    }
    return true;
  }

  public void toggleAuth() {
    if (getAuthStrategy() instanceof BasicAuthStrategy) {
      setAuthStrategy(noAuth);
    } else {
      setAuthStrategy(basicAuth);
    }
  }

  public AuthStrategy getAuthStrategy() {
    if (authStrategy == null) {
      if (auth) {
        setAuthStrategy(basicAuth);
      } else {
        setAuthStrategy(noAuth);
      }
    }
    return authStrategy;
  }

  public void setAuthStrategy(AuthStrategy authStrategy) {
    this.authStrategy = authStrategy;
  }
}
