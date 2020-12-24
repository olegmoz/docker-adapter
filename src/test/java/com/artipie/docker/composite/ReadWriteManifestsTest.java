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
package com.artipie.docker.composite;

import com.artipie.asto.Content;
import com.artipie.docker.Manifests;
import com.artipie.docker.Tag;
import com.artipie.docker.Tags;
import com.artipie.docker.fake.FullTagsManifests;
import com.artipie.docker.manifest.Manifest;
import com.artipie.docker.ref.ManifestRef;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ReadWriteManifests}.
 *
 * @since 0.5
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
final class ReadWriteManifestsTest {

    @Test
    void shouldCallGetWithCorrectRef() {
        final ManifestRef ref = new ManifestRef.FromString("get");
        final CaptureGetManifests fake = new CaptureGetManifests();
        new ReadWriteManifests(fake, new CapturePutManifests()).get(ref)
            .toCompletableFuture().join();
        MatcherAssert.assertThat(
            fake.ref(),
            new IsEqual<>(ref)
        );
    }

    @Test
    void shouldCallPutPassingCorrectData() {
        final byte[] data = "data".getBytes();
        final ManifestRef ref = new ManifestRef.FromString("ref");
        final CapturePutManifests fake = new CapturePutManifests();
        new ReadWriteManifests(new CaptureGetManifests(), fake).put(
            ref,
            new Content.From(data)
        ).toCompletableFuture().join();
        MatcherAssert.assertThat(
            "ManifestRef from put method is wrong.",
            fake.ref(),
            new IsEqual<>(ref)
        );
        MatcherAssert.assertThat(
            "Size of content from put method is wrong.",
            fake.content().size().get(),
            new IsEqual<>((long) data.length)
        );
    }

    @Test
    void shouldDelegateTags() {
        final Optional<Tag> from = Optional.of(new Tag.Valid("foo"));
        final int limit = 123;
        final Tags tags = () -> new Content.From("{...}".getBytes());
        final FullTagsManifests fake = new FullTagsManifests(tags);
        final Tags result = new ReadWriteManifests(
            fake,
            new CapturePutManifests()
        ).tags(from, limit).toCompletableFuture().join();
        MatcherAssert.assertThat(
            "Forwards from",
            fake.capturedFrom(),
            new IsEqual<>(from)
        );
        MatcherAssert.assertThat(
            "Forwards limit",
            fake.capturedLimit(),
            new IsEqual<>(limit)
        );
        MatcherAssert.assertThat(
            "Returns tags",
            result,
            new IsEqual<>(tags)
        );
    }

    /**
     * Manifests implementation that captures get method for checking
     * correctness of parameters. Put method is unsupported.
     *
     * @since 0.5
     */
    private static class CaptureGetManifests implements Manifests {
        /**
         * Manifest reference.
         */
        private volatile ManifestRef refcheck;

        @Override
        public CompletionStage<Manifest> put(final ManifestRef ref, final Content content) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CompletionStage<Optional<Manifest>> get(final ManifestRef ref) {
            this.refcheck = ref;
            return CompletableFuture.completedFuture(Optional.empty());
        }

        @Override
        public CompletionStage<Tags> tags(final Optional<Tag> from, final int limit) {
            throw new UnsupportedOperationException();
        }

        public ManifestRef ref() {
            return this.refcheck;
        }
    }

    /**
     * Manifests implementation that captures put method for checking
     * correctness of parameters. Get method is unsupported.
     *
     * @since 0.5
     */
    private static class CapturePutManifests implements Manifests {
        /**
         * Manifest reference.
         */
        private volatile ManifestRef refcheck;

        /**
         * Manifest content.
         */
        private volatile Content contentcheck;

        @Override
        public CompletionStage<Manifest> put(final ManifestRef ref, final Content content) {
            this.refcheck = ref;
            this.contentcheck = content;
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<Optional<Manifest>> get(final ManifestRef ref) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CompletionStage<Tags> tags(final Optional<Tag> from, final int limit) {
            throw new UnsupportedOperationException();
        }

        public ManifestRef ref() {
            return this.refcheck;
        }

        public Content content() {
            return this.contentcheck;
        }
    }
}
