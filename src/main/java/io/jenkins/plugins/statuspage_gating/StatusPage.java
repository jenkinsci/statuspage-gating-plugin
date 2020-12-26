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
import hudson.ExtensionList;
import hudson.util.Secret;
import io.jenkins.plugins.statuspage_gating.api.StatusPageIo;
import jenkins.model.GlobalConfiguration;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * User configuration of where to pull the {@link Metrics}.
 */
@Extension
@Symbol("statuspageGating")
public final class StatusPage extends GlobalConfiguration {

    private List<Source> sources = Collections.emptyList();

    public static StatusPage get() {
        return ExtensionList.lookupSingleton(StatusPage.class);
    }

    public List<Source> getSources() {
        return sources;
    }

    @DataBoundSetter
    public StatusPage setSources(@Nonnull List<Source> sources) {
        if (sources == null || sources.size() == 0) throw new IllegalArgumentException("No sources provided: " + sources);

        this.sources = Collections.unmodifiableList(new ArrayList<>(sources));
        return this;
    }

    public static final class Source {
        private final @Nonnull String label;
        private final @Nonnull List<String> pages;
        private final @Nonnull String url;
        private final @CheckForNull Secret apiKey;

        @DataBoundConstructor
        public Source(
                @Nonnull String label,
                @Nonnull List<String> pages,
                @CheckForNull String url,
                @CheckForNull Secret apiKey
        ) {
            this.label = label;
            this.pages = Collections.unmodifiableList(new ArrayList<>(pages));
            this.url = url == null ? StatusPageIo.DEFAULT_ROOT_URL : url;
            this.apiKey = apiKey;
        }

        public @Nonnull String getLabel() {
            return label;
        }

        public @Nonnull List<String> getPages() {
            return pages;
        }

        public @Nonnull String getUrl() {
            return url;
        }

        public @CheckForNull Secret getApiKey() {
            return apiKey;
        }
    }
}
