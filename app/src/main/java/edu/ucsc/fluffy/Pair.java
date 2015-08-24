package edu.ucsc.fluffy;

import java.io.Serializable;
//import libcore.util.Objects;


/**
 * Created by mrg on 8/20/15.
 */
public class Pair<S, T> implements Serializable {

    public final S first;
    public final T second;


    public static <S, T> Pair<S, T> create (S a, T b) {
        return new Pair<S,T>(a,b);
    }

    @Override
    public int hashCode() {
        return (first == null ? 0 : first.hashCode()) ^ (second == null ? 0 : second.hashCode());
    }

  /*  @Override
    public boolean equals(Object o) {
        if (!(o instanceof Pair)) {
            return false;
        }
        Pair<?, ?> p = (Pair<?, ?>) o;
        return Objects.equal(p.first, first) && Objects.equal(p.second, second);
    }*/

    public Pair(S first, T second) {
        this.first = first;
        this.second = second;
    }

    public S getFirst() {
        return first;
    }


    public T getSecond() {
        return second;
    }

}
