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

package de.gematik.kim.kas.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.util.UriComponents;

@Controller
public class UrlController {

  @Value("${gematik.kim.kas.version}")
  private String version;
  @Value("${gematik.kim.kas.path-prefix}")
  private String prefix;
  @Value("#{'${gematik.kim.kas.swagger-ui-base-addr}'.split(',')}")
  private List<String> swaggerBaseUri;
  @Value("${gematik.kim.kas.use-first-swagger-base-ui-addr-for-add-attachment}")
  private boolean useSwaggerUri;
  private static final String PATH = "attachment";

  public String getFullUrl(String filename, UriComponents currentUriRequest) {
    if ((!useSwaggerUri || swaggerBaseUri.isEmpty()) && currentUriRequest != null) {
      String usedHost = currentUriRequest.getHost();
      int usedPort = currentUriRequest.getPort();
      String protocol = currentUriRequest.getScheme();

      String hostWithPort = usedHost + (usedPort > 0 ? ":" + usedPort : "");

      return String.format("%s://%s/%s/%s/%s/%s",
          protocol,
          hostWithPort,
          prefix,
          version,
          PATH,
          filename);
    }

    return String.format("%s/%s/%s/%s",
        swaggerBaseUri.get(0),
        version,
        PATH,
        filename);
  }
}
