package flabbergast.lang;

import java.util.function.Predicate;
import java.util.stream.Stream;

/** A provider of KWS “external” functions to be made available to KWS programs */
public interface KwsService {
  /**
   * Find all functions
   *
   * @param flags the flags request to decide what functions should be made available
   */
  Stream<KwsBinding> discover(Predicate<ServiceFlag> flags);
}
