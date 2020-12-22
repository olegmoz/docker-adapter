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
package com.artipie.docker.proxy;

import com.artipie.asto.Content;
import com.artipie.docker.Blob;
import com.artipie.docker.Digest;
import com.artipie.docker.RepoName;
import com.artipie.http.Headers;
import com.artipie.http.Slice;
import com.artipie.http.headers.ContentLength;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import io.reactivex.Flowable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Proxy implementation of {@link Blob}.
 *
 * @since 0.3
 */
public final class ProxyBlob implements Blob {

    /**
     * Remote repository.
     */
    private final Slice remote;

    /**
     * Repository name.
     */
    private final RepoName name;

    /**
     * Blob digest.
     */
    private final Digest dig;

    /**
     * Blob size.
     */
    private final long bsize;

    /**
     * Ctor.
     *
     * @param remote Remote repository.
     * @param name Repository name.
     * @param dig Blob digest.
     * @param size Blob size.
     * @checkstyle ParameterNumberCheck (5 lines)
     */
    public ProxyBlob(
        final Slice remote,
        final RepoName name,
        final Digest dig,
        final long size
    ) {
        this.remote = remote;
        this.name = name;
        this.dig = dig;
        this.bsize = size;
    }

    @Override
    public Digest digest() {
        return this.dig;
    }

    @Override
    public CompletionStage<Long> size() {
        return CompletableFuture.completedFuture(this.bsize);
    }

    @Override
    public CompletionStage<Content> content() {
        final CompletableFuture<Content> result = new CompletableFuture<>();
        this.remote.response(
            new RequestLine(RqMethod.GET, new BlobPath(this.name, this.dig).string()).toString(),
            Headers.EMPTY,
            Flowable.empty()
        ).send(
            (status, headers, body) -> {
                if (status != RsStatus.OK) {
                    throw new IllegalArgumentException(
                        String.format("Unexpected status: %s", status)
                    );
                }
                final CompletableFuture<Void> terminated = new CompletableFuture<>();
                result.complete(
                    new Content.From(
                        new ContentLength(headers).longValue(),
                        Flowable.fromPublisher(body)
                            .doOnError(terminated::completeExceptionally)
                            .doOnTerminate(() -> terminated.complete(null))
                    )
                );
                return terminated;
            }
        ).handle(
            (nothing, throwable) -> {
                if (throwable != null) {
                    result.completeExceptionally(throwable);
                }
                return nothing;
            }
        );
        return result;
    }
}
