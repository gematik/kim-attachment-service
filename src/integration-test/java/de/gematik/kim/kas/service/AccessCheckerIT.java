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

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import de.gematik.kim.kas.KasApplication;
import de.gematik.kim.kas.controller.FileController;
import de.gematik.kim.kas.service.cron.AccessChecker;
import java.io.File;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.expression.AccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@Slf4j
@AutoConfigureMockMvc
@ActiveProfiles("it")
@TestInstance(Lifecycle.PER_CLASS)
@SpringBootTest(classes = KasApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AccessCheckerIT {

  public static final String RECIPIENT_HEADER = "recipient";
  public static final String RECIPIENT = "SomeRecipient@gematik.de";
  @MockBean
  private FileController fileController;
  @Autowired
  private MockMvc mvc;
  @Autowired
  private AccessChecker accessChecker;

  @Value("${gematik.kim.kas.max-attachment-requests}")
  public int MAX_ATT_REQUESTS;
  @Value("${gematik.kim.kas.version}")
  public String VERSION;
  @Value("${gematik.kim.kas.path-prefix}")
  private String PREFIX;
  @MockBean
  private AmClient amClient;
  @MockBean
  private FileLoadService fileLoadService;

  private MockHttpServletRequestBuilder request;


  @BeforeEach
  public void prepareRequest() throws FileNotFoundException, AccessException {
    File f = new File(
        "." + File.separator + "src" + File.separator + "test" + File.separator + "resources"
            + File.separator + "Test.txt");
    when(fileLoadService.loadFile(any(), any())).thenReturn(f);
    request = get("/" + PREFIX + "/" + VERSION + "/attachment/" + "Test")
        .header("authorization",
            "Basic " + new String(Base64.getEncoder().encode("username:password".getBytes(
                StandardCharsets.UTF_8))))
        .header(RECIPIENT_HEADER, RECIPIENT)
        .contentType(MediaType.APPLICATION_OCTET_STREAM);
  }

  @Test
  public void testReachAccessLimit() throws Exception {
    await().untilAsserted(() -> assertEquals(0, accessChecker.getUserAccessCount().size()));
    for (int i = 1; i < MAX_ATT_REQUESTS + 2; i++) {
      MvcResult result = mvc.perform(request).andReturn();
      assertEquals(
          (i > MAX_ATT_REQUESTS) ? HttpStatus.TOO_MANY_REQUESTS.value() : HttpStatus.OK.value(),
          result.getResponse().getStatus());
    }
  }

  @Test
  public void testReachAccessLimitAndReset() throws Exception {
    MvcResult result;
    await().untilAsserted(() -> assertEquals(0, accessChecker.getUserAccessCount().size()));
    for (int i = 1; i < MAX_ATT_REQUESTS + 2; i++) {
      result = mvc.perform(request).andReturn();
      assertEquals(
          (i > MAX_ATT_REQUESTS) ? HttpStatus.TOO_MANY_REQUESTS.value() : HttpStatus.OK.value(),
          result.getResponse().getStatus());
    }
    await().atMost(5, TimeUnit.SECONDS)
        .untilAsserted(() -> assertEquals(0, accessChecker.getUserAccessCount().size()));
    result = mvc.perform(request).andReturn();
    assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
  }
}


