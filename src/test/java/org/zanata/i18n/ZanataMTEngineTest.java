/*
 * Copyright 2017, Red Hat, Inc. and individual contributors as indicated by the
 * @author tags. See the copyright.txt file in the distribution for a full
 * listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA, or see the FSF
 * site: http://www.fsf.org.
 */
package org.zanata.i18n;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Carlos Munoz <a href="mailto:camunoz@redhat.com">camunoz@redhat.com</a>
 */
public class ZanataMTEngineTest {

    private static final String AUTH_USER = "Test-User";
    private static final String AUTH_TOKEN = "Test-Token";

    static WireMockServer mockServer;
    ZanataMTEngine engine;

    @BeforeAll
    public static void setupMockServer() {
        mockServer = new WireMockServer(options().dynamicPort());
        mockServer.start();

        // 200 response
        mockServer.stubFor(
            post(urlPathEqualTo("/api/document/translate"))
                .withHeader("X-Auth-User", equalTo(AUTH_USER))
                .withHeader("X-Auth-Token", equalTo(AUTH_TOKEN))
                .withQueryParam("targetLang", matching(".*"))
                .withRequestBody(matchingJsonPath("$.url"))
                .withRequestBody(matchingJsonPath("$.contents"))
            .willReturn(aResponse().withStatus(200)
                // This matches the response format for Zanata MT
                .withBody("{\"url\":\"http://redhat.com\",\"contents\":[{\"value\":\"Esta es la cadena traducida\",\"type\":\"text/plain\",\"metadata\":null}],\"locale\":\"es\",\"backendId\":\"MS\",\"warnings\":[]}")));

        // 500 error on the server
        mockServer.stubFor(
                post(urlPathEqualTo("/api/document/translate"))
                    .withHeader("X-Auth-User", equalTo(AUTH_USER))
                    .withHeader("X-Auth-Token", equalTo(AUTH_TOKEN))
                    .withQueryParam("targetLang", matching(".*"))
                    .withRequestBody(matchingJsonPath("$.url"))
                    .withRequestBody(matchingJsonPath("$.contents[?(@.value == 'exception')]"))
                .willReturn(aResponse().withStatus(500)));
    }

    @AfterAll
    public static void stopMockServer() {
        mockServer.stop();
    }

    @BeforeEach
    public void initialize() throws Exception {
        engine = new ZanataMTEngine("http://localhost:" + mockServer.port(), AUTH_USER, AUTH_TOKEN);
    }

    @Test
    public void exceptionWhenTranslating() throws Exception {
        assertThrows(Exception.class,
                () -> engine.translate("exception", "en-US", "es"));
    }

    @Test
    public void successfulTranslation() throws Exception {
        assertEquals("Esta es la cadena traducida",
                engine.translate("A Source string", "en-US", "es"));
    }

    // TODO Test timeouts

//    @Test
//    public void testReal() throws Exception {
//        String translatedString = new ZanataMTEngine(
//                "https://stage-zanata-mt.int.open.paas.redhat.com/", "test",
//                "test")
//                .translate("This is a test string", "en-US", "es");
//        System.out.println(translatedString);
//    }

}
