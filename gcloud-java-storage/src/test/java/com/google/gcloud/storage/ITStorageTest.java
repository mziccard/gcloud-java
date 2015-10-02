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

package com.google.gcloud.storage;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.common.collect.ImmutableList;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Calendar;

import org.junit.AfterClass;
import static org.junit.Assert.assertNull;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ITStorageTest {

  private static StorageOptions options;
  private static Storage storage;
  private static RemoteGcsHelper gcsHelper;
  private static String bucket;

  private static final String CONTENT_TYPE = "text/plain";
  private static final byte[] BLOB_BYTE_CONTENT = {0xD, 0xE, 0xA, 0xD};
  private static final String BLOB_STRING_CONTENT = "Hello Google Cloud Storage!";

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @BeforeClass
  public static void beforeClass() {
    gcsHelper = RemoteGcsHelper.create();
    if (gcsHelper != null) {
      options = gcsHelper.options();
      storage = StorageFactory.instance().get(options);
      bucket = gcsHelper.bucket();
      storage.create(BucketInfo.of(bucket));
    }
  }

  @AfterClass
  public static void afterClass() {
    if (storage != null) {
      for (BlobInfo info : storage.list(bucket)) {
        //storage.delete(bucket, info.name());
      }
      storage.delete(bucket);
    }
  }

  @Before
  public void beforeMethod() {
    org.junit.Assume.assumeNotNull(storage);
  }

  @Test
  public void testListBuckets() {
    ListResult<BucketInfo> bucketList = storage.list(Storage.BucketListOption.prefix(bucket));
    for (BucketInfo bucketInfo : bucketList) {
      assertTrue(bucketInfo.name().startsWith(bucket));
    }
  }

  @Test
  public void testCreateBlob() {
    String blobName = "test-create-blob";
    BlobInfo blob = BlobInfo.of(bucket, blobName);
    BlobInfo remoteBlob = storage.create(blob, BLOB_BYTE_CONTENT);
    assertNotNull(remoteBlob);
    assertEquals(blob.bucket(), remoteBlob.bucket());
    assertEquals(blob.name(), remoteBlob.name());
    byte[] readBytes = storage.readAllBytes(bucket, blobName);
    assertArrayEquals(BLOB_BYTE_CONTENT, readBytes);
    assertTrue(storage.delete(bucket, blobName));
  }

  @Test
  public void testCreateEmptyBlob() {
    String blobName = "test-create-empty-blob";
    BlobInfo blob = BlobInfo.of(bucket, blobName);
    BlobInfo remoteBlob = storage.create(blob);
    assertNotNull(remoteBlob);
    assertEquals(blob.bucket(), remoteBlob.bucket());
    assertEquals(blob.name(), remoteBlob.name());
    byte[] readBytes = storage.readAllBytes(bucket, blobName);
    assertArrayEquals(new byte[0], readBytes);
    assertTrue(storage.delete(bucket, blobName));
  }

  @Test
  public void testCreateBlobStream() throws UnsupportedEncodingException {
    String blobName = "test-create-blob-stream";
    BlobInfo blob = BlobInfo.builder(bucket, blobName).contentType(CONTENT_TYPE).build();
    ByteArrayInputStream stream = new ByteArrayInputStream(BLOB_STRING_CONTENT.getBytes(UTF_8));
    BlobInfo remoteBlob = storage.create(blob, stream);
    assertNotNull(remoteBlob);
    assertEquals(blob.bucket(), remoteBlob.bucket());
    assertEquals(blob.name(), remoteBlob.name());
    assertEquals(blob.contentType(), remoteBlob.contentType());
    byte[] readBytes = storage.readAllBytes(bucket, blobName);
    assertEquals(BLOB_STRING_CONTENT, new String(readBytes, UTF_8));
    assertTrue(storage.delete(bucket, blobName));
  }

  @Test
  public void testCreateBlobFail() {
    String blobName = "test-create-blob-fail";
    BlobInfo blob = BlobInfo.of(bucket, blobName);
    storage.create(blob);
    try {
      storage.create(blob.toBuilder().generation(42L).build(), BLOB_BYTE_CONTENT,
          Storage.BlobTargetOption.generationMatch());
      fail("StorageException was expected");
    } catch (StorageException ex) {
      // expected
    }
    assertTrue(storage.delete(bucket, blobName));
  }

  @Test
  public void testUpdateBlob() {
    String blobName = "test-update-blob";
    BlobInfo blob = BlobInfo.of(bucket, blobName);
    BlobInfo remoteBlob = storage.create(blob);
    assertNotNull(remoteBlob);
    BlobInfo updatedBlob = storage.update(blob.toBuilder().contentType(CONTENT_TYPE).build());
    assertNotNull(updatedBlob);
    assertEquals(blob.bucket(), updatedBlob.bucket());
    assertEquals(blob.name(), updatedBlob.name());
    assertEquals(CONTENT_TYPE, updatedBlob.contentType());
    assertTrue(storage.delete(bucket, blobName));
  }

  @Test
  public void testUpdateBlobFail() {
    String blobName = "test-update-blob-fail";
    BlobInfo blob = BlobInfo.of(bucket, blobName);
    BlobInfo remoteBlob = storage.create(blob);
    assertNotNull(remoteBlob);
    try {
      storage.update(blob.toBuilder().contentType(CONTENT_TYPE).generation(42L).build(),
          Storage.BlobTargetOption.generationMatch());
      fail("StorageException was expected");
    } catch (StorageException ex) {
      // expected
    }
    assertTrue(storage.delete(bucket, blobName));
  }

  @Test
  public void testDeleteNonExistingBlob() {
    String blobName = "test-delete-non-existing-blob";
    BlobInfo blob = BlobInfo.of(bucket, blobName);
    assertTrue(!storage.delete(bucket, blob.name()));
  }

  @Test
  public void testDeleteBlobFail() {
    String blobName = "test-delete-blob-fail";
    BlobInfo blob = BlobInfo.of(bucket, blobName);
    assertNotNull(storage.create(blob));
    try {
      storage.delete(bucket, blob.name(), Storage.BlobSourceOption.generationMatch(42L));
      fail("StorageException was expected");
    } catch (StorageException ex) {
      // expected
    }
    assertTrue(storage.delete(bucket, blob.name()));
  }

  @Test
  public void testComposeBlob() {
    String sourceBlobName1 = "test-compose-blob-source-1";
    String sourceBlobName2 = "test-compose-blob-source-2";
    BlobInfo sourceBlob1 = BlobInfo.of(bucket, sourceBlobName1);
    BlobInfo sourceBlob2 = BlobInfo.of(bucket, sourceBlobName2);
    assertNotNull(storage.create(sourceBlob1, BLOB_BYTE_CONTENT));
    assertNotNull(storage.create(sourceBlob2, BLOB_BYTE_CONTENT));
    String targetBlobName = "test-compose-blob-target";
    BlobInfo targetBlob = BlobInfo.of(bucket, targetBlobName);
    Storage.ComposeRequest req =
        Storage.ComposeRequest.of(ImmutableList.of(sourceBlobName1, sourceBlobName2), targetBlob);
    BlobInfo remoteBlob = storage.compose(req);
    assertNotNull(remoteBlob);
    assertEquals(bucket, remoteBlob.bucket());
    assertEquals(targetBlobName, remoteBlob.name());
    byte[] readBytes = storage.readAllBytes(bucket, targetBlobName);
    byte[] composedBytes = Arrays.copyOf(BLOB_BYTE_CONTENT, BLOB_BYTE_CONTENT.length * 2);
    System.arraycopy(BLOB_BYTE_CONTENT, 0, composedBytes, BLOB_BYTE_CONTENT.length,
        BLOB_BYTE_CONTENT.length);
    assertArrayEquals(composedBytes, readBytes);
    assertTrue(storage.delete(bucket, sourceBlobName1));
    assertTrue(storage.delete(bucket, sourceBlobName2));
    assertTrue(storage.delete(bucket, targetBlobName));
  }

  @Test
  public void testComposeBlobFail() {
    String sourceBlobName1 = "test-compose-blob-fail-source-1";
    String sourceBlobName2 = "test-compose-blob-fail-source-2";
    BlobInfo sourceBlob1 = BlobInfo.of(bucket, sourceBlobName1);
    BlobInfo sourceBlob2 = BlobInfo.of(bucket, sourceBlobName2);
    assertNotNull(storage.create(sourceBlob1));
    assertNotNull(storage.create(sourceBlob2));
    String targetBlobName = "test-compose-blob-fail-target";
    BlobInfo targetBlob = BlobInfo.of(bucket, targetBlobName);
    Storage.ComposeRequest req = Storage.ComposeRequest.builder()
        .addSource(sourceBlobName1, 42L)
        .addSource(sourceBlobName2, 42L)
        .target(targetBlob)
        .build();
    try {
      storage.compose(req);
      fail("StorageException was expected");
    } catch (StorageException ex) {
      // expected
    }
    assertTrue(storage.delete(bucket, sourceBlobName1));
    assertTrue(storage.delete(bucket, sourceBlobName2));
  }

  @Test
  public void testCopyBlob() {
    String sourceBlobName = "test-copy-blob-source";
    BlobInfo blob = BlobInfo.of(bucket, sourceBlobName);
    assertNotNull(storage.create(blob, BLOB_BYTE_CONTENT));
    String targetBlobName = "test-copy-blob-target";
    Storage.CopyRequest req = Storage.CopyRequest.of(bucket, sourceBlobName, targetBlobName);
    BlobInfo remoteBlob = storage.copy(req);
    assertNotNull(remoteBlob);
    assertEquals(bucket, remoteBlob.bucket());
    assertEquals(targetBlobName, remoteBlob.name());
    byte[] readBytes = storage.readAllBytes(bucket, targetBlobName);
    assertArrayEquals(BLOB_BYTE_CONTENT, readBytes);
    assertTrue(storage.delete(bucket, sourceBlobName));
    assertTrue(storage.delete(bucket, targetBlobName));
  }

  @Test
  public void testCopyBlobUpdateMetadata() {
    String sourceBlobName = "test-copy-blob-and-metadata-source";
    BlobInfo sourceBlob = BlobInfo.of(bucket, sourceBlobName);
    assertNotNull(storage.create(sourceBlob, BLOB_BYTE_CONTENT));
    String targetBlobName = "test-copy-blob-and-metadata-target";
    BlobInfo targetBlob =
        BlobInfo.builder(bucket, targetBlobName).contentType(CONTENT_TYPE).build();
    Storage.CopyRequest req = Storage.CopyRequest.of(bucket, sourceBlobName, targetBlob);
    BlobInfo remoteBlob = storage.copy(req);
    assertNotNull(remoteBlob);
    assertEquals(bucket, remoteBlob.bucket());
    assertEquals(targetBlobName, remoteBlob.name());
    assertEquals(CONTENT_TYPE, remoteBlob.contentType());
    assertTrue(storage.delete(bucket, sourceBlobName));
    assertTrue(storage.delete(bucket, targetBlobName));
  }

  @Test
  public void testCopyBlobFail() {
    String sourceBlobName = "test-copy-blob-fail-source";
    BlobInfo blob = BlobInfo.of(bucket, sourceBlobName);
    assertNotNull(storage.create(blob));
    String targetBlobName = "test-copy-blob-fail-target";
    Storage.CopyRequest req = new Storage.CopyRequest.Builder()
        .source(bucket, sourceBlobName)
        .target(BlobInfo.builder(bucket, targetBlobName).build())
        .sourceOptions(Storage.BlobSourceOption.generationMatch(42L))
        .build();
    try {
      storage.copy(req);
      fail("StorageException was expected");
    } catch (StorageException ex) {
      // expected
    }
    assertTrue(storage.delete(bucket, sourceBlobName));
  }

  @Test
  public void testBatchRequest() {
    String sourceBlobName1 = "test-batch-request-blob-1";
    String sourceBlobName2 = "test-batch-request-blob-2";
    BlobInfo sourceBlob1 = BlobInfo.of(bucket, sourceBlobName1);
    BlobInfo sourceBlob2 = BlobInfo.of(bucket, sourceBlobName2);
    assertNotNull(storage.create(sourceBlob1));
    assertNotNull(storage.create(sourceBlob2));

    // Batch update request
    BlobInfo updatedBlob1 = sourceBlob1.toBuilder().contentType(CONTENT_TYPE).build();
    BlobInfo updatedBlob2 = sourceBlob2.toBuilder().contentType(CONTENT_TYPE).build();
    BatchRequest updateRequest = BatchRequest.builder()
        .update(updatedBlob1)
        .update(updatedBlob2)
        .build();
    BatchResponse updateResponse = storage.apply(updateRequest);
    assertEquals(2, updateResponse.updates().size());
    assertEquals(0, updateResponse.deletes().size());
    assertEquals(0, updateResponse.gets().size());
    BlobInfo remoteUpdatedBlob1 = updateResponse.updates().get(0).get();
    BlobInfo remoteUpdatedBlob2 = updateResponse.updates().get(1).get();
    assertEquals(bucket, remoteUpdatedBlob1.bucket());
    assertEquals(bucket, remoteUpdatedBlob2.bucket());
    assertEquals(updatedBlob1.name(), remoteUpdatedBlob1.name());
    assertEquals(updatedBlob2.name(), remoteUpdatedBlob2.name());
    assertEquals(updatedBlob1.contentType(), remoteUpdatedBlob1.contentType());
    assertEquals(updatedBlob2.contentType(), remoteUpdatedBlob2.contentType());

    // Batch get request
    BatchRequest getRequest = BatchRequest.builder()
        .get(bucket, sourceBlobName1)
        .get(bucket, sourceBlobName2)
        .build();
    BatchResponse getResponse = storage.apply(getRequest);
    assertEquals(2, getResponse.gets().size());
    assertEquals(0, getResponse.deletes().size());
    assertEquals(0, getResponse.updates().size());
    BlobInfo remoteBlob1 = getResponse.gets().get(0).get();
    BlobInfo remoteBlob2 = getResponse.gets().get(1).get();
    assertEquals(remoteUpdatedBlob1, remoteBlob1);
    assertEquals(remoteUpdatedBlob2, remoteBlob2);

    // Batch delete request
    BatchRequest deleteRequest = BatchRequest.builder()
        .delete(bucket, sourceBlobName1)
        .delete(bucket, sourceBlobName2)
        .build();
    BatchResponse deleteResponse = storage.apply(deleteRequest);
    assertEquals(2, deleteResponse.deletes().size());
    assertEquals(0, deleteResponse.gets().size());
    assertEquals(0, deleteResponse.updates().size());
    assertTrue(deleteResponse.deletes().get(0).get());
    assertTrue(deleteResponse.deletes().get(1).get());
  }

  @Test
  public void testBatchRequestFail() {
    String blobName = "test-batch-request-blob-fail";
    BlobInfo blob = BlobInfo.of(bucket, blobName);
    assertNotNull(storage.create(blob));
    BlobInfo updatedBlob = blob.toBuilder().generation(42L).build();
    BatchRequest batchRequest = BatchRequest.builder()
        .update(updatedBlob, Storage.BlobTargetOption.generationMatch())
        .delete(bucket, blobName, Storage.BlobSourceOption.generationMatch(42L))
        .get(bucket, blobName, Storage.BlobSourceOption.generationMatch(42L))
        .build();
    BatchResponse updateResponse = storage.apply(batchRequest);
    assertEquals(1, updateResponse.updates().size());
    assertEquals(1, updateResponse.deletes().size());
    assertEquals(1, updateResponse.gets().size());
    assertTrue(updateResponse.updates().get(0).failed());
    assertTrue(updateResponse.gets().get(0).failed());
    assertTrue(updateResponse.deletes().get(0).failed());
    assertTrue(storage.delete(bucket, blobName));
  }

  @Test
  public void testReadAndWriteChannels() throws UnsupportedEncodingException, IOException {
    String blobName = "test-read-and-write-channels-blob";
    BlobInfo blob = BlobInfo.of(bucket, blobName);
    byte[] stringBytes;
    try (BlobWriteChannel writer = storage.writer(blob)) {
      stringBytes = BLOB_STRING_CONTENT.getBytes(UTF_8);
      writer.write(ByteBuffer.wrap(BLOB_BYTE_CONTENT));
      writer.write(ByteBuffer.wrap(stringBytes));
    }
    ByteBuffer readBytes;
    ByteBuffer readStringBytes;
    try (BlobReadChannel reader = storage.reader(bucket, blobName)) {
      readBytes = ByteBuffer.allocate(BLOB_BYTE_CONTENT.length);
      readStringBytes = ByteBuffer.allocate(stringBytes.length);
      reader.read(readBytes);
      reader.read(readStringBytes);
    }
    assertArrayEquals(BLOB_BYTE_CONTENT, readBytes.array());
    assertEquals(BLOB_STRING_CONTENT, new String(readStringBytes.array(), UTF_8));
    assertTrue(storage.delete(bucket, blobName));
  }

  @Test
  public void testReadChannelFail() throws UnsupportedEncodingException, IOException {
    String blobName = "test-read-channel-blob-fail";
    BlobInfo blob = BlobInfo.of(bucket, blobName);
    assertNotNull(storage.create(blob));
    try (BlobReadChannel reader =
        storage.reader(bucket, blobName, Storage.BlobSourceOption.metagenerationMatch(42L))) {
      reader.read(ByteBuffer.allocate(42));
      fail("StorageException was expected");
    } catch (StorageException ex) {
      // expected
    }
    assertTrue(storage.delete(bucket, blobName));
  }

  @Test
  public void testWriteChannelFail() throws UnsupportedEncodingException, IOException {
    String blobName = "test-write-channel-blob-fail";
    BlobInfo blob = BlobInfo.builder(bucket, blobName).generation(42L).build();
    try {
      BlobWriteChannel writer = storage.writer(blob, Storage.BlobTargetOption.generationMatch());
      writer.write(ByteBuffer.allocate(42));
      writer.close();
      fail("StorageException was expected");
    } catch (StorageException ex) {
      // expected
    }
  }

  @Test
  public void testGetSignedUrl() throws IOException {
    String blobName = "test-get-signed-url-blob";
    BlobInfo blob = BlobInfo.of(bucket, blobName);
    assertNotNull(storage.create(BlobInfo.of(bucket, blobName), BLOB_BYTE_CONTENT));
    Calendar calendar = Calendar.getInstance();
    calendar.add(Calendar.HOUR, 1);
    long expiration = calendar.getTimeInMillis() / 1000;
    URL url = storage.signUrl(blob, expiration);
    URLConnection connection = url.openConnection();
    byte[] readBytes = new byte[BLOB_BYTE_CONTENT.length];
    try (InputStream responseStream = connection.getInputStream()) {
      assertEquals(BLOB_BYTE_CONTENT.length, responseStream.read(readBytes));
    }
    assertArrayEquals(BLOB_BYTE_CONTENT, readBytes);
    assertTrue(storage.delete(bucket, blobName));
  }

  @Test
  public void testPostSignedUrl() throws IOException {
    String blobName = "test-post-signed-url-blob";
    BlobInfo blob = BlobInfo.of(bucket, blobName);
    assertNotNull(storage.create(BlobInfo.of(bucket, blobName)));
    Calendar calendar = Calendar.getInstance();
    calendar.add(Calendar.HOUR, 1);
    long expiration = calendar.getTimeInMillis() / 1000;
    URL url = storage.signUrl(blob, expiration, Storage.SignUrlOption.httpMethod(HttpMethod.POST));
    URLConnection connection = url.openConnection();
    connection.setDoOutput(true);
    connection.connect();
    BlobInfo remoteBlob = storage.get(bucket, blobName);
    assertNotNull(remoteBlob);
    assertEquals(bucket, remoteBlob.bucket());
    assertEquals(blob.name(), remoteBlob.name());
    assertTrue(storage.delete(bucket, blobName));
  }
}
