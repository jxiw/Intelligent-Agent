package tao;

import java.util.List;

public class RegressionModel{

  double a;
  double b;
  
  public RegressionModel(){
	  a = b = 0;
  }

  public void compute(List<Double> estimationRatio, int start, int end) {
    if (end - start <= 1) {
    	b = 1; a = 0;
    }
    b = MathUtils.covariance(estimationRatio, start, end) / MathUtils.variance(start, end);
    a = MathUtils.mean(estimationRatio, start, end) - b * MathUtils.mean(start, end);
  }
  
  public double evaluateAt(double x) {
    return a + b * x;
  }
}
