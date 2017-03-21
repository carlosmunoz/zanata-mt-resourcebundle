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

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author Carlos Munoz <a href="mailto:camunoz@redhat.com">camunoz@redhat.com</a>
 */
public class ZanataMTEngine implements MachineTranslationEngine {

    private final Client webClient;
    private String zanataMtUrl;
    private String zanataMtAuthUser;
    private String zanataMtAuthToken;

    public ZanataMTEngine(String zanataMtUrl, String zanataMtAuthUser,
            String zanataMtAuthToken) {
        this(ClientBuilder.newClient(), zanataMtUrl, zanataMtAuthUser,
                zanataMtAuthToken);
    }

    public ZanataMTEngine(Client webClient, String zanataMtUrl,
            String zanataMtAuthUser, String zanataMtAuthToken) {
        this.webClient = webClient;
        this.zanataMtUrl = zanataMtUrl;
        this.zanataMtAuthUser = zanataMtAuthUser;
        this.zanataMtAuthToken = zanataMtAuthToken;
    }

    @Override
    public String translate(String source, String srcLocale,
            String targetLocale) throws Exception {
        WebTarget target = webClient.target(zanataMtUrl)
                .path("/api/document/translate")
                .queryParam("targetLang", targetLocale);

        Response response = target.request(MediaType.APPLICATION_JSON_TYPE)
                .header("X-Auth-Token", zanataMtAuthToken)
                .header("X-Auth-User", zanataMtAuthUser)
                .post(Entity.entity("{\n" +
                        "\"url\": \"http://redhat.com\",\n" +
                        "\"contents\": [ { \"type\" : \"text/plain\", \"value\" : \"" + source + "\" }],\n" +
                        "\"locale\": \"" + srcLocale + "\"\n" +
                        "}", MediaType.APPLICATION_JSON_TYPE));

        // Only 200 is good
        if(response.getStatus() != 200) {
            throw new Exception(
                    "Could not get translated string (" + response.getStatus() +
                            ")");
        }

        JsonParser parser = new JsonParser();
        String entity = response.readEntity(String.class);
        JsonElement element = parser.parse(entity);

        response.close();
        return extractTranslatedString(element);
    }

    private String extractTranslatedString(JsonElement element) {
        return element.getAsJsonObject().get("contents").getAsJsonArray().get(0)
                .getAsJsonObject().get("value").getAsString();
    }
}
