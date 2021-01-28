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

import hudson.Extension;
import hudson.Functions;
import hudson.model.PeriodicWork;
import io.jenkins.plugins.gating.GatingMetrics;
import io.jenkins.plugins.gating.MetricsSnapshot;
import io.jenkins.plugins.statuspage_gating.api.Component;
import io.jenkins.plugins.statuspage_gating.api.Page;
import io.jenkins.plugins.statuspage_gating.api.StatusPageIo;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Periodically update Metrics from statuspage.
 */
@Extension
public final class MetricsUpdater extends PeriodicWork {
    private static final Logger LOGGER = Logger.getLogger(MetricsUpdater.class.getName());

    @Inject private StatusPage statusPage;

    @Inject private GatingMetrics metrics;

    @Override
    public long getRecurrencePeriod() {
        return MIN;
    }

    @Override
    public long getInitialDelay() {
        return Functions.getIsUnitTest() ? DAY * 365: 0;
    }

    @Override
    protected void doRun() {
        for (StatusPage.Source source : statusPage.getSources()) {
            Map<String, MetricsSnapshot.Resource> statuses = new HashMap<>();
            try (StatusPageIo spi = ClientFactory.get().create(source.getUrl(), source.getApiKey())) {
                for (Page page : spi.listPages()) {
                    // Only read the page configured
                    if (!Objects.equals(page.getName(), source.getPage())) continue;

                    List<Component> components = spi.listComponents(page);

                    for (Component component : components) {
                        String resourceId = String.format("%s/%s", source.getLabel(), component.getName());
                        statuses.put(resourceId, new MetricsSnapshot.Resource(
                                resourceId, component.getStatus(), component.getDescription()
                        ));
                    }
                }
                metrics.update(new MetricsSnapshot(statusPage, source.getLabel(), statuses));
            } catch (Throwable ex) {
                LOGGER.log(Level.WARNING, "Failed obtaining metrics from source " + source, ex);
                metrics.reportError(new MetricsSnapshot.Error(statusPage, source.getLabel(), "Failed obtaining metrics from source", ex));
            }
        }
    }
}
