package flabbergast;

interface Ensurable {

	void ensureType(ErrorCollector collector, TypeSet candidate_parameter_type,
			Ptr<Boolean> success, boolean b);

}
