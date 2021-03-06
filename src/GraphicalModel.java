import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

public class GraphicalModel {
	int numVariables;
	ArrayList<Factor> factors;

	ArrayList<Variable> variables;
	LinkedList<Factor> remainFactors;

	LinkedList<Variable> orderVariables;
	//LinkedList<ArrayList<Factor>> clusters;
	LinkedList<Variable> evidenceVars;
	ArrayList<Variable> nonEvidenceVars;
	int evidenceCount = 0;
	Factor lastFactor;

	double reservedResult = 1.0;
	int emptyFactorCount = 0;

	String network;

	public GraphicalModel(String fileName) {
		buildModel(fileName);
	}

	public Variable getVariable(int index) {
		if (0 > index)
			return null;

		return variables.get(index);
	}

	public void setVariable(int index, Variable variable) {
		if (0 > index)
			return;

		variables.set(index, variable);
	}

	public Factor getFactor(int index) {
		if (0 > index)
			return null;

		return factors.get(index);
	}

	public void setFactor(int index, Factor factor) {
		if (0 > index)
			return;

		factors.set(index, factor);
	}

	// @SuppressWarnings({ "null", "unused" })
	private void buildMarkovNetwork(BufferedReader reader)
			throws NumberFormatException, IOException {

		buildVariableAndFactorWithValue(reader);

		// Then set CPT
		int indexFactor = 0;
		String head = null;
		int actualCount = 0;
		while (null != (head = reader.readLine())) {
			if (head.equals("")) {
				continue; // escape the newline
			}
			actualCount++;

			int numCells = Integer.valueOf(head);
			Factor factor = factors.get(indexFactor++);
			factor.initTable(numCells);

			@SuppressWarnings("unused")
			int numLinesToRead = numCells / factor.numColomns();
			String tableRow = null;

			tableRow = reader.readLine();
			if (null == tableRow || tableRow.equals("")) {
				System.out.println("Function row format error: less lines");
				System.exit(-1);
			}

			tableRow = tableRow.trim();
			String[] tableRowValues = tableRow.split("\t| ");
			if (tableRowValues.length != numCells) {
				System.out.println("Function row format error: less colomn");
				System.exit(-1);

			}

			for (int j = 0; j < tableRowValues.length; j++) {
				factor.setTableValue(j, Double.valueOf(tableRowValues[j]));
			}
		}

		if (actualCount != factors.size()) {
			System.out
					.println("Format error: actual Factor data less the preamble");
			System.exit(-1);
		}
	}

	private void buildVariableAndFactorWithValue(BufferedReader reader)
			throws NumberFormatException, IOException {
		int size = Integer.valueOf(reader.readLine());
		numVariables = size;
		variables = new ArrayList<>(size);

		String line3 = null;
		while (null != (line3 = reader.readLine())) {
			if (line3.length() > 1)
				break;
		}
		String[] domains = line3.split(" ");

		// set variables
		int indexVar = 0;
		for (String s : domains) {
			int domainSize = Integer.valueOf(s);
			Variable v = new Variable(domainSize);
			v.index = indexVar++;
			variables.add(v);
		}

		String line4 = reader.readLine();
		int numFactors = Integer.valueOf(line4);
		factors = new ArrayList<>(numFactors);

		// initialize factors without setting the table
		String factorLine = null;
		while (null != (factorLine = reader.readLine())
				&& !factorLine.equals("")) {
			String[] args = factorLine.split("\t| ");
			if (2 > args.length)
				continue;
			@SuppressWarnings("unused")
			int indexLastVariable = Integer.valueOf(args[args.length - 1]);
			int numScopes = Integer.valueOf(args[0]);
			// variables.get(indexLastVariable).domainSize();
			Factor factor = new Factor(numScopes);
			factors.add(factor);
			factor.index = factors.size() - 1;

			for (int i = 1; i < args.length; i++) {
				int indexVariable = Integer.valueOf(args[i]);
				factor.setVariable(i - 1, this.getVariable(indexVariable));
			}

			// set neighbors of variables
			for (int i = 1; i < args.length; i++) {
				int iVar = Integer.valueOf(args[i]);
				for (int j = 1; j < args.length; j++) {
					int jVar = Integer.valueOf(args[j]);
					if (i == j)
						continue;

					variables.get(iVar).addNeighbor(variables.get(jVar));
				}
			}
		}

		if (null == factorLine) {
			System.out.println("File Format problem");
			System.exit(-1);
		}
	}

