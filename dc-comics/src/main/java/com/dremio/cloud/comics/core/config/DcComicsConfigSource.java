package com.dremio.cloud.comics.core.config;

import org.eclipse.microprofile.config.spi.ConfigSource;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class DcComicsConfigSource implements ConfigSource {

    public Map<String, String> getProperties() {
        return new HashMap<String, String>();
    }

    public Set<String> getPropertyNames() {
        return null;
    }

    public int getOrdinal() {
        // https://quarkus.io/guides/config-extending-support
        // positioning DC Comics config source between application.properties and microprofile-config.properties
        return 200;
    }

    public String getValue(String s) {
        return null;
    }

    public String getName() {
        return null;
    }
}
