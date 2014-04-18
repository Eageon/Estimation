import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

public class wCutSet {
	public static ArrayList<Variable> generateWCutSet(GraphicalModel model,
			int w) {
		model.computeOrder();

		LinkedList<Variable> X = new LinkedList<>();

		LinkedList<ArrayList<Factor>> factorClusters = model.generateClusters();
		ArrayList<LinkedList<Variable>> variableClusters = new ArrayList<>();

		for (ArrayList<Factor> factorCluster : factorClusters) {
			Set<Variable> varsInClusterSet = new HashSet<>();
			for (Factor factor : factorCluster) {
				for (Variable var : factor.variables) {
					varsInClusterSet.add(var);
				}
			}
			LinkedList<Variable> varsInCluster = new LinkedList<>(
					varsInClusterSet);
			variableClusters.add(varsInCluster);
		}

		boolean needRepeat = true;
		boolean noClusterHasMoreThanWplusOne = true;
		for (LinkedList<Variable> cluster : variableClusters) {
			if (cluster.size() > w + 1) {
				noClusterHasMoreThanWplusOne &= false;
			}
		}

		needRepeat = !noClusterHasMoreThanWplusOne;

		while (needRepeat) {
			Variable varMostInClusters = model.nonEvidenceVars.get(0);
			int mostCount = 0;

			// find the variable appear most in clusters
			for (Variable var : model.nonEvidenceVars) {
				int currCount = 0;
				for (LinkedList<Variable> cluster : variableClusters) {
					boolean hasThisVar = false;
					if (true == cluster.contains(var)) {
						currCount++;
						break;
					}
				}
				if (currCount > mostCount) {
					mostCount = currCount;
					varMostInClusters = var;
				}
			}

			// remove varMostInClusters from all clusters

			noClusterHasMoreThanWplusOne = true;
			for (LinkedList<Variable> cluster : variableClusters) {
				cluster.remove(varMostInClusters);
				if (cluster.size() > w + 1) {
					noClusterHasMoreThanWplusOne &= false;
				}
			}

			X.add(varMostInClusters);
			needRepeat = !noClusterHasMoreThanWplusOne;
		}

		return new ArrayList<>(X);
	}
}
