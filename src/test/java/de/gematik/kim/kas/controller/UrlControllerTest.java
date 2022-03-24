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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

@SpringBootTest(classes = UrlController.class)
class UrlControllerTest {

  @Value("${gematik.kim.kas.version}")
  public String VERSION;
  @Value("${gematik.kim.kas.path-prefix}")
  private String PREFIX;
  @Autowired
  UrlController urlController;

  @BeforeEach
  void setup() {
    setField(urlController, "version", VERSION);
    setField(urlController, "prefix", PREFIX);
  }

  @Test
  @SneakyThrows
  void shouldBuildFullUrlWithoutPort() {
    String filename = "filename";
    UriComponents uriComponents = UriComponentsBuilder.newInstance()
        .scheme("https")
        .host("kim-kas")
        .build();

    String url = urlController.getFullUrl(filename, uriComponents);

    assertThat(url).isEqualTo(
        new StringBuffer().append("https://kim-kas/")
            .append(PREFIX)
            .append("/")
            .append(VERSION)
            .append("/attachment/")
            .append(filename)
            .toString());
  }

  @Test
  @SneakyThrows
  void shouldBuildFullUrlWithPort() {
    String filename = "filename";

    UriComponents uriComponents = UriComponentsBuilder.newInstance()
        .scheme("https")
        .host("kim-kas")
        .port(8080)
        .build();

    String url = urlController.getFullUrl(filename, uriComponents);

    assertThat(url).isEqualTo(
        new StringBuffer().append("https://kim-kas:8080/")
            .append(PREFIX)
            .append("/")
            .append(VERSION)
            .append("/attachment/")
            .append(filename)
            .toString());
  }
}
