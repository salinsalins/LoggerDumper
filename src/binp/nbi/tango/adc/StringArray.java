package binp.nbi.tango.adc;

public class StringArray {

	static boolean contains(String[] array, String item) {
		for (String s : array) {
			if (s.equals(item))
				return true;
		}
		return false;
	}

	static boolean matches(String[] array, String item) {
		for (String s : array) {
			if (s.matches(item))
				return true;
		}
		return false;
	}

	static int indexOf(String[] array, String item) {
		if (array == null || array.length <= 0)
			return -1;
		for (int i = 0; i < array.length; i++) {
			if (array[i].equals(item))
				return i;
		}
		return -1;
	}

	static int indexOfMatch(String[] array, String item) {
		if (array == null || array.length <= 0)
			return -1;
		for (int i = 0; i < array.length; i++) {
			if (array[i].matches(item))
				return i;
		}
		return -1;
	}
}
