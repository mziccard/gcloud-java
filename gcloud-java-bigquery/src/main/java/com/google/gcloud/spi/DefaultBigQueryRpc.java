/*
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.gcloud.spi;

import static com.google.gcloud.spi.BigQueryRpc.Option.DELETE_CONTENTS;
import static com.google.gcloud.spi.BigQueryRpc.Option.FIELDS;
import static com.google.gcloud.spi.BigQueryRpc.Option.START_INDEX;
import static com.google.gcloud.spi.BigQueryRpc.Option.TIMEOUT;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;

import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.bigquery.Bigquery;
import com.google.api.services.bigquery.model.Dataset;
import com.google.api.services.bigquery.model.DatasetList;
import com.google.api.services.bigquery.model.DatasetReference;
import com.google.api.services.bigquery.model.GetQueryResultsResponse;
import com.google.api.services.bigquery.model.Job;
import com.google.api.services.bigquery.model.JobList;
import com.google.api.services.bigquery.model.JobReference;
import com.google.api.services.bigquery.model.JobStatus;
import com.google.api.services.bigquery.model.QueryRequest;
import com.google.api.services.bigquery.model.QueryResponse;
import com.google.api.services.bigquery.model.Table;
import com.google.api.services.bigquery.model.TableDataInsertAllRequest;
import com.google.api.services.bigquery.model.TableDataInsertAllResponse;
import com.google.api.services.bigquery.model.TableDataList;
import com.google.api.services.bigquery.model.TableList;
import com.google.api.services.bigquery.model.TableReference;
import com.google.api.services.bigquery.model.TableRow;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

import static com.google.gcloud.spi.BigQueryRpc.Option.MAX_RESULTS;
import static com.google.gcloud.spi.BigQueryRpc.Option.PAGE_TOKEN;

import com.google.gcloud.bigquery.BigQueryException;
import com.google.gcloud.bigquery.BigQueryOptions;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DefaultBigQueryRpc implements BigQueryRpc {

  public static final String DEFAULT_PROJECTION = "full";
  // see: https://cloud.google.com/bigquery/troubleshooting-errors
  private static final Set<Integer> RETRYABLE_CODES = ImmutableSet.of(500, 502, 503, 504);
  private final BigQueryOptions options;
  private final Bigquery bigquery;

  public DefaultBigQueryRpc(BigQueryOptions options) {
    HttpTransport transport = options.httpTransportFactory().create();
    HttpRequestInitializer initializer = options.httpRequestInitializer();
    this.options = options;
    bigquery = new Bigquery.Builder(transport, new JacksonFactory(), initializer)
        .setRootUrl(options.host())
        .setApplicationName(options.applicationName())
        .build();
  }

  private static BigQueryException translate(IOException exception) {
    BigQueryException translated;
    if (exception instanceof GoogleJsonResponseException
        && ((GoogleJsonResponseException) exception).getDetails() != null) {
      translated = translate(((GoogleJsonResponseException) exception).getDetails());
    } else {
      translated =
          new BigQueryException(BigQueryException.UNKNOWN_CODE, exception.getMessage(), false);
    }
    translated.initCause(exception);
    return translated;
  }

  private static BigQueryException translate(GoogleJsonError exception) {
    boolean retryable = RETRYABLE_CODES.contains(exception.getCode());
    return new BigQueryException(exception.getCode(), exception.getMessage(), retryable);
  }

  @Override
  public Dataset getDataset(String datasetId, Map<Option, ?> options) throws BigQueryException {
    try {
      return bigquery.datasets()
          .get(this.options.projectId(), datasetId)
          .setFields(FIELDS.getString(options))
          .execute();
    } catch(IOException ex) {
      BigQueryException serviceException = translate(ex);
      if (serviceException.code() == HTTP_NOT_FOUND) {
        return null;
      }
      throw serviceException;
    }
  }

  @Override
  public Tuple<String, Iterable<Dataset>> listDatasets(Map<Option, ?> options)
      throws BigQueryException {
    try {
      DatasetList datasetsList = bigquery.datasets()
          .list(this.options.projectId())
          .setAll(Option.ALL_DATASETS.getBoolean(options))
          .setMaxResults(MAX_RESULTS.getLong(options))
          .setPageToken(PAGE_TOKEN.getString(options))
          .execute();
      Iterable<DatasetList.Datasets> datasets = datasetsList.getDatasets();
      return Tuple.of(datasetsList.getNextPageToken(),
          Iterables.transform(datasets != null ? datasets : ImmutableList.<DatasetList.Datasets>of(),
              new Function<DatasetList.Datasets, Dataset>() {
                @Override
                public Dataset apply(DatasetList.Datasets f) {
                  return new Dataset()
                      .setDatasetReference(f.getDatasetReference())
                      .setFriendlyName(f.getFriendlyName())
                      .setId(f.getId())
                      .setKind(f.getKind());
                }
              }));
    } catch (IOException ex) {
      throw translate(ex);
    }
  }

  @Override
  public Dataset create(Dataset dataset, Map<Option, ?> options) throws BigQueryException {
    try {
      return bigquery.datasets().insert(this.options.projectId(), dataset)
          .setFields(FIELDS.getString(options))
          .execute();
    } catch (IOException ex) {
      throw translate(ex);
    }
  }

  @Override
  public boolean deleteDataset(String datasetId, Map<Option, ?> options) throws BigQueryException {
    try {
      bigquery.datasets().delete(this.options.projectId(), datasetId)
          .setDeleteContents(DELETE_CONTENTS.getBoolean(options))
          .execute();
      return true;
    } catch (IOException ex) {
      BigQueryException serviceException = translate(ex);
      if (serviceException.code() == HTTP_NOT_FOUND) {
        return false;
      }
      throw serviceException;
    }
  }

  @Override
  public Dataset patch(Dataset dataset, Map<Option, ?> options) throws BigQueryException {
    try {
      DatasetReference reference = dataset.getDatasetReference();
      return bigquery.datasets()
          .patch(this.options.projectId(), reference.getDatasetId(), dataset)
          .setFields(FIELDS.getString(options))
          .execute();
    } catch (IOException ex) {
      throw translate(ex);
    }
  }

  @Override
  public Table getTable(String datasetId, String tableId, Map<Option, ?> options)
      throws BigQueryException {
    try {
      return bigquery.tables()
          .get(this.options.projectId(), datasetId, tableId)
          .setFields(FIELDS.getString(options))
          .execute();
    } catch(IOException ex) {
      BigQueryException serviceException = translate(ex);
      if (serviceException.code() == HTTP_NOT_FOUND) {
        return null;
      }
      throw serviceException;
    }
  }

  @Override
  public Tuple<String, Iterable<Table>> listTables(String datasetId, Map<Option, ?> options)
      throws BigQueryException {
    try {
      TableList tableList = bigquery.tables()
          .list(this.options.projectId(), datasetId)
          .setMaxResults(MAX_RESULTS.getLong(options))
          .setPageToken(PAGE_TOKEN.getString(options))
          .execute();
      Iterable<TableList.Tables> tables = tableList.getTables();
      return Tuple.of(tableList.getNextPageToken(),
          Iterables.transform(tables != null ? tables : ImmutableList.<TableList.Tables>of(),
              new Function<TableList.Tables, Table>() {
                @Override
                public Table apply(TableList.Tables f) {
                  return new Table()
                      .setFriendlyName(f.getFriendlyName())
                      .setId(f.getId())
                      .setKind(f.getKind())
                      .setTableReference(f.getTableReference())
                      .setType(f.getType());
                }
              }));
    } catch (IOException ex) {
      throw translate(ex);
    }
  }

  @Override
  public Table create(Table table, Map<Option, ?> options)
      throws BigQueryException {
    try {
      return bigquery.tables()
          .insert(this.options.projectId(), table.getTableReference().getDatasetId(), table)
          .setFields(FIELDS.getString(options))
          .execute();
    } catch (IOException ex) {
      throw translate(ex);
    }
  }

  @Override
  public boolean deleteTable(String datasetId, String tableId, Map<Option, ?> options)
      throws BigQueryException {
    try {
      bigquery.tables().delete(this.options.projectId(), datasetId, tableId).execute();
      return true;
    } catch (IOException ex) {
      BigQueryException serviceException = translate(ex);
      if (serviceException.code() == HTTP_NOT_FOUND) {
        return false;
      }
      throw serviceException;
    }
  }

  @Override
  public Table patch(Table table, Map<Option, ?> options) throws BigQueryException {
    try {
      TableReference reference = table.getTableReference();
      return bigquery.tables()
          .patch(this.options.projectId(), reference.getDatasetId(), reference.getTableId(), table)
          .setFields(FIELDS.getString(options))
          .execute();
    } catch (IOException ex) {
      throw translate(ex);
    }
  }

  @Override
  public TableDataInsertAllResponse insertAll(TableReference table,
      TableDataInsertAllRequest request, Map<Option, ?> options) throws BigQueryException {
    try {
      return bigquery.tabledata()
          .insertAll(this.options.projectId(), table.getDatasetId(), table.getTableId(), request)
          .execute();
    } catch (IOException ex) {
      throw translate(ex);
    }
  }

  @Override
  public Tuple<String, Iterable<TableRow>> listTableData(String datasetId, String tableId,
      Map<Option, ?> options) throws BigQueryException {
    try {
      TableDataList tableDataList = bigquery.tabledata()
          .list(this.options.projectId(), datasetId, tableId)
          .setMaxResults(MAX_RESULTS.getLong(options))
          .setPageToken(PAGE_TOKEN.getString(options))
          .setStartIndex(START_INDEX.getLong(options) != null ?
              BigInteger.valueOf(START_INDEX.getLong(options)) : null)
          .execute();
      return Tuple.<String, Iterable<TableRow>>of(tableDataList.getPageToken(),
          tableDataList.getRows());
    } catch (IOException ex) {
      throw translate(ex);
    }
  }

  @Override
  public Job getJob(String jobId, Map<Option, ?> options) throws BigQueryException {
    try {
      return bigquery.jobs()
          .get(this.options.projectId(), jobId)
          .setFields(FIELDS.getString(options))
          .execute();
    } catch(IOException ex) {
      BigQueryException serviceException = translate(ex);
      if (serviceException.code() == HTTP_NOT_FOUND) {
        return null;
      }
      throw serviceException;
    }
  }

  @Override
  public Tuple<String, Iterable<Job>> listJobs(Map<Option, ?> options) throws BigQueryException {
    try {
      JobList jobsList = bigquery.jobs()
          .list(this.options.projectId())
          .setAllUsers(Option.ALL_USERS.getBoolean(options))
          .setFields(Option.FIELDS.getString(options))
          .setStateFilter(Option.STATE_FILTER.<List<String>>get(options))
          .setMaxResults(MAX_RESULTS.getLong(options))
          .setPageToken(PAGE_TOKEN.getString(options))
          .setProjection(DEFAULT_PROJECTION)
          .execute();
      Iterable<JobList.Jobs> jobs = jobsList.getJobs();
      return Tuple.of(jobsList.getNextPageToken(),
          Iterables.transform(jobs != null ? jobs : ImmutableList.<JobList.Jobs>of(),
              new Function<JobList.Jobs, Job>() {
                @Override
                public Job apply(JobList.Jobs f) {
                  JobStatus statusPb = f.getStatus() != null ? f.getStatus() : new JobStatus();
                  if (statusPb.getState() == null) {
                    statusPb.setState(f.getState());
                  }
                  if (statusPb.getErrorResult() == null) {
                    statusPb.setErrorResult(f.getErrorResult());
                  }
                  return new Job()
                      .setConfiguration(f.getConfiguration())
                      .setId(f.getId())
                      .setJobReference(f.getJobReference())
                      .setKind(f.getKind())
                      .setStatistics(f.getStatistics())
                      .setStatus(f.getStatus())
                      .setUserEmail(f.getUserEmail());
                }
              }));
    } catch (IOException ex) {
      throw translate(ex);
    }
  }

  @Override
  public Job create(Job job, Map<Option, ?> options) throws BigQueryException {
    try {
      return bigquery.jobs()
          .insert(this.options.projectId(), job)
          .setFields(FIELDS.getString(options))
          .execute();
    } catch (IOException ex) {
      throw translate(ex);
    }
  }

  @Override
  public boolean cancel(String jobId, Map<Option, ?> options) throws BigQueryException {
    try {
      bigquery.jobs().cancel(this.options.projectId(), jobId).execute();
      return true;
    } catch (IOException ex) {
      BigQueryException serviceException = translate(ex);
      if (serviceException.code() == HTTP_NOT_FOUND) {
        return false;
      }
      throw serviceException;
    }
  }

  @Override
  public GetQueryResultsResponse getQueryResults(JobReference job, Map<Option, ?> options)
      throws BigQueryException {
    try {
      return bigquery.jobs().getQueryResults(this.options.projectId(), job.getJobId())
          .setMaxResults(MAX_RESULTS.getLong(options))
          .setPageToken(PAGE_TOKEN.getString(options))
          .setStartIndex(START_INDEX.getLong(options) != null ?
              BigInteger.valueOf(START_INDEX.getLong(options)) : null)
          .setTimeoutMs(TIMEOUT.getLong(options))
          .execute();
    } catch(IOException ex) {
      BigQueryException serviceException = translate(ex);
      if (serviceException.code() == HTTP_NOT_FOUND) {
        return null;
      }
      throw serviceException;
    }
  }

  @Override
  public QueryResponse query(QueryRequest request, Map<Option, ?> options)
      throws BigQueryException {
    try {
      return bigquery.jobs().query(this.options.projectId(), request).execute();
    } catch (IOException ex) {
      throw translate(ex);
    }
  }
}