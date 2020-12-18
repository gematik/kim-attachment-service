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

package de.gematik.kim.kas;

import java.security.Security;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jsse.provider.BouncyCastleJsseProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@Slf4j
@EnableScheduling
public class KasApplication {

    public static void main(String[] args) {
        setupSecurityProvider();

        new SpringApplication(KasApplication.class).run(args);
    }

    private static void setupSecurityProvider() {
        // Disabled by default
        // see https://www.java.com/en/configure_crypto.html
        System.setProperty("jdk.tls.namedGroups", "secp256r1, secp384r1, brainpoolP256r1, brainpoolP384r1, brainpoolP512r1");

        Security.insertProviderAt(new BouncyCastleJsseProvider(), 1);
        Security.insertProviderAt(new BouncyCastleProvider(), 2);

        // Setup default KeyManagerFactory algorithm so that BC is used as a default
        // otherwise ECC brainpool certificates are not loaded properly
        Security.setProperty("ssl.KeyManagerFactory.algorithm", "PKIX");
    }
}
