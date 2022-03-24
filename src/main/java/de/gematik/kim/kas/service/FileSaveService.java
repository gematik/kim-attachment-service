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

import de.gematik.kim.kas.controller.FileController;
import de.gematik.kim.kas.controller.MaxMailSizeController;
import de.gematik.kim.kas.controller.UrlController;
import de.gematik.kim.kas.db.Entry;
import de.gematik.kim.kas.db.EntryRepository;
import de.gematik.kim.kas.exceptions.CouldNotSaveException;
import de.gematik.kim.kas.exceptions.FileToLargeException;
import de.gematik.kim.kas.exceptions.InvalidEmailFoundException;
import de.gematik.kim.kas.exceptions.NotEnoughSpaceException;
import de.gematik.kim.kas.exceptions.TimeParseException;
import de.gematik.kim.kas.utils.DateParser;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponents;

@Slf4j
@Service
@AllArgsConstructor
public class FileSaveService {

  private final MaxMailSizeController maxMailSizeController;
  private final FileController fileController;
  private final EntryRepository er;
  private final UrlController urlController;
  private final AmClient amClient;

  @SuppressWarnings({"squid:S5843", "squid:S5998"})
  private final Pattern pattern = Pattern.compile(
      "^([a-zA-Z0-9_\\-\\.]+)@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.)|(([a-zA-Z0-9\\-]+\\.)+))([a-zA-Z\\-]+|[0-9]{1,3})(\\]?)$");

  @Transactional
  public String saveFile(String messageID, List<String> recipients, String expires,
      MultipartFile attachment)
      throws InvalidEmailFoundException, FileToLargeException, CouldNotSaveException, TimeParseException, NotEnoughSpaceException, IOException {

    String fileName = getUniqueFileName();
    doChecks(recipients, expires, attachment);
    amClient.checkAndGetRemainingQuota(attachment.getSize(), MDC.get(MAIL));

    er.save(Entry.builder()
        .fileName(fileName)
        .deleteTime(parseMailDateFormatToLocalDateTime(expires))
        .owner(MDC.get(MAIL))
        .recipients(recipients)
        .size(attachment.getSize())
        .build());

    fileController.storeFile(attachment.getBytes(), fileName);
    UriComponents currentUriRequest = ServletUriComponentsBuilder.fromCurrentRequest().build();
    String url = urlController.getFullUrl(fileName, currentUriRequest);
    return url;
  }


  private String getUniqueFileName() {
    String fileName = UUID.randomUUID().toString();
    while (er.existsByFileName(fileName)) {
      fileName = UUID.randomUUID().toString();
    }
    return fileName;
  }

  private void doChecks(List<String> recipients, String expires, MultipartFile attachment)
      throws FileToLargeException, InvalidEmailFoundException, TimeParseException, NotEnoughSpaceException, IOException {
    checkSize(attachment.getBytes());
    checkMails(recipients);
    checkDateFormat(expires);
  }

  private void checkSize(byte[] attachment) throws FileToLargeException {
    if (attachment.length > maxMailSizeController.getMaxMailSize()) {
      log.error("Error: Data too large " + attachment.length + " / "
          + maxMailSizeController.getMaxMailSize());
      throw new FileToLargeException("Payload to large");
    }
  }

  private void checkMails(List<String> recipients) throws InvalidEmailFoundException {
    List<String> check = recipients.stream().filter(m -> !pattern.matcher(m).matches()).collect(
        Collectors.toList());
    if (!check.isEmpty()) {
      throw new InvalidEmailFoundException("Invalid receiver email found: " + check);
    }
  }

  private void checkDateFormat(String expires) throws TimeParseException {
    LocalDateTime exp = DateParser.parse(expires);
    if (exp.isBefore(LocalDateTime.now())) {
      throw new TimeParseException(
          "Already expired: " + expires + " < " + LocalDateTime.now(
              ZoneId.systemDefault()));
    }
  }

  private LocalDateTime parseMailDateFormatToLocalDateTime(String stringDate)
      throws TimeParseException {
    LocalDateTime exp;
    try {
      exp = DateParser.parse(stringDate);
    } catch (Exception ex) {
      log.error("Could not parse Time: \"" + stringDate + "\"");
      throw new TimeParseException("Could not parse Time: \"" + stringDate + "\"");
    }
    return exp;
  }


}
