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
import io.jenkins.plugins.gating.GatingMatrices;
import io.jenkins.plugins.gating.ResourceStatus;
import io.jenkins.plugins.statuspage_gating.api.AbstractObject;
import io.jenkins.plugins.statuspage_gating.api.Component;
import io.jenkins.plugins.statuspage_gating.api.ComponentGroup;
import io.jenkins.plugins.statuspage_gating.api.Page;
import io.jenkins.plugins.statuspage_gating.api.StatusPageIo;

import javax.inject.Inject;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Periodically update Metrics from statuspage.
 */
@Extension
public final class MatricesUpdater extends PeriodicWork {
    private static final Logger LOGGER = Logger.getLogger(MatricesUpdater.class.getName());

    @Inject private StatusPage statusPage;

    @Inject private GatingMatrices matrices;

    @Override
    public long getRecurrencePeriod() {
        return Functions.getIsUnitTest() ? DAY * 365: MIN;
    }

    @Override
    protected void doRun() {
        for (StatusPage.Source source : statusPage.getSources()) {
            Map<String, ResourceStatus> statuses = new HashMap<>();
            try (StatusPageIo spi = new StatusPageIo(source.getUrl(), source.getApiKey())) {
                for (Page page : spi.listPages()) {
                    List<Component> components = spi.listComponents(page);

                    for (Component component : components) {
                        String resourceId = String.format("%s/%s/%s", source.getLabel(), page.getName(), component.getName());
                        statuses.put(resourceId, component.getStatus());
                    }
                }
            } catch (Throwable ex) {
                LOGGER.log(Level.WARNING, "Failed obtaining matrices from source " + source, ex);
            }
            matrices.update(new GatingMatrices.Snapshot(statusPage, source.getLabel(), statuses));
        }
    }
}
