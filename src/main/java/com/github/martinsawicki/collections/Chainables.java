/**
 * Copyright (c) Martin Sawicki. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */
package com.github.martinsawicki.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * This is the source of all the static methods underlying the default implementation of {@link Chainable} as well as some other conveniences.
 * @author Martin Sawicki
 *
 */
public final class Chainables {
    private Chainables() {
        throw new AssertionError("Not instantiable, just stick to the static methods.");
    }

    public interface Chainable<T> extends Iterable<T> {
        /**
         * Returns an empty chain.
         * @return an empty {@link Chainable}
         * @sawicki.similar
         * <table summary="Similar to:">
         * <tr><td><i>Java:</i></td><td>{@link java.util.stream.Stream#empty()}</td></tr>
         * <tr><td><i>C#:</i></td><td>{@code Enumerable.Empty()}</td></tr>
         * </table>
         * @see #any()
         */
        static <T> Chainable<T> empty() {
            return Chain.empty();
        }

        /**
         * Creates a new chain from the specified {@code items} in a "lazy" fashion, i.e. not traversing/evaluating the items, just holding an internal reference
         * to them.
         * @param items
         * @return a {@link Chainable} wrapper for the specified {@code items}
         * @sawicki.similar
         * <table summary="Similar to:">
         * <tr><td><i>Java:</i></td><td>{@link Collection#stream()} but operating on {@link Iterable}, so not requiring a {@link Collection} as a starting point</td></tr>
         * <tr><td><i>C#:</i></td><td>{@code Enumerable.AsEnumerable()}</td></tr>
         * </table>
         */
        static <T> Chainable<T> from(Iterable<T> items) {
            return Chain.from(items);
        }

        /**
         * Creates a new chain from the specified {@code items} in a "lazy" fashion, i.e. not traversing/evaluating/copying the items, just holding an internal reference
         * to them.
         * @param items
         * @return an {@link Chainable} wrapper for the specified {@code items}
         * @sawicki.similar
         * <table summary="Similar to:">
         * <tr><td><i>Java:</i></td><td>{@link java.util.stream.Stream#of(Object...)}</td></tr>
         * <tr><td><i>C#:</i></td><td>{@code Enumerable.AsEnumerable()}</td></tr>
         * </table>
         */
        @SafeVarargs
        static <T> Chainable<T> from(T...items) {
            return Chain.from(items);
        }

        /**
         * Creates a new chain from the specified {@code stream}, which supports multiple traversals, just like a standard {@link java.lang.Iterable},
         * even though the underlying {@link java.lang.stream.Stream} does not.
         * <p>
         * Note that upon subsequent traversals of the chain, the original stream is not recomputed, but rather its values as obtained during its
         * first traversal are cached internally and used for any subsequent traversals.
         * @param stream
         * @return a chain based on the specified {@code stream}
         */
        static <T> Chainable<T> from(Stream<T> stream) {
            if (stream == null) {
                return Chainable.empty();
            }

            return Chainable.from(new Iterable<T>() {

                List<T> cache = new ArrayList<>();
                Iterator<T> iter = stream.iterator();

                @Override
                public Iterator<T> iterator() {
                    if (this.iter == null) {
                        return this.cache.iterator();
                    } else {
                        return new Iterator<T>() {
                            @Override
                            public boolean hasNext() {
                                if (iter.hasNext()) {
                                    return true;
                                } else {
                                    iter = null;
                                    return false;
                                }
                            }

                            @Override
                            public T next() {
                                T next = iter.next();
                                cache.add(next);
                                return next;
                            }
                        };
                    }
                }
            });
        }

        /**
         * Determines whether this chain contains any items.
         * @return {@code true} if not empty (i.e. the opposite of {@link #isEmpty()})
         * @sawicki.similar
         * <table summary="Similar to:">
         * <tr><td><i>C#:</i></td><td>{@code Enumerable.Any()}</td></tr>
         * </table>
         */
        default boolean any() {
            return !Chainables.isNullOrEmpty(this);
        }

        /**
         * Ensures all items are traversed, forcing any of the predecessors in the chain to be fully evaluated.
         * <p>This is somewhat similar to {@link #toList()}, except that what is returned is still a {@link Chainable}.
         * @return self
         */
        default Chainable<T> apply() {
            return Chainables.apply(this);
        }

        /**
         * Applies the specified {@code action} to all the items in this chain, triggering a full evaluation of all the items.
         * @param action
         * @return self
         * @sawicki.similar
         * <table summary="Similar to:">
         * <tr><td><i>Java:</i></td><td>{@link java.util.stream.Stream#forEach(Consumer)}</td></tr>
         * </table>
         */
        default Chainable<T> apply(Consumer<T> action) {
            return Chainables.apply(this, action);
        }

        /**
         * Applies the specified {@code action} to each item one by one lazily, i.e. without triggering a full evaluation of the entire {@link Chainable},
         * but only to the extent that the returned {@link Chainable} is evaluated using another function.
         * @param action
         * @return self
         * @sawicki.similar
         * <table summary="Similar to:">
         * <tr><td><i>Java:</i></td><td>{@link java.util.stream.Stream#peek(Consumer)}</td></tr>
         * <tr><td><i>C#:</i></td><td>{@code Enumerable.Select()}</td></tr>
         * </table>
         */
        default Chainable<T> applyAsYouGo(Consumer<T> action) {
            return Chainables.applyAsYouGo(this, action); // TODO: shouldn't this call applyAsYouGo?
        }

        /**
         * Returns a chain of the initial items from this chain that satisfy the specified {@code condition}, stopping before the first item that does not.
         * <p>
         * For example, if the chain consists of { 1, 3, 5, 6, 7, 9, ...} and the {@code condition} checks for the oddity of each number,
         * then the returned chain will consist of only { 1, 3, 5 }
         * @param condition
         * @return items <i>before</i> the first one that fails the specified {@code condition}
         * @sawicki.similar
         * <table summary="Similar to:">
         * <tr><td><i>C#:</i></td><td>{@code Enumerable.TakeWhile()}</td></tr>
         * </table>
         * @see #asLongAsValue(Object)
         */
        default Chainable<T> asLongAs(Predicate<T> condition) {
            return (condition == null) ? this : this.before(condition.negate());
        }

        /**
         * Returns a chain of the initial items from this chain that are equal to the specified {@code value}, stopping before the first item that is not.
         * <p>
         * For example, if the chain consists of { 1, 1, 2, 1, 1, ...} and the {@code value} is 1 then the returned chain will be { 1, 1 }.
         * @param value value to match
         * @return items <i>before</i> the first one that is not equal to the specified {@code value}
         * @see #asLongAs(Predicate)
         */
        default Chainable<T> asLongAsValue(T value) {
            return Chainables.asLongAsValue(this, value);
        }

        /**
         * Determines whether this chain contains at least the specified {@code min} number of items, stopping the traversal as soon as that can be determined.
         * @param min
         * @return {@code true} if there are at least the specified {@code min} number of items in this chain
         */
        default boolean atLeast(int min) {
            return Chainables.atLeast(this, min);
        }

        /**
         * Determines whether this chain contains no more than the specified {@code max} number of items, stopping the traversal as soon as that can be determined.
         * @param max
         * @return {@code true} if there are at most the specified {@code max} number of items
         */
        default boolean atMost(int max) {
            return Chainables.atMost(this, max);
        }

        /**
         * Returns a chain of initial items from this chain before the first one that satisfies the specified {@code condition}.
         * <p>
         * For example, if this chain consists of { 1, 3, 2, 5, 6 } and the {@code condition} returns {@code true} for even numbers, then the resulting chain
         * will consist of { 1, 3 }.
         * @param condition
         * @return the initial items before and not including the one that meets the specified {@code condition}
         * @sawicki.similar
         * <table summary="Similar to:">
         * <tr><td><i>C#:</i></td><td>{@code Enumerable.TakeWhile(), but with a negated predicate}</td></tr>
         * </table>
         * @see #notBefore(Predicate)
         * @see #notAsLongAs(Predicate)
         * @see #asLongAs(Predicate)
         * @see #notAfter(Predicate)
         */
        default Chainable<T> before(Predicate<T> condition) {
            return Chainables.before(this, condition);
        }

