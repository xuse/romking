package io.github.xuse.jetui.repository;

import java.util.Optional;
import java.util.stream.Stream;

public interface ListDataProvider<T, F> {
	int count(Optional<F> f);

	Stream<T> list(Optional<F> f, int offset, int limit);
}
