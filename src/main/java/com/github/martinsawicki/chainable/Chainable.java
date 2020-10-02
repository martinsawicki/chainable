/**
 * Copyright (c) Martin Sawicki. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */
package com.github.martinsawicki.chainable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import java.util.function.ToLongFunction;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import com.github.martinsawicki.annotation.Experimental;
import com.github.martinsawicki.chainable.Chainables.Chain;
import com.github.martinsawicki.function.ToStringFunction;

/**
 * {@link Chainable} is a fluent interface-style sub type of {@link java.lang.Iterable} with additional methods facilitating the use of the
 * iterator pattern, functional programming and lazy evaluation, intended for achieving code that is more succinct, readable, simpler to implement
 * and sometimes faster than its non-lazy/non-functional equivalent.
 * <p>
 * {@link Chainable} is somewhat analogous to and inspired by C#'s {@code Enumerable<T>} (LINQ), and conceived of before but ultimately also
 * somewhat overlapping with Java 8's {@link java.util.stream.Stream}.
 * <p>
 * One of the key differences from {@link java.util.stream.Stream} is that {@link Chainable} fully preserves the functional and
 * re-entrancy semantics of {@link java.lang.Iterable}, i.e. it can be traversed multiple times, with multiple iterator instantiations,
 * whereas {@link java.util.stream.Stream} cannot be.
 * <p>
 * Also, the {@link Chainable} API surface contains various unique convenience methods, as {@link Chainable} is intended primarily for sequential
 * access and not so much the parallelism that has been a key guiding design principle behind Java's {@link Stream}.
 * <p>
 * Having said that, a basic level of interoperability between {@link java.util.stream.Stream} and {@link Chainable} is possible: a chain can
 * be created from a stream (see {@link Chainable#from(Stream)}) and a stream can be created from a chain (see {@link Chainable#stream()}).
 * <p>
 * (A note on the vocabulary: {@link Chainable} is the interface, whereas the word "chain" is used throughout the documentation to refer to a
 * specific instance of a {@link Chainable}).
 *
 * @author Martin Sawicki
 *
 * @param <T>
 */
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
     * Creates a new chain from the specified {@code items} in a "lazy" fashion, not traversing/evaluating the items,
     * just holding internal references to them.
     * @param items the items to create the chain from
     * @return a chain for the specified {@code items}
     * @sawicki.similar
     * <table summary="Similar to:">
     * <tr><td><i>Java:</i></td><td>{@link Collection#stream()} but operating on {@link Iterable}, so not requiring a {@link Collection} as its starting point</td></tr>
     * <tr><td><i>C#:</i></td><td>{@code Enumerable.AsEnumerable()}</td></tr>
     * </table>
     */
    static <T> Chainable<T> from(Iterable<T> items) {
        return Chain.from(items);
    }

    /**
     * Creates a new chain from the specified {@code items} in a "lazy" fashion, not traversing/evaluating/copying the items,
     * just holding internal references to them.
     * @param items the items to create a chain from
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
     * Creates a new chain from the specified {@code stream}, which supports multiple traversals, just like a
     * standard {@link java.lang.Iterable}, even though the underlying {@link java.util.stream.Stream} does not.
     * <p>
     * Note that upon subsequent traversals of the chain, the original stream is not recomputed, but rather its values as
     * obtained during its first traversal are cached internally and used for any subsequent traversals.
     * @param stream the stream to create a chain from
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
     * Returns a chain of items after the first one in this chain.
     * @return items following the first one
     * @sawicki.similar
     * <table summary="Similar to:">
     * <tr><td><i>Java:</i></td><td>{@link java.util.stream.Stream#skip(long)} with 1 as the number to skip</td></tr>
     * <tr><td><i>C#:</i></td><td>{@code Enumerable.Skip()}</td></tr>
     * </table>
     */
    default Chainable<T> afterFirst() {
        return Chainables.afterFirst(this);
    }

    /**
     * Returns a chain after skipping the first specified number of items.
     * @param number the number of initial items to skip
     * @return the remaining chain
     * @sawicki.similar
     * <table summary="Similar to:">
     * <tr><td><i>Java:</i></td><td>{@link java.util.stream.Stream#skip(long)}</td></tr>
     * <tr><td><i>C#:</i></td><td>{@code Enumerable.Skip()}</td></tr>
     * </table>
     */
    default Chainable<T> afterFirst(long number) {
        return Chainables.afterFirst(this, number);
    }

    /**
     * Determines whether all the items in this chain satisfy the specified {@code condition}.
     * @param condition
     * @return {@code true} if all items satisfy the specified {@code condition}, otherwise {@code false}
     * @sawicki.similar
     * <table summary="Similar to:">
     * <tr><td><i>Java:</i></td><td>{@link java.util.stream.Stream#allMatch(Predicate)}</td></tr>
     * <tr><td><i>C#:</i></td><td>{@code Enumerable.All()}</td></tr>
     * </table>
     */
    default boolean allWhere(Predicate<T> condition) {
        return Chainables.allWhere(this, condition);
    }

    /**
     * Determines whether all the items in this chain satisfy any of the specified {@code conditions}
     * @param conditions
     * @return {@code true} if all items satisfy at least one of the {@code conditions}, otherwise {@code false}
     * @see #allWhere(Predicate)
     */
    @SuppressWarnings("unchecked")
    default boolean allWhereEither(Predicate<T>...conditions) {
        return Chainables.allWhereEither(this, conditions);
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
     * Determines whether any of the items in this chain satisfy the specified {@code condition}.
     * @param condition the condition to satisfy
     * @return {@code true} if there are any items that satisfy the specified {@code condition}
     * @sawicki.similar
     * <table summary="Similar to:">
     * <tr><td><i>Java:</i></td><td>{@link java.util.stream.Stream#anyMatch(Predicate)}</td></tr>
     * <tr><td><i>C#:</i></td><td>{@code Enumerable.Any(Func)}</td></tr>
     * </table>
     */
    default boolean anyWhere(Predicate<T> condition) {
        return Chainables.anyWhere(this, condition);
    }

    /**
     * Determines whether any of the items in this chain satisfy any of the specified {@code conditions}.
     * @param conditions the conditions to satisfy
     * @return true if there are any items that satisfy any of the specified {@code conditions}
     * @see #anyWhere(Predicate)
     */
    @SuppressWarnings("unchecked")
    default boolean anyWhereEither(Predicate<T>... conditions) {
        return Chainables.anyWhereEither(this, conditions);
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
     * Applies the specified {@code action} to each item one by one lazily, that is without triggering a full
     * evaluation of the entire chain, but only to the extent that the returned chain is evaluated using another function.
     * @param action
     * @return self
     * @sawicki.similar
     * <table summary="Similar to:">
     * <tr><td><i>Java:</i></td><td>{@link java.util.stream.Stream#peek(Consumer)}</td></tr>
     * <tr><td><i>C#:</i></td><td>{@code Enumerable.Select()}</td></tr>
     * </table>
     * @see #apply()
     * @see #apply(Consumer)
     */
    default Chainable<T> applyAsYouGo(Consumer<T> action) {
        return Chainables.applyAsYouGo(this, action); // TODO: shouldn't this call applyAsYouGo?
    }

    /**
     * Sorts in the ascending order by an automatically detected key based on the first item in the chain.
     * <P>
     * If the item type in the chain is {@link String}, or {@link Double}, or {@link Long}, then the value is used as the key.
     * For other types, the return value of {@code toString()} is used as the key.
     * <P>
     * Note this triggers a full traversal/evaluation of the chain.
     * @return sorted items
     * @see #descending()
     */
    default Chainable<T> ascending() {
        return Chainables.ascending(this);
    }

    /**
     * Sorts the items in this chain in the ascending order based on the {@link String}
     * keys returned by the specified {@code keyExtractor} applied to each item.
     * <P>
     * Note this triggers a full traversal/evaluation of the chain.
     * @param keyExtractor
     * @return sorted items
     * @sawicki.similar
     * <table summary="Similar to:">
     * <tr><td><i>Java:</i></td><td>{@link java.util.stream.Stream#sorted(Comparator)}, but specific to {@link String} outputs</td></tr>
     * <tr><td><i>C#:</i></td><td>{@code Enumerable.ThenBy()}</td></tr>
     * </table>
     * @see #descending(ToStringFunction)
     */
    default Chainable<T> ascending(ToStringFunction<T> keyExtractor) {
        return Chainables.ascending(this, keyExtractor);
    }

    /**
     * Sorts the items in this chain in the ascending order based on the {@link Long}
     * keys returned by the specified {@code keyExtractor} applied to each item.
     * <P>
     * Note this triggers a full traversal/evaluation of the chain.
     * @param keyExtractor
     * @return sorted items
     * @sawicki.similar
     * <table summary="Similar to:">
     * <tr><td><i>Java:</i></td><td>{@link java.util.stream.Stream#sorted(Comparator)}, but specific to {@link Long} outputs</td></tr>
     * <tr><td><i>C#:</i></td><td>{@code Enumerable.ThenBy()}</td></tr>
     * </table>
     * @see #descending(ToLongFunction)
     */
    default Chainable<T> ascending(ToLongFunction<T> keyExtractor) {
        return Chainables.ascending(this, keyExtractor);
    }

    /**
     * Sorts the items in this chain in the ascending order based on the {@link Double}
     * keys returned by the specified {@code keyExtractor} applied to each item.
     * <P>
     * Note this triggers a full traversal/evaluation of the chain.
     * @param keyExtractor
     * @return sorted items
     * @sawicki.similar
     * <table summary="Similar to:">
     * <tr><td><i>Java:</i></td><td>{@link java.util.stream.Stream#sorted(Comparator)}, but specific to {@link Double} outputs</td></tr>
     * <tr><td><i>C#:</i></td><td>{@code Enumerable.ThenBy()}</td></tr>
     * </table>
     * @see #descending(ToDoubleFunction)
     */
    default Chainable<T> ascending(ToDoubleFunction<T> keyExtractor) {
        return Chainables.ascending(this, keyExtractor);
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
     * @see #asLongAsEquals(Object)
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
    default Chainable<T> asLongAsEquals(T value) {
        return Chainables.asLongAsEquals(this, value);
    }

    /**
     * Returns a chain of items from this chain that are of the same type as the specified {@code example}.
     * <p>
     * For example, consider a mixed collection of super-classes and subclasses, or hybrid interfaces.
     * @param example
     * @return only those items that are of the same type as the specified {@code example}
     */
    default <O> Chainable<O> ofType(O example) {
        return Chainables.ofType(this, example);
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
     * Traverses the items in this chain in a breadth-first order as if it were a tree, where for each item, the items output by the specified
     * {@code childExtractor} applied to it are appended at the end of the chain.
     * <p>
     * To indicate the absence of children for an item, the child extractor may output {@code null}.
     * <p>
     * The traversal protects against potential cycles by not visiting items that satisfy the equality ({@code equals()})
     * check against an item already seen before.
     * @param childExtractor
     * @return resulting chain
     * @see #breadthFirstUntil(Function, Function)
     * @see #breadthFirstWhile(Function, Function)
     * @see #depthFirst(Function)
     */
    default Chainable<T> breadthFirst(Function<T, Iterable<T>> childExtractor) {
        return Chainables.breadthFirst(this, childExtractor);
    }

    /**
     * Traverses the items in this chain in a breadth-first order as if it were a tree, where for each item, the items output by the specified
     * {@code childExtractor} applied to it are appended at the end of the chain, <i>up to and including</i> the parent item that satisfies the specified
     * {@code condition}, but not its descendants that would be otherwise returned by the {@code childExtractor}.
     * <p>
     * It can be thought of trimming the breadth-first traversal of a hypothetical tree right below the level of the item satisfying
     * the {@code condition}, but continuing with other items in the chain.
     * <p>
     * To indicate the absence of children for an item, the child extractor may output {@code null}.
     * <p>
     * The traversal protects against potential cycles by not visiting items that satisfy the equality ({@code equals()}) check against an item already seen before.
     * @param childExtractor
     * @param condition
     * @return resulting chain
     * @see #breadthFirstWhile(Function, Function)
     * @see #breadthFirst(Function)
     * @see #depthFirst(Function)
     */
    default Chainable<T> breadthFirstUntil(Function<T, Iterable<T>> childExtractor, Function<T, Boolean> condition) {
        return Chainables.breadthFirstUntil(this, childExtractor, condition);
    }

    /**
     * Traverses the items in this chain in a breadth-first order as if it's a tree, where for each item, those of its children returned by the specified
     * {@code childTraverser} are appended to the end of the chain that satisfy the specified {@code condition}.
     * <p>
     * It can be thought of trimming the breadth-first traversal of a hypothetical tree right above the level of each item satisfying
     * the {@code condition}, but continuing with other items in the chain.
     * @param childExtractor
     * @param condition
     * @return resulting chain
     * @see #breadthFirstUntil(Function, Function)
     * @see #breadthFirst(Function)
     * @see #depthFirst(Function)
     */
    default Chainable<T> breadthFirstWhile(Function<T, Iterable<T>> childExtractor, Function<T, Boolean> condition) {
        return Chainables.breadthFirstWhile(this, childExtractor, condition);
    }

    /**
     * Creates a chain that caches its evaluated items, once a full traversal is completed, so that subsequent traversals no longer
     * re-evaluate each item but fetch them directly from the cache populated by the first traversal.
     * <p>
     * Note that if the first traversal is only partial (i.e. it does not reach the end) the cache is not yet activated, so the next traversal will still
     * re-evaluate each item from the beginning, as if run for the first time.
     * <p>
     * If there are multiple iterators used from the chain, the first iterator to complete the traversal wins as far as cache population goes.
     * The remaining iterators will continue unaffected, but their results, if different from the results of the first finished
     * iterator, will be ignored for caching purposes.
     * @return a chain that, upon the completion of the first full traversal, behaves like a fixed value list
     */
    default Chainable<T> cached() {
        return Chainables.cached(this);
    }

    /**
     * Casts the items in this chain to the specified class.
     * @param clazz
     * @return items as cast to the type indicated by the specified {@code clazz}
     * @sawicki.similar
     * <table summary="Similar to:">
     * <tr><td><i>Java:</i></td><td>{@link java.util.stream.Stream#map(Function)}, where the specified function casts each item to the specified type</td></tr>
     * <tr><td><i>C#:</i></td><td>{@code Enumerable.Cast()}</td></tr>
     * </table>
     */
    default <T2> Chainable<T2> cast(Class<T2> clazz) {
        return Chainables.cast(this, clazz);
    }

    /**
     * Appends to the chain the result of the specified {@code nextItemExtractor} applied to the last item, unless the last item is null.
     * <p>
     * If the {@code nextItemExtractor} returns {@code null}, that is considered as the end of the chain and is not included in the resulting chain.
     * @param nextItemExtractor
     * @return resulting {@link Chainable}
     * @sawicki.similar
     * <table summary="Similar to:">
     * <tr><td><i>Java:</i></td><td>{@link java.util.stream.Stream#iterate(Object, java.util.function.UnaryOperator)}, except that the "seed" is just the last item of the underlying {@link Chainable}</td></tr>
     * </table>
     */
    default Chainable<T> chain(UnaryOperator<T> nextItemExtractor) {
        return Chainables.chain(this, nextItemExtractor);
    }

    /**
     * If the last of this chain satisfies the specified {@code condition}, then the result of the specified {@code nextItemExtractor}
     * applied to that last item is appended to the chain.
     * @param condition
     * @param nextItemExtractor
     * @return resulting {@link Chainable}
     * @see #chain(UnaryOperator)
     */
    default Chainable<T> chainIf(Predicate<T> condition, UnaryOperator<T> nextItemExtractor) {
        return Chainables.chainIf(this, condition, nextItemExtractor);
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
     * @param iterables
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
     */
    default Chainable<T> concat(Function<T, Iterable<T>> lister) {
        return Chainables.concat(this, lister);
    }

    /**
     * Determines whether this chain contains the specified {@code item}.
     * @param item the item to look for
     * @return {@code true} if this contains the specified {@code item}
     * @sawicki.similar
     * <table summary="Similar to:">
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
    default long count() {
        return Chainables.count(this);
    }

    /**
     * Traverses the items in a depth-first manner, by visiting the children of each item in the chain, as returned by the
     * specified {@code childExtractor} before visting its siblings, in a de-facto recursive manner.
     * <p>
     * For items that do not have children, the {@code childExtractor} may return {@code null}.
     * <p>
     * Note that the traversal protects against potential cycles by not visiting items that satisfy the equality ({@code equals()}) check against an item already seen before.
     * @param childExtractor
     * @return resulting chain
     * @see #breadthFirst(Function)
     */
    default Chainable<T> depthFirst(Function<T, Iterable<T>> childExtractor) {
        return Chainables.depthFirst(this, childExtractor);
    }

    /**
     * Sorts in the descending order by an automatically detected key based on the first item in the chain.
     * <P>
     * If the item type in the chain is {@link String}, or {@link Double}, or {@link Long}, then the value is used as the key.
     * For other types, the return value of {@code toString()} is used as the key.
     * <P>
     * Note this triggers a full traversal/evaluation of the chain.
     * @return sorted items
     * @see #ascending()
     */
    default Chainable<T> descending() {
        return Chainables.descending(this);
    }

    /**
     * Sorts the items in this chain in the descending order based on the {@link Long}
     * keys returned by the specified {@code keyExtractor} applied to each item.
     * <P>
     * Note this triggers a full traversal/evaluation of the chain.
     * @param keyExtractor
     * @return sorted items
     * @sawicki.similar
     * <table summary="Similar to:">
     * <tr><td><i>Java:</i></td><td>{@link java.util.stream.Stream#sorted(Comparator)}, but specific to {@link Long} outputs</td></tr>
     * <tr><td><i>C#:</i></td><td>{@code Enumerable.ThenByDescending()}</td></tr>
     * </table>
     * @see #ascending(ToLongFunction)
     */
    default Chainable<T> descending(ToLongFunction<T> keyExtractor) {
        return Chainables.descending(this, keyExtractor);
    }

    /**
     * Sorts the items in this chain in the descending order based on the {@link Double}
     * keys returned by the specified {@code keyExtractor} applied to each item.
     * <P>
     * Note this triggers a full traversal/evaluation of the chain.
     * @param keyExtractor
     * @return sorted items
     * @sawicki.similar
     * <table summary="Similar to:">
     * <tr><td><i>Java:</i></td><td>{@link java.util.stream.Stream#sorted(Comparator)}, but specific to {@link Double} outputs</td></tr>
     * <tr><td><i>C#:</i></td><td>{@code Enumerable.ThenByDescending()}</td></tr>
     * </table>
     * @see #ascending(ToDoubleFunction)
     */
    default Chainable<T> descending(ToDoubleFunction<T> keyExtractor) {
        return Chainables.descending(this, keyExtractor);
    }

    /**
     * Sorts the items in this chain in the descending order based on the {@link String}
     * keys returned by the specified {@code keyExtractor} applied to each item.
     * <P>
     * Note this triggers a full traversal/evaluation of the chain.
     * @param keyExtractor
     * @return sorted items
     * @sawicki.similar
     * <table summary="Similar to:">
     * <tr><td><i>Java:</i></td><td>{@link java.util.stream.Stream#sorted(Comparator)}, but specific to {@link String} outputs</td></tr>
     * <tr><td><i>C#:</i></td><td>{@code Enumerable.ThenByDescending()}</td></tr>
     * </table>
     * @see #ascending(ToStringFunction)
     */
    default Chainable<T> descending(ToStringFunction<T> keyExtractor) {
        return Chainables.descending(this, keyExtractor);
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
     * Returns the first item in the chain.
     * @return the first item or {@code null} if none
     * @sawicki.similar
     * <table summary="Similar to:">
     * <tr><td><i>Java:</i></td><td>{@link java.util.stream.Stream#findFirst()}</td></tr>
     * <tr><td><i>C#:</i></td><td>{@code Enumerable.FirstOrDefault()}</td></tr>
     * </table>
     */
    default T first() {
        return Chainables.first(this);
    }

    /**
     * Returns the first {@code count} of items in this chain.
     * @param count
     * @return the specified {@code count} of items from the beginning
     * @sawicki.similar
     * <table summary="Similar to:">
     * <tr><td><i>Java:</i></td><td>{@link java.util.stream.Stream#limit(long)}</td></tr>
     * <tr><td><i>C#:</i></td><td>{@code Enumerable.Take()}</td></tr>
     * </table>
     */
    default Chainable<T> first(int count) {
        return Chainables.first(this, count);
    }

    /**
     * Finds the first item satisfying the specified {@code condition}.
     * @param condition
     * @return the first item satisfying the specified {@code condition}
     * @sawicki.similar
     * <table summary="Similar to:">
     * <tr><td><i>Java:</i></td><td>a combination of {@link java.util.stream.Stream#filter(Predicate)} and {@link java.util.stream.Stream#findFirst()}</td></tr>
     * <tr><td><i>C#:</i></td><td>{@code Enumerable.FirstOrDefault()}</td></tr>
     * </table>
     * @see #firstWhereEither(Predicate...)
     */
    default T firstWhere(Predicate<T> condition) {
        return Chainables.firstWhereEither(this, condition);
    }

    /**
     * Finds the first item satisying any of the specified {@code conditions}.
     * @param conditions
     * @return the first item satisfying any of the specified {@code conditions}
     * @see #firstWhere(Predicate)
     */
    @SuppressWarnings("unchecked")
    default T firstWhereEither(Predicate<T>... conditions) {
        return Chainables.firstWhereEither(this, conditions);
    }

    /**
     * Interleaves the items of the specified {@code iterables}.
     * <p><b>Example:</b>
     * <table summary="Example:">
     * <tr><td>{@code items1}:</td><td>1, 3, 5</td></tr>
     * <tr><td>{@code items2}:</td><td>2, 4, 6</td></tr>
     * <tr><td><i>result:</i></td><td>1, 2, 3, 4, 5, 6</td></tr>
     * </table>
     * @param iterables to merge by interleaving
     * @return items from the interleaved merger of the specified {@code iterables}
     */
    @SuppressWarnings("unchecked")
    default Chainable<T> interleave(Iterable<T>...iterables) {
        return Chainables.interleave(this, iterables);
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
     * Enables the item existence check to be performed iteratively, emitting {@code null} values as long as the item is not <i>yet</i> found,
     * and ultimately emitting either {@code true} if the item is found, or otherwise {@code false} if the end has been reached.
     * @param item item to search for
     * @return a {@link Chainable} consisting of {@code null} values as long as the search is not completed, and ultimately either {@code true} or {@code false}
     * @see #contains(Object)
     */
    @Experimental
    default Chainable<Boolean> iterativeContains(T item) {
        return Chainables.iterativeContains(this, item);
    }

    /**
     * Joins all the members of the chain into a string with no delimiters, calling each member's {@code toString()} method.
     * @return the merged string
     * @see #join(String)
     */
    default String join() {
        return Chainables.join("", this);
    }

    /**
     * Joins all the members of the chain into a string with the specified {@code delimiter}, calling each member's {@code toString()} method.
     * @param delimiter the delimiter to insert between the members
     * @return the resulting string
     * @see #join()
     */
    default String join(String delimiter) {
        return Chainables.join(delimiter, this);
    }

    /**
     * Returns the last item in this chain.
     * <p>
     * This triggers a full traversal/evaluation of all the items.
     * @return the last item
     * @sawicki.similar
     * <table summary="Similar to:">
     * <tr><td><i>C#:</i></td><td>{@code Enumerable.LastOrDefault()}</td></tr>
     * </table>
     */
    default T last() {
        return Chainables.last(this);
    }

    /**
     * Returns the last {@code count} items from the end of this chain.
     * <p>
     * This triggers a full tarversal/evaluation of all the items.
     * @param count number of items to return from the end
     * @return up to the specified {@code count} of items from the end (or fewer if the chain is shorter than that)
     * @sawicki.similar
     * <table summary="Similar to:">
     * <tr><td><i>C#:</i></td><td>{@code Enumerable.TakeLast()}</td></tr>
     * </table>
     */
    default Chainable<T> last(int count) {
        return Chainables.last(this, count);
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
     * Determines whether none of the items in this chain satisfy the specified {@code condition}.
     * @param condition
     * @return {@code true} if there are no items that meet the specified {@code condition}
     * @sawicki.similar
     * <table summary="Similar to:">
     * <tr><td><i>Java:</i></td><td>{@link java.util.stream.Stream#noneMatch(Predicate)}</td></tr>
     * <tr><td><i>C#:</i></td><td>{@code Enumerable.Where()}, but with a negated predicate</td></tr>
     * </table>
     * @see #noneWhereEither(Predicate...)
     */
    default boolean noneWhere(Predicate<T> condition) {
        return Chainables.noneWhere(this, condition);
    }

    /**
     * Determines whether none of the items in this chain satisfy any of the specified {@code conditions}.
     * @param conditions
     * @return {@code true} if there are no items that meet any of the specified {@code conditions}
     * @see #noneWhere(Predicate)
     */
    @SuppressWarnings("unchecked")
    default boolean noneWhereEither(Predicate<T>... conditions) {
        return Chainables.noneWhereEither(this, conditions);
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
     * Returns a chain of remaining items from this chain starting with the specified {@code item}.
     * @param item
     * @return the remaining items in this chain starting with the specified {@code item}, if any
     * @see #notBefore(Predicate)
     * @see #notAsLongAsValue(Object)
     * @see #notAsLongAs(Predicate)
     * @see #notAfter(Predicate)
     * @see #asLongAs(Predicate)
     */
    default Chainable<T> notBeforeEquals(T item) {
        return Chainables.notBeforeEquals(this, item);
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
     * Computes the sum of values generated by the specified {@code valueExtractor} applied to each item in this chain.
     * <p>
     * This trighers a full traversal/evaluation of the items.
     * @param valueExtractor
     * @return sum of all the values returned by the specified {@code valueExtractor} applied to each item
     * @sawicki.similar
     * <table summary="Similar to:">
     * <tr><td><i>Java:</i></td><td>{@link java.util.stream.Stream#reduce(java.util.function.BinaryOperator)} or {@link java.util.stream.Stream#collect(java.util.stream.Collector)}, but specifically for summation</td></tr>
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
     * Puts the items from this chain into a map indexed by the specified {@code keyExtractor} applied to each item.
     * @param keyExtractor
     * @return a map of the items indexed by the key produced by the specified {@code keyExtractor}
     * @sawicki.similar
     * <table summary="Similar to:">
     * <tr><td><i>C#:</i></td><td>{@code Enumerable.ToDictionary()}</td></tr>
     * </table>
     */
    default <K> Map<K, T> toMap(Function<T, K> keyExtractor) {
        return Chainables.toMap(this, keyExtractor);
    }

    /**
     * Create a {@link ChainableQueue} with the current items as the initial contents of the queue, but not yet traversed/evaluated.
     * @return a mutable {@link ChainableQueue} with the current items as the initial contents of the queue
     */
    @Experimental
    default ChainableQueue<T> toQueue() {
        return Chainables.toQueue(this);
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