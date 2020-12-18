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

package de.gematik.kim.kas.controller;

import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;

@Controller
@Slf4j
public class CheckStorageController implements InitializingBean {

    @Value("${gematik.kim.kas.storage-path}")
    private String storagePath;

    @Autowired
    ApplicationContext context;

    @Override
    public void afterPropertiesSet() throws Exception {
        File file = new File(storagePath);
        file.mkdirs();
        if (!file.canWrite()) {
            log.error("Can not save file caused by permission denied!" + file.getAbsolutePath());
            SpringApplication.exit(context, () -> 1);
        }
    }
}
