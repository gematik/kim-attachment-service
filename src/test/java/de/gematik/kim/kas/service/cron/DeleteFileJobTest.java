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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.gematik.kim.kas.db.Entry;
import de.gematik.kim.kas.db.EntryRepository;
import de.gematik.kim.kas.service.AmClient;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Slf4j
@ExtendWith(MockitoExtension.class)
public class DeleteFileJobTest {

  public static final String OWNER = "owner_email@gematik.test";
  public static final String OWNER_PATH = "owner_emailATgematikPUNKTtest";
  private final static String storagePath = "target/test-storage/";
  private final static long keepIdTime = 31536000000L;
  @Mock
  private EntryRepository entryRepository;
  @Mock
  private AmClient amClient;
  private DeleteFileJob deleteFileJob;

  @BeforeEach
  public void setUp() {
    new File(storagePath + File.separator + OWNER_PATH).mkdirs();
    deleteFileJob = new DeleteFileJob(entryRepository, amClient, storagePath, keepIdTime);
  }

  @Test
  public void scheduleFileDeletionAndCheckFileIsDeleted() throws IOException {
    String filename = "scheduleFileDeletionAndCheckFileIsDeletedTestFile";
    File newFile = new File(storagePath + File.separator + OWNER_PATH + File.separator + filename);
    if (!newFile.exists()) {
      assertTrue(newFile.createNewFile(), "Could not create file");
      assertTrue(newFile.exists(), "File was not created: " + newFile.getAbsolutePath());
    }
    Entry entry = new Entry(filename, LocalDateTime.now().minus(2, ChronoUnit.MILLIS), OWNER,
        List.of());
    when(entryRepository.findByDeleteTimeBeforeAndDeletedFalse(any())).thenReturn(List.of(entry));
    deleteFileJob.deleteOldFiles();
    assertFalse(newFile.exists(), "File was not deleted: " + newFile.getAbsolutePath());
  }

  @Test
  public void scheduleIdDeletionAndCheckMethodIsCalled() {
    Entry entry = new Entry("fake", LocalDateTime.now().minus(keepIdTime + 2, ChronoUnit.MILLIS),
        OWNER, List.of());
    when(entryRepository.findByCreatedBefore(any())).thenReturn(List.of(entry));
    deleteFileJob.deleteOldFiles();
    verify(entryRepository).delete(eq(entry));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void scheduleDeleteFileKeepEntry() throws IOException {
    LocalDateTime requestTime = LocalDateTime.now().minus(2, ChronoUnit.MILLIS);
    String filename = "scheduleDeleteFileKeepEntryTestFile";
    File newFile = new File(storagePath + File.separator + OWNER_PATH + File.separator + filename);
    if (!newFile.exists()) {
      assertTrue(newFile.createNewFile(), "Could not create file");
      assertTrue(newFile.exists(), "File was not created: " + newFile.getAbsolutePath());
    }
    Entry entry = new Entry(filename, requestTime.minus(2, ChronoUnit.MILLIS), OWNER, List.of());
    when(entryRepository.findByDeleteTimeBeforeAndDeletedFalse(any())).thenReturn(List.of(entry));
    when(entryRepository.findByCreatedBefore(any())).thenReturn(Collections.emptyList());
    deleteFileJob.deleteOldFiles();
    assertFalse(newFile.exists(), "File was not deleted: " + newFile.getAbsolutePath());
    verify(entryRepository, never()).delete(eq(entry));
  }
}
