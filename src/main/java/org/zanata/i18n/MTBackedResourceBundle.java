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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * @author Carlos Munoz <a href="mailto:camunoz@redhat.com">camunoz@redhat.com</a>
 */
public class MTBackedResourceBundle extends ResourceBundle {

    private final ResourceBundle baseBundle;

    private final ResourceBundle targetBundle;

    private final Locale locale;

    private final MachineTranslationEngine translationEngine;

    private final Map<String, String> machineTranslatedEntries =
            new HashMap<>();

    public MTBackedResourceBundle(String baseName, Locale locale,
            String zanataMtUrl, String zanataMtAuthUser,
            String zanataMtAuthToken) {
        this(baseName, locale, new ZanataMTEngine(zanataMtUrl, zanataMtAuthUser,
                zanataMtAuthToken));
    }

    public MTBackedResourceBundle(String baseName, Locale locale,
            MachineTranslationEngine translationEngine) {
        this.baseBundle = ResourceBundle.getBundle(baseName);
        this.targetBundle = ResourceBundle.getBundle(baseName, locale);
        this.locale = locale;
        this.translationEngine = translationEngine;
        unsetParentBundle(targetBundle);
    }

    protected Object handleGetObject(String key) {
        Object value = null;
        // If there is a translation already present
        if (targetBundle.containsKey(key)) {
            value = targetBundle.getObject(key);
        } else if (machineTranslatedEntries.containsKey(key)) {
            // Or if there is a cached machine translation
            value = machineTranslatedEntries.get(key);
        } else {
            // Otherwise, translate the original string
            if(baseBundle.containsKey(key)) {
                // Get the original string from the base bundle
                String untranslatedString = baseBundle.getString(key);
                // Get from the machine translation service
                // TODO assuming English US
                try {
                    value = translationEngine.translate(untranslatedString, Locale.US.toString(),
                            locale.toString());
                    this.machineTranslatedEntries.put(key, (String)value);
                } catch (Exception e) {
                    // Return the untranslated string
                    // TODO log a warning?
                    value = untranslatedString;
                }
            }
        }
        return value;
    }

    public Enumeration<String> getKeys() {
        return baseBundle.getKeys();
    }

    /**
     * Unsets the parent bundle.
     * Useful to stop a bundle from falling back on its parent bundle when looking
     * for a string.
     */
    private void unsetParentBundle(ResourceBundle bundle) {
        try {
            Method setParentMethod = ResourceBundle.class
                    .getDeclaredMethod("setParent", ResourceBundle.class);
            setParentMethod.setAccessible(true);
            setParentMethod.invoke(bundle, new Object[]{null});
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(
                    "Error changing resource bundle's parent", e);
        }
    }
}
