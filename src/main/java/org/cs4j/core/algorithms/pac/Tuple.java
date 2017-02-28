package org.cs4j.core.algorithms.pac;

/**
 * Created by Roni Stern on 28/02/2017.
 * Taken from  https://dzone.com/articles/whats-wrong-java-8-part-v
 */
public class Tuple<T, U> {
    public final T _1;
    public final U _2;
    public Tuple(T arg1, U arg2) {
        super();
        this._1 = arg1;
        this._2 = arg2;
    }
    @Override
    public String toString() {
        return String.format("(%s, %s)", _1, _2);
    }
}
