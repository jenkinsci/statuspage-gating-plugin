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
import hudson.util.FormValidation;
import hudson.util.Secret;
import io.jenkins.plugins.statuspage_gating.StatusPage.Source;
import io.jenkins.plugins.statuspage_gating.api.Component;
import io.jenkins.plugins.statuspage_gating.api.Page;
import io.jenkins.plugins.statuspage_gating.api.StatusPageIo;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.ArgumentCaptor;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UiGlobalConfigTest {

    public static final SharedFixtureClient SHARED_FIXTURE_CLIENT = new SharedFixtureClient();

    @Rule
    public final JenkinsRule j = new JenkinsRule();
    public static final String BAR_PWD = "bar-pwd";

    @Test
    public void configRoundtripMultiple() throws Exception {
        StatusPage statusPage = StatusPage.get();
        List<Source> expectedSources = Arrays.asList(
                new Source("flabel", singletonList("fpage"), null, null),
                new Source("blabel", Arrays.asList("bpage1", "bpage2"), "https://bar.com", Secret.fromString(BAR_PWD))
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
        List<Source> expectedSources = singletonList(
                new Source("flabel", singletonList("fpage"), null, null)
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
        List<Source> expectedSources = Collections.emptyList();

        statusPage.setSources(expectedSources);
        j.configRoundtrip();
        assertSame(expectedSources, statusPage.getSources());
    }

    @Test
    public void source() {
        try {
            new Source("", singletonList("page"), null, null);
            fail();
        } catch (IllegalArgumentException ex) {}

        try {
            new Source("foo", emptyList(), null, null);
            fail();
        } catch (IllegalArgumentException ex) {}

        assertThat(new Source("label", singletonList("page"), null, null).getUrl(), equalTo(StatusPageIo.DEFAULT_ROOT_URL));
        assertThat(new Source("label", singletonList("page"), "", null).getUrl(), equalTo(StatusPageIo.DEFAULT_ROOT_URL));
        assertThat(new Source("label", singletonList("page"), "https://foo.com/v1", null).getUrl(), equalTo("https://foo.com/v1"));

        assertThat(new Source("label", singletonList("page"), null, null).getApiKey(), equalTo(null));
        assertThat(new Source("label", singletonList("page"), null, Secret.fromString("")).getApiKey(), equalTo(null));
        assertThat(new Source("label", singletonList("page"), null, Secret.fromString("foo")).getApiKey().getPlainText(), equalTo("foo"));
    }

    @Test
    public void formValidation() throws Exception {
        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Secret> apiKeyCaptor = ArgumentCaptor.forClass(Secret.class);

        ClientFactory.factory = mock(ClientFactory.class);
        when(ClientFactory.factory.create(any(String.class), any(Secret.class))).thenReturn(SHARED_FIXTURE_CLIENT);

        // Values are correctly passed to client
        StatusPage.get().doTestConnection("url", "apiKey", "page");
        verify(ClientFactory.factory).create(urlCaptor.capture(), apiKeyCaptor.capture());
        assertEquals("url", urlCaptor.getValue());
        assertEquals("apiKey", apiKeyCaptor.getValue().getPlainText());

        // Valid states
        ClientFactory.factory = new InjectingFactory(SHARED_FIXTURE_CLIENT);

        FormValidation fv = StatusPage.get().doTestConnection("url", "apiKey", "three");
        assertEquals("Some configured pages [three] do not exist: [three]", fv.getMessage());

        fv = StatusPage.get().doTestConnection("url", "apiKey", "twoName");
        assertEquals(FormValidation.Kind.OK, fv.kind);

        fv = StatusPage.get().doTestConnection("url", "", "twoName");
        assertEquals(FormValidation.Kind.OK, fv.kind);
        assertThat(fv.getMessage(), containsString(StatusPage.TEXT_NO_API_KEY));

        fv = StatusPage.get().doTestConnection("url", "apiKey", "");
        assertEquals(FormValidation.Kind.ERROR, fv.kind);
        assertThat(fv.getMessage(), containsString(StatusPage.TEXT_NO_PAGES));
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

    private static class SharedFixtureClient extends StatusPageIo {
        private Map<Page, List<Component>> map = new HashMap<>();
        {
                map.put(new Page("oneId", "oneName"), Collections.singletonList(
                        new Component("deadbeef", "Component #1", "Some desc", Component.Status.OPERATIONAL)
                ));
                map.put(new Page("twoId", "twoName"), Arrays.asList(
                        new Component("hexcat", "down-component", "it is down, alright", Component.Status.MAJOR_OUTAGE),
                        new Component("lizard", "some-other-component", "", Component.Status.DEGRADED_PERFORMANCE),
                        new Component("squirel", "Squirel", "", Component.Status.MAJOR_OUTAGE)
                ));
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
    }
}
