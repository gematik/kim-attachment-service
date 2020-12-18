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

package de.gematik.kim.kas.controller;

import de.gematik.kim.kas.db.Entry;
import de.gematik.kim.kas.db.EntryRepository;
import de.gematik.kim.kas.exceptions.CouldNotSaveException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.multipart.MultipartFile;

@Controller
@Slf4j
public class FileController {

    private String storagePath;
    private EntryRepository er;

    public FileController(@Value("${gematik.kim.kas.storage-path}") String storagePath, EntryRepository er) {
        this.storagePath = storagePath;
        this.er = er;
    }

    public String storeFile(byte[] file) throws CouldNotSaveException {
        String fileName = UUID.randomUUID().toString();
        while (er.existsByFileName(fileName)) {
            fileName = UUID.randomUUID().toString();
        }
        try {
            saveFile(file, storagePath + File.separator + fileName);
        } catch (FileNotFoundException ex) {
            log.error(ex.getMessage());
            throw new CouldNotSaveException("Not enough space on disk");
        } catch (IOException ex) {
            log.error(ex.getMessage());
            throw new CouldNotSaveException("Something went wrong while saving file.");
        }
        er.save(new Entry(fileName, LocalDateTime.now()));
        return fileName;
    }

    private void saveFile(byte[] data, String fileName) throws IOException {
        try (FileOutputStream stream = new FileOutputStream(fileName)) {
            stream.write(data);
        }
        log.info("File saved at {}", fileName);
    }


    public File getFile(String resource) throws FileNotFoundException {
        String filePath = storagePath + File.separator + resource;
        File file = new File(filePath);
        if (!file.exists()) {
            throw new FileNotFoundException("The requested file does not exist.");
        }
        log.info("Got file from {}", file.getAbsolutePath());
        return file;
    }

}
