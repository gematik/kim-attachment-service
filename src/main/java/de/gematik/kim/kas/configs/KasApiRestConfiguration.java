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

package de.gematik.kim.kas.configs;

import de.gematik.kim.kas.ApiClient;
import de.gematik.kim.kas.utils.KasHttpTraceRepository;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.trace.http.HttpTraceRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SecurityScheme(
    name = "basicAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "basic"
)
@OpenAPIDefinition(
    info = @io.swagger.v3.oas.annotations.info.Info(title = "KIM_KAS", version = "${gematik.kim.kas.version}"),
    security = @SecurityRequirement(name = "basicAuth")
)
@org.springframework.context.annotation.Configuration
public class KasApiRestConfiguration {

  @Value("${gematik.kim.kas.kim-am-base-url}")
  private String amPath;
  @Value("${gematik.kim.kas.version}")
  private String version;
  @Value("#{'${gematik.kim.kas.swagger-ui-base-addr}'.split(',')}")
  private List<String> swaggerBaseUri;

  /**
   * Adding actuator endpoint for http logging.
   *
   * @return HttpTraceRepository.
   */
  @Bean
  public HttpTraceRepository httpTraceRepository() {
    return new KasHttpTraceRepository();
  }

  /**
   * Configure CORS access to endpoints and swagger ui.
   *
   * @return CORS configuration.
   */
  @Bean
  public WebMvcConfigurer corsConfigurer() {
    return new WebMvcConfigurer() {
      @Override
      public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/actuator")
            .allowedMethods("GET", "HEAD")
            .allowedOrigins("*");
        registry.addMapping("/v1.1/**")
            .allowedMethods("GET", "HEAD", "POST")
            .allowedOrigins("*");
      }
    };
  }

  /**
   * Configure Swagger UI.
   *
   * @return Swagger UI configuration.
   */
  @Bean
  public OpenAPI kasOpenApi() {
    List<Server> servers = swaggerBaseUri.stream()
        .map(s -> new Server().url(s))
        .collect(Collectors.toList());

    return new OpenAPI()
        .info(new Info().title("KAS - KOM LE Attachment Service")
            .description("Storage for attachments of KIM messages")
            .version(version)
            .license(new License()
                .name("Apache 2.0")
                .url("http://www.apache.org/licenses/LICENSE-2.0")))
        .externalDocs(new ExternalDocumentation()
            .description("KIM (Communication in medicine) - API documentation")
            .url("https://github.com/gematik/api-kim"))
        .servers(servers);
  }

  @Bean
  public ApiClient getAMApiClient() {
    return new ApiClient().setBasePath(amPath);
  }
}
