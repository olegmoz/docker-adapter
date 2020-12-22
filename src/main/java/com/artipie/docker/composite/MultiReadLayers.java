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
import com.artipie.docker.Blob;
import com.artipie.docker.Digest;
import com.artipie.docker.Layers;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Multi-read {@link Layers} implementation.
 *
 * @since 0.3
 */
public final class MultiReadLayers implements Layers {

    /**
     * Layers for reading.
     */
    private final List<Layers> layers;

    /**
     * Ctor.
     *
     * @param layers Layers for reading.
     */
    public MultiReadLayers(final List<Layers> layers) {
        this.layers = layers;
    }

    @Override
    public CompletionStage<Blob> put(final Content content, final Digest digest) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletionStage<Blob> mount(final Blob blob) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletionStage<Optional<Blob>> get(final Digest digest) {
        //todo: log exceptions
        final CompletableFuture<Optional<Blob>> promise = new CompletableFuture<>();
        CompletableFuture.allOf(
            this.layers.stream()
                .map(
                    layer -> layer.get(digest)
                        .thenAccept(
                            opt -> {
                                if (opt.isPresent()) {
                                    promise.complete(opt);
                                }
                            }
                        )
                        .toCompletableFuture()
                )
                .toArray(CompletableFuture[]::new)
        ).handle(
            (nothing, throwable) -> promise.complete(Optional.empty())
        );
        return promise;
    }
}
