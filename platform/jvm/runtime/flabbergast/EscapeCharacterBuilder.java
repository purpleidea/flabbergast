package flabbergast;

import flabbergast.EscapeBuilder.Transformation;
import flabbergast.ReflectedFrame.Transform;

import java.util.Collections;
import java.util.Map;

public class EscapeCharacterBuilder extends BaseReflectedInterop<Transformation> {
    private String character;
    private String replacement;
    public EscapeCharacterBuilder(TaskMaster task_master, SourceReference source_reference,
                                  Context context, Frame self, Frame container) {
        super(task_master, source_reference, context, self, container);
    }

    @Override
    protected  Map<String, Transform<Transformation>> getAccessors() {
        return Collections.emptyMap();
    }
    @Override
    protected Transformation computeResult() throws Exception {
        int codepoint = EscapeBuilder.stringToCodepoint(character);
        return (builder) -> builder.single_substitutions.put(codepoint, replacement);
    }

    @Override
    protected void setup() {
        Sink<String> char_lookup = find(String.class, x -> character = x);
        char_lookup.allowDefault(false, null);
        char_lookup.lookup("char");
        Sink<String> replacement_lookup = find(String.class, x -> replacement = x);
        replacement_lookup.allowDefault(false, null);
        replacement_lookup.lookup("replacement");
    }
}
