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
package com.artipie.docker.asto;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.docker.RepoName;
import com.artipie.docker.Upload;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.reactivestreams.Publisher;

/**
 * Asto implementation of {@link Upload}.
 *
 * @since 0.2
 */
public final class AstoUpload implements Upload {

    /**
     * Storage.
     */
    private final Storage storage;

    /**
     * Uploads layout.
     */
    private final UploadsLayout layout;

    /**
     * Repository name.
     */
    private final RepoName name;

    /**
     * Upload UUID.
     */
    @SuppressWarnings("PMD.AvoidFieldNameMatchingMethodName")
    private final String uuid;

    /**
     * Ctor.
     *
     * @param storage Storage.
     * @param layout Uploads layout.
     * @param name Repository name.
     * @param uuid Upload UUID.
     * @checkstyle ParameterNumberCheck (2 lines)
     */
    public AstoUpload(
        final Storage storage,
        final UploadsLayout layout,
        final RepoName name,
        final String uuid
    ) {
        this.storage = storage;
        this.layout = layout;
        this.name = name;
        this.uuid = uuid;
    }

    @Override
    public String uuid() {
        return this.uuid;
    }

    @Override
    public CompletionStage<Void> start() {
        //todo: also created on start: /hashstates/sha256/0 file - with zero content
        //todo: +100 -> data is appended, /hashstates/sha256/100 file created -> range:0-99
        //todo: +464 -> data is appended, /hashstates/sha256/564 file created -> range:0-563
        return this.storage.save(this.data(), new Content.From(new byte[0])).thenCompose(
            ignored -> this.storage.save(
                new Key.From(new UploadKey(this.name, uuid), "startedat"),
                new Content.From("2020-05-19T12:58:11Z".getBytes()) //todo: current time
            )
        );
    }

    @Override
    public CompletionStage<Long> append(final Publisher<ByteBuffer> chunk) {
        return this.storage.size(this.data()).thenCompose(
            size -> {
                if (size > 0) {
                    throw new UnsupportedOperationException("Multiple chunks are not supported");
                }
                return this.storage.save(this.data(), new Content.From(chunk)).thenCompose(
                    ignored -> this.storage.size(this.data()).thenApply(updated -> updated - 1)
                );
            }
        );
    }

    @Override
    public CompletionStage<Content> content() {
        return this.storage.value(this.data());
    }

    @Override
    public CompletionStage<Long> offset() {
        return this.storage.size(this.data()).thenApply(size -> Math.max(size - 1, 0));
    }

    @Override
    public CompletionStage<Void> delete() {
        return this.storage.list(this.root())
            .thenCompose(
                list -> CompletableFuture.allOf(
                    list.stream().map(file -> this.storage.delete(file).toCompletableFuture())
                        .toArray(CompletableFuture[]::new)
                )
            );
    }

    /**
     * Root key for upload chunks.
     *
     * @return Root key.
     */
    Key root() {
        return this.layout.upload(this.name, this.uuid);
    }

    /**
     * Uploaded data key.
     *
     * @return Key.
     */
    private Key data() {
        return new Key.From(this.root(), "data");
    }
}