        /**
         * Returns a chain of initial items from this chain before the specified {@code value}.
         * @param item
         * @return the initial items until one is encountered that is the same as the specified {@code item}
         * @see #before(Predicate)
         */
        default Chainable<T> beforeValue(T item) {
            return Chainables.beforeValue(this, item);
        }

        /**
         * Collects all the items into the specified collection.
         * @param targetCollection
         * @return self
         * @sawicki.similar
         * <table summary="Similar to:">
         * <tr><td><i>Java:</i></td><td>Note this is NOT like {@link java.util.stream.Stream#collect(java.util.stream.Collector)}</td></tr>
         * <tr><td><i>C#:</i></td><td>{@code Enumerable.ToList()} and the like</td></tr>
         * </table>
         */
        default Chainable<T> collectInto(Collection<T> targetCollection) {
            return Chainables.collectInto(this, targetCollection);
        }

        /**
         * Appends the specified {@code items} to this chain.
         * @param items
         * @return the chain resulting from appending the specified {@code items} to this chain
         * @sawicki.similar
         * <table summary="Similar to:">
         * <tr><td><i>Java:</i></td><td>{@link java.util.stream.Stream#concat(Stream, Stream)}, except that this is a chainable method that concatenates the specified {@code items}
         * to the {@link Chainable} it is invoked on)</td></tr>
         * <tr><td><i>C#:</i></td><td>{@code Enumerable.Concat()}</td></tr>
         * </table>
         * @see #concat(Object)
         */
        default Chainable<T> concat(Iterable<T> items) {
            return Chainables.concat(this, items);
        }

        /**
         * Appends the items from the specified {@code iterables} to this chain, in the order they are provided.
         * @param itemSequences
         * @return the current items with the specified {@code itemSequences} added to the end
         * @see #concat(Iterable)
         */
        @SuppressWarnings("unchecked")
        default Chainable<T> concat(Iterable<T>...iterables) {
            return this.concat(Chainables.concat(iterables));
        }

        /**
         * Appends the specified {@code item} to this chain.
         * @param item
         * @return the chain resulting from appending the specified single {@code item} to this chain
         * @sawicki.similar
         * <table summary="Similar to:">
         * <tr><td><i>C#:</i></td><td>{@code Enumerable.Append()}</td></tr>
         * </table>
         * @see #concat(Iterable)
         */
        default Chainable<T> concat(T item) {
            return Chainables.concat(this, item);
        }

        /**
         * Appends the items produced by the specified {@code lister} applied to the last item in this chain.
         * @param lister
         * @return the resulting chain
         * @see #concat(Iterable)
         * @see #chain(Function)
         */
        default Chainable<T> concat(Function<T, Iterable<T>> lister) {
            return Chainables.concat(this, lister);
        }

        /**
         * Determines whether this chain contains the specified {@code item}.
         * @param item the item to look for
         * @return {@code true} if this contains the specified {@code item}
         * @sawicki.similar
         * <table summary="Similar to:">\
         * <tr><td><i>C#:</i></td><td>{@code Enumerable.Contains()}</td></tr>
         * </table>
         * @see #containsAll(Object...)
         * @see #containsAny(Object...)
         * @see #containsSubarray(Iterable)
         */
        default boolean contains(T item) {
            return Chainables.contains(this, item);
        }

        /**
         * Determines whether this chain contains all of the specified {@code items}.
         * @param items items to search for
         * @return {@code true} if this chain contains all the specified {@code items}
         * @see #contains(Object)
         * @see #containsAny(Object...)
         */
        @SuppressWarnings("unchecked")
        default boolean containsAll(T...items) {
            return Chainables.containsAll(this, items);
        }

        /**
         * Determines whether this chain contains any of the specified {@code items}.
         * @param items items to search for
         * @return {@code true} if this contains any of the specified {@code items}
         * @see #contains(Object)
         * @see #containsAll(Object...)
         */
        @SuppressWarnings("unchecked")
        default boolean containsAny(T...items) {
            return Chainables.containsAny(this, items);
        }

        /**
         * Determines whether this chain contains items in the specified {@code subarray} in that exact order.
         * @param subarray
         * @return true if this contains the specified {@code subarray} of items
         * (i.e. appearing consecutively at any point)
         * @see #contains(Object)
         * @see #containsAll(Object...)
         * @see #containsAny(Object...)
         */
        default boolean containsSubarray(Iterable<T> subarray) {
            return Chainables.containsSubarray(this, subarray);
        }

        /**
         * Returns a chain of items from this chain that are not duplicated.
         * @return items that are unique (no duplicates)
         * @sawicki.similar
         * <table summary="Similar to:">
         * <tr><td><i>Java:</i></td><td>{@link java.util.stream.Stream#distinct()}</td></tr>
         * <tr><td><i>C#:</i></td><td>{@code Enumerable.Distinct()}</td></tr>
         * </table>
         */
        default Chainable<T> distinct() {
            return Chainables.distinct(this);
        }

        /**
         * Returns a chain of items from this chain without duplicate keys, as returned by the specified {@code keyExtractor}.
         * <P>
         * In case of duplicates, the first item survives.
         * @param keyExtractor
         * @return first items whose keys, as extracted by the specified {@code keyExtractor}, are unique
         * @sawicki.similar
         * <table summary="Similar to:">
         * <tr><td><i>C#:</i></td><td>{@code Enumerable.Distinct()} with a custom comparer</td></tr>
         * </table>
         */
        default <V> Chainable<T> distinct(Function<T, V> keyExtractor) {
            return Chainables.distinct(this, keyExtractor);
        }

        /**
         * Determines whether this chain ends with the members of the specified {@code suffix} in the specific order they are returned.
         * <p>
         * This triggers a full traversal/evaluation of the chain.
         * @param suffix items to match to the end of the chain
         * @return {@code true} if this chain ends with the specified {@code suffix}
         * @see #endsWithEither(Iterable...)
         * @see #startsWith(Iterable)
         */
        default boolean endsWith(Iterable<T> suffix) {
            return Chainables.endsWith(this, suffix);
        }

        /**
         * Determines whether this chain ends with any of the specified {@code suffixes}.
         * <p>
         * This triggers a full traversal/evaluation of the chain.
         * @param suffixes
         * @return {@code true} if this ends with any one of the specified {@code suffixes} of items in its specific order
         * @see #endsWith(Iterable)
         */
        @SuppressWarnings("unchecked")
        default boolean endsWithEither(Iterable<T>...suffixes) {
            return Chainables.endsWithEither(this, suffixes);
        }

        /**
         * Determines whether this chain consists of the same items, in the same order, as those in the specified {@code items}, triggering a full traversal/evaluation of the chain if needed.
         * @param items
         * @return {@code true} the items match exactly
         * @sawicki.similar
         * <table summary="Similar to:">
         * <tr><td><i>C#:</i></td><td>{@code Enumerable.SequenceEqual()}</td></tr>
         * </table>
         * @see #equalsEither(Iterable...)
         */
        default boolean equals(Iterable<T> items) {
            return Chainables.equal(this, items);
        }

        /**
         * Determines whether this chain consists of the same items, in the same order, as in any of the specified {@code iterables}.
         * <p>
         * This triggers a full traversal/evaluation of the chain if needed.
         * @param iterables
         * @return true if the underlying items are the same as those in any of the specified {@code iterables}
         * in the same order
         * @see #equalsEither(Iterable...)
         */
        @SuppressWarnings("unchecked")
        default boolean equalsEither(Iterable<T>...iterables) {
            if (iterables == null) {
                return false;
            } else {
                for (Iterable<T> iterable : iterables) {
                    if (Chainables.equal(this, iterable)) {
                        return true;
                    }
                }
            }

            return false;
        }

