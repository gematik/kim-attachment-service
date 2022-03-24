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

package de.gematik.kim.kas.service.cron;

import static de.gematik.kim.kas.utils.StringFormater.formatMailToPath;

import de.gematik.kim.kas.db.Entry;
import de.gematik.kim.kas.db.EntryRepository;
import de.gematik.kim.kas.service.AmClient;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import javax.naming.CommunicationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class DeleteFileJob {

  private final EntryRepository entryRepository;
  private final String storagePath;
  private final long keepIdTime;
  private final AmClient amClient;

  public DeleteFileJob(EntryRepository entryRepository,
      AmClient amClient,
      @Value("${gematik.kim.kas.storage-path}") String storagePath,
      @Value("${gematik.kim.kas.time-to-keep-id}") long keepIdTime) {
    this.entryRepository = entryRepository;
    this.amClient = amClient;
    this.storagePath = storagePath;
    this.keepIdTime = keepIdTime;
  }

  @Scheduled(cron = "${gematik.kim.kas.cleantime}")
  public void deleteOldFiles() {
    log.info("Going to delete!");
    entryRepository.findByDeleteTimeBeforeAndDeletedFalse(LocalDateTime.now())
        .stream().map(this::deleteFile).distinct().forEach(this::releaseQuota);
    entryRepository.findByCreatedBefore(LocalDateTime.now().minus(keepIdTime, ChronoUnit.MILLIS))
        .forEach(this::deleteEntry);
  }

  private String deleteFile(Entry entry) {
    log.info("Found {}", entry);
    try {
      Files.deleteIfExists(Paths.get(
          storagePath + File.separator + formatMailToPath(entry.getOwner()) + File.separator
              + entry.getFileName()));
      entry.setDeleted(true);
      entryRepository.save(entry);
      log.info("Deleted file: {}", entry);
    } catch (IOException ex) {
      log.error("Could not delete file: {}", entry);
    }
    return entry.getOwner();
  }

  private void releaseQuota(String owner) {
    List<Entry> notDeletedEntries = entryRepository.findEntriesByOwnerAndDeletedFalse(owner);
    try {
      amClient.releaseQuota(
          notDeletedEntries.stream()
              .map(Entry::getSize)
              .reduce(0l, Long::sum)
          , owner);
    } catch (CommunicationException ex) {
      log.error(ex.getMessage());
    }
  }

  private void deleteEntry(Entry entry) {
    entryRepository.delete(entry);
    log.info("Deleted entry: {}", entry);
  }
}
