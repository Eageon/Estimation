import java.util.*;


public class Eliminator {
	
	static int factorCount = 0;
	
	ArrayList<Variable> variablesToEliminated;
	
	ArrayList<Factor> factors; // refer to factors
	ArrayList<Variable> variables; // refer to variables
	LinkedList<Variable> order;
	
	ArrayList<Integer>  orderOfIndex;  // to do
	
	class OrderComparator implements Comparator<Variable> {

		@Override
		public int compare(Variable o1, Variable o2) {
			// TODO Auto-generated method stub
			return order.indexOf(o1) - order.indexOf(o2);
		}
	}
	
	
	
	@SuppressWarnings("unused")
	private void sortVaraiblesToElminatedByOrdering() {
		Collections.sort(variablesToEliminated, new OrderComparator());
	}
	
// This is another strategy to implement elimination, I use bucket elimination instead
//	public Factor Sum_Product_VE() {
//		sortVaraiblesToElminatedByOrdering();
//		
//		for (int i = 0; i < variablesToEliminated.size(); i++) {
//			factors = Sum_Product_Eliminate_Var(variablesToEliminated.get(i));
//		}
//		
//		return Product(factors);
//	}

	@SuppressWarnings("unused")
	private ArrayList<Factor> Sum_Product_Eliminate_Var(Variable var) {
		
		LinkedList<Factor> factorPrime = var.factorMentionThis;
		
		ArrayList<Factor> factorPrimePrime = new ArrayList<>();
		
		for(Factor factor : factors) {
			if(!factorPrime.contains(factor)) {
				factorPrimePrime.add(factor);
			}
		}
		
		Factor bigFi = Product(factorPrime);
		Factor tao = SumOut(bigFi, var);
		
		factorPrimePrime.add(tao);
		
		return factorPrimePrime;
	}
	
	public static void setFactorCount(int count) {
		factorCount = count;
	}
	
	public static Factor Product(List<Factor> factorPrime) {
		LinkedList<Variable> set = new LinkedList<>();
		
		for(Factor factor : factorPrime) {
			for(Variable var : factor.variables) {
				if(!set.contains(var))
					set.add(var);
			}
		}
		
		ArrayList<Variable> Z = new ArrayList<>(set.size());
		@SuppressWarnings("unused")
		int numTables = 1;
		for(Variable var : set) {
			Z.add(var);
			numTables *= var.domainSize();
		}
		
		Factor fRet = new Factor(Z);
		fRet.initTable();
		for (int i = 0; i < fRet.table.size(); i++) {
			fRet.table.set(i, 1.0);
		}
		
		
		// for each instantiation
		for (int z = 0; z < fRet.table.size(); z++) {
			//Factor zFactor = factorPrime.get(z);
			int[] zValueIndex = fRet.tableIndexToVaraibleValue(z);
			
			for(Factor xFactor : factorPrime) {
			//for (int i = 0; i < factorPrime.size(); i++) {
				//Factor xFactor = factorPrime.get(i);
				int[] primeValues = new int[xFactor.numScopes()];
				// find the same variable
				for (int j = 0; j < primeValues.length; j++) {
					for (int m = 0; m < zValueIndex.length; m++) {
						if(fRet.variables.get(m) == xFactor.variables.get(j)) {
							primeValues[j] = zValueIndex[m];
							break; // need to improve
						}
					}
				}
				
				int xTableIndex = xFactor.variableValueToTableIndex(primeValues);
				// the multiply two double;
				fRet.table.set(z, fRet.table.get(z) * xFactor.table.get(xTableIndex));
				
			}
		}
		
		System.gc();
		
		if(fRet.numScopes() == 0) {
			System.out.println(fRet.index + " 0");
			for (Factor factor : factorPrime) {
				System.out.print(factor.index + " ");
			}
			System.out.println("");
			System.exit(-1);
		}
		
		return fRet;
	}
	
	/*
	 * Eliminate var in factor
	 */
	public static Factor SumOut(Factor factor, Variable var) {
		
		int numTmp = factor.table.size();
		int num = 0;
		
		ArrayList<Variable> varsAfterElim = new ArrayList<>(factor.numScopes());
		
		for(int i = 0; i < factor.variables.size(); i++) {
			Variable y = factor.variables.get(i);
			numTmp /= y.domainSize();
			
			if(y != var) {
				varsAfterElim.add(y);
			} else {
				num = numTmp;
			}
		}
		
		Factor fRet = new Factor(varsAfterElim);
		fRet.initTable();
		//fRet.tableZeros();
		
		
		int i = 0;
		int count = 0;
		for(int base = 0; base < factor.table.size() 
				&& (base + num * (var.domainSize() - 1)) < factor.table.size()
					;) {
			
			double value = 0.0;
			for(int k = 0; k < var.domainSize(); k++) {
				value += factor.getTabelValue(base + k * num);
			}
			
			fRet.setTableValue(i, value);
			if(fRet.table.get(i) == 2.0) {
				System.out.println("var = " + var.index + ", domain = " + var.domainSize() + ", table0 = " + factor.getTabelValue(0) + 
						", num = " + num + ", base = " + base + 
						", i = " + i + ", " + "value = " + value);
			}
			
			i++;
			count++;
			base++;
			if(count == (num)) {
				base += num;
				count = 0;
			}
//			else {
//				base++;
//			}
		}
		
		fRet.index = factorCount;
		factorCount++;
		return fRet;
	}
	
	public static void main(String[] args) {
		@SuppressWarnings("unused")
		Eliminator eliminator = new Eliminator();
		
		Variable A = new Variable(2);
		Variable B = new Variable(3);
		Variable C = new Variable(2);
		Variable D = new Variable(2);
		
		ArrayList<Variable> variables = new ArrayList<>();
		variables.add(A);
		variables.add(B);
		variables.add(C);
		
		ArrayList<Variable> variables2 = new ArrayList<>();
		//variables2.add(B);
		variables2.add(C);
		variables2.add(D);
		
		Factor factor = new Factor(variables);
		factor.initTable();
		
		Factor factor2 = new Factor(variables2);
		factor2.initTable();
		
		Random rand = new Random();
		
		for (int i = 0; i < factor.table.size(); i++) {
			factor.setTableValue(i, 10 * rand.nextDouble());
		}
		
		for (int i = 0; i < factor2.table.size(); i++) {
			factor2.setTableValue(i, 10 * rand.nextDouble());
		}
		
		System.out.println("Factor");
		factor.printFactor();
		System.out.println("");
		System.out.println("Factor 2");
		factor2.printFactor();
		
		LinkedList<Factor> factorList = new LinkedList<>();
		factorList.add(factor);
		factorList.add(factor2);
		
		//Factor newFactor = eliminator.Product(factorList);
		
//		eliminator.variables = variables;
		Factor newFactor2 = Eliminator.SumOut(factor, B);
		System.out.println("");
		System.out.println("New Factor");
		newFactor2.printFactor();
	}
}