        /**
         * Determines whether this chain contains any items.
         * @return {@code true} if empty, else {@code false}
         * @sawicki.similar
         * <table summary="Similar to:">
         * <tr><td><i>C#:</i></td><td>{@code Enumerable.Any()}, but negated</td></tr>
         * </table>
         * @see #any()
         */
        default boolean isEmpty() {
            return Chainables.isNullOrEmpty(this);
        }

        /**
         * Returns the item tha has the highest value extracted by the specified {@code valueExtractor} in this chain.
         * <p>
         * This triggers a full traversal/evaluation of the items.
         * @param valueExtractor
         * @return the item for which the specified {@code valueExtrator} returns the highest value
         * @sawicki.similar
         * <table summary="Similar to:">
         * <tr><td><i>Java:</i></td><td>{@link java.util.stream.Stream#max(Comparator)}</td></tr>
         * <tr><td><i>C#:</i></td><td>{@code Enumerable.Max()}</td></tr>
         * </table>
         * @see #min(Function)
         */
        default T max(Function<T, Double> valueExtractor) {
            return Chainables.max(this, valueExtractor);
        }

        /**
         * Returns the item that has the lowest value extracted by the specified {@code valueExtractor} in this chain.
         * <p>
         * This triggers a full traversal/evaluation of the items.
         * @param valueExtractor
         * @return the item for which the specified {@code valueExtrator} returns the lowest value
         * @sawicki.similar
         * <table summary="Similar to:">
         * <tr><td><i>Java:</i></td><td>{@link java.util.stream.Stream#min(Comparator)}</td></tr>
         * <tr><td><i>C#:</i></td><td>{@code Enumerable.Min()}</td></tr>
         * </table>
         * @see #max(Function)
         */
        default T min(Function<T, Double> valueExtractor) {
            return Chainables.min(this, valueExtractor);
        }

        /**
         * Returns a chain of initial items from this chain upto and including the fist item that satisfies the specified {@code condition}, and none after it.
         * <p>
         * For example, if the items are { 1, 3, 5, 2, 7, 9, ...} and the {@code condition} is true when the item is an even number, then the resulting chain
         * will consist of { 1, 3, 5, 2 }.
         * @param condition
         * @return the resulting items
         * @see #notBefore(Predicate)
         * @see #asLongAs(Predicate)
         * @see #notAsLongAs(Predicate)
         */
        default Chainable<T> notAfter(Predicate<T> condition) {
            return Chainables.notAfter(this, condition);
        }

        /**
         * Returns the remaining items from this chain starting with the first one that does NOT meet the specified {@code condition}.
         * <p>
         * For example, if the chain consists of { 1, 3, 5, 2, 7, 9, ... } and the {@code condition} returns {@code true} for odd numbers,
         * then the resulting chain will be { 2, 7, 9, ... }.
         * @param condition
         * @return items starting with the first one where the specified {@code condition} is no longer met
         * @sawicki.similar
         * <table summary="Similar to:">
         * <tr><td><i>C#:</i></td><td>{@code Enumerable.SkipWhile()}</td></tr>
         * </table>
         * @see #notAfter(Predicate)
         * @see #notBefore(Predicate)
         * @see #asLongAs(Predicate)
         * @see #before(Predicate)
         */
        default Chainable<T> notAsLongAs(Predicate<T> condition) {
            return Chainables.notAsLongAs(this, condition);
        }

        /**
         * Returns the remaining items from this chain starting with the first one that is NOT the specified {@code item}.
         * @param item
         * @return items starting with the first one that is not the specified {@code item}
         * (i.e. skipping the initial items that are)
         * @see #notAsLongAs(Predicate)
         */
        default Chainable<T> notAsLongAsValue(T item) {
            return Chainables.notAsLongAsValue(this, item);
        }

        /**
         * Returns a chain of remaining items from this chain starting with the first item that satisfies the specified {@code condition} and followed by all the remaining items.
         * <p>
         * For example, if the items are { 1, 3, 5, 2, 7, 9, ...} and the {@code condition} returns {@code true} for items that are even numbers, then the resulting
         * chain will consist of { 2, 7, 9, ... }.
         * @param condition
         * @return items starting with the one where the specified {@code condition} is met
         * @sawicki.similar
         * <table summary="Similar to:">
         * <tr><td><i>C#:</i></td><td>{@code Enumerable.SkipWhile()}, but with a negated predicate</td></tr>
         * </table>
         * @see #notAfter(Predicate)
         * @see #asLongAs(Predicate)
         * @see #notAsLongAs(Predicate)
         */
        default Chainable<T> notBefore(Predicate<T> condition) {
            return Chainables.notBefore(this, condition);
        }

        /**
         * Returns a chain of remaining items from this chain starting with the specified {@item}.
         * @param item
         * @return the remaining items in this chain starting with the specified {@code item}, if any
         * @see #notBefore(Predicate)
         * @see #notAsLongAsValue(Object)
         * @see #notAsLongAs(Predicate)
         * @see #notAfter(Predicate)
         * @see #asLongAs(Predicate)
         */
        default Chainable<T> notBeforeValue(T item) {
            return Chainables.notBeforeValue(this, item);
        }

        /**
         * Returns the items from this chain that do not satisy the specified {@code condition}.
         * @param condition
         * @return items that do not meet the specified {@code condition}
         * @sawicki.similar
         * <table summary="Similar to:">
         * <tr><td><i>Java:</i></td><td>{@link java.util.stream.Stream#filter(Predicate)}, but with a negated predicate</td></tr>
         * <tr><td><i>C#:</i></td><td>{@code Enumerable.Where()}, but with a negated predicate</td></tr>
         * </table>
         * @see #where(Predicate)
         */
        default Chainable<T> notWhere(Predicate<T> condition) {
            return Chainables.notWhere(this, condition);
        }

        /**
         * Returns a chain with each item from this chain replaced with items of the same type returned by the specified {@code replacer}.
         * <p>
         * Whenever the replacer returns {@code null}, the item is skipped (de-facto removed) from the resulting chain altogether.
         * @param replacer
         * @return replacement items
         * @sawicki.similar
         * <table summary="Similar to:">
         * <tr><td><i>Java:</i></td><td>{@link java.util.stream.Stream#flatMap(Function)}, but with the return type the same as the input type</td></tr>
         * <tr><td><i>C#:</i></td><td>{@code Enumerable.Select()}, but with the return type the same as the input type</td></tr>
         * </table>
         * @see #transformAndFlatten(Function)
         */
        default Chainable<T> replace(Function<T, Iterable<T>> replacer) {
            return Chainables.replace(this, replacer);
        }

        /**
         * Returns a chain where the items are in the opposite order to this chain.
         * <p>
         * This triggers a full traversal/evaluation of the items.
         * @return items in the opposite order
         * @sawicki.similar
         * <table summary="Similar to:">
         * <tr><td><i>C#:</i></td><td>{@code Enumerable.Reverse()}</td></tr>
         * </table>
         */
        default Chainable<T> reverse() {
            return Chainables.reverse(this);
        }

        /**
         * Counts the items in this chain.
         * <p>
         * This triggers a full traversal/evaluation of the items.
         * @return total number of items
         * @sawicki.similar
         * <table summary="Similar to:">
         * <tr><td><i>Java:</i></td><td>{@link java.util.stream.Stream#count()}</td></tr>
         * <tr><td><i>C#:</i></td><td>{@code Enumerable.Count()}</td></tr>
         * </table>
         */
        default int size() {
            return Chainables.count(this);
        }

        /**
         * Determines whether the initial items in this chain are the same and in the same order as in the specified {@code prefix}.
         * @param prefix
         * @return {@code true} if this starts with the exact sequence of items in the {@code prefix}
         * @see #endsWith(Iterable)
         * @see #startsWithEither(Iterable...)
         */
        default boolean startsWith(Iterable<T> prefix) {
            return Chainables.startsWithEither(this, prefix);
        }

