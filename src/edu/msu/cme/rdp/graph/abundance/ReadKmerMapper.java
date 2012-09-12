/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.msu.cme.rdp.graph.abundance;

import edu.msu.cme.rdp.kmer.set.KmerSet;
import edu.msu.cme.rdp.kmer.set.NuclKmerGenerator;
import edu.msu.cme.rdp.readseq.readers.SequenceReader;
import edu.msu.cme.rdp.readseq.readers.Sequence;
import edu.msu.cme.rdp.readseq.utils.IUBUtilities;
import java.awt.Point;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author fishjord
 */
public class ReadKmerMapper {

    private final int k;
    private final KmerSet<Set<String>> kmerSet;
    private int processedSeqs = 0;

    public ReadKmerMapper(File contigFile, int k) throws IOException {
        this.k = k;

        List<Sequence> contigSeqs = SequenceReader.readFully(contigFile);

        kmerSet = new KmerSet();

        Sequence seq;
        for (int index = 0; index < contigSeqs.size(); index++) {
            seq = contigSeqs.get(index);
            String seqstr = seq.getSeqString();
            addKmers(seqstr, index);
        }

        kmerSet.printStats();
    }

    private void addKmers(String seqString, int contigIndex) {

        kmerGen = new NuclKmerGenerator(seqString, k);

        while (kmerGen.hasNext()) {
            val = kmerGen.next();
            Set<String> kmers = kmerSet.get(val);
            if (kmers == null) {
                kmers = new HashSet();
                kmerSet.add(val, kmers);
            }
        }
    }

    private long val;
    private NuclKmerGenerator kmerGen;
    private Set<String> readIds;

    private void processRead(Sequence seq) {
        processRead(seq.getSeqName(), seq.getSeqString());
        processRead(seq.getSeqName(), IUBUtilities.reverseComplement(seq.getSeqString()));
        processedSeqs++;
    }

    private void processRead(String name, String seqString) {

        kmerGen = new NuclKmerGenerator(seqString, k);

        while (kmerGen.hasNext()) {
            val = kmerGen.next();

            readIds = kmerSet.get(val);

            if (readIds == null) {
                continue;
            }

            readIds.add(name);
        }
    }

    public void printResults(PrintStream out) throws IOException {
        Set<Long> keys = kmerSet.getKeys();

        kmerGen = new NuclKmerGenerator("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", k);

        //kmer shouldn't be null yet, so we'll use it to decode the longs
        for(Long key : keys) {
            out.print(kmerGen.decodeLong(key));
            for(String readid : kmerSet.get(key)) {
                out.print(" " + readid);
            }
            out.println();
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 3 && args.length != 4) {
            System.err.println("USAGE: ReadKmerMapper <nucl_contig_file> <reads_file> <k> [#threads]");
            System.exit(1);
        }

        final File nuclContigs = new File(args[0]);
        final File readsFile = new File(args[1]);
        final int k = Integer.valueOf(args[2]);
        final int maxThreads;
        final int maxTasks = 25000;

        if (args.length == 4) {
            maxThreads = Integer.valueOf(args[3]);
        } else {
            maxThreads = Runtime.getRuntime().availableProcessors();
        }

        System.err.println("Starting kmer mapping at " + new Date());
        System.err.println("*  Number of threads:       " + maxThreads);
        System.err.println("*  Max outstanding tasks:   " + maxTasks);
        System.err.println("*  Nucleotide contigs file: " + nuclContigs);
        System.err.println("*  Reads file:              " + readsFile);
        System.err.println("*  Kmer length:             " + k);

        long startTime = System.currentTimeMillis();
        final ReadKmerMapper kmerCounter = new ReadKmerMapper(nuclContigs, k);
        System.err.println("Kmer trie built in " + (System.currentTimeMillis() - startTime) + " ms");

        System.out.println();

        final AtomicInteger processed = new AtomicInteger();
        final AtomicInteger outstandingTasks = new AtomicInteger();


        SequenceReader reader = new SequenceReader(readsFile);
        Sequence seq;

        //ExecutorService service = Executors.newFixedThreadPool(maxThreads);

        startTime = System.currentTimeMillis();
        while ((seq = reader.readNextSequence()) != null) {
            kmerCounter.processRead(seq);
            processed.incrementAndGet();

            if ((processed.get()) % 1000000 == 0) {
                System.err.println("Processed " + processed + " sequences in " + (System.currentTimeMillis() - startTime) + " ms");
            }

            /*
             * final Sequence threadSeq = seq;
             *
             * Runnable r = new Runnable() {
             *
             * public void run() { //System.err.println("Processing sequence " +
             * threadSeq.getSeqName() + " in thread " +
             * Thread.currentThread().getName());
             * kmerCounter.processSeq(threadSeq);
             * //System.err.println("Processed count " + processed);
             * //System.err.println("Outstanding count count " +
             * outstandingTasks); processed.incrementAndGet();
             * outstandingTasks.decrementAndGet(); } };
             *
             * outstandingTasks.incrementAndGet(); service.submit(r);
             *
             * //System.err.println("Submitting " + threadSeq.getSeqName() + ",
             * outstanding tasks= " + outstandingTasks);
             *
             * while (outstandingTasks.get() >= maxTasks);
             *
             * if ((processed.get() + 1) % 1000000 == 0) {
             * System.err.println("Processed " + processed + " sequences in " +
             * (System.currentTimeMillis() - startTime) + " ms"); }
             */
        }

        reader.close();

        //service.shutdown();
        //service.awaitTermination(1, TimeUnit.DAYS);

        System.err.println("Processed " + processed + " sequences in " + (System.currentTimeMillis() - startTime) + " ms");

        kmerCounter.printResults(System.out);
        //System.err.println("Unique kmers in contigs: " + kmerCounter.trie.uniqueWords());
        System.err.println("Processing complete");
    }
}
