/*
 * Copyright 2006, 2007 Yuk Wah Wong.
 * 
 * This file is part of the WASP distribution.
 *
 * WASP is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * WASP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with WASP; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin St, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package wasp.math;

import wasp.util.Double;

/**
 * Common math operations.
 * 
 * @author ywwong
 * @author Christopher Manning
 *
 */
public class Math {

	private Math() {}

	public static double abs(double x) {
		return java.lang.Math.abs(x);
	}

	public static double ceil(double x) {
		return java.lang.Math.ceil(x);
	}
	
	public static double exp(double x) {
		return java.lang.Math.exp(x);
	}
	
	public static double log(double x) {
		return java.lang.Math.log(x);
	}
	
	public static double log2(double x) {
		return java.lang.Math.log(x)/java.lang.Math.log(2);
	}
	
	public static double sqrt(double x) {
		return java.lang.Math.sqrt(x);
	}
	
	public static double max(double x1, double x2) {
		return (x1 > x2) ? x1 : x2;
	}
	
	public static int max(int x1, int x2) {
		return (x1 > x2) ? x1 : x2;
	}
	
	public static short max(short x1, short x2) {
		return (x1 > x2) ? x1 : x2;
	}
	
	public static double min(double x1, double x2) {
		return (x1 < x2) ? x1 : x2;
	}
	
	public static int min(int x1, int x2) {
		return (x1 < x2) ? x1 : x2;
	}
	
	public static short min(short x1, short x2) {
		return (x1 < x2) ? x1 : x2;
	}
	
	public static double mean(double[] array) {
		double sum = 0;
		for (int i = 0; i < array.length; ++i)
			sum += array[i];
		return (array.length==0) ? 0 : sum/array.length;
	}
	
	public static double meanSq(double[] array) {
		double sumSq = 0;
		for (int i = 0; i < array.length; ++i)
			sumSq += array[i]*array[i];
		return (array.length==0) ? 0 : sumSq/array.length;
	}
	
	public static double random() {
		return java.lang.Math.random();
	}
	
	public static int random(int first, int last) {
		return (int) (first + java.lang.Math.random()*(last-first+1));
	}
	
	public static double round(double x, double precision) {
		return java.lang.Math.round(x/precision)*precision;
	}
	
	/**
	 * Returns the sample standard deviation given a sample of values from some larger population.
	 * 
	 * @param array a sample of values from some larger population.
	 * @return the sample standard deviation of the given sample.
	 */
	public static double stdDev(double[] array) {
		double mean = mean(array);
		double meanSq = meanSq(array);
		double n = array.length;
		return java.lang.Math.sqrt(n*(meanSq-mean*mean)/(n-1));
	}
	
	public static double[] confInterval95(double[] array) {
		double[] interval = new double[2];
		if (array.length == 1) {
			interval[0] = Double.NEGATIVE_INFINITY;
			interval[1] = Double.POSITIVE_INFINITY;
		} else {
			double mean = mean(array);
			double stdErr = stdDev(array) / java.lang.Math.sqrt(array.length);
			interval[0] = mean - t025(array.length-1)*stdErr;
			interval[1] = mean + t025(array.length-1)*stdErr;
		}
		return interval;
	}
	
	private static final double[] T025 = {
		12.706, 4.303, 3.182, 2.776, 2.571,
		2.447, 2.365, 2.306, 2.262, 2.228,
		2.201, 2.179, 2.160, 2.145, 2.131,
		2.120, 2.110, 2.101, 2.093, 2.086,
		2.080, 2.074, 2.069, 2.064, 2.060,
		2.056, 2.052, 2.048, 2.045, 1.960
	};
	private static double t025(int nu) {
		return (nu >= 30) ? T025[T025.length-1] : T025[nu-1];
	}
	
    /** If a difference is bigger than this in log terms, then the sum or
     *  difference of them will just be the larger (to 12 or so decimal
     *  places). 
     */
    private static final double LOG_TOLERANCE = 30.0;

    /** Returns the log of the sum of two numbers, which are
     *  themselves input in log form.  This uses natural logarithms.
     *  Reasonable care is taken to do this as efficiently as possible
     *  (under the assumption that the numbers might differ greatly in
     *  magnitude), with high accuracy, and without numerical overflow.
     *  Also, handle correctly the case of arguments being -Inf (e.g.,
     *  probability 0).
     *  
     *  @param lx First number, in log form
     *  @param ly Second number, in log form
     *  @return log(exp(lx) + exp(ly))
     */
    public static double logAdd(double lx, double ly) {
        double max, negDiff;
        if (lx > ly) {
            max = lx;
            negDiff = ly - lx;
        } else {
            max = ly;
            negDiff = lx - ly;
        }
        if (max == Double.NEGATIVE_INFINITY) {
            return max;
        } else if (negDiff < -LOG_TOLERANCE) {
            return max;
        } else {
            return max + log(1.0 + exp(negDiff));
        }
    }           

    /** Returns the log of the sum of an array of numbers, which are
     *  themselves input in log form.  This is all natural logarithms.
     *  Reasonable care is taken to do this as efficiently as possible
     *  (under the assumption that the numbers might differ greatly in
     *  magnitude), with high accuracy, and without numerical overflow.
     *  
     *  @param logInputs An array of numbers [log(x1), ..., log(xn)]
     *  @return log(x1 + ... + xn)
     */
    public static double logSum(double[] logInputs) {
        int leng = logInputs.length;
        if (leng == 0) {
            throw new IllegalArgumentException();
        }
        int maxIdx = 0;
        double max = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < leng; i++) {
            if (logInputs[i] > max) {
                maxIdx = i;
                max = logInputs[i];
            }
        }
        if (max == Double.NEGATIVE_INFINITY)
            return max;
        boolean haveTerms = false;
        double intermediate = 0.0;
        double cutoff = max - LOG_TOLERANCE;
        // we avoid rearranging the array and so test indices each time!
        for (int i = 0; i < leng; i++) {
            if (i != maxIdx && logInputs[i] >= cutoff) {
                haveTerms = true;
                intermediate += exp(logInputs[i] - max);
            }
        }
        if (haveTerms) {
            return max + log(1.0 + intermediate);
        } else {
            return max;
        }
    }           

}
