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

package de.gematik.kim.kas.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(Include.NON_NULL)
public class KasLogEvent {

  @JsonProperty("Url")
  private String url;

  @JsonProperty("Mandant")
  private String mandant;

  @JsonProperty("FileSize")
  private Long fileSize;

  @JsonProperty("MaxMailSize")
  private Long maxMailSize;

  @JsonProperty("Quota")
  private Long quota;

  @JsonProperty("RemainingQuota")
  private Long remainingQuota;

  @JsonProperty("RecipientList")
  private List<String> recipientList;

  @JsonProperty("NumberOfDownloads")
  private Integer numberOfDownloads;

}
