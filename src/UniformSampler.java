import java.util.ArrayList;
import java.util.Random;

public class UniformSampler {
	ArrayList<Variable> sampledVariables;
	ArrayList<Integer> sampleVariableValues;
	ArrayList<ArrayList<Double>> distributions;
	ArrayList<Variable> nextSample;
	ArrayList<Variable> sampleTopOrder;
	ArrayList<Variable> topOrder;
	GraphicalModel model;
	Random random;

	double Q = 1.0;

	public UniformSampler(ArrayList<Variable> variables) {
		sampledVariables = new ArrayList<>(variables);
		sampleVariableValues = new ArrayList<>(sampledVariables.size());
		nextSample = new ArrayList<>(sampledVariables);
		random = new Random();
		
		for (Variable variable : sampledVariables) {
			sampleVariableValues.add(-1);
		}
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
			if (target.get(i).value != sampledVariables.get(i).value) {
				return false;
			}
		}

		return true;
	}

	public void overallTopOrderToSampleTopOrder(
			ArrayList<Variable> overallTopOrder) {
		topOrder = new ArrayList<>(overallTopOrder);
		sampleTopOrder = new ArrayList<>(sampledVariables);

		for (Variable variable : overallTopOrder) {
			if (sampledVariables.contains(variable)) {
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
				dist.add((j + 1) * d); // should be one?
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
		clearEvidence();

		if (model.network.equals("MARKOV")) {
			for (int i = 0; i < sampledVariables.size(); i++) {
				Variable var = sampledVariables.get(i);
				ArrayList<Double> dist = distributions.get(i);
				double t = random.nextDouble();
				for (int j = 0; j < dist.size(); j++) {
					if (t < dist.get(j)) {
						var.setSoftEvidence(j);
						break;
					}
				}
			}
			return sampledVariables;
		}

		// For Bayesian
		for (int i = 0; i < topOrder.size(); i++) {
			Variable var = topOrder.get(i);
			double t = random.nextDouble();
			//var.setSoftEvidence(value);
			Factor factor = model.remainFactors.get(var.index);
			int[] values = new int[factor.variables.size()];
			for (int j = 0; j < factor.variables.size() - 1; j++) {
				values[j] = factor.getVariable(j).value;
				if(values[j] < 0) {
					System.out.println("Logical error");
					System.exit(0);
				}
			}
			Variable thisVariable = factor.getVariable(factor.variables.size() - 1);
			double dist = 0.0;
			for (int j = 0; j < thisVariable.domainSize(); j++) {
				values[values.length - 1] = j;	
				if (t <= (dist += factor.variableValueToProbability(values))) {
					var.setSoftEvidence(j);
					int indexSampleVar = sampleTopOrder.indexOf(var);
					if(-1 != indexSampleVar) {
						sampleVariableValues.set(indexSampleVar, j);
					}
					break;
				}
			}
		}
		

		model.clearEvidence();
		for (int i = 0; i < sampleVariableValues.size(); i++) {
			Variable var = sampledVariables.get(i);
			Integer val = sampleVariableValues.get(i);
			var.setSoftEvidence(val);
		}

		return sampledVariables;
	}

	// executed after sample()
	public double computeQ() {
		// double Q = 1.0;
		// for (Variable variable : sampledVariables) {
		// Q *= distributions.get(variable.index).get(variable.value);
		// }
		return Q;
	}

	public void clearEvidence() {
		for (Variable variable : sampledVariables) {
			variable.isEvidence = false;
			variable.value = -1;
		}
	}

	public int[] toIntArray() {
		int[] arr = new int[sampledVariables.size()];
		for (int i = 0; i < sampledVariables.size(); i++) {
			Variable var = sampledVariables.get(i);
			if (!var.isEvidence) {
				return null;
			}
			arr[i] = var.value;
		}

		return arr;
	}

}
