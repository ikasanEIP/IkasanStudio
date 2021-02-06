package org.ikasan.studio;

/**
 * Encapsulate the logic of a generic pair
 * @param <L> left side
 * @param <R> right side
 */
public class Pair<L,R> {

    private L left;
    private R right;

    public Pair() {}

    public Pair(L left, R right) {
        this.left = left;
        this.right = right;
    }

    public void setLeft(L left) {
        this.left = left;
    }
    public void setX(L x) {
        setLeft(x);
    }

    public void setY(R y) {
        setRight(y);
    }
    public void setRight(R right) {
        this.right = right;
    }

    public L getLeft() {
        return left;
    }

    public R getRight() {
        return right;
    }
    public L getX() {
        return getLeft();
    }

    public R getY() {
        return getRight();
    }
}
