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

import hudson.model.FreeStyleProject;
import hudson.model.JobProperty;
import hudson.model.Queue;
import hudson.model.queue.CauseOfBlockage;
import io.jenkins.plugins.gating.GatingMatrices;
import io.jenkins.plugins.gating.ResourceBlockage;
import io.jenkins.plugins.gating.ResourceRequirementProperty;
import io.jenkins.plugins.gating.ResourceStatus;
import io.jenkins.plugins.statuspage_gating.api.Component;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static io.jenkins.plugins.gating.ResourceStatus.Category.UP;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GatingTest {

    public static final String RESOURCE_NAME = "statuspage/p1/C1/r1";
    public static final String COMPONENT_NAME = "statuspage/p2/C3";

    @Rule
    public final JenkinsRule j = new JenkinsRule();

    @Test
    public void allDown() throws Exception {

        Map<String, ResourceStatus> status = new HashMap<>();
        status.put(RESOURCE_NAME, Component.Status.MAJOR_OUTAGE);
        status.put(COMPONENT_NAME, Component.Status.UNDER_MAINTENANCE);

        ResourceRequirementProperty reqs = ResourceRequirementProperty.builder()
                .require(RESOURCE_NAME, UP)
                .require(COMPONENT_NAME, Component.Status.OPERATIONAL)
                .build()
        ;

        Queue.Item item = runJob(status, reqs);
        CauseOfBlockage cob = item.getCauseOfBlockage();
        assertEquals(
                String.format("Some resource are not available: %s[OPERATIONAL] is UNDER_MAINTENANCE, %s[UP] is MAJOR_OUTAGE", COMPONENT_NAME, RESOURCE_NAME),
                cob.getShortDescription()
        );
    }

    @Test
    public void allUp() throws Exception {
        HashMap<String, ResourceStatus> status = new HashMap<>();
        status.put(RESOURCE_NAME, Component.Status.OPERATIONAL);
        status.put(COMPONENT_NAME, Component.Status.OPERATIONAL);

        ResourceRequirementProperty reqs = ResourceRequirementProperty.builder()
                .require(COMPONENT_NAME, UP)
                .build()
        ;

        runJob(status, reqs);
        assertTrue(j.getInstance().getQueue().isEmpty());
    }

    @Test
    public void someDown() throws Exception {

        Map<String, ResourceStatus> status = new HashMap<>();
        status.put(RESOURCE_NAME, Component.Status.MAJOR_OUTAGE);
        status.put(COMPONENT_NAME, Component.Status.OPERATIONAL);

        ResourceRequirementProperty reqs = ResourceRequirementProperty.builder()
                .require(RESOURCE_NAME, UP)
                .require(COMPONENT_NAME, UP)
                .build()
        ;

        Queue.Item item = runJob(status, reqs);
        CauseOfBlockage cob = item.getCauseOfBlockage();
        assertEquals(
                String.format("Some resource are not available: %s[UP] is MAJOR_OUTAGE", RESOURCE_NAME),
                cob.getShortDescription()
        );
    }

    private Queue.Item runJob(
            Map<String, ResourceStatus> status,
            JobProperty<? super FreeStyleProject> reqs
    ) throws IOException, InterruptedException {
        setStatus(status);

        FreeStyleProject p = j.createFreeStyleProject();

        p.addProperty(reqs);

        p.scheduleBuild2(0);

        Queue queue = j.getInstance().getQueue();

        while (true) {

            // Build
            if (p.getBuildByNumber(1) != null) return null;

            // Blocked
            Queue.Item item = queue.getItem(p);
            if (item != null && item.getCauseOfBlockage() instanceof ResourceBlockage) return item;

            Thread.sleep(1000);
        }
    }

    private void setStatus(Map<String, ResourceStatus> status) {
        GatingMatrices.get().update("statuspage", new GatingMatrices.Snapshot(status));
    }
}
