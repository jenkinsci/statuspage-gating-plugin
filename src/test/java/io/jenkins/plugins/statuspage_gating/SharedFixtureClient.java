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
package io.jenkins.plugins.statuspage_gating;

import com.google.common.collect.ImmutableMap;
import hudson.util.Secret;
import io.jenkins.plugins.gating.MetricsSnapshot;
import io.jenkins.plugins.statuspage_gating.api.Component;
import io.jenkins.plugins.statuspage_gating.api.Page;
import io.jenkins.plugins.statuspage_gating.api.StatusPageIo;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fake metrics for testing purposes.
 */
class SharedFixtureClient extends StatusPageIo {
    public static final SharedFixtureClient SHARED_FIXTURE_CLIENT = new SharedFixtureClient();

    private final Map<Page, List<Component>> map = new HashMap<>();
    {
        map.put(new Page("oneId", "oneName"), Collections.singletonList(
                new Component("deadbeef", "Component #1", "Some desc", Component.Status.OPERATIONAL)
        ));
        map.put(new Page("twoId", "twoName"), Arrays.asList(
                new Component("hexcat", "down-component", "it is down, alright", Component.Status.MAJOR_OUTAGE),
                new Component("lizard", "some-other-component", "", Component.Status.DEGRADED_PERFORMANCE),
                new Component("squirrel", "Squirrel", "", Component.Status.MAJOR_OUTAGE)
        ));
    }

    public static void reportMetrics() {
        ClientFactory.factory = new SharedFixtureClient.InjectingFactory(SHARED_FIXTURE_CLIENT);
    }

    public static void declareSources() {
        StatusPage.get().setSources(Arrays.asList(
                new StatusPage.Source("one", "oneName", null, null),
                new StatusPage.Source("Second One", "twoName", null, null)
        ));
    }

    public static ImmutableMap<String, ImmutableMap<String, MetricsSnapshot.Resource>> getReportedMetrics() {
        return ImmutableMap.of(
                "one", ImmutableMap.of(
                        "one/Component #1", new MetricsSnapshot.Resource("one/Component #1", Component.Status.OPERATIONAL, "Some desc")
                ),
                "Second One", ImmutableMap.of(
                        "Second One/down-component", new MetricsSnapshot.Resource("Second One/down-component", Component.Status.MAJOR_OUTAGE, "it is down, alright"),
                        "Second One/some-other-component", new MetricsSnapshot.Resource("Second One/some-other-component", Component.Status.DEGRADED_PERFORMANCE, ""),
                        "Second One/Squirrel", new MetricsSnapshot.Resource("Second One/Squirrel", Component.Status.MAJOR_OUTAGE, "")
                )
        );
    }

    public SharedFixtureClient() {
        super(null, null);
    }

    @Override public @Nonnull List<Page> listPages() {
        return new ArrayList<>(map.keySet());
    }

    @Override public @Nonnull List<Component> listComponents(Page page) {
        return map.get(page);
    }

    public static final class InjectingFactory extends ClientFactory {
        private final StatusPageIo spio;

        public InjectingFactory(StatusPageIo spio) {
            this.spio = spio;
        }

        @Override
        public StatusPageIo create(String rootUrl, Secret apiKey) {
            return spio;
        }
    }
}
