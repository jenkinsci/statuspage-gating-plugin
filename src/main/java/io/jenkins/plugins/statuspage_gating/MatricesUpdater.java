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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Periodically update Metrics from statuspage.
 */
@Extension
public final class MatricesUpdater extends PeriodicWork {

    @Inject private StatusPage statusPage;

    @Inject private GatingMatrices matrices;

    @Override
    public long getRecurrencePeriod() {
        return Functions.getIsUnitTest() ? DAY * 365: MIN;
    }

    // TODO: Consult other sources when one is down/misconfigured
    // TODO: do not wipe past results on failure?
    @Override
    protected void doRun() throws Exception {
        Map<String, ResourceStatus> statuses = new HashMap<>();

        for (StatusPage.Source source : statusPage.getSources()) {
            try (StatusPageIo spi = new StatusPageIo(source.getUrl(), source.getApiKey())) {
                for (Page page : spi.listPages()) {
                    List<ComponentGroup> groups = spi.listComponentGroups(page);
                    List<Component> components = spi.listComponents(page);

                    Map<String, Component> idToComponent = components.stream().collect(Collectors.toMap(AbstractObject::getId, c -> c));

                    for (ComponentGroup group : groups) {
                        ArrayList<Component.Status> groupStatuses = new ArrayList<>();

                        for (String cid : group.getComponentIds()) {
                            Component component = idToComponent.get(cid);
                            String resourceId = String.format("%s/%s/%s/%s", source.getLabel(), page.getName(), group.getName(), component.getName());
                            statuses.put(resourceId, component.getStatus());

                            groupStatuses.add(component.getStatus());
                        }
                        String resourceId = String.format("%s/%s/%s", source.getLabel(), page.getName(), group.getName());
                        statuses.put(resourceId, ComponentGroup.compact(groupStatuses));
                    }
                }
            }
            matrices.update(source.getLabel(), new GatingMatrices.Snapshot(statuses));
        }
    }
}
