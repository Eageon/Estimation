import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;

public class ImportanceSampling {
	ArrayList<Variable> cutset;

	public double startSampling(GraphicalModel model, int w, int N,
			boolean isAdaptive) {
		ArrayList<Variable> topOrder = model.topologicalOrder();
		ArrayList<Variable> cutSet = wCutSet.generateWCutSet(model, w);
		this.cutset = cutSet;
		System.out.println(cutSet.size());
		UniformSampler Q = new UniformSampler(cutSet);
		Q.model = model;
		Q.overallTopOrderToSampleTopOrder(topOrder);
		Q.generateSamples();
		
		double Z = 0.0;
		Q.sample();
		ArrayList<Integer> softOrder = model.computeSoftOrder();
		ArrayList<ArrayList<Factor>> clusters = model
				.generateSoftClusters(softOrder);
		Q.clearEvidence();

		int sample100 = 0;
		Q.clearEvidence();
		ArrayList<Variable> nextSample = Q.nextEstimation(100 * 1.0 / N);
		Q.clearEvidence();
		for (ArrayList<Factor> cluster : clusters) {
			for (Factor factor : cluster) {
				if (factor.numScopes() == 0) {
					System.out.println("zero factor");
					System.out.println(factor.index);
					System.exit(0);
				}
			}
		}
		ArrayList<Double> wCache = new ArrayList<>(100);
		ArrayList<int[]> valsCache = new ArrayList<>(100);
		for (int i = 0; i < sample100; i++) {
			wCache.add(0.0);
		}
		double updateDenomintor = 0.0;
		double updateNumnomitor = 0.0;

		int tmp = 1;
		for (int i = 1; i < N; i++) {
			double z = i * (1.0 / N);
			Q.clearEvidence();
			ArrayList<Variable> softEvidence = Q.sample();

			double numerator = model.softBucketElimination(softOrder, clusters);
			double frame = 0.0;
	
			frame = numerator / Q.computeQ();
			
			if (frame == Double.POSITIVE_INFINITY) {
				System.out.println(i);
				System.out.println(Q.computeQ());
				System.exit(0);
			}
			System.out.println(i);
			System.out.println(z);
			System.out.println(Q.computeQ());
			System.out.println(numerator);
			System.out.println(frame);
			Z += frame;

			if (isAdaptive) {
				updateDenomintor += frame;
				sample100++;
				if (Q.isPresentSample(nextSample)) {
					updateNumnomitor += frame;
					System.out.println("Always present");
				}
				if (sample100 == 100 * tmp) {
					System.out.println("adapt");
					tmp++;
					if (updateDenomintor != 0.0 && updateNumnomitor != 0.0) {
						Q.Q = updateNumnomitor / updateDenomintor;
					}
					Q.clearEvidence();
					nextSample = Q.nextEstimation(tmp * 100 * (1.0 / N));
				}
			}
		}

		return Z / N;
	}

	public static void usage() {
		System.out.println("java  ImportanceSampling " + "FILENAME " + "w "
				+ "N");
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		if (3 > args.length) {
			usage();
			System.exit(0);
		}

		String fileName = args[0];
		int w = Integer.valueOf(args[1]);
		int N = Integer.valueOf(args[2]);
		boolean isAdaptive = (args[3].equals("adaptive")) ? true : false;

		long startTime = System.currentTimeMillis();

		try {
			PrintStream writer = new PrintStream(fileName + ".output." + w
					+ "." + N + "" + args[3]);
			GraphicalModel model = new GraphicalModel(fileName);
			writer.println("Network data loading completed: "
					+ model.variables.size() + " variables, "
					+ model.factors.size() + " factors");
			writer.println(model.network + " network");
			model.readSoftEvidence(fileName + ".evid");

			writer.println("Evidence loaded, and variables instantiation completed. "
					+ model.evidenceCount + " evidence");

			ImportanceSampling sampling = new ImportanceSampling();
			double result = sampling.startSampling(model, w, N, isAdaptive);

			writer.println("Elimination completed");
			writer.println("");
			writer.println("====================RESULT========================");
			if (model.network.equals("MARKOV")) {
				writer.println("Z = " + result);
			} else {
				writer.println("The probability of evidence = " + result);
				writer.println("");
				System.out.println("Empty Factor Count = "
						+ model.emptyFactorCount);
				System.out.println("probe = " + model.probe);
			}

			long endTime = System.currentTimeMillis();
			writer.println("Running Time = " + (double) (endTime - startTime)
					/ 1000 + "secs");
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Succeed!");
		System.out.println("Output file is " + fileName + ".output");
	}

}
