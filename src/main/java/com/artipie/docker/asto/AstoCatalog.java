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
import com.artipie.docker.Catalog;
import com.artipie.docker.RepoName;
import com.artipie.docker.misc.CatalogPage;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Asto implementation of {@link Catalog}. Catalog created from list of keys.
 *
 * @since 0.9
 */
final class AstoCatalog implements Catalog {

    /**
     * Repositories root key.
     */
    private final Key root;

    /**
     * List of keys inside repositories root.
     */
    private final Collection<Key> keys;

    /**
     * From which name to start, exclusive.
     */
    private final Optional<RepoName> from;

    /**
     * Maximum number of names returned.
     */
    private final int limit;

    /**
     * Ctor.
     *
     * @param root Repositories root key.
     * @param keys List of keys inside repositories root.
     * @param from From which tag to start, exclusive.
     * @param limit Maximum number of tags returned.
     * @checkstyle ParameterNumberCheck (2 lines)
     */
    AstoCatalog(
        final Key root,
        final Collection<Key> keys,
        final Optional<RepoName> from,
        final int limit
    ) {
        this.root = root;
        this.keys = keys;
        this.from = from;
        this.limit = limit;
    }

    @Override
    public Content json() {
        return new CatalogPage(this.repos(), this.from, this.limit).json();
    }

    /**
     * Convert keys to ordered set of repository names.
     *
     * @return Ordered repository names.
     */
    private Collection<RepoName> repos() {
        return new Children(this.root, this.keys).names().stream()
            .map(RepoName.Simple::new)
            .collect(Collectors.toList());
    }
}
