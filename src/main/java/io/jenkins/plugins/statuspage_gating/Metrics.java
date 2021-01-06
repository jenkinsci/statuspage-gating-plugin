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

import com.google.common.annotations.VisibleForTesting;
import hudson.Extension;
import hudson.Functions;
import hudson.model.PeriodicWork;
import hudson.model.queue.CauseOfBlockage;
import io.jenkins.plugins.statuspage_gating.api.Component;
import io.jenkins.plugins.statuspage_gating.api.ComponentGroup;
import io.jenkins.plugins.statuspage_gating.api.Page;
import io.jenkins.plugins.statuspage_gating.api.StatusPageIo;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public enum Metrics {
    SINGLETON;

    @GuardedBy("metricsLock")
    private @CheckForNull Map<StatusPage.Source, SourceSnapshot> metrics = null;
    private final Object metricsLock = new Object();

    // TODO get status for inspection purposes

    public @CheckForNull CauseOfBlockage canRun(StatusRequirement req) {
        // TODO finalize
        return null;
    }

    @VisibleForTesting
    /*package*/ void update(@Nonnull Map<StatusPage.Source, SourceSnapshot> metrics) {
        synchronized (metricsLock) {
            this.metrics = new HashMap<>(metrics);
            System.out.println("Updated metrics: " + toString());
        }
    }

    @Override
    public String toString() {
        synchronized (metricsLock) {
            if (metrics == null) return "Metrics: empty";
            String nl = String.format("%n");
            StringBuilder sb = new StringBuilder("Metrics: ").append(nl);
            metrics.forEach((source, sourceSnapshot) -> {
                sb.append("Source ").append(source.getLabel()).append(':').append(nl);
                sourceSnapshot.pages.forEach((page, pageSnapshot) -> {
                    sb.append("\tPage ").append(page.getName()).append(':').append(nl);
                    for (Component component : pageSnapshot.listComponents) {
                        sb.append("\t\t Component ").append(component.getName()).append(':').append(component.getStatus()).append(nl);
                    }
                });
            });

            return sb.toString();
        }
    }

    static class PageSnapshot {
        private final @Nonnull List<ComponentGroup> listComponentGroups;
        private final @Nonnull List<Component> listComponents;

        public PageSnapshot(@Nonnull List<ComponentGroup> listComponentGroups, @Nonnull List<Component> listComponents) {

            this.listComponentGroups = new ArrayList<>(listComponentGroups);
            this.listComponents = new ArrayList<>(listComponents);
        }
    }

    /*package*/ static class SourceSnapshot {
        private @CheckForNull Map<Page, PageSnapshot> pages;

        public SourceSnapshot(@CheckForNull Map<Page, PageSnapshot> pages) {
            this.pages = pages;
        }
    }

    @Extension
    public static final class Updater extends PeriodicWork {

        @Inject
        private StatusPage statusPage;

        @Override
        public long getRecurrencePeriod() {
            return Functions.getIsUnitTest() ? DAY * 365 : MIN;
        }

        // TODO: Consult other sources when one is down/misconfigured
        // TODO: do not wipe past results on failure?
        @Override
        protected void doRun() throws Exception {
            Map<StatusPage.Source, SourceSnapshot> sourceSnapshots = new HashMap<>();
            for (StatusPage.Source source : statusPage.getSources()) {
                Map<Page, PageSnapshot> pageSnapshots;
                try (StatusPageIo spi = new StatusPageIo(source.getUrl(), source.getApiKey())) {
                    pageSnapshots = new HashMap<>();
                    for (Page page : spi.listPages()) {
                        PageSnapshot ms = new PageSnapshot(spi.listComponentGroups(page), spi.listComponents(page));
                        pageSnapshots.put(page, ms);
                    }
                }
                sourceSnapshots.put(source, new SourceSnapshot(pageSnapshots));
            }
            SINGLETON.update(sourceSnapshots);
        }
    }
}
