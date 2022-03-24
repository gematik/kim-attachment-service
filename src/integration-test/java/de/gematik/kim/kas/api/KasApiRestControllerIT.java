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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.gematik.kim.kas.KasApplication;
import de.gematik.kim.kas.filter.MdcFilter;
import de.gematik.kim.kas.model.AddAttachmentResponse;
import de.gematik.kim.kas.service.AmClient;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
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

@AutoConfigureMockMvc
@ActiveProfiles("it")
@SpringBootTest(classes = KasApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class KasApiRestControllerIT {

  public static final String RECIPIENT = "test@gematik.de";
  @Value("${gematik.kim.kas.version}")
  public String VERSION;
  @Value("${gematik.kim.kas.path-prefix}")
  private String prefix;
  @Value("${kim.kas.auth-test-mail}")
  private String testMail;
  @Autowired
  private MockMvc mvc;
  @Autowired
  private MdcFilter mdcFilter;
  @Autowired
  private ObjectMapper mapper;
  @MockBean
  private AmClient amClient;

  @Test
  void testCorrectFileSize() throws Exception {
    byte[] fileMock = "SomethingWithSense".getBytes();

    AtomicReference<String> link = new AtomicReference<>();
    mvc.perform(
            multipart("/{prefix}/{version}/attachment", prefix, VERSION)
                .file("attachment", fileMock)
                .param("messageID", "SomeMessageID")
                .param("recipients", RECIPIENT)
                .param("expires",
                    ZonedDateTime.now(ZoneId.of("Europe/Paris")).plus(30, ChronoUnit.MINUTES).format(
                            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z", Locale.GERMAN)))
                .header("Accept", "application/json")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.MULTIPART_FORM_DATA_VALUE)
        )
        .andExpect(status().is2xxSuccessful())
        .andDo(c -> link.set(
            mapper.readValue(c.getResponse().getContentAsString(), AddAttachmentResponse.class)
                .getSharedLink()));

    mdcFilter.setMailToMDC(testMail);

    mvc.perform(get(link.get())
            .header("recipient", RECIPIENT))
        .andExpect(status().is2xxSuccessful())
        .andExpect(header().exists(HttpHeaders.CONTENT_LENGTH))
        .andExpect(header().string(HttpHeaders.CONTENT_LENGTH, String.valueOf(fileMock.length)));
  }

}
