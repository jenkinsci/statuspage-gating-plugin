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

import com.google.common.collect.ImmutableSet;
import hudson.ExtensionList;
import io.jenkins.plugins.gating.GatingMetrics;
import io.jenkins.plugins.gating.MetricsSnapshot;
import io.jenkins.plugins.statuspage_gating.api.Page;
import io.jenkins.plugins.statuspage_gating.api.StatusPageIo;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static io.jenkins.plugins.statuspage_gating.ClientFactory.factory;
import static org.junit.Assert.assertEquals;

public class UpdateTest {

    @Rule public final JenkinsRule j = new JenkinsRule();

    @Test
    public void update() {
        SharedFixtureClient.declareSources();
        SharedFixtureClient.reportMetrics();

        MetricsUpdater ma = ExtensionList.lookupSingleton(MetricsUpdater.class);
        ma.doRun();

        Map<String, MetricsSnapshot> metrics = GatingMetrics.get().getMetrics();
        MetricsSnapshot one = metrics.get("one");
        MetricsSnapshot two = metrics.get("Second One");
        assertEquals(2, metrics.size());

        assertEquals("one", one.getSourceLabel());
        assertEquals(SharedFixtureClient.getReportedMetrics().get("one"), one.getStatuses());

        assertEquals("Second One", two.getSourceLabel());
        assertEquals(SharedFixtureClient.getReportedMetrics().get("Second One"), two.getStatuses());
    }

    @Test
    public void failUpdate() {
        SharedFixtureClient.declareSources();

        factory = new SharedFixtureClient.InjectingFactory(new StatusPageIo("", null) {
            @Override public @Nonnull List<Page> listPages() throws IOException {
                throw new IOException("Can't do");
            }
        });

        MetricsUpdater ma = ExtensionList.lookupSingleton(MetricsUpdater.class);
        ma.doRun();

        GatingMetrics gatingMetrics = GatingMetrics.get();
        MatcherAssert.assertThat(gatingMetrics.getStatusOfAllResources(), Matchers.anEmptyMap());

        Map<String, MetricsSnapshot.Error> errors = gatingMetrics.getErrors();
        assertEquals(ImmutableSet.of("one", "Second One"), errors.keySet());
        assertEquals("Can't do", errors.get("one").getCause().getMessage());
        assertEquals("Can't do", errors.get("Second One").getCause().getMessage());
    }
}
