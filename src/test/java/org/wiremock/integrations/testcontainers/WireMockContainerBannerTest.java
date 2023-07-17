/*
 * Copyright (C) 2023 WireMock Inc, Oleg Nenashev and all project contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wiremock.integrations.testcontainers;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WireMockContainerBannerTest {

    WireMockContainer wireMockContainer = new WireMockContainer(WireMockContainer.WIREMOCK_2_LATEST);

    @Test
    void bannerIsByDefaultDisabled() {
        // when
        wireMockContainer.configure();

        // then
        assertThat(wireMockContainer.getCommandParts())
                .contains("--disable-banner");
    }

    @Test
    void enableBanner() {
        // given
        wireMockContainer.withBanner();

        // when
        wireMockContainer.configure();

        // then
        assertThat(wireMockContainer.getCommandParts())
                .doesNotContain("--disable-banner");
    }

    @Test
    void disableBanner() {
        // given
        wireMockContainer.withoutBanner();

        // when
        wireMockContainer.configure();

        // then
        assertThat(wireMockContainer.getCommandParts())
                .contains("--disable-banner");
    }
}
