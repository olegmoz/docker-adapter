/*
 * MIT License
 *
 * Copyright (c) 2020 Artipie
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
package com.artipie.docker.http;

import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link ManifestEntity.Request}.
 * @since 0.4
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class ManifestEntityRequestTest {

    @Test
    void shouldReadName() {
        final ManifestEntity.Request request = new ManifestEntity.Request(
            new RequestLine(RqMethod.GET, "/v2/my-repo/manifests/3").toString()
        );
        MatcherAssert.assertThat(request.name().value(), new IsEqual<>("my-repo"));
    }

    @Test
    void shouldReadReference() {
        final ManifestEntity.Request request = new ManifestEntity.Request(
            new RequestLine(RqMethod.GET, "/v2/my-repo/manifests/sha256:123abc").toString()
        );
        MatcherAssert.assertThat(request.reference().string(), new IsEqual<>("sha256:123abc"));
    }

    @Test
    void shouldReadCompositeName() {
        final String name = "zero-one/two.three/four_five";
        MatcherAssert.assertThat(
            new ManifestEntity.Request(
                new RequestLine(
                    "HEAD", String.format("/v2/%s/manifests/sha256:234434df", name)
                ).toString()
            ).name().value(),
            new IsEqual<>(name)
        );
    }

}
