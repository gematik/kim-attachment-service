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

import static de.gematik.kim.kas.service.auth.AuthStrategy.MAIL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.gematik.kim.kas.controller.FileController;
import de.gematik.kim.kas.controller.MaxMailSizeController;
import de.gematik.kim.kas.controller.UrlController;
import de.gematik.kim.kas.db.EntryRepository;
import de.gematik.kim.kas.exceptions.FileToLargeException;
import de.gematik.kim.kas.exceptions.InvalidEmailFoundException;
import de.gematik.kim.kas.exceptions.NotEnoughSpaceException;
import de.gematik.kim.kas.exceptions.TimeParseException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

@SpringBootTest(classes = FileSaveService.class)
class FileSaveServiceTest {

  private String messageId = "SomeMessageId";
  private MockMultipartFile testFile = new MockMultipartFile("attachment", "SomeName",
      MediaType.APPLICATION_OCTET_STREAM_VALUE, "SomeString".getBytes());
  private List<String> wrongFormatedEmailList = List.of("WrongFormattedMail.de",
      "rightFormatedMail@gematik.test", "another@wrongMail");
  private List<String> rightFormatedEmailList = List.of(
      "kimar-empfaenger-22@gem.kim.telematik-test",
      "onlyRight@gematik.test",
      "rightFormatedMail@gematik.test",
      "Some_right@gematik.test");
  private String expiredTime = ZonedDateTime.now(ZoneId.of("Europe/Paris"))
      .minusMinutes(30)
      .format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH));
  private String validTime = ZonedDateTime.now(ZoneId.of("Europe/Paris"))
      .plusMinutes(30)
      .format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss zzz", Locale.GERMAN));
  @MockBean
  private MaxMailSizeController maxMailSizeController;
  @MockBean
  private FileController fileController;
  @MockBean
  private EntryRepository er;
  @MockBean
  private UrlController urlController;
  @MockBean
  private AmClient amClient;
  @Autowired
  private FileSaveService underTest;

  @BeforeAll
  public static void setMDC() {
    MDC.put(MAIL, "mail@gematik.de");
  }

  @Test
  @SneakyThrows
  public void sendBigFileAndExpectFileToLargeException() {
    when(maxMailSizeController.getMaxMailSize()).thenReturn(0l);
    Exception ex = assertThrows(FileToLargeException.class,
        () -> underTest.saveFile(messageId, null, null, testFile));
    assertEquals("Payload to large", ex.getMessage());
  }

  @Test
  @SneakyThrows
  public void malFormattedRecipientMailAndExpectInvalidEmailFoundException() {
    when(maxMailSizeController.getMaxMailSize()).thenReturn(100000l);
    Exception ex = assertThrows(InvalidEmailFoundException.class,
        () -> underTest.saveFile(messageId, wrongFormatedEmailList, "", testFile));
    assertEquals("Invalid receiver email found: [WrongFormattedMail.de, another@wrongMail]",
        ex.getMessage());
  }

  @Test
  @SneakyThrows
  public void noExpiryDateDeliveredAndExpectTimeParseException() {
    when(maxMailSizeController.getMaxMailSize()).thenReturn(100000l);
    Exception ex = assertThrows(TimeParseException.class,
        () -> underTest.saveFile(messageId, rightFormatedEmailList, null, testFile));
    assertEquals("Could not parse: \"null\"", ex.getMessage());
  }

  @Test
  @SneakyThrows
  public void malFormatedExpiryDateDeliveredAndExpectTimeParseException() {
    when(maxMailSizeController.getMaxMailSize()).thenReturn(100000l);
    Exception ex = assertThrows(TimeParseException.class,
        () -> underTest.saveFile(messageId, rightFormatedEmailList, "12 5 20 14:67:54 MEZ",
            testFile));
    assertEquals("Could not parse: 12 5 20 14:67:54 MEZ", ex.getMessage());
  }

  @Test
  @SneakyThrows
  public void outdatedExpiryDateDeliveredAndExpectTimeParseException() {
    when(maxMailSizeController.getMaxMailSize()).thenReturn(100000l);
    Exception ex = assertThrows(TimeParseException.class,
        () -> underTest.saveFile(messageId, rightFormatedEmailList, expiredTime, testFile));

    assertTrue(ex.getMessage().startsWith("Already expired: "),
        ex.getMessage() + " should start with \"Already expired: \"");
  }

  @Test
  @SneakyThrows
  public void sendFileOverQuotaAndExpectNotEnoughSpaceException() {
    when(amClient.checkAndGetRemainingQuota(anyLong(), anyString())).thenThrow(
        NotEnoughSpaceException.class);
    when(maxMailSizeController.getMaxMailSize()).thenReturn(100000l);
    assertThrows(NotEnoughSpaceException.class,
        () -> underTest.saveFile(messageId, rightFormatedEmailList, validTime, testFile));
  }

  @Test
  @SneakyThrows
  public void validAddAttachmentBodyAndSuccess() {
    when(maxMailSizeController.getMaxMailSize()).thenReturn(100000l);
    underTest.saveFile(messageId, rightFormatedEmailList, validTime, testFile);
    verify(er, times(1)).save(any());
    verify(fileController, times(1)).storeFile(any(), any());
    verify(urlController, times(1)).getFullUrl(any(), any());
  }
}
