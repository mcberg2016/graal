/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package org.graalvm.compiler.loop.phases;

import org.graalvm.compiler.debug.Debug;
import org.graalvm.compiler.debug.DebugCounter;
import org.graalvm.compiler.graph.Graph;
import org.graalvm.compiler.loop.LoopEx;
import org.graalvm.compiler.loop.LoopPolicies;
import org.graalvm.compiler.loop.LoopsData;
import org.graalvm.compiler.nodes.StructuredGraph;
import org.graalvm.compiler.phases.common.CanonicalizerPhase;
import org.graalvm.compiler.phases.common.util.HashSetNodeEventListener;
import org.graalvm.compiler.phases.tiers.PhaseContext;

public class LoopPartialUnrollPhase extends LoopPhase<LoopPolicies> {

    private static final DebugCounter PARTIAL_UNROLL_LOOPS = Debug.counter("LoopPartialUnroll");

    private final CanonicalizerPhase canonicalizer;

    public LoopPartialUnrollPhase(LoopPolicies policies, CanonicalizerPhase canonicalizer) {
        super(policies);
        this.canonicalizer = canonicalizer;
    }

    @Override
    @SuppressWarnings("try")
    protected void run(StructuredGraph graph, PhaseContext context) {
        if (graph.hasLoops()) {
            HashSetNodeEventListener listener = new HashSetNodeEventListener();
            try (Graph.NodeEventScope nes = graph.trackNodeEvents(listener)) {
                boolean changed = true;
                while (changed) {
                    LoopsData dataCounted = new LoopsData(graph);
                    dataCounted.detectedCountedLoops();
                    changed = false;
                    for (LoopEx loop : dataCounted.countedLoops()) {
                        if (!LoopTransformations.isUnrollableLoop(loop)) {
                            continue;
                        }
                        if (getPolicies().shouldPartiallyUnroll(loop)) {
                            if (loop.loopBegin().isSimpleLoop()) {
                                // First perform the pre/post transformation and do the partial
                                // unroll when we come around again.
                                LoopTransformations.insertPrePostLoops(loop, graph);
                                changed = true;
                            } else {
                                changed |= LoopTransformations.partialUnroll(loop, graph);
                                PARTIAL_UNROLL_LOOPS.increment();
                            }
                        }
                    }
                    dataCounted.deleteUnusedNodes();
                }
            }
            if (!listener.getNodes().isEmpty()) {
                canonicalizer.applyIncremental(graph, context, listener.getNodes());
                listener.getNodes().clear();
            }
        }
    }

    @Override
    public boolean checkContract() {
        return false;
    }
}
