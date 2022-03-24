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

package de.gematik.kim.kas.utils;

import java.util.LinkedList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.trace.http.HttpTrace;
import org.springframework.boot.actuate.trace.http.HttpTraceRepository;

@Slf4j
public class KasHttpTraceRepository implements HttpTraceRepository {

  @Value("${gematik.kim.kas.http-log-capacity}")
  private int capacity;

  private LinkedList<HttpTrace> traces = new LinkedList<>();

  @Override
  public List<HttpTrace> findAll() {
    return traces;
  }

  @Override
  public void add(HttpTrace trace) {
    log.info(
        "Request:: uri: " + trace.getRequest().getUri() + " - method: " + trace.getRequest()
            .getMethod() + " - headers: " + trace.getRequest()
            .getHeaders());
    log.info("Response:: status: " + trace.getResponse().getStatus() + " - headers: "
        + trace.getRequest().getHeaders());
    while (traces.size() >= capacity) {
      traces.removeFirst();
    }
    traces.addLast(trace);
  }
}
