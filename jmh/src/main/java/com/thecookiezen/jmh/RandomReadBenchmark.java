package com.thecookiezen.jmh;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.profile.AsyncProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
public class RandomReadBenchmark {

    @Benchmark
    public void testRead() {
        // Call your actual app code here
    }

    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
                .include(RandomReadBenchmark.class.getSimpleName())
                .forks(1)
                .addProfiler(AsyncProfiler.class, "libPath=/path/to/libasyncProfiler.so;output=flamegraph;dir=results")
                .build();

        new Runner(opt).run();
    }
}