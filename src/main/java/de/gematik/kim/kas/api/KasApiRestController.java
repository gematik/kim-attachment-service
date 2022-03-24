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

import static org.springframework.http.HttpStatus.CREATED;

import de.gematik.kim.kas.api.response.ResponsesMap;
import de.gematik.kim.kas.model.AddAttachmentResponse;
import de.gematik.kim.kas.model.ErrorResponse;
import de.gematik.kim.kas.service.FileLoadService;
import de.gematik.kim.kas.service.FileSaveService;
import de.gematik.kim.kas.service.cron.AccessChecker;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.util.List;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/${gematik.kim.kas.path-prefix}/${gematik.kim.kas.version}")
@RequiredArgsConstructor
@Tag(name = "KAS - KOM LE Attachment Service",
    description = "Storage for attachments of KIM messages")
public class KasApiRestController {

  private final AccessChecker accessChecker;
  private final FileSaveService fileSaveService;
  private final FileLoadService fileLoadService;


  /**
   * Upload a document to KAS.
   *
   * @param messageID  Some id related to the attachment
   * @param recipients list of recipients who are allowed to get the data
   * @param expires    time when data should be deleted
   * @param attachment the actual data
   * @return JSON with url to the uploaded data.
   */
  @Operation(summary = "Add encrypted attachment generate link to it",
      method = "add_Attachment()",
      tags = {"Kas", "KIM", "KOM-LE", "attachment"})
  @ApiResponses(value = {
      @ApiResponse(responseCode = "201",
          description = "Created - attachment was added successfully to the return link",
          content = {@Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = AddAttachmentResponse.class)
          )}),
      @ApiResponse(responseCode = "400", description = "Bad Request"),
      @ApiResponse(responseCode = "401", description = "Authorization failed"),
      @ApiResponse(responseCode = "413", description = "Payload to large"),
      @ApiResponse(responseCode = "500", description = "Internal server error")
  })
  @PostMapping(value = "/attachment",
      consumes = {MediaType.MULTIPART_FORM_DATA_VALUE},
      produces = {MediaType.APPLICATION_JSON_VALUE})
  public ResponseEntity<AddAttachmentResponse> addAttachment(
      @RequestParam(value = "messageID", required = false) String messageID,
      @RequestParam(value = "recipients", required = false) List<String> recipients,
      @RequestParam(value = "expires", required = false) String expires,
      @Parameter(description = "file detail") @Valid @RequestPart("attachment") MultipartFile attachment) {
    log.info("Got file");
    String url;
    try {
      url = fileSaveService.saveFile(messageID, recipients, expires, attachment);
    } catch (Exception ex) {
      log.error("Add attachment failed -> {}", ex.getMessage());
      return new ResponseEntity(ErrorResponse.builder().message(ex.getMessage()).build(),
          ResponsesMap.map.get(ex.getClass()));
    }
    log.info("Response {}: {}", CREATED.value(), url);

    return new ResponseEntity<>(AddAttachmentResponse.builder().sharedLink(url).build(), CREATED);

  }

  /**
   * Download data that was uploaded to the KAS.
   *
   * @param attachmentId UUID of the data
   * @return Binary stream of the requested data
   */
  @Operation(summary = "Returns encrypted attachment from link",
      method = "read_Attachment()",
      tags = {"Kas", "KIM", "KOM-LE", "attachment"})
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200",
          description = "OK - Attachment was downloaded successfully",
          content = {@Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE)}),
      @ApiResponse(responseCode = "404", description = "Resources not found"),
      @ApiResponse(responseCode = "429", description = "Too many requests"),
      @ApiResponse(responseCode = "500", description = "Internal server error")
  })
  @GetMapping(value = "/attachment/{attachmentId}")
  public ResponseEntity<?> readAttachment(
      @Parameter(description = "Link-Reference auf den verschüsselten Anhang im Dienst", required = true) @PathVariable("attachmentId") String
          attachmentId,
      @Parameter(in = ParameterIn.HEADER) @RequestHeader(value = "recipient") String recipient) {
    if (!accessChecker.check(attachmentId)) {
      return new ResponseEntity<>(ErrorResponse.builder().message("Too many requests").build(),
          HttpStatus.TOO_MANY_REQUESTS);
    }
    log.info("Get file: {}", attachmentId);
    File file;
    try {
      file = fileLoadService.loadFile(attachmentId, recipient);
      log.info("Response 200: File found - {}", file.getAbsolutePath());
      MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
      headers.add(HttpHeaders.CONTENT_LENGTH, String.valueOf(Files.size(file.toPath())));
      return new ResponseEntity<>(new InputStreamResource(new FileInputStream(file)), headers,
          HttpStatus.OK);
    } catch (Exception ex) {
      log.error("Read attachment faild -> {}", ex.getMessage());
      return new ResponseEntity<>(ErrorResponse.builder().message(ex.getMessage()).build(),
          ResponsesMap.map.get(ex.getClass()));
    }
  }

}
