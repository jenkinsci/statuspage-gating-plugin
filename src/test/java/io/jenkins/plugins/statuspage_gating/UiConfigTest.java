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

import com.gargoylesoftware.htmlunit.html.HtmlPage;
import hudson.util.Secret;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

public class UiConfigTest {

    @Rule
    public final JenkinsRule j = new JenkinsRule();
    public static final String BAR_PWD = "bar-pwd";

    @Test
    public void configRoundtripMultiple() throws Exception {
        StatusPage statusPage = StatusPage.get();
        List<StatusPage.Source> expectedSources = Arrays.asList(
                new StatusPage.Source("flabel", Collections.singletonList("fpage"), null, null),
                new StatusPage.Source("blabel", Arrays.asList("bpage1", "bpage2"), "https://bar.com", Secret.fromString(BAR_PWD))
        );

        statusPage.setSources(expectedSources);
        j.configRoundtrip();
        assertEquals(expectedSources, statusPage.getSources());
        assertNotSame(expectedSources, statusPage.getSources());

        try (JenkinsRule.WebClient wc = j.createWebClient()) {
            HtmlPage configure = wc.goTo("configure");
            String content = configure.getWebResponse().getContentAsString();
            assertThat(content, containsString("flabel"));
            assertThat(content, containsString("https://bar.com"));
            assertThat(content, containsString("bpage1"));
            assertThat(content, not(containsString(BAR_PWD)));
        }
    }

    @Test
    public void configRoundtripSingle() throws Exception {
        StatusPage statusPage = StatusPage.get();
        List<StatusPage.Source> expectedSources = Collections.singletonList(
                new StatusPage.Source("flabel", Collections.singletonList("fpage"), null, null)
        );

        statusPage.setSources(expectedSources);
        j.configRoundtrip();
        assertEquals(expectedSources, statusPage.getSources());
        assertNotSame(expectedSources, statusPage.getSources());

        try (JenkinsRule.WebClient wc = j.createWebClient()) {
            HtmlPage configure = wc.goTo("configure");
            String content = configure.getWebResponse().getContentAsString();
            assertThat(content, containsString("flabel"));
            assertThat(content, not(containsString(BAR_PWD)));
        }
    }

    @Test
    public void configRoundtripEmpty() throws Exception {
        StatusPage statusPage = StatusPage.get();
        List<StatusPage.Source> expectedSources = Collections.emptyList();

        statusPage.setSources(expectedSources);
        j.configRoundtrip();
        assertSame(expectedSources, statusPage.getSources());
    }
}
