import java.util.ArrayList;

/**
 * The implementation of Jun Yu specified Min Heap, which allow user
 * to modify the fields of element of heap. The algorithm keep 
 * adjusting the structure of heap so that it won't violate the
 * property of min heap.
 */

/**
 * @author Jun Yu
 *
 */
public class VariableHeap {
	private ArrayList<Variable> variables;
	private int[] Q;
	private int[] X;
	private int heapSize = 0;

	public VariableHeap(ArrayList<Variable> variablesRef) {
		int size = variablesRef.size();
		variables = variablesRef;
		Q = new int[size + 1];
		X = new int[size];
	}

	public void buildHeap() {
		heapSize = variables.size();

		for (int i = 0; i < variables.size(); i++) {
			Q[i + 1] = i;
			X[i] = i + 1;
		}

		for (int i = heapSize / 2; i > 0; i--) {
			percolateDown(i);
		}
	}

	private void exchange(int i, int j) {
		int tmp = Q[i];
		Q[i] = Q[j];
		Q[j] = tmp;
		X[Q[i]] = i;
		X[Q[j]] = j;
	}

	private void percolateDown(int hole) {
		int child;

		for (; hole * 2 <= heapSize; hole = child) {
			child = hole * 2;
			if (child != heapSize
					&& variables.get(Q[child + 1]).compareTo(variables.get(Q[child])) < 0)
				child++;
			if (variables.get(Q[child]).compareTo(variables.get(Q[hole])) < 0)
				exchange(hole, child);
			else
				break;
		}
	}

	public int deleteMin() {
		int minItem = Q[1];
		Q[1] = Q[heapSize--];
		percolateDown(1);

		return minItem;
	}

	// public int insert(Vertex newVertex) {
	// int hole = ++heapSize;
	// array[hole] = newVertex;
	// hole = percolateUp(hole);
	// return hole;
	// }
	//
	private int percolateUp(int hole) {
		for (Q[0] = Q[hole]; variables.get(Q[hole]).compareTo(variables.get(Q[hole / 2])) < 0; hole /= 2) {
			exchange(hole, hole / 2);
		}
		return hole;
	}

	public boolean isEmpty() {
		return (heapSize == 0) ? true : false;
	}
	
	/**
	 * 
	 * @param index of factor in raw array.
	 * @param newDeg
	 */
	public void adjustHeap(int index, boolean isDecreaseDegree) {
		int a = X[index];

		if (isDecreaseDegree)
			percolateUp(a);
		else
			percolateDown(a);
	}
	
	public void printHeap() {
		for(Integer index : Q) {
			System.out.print(index + ", ");
		}
		System.out.println("");
	}
}
