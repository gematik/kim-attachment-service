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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.handler;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.gematik.kim.kas.KasApplication;
import de.gematik.kim.kas.api.KasApiRestController;
import de.gematik.kim.kas.service.AmClient;
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
public class MdcFilterIT {

  @Value("${gematik.kim.kas.version}")
  public String VERSION;
  @Value("${gematik.kim.kas.path-prefix}")
  private String PREFIX;
  @Autowired
  private MockMvc mvc;
  @MockBean
  private AmClient amClient;

  @Autowired
  private MdcFilter mdcFilter;

  @Test
  public void testCommonNameHeader() throws Exception {
    mvc.perform(
            multipart("/{prefix}/{version}/attachment", PREFIX, VERSION)
                .file("attachment", "SomethingWithSense".getBytes())
                .param("messageID", "SomeMessageID")
                .param("recipients", "mail@gematik.test")
                .param("expires", ZonedDateTime.now(ZoneId.of("Europe/Berlin")).plusMinutes(30).format(
                    DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH)))
                .header("Accept", "application/json")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.MULTIPART_FORM_DATA_VALUE)
        )
        .andExpect(handler().handlerType(KasApiRestController.class))
        .andExpect(handler().methodName("addAttachment"))
        .andExpect(status().is2xxSuccessful())
        .andExpect(content().contentType("application/json"));

    // TODO: Verify mdc has common name
  }

}