	private void buildBayesianNetwork(BufferedReader reader)
			throws NumberFormatException, IOException {

		buildVariableAndFactorWithValue(reader);

		// Then set CPT / Factor
		int indexFactor = 0;
		String head = null;
		int actualCount = 0;
		while (null != (head = reader.readLine())) { // Each iteration is a CPT
			if (head.equals("")) {
				continue; // escape the newline
			}
			actualCount++;

			int numCells = Integer.valueOf(head);

			Factor factor = factors.get(indexFactor++);
			factor.initTable(numCells);

			int numLinesToRead = numCells / factor.numColomns();

			String tableRow = null;
			int i = 0;
			for (i = 0; i < numLinesToRead; i++) {
				tableRow = reader.readLine();
				if (null == tableRow || tableRow.equals("")) {
					System.out.println("Factor row format error: less lines");
					System.exit(-1);
				}

				tableRow = tableRow.trim();
				String[] tableRowValues = tableRow.split("\t| ");
				if (tableRowValues.length != factor.numColomns()) {
					System.out.println("Factor row format error: less colomn");
					System.exit(-1);

				}

				for (int j = 0; j < tableRowValues.length; j++) {
					factor.setTableValue(i * factor.numColomns() + j,
							Double.valueOf(tableRowValues[j]));
				}
			}

			// judge the validity of the file
			if (i != numLinesToRead) {
				System.out.println("Factor " + i + ": wrong CPT format");
				System.exit(-1);
			}
			// set graph is set neighbors of each variable
			// it is done by buildVaraibleAndFactorValue
			// factor.setGraph(); // very important
		}

		if (actualCount != factors.size()) {
			System.out
					.println("Format error: actual Factor data less the preamble");
			System.exit(-1);
		}

	}

