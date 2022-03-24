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

import de.gematik.kim.kas.controller.FileController;
import de.gematik.kim.kas.db.Entry;
import de.gematik.kim.kas.db.EntryRepository;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Optional;
import javax.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.expression.AccessException;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class FileLoadService {

  private final FileController fileController;
  private final EntryRepository er;

  @Transactional
  public File loadFile(String resource, String recipient)
      throws FileNotFoundException, AccessException {
    Optional<Entry> optionalEntry = er.findEntryByFileName(resource);
    if (optionalEntry.isEmpty()) {
      throw new FileNotFoundException("No entry found for " + resource);
    }
    Entry entry = optionalEntry.get();
    entry.getRecipients().add(entry.getOwner());
    if (!entry.getRecipients().contains(recipient)) {
      throw new AccessException(recipient + " is no allowed recipient!");
    }
    File file = fileController.getFile(resource, entry.getOwner());
    entry.setNumberOfDownloads(entry.getNumberOfDownloads() + 1);

    return file;
  }

}


