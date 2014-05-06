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

public class GraphicalModel implements Iterable<int[]> {
	int numVariables;
	ArrayList<Factor> factors;

	ArrayList<Variable> variables;
	ArrayList<Factor> remainFactors;

	LinkedList<Variable> orderVariables;
	// LinkedList<ArrayList<Factor>> clusters;
	LinkedList<Variable> evidenceVars;
	ArrayList<Variable> nonEvidenceVars;

	// soft version of elimination
	ArrayList<Integer> softOrder;
	ArrayList<ArrayList<Factor>> softClusters;

	BufferedReader reader = null;

	int evidenceCount = 0;
	Factor lastFactor;

	double reservedResult = 1.0;
	int emptyFactorCount = 0;
	int probe = 0;

	String network;

	public GraphicalModel(String fileName) {
		this(fileName, true);
	}

	public GraphicalModel(String fileName, boolean initTable) {
		buildModel(fileName, initTable);
	}

	public GraphicalModel(String fileName, boolean initFactor, boolean initTable) {
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(fileName));
			this.reader = reader;
		} catch (FileNotFoundException e) {
			System.out.println("Can NOT find file " + fileName);
			System.exit(-1);
		}

		try {
			String network = reader.readLine();
			if (network.equals("MARKOV")) {
				this.network = "MARKOV";
				buildMarkovNetwork(reader, initFactor, initTable);
			} else if (network.equals("BAYES")) {
				this.network = "BAYES";
				buildBayesianNetwork(reader, initFactor, initTable);
			} else {
				System.out.println("Wrong preamble " + network);
			}

			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		nonEvidenceVars = variables;
		remainFactors = factors;
	}

	public void clearEvidence() {
		for (Variable var : variables) {
			var.value = -1;
			var.isEvidence = false;
		}

		for (Variable var : nonEvidenceVars) {
			var.value = -1;
			var.isEvidence = false;
		}
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

	public void initTabelWithoutSettingValue() {
		for (Factor factor : factors) {
			factor.initTable();
		}
	}

	public void buildMarkovNetwork(BufferedReader reader, boolean initFactor,
			boolean initTable) throws NumberFormatException, IOException {

		buildStructure(reader, initFactor);

		if (!initTable) {
			return;
		}

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

	public void buildStructure(BufferedReader reader, boolean initFactor)
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
			v.prevIndex = v.index;
			variables.add(v);
		}

		if (!initFactor) {
			return;
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

	public void buildBayesianNetwork(BufferedReader reader, boolean initFactor,
			boolean initTable) throws NumberFormatException, IOException {

		buildStructure(reader, initFactor);

		if (!initTable) {
			return;
		}

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

	public void buildModel(String fileName, boolean initTable) {
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(fileName));
			this.reader = reader;
		} catch (FileNotFoundException e) {
			System.out.println("Can NOT find file " + fileName);
			System.exit(-1);
		}

		try {
			String network = reader.readLine();
			if (network.equals("MARKOV")) {
				this.network = "MARKOV";
				buildMarkovNetwork(reader, true, initTable);
			} else if (network.equals("BAYES")) {
				this.network = "BAYES";
				buildBayesianNetwork(reader, true, initTable);
			} else {
				System.out.println("Wrong preamble " + network);
			}

			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		nonEvidenceVars = variables;
		remainFactors = factors;
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
				variable.setEvidence(value); // setEvidence will intantiate //
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
		nonEvidenceVars = variables;
		validateVariables();
		validateFactors();
	}

	public void readSoftEvidence(String fileName) {
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
				variable.setSoftEvidence(value); // setEvidence will intantiate
													// // factor
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
		nonEvidenceVars = variables;
		// validateVariables();
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
			LinkedList<Factor> copyRemainFactors = new LinkedList<>(
					remainFactors);
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

		// soft elimination you don't have to exec this function
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
		remainFactors = new ArrayList<>(factors.size());

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

	@SuppressWarnings("unused")
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

	public void setSoftEvidence(ArrayList<Variable> vars, int[] vals,
			boolean clear) {
		if (clear) {
			for (Variable var : variables) {
				var.isEvidence = false;
				var.value = -1;
			}
		}

		// align variable
		for (int i = 0; i < vars.size(); i++) {
			Variable var = vars.get(i);
			var.setSoftEvidence(vals[i]);
		}
	}

	public void setSoftEvidence(ArrayList<Variable> vars,
			ArrayList<Integer> vals, boolean clear) {
		// erase all of the evidence marker of variables
		if (clear) {
			for (Variable var : variables) {
				var.isEvidence = false;
				var.value = -1;
			}
		}

		// align variable
		// int index = 0;
		// for (Variable var : vars) {
		// var.setSoftEvidence(vals.get(index));
		// index++;
		// }
		for (int i = 0; i < vars.size(); i++) {
			Variable var = vars.get(i);
			var.setSoftEvidence(vals.get(i));
		}
	}

	public void setSoftEvidence(ArrayList<Variable> avaiVariables,
			ArrayList<Variable> vars, ArrayList<Integer> vals, boolean clear) {
		// erase all of the evidence marker of variables
		if (clear) {
			for (Variable var : avaiVariables) {
				var.isEvidence = false;
				var.value = -1;
			}
		}

		// align variable
		for (int i = 0; i < vars.size(); i++) {
			Variable var = vars.get(i);
			var.setSoftEvidence(vals.get(i));
		}
	}

	public double computeTempResult() {
		double tempResult = 1.0;

		for (Factor factor : remainFactors) {
			boolean allAssigned = true;
			for (Variable variable : factor.variables) {
				if (variable.isEvidence) {
					continue;
				}
				allAssigned = false;
			}
			if (allAssigned) {
				tempResult *= factor.getTabelValue(factor
						.underlyVariableToTableIndex());
			}
		}

		return tempResult;
	}

	public ArrayList<Variable> topologicalOrder() {
		ArrayList<Variable> copyVariables = new ArrayList<>(nonEvidenceVars);
		ArrayList<Factor> copyFactors = new ArrayList<>(remainFactors.size());
		for (int i = 0; i < remainFactors.size(); i++) {
			Factor oneFactorCopy = new Factor(remainFactors.get(i).variables);
			oneFactorCopy.index = i;
			copyFactors.add(oneFactorCopy);
		}

		ArrayList<Variable> topOrder = new ArrayList<>(nonEvidenceVars.size());
		if (network.equals("BAYES")) {
			while (!copyFactors.isEmpty()) {
				// find a node that doesn't have parent
				Factor thisFactor = copyFactors.get(0);
				for (Factor factor : copyFactors) {
					if (1 == factor.numScopes()) {
						thisFactor = factor;
						break;
					}
				}
				copyFactors.remove(thisFactor);

				// remove edge from remaining factor
				Variable nodeVariable = thisFactor.getNodeVariable();
				for (Factor factor : copyFactors) {
					if (factor.variables.contains(nodeVariable)
							&& nodeVariable != factor.getNodeVariable()) {
						factor.variables.remove(nodeVariable);
					}
				}
				topOrder.add(nodeVariable);
			}
		} else { // markov network
			while(!copyVariables.isEmpty()) {
				// find variable then remove it
				Factor thisFactor = null;
				for (Iterator<Factor> iterator = copyFactors.iterator(); iterator
						.hasNext();) {
					Factor factor = (Factor) iterator.next();
					if(1 == factor.numScopes()) {
						iterator.remove();
						thisFactor = factor;
						break;
					}
				}
				
				Variable nodeVariable = thisFactor.getNodeVariable();
				copyVariables.remove(nodeVariable);
				for (Iterator<Factor> iterator = copyFactors.iterator(); iterator
						.hasNext();) {
					Factor factor = (Factor) iterator.next();
					if (factor.variables.contains(nodeVariable)) {
						 iterator.remove();
					}
				}
				topOrder.add(nodeVariable);
			}
		}

		return topOrder;
	}

	public ArrayList<Integer> computeSoftOrder() {
		// number of non-evidence variables
		int nne = 0;
		ArrayList<Boolean> processed = new ArrayList<>(nonEvidenceVars.size());
		for (int i = 0; i < nonEvidenceVars.size(); i++) {
			processed.add(new Boolean(false));
		}

		for (int i = 0; i < nonEvidenceVars.size(); i++) {
			if (nonEvidenceVars.get(i).isEvidence) {
				processed.set(i, true);
			} else {
				nne++;
			}
		}

		ArrayList<Integer> order = new ArrayList<Integer>(nne);
		// ArrayList<Set<Integer>> clusters = new ArrayList<Set<Integer>>(nne);
		ArrayList<Set<Integer>> graph = new ArrayList<>(nonEvidenceVars.size());

		for (int i = 0; i < nonEvidenceVars.size(); i++) {
			graph.add(new HashSet<Integer>());
		}

		for (int i = 0; i < remainFactors.size(); i++) {
			// Ignore the evidence variables
			for (int j = 0; j < remainFactors.get(i).variables.size(); j++) {
				int a = remainFactors.get(i).variables.get(j).index;
				if (nonEvidenceVars.get(a).isEvidence) {
					continue;
				}
				for (int k = j + 1; k < remainFactors.get(i).variables.size(); k++) {
					int b = remainFactors.get(i).variables.get(k).index;
					if (nonEvidenceVars.get(b).isEvidence) {
						continue;
					}
					if (a == b) {
						continue;
					}
					graph.get(a).add(b);
					graph.get(b).add(a);
				}
			}
		}

		// int tmp = 0;
		// for (Set<Integer> set : graph) {
		// System.out.println((tmp++) + ": " + set.size());
		// }

		for (int i = 0; i < nne; i++) {
			order.add(-1);
		}

		/*
		 * for (int i = 0; i < nne; i++) { clusters.add(new HashSet<Integer>());
		 * }
		 */

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
			// System.out.println(i + ": " + order.get(i) + " " +
			// nonEvidenceVars.get(order.get(i)).index + " " +
			// graph.get(i).size());
			// Connect the neighbors of order[i] to each other
			int var = order.get(i);
			processed.set(var, true);
			for (Integer a : graph.get(var)) {
				for (Integer b : graph.get(var)) {
					if (a == b)
						continue;
					graph.get(a).add(b);
					graph.get(b).add(a); // issue
				}
			}

			/*
			 * clusters.set(i, graph.get(var)); if (clusters.get(i).size() >
			 * max_cluster_size) { max_cluster_size = clusters.get(i).size(); }
			 */
			// Remove var from the graph
			for (Integer a : graph.get(var)) {
				graph.get(a).remove(var);
			}
			graph.get(var).clear();
		}

		softOrder = order;
		return order;
	}

	public ArrayList<ArrayList<Factor>> generateSoftClusters(
			ArrayList<Integer> softOrders) {

		Eliminator.setFactorCount(remainFactors.size());
		LinkedList<Factor> copyRemainFactors = new LinkedList<>(remainFactors);

		ArrayList<ArrayList<Factor>> clusters = new ArrayList<ArrayList<Factor>>(
				softOrders.size());

		for (Integer i : softOrders) {
			// LinkedList<Factor> mentions = var.getFactorsMentionThis();
			Variable var = nonEvidenceVars.get(i);
			if (true == var.isEvidence) {
				continue;
			}

			LinkedList<Factor> mentions = new LinkedList<>();
			// very important

			Iterator<Factor> remainIter = copyRemainFactors.iterator();

			while (remainIter.hasNext()) {
				Factor nextFactor = remainIter.next();
				if (nextFactor.isAllAssigned()) {
					reservedResult *= nextFactor.getTabelValue(nextFactor
							.underlyVariableToTableIndex());
					remainIter.remove();
					continue;
				}

				if (nextFactor.inScope(var)) {
					mentions.add(nextFactor);
					remainIter.remove();
				}
			}

			ArrayList<Factor> cluster = new ArrayList<>(mentions);
			clusters.add(cluster);
		}

		return clusters;
	}

	// private double baseResult = 1.0;

	@SuppressWarnings("unused")
	private double initBaseResult() {
		double baseResult = reservedResult;

		for (Factor factor : factors) {
			if (factor.isAllAssigned()) {
				baseResult *= factor.getTabelValue(factor
						.underlyVariableToTableIndex());
			}
		}

		return baseResult;
	}

	public double softBucketElimination() {
		return softBucketElimination(softOrder, softClusters);
	}

	public double softBucketElimination(ArrayList<Integer> softOrder,
			ArrayList<ArrayList<Factor>> arrClusters) {

		double result = reservedResult;
		emptyFactorCount = 0;
		probe = 0;

		LinkedList<ArrayList<Factor>> clusters = new LinkedList<>();
		// need deep copy
		for (ArrayList<Factor> oneCluster : arrClusters) {
			clusters.add(new ArrayList<>(oneCluster));
		}

		LinkedList<Integer> orderedVariables = new LinkedList<>(softOrder);

		// bucket order
		int prev = 0;
		while (!orderedVariables.isEmpty()) {
			ArrayList<Factor> cluster = clusters.poll();
			int orderIndex = orderedVariables.poll();
			Variable var = nonEvidenceVars.get(orderIndex);
			// System.out.println("order index = " + orderIndex);
			// System.out.println("var index = " + var.index);

			// if var is soft evidence, then deliver all the factor in this
			// bucket to
			// other non-evidence bucket.
			// if(var.isEvidence) {
			// for (Factor factor : cluster) {
			//
			// }
			//
			// continue;
			// }

			int naaf = 0;
			for (Factor factor : cluster) {

				if (!factor.isAllAssigned()) {
					naaf++;
				}

				if (!factor.inScope(var)) {
					System.out.println("Really strange first");
					System.out.println(var);
					System.out.println(factor.variables);
				}
			}

			ArrayList<Factor> mentions = new ArrayList<>(naaf);
			for (Factor factor : cluster) {
				if (factor.table == null) {
					System.out.println("Ohhhhh");
				}

				if (!factor.isAllAssigned()) {
					mentions.add(factor);
				} else {
					// System.out.println("Little fish");
					// System.out.println(result);
					result *= factor.underlyProbability();
					if (result == Double.POSITIVE_INFINITY) {
						System.out.println("Infinity Result first");
						System.exit(0);
					}
					if (result == 0.0) {
						System.out.println(orderIndex);
						System.out.println("first");
					}
					probe++;
				}
			}

			if (mentions.size() == 0) {
				continue;
			}

			Factor newFactor = Eliminator.Product(mentions);
			newFactor = Eliminator.SumOut(newFactor, var);

			if (newFactor.numScopes() > prev) {
				// System.out.println("prev = " + prev + " now = "
				// + newFactor.numScopes());
			}

			if (newFactor.inScope(var)) {
				System.out.println("Really strange second");
			}

			if ((0 == newFactor.numScopes())) {
				result *= newFactor.getTabelValue(0);
				if (result == Double.POSITIVE_INFINITY) {
					System.out.println("Infinity second");
					System.exit(0);
				}
				emptyFactorCount++;
				if (result == 0.0) {
					System.out.println("second");
				}
				// System.out.println("Only factor value = "
				// + newFactor.getTabelValue(0));
				continue;
			}

			if (newFactor.isAllAssigned()) {
				result *= newFactor.getTabelValue(newFactor
						.underlyVariableToTableIndex());
				if (result == Double.POSITIVE_INFINITY) {
					System.out.println("Infinity Result third");
					System.exit(0);
				}
				if (result == 0.0) {
					System.out.println("third");
				}
				// System.out.println(result);
				continue;
			}

			boolean putInNewBucket = false;
			Iterator<Integer> carryVariableInt = orderedVariables.iterator();
			Iterator<ArrayList<Factor>> carryCluster = clusters.iterator();
			while (carryVariableInt.hasNext()) {
				ArrayList<Factor> nextCluster = carryCluster.next();
				if (newFactor.inScope(nonEvidenceVars.get(carryVariableInt
						.next()))) {
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
		// System.out.println("Empty Factor Count = " + emptyFactorCount);
		// System.out.println("probe = " + probe);
		if (result == Double.POSITIVE_INFINITY) {
			System.out.println("Infinity Result");
			System.exit(0);
		}
		return result;
	}

	public double probabilityEvidence() {
		double result = reservedResult;

		for (Factor factor : factors) {
			result *= factor.underlyProbability();
		}

		return result;
	}

	public double runSoftProcess() {
		ArrayList<Integer> order = computeSoftOrder();
		ArrayList<ArrayList<Factor>> clusters = generateSoftClusters(order);
		return softBucketElimination(order, clusters);
	}

	public static void usage() {
		System.out.println("java  GraphicalModel " + "FILENAME");
	}

	public static void runSoftElimination(String[] args) {
		// TODO Auto-generated method stub

	}

	public void initEmptyFactor() {
		factors = new ArrayList<>(variables.size());

		for (int i = 0; i < variables.size(); i++) {
			Factor factor = new Factor();
			factor.index = i;
			factor.variables.add(variables.get(i)); // node variable
			factors.add(factor);
		}
	}

	public static void main(String[] args) {
		if (1 != args.length) {
			usage();
			System.exit(0);
		}

		String fileName = args[0];

		try {
			PrintStream writer = new PrintStream(fileName + ".output");
			GraphicalModel model = new GraphicalModel(fileName);
			writer.println("Network data loading completed: "
					+ model.variables.size() + " variables, "
					+ model.factors.size() + " factors");
			writer.println(model.network + " network");
			model.readSoftEvidence(fileName + ".evid");
			writer.println("Evidence loaded, and variables instantiation completed. "
					+ model.evidenceCount + " evidence");
			ArrayList<Integer> order = model.computeSoftOrder();
			ArrayList<ArrayList<Factor>> clusters = model
					.generateSoftClusters(order);
			writer.println("Ordering computed");
			writer.println("Order:");
			for (Integer var : order) {
				writer.print(var + ", ");
			}
			writer.println("");

			double result = model.softBucketElimination(order, clusters);
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

	// return the iteration of completion
	@Override
	public Iterator<int[]> iterator() {
		// TODO Auto-generated method stub
		return null;
	}

	public class CompletionIterator implements Iterator<int[]> {

		int[] values;
		int[] domainSizes;

		int pointer = -1;

		public CompletionIterator(ArrayList<Variable> vars) {
			values = new int[vars.size()];
			domainSizes = new int[vars.size()];
			for (int i = 0; i < vars.size(); i++) {
				Variable var = vars.get(i);

				if (!var.isEvidence) {
					values[i] = -1;
				} else {
					values[i] = var.value;
				}
				domainSizes[i] = var.domainSize();
			}
		}

		@Override
		public boolean hasNext() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public int[] next() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void remove() {
			// TODO Auto-generated method stub

		}

	}
}
