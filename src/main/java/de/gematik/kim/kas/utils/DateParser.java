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

import de.gematik.kim.kas.exceptions.TimeParseException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class DateParser {

  private static List<String> formats = List.of(
      "dd MMM yy HH:mm:ss Z",
      "dd MMM yy HH:mm Z",
      "EEE, dd MMM yy HH:mm:ss Z",
      "EEE, dd MMM yy HH:mm Z"
  );
  private static List<Locale> locals = List.of(Locale.ENGLISH, Locale.GERMAN);
  private static SimpleDateFormat parser;


  public static LocalDateTime parse(String t) throws TimeParseException {
    if (Objects.isNull(t)) {
      throw new TimeParseException("Could not parse: \"null\"");
    }
    for (String f : formats) {
      for (Locale l : locals) {
        try {
          parser = new SimpleDateFormat(f, l);
          return LocalDateTime.ofInstant(parser.parse(t).toInstant(), ZoneId.systemDefault());
        } catch (ParseException ex) {
          // Do nothing
        }
      }
    }

    throw new TimeParseException("Could not parse: " + t);
  }
}
