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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.gematik.kim.kas.configs.InterceptorConfig;
import de.gematik.kim.kas.exceptions.CouldNotSaveException;
import de.gematik.kim.kas.exceptions.FileToLargeException;
import de.gematik.kim.kas.exceptions.TimeParseException;
import de.gematik.kim.kas.filter.BaseAuthFilter;
import de.gematik.kim.kas.service.FileLoadService;
import de.gematik.kim.kas.service.FileSaveService;
import de.gematik.kim.kas.service.cron.AccessChecker;
import de.gematik.kim.kas.service.cron.DeleteFileJob;
import java.io.File;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.expression.AccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@Slf4j
@ExtendWith(SpringExtension.class)
@WebMvcTest(KasApiRestController.class)
class KasApiRestControllerTest {

  public static final String RECIPIENT = "SomeRecipient@gematik.de";
  public static final String RECIPIENTS_HEADER = "recipient";
  @Value("${gematik.kim.kas.version}")
  public String VERSION;
  @Value("${gematik.kim.kas.path-prefix}")
  private String prefix;

  @MockBean
  private DeleteFileJob deleteFileJob;
  @MockBean
  private InterceptorConfig interceptorConfig;
  @MockBean
  private BaseAuthFilter baseAuthFilter;
  @MockBean
  private FileSaveService fileSaveService;
  @MockBean
  private FileLoadService fileLoadService;
  @MockBean
  private AccessChecker accessChecker;
  @Autowired
  private MockMvc mvc;

  @BeforeEach
  public void prepare() {
    when(accessChecker.check(any())).thenReturn(true);
  }


  @Test
  void addFileToStorageAndExpectIsCreated() throws Exception {
    byte[] fileData = "Something-with-sense".getBytes();
    String uuid = "Some-UUID";
    when(fileSaveService.saveFile(any(), any(), any(), any())).thenReturn(
        "http://localhost:8080/" + prefix + "/" + VERSION + "/attachment/" + uuid);

    mvc.perform(multipart("/" + prefix + "/" + VERSION + "/attachment")
            .file("attachment", fileData)
            .header("authorization", "Basic " + new String(
                Base64.getEncoder().encode("username:password".getBytes(StandardCharsets.UTF_8))))
            .header(HttpHeaders.CONTENT_TYPE, MediaType.MULTIPART_FORM_DATA_VALUE))
        .andExpect(status().isCreated())
        .andExpect(content().string(
            "{\"sharedLink\":\"http://localhost:8080/" + prefix + "/" + VERSION + "/attachment/"
                + uuid + "\"}"));
  }


  @Test
  void fullStorageAndExpectInternalServerError() throws Exception {
    byte[] fileData = "Something-with-sense".getBytes();
    when(fileSaveService.saveFile(any(), any(), any(), any())).thenThrow(
        new CouldNotSaveException("Not enough space on disk"));

    mvc.perform(multipart("/" + prefix + "/" + VERSION + "/attachment")
            .file("attachment", fileData)
            .header("authorization", "Basic " + new String(
                Base64.getEncoder().encode("username:password".getBytes(StandardCharsets.UTF_8))))
            .header(HttpHeaders.CONTENT_TYPE, MediaType.MULTIPART_FORM_DATA_VALUE))
        .andExpect(status().isInternalServerError());
  }

  @Test
  void fileToBigAndExpectPayloadToLarge() throws Exception {
    byte[] fileData = "Something-with-sense".getBytes();
    when(fileSaveService.saveFile(any(), any(), any(), any())).thenThrow(
        new FileToLargeException("File to large"));

    mvc.perform(multipart("/" + prefix + "/" + VERSION + "/attachment")
            .file("attachment", fileData)
            .header("authorization", "Basic " + new String(
                Base64.getEncoder().encode("username:password".getBytes(StandardCharsets.UTF_8))))
            .header(HttpHeaders.CONTENT_TYPE, MediaType.MULTIPART_FORM_DATA_VALUE))
        .andExpect(status().isPayloadTooLarge());
  }


