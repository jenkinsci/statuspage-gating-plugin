/*
 * Copyright (c) Red Hat, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.jenkins.plugins.statuspage_gating.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.annotation.Nonnull;
import java.util.logging.Level;

/**
 * @see <a href="https://developer.statuspage.io/#tag/components">Api Docs</a>
 */
public final class Component extends AbstractObject {
    private final String description;
    private final Status status;

    public enum Status {
        OPERATIONAL,
        UNDER_MAINTENANCE,
        DEGRADED_PERFORMANCE,
        PARTIAL_OUTAGE,
        MAJOR_OUTAGE,
        UNKNOWN;

        @JsonCreator // Needed for Jackson to comprehend the "" -> UNKNOWN transition, that cannot be expressed through JsonProperty
        public static @Nonnull Status forValue(String value) {
            if ("".equals(value)) return UNKNOWN;
            try {
                return valueOf(value.toUpperCase());
            } catch (NullPointerException|IllegalArgumentException ex) {
                LOGGER.log(Level.WARNING, "Failed to deserialize Component Status from '" + value + "'", ex);
                return UNKNOWN;
            }
        }
    }

    public Component(
            @JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("description") String description,
            @JsonProperty("status") Status status
    ) {
        super(id, name);
        this.description = description;
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public Status getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return String.format("Component{id='%s', name='%s', status=%s}", getId(), getName(), status);
    }
}
