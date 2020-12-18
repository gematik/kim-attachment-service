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

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.gematik.kim.kas.controller.FileController;
import de.gematik.kim.kas.controller.MaxMailSizeController;
import de.gematik.kim.kas.controller.UrlController;
import de.gematik.kim.kas.exceptions.CouldNotSaveException;
import java.io.File;
import java.io.FileNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@RunWith(SpringRunner.class)
@Slf4j
@WebMvcTest(KasApiRestController.class)
public class KasApiRestControllerTest {

    @MockBean
    private MaxMailSizeController maxMailSizeController;
    @MockBean
    private FileController fileController;
    @MockBean
    private UrlController urlController;
    @Autowired
    private MockMvc mvc;

    @Test
    public void callMaxMailSizeRestInterfaceAndCheckValueCorrect() throws Exception {
        Long expectedSize = 12345L;
        when(maxMailSizeController.getMaxMailSize()).thenReturn(expectedSize);

        mvc.perform(get("/v1.1/MaxMailSize"))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.MaxMailSize").value(expectedSize));
    }

    @Test
    public void addFileToStorageAndExpectIsCreated() throws Exception {
        byte[] fileData = "Something-with-sense".getBytes();
        String uuid = "Some-UUID";
        when(fileController.storeFile(eq(fileData))).thenReturn(uuid);
        when(urlController.getFullUrl(uuid, false)).thenReturn("http://localhost:8080/v1.1/" + uuid);
        mvc.perform(post("/v1.1/").content(fileData))
                .andExpect(status().isCreated())
                .andExpect(content().string("{\"Shared-Link\":\"http://localhost:8080/v1.1/" + uuid + "\"}"));
    }

    @Test
    public void fullStorageAndExpectInternalServerError() throws Exception {
        byte[] fileData = "Something-with-sense".getBytes();
        when(fileController.storeFile(eq(fileData))).thenThrow(new CouldNotSaveException("Not enough space on disk"));
        mvc.perform(post("/v1.1/").content(fileData))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void uploadEmptyFileAndExpectBadRequest() throws Exception {
        mvc.perform(post("/v1.1/")).andExpect(status().isBadRequest());
    }

    @Test
    public void downloadFileAndExpectOk() throws Exception {
        File f = new File(
                "." + File.separator + "src" + File.separator + "test" + File.separator + "resources" + File.separator
                        + "Test.txt");
        when(fileController.getFile("Test")).thenReturn(f);
        MvcResult result = mvc.perform(get("/v1.1/Test")
                .contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andReturn();
        assertEquals(HttpStatus.OK.value(), result.getResponse().getStatus());
        assertEquals(25, result.getResponse().getContentAsByteArray().length);
        assertEquals(MediaType.APPLICATION_OCTET_STREAM_VALUE, result.getResponse().getContentType());
        assertEquals("This is just a test file!", result.getResponse().getContentAsString());
    }

    @Test
    public void tryDownloadNotExistingFileAndExpectNotFound() throws Exception {
        when(fileController.getFile("NonExistingFile"))
                .thenThrow(new FileNotFoundException("The requested file does not exist."));
        MvcResult result = mvc.perform(get("/NonExistingFile")
                .contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andReturn();
        assertEquals(HttpStatus.NOT_FOUND.value(), result.getResponse().getStatus());
    }

}
