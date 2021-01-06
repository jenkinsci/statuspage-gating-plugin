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
package io.jenkins.plugins.statuspage_gating.StatusPage

import io.jenkins.plugins.statuspage_gating.StatusPage
import io.jenkins.plugins.statuspage_gating.api.StatusPageIo

def f = namespace(lib.FormTagLib)
StatusPage sp = (StatusPage) instance

f.section(title: "StatusPage.io Gating") {
    f.entry(title: "Sources") {
        f.repeatable(var: "instance", name: "sources", items: sp.getSources(), header: "Source", add: "Add Source") {
            StatusPage.Source source = (StatusPage.Source) instance
            table() {
                f.entry(field: "label", title: "Label", description: "Disambiguate multiple gating sources") {
                    f.textbox(clazz: "required", value: source?.getLabel())
                }

                f.entry(field: "apiKey", title: "API Key", description: "Optional API key used for authentication in case it is required") {
                    f.password(value: source?.getApiKey())
                }

                f.entry(field: "url", title: "Service URL", description: "Optional service URL used in case it is different from '${StatusPageIo.DEFAULT_ROOT_URL}'") {
                    f.textbox(value: source?.getUrl())
                }

                f.entry(field: "pages", title: "Pages") {
                    f.textarea(fiel: "pages", value: source?.getPages()?.join("\n"))
                }

                f.entry() {
                    f.repeatableDeleteButton(value: "Delete Source")
                }
            }
        }
    }
}
