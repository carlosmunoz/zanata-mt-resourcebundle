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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * @author Carlos Munoz <a href="mailto:camunoz@redhat.com">camunoz@redhat.com</a>
 */

public class MTBackendResourceBundleTest {

    private MTBackedResourceBundle bundle;

    @Mock private MachineTranslationEngine mockEngine;

    @BeforeEach
    public void initialize() throws Exception {
        initMocks(this);
        bundle = new MTBackedResourceBundle("test", new Locale("es"),
                mockEngine);

        ArgumentCaptor<String> sourceStringCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> sourceLocaleCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> targetLocaleCaptor = ArgumentCaptor.forClass(String.class);
        when(mockEngine.translate(sourceStringCaptor.capture(), sourceLocaleCaptor.capture(), targetLocaleCaptor.capture()))
                .thenReturn("machine translated string");
    }

    @Test
    @DisplayName("Get local translation when available")
    public void getLocalTranslationWhenAvailable() throws Exception {
        assertEquals("Hola Mundo!", bundle.getString("test.helloworld"));
    }

    @Test
    @DisplayName("Get machine translation when string is not locally translated")
    public void getFromMt() throws Exception {
        assertEquals("machine translated string", bundle.getString("test.untranslated.locally"));
    }

    @Test
    @DisplayName("Get locally translated even if parent doesn't contain the key")
    public void getLocalTranslationFromTranslatedBundle() throws Exception {
        assertEquals("I am only available in this file", bundle.getString("test.onlytranslated"));
    }

    @Test
    @DisplayName("Get locally translated when failed to get from machine translation engine")
    public void getLocalTranslationWhenEngineFails() throws Exception {
        // When an exception is thrown invoking the translation engine
        when(mockEngine.translate(anyString(), anyString(), anyString())).thenThrow(new RuntimeException());

        assertEquals("I will be machine translated", bundle.getString("test.untranslated.locally"));
    }

    @Test
    @DisplayName("String doesn't exist anywhere")
    public void resourceDoesntExist() throws Exception {
        assertThrows(MissingResourceException.class, () -> bundle.getString("non.existent.key"));
    }

    @Test
    public void testResultsAreCached() throws Exception {
        // Translate first time
        String firstResult = bundle.getString("test.untranslated.locally");
        // make sure the engine is called once
        verify(mockEngine, times(1))
                .translate(anyString(), anyString(), anyString());
        // translate the same string again
        String secondResult = bundle.getString("test.untranslated.locally");
        // make sure the engine is not called again
        verifyZeroInteractions(mockEngine);
        assertEquals(firstResult, secondResult);
    }
}
