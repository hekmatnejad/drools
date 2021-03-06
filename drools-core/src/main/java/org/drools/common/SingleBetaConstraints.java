/*
 * Copyright 2005 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.drools.common;

import org.drools.RuleBaseConfiguration;
import org.drools.core.util.index.IndexUtil;
import org.drools.reteoo.BetaMemory;
import org.drools.reteoo.BetaNode;
import org.drools.reteoo.LeftTuple;
import org.drools.reteoo.builder.BuildContext;
import org.drools.rule.ContextEntry;
import org.drools.rule.constraint.MvelConstraint;
import org.drools.spi.BetaNodeFieldConstraint;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.List;

public class SingleBetaConstraints
    implements
    BetaConstraints {

    private static final long             serialVersionUID = 510l;

    protected BetaNodeFieldConstraint     constraint;

    private boolean                       indexed;

    private transient boolean             disableIndex;

    public SingleBetaConstraints() {

    }

    public SingleBetaConstraints(final BetaNodeFieldConstraint[] constraint,
                                 final RuleBaseConfiguration conf) {
        this( constraint[0],
              conf,
              false );
    }

    public SingleBetaConstraints(final BetaNodeFieldConstraint constraint,
                                 final RuleBaseConfiguration conf) {
        this( constraint,
              conf,
              false );
    }

    public SingleBetaConstraints(final BetaNodeFieldConstraint constraint,
                                 final RuleBaseConfiguration conf,
                                 final boolean disableIndex) {
        this.constraint = constraint;
        this.disableIndex = disableIndex;
    }

    public void init(BuildContext context, short betaNodeType) {
        RuleBaseConfiguration config = context.getRuleBase().getConfiguration();

        if ( (disableIndex) || (!config.isIndexLeftBetaMemory() && !config.isIndexRightBetaMemory()) ) {
            this.indexed = false;
        } else {
            initIndexes(config.getCompositeKeyDepth(), betaNodeType);
        }
    }

    public void initIndexes(int depth, short betaNodeType) {
        indexed = depth >= 1 && IndexUtil.isIndexableForNode(betaNodeType, constraint);
    }

    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        constraint  = (BetaNodeFieldConstraint)in.readObject();
        indexed     = in.readBoolean();

    }

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(constraint);
        out.writeBoolean(indexed);
    }

    public ContextEntry[] createContext() {
        return new ContextEntry[] { this.constraint.createContextEntry() };
    }

    /* (non-Javadoc)
     * @see org.drools.common.BetaNodeConstraints#updateFromTuple(org.drools.reteoo.ReteTuple)
     */
    public void updateFromTuple(final ContextEntry[] context,
                                final InternalWorkingMemory workingMemory,
                                final LeftTuple tuple) {
        context[0].updateFromTuple( workingMemory,
                                 tuple );
    }

    /* (non-Javadoc)
     * @see org.drools.common.BetaNodeConstraints#updateFromFactHandle(org.drools.common.InternalFactHandle)
     */
    public void updateFromFactHandle(final ContextEntry[] context,
                                     final InternalWorkingMemory workingMemory,
                                     final InternalFactHandle handle) {
        context[0].updateFromFactHandle( workingMemory,
                                           handle );
    }

    /* (non-Javadoc)
     * @see org.drools.common.BetaNodeConstraints#isAllowedCachedLeft(java.lang.Object)
     */
    public boolean isAllowedCachedLeft(final ContextEntry[] context,
                                       final InternalFactHandle handle) {
        return this.indexed || this.constraint.isAllowedCachedLeft( context[0],
                                                                    handle );
    }

    /* (non-Javadoc)
     * @see org.drools.common.BetaNodeConstraints#isAllowedCachedRight(org.drools.reteoo.ReteTuple)
     */
    public boolean isAllowedCachedRight(final ContextEntry[] context,
                                        final LeftTuple tuple) {
        return this.constraint.isAllowedCachedRight( tuple,
                                                     context[0] );
    }

    public boolean isIndexed() {
        return this.indexed;
    }

    public int getIndexCount() {
        return (this.indexed ? 1 : 0);
    }

    public boolean isEmpty() {
        return false;
    }

    public BetaMemory createBetaMemory(final RuleBaseConfiguration config, 
                                       final short nodeType) {
        return IndexUtil.Factory.createBetaMemory(config, nodeType, constraint);
    }

    public int hashCode() {
        return this.constraint.hashCode();
    }
    
    public BetaNodeFieldConstraint getConstraint() {
        return this.constraint;
    }

    /* (non-Javadoc)
     * @see org.drools.common.BetaNodeConstraints#getConstraints()
     */
    public BetaNodeFieldConstraint[] getConstraints() {
        return new BetaNodeFieldConstraint[] { this.constraint };
    }

    /**
     * Determine if another object is equal to this.
     *
     * @param object
     *            The object to test.
     *
     * @return <code>true</code> if <code>object</code> is equal to this,
     *         otherwise <code>false</code>.
     */
    public boolean equals(final Object object) {
        if ( this == object ) {
            return true;
        }

        if ( object == null || getClass() != object.getClass() ) {
            return false;
        }

        final SingleBetaConstraints other = (SingleBetaConstraints) object;

        return this.constraint == other.constraint || this.constraint.equals( other.constraint );
    }

    public void resetFactHandle(ContextEntry[] context) {
        context[0].resetFactHandle();
    }

    public void resetTuple(ContextEntry[] context) {
        context[0].resetTuple();
    }
    
    public BetaConstraints getOriginalConstraint() {
        throw new UnsupportedOperationException();
    }

    public long getListenedPropertyMask(List<String> settableProperties) {
        return constraint instanceof MvelConstraint ? ((MvelConstraint)constraint).getListenedPropertyMask(settableProperties) : Long.MAX_VALUE;
    }

    public boolean isLeftUpdateOptimizationAllowed() {
        return true;
    }
}
