/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.analytics.pca;

import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.search.aggregations.matrix.stats.BaseInternalMatrixStats;
import org.elasticsearch.search.aggregations.matrix.stats.InternalMatrixStats;
import org.elasticsearch.search.aggregations.matrix.stats.MatrixStats;
import org.elasticsearch.search.aggregations.matrix.stats.MatrixStatsResults;
import org.elasticsearch.search.aggregations.matrix.stats.RunningStats;
import org.elasticsearch.search.aggregations.pipeline.PipelineAggregator;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class InternalPCAStats extends BaseInternalMatrixStats implements MatrixStats {
    private final boolean useCovariance;

    /** per shard ctor */
    InternalPCAStats(String name, long count, RunningStats multiFieldStatsResults, MatrixStatsResults results,
                     List<PipelineAggregator> pipelineAggregators, Map<String, Object> metaData, boolean useCovariance) {
        super(name, count, multiFieldStatsResults, results, pipelineAggregators, metaData);
        this.useCovariance = useCovariance;
    }

    /**
     * Read from a stream.
     */
    public InternalPCAStats(StreamInput in) throws IOException {
        super(in);
        this.useCovariance = in.readOptionalBoolean();
    }

    @Override
    protected void doWriteTo(StreamOutput out) throws IOException {
        super.doWriteTo(out);
        out.writeOptionalBoolean(useCovariance);
    }

    @Override
    protected PCAStatsResults readOptionalWriteableMatrixStatsResults(StreamInput in) throws IOException {
        return in.readOptionalWriteable(PCAStatsResults::new);
    }

    @Override
    public String getWriteableName() {
        return PCAAggregationBuilder.NAME;
    }

    @Override
    public InternalPCAStats newInternalMatrixStats(String name, long count, RunningStats multiFieldStatsResults,
            MatrixStatsResults results, List<PipelineAggregator> pipelineAggregators, Map<String, Object> metaData) {
        return new InternalPCAStats(name, count, multiFieldStatsResults, results, pipelineAggregators, metaData, useCovariance);
    }

    @Override
    public PCAStatsResults newMatrixStatsResults(RunningStats runningStats) {
        return new PCAStatsResults(runningStats, this.useCovariance);
    }

    @Override
    public PCAStatsResults newMatrixStatsResults() {
        return new PCAStatsResults();
    }

    public PCAStatsResults getResults() {
        return (PCAStatsResults)results;
    }

    public static class Fields extends BaseInternalMatrixStats.Fields {
        public static final String PRINCIPAL_COMPONENTS = "pc";
        public static final String EIGENVALUE = "eigenvalue";
        public static final String EIGENVECTOR = "eigenvector";
    }

    @Override
    public XContentBuilder doXContentBody(XContentBuilder builder, Params params) throws IOException {
        builder.field(CommonFields.DOC_COUNT.getPreferredName(), getDocCount());
        PCAStatsResults results = getResults();
        if (results != null && results.getFieldCounts().keySet().isEmpty() == false) {
            // put fields
            builder.startArray(InternalMatrixStats.Fields.FIELDS);
            for (String fieldName : results.getFieldCounts().keySet()) {
                builder.value(fieldName);
            }
            builder.endArray();

            // put principal components
            builder.startArray(Fields.PRINCIPAL_COMPONENTS);
            for (String fieldName : results.getFieldCounts().keySet()) {
                builder.startObject();
                builder.field(Fields.EIGENVALUE, results.getEigenValue(fieldName));
                double[] eigenVec = results.getEigenVector(fieldName);
                builder.array(Fields.EIGENVECTOR, eigenVec);
                builder.endObject();
            }
            builder.endArray();
        }
        return builder;
    }
}