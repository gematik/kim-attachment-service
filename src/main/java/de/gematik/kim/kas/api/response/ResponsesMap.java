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

package de.gematik.kim.kas.api.response;

import de.gematik.kim.kas.exceptions.CouldNotSaveException;
import de.gematik.kim.kas.exceptions.FileToLargeException;
import de.gematik.kim.kas.exceptions.InvalidEmailFoundException;
import de.gematik.kim.kas.exceptions.NotEnoughSpaceException;
import de.gematik.kim.kas.exceptions.TimeParseException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.springframework.expression.AccessException;
import org.springframework.http.HttpStatus;

public class ResponsesMap {

  public final static Map<Class, HttpStatus> map = new HashMap<>() {{
    put(CouldNotSaveException.class, HttpStatus.INTERNAL_SERVER_ERROR);
    put(IOException.class, HttpStatus.INTERNAL_SERVER_ERROR);
    put(FileToLargeException.class, HttpStatus.PAYLOAD_TOO_LARGE);
    put(InvalidEmailFoundException.class, HttpStatus.BAD_REQUEST);
    put(IllegalArgumentException.class, HttpStatus.BAD_REQUEST);
    put(TimeParseException.class, HttpStatus.BAD_REQUEST);
    put(FileNotFoundException.class, HttpStatus.NOT_FOUND);
    put(AccessException.class, HttpStatus.UNAUTHORIZED);
    put(NotEnoughSpaceException.class, HttpStatus.INSUFFICIENT_STORAGE);
  }};

}
