package de.codecrafter47.taboverlay.bukkit.internal.util;

import lombok.RequiredArgsConstructor;

import java.util.Objects;

@RequiredArgsConstructor
public class UnorderedPair<T> {
    public final T a;
    public final T b;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UnorderedPair<?> that = (UnorderedPair<?>) o;
        return (Objects.equals(a, that.a) &&
                Objects.equals(b, that.b))
                || (Objects.equals(a, that.b) &&
                Objects.equals(b, that.a));
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(a) + Objects.hashCode(b);
    }
}