        /**
         * Determines whether the initial items in this chain are the same and in the same order any of the specified {@code prefixes}.
         * @param prefixes
         * @return true if this starts with any of the specified {@code prefixes} of items
         * @see #startsWith(Iterable)
         */
        @SuppressWarnings("unchecked")
        default boolean startsWithEither(Iterable<T>... prefixes) {
            return Chainables.startsWithEither(this, prefixes);
        }

        /**
         * Creates a stream from this chain.
         * @return a stream representing this chain.
         */
        default Stream<T> stream() {
            return Chainables.toStream(this);
        }

        /**
         * Computes the sum of values generated by the specified {@code valueExtractor} applied to each iten in this chain.
         * <p>
         * This trighers a full traversal/evaluation of the items.
         * @param valueExtractor
         * @return sum of all the values returned by the specified {@code valueExtractor} applied to each item
         * @sawicki.similar
         * <table summary="Similar to:">
         * <tr><td><i>Java:</i></td><td>{@link java.util.stream.Stream#reduce(java.util.function.BinaryOperator))} or {@link java.util.stream.Stream#collect(java.util.stream.Collector))}, but specifically for summation</td></tr>
         * <tr><td><i>C#:</i></td><td>{@code Enumerable.Aggregate()}, but specifically for summation</td></tr>
         * </table>
         */
        default long sum(Function<T, Long> valueExtractor) {
            return Chainables.sum(this, valueExtractor);
        }

        /**
         * Transforms this chain into a list, tigerring a full evaluation.
         * @return a new list containing all the items
         * @sawicki.similar
         * <table summary="Similar to:">
         * <tr><td><i>C#:</i></td><td>{@code Enumerable.ToList()}</td></tr>
         * </table>
         */
        default List<T> toList() {
            return Chainables.toList(this);
        }

        /**
         * Transforms each item into another item, of a possibly different type, by applying the specified {@code transformer}
         * @param transformer
         * @return the resulting items from the transformation
         * @sawicki.similar
         * <table summary="Similar to:">
         * <tr><td><i>Java:</i></td><td>{@link java.util.stream.Stream#map(Function)}</td></tr>
         * <tr><td><i>C#:</i></td><td>{@code Enumerable.Select()}</td></tr>
         * </table>
         * @see #transformAndFlatten(Function)
         */
        default <O> Chainable<O> transform(Function<T, O> transformer) {
            return Chainables.transform(this, transformer);
        }

        /**
         * Transforms each item into several other items, possibly of a different type, using the specified {@code transformer}.
         * @param transformer
         * @return the resulting items from the transformation
         * @sawicki.similar
         * <table summary="Similar to:">
         * <tr><td><i>Java:</i></td><td>{@link java.util.stream.Stream#flatMap(Function)}</td></tr>
         * <tr><td><i>C#:</i></td><td>{@code Enumerable.SelectMany()}</td></tr>
         * </table>
         * @see #transform(Function)
         */
        default <O> Chainable<O> transformAndFlatten(Function<T, Iterable<O>> transformer) {
            return Chainables.transformAndFlatten(this, transformer);
        }

        /**
         * Returns a chain of items from this chain that satisfy the specified {@code condition}.
         * @param condition
         * @return matching items
         * @sawicki.similar
         * <table summary="Similar to:">
         * <tr><td><i>Java:</i></td><td>{@link java.util.stream.Stream#filter(Predicate)}</td></tr>
         * <tr><td><i>C#:</i></td><td>{@code Enumerable.Where()}</td></tr>
         * </table>
         */
        default Chainable<T> where(Predicate<T> condition) {
            return Chainables.whereEither(this, condition);
        }

        /**
         * Returns a chain of items from this chain that satisfy any of the specified {@code conditions}.
         * @param conditions
         * @return items that meet any of the specified {@code conditions}
         * @see #where(Predicate)
         */
        @SuppressWarnings("unchecked")
        default Chainable<T> whereEither(Predicate<T>... conditions) {
            return Chainables.whereEither(this, conditions);
        }

        /**
         * Filters out {@code null} values from the underlying {@link Chainable}.
         * @return non-null items
         */
        default Chainable<T> withoutNull() {
            return Chainables.withoutNull(this);
        }
    }

    private static class Chain<T> implements Chainable<T> {
        protected Iterable<T> iterable;

        private Chain(Iterable<T> iterable) {
            this.iterable = (iterable != null) ? iterable : new ArrayList<>();
        }

        static <T> Chain<T> empty() {
            return Chain.from(Collections.emptyList());
        }

        static <T> Chain<T> from(Iterable<T> iterable) {
            if (iterable instanceof Chain<?>) {
                return (Chain<T>) iterable;
            } else {
                return new Chain<>(iterable);
            }
        }

        @SuppressWarnings("unchecked")
        static <T> Chain<T> from(T...items) {
            return Chain.from(new Iterable<T>() {

                @Override
                public Iterator<T> iterator() {
                    return new Iterator<T>() {
                        final T[] sourceItems = items;
                        int nextIndex = 0;
                        @Override
                        public boolean hasNext() {
                            return (this.sourceItems != null) ? (this.nextIndex < this.sourceItems.length) : false;
                        }

                        @Override
                        public T next() {
                            this.nextIndex++;
                            return this.sourceItems[this.nextIndex-1];
                        }
                    };
                }
            });
        }

        @Override
        public Iterator<T> iterator() {
            return this.iterable.iterator();
        }

        @Override
        public String toString() {
            return this.iterable.toString();
        }
    }

    /**
     * Determines whether the specified iterable contains at least one element.
     *
     * @param iterable the {@link java.lang.Iterable} to check
     * @return {@code true} if the specified {@code iterable} has at least one item
     * @see Chainable#any()
     */
    public static <V> boolean any(Iterable<V> iterable) {
        return !isNullOrEmpty(iterable);
    }

    /**
     * @param items
     * @param action
     * @return
     * @see Chainable#apply(Consumer)
     */
    public static <T> Chainable<T> apply(Iterable<T> items, Consumer<T> action) {
        if (items == null) {
            return null;
        } else if (action == null) {
            return Chainable.from(items);
        }

        // Apply to all
        List<T> itemsList = Chainables.toList(items);
        for (T item : itemsList) {
            try {
                action.accept(item);
            } catch (Exception e) {
                // TODO What to do with exceptions
                // String s = e.getMessage();
            }
        }

        return Chainable.from(itemsList);
    }

    /**
     * @param items
     * @return
     * @see Chainable#apply()
     */
    public static <T> Chainable<T> apply(Iterable<T> items) {
        return apply(items, o -> {}); // NOP
    }

    /**
     * @param items
     * @param action
     * @return
     * @see Chainable#applyAsYouGo(Consumer)
     */
    public static <T> Chainable<T> applyAsYouGo(Iterable<T> items, Consumer<T> action) {
        if (items == null) {
            return null;
        } else if (action == null) {
            return Chainable.from(items);
        } else {
            return Chainable.from(new Iterable<T>() {
                @Override
                public Iterator<T> iterator() {
                    return new Iterator<T>() {
                        final private Iterator<T> itemIter = items.iterator();

                        @Override
                        public boolean hasNext() {
                            return this.itemIter.hasNext();
                        }

                        @Override
                        public T next() {
                            if (this.hasNext()) {
                                T item = this.itemIter.next();
                                action.accept(item);
                                return item;
                            } else {
                                return null;
                            }
                        }
                    };
                }
            });
        }
    }

    /**
     * Returns items before the first one that does not satisfy the specified {@code condition}.
     * @param items items to return from
     * @param condition the condition for the returned items to satisfy
     * @return items before the first one is encountered taht no longer satisfies the specified condition
     */
    public static <T> Chainable<T> asLongAs(Iterable<T> items, Predicate<T> condition) {
        return (condition == null) ? Chainable.from(items) : before(items, condition.negate());
    }

    /**
     * Returns items before the first one that is not equal to the specified item.
     * @param items items to return from
     * @param item the item that returned items must be equal to
     * @return items before the first one is encountered that no longer equals the specified item
     */
    public static <T> Chainable<T> asLongAsValue(Iterable<T> items, T item) {
        return asLongAs(items, o -> o == item);
    }

