package flabbergast;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

abstract class AstTypeableNode extends AstNode {
    static TypeSet checkReflectedMethod(ErrorCollector collector,
                                        AstNode where, List<Method> methods,
                                        List<? extends TypeableElement> arguments, TypeSet return_type,
                                        Ptr<Boolean> success) {
        /* If there are no candidate methods, don't bother checking the types. */
        if (methods.size() == 0) {
            return TypeSet.EMPTY;
        }
        /* Find all the methods that match the needed type. */
        List<Method> candidate_methods = new ArrayList<Method>();
        TypeSet candiate_return = TypeSet.EMPTY;
        TypeSet total_union = TypeSet.EMPTY;
        for (Method method : methods) {
            Type type = Type.fromNative(method.getReturnType());
            total_union = total_union.union(type);
            if (return_type.contains(type)) {
                candidate_methods.add(method);
                candiate_return = candiate_return.union(type);
            }
        }
        /* Produce an error for the union of all the types. */
        if (candidate_methods.size() == 0) {
            collector
            .reportExpressionTypeError(where, return_type, total_union);
            return TypeSet.EMPTY;
        }
        /*
         * Check that the arguments match the union of the parameters of all the
         * methods. This means that we might still not have a valid method, but
         * we can check again during codegen.
         */
        for (int it = 0; it < arguments.size(); it++) {
            TypeSet candidate_parameter_type = TypeSet.EMPTY;
            for (Method method : methods) {
                Class<?> param_type = Modifier.isStatic(method.getModifiers())
                                      ? method.getParameterTypes() [it]
                                      : (it == 0 ? method.getDeclaringClass() : method
                                         .getParameterTypes() [it - 1]);
                candidate_parameter_type = candidate_parameter_type.union(Type
                                           .fromNative(param_type));
            }
            arguments.get(it).ensureType(collector, candidate_parameter_type,
                                         success, true);
        }
        return candiate_return;
    }

    static Class<?> getReflectedType(ErrorCollector collector, AstNode where,
                                     String type_name, Ptr<Boolean> success) {
        try {
            return Class.forName(type_name);
        } catch (ClassNotFoundException e) {
            success.set(false);
            collector.reportRawError(where, "No such type " + type_name
                                     + " found.");
            return null;
        }
    }
    static Field reflectField(ErrorCollector collector, AstNode where,
                              String type_name, String field_name, Ptr<Boolean> success) {
        Class<?> reflected_type = getReflectedType(collector, where, type_name,
                                  success);
        if (reflected_type == null) {
            return null;
        }
        try {
            Field field = reflected_type.getField(field_name);
            if (!Modifier.isStatic(field.getModifiers())) {
                success.set(false);
                collector.reportRawError(where, "The field " + field_name
                                         + " in  type " + type_name + " has is not static.");
                return null;
            }
            return field;
        } catch (NoSuchFieldException e) {
            success.set(false);
            collector.reportRawError(where, "The type " + type_name
                                     + " has no field named " + field_name);
            return null;
        } catch (SecurityException e) {
            success.set(false);
            collector.reportRawError(where, "The field " + field_name
                                     + " in  type " + type_name + " is not public.");
            return null;
        }

    }

    static void reflectMethod(ErrorCollector collector, AstNode where,
                              String type_name, String method_name, int arity,
                              List<Method> methods, Ptr<Boolean> success) {
        Class<?> reflected_type = getReflectedType(collector, where, type_name,
                                  success);
        if (reflected_type == null) {
            return;
        }

        for (Method method : reflected_type.getMethods()) {
            int adjusted_arity = method.getParameterTypes().length
                                 + (Modifier.isStatic(method.getModifiers()) ? 0 : 1);
            if (method.getName().equals(method_name) && adjusted_arity == arity) {
                methods.add(method);
            }
        }
        if (methods.size() == 0) {
            success.set(false);
            collector.reportRawError(where, "The type " + type_name
                                     + " has no public method named " + method_name
                                     + " which takes " + arity + " parameters.");
        }

    }

    protected Environment environment;
    protected TypeSet inferred_type;

    public boolean analyse(ErrorCollector collector) {
        Environment environment = new Environment(getFileName(), getStartRow(),
                getStartColumn(), getEndRow(), getEndColumn(), null, false,
                false);
        List<AstTypeableNode> queue = new ArrayList<AstTypeableNode>();
        Ptr<Boolean> success = new Ptr<Boolean> (true);
        propagateEnvironment(collector, queue, environment, success);
        SortedMap<Integer, Set<AstTypeableNode>> sorted_nodes = new TreeMap<Integer, Set<AstTypeableNode>>();

        for (AstTypeableNode element : queue) {
            if (!sorted_nodes.containsKey(element.getEnvironmentPriority())) {
                sorted_nodes.put(element.getEnvironmentPriority(),
                                 new HashSet<AstTypeableNode>());
            }
            sorted_nodes.get(element.getEnvironmentPriority()).add(element);
        }
        for (Set<AstTypeableNode> items : sorted_nodes.values()) {
            for (AstTypeableNode element : items) {
                element.makeTypeDemands(collector, success);
            }
        }
        return success.get();
    }

    public int getEnvironmentPriority() {
        return environment.getPriority();
    }

    public TypeSet getInferredType() {
        if (inferred_type == null) {
            return TypeSet.ALL;
        }
        return inferred_type;
    }

    abstract void makeTypeDemands(ErrorCollector collector, Ptr<Boolean> success);

    abstract Environment propagateEnvironment(ErrorCollector collector,
            List<AstTypeableNode> queue, Environment environment,
            Ptr<Boolean> success);

}
