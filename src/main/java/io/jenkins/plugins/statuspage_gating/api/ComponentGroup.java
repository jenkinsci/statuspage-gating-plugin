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

import com.fasterxml.jackson.annotation.JsonProperty;
import io.jenkins.plugins.gating.ResourceStatus;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

/**
 * @see <a href="https://developer.statuspage.io/#tag/component-groups">Api Docs</a>
 */
public final class ComponentGroup extends AbstractObject {
    private List<String> componentIds;

    public ComponentGroup(
            @JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("components") List<String> componentIds
    ) {
        super(id, name);
        this.componentIds = componentIds;
    }

    public List<String> getComponentIds() {
        return componentIds;
    }

    @Override
    public String toString() {
        return String.format("ComponentGroup{id='%s', name='%s', componentIds=%s}", getId(), getName(), componentIds);
    }

    /**
     * Identify {@link ResourceStatus} or most-favorable {@link ResourceStatus.Category} to describe all provided statuses.
     *
     * @return Status of all components if all are the same, Status category of the worse one otherwise.
     */
    public static ResourceStatus compact(List<Component.Status> groupStatuses) {
        HashSet<Component.Status> resourceStatuses = new HashSet<>(groupStatuses);
        if (resourceStatuses.size() == 1) return groupStatuses.get(0);

        groupStatuses.sort(Comparator.comparing(Enum::ordinal));

        return groupStatuses.get(0).getCategory();
    }
}