    /**
     * @param items
     * @param number
     * @return true if there are at least the specified {@code min} number of {@code items}, stopping the traversal as soon as that can be determined
     * @see Chainable#atLeast(int)
     */
    public static <T> boolean atLeast(Iterable<T> items, int min) {
        if (min <= 0) {
            return true;
        } else if (items == null) {
            return false;
        }

        Iterator<T> iter = items.iterator();
        while (min > 0 && iter.hasNext()) {
            iter.next();
            min--;
        }

        return min == 0;
    }

    /**
     * @param items
     * @param max
     * @return true if there are at most the specified {@code max} number of {@code items}, stopping the traversal as soon as that can be determined
     * @see Chainable#atMost(int)
     */
    public static <T> boolean atMost(Iterable<T> items, int max) {
        if (items == null && max >= 0) {
            return true;
        } else if (items == null) {
            return false;
        }

        Iterator<T> iter = items.iterator();
        while (max > 0 && iter.hasNext()) {
            iter.next();
            max--;
        }

        return max >= 0 && !iter.hasNext();
    }

    /**
     * Returns items before the first item satisfying the specified condition is encountered.
     * @param items items to return from
     * @param condition the condition that stops further items from being returned
     * @return items before the specified condition is satisfied
     * @see Chainable#before(Predicate)
     */
    public static <T> Chainable<T> before(Iterable<T> items, Predicate<T> condition) {
        if (items == null) {
            return null;
        } else if (condition == null) {
            return Chainable.from(items);
        }

        return Chainable.from(new Iterable<T>() {

            @Override
            public Iterator<T> iterator() {
                return new Iterator<T>() {
                    private final Iterator<T> iterator = items.iterator();
                    private T nextItem = null;
                    boolean stopped = false;

                    @Override
                    public boolean hasNext() {
                        if (this.stopped) {
                            return false;
                        } else if (this.nextItem != null) {
                            return true;
                        } else if (!this.iterator.hasNext()) {
                            return false;
                        } else {
                            this.nextItem = this.iterator.next();
                            if (condition.test(this.nextItem)) {
                                this.stopped = true;
                                return false;
                            } else {
                                return true;
                            }
                        }
                    }

                    @Override
                    public T next() {
                        if (this.hasNext()) {
                            T item = this.nextItem;
                            this.nextItem = null;
                            return item;
                        } else {
                            return null;
                        }
                    }
                };
            }
        });
    }

    /**
     * Returns items until the specified item is encountered.
     * @param items items to return from
     * @param item the item which, when encountered, will stop the rest of the items from being returned
     * @return items before the specified item is encountered
     * @see Chainable#beforeValue(Object)
     */
    public static <T> Chainable<T> beforeValue(Iterable<T> items, T item) {
        return before(items, o -> o==item);
    }

    /**
     * @param items
     * @param targetCollection
     * @return
     * @see Chainable#collectInto(Collection)
     */
    public static <T> Chainable<T> collectInto(Iterable<T> items, Collection<T> targetCollection) {
        if (items == null || targetCollection == null) {
            return Chainable.from(items);
        } else {
            return Chainables.applyAsYouGo(items, o -> targetCollection.add(o));
        }
    }

    /**
     * @param items
     * @param lister
     * @return
     * @see Chainable#concat(Function)
     */
    public static <T> Chainable<T> concat(Iterable<T> items, Function<T, Iterable<T>> lister) {
        if (lister == null) {
            return Chainable.from(items);
        } else if (items == null) {
            return null;
        }

        return Chainable.from(new Iterable<T>() {
            @Override
            public Iterator<T> iterator() {

                return new Iterator<T>() {
                    private final Iterator<T> iter1 = items.iterator();
                    private Iterator<T> iter2 = null;

                    @Override
                    public boolean hasNext() {
                        return this.iter1.hasNext() || !Chainables.isNullOrEmpty(this.iter2);
                    }

                    @Override
                    public T next() {
                        if (!this.hasNext()) {
                            return null;
                        } else if (Chainables.isNullOrEmpty(this.iter2)) {
                            T item = this.iter1.next();
                            Iterable<T> items2 = lister.apply(item);
                            this.iter2 = (Chainables.isNullOrEmpty(items2)) ? null : items2.iterator();
                            return item;
                        } else {
                            return this.iter2.next();
                        }
                    }
                };
            }
        });
    }

    /**
     * Concatenates the two iterables, by first iterating through the first iterable
     * and the through the second.
     * @param items1 the first iterable
     * @param items2 the second iterable
     * @return concatenated iterable
     */
    // TODO Should this be removed now that concat(...) exists?
    public static <T> Chainable<T> concat(Iterable<T> items1, Iterable<T> items2) {
        if (items1 == null && items2 == null) {
            return null;
        } else if (Chainables.isNullOrEmpty(items1)) {
            return Chainable.from(items2);
        } else if (Chainables.isNullOrEmpty(items2)) {
            return Chainable.from(items1);
        } else {
            return Chainable.from(new Iterable<T>() {

                @Override
                public Iterator<T> iterator() {
                    return new Iterator<T>() {
                        private final Iterator<T> iter1 = items1.iterator();
                        private final Iterator<T> iter2 = items2.iterator();

                        @Override
                        public boolean hasNext() {
                            return this.iter1.hasNext() || this.iter2.hasNext();
                        }

                        @Override
                        public T next() {
                            if (this.iter1.hasNext()) {
                                return this.iter1.next();
                            } else if (this.iter2.hasNext()) {
                                return this.iter2.next();
                            } else {
                                return null;
                            }
                        }
                    };
                }
            });
        }
    }

    /**
     * Concatenates the specified iterable with the specified single item.
     *
     * @param items
     *            the iterable to concatenate the single item with
     * @param item
     *            the item to concatenate
     * @return the resulting concatenation
     */
    public static <T> Chainable<T> concat(Iterable<T> items, T item) {
        return concat(items, (Iterable<T>) Arrays.asList(item));
    }

    /**
     * @param itemSequences
     * @return
     * @see Chainable#concat(Iterable...)
     */
    @SafeVarargs
    public static <T> Chainable<T> concat(Iterable<T>...itemSequences) {
        if (Chainables.isNullOrEmpty(itemSequences)) {
            return Chainable.empty();
        } else {
            return Chainable.from(new Iterable<T>() {
                @Override
                public Iterator<T> iterator() {
                    return new Iterator<T>() {
                        private int i = 0;
                        private Iterator<T> curIter = null;

                        @Override
                        public boolean hasNext() {
                            // Get the next non-empty iterator
                            while (Chainables.isNullOrEmpty(this.curIter) && i < itemSequences.length) {
                                this.curIter = (itemSequences[i] != null) ? itemSequences[i].iterator() : null;
                                i++;
                            }

                            return (this.curIter != null) ? this.curIter.hasNext() : false;
                        }

                        @Override
                        public T next() {
                            return (this.hasNext()) ? this.curIter.next() : null;
                        }
                    };
                }
            });
        }
    }

    /**
     * @param item
     * @param items
     * @return
     * @see Chainable#concat(Iterable)
     */
    public static <T> Chainable<T> concat(T item, Iterable<T> items) {
        return concat((Iterable<T>) Arrays.asList(item), items);
    }

    /**
     * @param container
     * @param item
     * @return true if the specified {@code item} is among the members of the specified {@code container}, else false
     */
    public static <T> boolean contains(Iterable<T> container, T item) {
        if (container == null) {
            return false;
        } else if (!(container instanceof Set<?>)) {
            return !Chainables.isNullOrEmpty(Chainables.whereEither(container, i -> i.equals(item)));
        } else if (item == null) {
            return false;
        } else {
            return ((Set<?>) container).contains(item);
        }
    }

