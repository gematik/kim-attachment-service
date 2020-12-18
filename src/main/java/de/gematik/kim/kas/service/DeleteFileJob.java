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

package de.gematik.kim.kas.service;

import de.gematik.kim.kas.db.Entry;
import de.gematik.kim.kas.db.EntryRepository;
import java.io.File;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class DeleteFileJob {

    @Autowired
    EntryRepository entryRepository;
    @Value("${gematik.kim.kas.storage-path}")
    private String storagePath;
    @Value("${gematik.kim.kas.time-to-keep-file}")
    private long keepFileTime;
    @Value("${gematik.kim.kas.time-to-keep-id}")
    private long keepIdTime;

    @Scheduled(cron = "${gematik.kim.kas.cleantime}")
    public void deleteOldFiles() {
        entryRepository.findByCreatedBefore(LocalDateTime.now().minus(keepFileTime, ChronoUnit.MILLIS))
            .forEach(this::deleteFile);
        entryRepository.findByCreatedBefore(LocalDateTime.now().minus(keepIdTime, ChronoUnit.MILLIS))
            .forEach(this::deleteEntry);
    }

    private void deleteFile(Entry entry) {
        if (!entry.isDeleted()) {
            if (new File(storagePath + File.separator + entry.getFileName()).delete()) {
                entry.setDeleted(true);
                entryRepository.save(entry);
                log.info("Deleted file: {}", entry);
            } else {
                log.error("Could not delete file: {}", entry);
            }
        }
    }

    private void deleteEntry(Entry entry) {
        entryRepository.delete(entry);
        log.info("Deleted entry: {}", entry);
    }
}
