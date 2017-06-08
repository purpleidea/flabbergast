package flabbergast;

import java.lang.Math;

public class StandardInterop extends Interop {
    public static final StandardInterop INSTANCE = new StandardInterop();
    private StandardInterop() {
        addMap(Double.class, Double.class, "math/abs", Math::abs);
        addMap(Double.class, Double.class, "math/ceiling", Math::ceil);
        addMap(Double.class, Double.class, "math/floor", Math::floor);
        addMap(Double.class, Double.class, "math/log", (x, base) -> Math.log(x) / Math.log(base), Double.class, "real_base");
        addMap(Double.class, Double.class, "math/power", Math::pow, Double.class, "real_exponent");
        addMap(Double.class, Double.class, "math/round", (x, places) -> {
            double shift = Math.pow(10, places);
            return Math.round(x * shift) / shift;
        }, Long.class, "real_places");
        addMap(Double.class, Double.class, "math/circle/arccos", (x, angle_unit) -> Math.acos(x) / angle_unit, Double.class, "angle_unit");
        addMap(Double.class, Double.class, "math/circle/arcsin", (x, angle_unit) -> Math.asin(x) / angle_unit, Double.class, "angle_unit");
        addMap(Double.class, Double.class, "math/circle/arctan", (x, angle_unit) -> Math.atan(x) / angle_unit, Double.class, "angle_unit");
        addMap(Double.class, Double.class, "math/circle/cos", (x, angle_unit) -> Math.cos(x * angle_unit), Double.class, "angle_unit");
        addMap(Double.class, Double.class, "math/circle/sin", (x, angle_unit) -> Math.sin(x * angle_unit), Double.class, "angle_unit");
        addMap(Double.class, Double.class, "math/circle/tan", (x, angle_unit) -> Math.tan(x * angle_unit), Double.class, "angle_unit");
        addMap(Double.class, Double.class, "math/hyperbola/arccos", (x, angle_unit) -> Math.log(x + Math.sqrt(x * x - 1.0)) / angle_unit, Double.class, "angle_unit");
        addMap(Double.class, Double.class, "math/hyperbola/arcsin", (x, angle_unit) ->	Math.log(x + Math.sqrt(x * x + 1.0)) / angle_unit, Double.class, "angle_unit");
        addMap(Double.class, Double.class, "math/hyperbola/arctan", (x, angle_unit) ->	0.5 * Math.log((1.0 + x) / (1.0 - x)) / angle_unit, Double.class, "angle_unit");
        addMap(Double.class, Double.class, "math/hyperbola/cos", (x, angle_unit) -> Math.cosh(x * angle_unit), Double.class, "angle_unit");
        addMap(Double.class, Double.class, "math/hyperbola/sin", (x, angle_unit) -> Math.sinh(x * angle_unit), Double.class, "angle_unit");
        addMap(Double.class, Double.class, "math/hyperbola/tan", (x, angle_unit) -> Math.tanh(x * angle_unit), Double.class, "angle_unit");
    }
}