    /**
     * @param container
     * @param items
     * @return
     * @see Chainable#containsAll(Object...)
     */
    @SafeVarargs
    public static <T> boolean containsAll(T[] container, T...items) {
        return containsAll(Chainable.from(container), items);
    }

    /**
     * @param container
     * @param items
     * @return
     * @see Chainable#containsAll(Object...)
     */
    @SafeVarargs
    public static <T> boolean containsAll(Iterable<T> container, T...items) {
        Set<T> searchSet = new HashSet<>(Arrays.asList(items));
        if (container == null) {
            return false;
        } else if (items == null) {
            return true;
        } else if (container instanceof Set<?>) {
            // Fast path for wrapped sets
            return ((Set<T>) container).containsAll(searchSet);
        } else {
            for (T item : container) {
                searchSet.remove(item);
                if (searchSet.isEmpty()) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * @param container
     * @param items
     * @return true if any of the specified {@code items} are among the members of the specified {@code container}
     * @see Chainable#containsAny(Object...)
     */
    @SafeVarargs
    public static <T> boolean containsAny(Iterable<T> container, T...items) {
        if (container == null) {
            return false;
        } else if (items == null) {
            return true;
        }

        for (T item : items) {
            if (Chainables.contains(container, item)) {
                return true;
            }
        }

        return false;
    }

    /**
     * @param container
     * @param items
     * @return
     * @see Chainable#containsAny(Object...)
     */
    @SafeVarargs
    public static <T> boolean containsAny(T[] container, T...items) {
        return containsAny(Chainable.from(container), items);
    }

    /**
     * @param items
     * @param subarray
     * @return
     * @see Chainable#containsSubarray(Iterable)
     */
    public static <T> boolean containsSubarray(Iterable<T> items, Iterable<T> subarray) {
        if (items == null) {
            return false;
        } else if (Chainables.isNullOrEmpty(subarray)) {
            return true;
        }

        // Brute force evaluation of everything (TODO: make it lazy and faster?)
        List<T> subList = Chainables.toList(subarray);
        List<T> itemsCached = Chainables.toList(items);

        for (int i = 0; i < itemsCached.size() - subList.size(); i++) {
            boolean matched = true;
            for (int j = 0; j < subList.size(); j++) {
                if (!Objects.equals(itemsCached.get(i+j), subList.get(j))) {
                    matched = false;
                    break;
                }
            }

            if (matched) {
                return true;
            }
        }

        return false;
    }

    /**
     * Counts the number of items, forcing a complete traversal.
     *
     * @param items an items to count
     * @return the number of items
     * @see Chainable#size()
     */
    public static <T> int count(Iterable<T> items) {
        if (items == null) {
            return 0;
        }

        if (items instanceof Collection<?>) {
            return ((Collection<?>)items).size();
        }

        Iterator<T> iter = items.iterator();
        int size = 0;
        while (iter.hasNext()) {
            iter.next();
            size++;
        }

        return size;
    }

    /**
     * @param items
     * @param keyExtractor
     * @return
     * @see Chainable#distinct(Function)
     */
    public static <T, V> Chainable<T> distinct(Iterable<T> items, Function<T, V> keyExtractor) {
        return (keyExtractor == null) ? distinct(items) : Chainable.from(new Iterable<T>() {
            @Override
            public Iterator<T> iterator() {
                return new Iterator<T>() {
                    final Map<V, T> seen = new HashMap<>();
                    final Iterator<T> iter = items.iterator();
                    T next = null;
                    V value = null;
                    boolean hasNext = false;

                    @Override
                    public boolean hasNext() {
                        if (this.hasNext) {
                            return true;
                        }

                        while (this.iter.hasNext()) {
                            this.next = this.iter.next();
                            this.value = keyExtractor.apply(this.next);
                            if (!seen.containsKey(this.value)) {
                                this.hasNext = true;
                                return true;
                            }
                        }

                        return this.hasNext = false;
                    }

                    @Override
                    public T next() {
                        if (this.hasNext()) {
                            this.seen.put(this.value, this.next);
                            this.hasNext = false;
                            return this.next;
                        } else {
                            return null;
                        }
                    }
                };
            }});
    }

    /**
     * @param items
     * @return
     * @see Chainable#distinct()
     */
    public static <T> Chainable<T> distinct(Iterable<T> items) {
        return Chainable.from(new Iterable<T>() {
            @Override
            public Iterator<T> iterator() {
                return new Iterator<T>() {
                    final Set<T> seen = new HashSet<>();
                    final Iterator<T> iter = items.iterator();
                    T next = null;

                    @Override
                    public boolean hasNext() {
                        if (this.next != null) {
                            return true;
                        }

                        while (this.iter.hasNext()) {
                            this.next = this.iter.next();
                            if (seen.contains(this.next)) {
                                this.next = null;
                            } else {
                                return true;
                            }
                        }

                        this.next = null;
                        return false;
                    }

                    @Override
                    public T next() {
                        if (!this.hasNext()) {
                            return null;
                        } else if (this.next != null) {
                            T item = this.next;
                            this.seen.add(item);
                            this.next = null;
                            return item;
                        } else {
                            return null;
                        }
                    }
                };
            }
        });
    }

    /**
     * @param items
     * @param suffix
     * @return
     * @see Chainable#endsWith(Iterable)
     */
    public static <T> boolean endsWith(Iterable<T> items, Iterable<T> suffix) {
        return Chainables.endsWithEither(items, suffix);
    }

    /**
     * @param items
     * @param suffixes
     * @return
     * @see Chainable#endsWithEither(Iterable...)
     */
    @SafeVarargs
    public static <T> boolean endsWithEither(Iterable<T> items, Iterable<T>...suffixes) {
        if (Chainables.isNullOrEmpty(items)) {
            return false;
        } else if (suffixes == null) {
            return false;
        }

        List<T> itemList = Chainables.toList(items);
        for (Iterable<T> suffix : suffixes) {
            // Check each suffix
            List<T> suffixSequence = Chainables.toList(suffix);
            if (suffixSequence.size() > itemList.size()) {
                // If different size, assume non-match and check the next suffix
                continue;
            }

            Iterator<T> suffixIter = suffixSequence.iterator();
            int i = 0;
            boolean matching = true;
            for (i = itemList.size() - suffixSequence.size(); i < itemList.size(); i++) {
                if (!suffixIter.hasNext()) {
                    matching = false;
                    break;
                }

                T suffixItem = suffixIter.next();
                T item = itemList.get(i);
                if (suffixItem == null && item == null) {
                    // Items both null so matching so far...
                    continue;
                } else if (suffixItem == null || item == null) {
                    // Items no longer matching so bail out
                    matching = false;
                    break;
                } else if (!suffixItem.equals(item)) {
                    matching = false;
                    break;
                }
            }

            if (matching) {
                return true;
            }
        }

        return false;
    }

    /**
     * @param items1
     * @param items2
     * @return
     * @see Chainable#equals(Iterable)
     */
    public static <T> boolean equal(Iterable<T> items1, Iterable<T> items2) {
        if (items1 == items2) {
            return true;
        } else if (items1 == null || items2 == null) {
            return false;
        } else {
            Iterator<T> iterator1 = items1.iterator();
            Iterator<T> iterator2 = items2.iterator();
            while (iterator1.hasNext() && iterator2.hasNext()) {
                if (!iterator1.next().equals(iterator2.next())) {
                    return false;
                }
            }

            if (iterator1.hasNext() || iterator2.hasNext()) {
                // One is longer than the other
                return false;
            } else {
                return true;
            }
        }
    }

    /**
     * Determines whether the specified array is empty or null.
     * @param array the array to check
     * @return {@code true} if the specified array is null or empty, else {@code false}
     */
    public static boolean isNullOrEmpty(Object[] array) {
        return (array != null) ? array.length == 0 : true;
    }

    /**
     * @param iterable the {@link java.lang.Iterable} to check
     * @return {@code true} if the specified {@code iterable} is null or empty, else false
     */
    public static boolean isNullOrEmpty(Iterable<?> iterable) {
        return (iterable != null) ? !iterable.iterator().hasNext() : true;
    }

    /**
     * @param iterables
     * @return {@code true} if any of the specified {@code iterables} are null or empty
     */
    public static boolean isNullOrEmptyEither(Iterable<?>...iterables) {
        for (Iterable<?> iterable : iterables) {
            if (Chainables.isNullOrEmpty(iterable)) {
                return true;
            }
        }

        return false;
    }

    /**
     * @param map
     * @return {@code true} if the specified {@code map} is null or empty
     */
    public static <K, V> boolean isNullOrEmpty(Map<K,V> map) {
        return (map != null) ? map.isEmpty() : true;
    }

    /**
     * @param iterator
     * @return {@code true} if the specified {@code iterator} is null or empty
     */
    public static <V> boolean isNullOrEmpty(Iterator<V> iterator) {
        return (iterator != null) ? !iterator.hasNext() : true;
    }

    /**
     * Joins the items produced by the specified {@code iterator} into a single string, invoking {@code toString()) on each item,
     * separating each string with the specified {@code delimiter}, skipping {@code null} values.
     * @param delimiter the text to insert between items
     * @param iterator the iterator to traverse
     * @return the joined string
     */
    public static <T> String join(String delimiter, Iterator<T> iterator) {
        if (iterator == null) {
            return null;
        }

        StringBuilder info = new StringBuilder();
        while (iterator.hasNext()) {
            T next = iterator.next();
            if (next != null) {
                info
                    .append(next.toString())
                    .append(delimiter);

            }
        }

        if (info.length() > delimiter.length()) {
            info.setLength(info.length() - delimiter.length());
        }

        return info.toString();
    }

    /**
     * Joins the items in specified {@code stream} into a single string, applying a {@code toString()} to each item and separating them with the specified
     * {@code delimiter, skipping {@code null} values..
     * @param delimiter the text to insert between consecutive strings
     * @param stream the stream whose items are to be joined
     * @return the joined string
     */
    public static <T> String join(String delimiter, Stream<T> stream) {
        return (stream != null) ? Chainables.join(delimiter, stream.iterator()) : null;
    }

    /**
     * Joins the specified {@code items} into a single string, invoking {@code toString()}) on each, separating them with the specified {@code delimiter},
     * skipping {@code null} values.
     * @param delimiter the text to insert between the items
     * @param items the items to join
     * @return the joined string
     */
    public static <T> String join(String delimiter, Iterable<T> items) {
        return join(delimiter, items.iterator());
    }

    /**
     * @param items
     * @param valueExtractor
     * @return
     * @see Chainable#max(Function)
     */
    public static <T> T max(Iterable<T> items, Function<T, Double> valueExtractor) {
        Double max = null;
        T maxItem = null;
        if (!Chainables.isNullOrEmpty(items)) {
            for (T item : items) {
                Double number = valueExtractor.apply(item);
                if (max == null || number > max) {
                    max = number;
                    maxItem = item;
                }
            }
        }

        return maxItem;
    }

    /**
     * @param items
     * @param valueExtractor
     * @return
     * @see Chainable#min(Function)
     */
    public static <T> T min(Iterable<T> items, Function<T, Double> valueExtractor) {
        Double min = null;
        T minItem = null;
        if (!Chainables.isNullOrEmpty(items)) {
            for (T item : items) {
                Double number = valueExtractor.apply(item);
                if (min == null || number < min) {
                    min = number;
                    minItem = item;
                }
            }
        }

        return minItem;
    }

    /**
     * Returns items until and including the first item satisfying the specified condition, and no items after that
     * @param items items to return from
     * @param condition the condition that the last item needs to meet
     * @return items before and including the first item where the specified condition is satisfied
     * @see Chainable#notAfter(Predicate)
     */
    public static <T> Chainable<T> notAfter(Iterable<T> items, Predicate<T> condition) {
        if (items == null) {
            return null;
        } else if (condition == null) {
            return Chainable.from(items);
        }

        return Chainable.from(new Iterable<T>() {

            @Override
            public Iterator<T> iterator() {
                return new Iterator<T>() {
                    private final Iterator<T> iterator = items.iterator();
                    private T nextItem = null;
                    boolean stopped = false;

                    @Override
                    public boolean hasNext() {
                        if (this.stopped) {
                            // Last item if any
                            return this.nextItem != null;
                        } else if (this.nextItem != null) {
                            return true;
                        } else if (!this.iterator.hasNext()) {
                            this.stopped = true;
                            this.nextItem = null;
                            return false;
                        } else {
                            this.nextItem = this.iterator.next();
                            if (condition.test(this.nextItem)) {
                                this.stopped = true;
                            }

                            return true;
                        }
                    }

                    @Override
                    public T next() {
                        if (this.hasNext()) {
                            T item = this.nextItem;
                            this.nextItem = null;
                            return item;
                        } else {
                            return null;
                        }
                    }
                };
            }
        });
    }

    /**
     * @param items
     * @param condition
     * @return
     * @see Chainable#notAsLongAs(Predicate)
     */
    public static <T> Chainable<T> notAsLongAs(Iterable<T> items, Predicate<T> condition) {
        return (items != null) ? Chainable.from(items).notBefore(condition.negate()) : null;
    }

    /**
     * @param items
     * @param value
     * @return
     * @see Chainable#notAsLongAsValue(Object)
     */
    public static <T> Chainable<T> notAsLongAsValue(Iterable<T> items, T value) {
        return notBefore(items, o -> o!=value);
    }

    /**
     * @param items
     * @param condition
     * @return
     * @see Chainable#notBefore(Predicate)
     */
    //##
    private static <T> Chainable<T> notBefore(Iterable<T> items, Predicate<T> condition) {
        if (items == null) {
            return null;
        } else if (condition == null) {
            return Chainable.from(items);
        } else {
            return Chainable.from(new Iterable<T>() {
                @Override
                public Iterator<T> iterator() {
                    return new Iterator<T>() {
                        final Iterator<T> iterator = items.iterator();
                        T nextItem = null;
                        boolean start = false;

                        @Override
                        public boolean hasNext() {
                            if (this.nextItem != null) {
                                return true;
                            } else if (!this.iterator.hasNext()) {
                                this.nextItem = null;
                                return false;
                            } else if (this.start) {
                                this.nextItem = this.iterator.next();
                                return true;
                            } else {
                                while (this.iterator.hasNext()) {
                                    this.nextItem = this.iterator.next();
                                    if (condition.test(this.nextItem)) {
                                        this.start = true;
                                        break;
                                    } else {
                                        this.nextItem = null;
                                    }
                                }

                                return this.nextItem != null;
                            }
                        }

                        @Override
                        public T next() {
                            if (!this.hasNext()) {
                                return null;
                            }

                            T item = this.nextItem;
                            this.nextItem = null;
                            return item;
                        }
                    };
                }
            });
        }
    }

    /**
     * Returns the rest of the specified items starting with the specified item, if found.
     * @param items items to skip over
     * @param item item to skip until
     * @return the rest of the items
     * @see Chainable#notBeforeValue(Object)
     */
    public static <T> Chainable<T> notBeforeValue(Iterable<T> items, T item) {
        return notBefore(items, (Predicate<T>)(o -> o == item));
    }

    /**
     * @param items
     * @param condition
     * @return
     * @see Chainable#notWhere(Predicate)
     */
    public static final <T> Chainable<T> notWhere(Iterable<T> items, Predicate<T> condition) {
        return (condition != null) ? Chainables.whereEither(items, condition.negate()) : Chainable.from(items);
    }

    /**
     * @param items
     * @param replacer
     * @return
     * @see Chainable#replace(Function)
     */
    public static <T> Chainable<T> replace(Iterable<T> items, Function<T, Iterable<T>> replacer) {
        return transformAndFlatten(items, replacer).withoutNull();
    }

    /**
     * @param items
     * @return
     * @see Chainable#reverse()
     */
    public static <T> Chainable<T> reverse(Iterable<T> items) {
        if (items == null) {
            return Chainable.from(Arrays.asList());
        } else {
            return Chainable.from(new Iterable<T>() {
                @Override
                public Iterator<T> iterator() {
                    return new Iterator<T>() {
                        List<T> list = Chainables.toList(items);
                        int nextIndex = list.size() - 1;

                        @Override
                        public boolean hasNext() {
                            return (nextIndex >= 0);
                        }

                        @Override
                        public T next() {
                            return (this.hasNext()) ? list.get(this.nextIndex--) : null;
                        }
                    };
                }
            });
        }
    }

    /**
     * Splits the specified {@code text} using the specified {@code delimiterChars}.
     * @param text
     * @param delimiterCharacters
     * @return the split strings, including the delimiters
     */
    public static Chainable<String> split(String text, String delimiterCharacters) {
        return split(text, delimiterCharacters, true);
    }

    /**
     * Splits the specified {@code text} using the specified {@code delimiterChars}.
     * @param text
     * @param delimiterCharacters
     * @param includeDelimiters if true, the delimiter chars are included in the returned results
     * @return the split strings
     */
    public static Chainable<String> split(String text, String delimiterCharacters, boolean includeDelimiters) {
        if (text == null || delimiterCharacters == null) {
            return null;
        } else {
            return Chainable.from(new Iterable<String>() {
                @Override
                public Iterator<String> iterator() {
                    return new Iterator<String>() {
                        StringTokenizer tokenizer = new StringTokenizer(text, delimiterCharacters, includeDelimiters);

                        @Override
                        public boolean hasNext() {
                            return this.tokenizer.hasMoreTokens();
                        }

                        @Override
                        public String next() {
                            return this.tokenizer.nextToken();
                        }
                    };
                }
            });
        }
    }

    /**
     * @param items
     * @param prefixes
     * @return
     * @see Chainable#startsWithEither(Iterable...)
     */
    @SafeVarargs
    public static <T> boolean startsWithEither(Iterable<T> items, Iterable<T>... prefixes) {
        if (Chainables.isNullOrEmpty(items)) {
            return false;
        } else if (prefixes == null) {
            return false;
        }

        for (Iterable<T> prefix : prefixes) {
            Iterator<T> prefixIterator = prefix.iterator();
            for (T item : items) {
                if (!prefixIterator.hasNext()) {
                    return true;
                }

                T prefixItem = prefixIterator.next();
                if (prefixItem == item) {
                    continue;
                } else if (prefixItem == null) {
                    break;
                } else if (!prefixItem.equals(item)) {
                    break;
                }
            }

            // Nothing left in prefix to match so it's a match
            if (!prefixIterator.hasNext()) {
                return true;
            }
        }

        return false;
    }

    /**
     * @param items
     * @param valueExtractor
     * @return
     * @see Chainable#sum(Function)
     */
    public static <T> long sum(Iterable<T> items, Function<T, Long> valueExtractor) {
        int sum = 0;
        if (!Chainables.isNullOrEmpty(items)) {
            Chainable<Long> numbers = Chainables.withoutNull(items).transform(valueExtractor);
            for (Long number : numbers) {
                if (number != null) {
                    sum += number;
                }
            }
        }

        return sum;
    }

    /**
     * @param items
     * @return
     */
    public static String[] toArray(Iterable<String> items) {
        int len;
        if (items == null) {
            len = 0;
        } else {
            len = count(items);
        }

        String[] array = new String[len];
        int i = 0;
        for (String item : items) {
            array[i++] = item;
        }

        return array;
    }

    /**
     * @param items
     * @return
     * @see Chainable#toList()
     */
    public static <T> List<T> toList(Iterable<T> items) {
        if (items == null) {
            return null;
        } else if (items instanceof List<?>) {
            return (List<T>) items;
        } else {
            List<T> list = new ArrayList<>();
            for (T item : items) {
                list.add(item);
            }

            return list;
        }
    }

    /**
     * Converts the specified {@code items} into a sequential stream.
     * @param items the items to convert into a stream
     * @return the resulting stream
     * @see Chainable#stream()
     */
    public static <T> Stream<T> toStream(Iterable<T> items) {
        return StreamSupport.stream(items.spliterator(), false);
    }

    /**
     * Uses the specified transformer function to transform the specified items and returns the resulting items.
     * @param items items to be transformed (LINQ: select())
     * @param transformer function performing the transformation
     * @return the transformed items
     * @see Chainable#transform(Function)
     */
    public static <I, O> Chainable<O> transform(Iterable<I> items, Function<I, O> transformer) {
        if (items == null || transformer == null) {
            return null;
        }

        // TODO: transform should perhaps ignore NULL?
        return Chainable.from(new Iterable<O>() {
            @Override
            public Iterator<O> iterator() {
                return new Iterator<O>() {
                    Iterator<I> iterator = items.iterator();

                    @Override
                    public boolean hasNext() {
                        return this.iterator.hasNext();
                    }

                    @Override
                    public O next() {
                        return (this.iterator.hasNext()) ?
                                transformer.apply(this.iterator.next()) : null;
                    }
                };
            }
        });
    }

    /**
     * @param items
     * @param transformer
     * @return
     * @see Chainable#transformAndFlatten(Function)
     */
    public static <I, O> Chainable<O> transformAndFlatten(Iterable<I> items, Function<I, Iterable<O>> transformer) {
        if (items == null || transformer == null) {
            return null;
        }

        return Chainable.from(new Iterable<O>() {
            @Override
            public Iterator<O> iterator() {
                return new Iterator<O>() {
                    private final Iterator<I> iterIn = items.iterator();
                    private Iterator<O> iterOut = null;
                    private boolean stopped = false;

                    @Override
                    public boolean hasNext() {
                        if (stopped) {
                            return false;
                        } else if (!Chainables.isNullOrEmpty(this.iterOut)) {
                            return true;
                        } else {
                            while (this.iterIn.hasNext()) {
                                I itemIn = this.iterIn.next();
                                Iterable<O> results = transformer.apply(itemIn);
                                if (!Chainables.isNullOrEmpty(results)) {
                                    this.iterOut = results.iterator();
                                    return true;
                                }
                            }

                            this.stopped = true;
                            return false;
                        }
                    }

                    @Override
                    public O next() {
                        return this.hasNext() ? this.iterOut.next() : null;
                    }
                };
            }
        });
    }

    /**
     * @param items
     * @param predicates
     * @return
     * @see Chainable#whereEither(Predicate...)
     */
    @SafeVarargs
    public static final <T> Chainable<T> whereEither(Iterable<T> items, Predicate<T>... predicates) {
        if (items == null) {
            return null;
        } else if (predicates == null || predicates.length == 0) {
            return Chainable.from(items);
        }

        return Chainable.from(new Iterable<T>() {

            @Override
            public Iterator<T> iterator() {
                return new Iterator<T>() {
                    final Iterator<T> innerIterator = items.iterator();
                    T nextItem = null;
                    boolean stopped = false;

                    @Override
                    public boolean hasNext() {
                        if (this.stopped) {
                            return false;
                        } else if (this.nextItem != null) {
                            return true;
                        }

                        while (this.innerIterator.hasNext()) {
                            this.nextItem = this.innerIterator.next();

                            // Skip over null items TODO: really?
                            if (this.nextItem == null) {
                                continue;
                            }

                            for (Predicate<T> predicate : predicates) {
                                if (predicate.test(this.nextItem)) {
                                    return true;
                                }
                            }
                        }

                        this.nextItem = null;
                        this.stopped = true;
                        return false;
                    }

                    @Override
                    public T next() {
                        if (this.hasNext()) {
                            T item = this.nextItem;
                            this.nextItem = null;
                            return item;
                        } else {
                            return null;
                        }
                    }
                };
            }
        });
    }

    /**
     * @param items
     * @return
     * @see Chainable#withoutNull()
     */
    public static <T> Chainable<T> withoutNull(Iterable<T> items) {
        return (items != null) ? Chainable.from(items).where(i -> i != null) : null;
    }
}

