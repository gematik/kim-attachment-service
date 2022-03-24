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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.gematik.kim.kas.KasApplication;
import de.gematik.kim.kas.service.AmClient;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@Slf4j
@AutoConfigureMockMvc
@ActiveProfiles("it")
@SpringBootTest(classes = KasApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AuthToggleControllerIT {

  @Value("${gematik.kim.kas.version}")
  public String VERSION;
  @Value("${gematik.kim.kas.path-prefix}")
  private String PREFIX;
  @Autowired
  private MockMvc mvc;
  @MockBean
  AmClient amClient;

  @Test
  void sendRequestWithoutAuthAndExpect401() throws Exception {
    mvc.perform(post("/{prefix}/{version}/switchAuth", PREFIX, VERSION));

    mvc.perform(post("/{prefix}/{version}/attachment", PREFIX, VERSION)
            .header("Accept", "application/json")
            .header(HttpHeaders.CONTENT_TYPE, MediaType.MULTIPART_FORM_DATA_VALUE)
            .content("SomeThing".getBytes(StandardCharsets.UTF_8)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void sendRequestWithoutAuthAfterDisableAuthAndExpect201() throws Exception {
    mvc.perform(post("/{prefix}/{version}/switchAuth", PREFIX, VERSION));
    byte[] fileMock = "SomethingWithSense".getBytes();
    mvc.perform(multipart("/{prefix}/{version}/attachment", PREFIX, VERSION)
            .file("attachment", fileMock)
            .param("messageID", "SomeMessageID")
            .param("recipients", "test@gematik.de")
            .param("expires", ZonedDateTime.now(ZoneId.of("Europe/Berlin")).plusMinutes(30).format(
                DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH)))
            .header("Accept", "application/json")
            .header(HttpHeaders.CONTENT_TYPE, MediaType.MULTIPART_FORM_DATA_VALUE)
            .content("SomeThing".getBytes(StandardCharsets.UTF_8)))
        .andExpect(status().isCreated());
  }

}
