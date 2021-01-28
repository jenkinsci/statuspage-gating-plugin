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

import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import org.hamcrest.MatcherAssert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.net.URL;
import java.util.List;
import java.util.Objects;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class JCascTest {
    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test @ConfiguredWithCode("JCascTest/jcasc.yaml")
    public void read() throws Exception {
        StatusPage statusPage = StatusPage.get();

        List<StatusPage.Source> srcs = statusPage.getSources();
        StatusPage.Source upstream = srcs.get(0);
        assertEquals("foobar", Objects.requireNonNull(upstream.getApiKey()).getPlainText());
        assertEquals("upstream", upstream.getLabel());
        assertEquals("foo", upstream.getPage());
        assertEquals("https://api.statuspage.io/v1/", upstream.getUrl());

        StatusPage.Source proxy = srcs.get(1);
        assertNull(proxy.getApiKey());
        assertEquals("proxy", proxy.getLabel());
        assertEquals("proxypage", proxy.getPage());
        assertEquals("https://acme.com", proxy.getUrl());

        try (JenkinsRule.WebClient wc = j.createWebClient()) {
            URL url = new URL(wc.getContextPath() + "configuration-as-code/viewExport");
            HtmlPage page = wc.getPage(wc.addCrumb(new WebRequest(url, HttpMethod.POST)));
            String content = page.getBody().getTextContent();
            MatcherAssert.assertThat(content, containsString("statuspageGating:"));
            MatcherAssert.assertThat(content, containsString("label: \"upstream\""));
            MatcherAssert.assertThat(content, containsString("url: \"https://acme.com\""));
        }
    }
}
