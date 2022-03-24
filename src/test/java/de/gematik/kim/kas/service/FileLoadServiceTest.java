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

import static de.gematik.kim.kas.service.auth.AuthStrategy.MAIL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import de.gematik.kim.kas.controller.FileController;
import de.gematik.kim.kas.db.Entry;
import de.gematik.kim.kas.db.EntryRepository;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;
import org.assertj.core.util.Lists;
import org.jboss.logging.MDC;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.expression.AccessException;

@SpringBootTest(classes = FileLoadService.class)
class FileLoadServiceTest {

  public static final String TEST_OWNER_MAIL = "some@owner.de";
  public static final String TEST_FILE_NAME = "SomeFileName";
  @MockBean
  private FileController fileController;
  @MockBean
  private EntryRepository er;
  private static final String TEST_RECEIVER_MAIL = "some@gematik.test";

  @Autowired
  private FileLoadService underTest;

  @Test
  public void noEntryFoundAndExpectFileNotFoundException() {
    String fileName = TEST_FILE_NAME;
    when(er.findEntryByFileName(fileName)).thenReturn(Optional.empty());
    FileNotFoundException ex = assertThrows(FileNotFoundException.class,
        () -> underTest.loadFile(fileName, "someRecipient"));
    assertEquals("No entry found for SomeFileName", ex.getMessage());
  }

  @Test
  public void successfulGetFileAsReceiver() throws AccessException, IOException {
    String fileName = TEST_FILE_NAME;

    File f = new File(fileName);

    Entry entry = Entry.builder()
        .fileName(fileName)
        .owner(TEST_OWNER_MAIL)
        .recipients(Lists.list(TEST_RECEIVER_MAIL))
        .deleteTime(LocalDateTime.now().plusDays(30))
        .build();
    when(er.findEntryByFileName(fileName)).thenReturn(Optional.of(entry));
    when(fileController.getFile(fileName, entry.getOwner())).thenReturn(f);
    File loadedFile = underTest.loadFile(fileName, TEST_RECEIVER_MAIL);
    assertEquals(f.getName(), loadedFile.getName());
  }

  @Test
  public void successfulGetFileAsOwner() throws AccessException, IOException {
    String fileName = TEST_FILE_NAME;

    File f = new File(fileName);

    Entry entry = Entry.builder()
        .fileName(fileName)
        .owner(TEST_OWNER_MAIL)
        .recipients(new ArrayList<>())
        .deleteTime(LocalDateTime.now().plusDays(30))
        .build();
    when(er.findEntryByFileName(fileName)).thenReturn(Optional.of(entry));
    when(fileController.getFile(fileName, entry.getOwner())).thenReturn(f);
    File loadedFile = underTest.loadFile(fileName, TEST_OWNER_MAIL);
    assertEquals(f.getName(), loadedFile.getName());
  }

  @Test
  public void getRejectedIfRequesterIsNotListedInReceiver() {
    MDC.put(MAIL, TEST_RECEIVER_MAIL);
    String fileName = TEST_FILE_NAME;

    Entry entry = Entry.builder()
        .fileName(fileName)
        .owner(TEST_OWNER_MAIL)
        .recipients(new ArrayList<>())
        .deleteTime(LocalDateTime.now().plusDays(30))
        .build();
    when(er.findEntryByFileName(fileName)).thenReturn(Optional.of(entry));
    AccessException ex = assertThrows(AccessException.class,
        () -> underTest.loadFile(fileName, TEST_RECEIVER_MAIL));
    assertEquals(TEST_RECEIVER_MAIL + " is no allowed recipient!", ex.getMessage());
  }
}
