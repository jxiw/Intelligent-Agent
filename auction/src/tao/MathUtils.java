package tao;

import java.util.List;

class MathUtils {
	
	  public static double covariance(List<Double> data, int start, int end) {
	    double xmean = mean(start, end);
	    double ymean = mean(data, start, end);
	    double result = 0;

	    for (int i = start; i <= end; i++)
	      result += (i - xmean) * (data.get(i) - ymean);
	    result /= (end - start);
	    return result;
	  }

	  public static double mean(List<Double> data, int start, int end) {
	    double sum = 0;
	    for (int i = start; i <= end; i++)
	      sum += data.get(i);
	    return sum / (end - start + 1);
	  }
	  
	  public static double mean(int start, int end) {
		    return (start + end)/2.0;
	  }

	  // calculate variance of y
	  public static double variance(List<Double> data, int start, int end) {
	    double mean = mean(data, start, end);
	    double sumOfSquaredDeviations = 0;

	    for (int i = start; i <= end; i++)
	      sumOfSquaredDeviations += Math.pow(data.get(i) - mean, 2);
	    return sumOfSquaredDeviations / (end - start);
	  }
	  
	  // calculate variance of x
	  public static double variance(int start, int end) {
		    double mean = mean(start, end);
		    double sumOfSquaredDeviations = 0;

		    for (int i = start; i <= end; i++)
		      sumOfSquaredDeviations += Math.pow(i - mean, 2);
		    return sumOfSquaredDeviations / (end - start);
	  }
	}
