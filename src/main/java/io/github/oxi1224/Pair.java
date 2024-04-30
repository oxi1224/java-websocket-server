package io.github.oxi1224;

public class Pair<K, V> {
  private K key;
  private V val;

  public Pair(K key, V val) {
    this.key = key;
    this.val = val;
  }

  public void setKey(K key) {
    this.key = key;
  }

  public void setValue(V val) {
    this.val = val;
  }

  public K getKey() {
    return this.key;
  }

  public V getValue() {
    return this.val;
  }
}
