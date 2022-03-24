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

package de.gematik.kim.kas.controller;

import static de.gematik.kim.kas.service.auth.AuthStrategy.MAIL;
import static de.gematik.kim.kas.utils.StringFormater.formatMailToPath;

import de.gematik.kim.kas.exceptions.CouldNotSaveException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;

@Controller
@Slf4j
public class FileController {

  private final String storagePath;

  public FileController(@Value("${gematik.kim.kas.storage-path}") String storagePath) {
    this.storagePath = storagePath;
  }

  public void storeFile(byte[] file, String fileName) throws CouldNotSaveException {
    try {
      String userPath = formatMailToPath(MDC.get(MAIL));
      checkPath(storagePath, userPath);
      saveFile(file, storagePath + File.separator + userPath + File.separator + fileName);
    } catch (FileNotFoundException ex) {
      log.error(ex.getMessage());
      throw new CouldNotSaveException("Not enough space on disk");
    } catch (IOException ex) {
      log.error(ex.getMessage());
      throw new CouldNotSaveException("Something went wrong while saving file.");
    }
  }

  public File getFile(String resource, String owner) throws FileNotFoundException {
    String filePath =
        storagePath + File.separator + formatMailToPath(owner) + File.separator + resource;
    File file = new File(filePath);
    if (!file.exists()) {
      throw new FileNotFoundException("The requested file does not exist.");
    }
    log.info("Got file from {}", file.getAbsolutePath());
    return file;
  }

  private void checkPath(String storagePath, String userPath) throws IOException {
    File file = new File(storagePath + File.separator + userPath);
    if (file.exists()) {
      return;
    }
    if (!file.mkdir()) {
      throw new IOException("Could not create path for user: " + userPath);
    }
  }

  private void saveFile(byte[] data, String fileName) throws IOException {
    try (FileOutputStream stream = new FileOutputStream(fileName)) {
      stream.write(data);
    }
    log.info("File saved at {}", fileName);
  }


}
