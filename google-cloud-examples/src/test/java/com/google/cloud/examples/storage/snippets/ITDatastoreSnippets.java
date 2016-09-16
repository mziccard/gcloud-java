/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

package com.google.cloud.examples.storage.snippets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.google.cloud.datastore.Batch;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyFactory;
import com.google.cloud.examples.datastore.snippets.DatastoreSnippets;
import com.google.common.collect.Lists;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;

public class ITDatastoreSnippets {

  private static Datastore datastore;
  private static DatastoreSnippets datastoreSnippets;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public Timeout globalTimeout = Timeout.seconds(300);

  @BeforeClass
  public static void beforeClass() {
    datastore = DatastoreOptions.defaultInstance().service();
    datastoreSnippets = new DatastoreSnippets(datastore);
  }

  @AfterClass
  public static void afterClass() throws ExecutionException, InterruptedException {
    // TODO: do the right work for Datastore, similar to the Storage code below
//    if (storage != null) {
//      boolean wasDeleted = RemoteStorageHelper.forceDelete(storage, BUCKET, 5, TimeUnit.SECONDS);
//      if (!wasDeleted && log.isLoggable(Level.WARNING)) {
//        log.log(Level.WARNING, "Deletion of bucket {0} timed out, bucket is not empty", BUCKET);
//      }
//    }
  }

  @Test
  public void testRunInTransaction() {
    final String testString = "Test String";
    String result = datastoreSnippets.runInTransaction(testString);
    assertEquals(testString, result);
  }

  @Test
  public void testNewBatch() {
    final String testKey1 = "new_batch_key1";
    final Key key1 = datastore.newKeyFactory().kind("MyClass").newKey(testKey1);
    final String testKey2 = "new_batch_key2";
    final Key key2 = datastore.newKeyFactory().kind("MyClass").newKey(testKey2);
    datastore.delete(key1, key2);
    Batch batch = datastoreSnippets.newBatch(testKey1, testKey2);
    assertNotNull(batch);
    datastore.delete(key1, key2);
  }

  @Test
  public void testAllocateIdSingle() {
    Key key = datastoreSnippets.allocateIdSingle();
    assertNotNull(key);
  }

  @Test
  public void testAllocateIdMultiple() {
    List<Key> keys = datastoreSnippets.allocateIdMultiple();
    assertEquals(2, keys.size());
  }

  // GARRETT STARTS HERE

  @Test
  public void testEntityPutGet() {
    String key = "my_single_key";
    datastoreSnippets.putSingleEntity(key);
    Entity entity = datastoreSnippets.getEntityWithKey(key);
    assertEquals("value", entity.getString("propertyName"));

    datastore.delete(datastore.newKeyFactory().kind("MyClass").newKey(key));
  }

  private Map<String, Entity> createEntityMap(List<Entity> entities) {
    Map<String, Entity> entityMap = new HashMap<>();
    for (Entity entity : entities) {
      entityMap.put(entity.key().name(), entity);
    }
    return entityMap;
  }

  @Test
  public void testBatchEntityCrud() {
    String key1 = "batch_key1";
    String key2 = "batch_key2";
    datastoreSnippets.batchPutEntities(key1, key2);

    assertNotNull(datastoreSnippets.getEntityWithKey(key1));
    assertNotNull(datastoreSnippets.getEntityWithKey(key2));

    List<Entity> entities = Lists
        .newArrayList(datastoreSnippets.getEntitiesWithKeys(key1, key2));
    assertEquals(2, entities.size());
    Map<String, Entity> entityMap = createEntityMap(entities);
    assertEquals("value1", entityMap.get(key1).getString("propertyName"));
    assertEquals("value2", entityMap.get(key2).getString("propertyName"));

    datastoreSnippets.batchUpdateEntities(key1, key2);

    List<Entity> fetchedEntities = datastoreSnippets.fetchEntitiesWithKeys(key1, key2);
    assertEquals(2, fetchedEntities.size());
    Map<String, Entity> fetchedEntityMap = createEntityMap(fetchedEntities);
    assertEquals("updatedValue1", fetchedEntityMap.get(key1).getString("propertyName"));
    assertEquals("updatedValue2", fetchedEntityMap.get(key2).getString("propertyName"));

    datastoreSnippets.batchDeleteEntities(key1, key2);

    assertNull(datastoreSnippets.getEntityWithKey(key1));
    assertNull(datastoreSnippets.getEntityWithKey(key2));

    List<Entity> fetchedEntities2 = datastoreSnippets.fetchEntitiesWithKeys(key1, key2);
    assertEquals(2, fetchedEntities2.size());
    assertNull(fetchedEntities2.get(0));
    assertNull(fetchedEntities2.get(1));
  }

  // GARRETT ENDS HERE

  // MIKE STARTS HERE

  @Test
  public void testCreateKeyFactory() {
    KeyFactory keyFactory = datastoreSnippets.createKeyFactory();
    assertNotNull(keyFactory);
  }

  private void addEntity(String keyName, String keyClass, String value) {
    Key key = datastore.newKeyFactory().kind(keyClass).newKey(keyName);
    Entity.Builder entityBuilder = Entity.builder(key);
    entityBuilder.set("propertyName", value);
    Entity entity = entityBuilder.build();
    datastore.put(entity);
  }
  
  private void deleteEntity(String keyName, String keyClass) {
    Key key = datastore.newKeyFactory().kind(keyClass).newKey(keyName);
    datastore.delete(key);
  }
  
  @Test
  public void testRunQuery() {
    String keyNameToFind = "my_key_name_to_find";
    String keyNameToMiss = "my_key_name_to_miss";
    
    String kindToFind = "ClassToFind";
    String kindToMiss = "OtherClass";
    
    addEntity(keyNameToFind, kindToFind, "");
    addEntity(keyNameToMiss, kindToMiss, "");
    
    List<Entity> queryResults = datastoreSnippets.runQuery(kindToFind);
    assertNotNull(queryResults);
    assertEquals(1, queryResults.size());
    
    deleteEntity(keyNameToFind, kindToFind);
    deleteEntity(keyNameToMiss, kindToMiss);
  }

  // MIKE ENDS HERE
}