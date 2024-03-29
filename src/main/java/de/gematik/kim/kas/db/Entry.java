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

package de.gematik.kim.kas.db;

import java.time.LocalDateTime;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Entry {

  public Entry(
      @NotNull String fileName,
      @NotNull LocalDateTime deleteTime,
      @NotNull String owner,
      @NotNull List<String> recipients) {
    this.fileName = fileName;
    this.deleteTime = deleteTime;
    this.owner = owner;
    this.recipients = recipients;
    this.created = LocalDateTime.now();
  }

  @Id
  @GeneratedValue
  Long id;

  @NotNull
  @Column(unique = true)
  String fileName;

  @NotNull
  String owner;

  @NotNull
  LocalDateTime deleteTime;

  @ElementCollection
  List<String> recipients;

  @NotNull
  int numberOfDownloads;

  @NotNull
  boolean deleted;

  @NotNull
  LocalDateTime created;

  @NotNull
  Long size;

}

