package edu.udel.cis.vsl.gmc;

public class FalsePredicate<S> implements StatePredicateIF<S> {

	public boolean holdsAt(S state) {
		return false;
	}

	public String explanation() {
		return "The false predicate is always false.";
	}

	public String toString() {
		return "FalsePredicate";
	}

}
