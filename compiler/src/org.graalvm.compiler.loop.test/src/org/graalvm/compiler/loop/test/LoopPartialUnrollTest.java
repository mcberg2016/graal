/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.compiler.loop.test;

import org.graalvm.compiler.core.test.GraalCompilerTest;
import org.graalvm.compiler.nodes.LoopBeginNode;
import org.graalvm.compiler.nodes.StructuredGraph;
import org.junit.Test;

public class LoopPartialUnrollTest extends GraalCompilerTest {

    public static int sumArray(int[] text) {
        int sum = 0;
        for (int i = 0; i < text.length; ++i) {
            sum += text[i];
        }
        return sum;
    }

    @Test
    public void testSumArray() {
        int[] data = new int[1024];
        for (int i = 0; i < data.length; i++) {
            data[i] = i;
        }
        for (int i = 0; i < 1000; i++) {
            sumArray(data);
        }
        test("sumArray", data);
    }

    @Test
    public void testSumReductionFloat() {
        float[] a = new float[512];
        float[] b = new float[512];
        float[] c = new float[512];
        float[] d = new float[512];

        for (int k = 50; k < 58; k++) {
            sumInitListFloat(a, b, c, k);
            float sum = 0;
            for (int j = 0; j < 20000 * 50; j++) {
                sum = sumListReductionFloat(a, b, c, d, sum, k);
            }
        }

        for (int k = 50; k < 512; k++) {
            sumInitListFloat(a, b, c, k);
            test("sumListReductionFloat", a, b, c, d, 0.0f, k);
        }

    }

    public static void sumInitListFloat(float[] a, float[] b, float[] c, int processLen) {
        for (int i = 0; i < processLen; i++) {
            a[i] = i * 2;
            b[i] = i;
            c[i] = i + 5;
        }
    }

    public static float sumListReductionFloat(float[] a, float[] b, float[] c, float[] d, float total, int processLen) {
        float newTotal = total;
        for (int j = 0; j < 2; j++) {
            int x = 0;
            for (int i = x; i < processLen; i++) {
                d[i] = (a[i] * b[i]) + (a[i] * c[i]) + (b[i] * c[i]);
            }
        }

        newTotal += d[0];
        newTotal += d[processLen - 1];
        return newTotal;
    }

    @Test
    public void testSumReductionDouble() {
        double[] a = new double[512];
        double[] b = new double[512];
        double[] c = new double[512];
        double[] d = new double[512];

        for (int k = 50; k < 58; k++) {
            sumInitListDouble(a, b, c, k);
            double sum = 0;
            for (int j = 0; j < 20000 * 50; j++) {
                sum = sumListReductionDouble(a, b, c, d, sum, k);
            }
        }

        for (int k = 50; k < 512; k++) {
            sumInitListDouble(a, b, c, k);
            test("sumListReductionDouble", a, b, c, d, 0.0, k);
        }

    }

    public static void sumInitListDouble(double[] a, double[] b, double[] c, int processLen) {
        for (int i = 0; i < processLen; i++) {
            a[i] = i * 2;
            b[i] = i;
            c[i] = i + 5;
        }
    }

    public static double sumListReductionDouble(double[] a, double[] b, double[] c, double[] d, double total, int processLen) {
        double newTotal = total;
        for (int j = 0; j < 2; j++) {
            int x = 0;
            for (int i = x; i < processLen; i++) {
                d[i] = (a[i] * b[i]) + (a[i] * c[i]) + (b[i] * c[i]);
            }
        }

        newTotal += d[0];
        newTotal += d[processLen - 1];
        return newTotal;
    }

    @Override
    protected boolean checkLowTierGraph(StructuredGraph graph) {
        for (LoopBeginNode loop : graph.getNodes().filter(LoopBeginNode.class)) {
            if (loop.isMainLoop()) {
                return true;
            }
        }
        return true;
    }

    public static long testMultiplySnippet(int arg) {
        long r = 1;
        for (int i = 0; i < arg; i++) {
            r *= i;
        }
        return r;
    }

    @Test
    public void testMultiply() {
        for (int i = 0; i < 1000; i++) {
            testMultiplySnippet(10);
        }
        test("testMultiplySnippet", 9);
    }

    public static int testNestedSumSnippet(int d) {
        int c = 0;
        for (int i = 0; i < d; i++) {
            for (int j = 0; j < i; j++) {
                c += 1;
            }
        }
        return c;
    }

    @Test
    public void testNestedSum() {
        for (int i = 0; i < 1000; i++) {
            testNestedSumSnippet(10);
        }
        for (int i = 0; i < 1000; i++) {
            test("testNestedSumSnippet", i);
        }
    }

    public static int testSumDownSnippet(int d) {
        int c = 0;
        for (int j = d; j > -4; j--) {
            c += 1;
        }
        return c;
    }

    @Test
    public void testSumDown() {
        for (int i = 0; i < 1000; i++) {
            testSumDownSnippet(10);
        }
        test("testSumDownSnippet", 1);
        for (int i = 0; i < 8; i++) {
            test("testSumDownSnippet", i);
        }
    }

    @Test
    public void testLoopCarried() {
        for (int i = 0; i < 1000; i++) {
            testLoopCarriedSnippet(10, 8);
        }
        test("testLoopCarriedSnippet", 1, 2);
    }

    public static int testLoopCarriedSnippet(int a, int b) {
        int c = a;
        int d = b;
        for (int j = 0; j < a; j++) {
            d = c;
            c += 1;
        }
        return c + d;
    }

    public static long init = Runtime.getRuntime().totalMemory();
    private int x;
    private int z;

    public int[] testComplexSnippet(int d) {
        x = 3;
        int y = 5;
        z = 7;
        for (int i = 0; i < d; i++) {
            for (int j = 0; j < i; j++) {
                z += x;
            }
            y = x ^ z;
            if ((i & 4) == 0) {
                z--;
            } else if ((i & 8) == 0) {
                Runtime.getRuntime().totalMemory();
            }
        }
        return new int[]{x, y, z};
    }

    @Test
    public void testComplex() {
        for (int i = 0; i < 1000; i++) {
            testComplexSnippet(i);
        }
        for (int i = 0; i < 10; i++) {
            test("testComplexSnippet", i);
        }
        test("testComplexSnippet", 10);
        test("testComplexSnippet", 100);
        test("testComplexSnippet", 1000);
    }

    public final double[] execute(double[] Gi) {
        int N = Gi.length;

        int m1 = N - 1;
        for (int i = 1; i < m1; i++) {
            for (int j = 1; j < m1; j++) {
                Gi[j] = Gi[j + 1];
            }
        }
        return Gi;
    }

    public static double[] getDoubles(int N) {
        double A[] = new double[N];
        for (int i = 0; i < A.length; i++) {
            A[i] = 1;
        }
        return A;
    }

    @Test
    public void testSOR() {
        int N = 250;
        for (int i = 0; i < 10; i++) {
            execute(getDoubles(N));
        }
        for (N = 250; N >= 3; N--) {

            final int Nfinal = N;
            test("execute", supply(() -> getDoubles(Nfinal)));
        }
    }
}
