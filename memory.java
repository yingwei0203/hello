package findJavaMemory;

public class memory {
	public static void main(String[] args) {
		System.out.println("Total Memory:" + Runtime.getRuntime().totalMemory());
		System.out.println("Free Memory:" + Runtime.getRuntime().freeMemory());
	}
}
