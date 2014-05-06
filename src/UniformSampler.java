import java.util.ArrayList;


public class UniformSampler {
	ArrayList<Variable> sampledVariables;
	ArrayList<ArrayList<Double>> distributions;
	ArrayList<Variable> nextSample;
	
	double Q = 1.0;
	
	public UniformSampler(ArrayList<Variable> variables) {
		sampledVariables = variables;
		nextSample = new ArrayList<>(sampledVariables);
	}
	
	// use this before sample()
	public ArrayList<Variable> nextEstimation(double nextZ) {
		sample(nextZ);
		
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
	
	public ArrayList<Variable> sample(double t) {
		
		for (int i = 0; i < sampledVariables.size(); i++) {
			Variable var = sampledVariables.get(i);
			ArrayList<Double> dist = distributions.get(i);
			
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
		
		
		return Q;
	}
}



