//package CovidInfoAssignment;
//import java.io.IOException;
//
//public class Benchmark {
//  public static void main(String[] args) throws IOException {
//      org.openjdk.jmh.Main.main(args);
//  }
//}

// Benchmarking Results
// Producer

//# Warmup Iteration   1: 11502.681 ops/s
//        Iteration   1: 13090.682 ops/s
//        Iteration   2: 8678.023 ops/s
//        Iteration   3: 11926.081 ops/s
//        Iteration   4: 12935.639 ops/s
//        Iteration   5: 13015.756 ops/s

//Result "CovidInfoAssignment.CovidInfo.StartProducer":
//        11929.236 ±(99.9%) 7232.840 ops/s [Average]
//        (min, avg, max) = (8678.023, 11929.236, 13090.682), stdev = 1878.345
//        CI (99.9%): [4696.397, 19162.076] (assumes normal distribution)

//        Benchmark  	                                Mode	Cnt	Score	    Error	    Units
//        CovidInfoAssignment.CovidInfo.StartProducer	thrpt	5	11929.236	±7232.840	ops/s

// Consumer
//# Warmup Iteration   1: 0.010 s/op
//        Iteration   1: 2.726 s/op
//        Iteration   2: 2.935 s/op
//        Iteration   3: 2.340 s/op
//        Iteration   4: 2.538 s/op
//        Iteration   5: 3.498 s/op

//Result "CovidInfoAssignment.CovidInfo.StartConsumer":
//        2.808 ±(99.9%) 1.713 s/op [Average]
//        (min, avg, max) = (2.340, 2.808, 3.498), stdev = 0.445
//        CI (99.9%): [1.095, 4.520] (assumes normal distribution)

//        Benchmark  	                                Mode	Cnt	Score	Error	Units
//        CovidInfoAssignment.CovidInfo.StartConsumer 	avgt	5	2.808	±1.713	s/op



