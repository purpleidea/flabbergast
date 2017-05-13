package flabbergast.kubernetes;

import flabbergast.export.NativeBinding;
import flabbergast.lang.AnyConverter;
import flabbergast.lang.Definition;
import flabbergast.lang.SpecialLocation;
import flabbergast.util.WhinyFunction;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.FilterWatchListDeletable;
import java.util.function.BiFunction;

abstract class LabelSelection {
  static final Definition ALL =
      NativeBinding.function(
          NativeBinding.asProxy(),
          args ->
              new LabelSelection() {
                @Override
                <T, L> FilterWatchListDeletable<T, L, Boolean, Watch, Watcher<T>> applySelection(
                    FilterWatchListDeletable<T, L, Boolean, Watch, Watcher<T>> input) {
                  var output = input;
                  for (final var selection : args.values()) {
                    output = selection.applySelection(input);
                  }
                  return output;
                }
              },
          AnyConverter.frameOf(
              AnyConverter.asProxy(
                  LabelSelection.class,
                  false,
                  SpecialLocation.library("cnfc", "kubernetes")
                      .attributes("label_selector")
                      .any()
                      .instantiated()),
              false),
          "args");
  static final Definition IN_DEFINITION =
      make(
          (key, values) ->
              new LabelSelection() {
                @Override
                <T, L> FilterWatchListDeletable<T, L, Boolean, Watch, Watcher<T>> applySelection(
                    FilterWatchListDeletable<T, L, Boolean, Watch, Watcher<T>> input) {
                  return input.withLabelIn(key, values);
                }
              });
  static final Definition NOT_IN_DEFINITION =
      make(
          (key, values) ->
              new LabelSelection() {
                @Override
                <T, L> FilterWatchListDeletable<T, L, Boolean, Watch, Watcher<T>> applySelection(
                    FilterWatchListDeletable<T, L, Boolean, Watch, Watcher<T>> input) {
                  return input.withLabelNotIn(key, values);
                }
              });
  static final Definition WITHOUT_DEFINITION =
      make(
          key ->
              new LabelSelection() {
                @Override
                <T, L> FilterWatchListDeletable<T, L, Boolean, Watch, Watcher<T>> applySelection(
                    FilterWatchListDeletable<T, L, Boolean, Watch, Watcher<T>> input) {
                  return input.withoutLabel(key);
                }
              });
  static final Definition WITH_DEFINITION =
      make(
          key ->
              new LabelSelection() {
                @Override
                <T, L> FilterWatchListDeletable<T, L, Boolean, Watch, Watcher<T>> applySelection(
                    FilterWatchListDeletable<T, L, Boolean, Watch, Watcher<T>> input) {
                  return input.withLabel(key);
                }
              });

  static Definition make(WhinyFunction<String, LabelSelection> ctor) {
    return NativeBinding.function(
        NativeBinding.asProxy(), ctor, AnyConverter.asString(false), "key");
  }

  static Definition make(BiFunction<String, String[], LabelSelection> ctor) {
    return NativeBinding.function(
        NativeBinding.asProxy(),
        (key, values) -> ctor.apply(key, values.values().stream().toArray(String[]::new)),
        AnyConverter.asString(false),
        "key",
        AnyConverter.frameOf(AnyConverter.asString(false), false),
        "args");
  }

  abstract <T, L> FilterWatchListDeletable<T, L, Boolean, Watch, Watcher<T>> applySelection(
      FilterWatchListDeletable<T, L, Boolean, Watch, Watcher<T>> input);
}
