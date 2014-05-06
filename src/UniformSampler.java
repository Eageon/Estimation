import java.util.ArrayList;
import java.util.Random;


public class UniformSampler {
	ArrayList<Variable> sampledVariables;
	ArrayList<ArrayList<Double>> distributions;
	ArrayList<Variable> nextSample;
	ArrayList<Variable> sampleTopOrder;
	Random random;
	
	double Q = 1.0;
	
	public UniformSampler(ArrayList<Variable> variables) {
		sampledVariables = new ArrayList<>(variables);
		nextSample = new ArrayList<>(sampledVariables);
		random = new Random();
	}
	
	// use this before sample()
	public ArrayList<Variable> nextEstimation(double nextZ) {
		sample();
		
		for (int i = 0; i < sampledVariables.size(); i++) {
			nextSample.get(i).setSoftEvidence(sampledVariables.get(i).value);
		}
		
		return nextSample;
	}
	
	public boolean isPresentSample(ArrayList<Variable> target) {
		for (int i = 0; i < sampledVariables.size(); i++) {
			if(target.get(i).value != sampledVariables.get(i).value) {
				return false;
			}
		}
		
		return true;
	}
	
	public void overallTopOrderToSampleTopOrder(ArrayList<Variable> overallTopOrder) {
		sampleTopOrder = new ArrayList<>(sampledVariables);
		
		for (Variable variable : overallTopOrder) {
			if(sampledVariables.contains(variable)) {
				sampleTopOrder.add(variable);
			}
		}
	}
	
	public void generateSamples() {
		distributions = new ArrayList<>(sampledVariables.size());
		
		for (int i = 0; i < sampledVariables.size(); i++) {
			Variable var = sampledVariables.get(i);
			double d = 1.0 / var.domainSize();
			ArrayList<Double> dist = new ArrayList<>(var.domainSize());
			for (int j = 0; j < var.domainSize(); j++) {
				dist.add((j + 1) * d);  // should be one?
			}
			dist.set(var.domainSize() - 1, 1.0);
			distributions.add(dist);
		}
		
		for (int i = 0; i < sampledVariables.size(); i++) {
			Variable var = sampledVariables.get(i);
			double d = 1.0 / var.domainSize();
			Q *= d;
		}
	}
	
	public ArrayList<Variable> sample() {
			
		for (int i = 0; i < sampledVariables.size(); i++) {
			Variable var = sampledVariables.get(i);
			ArrayList<Double> dist = distributions.get(i);
			double t = random.nextDouble();
			for (int j = 0; j < dist.size(); j++) {
				if(t < dist.get(j)) {
					var.setSoftEvidence(j);
					break;
				}
			}
		}
		
		return sampledVariables;
	}
	
	// executed after sample()
	public double computeQ() {
		double Q = 1.0;
		for (Variable variable : sampledVariables) {
			Q *= distributions.get(variable.index).get(variable.value);
		}
		return Q;
	}
	
	public void clearEvidence() {
		for (Variable variable : sampledVariables) {
			variable.isEvidence = false;
			variable.value = -1;
		}
	}
}



