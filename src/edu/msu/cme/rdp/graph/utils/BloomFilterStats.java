/*
 * Copyright (C) 2012 Jordan Fish <fishjord at msu.edu>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package edu.msu.cme.rdp.graph.utils;

import edu.msu.cme.rdp.graph.filter.BloomFilter;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.PrintStream;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author fishjord
 */
public class BloomFilterStats {

    public static void printStats(BloomFilter filter, PrintStream out) {


        long n = filter.getUniqueKmers();
        long m = (long) Math.pow(2, filter.getHashSizeLog2());
        int k = filter.getHashCount();

        //(1-e^(-k*((n+.5)/(m-1))))^k
        double falsePositiveRate = Math.pow((1 - Math.pow(Math.E, -k * ((n + .5) / (m - 1)))), k);

        out.println("Bloom filter created on:       " + filter.getCreatedOn());
        out.println("Serializable id:               " + BloomFilter.serialVersionUID);
        out.println();
        out.println("Bloom filter size log 2:       " + filter.getHashSizeLog2());
        out.println("Bloom filter size (bits) (m):  " + m);
        out.println();
        out.println("Number of bitsets:             " + filter.getNumBitsets());
        out.println("Bitset size (bits):            " + filter.getBitsetSize());
        out.println("Bitset size log2:              " + filter.getBitsetSizeLog2());
        out.println();
        out.println("Number of hashes (k):          " + filter.getHashCount());
        out.println("Hash function name:            " + filter.getHasherClassName());
        out.println();
        out.println("Bitset Mask:                   " + StringUtils.leftPad(Long.toBinaryString(filter.getBitsetMask()), 64, '0'));
        out.println("Hash mask:                     " + StringUtils.leftPad(Long.toBinaryString(filter.getHashMask()), 64, '0'));
        out.println();
        out.println("Kmer length:                   " + filter.getKmerSize());
        out.println();
        out.println("Total kmers in bloom filter:   " + filter.getTotalKmers());
        out.println("Total strings inserted:        " + filter.getTotalStrings());
        out.println("Total unique kmers:            " + filter.getUniqueKmers());        
        if ( filter.getSingltonKmers() > -1){
            out.println("Total mercy kmers if set:      " + filter.getMercyKmers());
            out.println("Total singleton kmers:         " + filter.getSingltonKmers());
        }
        out.println("Predicted false positive rate: " + falsePositiveRate);
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("USAGE: BloomFilterStats <bloom_filter>");
            System.exit(1);
        }

        File bloomFile = new File(args[0]);

        ObjectInputStream ois = ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(bloomFile)));
        BloomFilter filter = (BloomFilter) ois.readObject();
        ois.close();

        printStats(filter, System.out);
    }
}