  @Test
  void uploadEmptyFileAndExpectBadRequest() throws Exception {
    when(fileSaveService.saveFile(any(), any(), any(), any())).thenThrow(
        new IllegalArgumentException("No data found"));
    mvc.perform(post("/" + prefix + "/" + VERSION + "/attachment")
            .header("authorization", "Basic " + new String(
                Base64.getEncoder().encode("username:password".getBytes(StandardCharsets.UTF_8))))
            .header(HttpHeaders.CONTENT_TYPE, MediaType.MULTIPART_FORM_DATA_VALUE))
        .andExpect(status().isBadRequest());
  }

  @Test
  void invalidTimeAndExpectBadRequest() throws Exception {
    when(fileSaveService.saveFile(any(), any(), any(), any())).thenThrow(
        new TimeParseException("Problem with parsing expiry date"));
    mvc.perform(post("/" + prefix + "/" + VERSION + "/attachment")
            .header("authorization", "Basic " + new String(
                Base64.getEncoder().encode("username:password".getBytes(StandardCharsets.UTF_8))))
            .header(HttpHeaders.CONTENT_TYPE, MediaType.MULTIPART_FORM_DATA_VALUE))
        .andExpect(status().isBadRequest());
  }

  @Test
  void downloadFileAndExpectOk() throws Exception {
    File f = new File(
        "." + File.separator + "src" + File.separator + "test" + File.separator + "resources"
            + File.separator
            + "Test.txt");
    when(fileLoadService.loadFile(eq("Test"), any())).thenReturn(f);

    MvcResult result = mvc.perform(get("/" + prefix + "/" + VERSION + "/attachment/" + "Test")
            .header("authorization", "Basic " + new String(
                Base64.getEncoder().encode("username:password".getBytes(StandardCharsets.UTF_8))))
            .header(HttpHeaders.CONTENT_TYPE, MediaType.MULTIPART_FORM_DATA_VALUE)
            .header(RECIPIENTS_HEADER, RECIPIENT))
        .andReturn();
    assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
    assertEquals(25, result.getResponse().getContentAsByteArray().length);
    assertEquals("This is just a test file!", result.getResponse().getContentAsString());
  }

  @Test
  void tryDownloadNotExistingFileAndExpectNotFound() throws Exception {
    when(fileLoadService.loadFile(eq("NonExistingFile"), any()))
        .thenThrow(new FileNotFoundException("The requested file does not exist."));

    MvcResult result = mvc.perform(
            get("/" + prefix + "/" + VERSION + "/attachment/NonExistingFile")
                .header("authorization", "Basic " + new String(
                    Base64.getEncoder().encode("username:password".getBytes(StandardCharsets.UTF_8))))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.MULTIPART_FORM_DATA_VALUE)
                .header(RECIPIENTS_HEADER, RECIPIENT))
        .andReturn();
    assertEquals(HttpStatus.NOT_FOUND.value(), result.getResponse().getStatus());
  }

  @Test
  void tryDownloadFileWithNotValidRecipientAndExpectUnauthorized() throws Exception {
    File f = new File(
        "." + File.separator + "src" + File.separator + "test" + File.separator + "resources"
            + File.separator
            + "Test.txt");
    when(fileLoadService.loadFile(eq("Test"), any())).thenThrow(
        new AccessException("Mail ist not allowed"));

    mvc.perform(get("/" + prefix + "/" + VERSION + "/attachment/" + "Test")
            .header("authorization", "Basic " + new String(
                Base64.getEncoder().encode("username:password".getBytes(StandardCharsets.UTF_8))))
            .header(HttpHeaders.CONTENT_TYPE, MediaType.MULTIPART_FORM_DATA_VALUE)
            .header(RECIPIENTS_HEADER, RECIPIENT))
        .andExpect(status().isUnauthorized())
        .andExpect(content().string("{\"message\":\"Mail ist not allowed\"}"));
  }


}

