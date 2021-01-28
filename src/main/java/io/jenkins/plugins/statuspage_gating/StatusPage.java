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
import hudson.util.FormValidation;
import hudson.util.Secret;
import io.jenkins.plugins.gating.MetricsProvider;
import io.jenkins.plugins.statuspage_gating.api.Page;
import io.jenkins.plugins.statuspage_gating.api.StatusPageIo;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.interceptor.RequirePOST;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * User configuration of where to pull the Metrics.
 */
@Extension
@Symbol("statuspageGating")
public final class StatusPage extends GlobalConfiguration implements MetricsProvider {

    public static final String TEXT_NO_API_KEY = "No API key provided, make sure desired page is available without authentication.";
    public static final String TEXT_NO_PAGE = "No page configured!";

    private List<Source> sources = Collections.emptyList();

    public static StatusPage get() {
        return ExtensionList.lookupSingleton(StatusPage.class);
    }

    public StatusPage() {
        load();
    }

    public List<Source> getSources() {
        return sources;
    }

    @DataBoundSetter
    public void setSources(@Nonnull List<Source> sources) {
        if (sources == null) throw new IllegalArgumentException("No sources provided: " + sources);

        this.sources = sources.isEmpty()
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(sources))
        ;
        save();
    }

    @RequirePOST
    @Restricted(NoExternalUse.class)
    public FormValidation doTestConnection(
            @QueryParameter String url,
            @QueryParameter String apiKey,
            @QueryParameter("page") String configuredPage
    ) throws FormValidation {
        Jenkins.get().checkPermission(Jenkins.ADMINISTER);

        if (configuredPage.isEmpty()) {
            return FormValidation.error(TEXT_NO_PAGE);
        }

        // Push values through Source to set defaults and process data
        Source source = new Source(
                UUID.randomUUID().toString(),
                configuredPage,
                url,
                Secret.fromString(apiKey)
        );

        try (StatusPageIo spi = ClientFactory.get().create(source.getUrl(), source.getApiKey())) {
            List<Page> actualPages = spi.listPages();
            String actualPageNames = actualPages.stream().map(Page::getName).collect(Collectors.joining(", "));

            Optional<Page> exists = actualPages.stream()
                    .filter(p -> p.getName().equals(configuredPage))
                    .findFirst()
            ;
            if (!exists.isPresent()) {
                return FormValidation.error("Configured page " + source.page + " does not exist in: " + actualPageNames);
            }

            StringBuilder sb = new StringBuilder("Connected!");
            if (source.getApiKey() == null) {
                sb.append(' ').append(TEXT_NO_API_KEY);
            }
            sb.append(" Existing pages: ").append(actualPageNames);
            return FormValidation.ok(sb.toString());
        } catch (Exception e) {
            FormValidation fv = FormValidation.error(e, "Verification failed");
            fv.addSuppressed(e);
            throw fv;
        }
    }

    @Override
    public @Nonnull Set<String> getLabels() {
        return sources.stream().map(Source::getLabel).collect(Collectors.toSet());
    }

    public static final class Source {
        private final @Nonnull String label;
        private final @Nonnull String page;
        private final @Nonnull String url;
        private final @CheckForNull Secret apiKey;

        @DataBoundConstructor
        public Source(
                @Nonnull String label,
                @Nonnull String page,
                @CheckForNull String url,
                @CheckForNull Secret apiKey
        ) {
            if (label == null || label.isEmpty() || page == null || page.isEmpty()) {
                throw new IllegalArgumentException();
            }

            this.label = label;
            this.page = page;
            this.url = StringUtils.defaultIfBlank(url, StatusPageIo.DEFAULT_ROOT_URL);
            this.apiKey = apiKey == null || apiKey.getPlainText().isEmpty() ? null : apiKey;
        }

        public @Nonnull String getLabel() {
            return label;
        }

        public @Nonnull String getPage() {
            return page;
        }

        public @Nonnull String getUrl() {
            return url;
        }

        public @CheckForNull Secret getApiKey() {
            return apiKey;
        }

        @Override
        public String toString() {
            return String.format("StatusPage.Source{label='%s', page=%s, url='%s'}", label, page, url);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Source source = (Source) o;
            return label.equals(source.label) &&
                    page.equals(source.page) &&
                    url.equals(source.url) &&
                    Objects.equals(apiKey, source.apiKey);
        }

        @Override
        public int hashCode() {
            return Objects.hash(label, page, url, apiKey);
        }
    }
}
