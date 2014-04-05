import java.util.LinkedList;


public class Variable implements Comparable<Variable> {
	int d;
	boolean isEvdence = false;
	int value;
	
	int index = -1;
	
	LinkedList<Variable> neighbors = new LinkedList<>();
	LinkedList<Factor> factorMentionThis = new LinkedList<>();
	
	public Variable(int domainSize) {
		d = domainSize;
		isEvdence = false;
		value = -1;
	}
	
	public int domainSize() {
		return d;
	}
	
	public void setEvidence(int observedValue) {
		isEvdence = true;
		value = observedValue;
		
		LinkedList<Factor> factorMentionThisCopy = new LinkedList<>(factorMentionThis);
		for(Factor factor : factorMentionThisCopy) {
			// instantiate the variable mentions this variable
			factor.instantiateVariable(this, observedValue);
			//this.removeMentionFactor(factor);
		}
		
		factorMentionThis.clear();
		for (Variable var : neighbors) {
			if(var != this) {
				var.removeNeighbor(this);
			}
		}
		neighbors.clear();
	}
	
	public int degree() {
		return neighbors.size();
	}

	@Override
	public int compareTo(Variable o) {
		// TODO Auto-generated method stub
		return this.degree() - o.degree();
	}
	
	public void addNeighbor(Variable neigh) {
		if(!neighbors.contains(neigh))
			neighbors.add(neigh);
	}
	
	public void removeNeighbor(Variable nei) {
		neighbors.remove(nei);
	}
	
	public void destroyVariableNeighborhood() {
		for(Variable nei : neighbors) {		
			nei.removeNeighbor(this);
		}
		
	}
	
	public boolean isAdjacent(Variable var) {
		for(Variable nei : neighbors) {
			if(var == nei)
				return true;
		}
		
		return false;
	}
	
	public void addMentionFactor(Factor factor) {
		if(!factorMentionThis.contains(factor))
			factorMentionThis.add(factor);
	}
	
	public void removeMentionFactor(Factor factor) {
		factorMentionThis.remove(factor);
	}
	
	public LinkedList<Factor> getFactorsMentionThis() {
//		ArrayList<Factor> mention = new ArrayList<>(factorMentionThis.size());
//		
//		for(Factor factor : factorMentionThis) {
//			mention.add(factor);
//		}
//		
//		return mention;
		
		return factorMentionThis;
	}
}
