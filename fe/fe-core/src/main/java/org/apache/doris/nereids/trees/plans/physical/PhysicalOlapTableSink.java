// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package org.apache.doris.nereids.trees.plans.physical;

import org.apache.doris.catalog.Column;
import org.apache.doris.catalog.Database;
import org.apache.doris.catalog.HashDistributionInfo;
import org.apache.doris.catalog.OlapTable;
import org.apache.doris.nereids.memo.GroupExpression;
import org.apache.doris.nereids.properties.DistributionSpecHash.ShuffleType;
import org.apache.doris.nereids.properties.LogicalProperties;
import org.apache.doris.nereids.properties.PhysicalProperties;
import org.apache.doris.nereids.trees.expressions.Expression;
import org.apache.doris.nereids.trees.expressions.Slot;
import org.apache.doris.nereids.trees.plans.Plan;
import org.apache.doris.nereids.trees.plans.PlanType;
import org.apache.doris.nereids.trees.plans.visitor.PlanVisitor;
import org.apache.doris.statistics.Statistics;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * physical olap table sink for insert command
 */
public class PhysicalOlapTableSink<CHILD_TYPE extends Plan> extends PhysicalUnary<CHILD_TYPE> {
    private final Database database;
    private final OlapTable targetTable;
    private final List<Column> cols;
    private final List<Long> partitionIds;
    private final boolean singleReplicaLoad;

    public PhysicalOlapTableSink(Database database, OlapTable targetTable, List<Long> partitionIds, List<Column> cols,
            boolean singleReplicaLoad, LogicalProperties logicalProperties, CHILD_TYPE child) {
        this(database, targetTable, partitionIds, cols, singleReplicaLoad, Optional.empty(), logicalProperties, child);
    }

    /**
     * Constructor
     */
    public PhysicalOlapTableSink(Database database, OlapTable targetTable, List<Long> partitionIds, List<Column> cols,
            boolean singleReplicaLoad, Optional<GroupExpression> groupExpression, LogicalProperties logicalProperties,
            CHILD_TYPE child) {
        super(PlanType.PHYSICAL_OLAP_TABLE_SINK, groupExpression, logicalProperties, child);
        this.database = Preconditions.checkNotNull(database, "database != null in PhysicalOlapTableSink");
        this.targetTable = Preconditions.checkNotNull(targetTable, "targetTable != null in PhysicalOlapTableSink");
        this.cols = cols;
        this.partitionIds = partitionIds;
        this.singleReplicaLoad = singleReplicaLoad;
    }

    /**
     * Constructor
     */
    public PhysicalOlapTableSink(Database database, OlapTable targetTable, List<Long> partitionIds, List<Column> cols,
            boolean singleReplicaLoad, Optional<GroupExpression> groupExpression, LogicalProperties logicalProperties,
            PhysicalProperties physicalProperties, Statistics statistics, CHILD_TYPE child) {
        super(PlanType.PHYSICAL_OLAP_TABLE_SINK, groupExpression, logicalProperties, physicalProperties,
                statistics, child);
        this.database = Objects.requireNonNull(database, "database != null in PhysicalOlapTableSink");
        this.targetTable = Objects.requireNonNull(targetTable, "targetTable != null in PhysicalOlapTableSink");
        this.cols = copyIfNotNull(cols);
        this.partitionIds = copyIfNotNull(partitionIds);
        this.singleReplicaLoad = singleReplicaLoad;
    }

    public Database getDatabase() {
        return database;
    }

    public OlapTable getTargetTable() {
        return targetTable;
    }

    public List<Column> getCols() {
        return cols;
    }

    public List<Long> getPartitionIds() {
        return partitionIds;
    }

    public boolean isSingleReplicaLoad() {
        return singleReplicaLoad;
    }

    private <T> List<T> copyIfNotNull(List<T> list) {
        return list == null ? null : ImmutableList.copyOf(list);
    }

    @Override
    public Plan withChildren(List<Plan> children) {
        Preconditions.checkArgument(children.size() == 1, "PhysicalOlapTableSink only accepts one child");
        return new PhysicalOlapTableSink<>(database, targetTable, partitionIds, cols, singleReplicaLoad,
                getLogicalProperties(), children.get(0));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PhysicalOlapTableSink<?> that = (PhysicalOlapTableSink<?>) o;
        return singleReplicaLoad == that.singleReplicaLoad
                && Objects.equals(database, that.database)
                && Objects.equals(targetTable, that.targetTable)
                && Objects.equals(cols, that.cols)
                && Objects.equals(partitionIds, that.partitionIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(database, targetTable, cols, partitionIds, singleReplicaLoad);
    }

    @Override
    public List<Slot> computeOutput() {
        return child().getOutput();
    }

    @Override
    public List<Slot> getOutput() {
        return computeOutput();
    }

    /**
     * override function of AbstractPlan.
     */
    @Override
    public Set<Slot> getOutputSet() {
        return ImmutableSet.copyOf(getOutput());
    }

    @Override
    public <R, C> R accept(PlanVisitor<R, C> visitor, C context) {
        return visitor.visitPhysicalOlapTableSink(this, context);
    }

    @Override
    public List<? extends Expression> getExpressions() {
        return ImmutableList.of();
    }

    @Override
    public Plan withGroupExpression(Optional<GroupExpression> groupExpression) {
        return new PhysicalOlapTableSink<>(database, targetTable, partitionIds, cols, singleReplicaLoad,
                groupExpression, getLogicalProperties(), child());
    }

    @Override
    public Plan withLogicalProperties(Optional<LogicalProperties> logicalProperties) {
        return new PhysicalOlapTableSink<>(database, targetTable, partitionIds, cols, singleReplicaLoad,
                groupExpression, logicalProperties.get(), child());
    }

    @Override
    public PhysicalPlan withPhysicalPropertiesAndStats(PhysicalProperties physicalProperties, Statistics statistics) {
        return new PhysicalOlapTableSink<>(database, targetTable, partitionIds, cols, singleReplicaLoad,
                groupExpression, getLogicalProperties(), physicalProperties, statistics, child());
    }

    /**
     * get output physical properties
     */
    public PhysicalProperties getRequirePhysicalProperties() {
        if (targetTable.isPartitioned()) {
            HashDistributionInfo distributionInfo = ((HashDistributionInfo) targetTable.getDefaultDistributionInfo());
            List<Column> distributedColumns = distributionInfo.getDistributionColumns();
            List<Integer> columnIndexes = Lists.newArrayList();
            int idx = 0;
            for (int i = 0; i < targetTable.getFullSchema().size(); ++i) {
                if (targetTable.getFullSchema().get(i).equals(distributedColumns.get(idx))) {
                    columnIndexes.add(i);
                    idx++;
                    if (idx == distributedColumns.size()) {
                        break;
                    }
                }
            }
            return PhysicalProperties.createHash(columnIndexes.stream()
                    .map(colIdx -> child().getOutput().get(colIdx).getExprId())
                    .collect(Collectors.toList()), ShuffleType.NATURAL);
        } else {
            return PhysicalProperties.GATHER;
        }
    }
}
