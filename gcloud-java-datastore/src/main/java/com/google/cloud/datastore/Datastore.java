/*
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.datastore;

import com.google.cloud.Service;

import java.util.Iterator;
import java.util.List;

/**
 * An interface for Google Cloud Datastore.
 */
public interface Datastore extends Service<DatastoreOptions>, DatastoreReaderWriter {

  /**
   * Returns a new Datastore transaction.
   *
   * @throws DatastoreException upon failure
   */
  Transaction newTransaction();

  /**
   * A callback for running with a transactional
   * {@link com.google.cloud.datastore.DatastoreReaderWriter}.
   * The associated transaction will be committed after a successful return from the {@code run}
   * method. Any propagated exception will cause the transaction to be rolled-back.
   *
   * @param <T> the type of the return value
   */
  interface TransactionCallable<T> {
    T run(DatastoreReaderWriter readerWriter) throws Exception;
  }

  /**
   * Invokes the callback's {@link Datastore.TransactionCallable#run} method with a
   * {@link DatastoreReaderWriter} that is associated with a new transaction.
   * The transaction will be committed upon successful invocation.
   * Any thrown exception will cause the transaction to rollback and will be propagated
   * as a {@link DatastoreException} with the original exception as its root cause.
   *
   * @param callable the callback to call with a newly created transactional readerWriter
   * @throws DatastoreException upon failure
   */
  <T> T runInTransaction(TransactionCallable<T> callable);

  /**
   * Returns a new Batch for processing multiple write operations in one request.
   */
  Batch newBatch();

  /**
   * Allocate a unique id for the given key.
   * The returned key will have the same information (projectId, kind, namespace and ancestors)
   * as the given key and will have a newly assigned id.
   *
   * @throws DatastoreException upon failure
   */
  Key allocateId(IncompleteKey key);

  /**
   * Returns a list of keys using the allocated ids ordered by the input.
   *
   * @throws DatastoreException upon failure
   * @see #allocateId(IncompleteKey)
   */
  List<Key> allocateId(IncompleteKey... keys);

  /**
   * {@inheritDoc}
   * @throws DatastoreException upon failure
   */
  @Override
  void update(Entity... entities);

  /**
   * {@inheritDoc}
   * @throws DatastoreException upon failure
   */
  @Override
  Entity put(FullEntity<?> entity);

  /**
   * {@inheritDoc}
   * @throws DatastoreException upon failure
   */
  @Override
  List<Entity> put(FullEntity<?>... entities);

  /**
   * {@inheritDoc}
   * @throws DatastoreException upon failure
   */
  @Override
  void delete(Key... keys);

  /**
   * Returns a new KeyFactory for this service
   */
  KeyFactory newKeyFactory();

  /**
   * Returns an {@link Entity} for the given {@link Key} or {@code null} if it doesn't exist.
   * {@link ReadOption}s can be specified if desired.
   *
   * @throws DatastoreException upon failure
   */
  Entity get(Key key, ReadOption... options);

  /**
   * Returns an {@link Entity} for each given {@link Key} that exists in the Datastore. The order of
   * the result is unspecified. Results are loaded lazily, so it is possible to get a
   * {@code DatastoreException} from the returned {@code Iterator}'s
   * {@link Iterator#hasNext hasNext} or {@link Iterator#next next} methods. {@link ReadOption}s can
   * be specified if desired.
   *
   * @throws DatastoreException upon failure
   * @see #get(Key)
   */
  Iterator<Entity> get(Iterable<Key> keys, ReadOption... options);

  /**
   * Returns a list with a value for each given key (ordered by input). {@code null} values are
   * returned for nonexistent keys. When possible prefer using {@link #get(Key...)} to avoid eagerly
   * loading the results. {@link ReadOption}s can be specified if desired.
   */
  List<Entity> fetch(Iterable<Key> keys, ReadOption... options);

  /**
   * Submits a {@link Query} and returns its result. {@link ReadOption}s can be specified if
   * desired.
   *
   * @throws DatastoreException upon failure
   */
  <T> QueryResults<T> run(Query<T> query, ReadOption... options);
}
