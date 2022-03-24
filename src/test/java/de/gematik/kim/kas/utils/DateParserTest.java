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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.gematik.kim.kas.exceptions.TimeParseException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

class DateParserTest {

  @Test
  @SneakyThrows
  void parseSuccessful() {
    List<String> formats = List.of(
        "dd MMM yy HH:mm:ss Z",
        "dd MMM yy HH:mm:ss zzz",
        "EEE, dd MMM yy HH:mm:ss Z",
        "EEE, dd MMM yy HH:mm:ss zzz",
        "EEE, dd MMM yy HH:mm Z",
        "EEE, dd MMM yy HH:mm zzz",
        "EEE, dd MMM yyyy HH:mm Z",
        "EEE, dd MMM yyyy HH:mm zzz",
        "EEE, dd MMM yyyy HH:mm:ss z",
        "EEE, dd MMM yy HH:mm:ss z",
        "EEE, dd MMM yy HH:mm z",
        "dd MMM yy HH:mm:ss z",
        "dd MMM yy HH:mm z"
    );
    List<Locale> locals = List.of(Locale.ENGLISH, Locale.GERMAN);
    List<String> times = new ArrayList<>();

    for (String f : formats) {
      for (Locale l : locals) {
        String timeString =
            ZonedDateTime.now(ZoneId.of("Europe/Paris"))
                .plus(90, ChronoUnit.DAYS)
                .format(DateTimeFormatter.ofPattern(f, l));
        times.add(timeString);
      }
    }
    for (String t : times) {
      DateParser.parse(t);
    }
  }

  @Test
  @SneakyThrows
  void parseMalFormattedAndExpectError() {
    String time = ZonedDateTime.now(ZoneId.of("Europe/Paris"))
        .plus(90, ChronoUnit.DAYS)
        .format(DateTimeFormatter.ofPattern("EEE, dd-MMM-yy HH:mm Z", Locale.ENGLISH));
    TimeParseException ex = assertThrows(TimeParseException.class, () -> DateParser.parse(time));

    assertEquals("Could not parse: " + time, ex.getMessage());
  }

}
