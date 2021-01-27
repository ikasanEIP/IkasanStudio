package org.ikasan.studio;

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
    public void setX(L left) {
        this.left = left;
    }

    public void setY(R right) {
        this.right = right;
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
        return left;
    }

    public R getY() {
        return right;
    }
}
