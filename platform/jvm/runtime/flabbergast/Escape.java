package flabbergast;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;
import java.util.List;
import java.util.ArrayList;
import java.lang.Comparable;
import java.util.concurrent.atomic.AtomicInteger;

public class Escape extends Computation {
	private interface ConsumeChar {
		void invoke(int c);
	}
	private interface ConsumeString {
		void invoke(String str);
	}

	private class Range implements Comparable<Range> {
		int start;
		int end;
		String replacement;
		public Range(int start, int end, String replacement) {
			this.start = start;
			this.end = end;
			this.replacement = replacement;
		}
		public int compareTo(Range r) {
			return start - r.start;
		}
	}

	private Map<Integer, String> single_substitutions = new TreeMap<Integer, String>();
	private List<Range> ranges = new ArrayList<Range>();
	private String[] input;
	private TaskMaster master;
	private SourceReference source_ref;
	private Context context;
	private Frame self;
	private AtomicInteger interlock = new AtomicInteger(3);
	private boolean state = false;

	public Escape(TaskMaster master, SourceReference source_ref,
			Context context, Frame self, Frame container) {
		this.master = master;
		this.source_ref = source_ref;
		this.context = context;
		this.self = self;
	}

	private class HandleArgs implements ConsumeResult {
		@Override
		public void consume(Object result) {
			if (result instanceof Frame) {
				Frame input = (Frame) result;
				interlock.addAndGet(input.count());
				Escape.this.input = new String[input.count()];
				int index = 0;
				for (String name : input) {
					final int target_index = index++;
					input.getOrSubscribe(name, new ConsumeResult() {
						@Override
						public void consume(Object arg) {
							if (arg instanceof Stringish) {
								Escape.this.input[target_index] = arg
										.toString();
								if (interlock.decrementAndGet() == 0) {
									master.slot(Escape.this);
								}
							} else {
								master.reportOtherError(
										source_ref,
										String.format(
												"Expected “args” to contain strings. Got %s instead.",
												arg.getClass()));
							}
						}
					});
				}
				if (interlock.decrementAndGet() == 0) {
					master.slot(Escape.this);
				}
			} else {
				master.reportOtherError(source_ref, String.format(
						"Expected “args” to be a frame. Got %s instead.",
						Stringish.hideImplementation(result.getClass())));
			}
		}
	}

	void handleRange(final Frame spec) {
		lookupChar(spec, "start", new ConsumeChar() {
			@Override
			public void invoke(final int start) {
				lookupChar(spec, "end", new ConsumeChar() {
					@Override
					public void invoke(final int end) {
						lookupString(spec, "format_str", new ConsumeString() {
							@Override
							public void invoke(final String replacement) {
								ranges.add(new Range(start, end, replacement));
								if (interlock.decrementAndGet() == 0) {
									master.slot(Escape.this);
								}
							}
						});
					}
				});
			}
		});
	}

	void handleSubstition(final Frame spec) {
		lookupChar(spec, "char", new ConsumeChar() {
			@Override
			public void invoke(final int c) {
				lookupString(spec, "replacement", new ConsumeString() {
					@Override
					public void invoke(final String replacement) {
						single_substitutions.put(c, replacement);
						if (interlock.decrementAndGet() == 0) {
							master.slot(Escape.this);
						}
					}
				});
			}
		});
	}

	private class HandleTransformation implements ConsumeResult {
		@Override
		public void consume(Object result) {
			if (result instanceof Frame) {
				final Frame frame = (Frame) result;
				Lookup lookup = new Lookup(master, source_ref,
						new String[]{"type"}, Context.prepend(frame, null));
				lookup.listen(new ConsumeResult() {
					@Override
					public void consume(Object type_result) {
						if (type_result instanceof Long) {
							long type = (Long) type_result;
							switch ((int) type) {
								case 0 :
									handleSubstition(frame);
									return;
								case 1 :
									handleRange(frame);
									return;
							}
						}
						master.reportOtherError(source_ref,
								"Illegal transformation specified.");
					}
				});
				master.slot(lookup);
			}
		}
	}
	private class HandleTransformations implements ConsumeResult {
		@Override
		public void consume(Object result) {
			if (result instanceof Frame) {
				Frame input = (Frame) result;
				interlock.addAndGet(input.count());
				for (String name : input) {
					input.getOrSubscribe(name, new HandleTransformation());
				}
				if (interlock.decrementAndGet() == 0) {
					master.slot(Escape.this);
				}
			} else {
				master.reportOtherError(
						source_ref,
						String.format(
								"Expected “transformations” to be a frame. Got %s instead.",
								Stringish.hideImplementation(result.getClass())));
			}
		}
	}

	void lookupChar(Frame frame, final String name, final ConsumeChar consume) {
		lookupString(frame, name, new ConsumeString() {
			@Override
			public void invoke(String str) {
				if (str.offsetByCodePoints(0, 1) == str.length()) {
					consume.invoke(str.codePointAt(0));
				} else {
					master.reportOtherError(source_ref, String.format(
							"String “%s” for “%s” must be a single codepoint.",
							str, name));
				}
			}
		});
	}

	void lookupString(Frame frame, final String name,
			final ConsumeString consume) {
		Lookup lookup = new Lookup(master, source_ref, new String[]{name},
				Context.prepend(frame, null));
		lookup.listen(new ConsumeResult() {
			@Override
			public void consume(Object result) {
				if (result instanceof Stringish) {
					String str = result.toString();
					consume.invoke(str);
				} else {
					master.reportOtherError(source_ref, String.format(
							"Expected “%s” to be a string. Got %s instead.",
							name, result.getClass()));
				}
			}
		});
		master.slot(lookup);
	}

	@Override
	protected void run() {
		if (!state) {
			Lookup input_lookup = new Lookup(master, source_ref,
					new String[]{"args"}, context);
			input_lookup.listen(new HandleArgs());
			master.slot(input_lookup);
			Lookup transformation_lookup = new Lookup(master, source_ref,
					new String[]{"transformations"}, context);
			transformation_lookup.listen(new HandleTransformations());
			master.slot(transformation_lookup);
			state = true;
			if (interlock.decrementAndGet() > 0) {
				return;
			}
		}
		Frame output_frame = new Frame(master, master.nextId(), source_ref,
				context, self);
		for (int index = 0; index < input.length; index++) {
			StringBuilder buffer = new StringBuilder();
			for (int it = 0; it < input[index].length(); it = input[index]
					.offsetByCodePoints(it, 1)) {
				int c = input[index].codePointAt(it);
				boolean is_surrogate = Character.isSupplementaryCodePoint(c);
				String replacement = single_substitutions.get(c);
				if (replacement != null) {
					buffer.append(replacement);
				} else {
					boolean matched = false;
					for (Range range : ranges) {
						if (c >= range.start && c <= range.end) {
							byte[] utf8 = input[index].substring(it,
									it + (is_surrogate ? 2 : 1)).getBytes(
									StandardCharsets.UTF_8);
							buffer.append(String.format(range.replacement, c,
									(int) input[index].charAt(it), is_surrogate
											? (int) input[index].charAt(it + 1)
											: 0, (int) utf8[0], utf8.length > 1
											? (int) utf8[1]
											: 0, utf8.length > 2
											? (int) utf8[2]
											: 0, utf8.length > 3
											? (int) utf8[3]
											: 0));
							matched = true;
							break;
						}
					}
					if (!matched) {
						buffer.appendCodePoint(c);
					}
				}
			}
			output_frame.set(TaskMaster.ordinalNameStr(index),
					new SimpleStringish(buffer.toString()));
		}
		result = output_frame;
	}
}
