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

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.Test;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class DeserializationTest {

    private static final TypeReference<List<Page>> TYPE_PAGES = new TypeReference<List<Page>>(){};
    private static final TypeReference<List<Component>> TYPE_COMPONENTS = new TypeReference<List<Component>>(){};
    private static final TypeReference<List<ComponentGroup>> TYPE_COMPONENT_GROUPS = new TypeReference<List<ComponentGroup>>(){};

    @Test
    public void page() throws Exception {
        List<Page> pages = read(TYPE_PAGES, "pages");
        assertEquals(1, pages.size());
        Page page = pages.get(0);
        assertEquals("d78dc5bb023f", page.getId());
        assertEquals("MyPage", page.getName());
    }

    @Test
    public void components() throws Exception {
        Map<String, Component> components = read(TYPE_COMPONENTS, "components").stream()
                .collect(Collectors.toMap(Component::getId, Function.identity()));
        assertEquals(6, components.size());

        Component as = components.get("aaaaaaaaaaaa");
        assertEquals("aaaa", as.getName());
        assertEquals(Component.Status.OPERATIONAL, as.getStatus());
        assertEquals("Operational Resource", as.getDescription());

        Component bs = components.get("bbbbbbbbbbbb");
        assertEquals("bbbb", bs.getName());
        assertEquals(Component.Status.UNDER_MAINTENANCE, bs.getStatus());
        assertEquals("Maintenance Resource", bs.getDescription());

        Component cs = components.get("cccccccccccc");
        assertEquals("cccc", cs.getName());
        assertEquals(Component.Status.DEGRADED_PERFORMANCE, cs.getStatus());
        assertEquals("Degraded Resource", cs.getDescription());

        Component ds = components.get("dddddddddddd");
        assertEquals("dddd", ds.getName());
        assertEquals(Component.Status.PARTIAL_OUTAGE, ds.getStatus());
        assertEquals("Partial Outage Resource", ds.getDescription());

        Component es = components.get("eeeeeeeeeeee");
        assertEquals("eeee", es.getName());
        assertEquals(Component.Status.MAJOR_OUTAGE, es.getStatus());
        assertEquals("Major Outage Resource", es.getDescription());

        Component fs = components.get("ffffffffffff");
        assertEquals("ffff", fs.getName());
        assertEquals(Component.Status.UNKNOWN, fs.getStatus());
        assertEquals("Unknown Resource", fs.getDescription());
    }

    @Test
    public void componentGroups() throws Exception {
        Map<String, ComponentGroup> cgs = read(TYPE_COMPONENT_GROUPS, "component-groups").stream()
                .collect(Collectors.toMap(ComponentGroup::getName, Function.identity()));
        assertEquals(2, cgs.size());

        ComponentGroup foo = cgs.get("Foo");
        assertEquals("wfwsc3371234", foo.getId());
        assertEquals(Arrays.asList("aaaaaaaaaaaa", "bbbbbbbbbbbb"), foo.getComponentIds());

        ComponentGroup bar = cgs.get("Bar");
        assertEquals("wfwsc3371235", bar.getId());
        assertEquals(Arrays.asList("cccccccccccc", "dddddddddddd", "eeeeeeeeeeee", "ffffffffffff"), bar.getComponentIds());
    }

    private <T> T read(TypeReference<T> type, String file) throws java.io.IOException {
        try (InputStream res = getClass().getResourceAsStream("DeserializationTest/" + file + ".json")) {
            return StatusPageIo.deserializeBody(type, res);
        }
    }
}
