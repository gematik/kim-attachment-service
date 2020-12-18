/*
 * Copyright (c) 2020 gematik GmbH
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.gematik.kim.kas.api;

import de.gematik.kim.kas.controller.FileController;
import de.gematik.kim.kas.controller.MaxMailSizeController;
import de.gematik.kim.kas.controller.UrlController;
import de.gematik.kim.kas.exceptions.CouldNotSaveException;
import de.gematik.kim.kas.model.AddAttachmentResponse;
import de.gematik.kim.kas.model.MaxMailSizeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "KAS - KOM LE Attachment Service",
        description = "Storage for attachments of KIM messages")
public class KasApiRestController {

    private final MaxMailSizeController maxMailSizeController;
    private final FileController fileController;
    private final UrlController urlController;

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
            @ApiResponse(responseCode = "400", description = "Bad Request")
    })
    @PostMapping(value = "/v1.1",
            consumes = {MediaType.APPLICATION_OCTET_STREAM_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<AddAttachmentResponse> addAttachment(
            @Parameter(description = "file data") @RequestBody byte[] requestEntity) {
        log.info("Got file");
        String fileName;
        try {
            fileName = fileController.storeFile(requestEntity);
        } catch (CouldNotSaveException ex) {
            log.error(ex.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        String protocol = ServletUriComponentsBuilder.fromCurrentRequest().build().getScheme();
        boolean isHttps = "https".equals(protocol);
        String url = urlController.getFullUrl(fileName, isHttps);
        AddAttachmentResponse attachmentResponse = AddAttachmentResponse.builder()
                .sharedLink(url)
                .build();
        ResponseEntity<AddAttachmentResponse> response = new ResponseEntity<>(attachmentResponse, HttpStatus.CREATED);
        log.info("Response {}: {}", response.getStatusCodeValue(), url);

        return response;
    }

    @Operation(summary = "Returns encrypted attachment from link",
            method = "read_Attachment()",
            tags = {"Kas", "KIM", "KOM-LE", "attachment"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "OK - Attachment was downloaded successfully",
                    content = {@Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE)}),
            @ApiResponse(responseCode = "404", description = "Resources not found")
    })
    @GetMapping(value = "/v1.1/{Ressource}",
            produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE})
    public ResponseEntity<Object> readAttachment(
            @Parameter(description = "Link-Referenz auf den verschüsselten Anhang im Dienst", required = true) @PathVariable("Ressource") String ressource) {
        log.info("Get file: {}", ressource);
        try {
            File file = fileController.getFile(ressource);
            log.info("Response 200: File found - {}", file.getAbsolutePath());

            return new ResponseEntity<>(new InputStreamResource(new FileInputStream(file)), HttpStatus.OK);
        } catch (FileNotFoundException ex) {
            log.error("Response 404: File found - {}", ex.getMessage());

            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @Operation(summary = "Returns maximum size in bytes of a complete KIM message including all attachments (base64 coded)",
            method = "read_MaxMailSize()",
            tags = {"Kas", "KIM", "KOM-LE", "attachment"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK - Max mail size in bytes was returned",
                    content = {@Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MaxMailSizeResponse.class))
                    }
            )
    })
    @GetMapping(value = "/v1.1/MaxMailSize", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<MaxMailSizeResponse> readMaxMailSize() {
        Long maxMailSize = maxMailSizeController.getMaxMailSize();
        MaxMailSizeResponse maxMailSizeResponse = MaxMailSizeResponse.builder()
                .maxMailSize(maxMailSize)
                .build();

        log.info("MaxMailSize requested and {} answered", maxMailSize);

        return new ResponseEntity<>(maxMailSizeResponse, HttpStatus.OK);
    }
}