	public void buildModel(String fileName) {
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(fileName));
		} catch (FileNotFoundException e) {
			System.out.println("Can NOT find file " + fileName);
			System.exit(-1);
		}

		try {
			String network = reader.readLine();
			if (network.equals("MARKOV")) {
				this.network = "MARKOV";
				buildMarkovNetwork(reader);
			} else if (network.equals("BAYES")) {
				this.network = "BAYES";
				buildBayesianNetwork(reader);
			} else {
				System.out.println("Wrong preamble " + network);
			}

			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void readEvidence(String fileName) {
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(fileName));
		} catch (FileNotFoundException e) {
			System.out.println("Can NOT find file " + fileName);
			System.exit(-1);
		}

		String numEvidenceLine = null;
		try {
			if (null == (numEvidenceLine = reader.readLine())) {
				System.out
						.println("Format error: Can NOT read number of evidence");
				System.exit(-1);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		numEvidenceLine = numEvidenceLine.trim();
		int numEvidence = Integer.valueOf(numEvidenceLine);

		String line = null;
		int actualEvidence = 0;
		try {
			while (null != (line = reader.readLine())) {
				if (line.equals("")) {
					continue; // escape the newline
				}

				actualEvidence++;

				line = line.trim();
				// line = line.replaceAll("\\s+", " ");
				String[] args = line.split("\\s+|\t");
				if (2 != args.length) {
					System.out
							.println("Format error: Evidence line must contain exact two argument");
					System.exit(-1);
				}

				int indexVariable = Integer.valueOf(args[0]);
				int value = Integer.valueOf(args[1]);

				Variable variable = variables.get(indexVariable);
				variable.setEvidence(value); // setEvidence will intantiate
												// factor
			}
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (actualEvidence != numEvidence) {
			System.out
					.println("Format error: actual evidence less than indication");
			System.exit(-1);
		}

		evidenceCount = actualEvidence;
		validateVariables();
		validateFactors();
	}

	/**
	 * Compute the min-degree order
	 */
	public LinkedList<Variable> computeOrder() {
		orderVariables = new LinkedList<>();

		VariableHeap minHeap = new VariableHeap(nonEvidenceVars);
		minHeap.buildHeap();
		// minHeap.printHeap();

		while (!minHeap.isEmpty()) {
			Variable minDegreeVar = nonEvidenceVars.get(minHeap.deleteMin());
			if (false == minDegreeVar.isEvidence) {
				orderVariables.add(minDegreeVar);
			}

			// add edge to non-adjacent neighbor variables
			for (Variable v : minDegreeVar.neighbors) {
				for (Variable n : minDegreeVar.neighbors) {
					if (n == v) {
						continue;
					}

					// not adjacent then add edge
					v.addNeighbor(n);
				}
				minHeap.adjustHeap(v.index, false);
			}

			// delete minDegreeVar from graph and the edges
			minDegreeVar.destroyVariableNeighborhood();
			for (Variable n : minDegreeVar.neighbors) {
				minHeap.adjustHeap(n.index, true);
			}
			minDegreeVar.neighbors.clear();
		}

		return new LinkedList<Variable>(orderVariables); // make a copy
	}

	public LinkedList<ArrayList<Factor>> generateClusters() {

		Eliminator.setFactorCount(remainFactors.size());

		LinkedList<ArrayList<Factor>> clusters = new LinkedList<ArrayList<Factor>>();

		for (Variable var : orderVariables) {
			// LinkedList<Factor> mentions = var.getFactorsMentionThis();
			if (true == var.isEvidence) {
				continue;
			}

			LinkedList<Factor> mentions = new LinkedList<>();
			LinkedList<Factor> copyRemainFactors = new LinkedList<>(remainFactors);
			Iterator<Factor> remainIter = copyRemainFactors.iterator();

			while (remainIter.hasNext()) {
				Factor nextFactor = remainIter.next();
				if (nextFactor.inScope(var)) {
					mentions.add(nextFactor);
					remainIter.remove();
				}
			}

			/*
			 * if(0 == mentions.size()) { // evidence variable
			 * System.out.println("Empty bucket: variable index = " +
			 * var.index); }
			 */

			ArrayList<Factor> cluster = new ArrayList<>(mentions);
			clusters.add(cluster);
		}

		return clusters;
	}

	public double startElimination() {
		
		LinkedList<ArrayList<Factor>> clusters = generateClusters();
		
		double result = reservedResult;

		// remainFactors = new LinkedList<>(factors);
		Eliminator.setFactorCount(remainFactors.size());

		while (!orderVariables.isEmpty()) {
			ArrayList<Factor> cluster = clusters.poll();
			Variable var = orderVariables.poll();

			ArrayList<Factor> mentions = cluster;
			if (mentions.size() == 0) {
				continue;
			}
			Factor newFactor = Eliminator.Product(mentions);
			newFactor = Eliminator.SumOut(newFactor, var);

			if (0 == newFactor.numScopes()) {
				result *= newFactor.getTabelValue(0);
				emptyFactorCount++;
				continue;
			}

			boolean putInNewBucket = false;
			Iterator<Variable> carryVariable = orderVariables.iterator();
			Iterator<ArrayList<Factor>> carryCluster = clusters.iterator();
			while (carryVariable.hasNext()) {
				ArrayList<Factor> nextCluster = carryCluster.next();
				if (newFactor.inScope(carryVariable.next())) {
					nextCluster.add(newFactor); // add new factor to next
												// properiate bucket
					putInNewBucket = true;
					break;
				}
			}

			if (false == putInNewBucket) {
				System.out.println(newFactor.table);
			}

			// LinkedList<Factor> mentionsCopy = new LinkedList<>(mentions);

			// remove mention factor from the list of mentions of all the
			// variables
			// that get envolved with this factor
			/*
			 * for (int i = 0; i < mentionsCopy.size(); i++) { Factor
			 * mentionFactor = mentionsCopy.get(i);
			 * remainFactors.remove(mentionFactor);
			 * 
			 * for (int j = 0; j < mentionFactor.numScopes(); j++) {
			 * mentionFactor.getVariable(j).removeMentionFactor( mentionFactor);
			 * } }
			 * 
			 * // add the new factor to the mention list of the all the
			 * variables // in the scope of new factor for (Variable varScope :
			 * newFactor.variables) { if(var == varScope) { continue; }
			 * varScope.addMentionFactor(newFactor); }
			 */

			lastFactor = newFactor;
			if (null == lastFactor) {
				System.out.println();
			}
			// remainFactors.add(newFactor);
			// validateRemainFactors();
		}

		// this.evidenceVars = evidenceVarsAfterElim;
		// prune();
		// finalize();
		return result;
	}

	@SuppressWarnings("unused")
	private void prune() {
		Iterator<Factor> iter = remainFactors.iterator();
		while (iter.hasNext()) {
			Factor factor = iter.next();
			if (!(factor.table.get(0) < 1.0) || !(factor.table.get(0) > 0.0)) {
				iter.remove();
			}
		}
	}

	private void validateVariables() {
		nonEvidenceVars = new ArrayList<>(variables.size() - evidenceCount);

		int count = 0;
		for (Variable variable : variables) {
			if (false == variable.isEvidence) {
				variable.index = count++; // relabel the index of variable
				nonEvidenceVars.add(variable);
			}
		}
	}

	private void validateFactors() {
		Iterator<Factor> iter = factors.iterator();
		remainFactors = new LinkedList<>();

		int index = 0;
		while (iter.hasNext()) {
			Factor factor = iter.next();
			if (0 == factor.numScopes()) {
				for (Double var : factor.table) {
					// System.out.print(var + " ");
					reservedResult *= var;
				}
				// System.out.println("");

				iter.remove();
			} else {
				factor.index = index++;
				remainFactors.add(factor);
			}
		}

		// System.out.println("End of validation");
	}

	private void validateRemainFactors() {
		Iterator<Factor> iter = remainFactors.iterator();

		while (iter.hasNext()) {
			Factor factor = iter.next();
			if (0 == factor.numScopes()) {
				for (Double var : factor.table) {
					System.out.print(var + " ");
					reservedResult *= var;
				}
				System.out.println("");

				iter.remove();
			}
		}

		System.out.println("End of remain validation");
	}

	public void finalize() {
		/*
		 * for (Factor factor : remainFactors) { // System.out.println("Factor "
		 * + factor.index); // for (Variable var : factor.variables) { //
		 * System.out.print(var.index + " "); // } // System.out.println(""); //
		 * System.out.println("End variables"); // for (Double var :
		 * factor.table) { // System.out.print(var + " "); // } //
		 * System.out.println(""); // System.out.println("End table"); result *=
		 * factor.table.get(0); }
		 */
		for (Double var : lastFactor.table) {
			reservedResult *= var;
		}
	}

	public void setSoftEvidence(LinkedList<Variable> avaiVariables,
			ArrayList<Variable> vars, ArrayList<Integer> vals) {
		// erase all of the evidence marker of variables
		for (Variable var : avaiVariables) {
			var.isEvidence = false;
			var.value = -1;
		}

		// align variable
		int index = 0;
		for (Variable var : vars) {
			var.setSoftEvidence(vals.get(index));
			index++;
		}
	}
	
	public double computeTempResult() {
		double tempResult = 1.0;
		
		for (Factor factor : remainFactors) {
			boolean allAssigned = true;
			for (Variable variable : factor.variables) {
				if(variable.isEvidence) {
					continue;
				}
				allAssigned = false;
			}
			if(allAssigned) {
				tempResult *= factor.getTabelValue(factor.underlyVariableToTableIndex());
			}
		}
		
		return tempResult;
	}

	public ArrayList<Integer> computeSoftOrder() {
		// number of non-evidence variables
		int nne = 0;
		ArrayList<Boolean> processed = new ArrayList<>(nonEvidenceVars.size());
		for (int i = 0; i < nonEvidenceVars.size(); i++) {
			if (nonEvidenceVars.get(i).isEvidence) {
				processed.set(i, true);
			} else {
				nne++;
			}
		}

		ArrayList<Integer> order = new ArrayList<Integer>(nne);
		ArrayList<Set<Integer>> clusters = new ArrayList<Set<Integer>>(nne);
		ArrayList<Set<Integer>> graph = new ArrayList<>(nonEvidenceVars.size());

		for (Set<Integer> set : graph) {
			set = new HashSet<Integer>();
		}

		for (Set<Integer> set : clusters) {
			set = new HashSet<Integer>();
		}

		for (int i = 0; i < remainFactors.size(); i++) {
			// Ignore the evidence variables
			for (int j = 0; j < remainFactors.get(i).variables.size(); j++) {
				int a = remainFactors.get(i).variables.get(j).index;
				if (nonEvidenceVars.get(a).isEvidence)
					continue;
				for (int k = j + 1; k < remainFactors.get(i).variables.size(); k++) {
					int b = remainFactors.get(i).variables.get(k).index;
					if (nonEvidenceVars.get(b).isEvidence)
						continue;
					graph.get(a).add(b);
					graph.get(b).add(a);
				}
			}
		}
		int max_cluster_size = 0;
		for (int i = 0; i < nne; i++) {
			// Find the node with the minimum number of nodes
			int min = nonEvidenceVars.size();
			for (int j = 0; j < graph.size(); j++) {
				if (processed.get(j))
					continue;
				if (min > graph.get(j).size()) {
					// order[i]=j;
					order.set(i, j);
					min = graph.get(j).size();
				}
			}
			// Connect the neighbors of order[i] to each other
			int var = order.get(i);
			processed.set(var, true);
			for (int a = 0; a < graph.size(); a++) {
				for (int b = 0; b < graph.size(); b++) {
					if (a == b)
						continue;
					graph.get(a).add(b);
					graph.get(b).add(a); // issue
				}
			}

			clusters.set(i, graph.get(var));
			if (clusters.get(i).size() > max_cluster_size) {
				max_cluster_size = clusters.get(i).size();
			}
			// Remove var from the graph
			for (int a = 0; a < graph.size(); a++) {
				graph.get(a).remove(var);
			}
			graph.get(var).clear();
		}

		return order;
	}
	
	public LinkedList<ArrayList<Factor>> generateSoftClusters(ArrayList<Integer> softOrders) {

		Eliminator.setFactorCount(remainFactors.size());

		LinkedList<ArrayList<Factor>> clusters = new LinkedList<ArrayList<Factor>>();

		for (Integer i : softOrders) {
			// LinkedList<Factor> mentions = var.getFactorsMentionThis();
			Variable var = nonEvidenceVars.get(i);
			if (true == var.isEvidence) {
				continue;
			}

			LinkedList<Factor> mentions = new LinkedList<>();
			// very important
			LinkedList<Factor> copyRemainFactors = new LinkedList<>(remainFactors);
			Iterator<Factor> remainIter = copyRemainFactors.iterator();

			while (remainIter.hasNext()) {
				Factor nextFactor = remainIter.next();
				if (nextFactor.inScope(var)) {
					mentions.add(nextFactor);
					remainIter.remove();
				}
			}

			/*
			 * if(0 == mentions.size()) { // evidence variable
			 * System.out.println("Empty bucket: variable index = " +
			 * var.index); }
			 */

			ArrayList<Factor> cluster = new ArrayList<>(mentions);
			clusters.add(cluster);
		}

		return clusters;
	}
	

	public double softBucketElimination(ArrayList<Integer> softOrder, LinkedList<ArrayList<Factor>> clusters) {
		double result = reservedResult;

		LinkedList<Integer> orderedVariables = new LinkedList<>(softOrder);

		// bucket order
		while (!orderedVariables.isEmpty()) {
			ArrayList<Factor> cluster = clusters.poll();
			Variable var = nonEvidenceVars.get(orderedVariables.poll());

			ArrayList<Factor> mentions = cluster;
			if (mentions.size() == 0) {
				continue;
			}
			
			
			Factor newFactor = Eliminator.Product(mentions);
			newFactor = Eliminator.SumOut(newFactor, var);

			if (0 == newFactor.numScopes()) {
				result *= newFactor.getTabelValue(0);
				emptyFactorCount++;
				continue;
			}
			double Q = 1.0;
			boolean putInNewBucket = false;
			Iterator<Integer> carryVariableInt = orderedVariables.iterator();
			Iterator<ArrayList<Factor>> carryCluster = clusters.iterator();
			while (carryVariableInt.hasNext()) {
				ArrayList<Factor> nextCluster = carryCluster.next();
				if (newFactor.inScope(nonEvidenceVars.get(carryVariableInt.next()))) {
					nextCluster.add(newFactor); // add new factor to next
												// properiate bucket
					putInNewBucket = true;
					break;
				}
			}

			if (false == putInNewBucket) {
				System.out.println(newFactor.table);
			}

			lastFactor = newFactor;
			if (null == lastFactor) {
				System.out.println();
			}
		}

		// this.evidenceVars = evidenceVarsAfterElim;
		// prune();
		// finalize();
		return result;
	}

	public static void usage() {
		System.out.println("java  GraphicalModel " + "FILENAME");
	}
	
	public static void runSoftElimination(String[] args) {
		// TODO Auto-generated method stub

	}

	public static void main(String[] args) {
		if (1 != args.length) {
			usage();
		}
		

		String fileName = args[0];

		try {
			PrintStream writer = new PrintStream(fileName + ".output");
			GraphicalModel model = new GraphicalModel(fileName);
			writer.println("Network data loading completed: "
					+ model.variables.size() + " variables, "
					+ model.factors.size() + " factors");
			writer.println(model.network + " network");
			model.readEvidence(fileName + ".evid");
			writer.println("Evidence loaded, and variables instantiation completed. "
					+ model.evidenceCount + " evidence");
			model.computeOrder();
			writer.println("Ordering computed");
			writer.println("Order:");
			for (Variable var : model.orderVariables) {
				writer.print(var.index + ", ");
			}
			writer.println("");

			double result = model.startElimination();
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
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Succeed!");
		System.out.println("Output file is " + fileName + ".output");

	}
}
