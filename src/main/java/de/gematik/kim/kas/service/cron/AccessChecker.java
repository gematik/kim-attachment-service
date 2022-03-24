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

import static de.gematik.kim.kas.service.auth.AuthStrategy.MAIL;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class AccessChecker {

  @Value("${gematik.kim.kas.time-to-keep-request-record}")
  private long keepRecordTime;
  @Value("${gematik.kim.kas.max-attachment-requests}")
  private long maxAttRequests;

  @Getter
  private final Map<String, ResourceAccessCount> userAccessCount = new HashMap<>();

  public boolean check(String resource) {
    String key = MDC.get(MAIL) + resource;
    if (!userAccessCount.containsKey(key)) {
      userAccessCount.put(key, new ResourceAccessCount(1, LocalDateTime.now()));
      return true;
    }
    ResourceAccessCount user = userAccessCount.get(key);
    int accessCount = user.getNumberOfAccess();
    if (accessCount < maxAttRequests) {
      user.setNumberOfAccess(accessCount + 1);
      user.setLastAccessTime(LocalDateTime.now());
      return true;
    }
    return false;
  }

  @Scheduled(cron = "${gematik.kim.kas.request-reset-time}")
  private void resetRequests() {
    for (Entry<String, ResourceAccessCount> entry : userAccessCount.entrySet()) {
      if (entry.getValue().getLastAccessTime()
          .isBefore(LocalDateTime.now().minus(keepRecordTime, ChronoUnit.MILLIS))) {
        userAccessCount.remove(entry.getKey());
      }
    }
  }

  @Data
  @AllArgsConstructor
  private class ResourceAccessCount {

    private int numberOfAccess;
    private LocalDateTime lastAccessTime;

  }
}
