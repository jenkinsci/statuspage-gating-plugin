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

package io.jenkins.plugins.statuspage_gating.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import hudson.util.Secret;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class StatusPageIo implements Closeable {
    public static final String DEFAULT_ROOT_URL = "https://api.statuspage.io/v1/";

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final CloseableHttpClient client = HttpClients.createSystem();

    private final @Nonnull String rootUrl;
    private final @CheckForNull Secret apiKey;

    public StatusPageIo(@Nonnull String rootUrl, @CheckForNull Secret apiKey) {
        this.rootUrl = rootUrl;
        this.apiKey = apiKey;
    }

    public @Nonnull List<Page> listPages() throws IOException {
        String url = rootUrl + "pages";
        return fetchResource(client, url, new TypeReference<List<Page>>(){});
    }

    public @Nonnull List<Component> listComponents(Page page) throws IOException {
        String url = rootUrl + "pages/" + page.getId() + "/components";
        return fetchResource(client, url, new TypeReference<List<Component>>(){});
    }

    private @Nonnull <T> T fetchResource(CloseableHttpClient client, String url, TypeReference<T> resourceType) throws IOException {
        HttpGet request = getRequest(url);
        try (CloseableHttpResponse rsp = client.execute(request)) {
            checkStatusCode(request, rsp);
            return deserializeBody(resourceType, rsp.getEntity().getContent());
        }
    }

    @VisibleForTesting
    /*package*/ static <T> T deserializeBody(TypeReference<T> resourceType, InputStream stream) throws IOException {
        return objectMapper.readValue(stream, resourceType);
    }

    private void checkStatusCode(HttpGet request, CloseableHttpResponse rsp) throws IOException {
        int statusCode = rsp.getStatusLine().getStatusCode();
        if (statusCode != 200) throw new IOException("Status code " + statusCode + " accessing " + request.getURI().toString());
    }

    private @Nonnull HttpGet getRequest(String pagesUrl) {
        HttpGet httpGet = new HttpGet(pagesUrl);
        if (apiKey != null) {
            httpGet.setHeader("Authorization", "OAuth " + apiKey.getPlainText());
        }
        return httpGet;
    }

    @Override
    public void close() throws IOException {
        client.close();
    }
}
