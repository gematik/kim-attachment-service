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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.gematik.kim.kas.db.Entry;
import de.gematik.kim.kas.db.EntryRepository;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class DeleteFileJobTest {

    private final String storagePath = "target/test-storage/";
    private final long keepFileTime = 7776000000L;
    private final long keepIdTime = 31536000000L;
    @Mock
    private EntryRepository entryRepository;
    @Resource
    @InjectMocks
    private DeleteFileJob deleteFileJob;

    @Before
    public void setUp() {
        ReflectionTestUtils.setField(deleteFileJob, "keepFileTime", keepFileTime);
        ReflectionTestUtils.setField(deleteFileJob, "keepIdTime", keepIdTime);
        ReflectionTestUtils.setField(deleteFileJob, "storagePath", storagePath);
        new File(storagePath).mkdirs();
    }

    @Test
    public void scheduleFileDeletionAndCheckFileIsDeleted() throws IOException {
        String filename = "toDelete";
        File newFile = new File(storagePath + filename);
        assertTrue("Could not create file", newFile.createNewFile());
        assertTrue("File was not created: " + newFile.getAbsolutePath(), newFile.exists());
        Entry entry = new Entry(filename, LocalDateTime.now().minus(keepFileTime + 2, ChronoUnit.MILLIS));
        when(entryRepository.findByCreatedBefore(any())).thenReturn(List.of(entry));
        deleteFileJob.deleteOldFiles();
        assertFalse("File was not deleted: " + newFile.getAbsolutePath(), newFile.exists());
    }

    @Test
    public void scheduleIdDeletionAndCheckMethodIsCalled() {
        Entry entry = new Entry("fake", LocalDateTime.now().minus(keepIdTime + 2, ChronoUnit.MILLIS));
        when(entryRepository.findByCreatedBefore(any())).thenReturn(List.of(entry));
        deleteFileJob.deleteOldFiles();
        verify(entryRepository).delete(eq(entry));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void scheduleDeleteFileKeepEntry() throws IOException {
        LocalDateTime requestTime = LocalDateTime.now().minus(keepFileTime, ChronoUnit.MILLIS);
        String filename = "toDelete";
        File newFile = new File(storagePath + filename);
        System.out.println(newFile.getAbsolutePath());
        assertTrue("Could not create file", newFile.createNewFile());
        assertTrue("File was not created: " + newFile.getAbsolutePath(), newFile.exists());
        Entry entry = new Entry(filename, requestTime.minus(keepFileTime + 2, ChronoUnit.MILLIS));
        when(entryRepository.findByCreatedBefore(any()))
                .thenReturn(List.of(entry), Collections.emptyList());
        deleteFileJob.deleteOldFiles();
        assertFalse("File was not deleted: " + newFile.getAbsolutePath(), newFile.exists());
        verify(entryRepository, never()).delete(eq(entry));
    }
}
