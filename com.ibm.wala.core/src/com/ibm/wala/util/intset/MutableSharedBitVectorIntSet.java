/*******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.util.intset;

import com.ibm.wala.util.collections.CompoundIntIterator;
import com.ibm.wala.util.collections.EmptyIntIterator;
import com.ibm.wala.util.debug.Assertions;

/**
 * 
 * The shared bit vector implementation described by [Heintze 1999] TODO: much
 * optimization possible.
 * 
 * @author sfink
 */
public class MutableSharedBitVectorIntSet implements MutableIntSet {

  private final static boolean DEBUG = false;

  private final static boolean PARANOID = false;

  private final static int OVERFLOW = 20;

  private MutableSparseIntSet privatePart;

  private BitVectorIntSet sharedPart;

  /**
   * 
   */
  public MutableSharedBitVectorIntSet() {
  }

  /**
   * @param set
   * @throws IllegalArgumentException  if set is null
   */
  public MutableSharedBitVectorIntSet(MutableSharedBitVectorIntSet set) {
    if (set == null) {
      throw new IllegalArgumentException("set is null");
    }
    if (set.privatePart != null) {
      this.privatePart = new MutableSparseIntSet(set.privatePart);
    }
    this.sharedPart = set.sharedPart;
  }

  /**
   * @param s
   * @throws IllegalArgumentException  if s is null
   */
  public MutableSharedBitVectorIntSet(SparseIntSet s) {
    if (s == null) {
      throw new IllegalArgumentException("s is null");
    }
    if (s.size() == 0) {
      return;
    }
    this.privatePart = new MutableSparseIntSet(s);
    checkOverflow();

    if (PARANOID) {
      checkIntegrity();
    }
  }

  /**
   * @param s
   */
  public MutableSharedBitVectorIntSet(BitVectorIntSet s) {
    copyValue(s);

    if (PARANOID) {
      checkIntegrity();
    }
  }

  /**
   * @param s
   */
  private void copyValue(BitVectorIntSet s) {
    if (s.size() == 0) {
      sharedPart = null;
      privatePart = null;
    } else if (s.size() < OVERFLOW) {
      sharedPart = null;
      privatePart = new MutableSparseIntSet(s);
    } else {
      sharedPart = BitVectorRepository.findOrCreateSharedSubset(s);
      if (sharedPart.size() == s.size()) {
        privatePart = null;
      } else {
        BitVectorIntSet temp = new BitVectorIntSet(s);
        temp.removeAll(sharedPart);
        if (!temp.isEmpty()) {
          privatePart = new MutableSparseIntSet(temp);
        } else {
          privatePart = null;
        }
      }
    }
    if (PARANOID) {
      checkIntegrity();
    }
  }

  /**
   * 
   */
  private void checkIntegrity() {
    Assertions._assert(privatePart == null || !privatePart.isEmpty());
    Assertions._assert(sharedPart == null || !sharedPart.isEmpty());
    if (privatePart != null && sharedPart != null) {
      Assertions._assert(privatePart.intersection(sharedPart).isEmpty());
    }
  }

  /**
   * 
   */
  private void checkOverflow() {

    if (PARANOID) {
      checkIntegrity();
    }
    if (privatePart != null && privatePart.size() > OVERFLOW) {
      if (sharedPart == null) {
        BitVectorIntSet temp = new BitVectorIntSet(privatePart);
        sharedPart = BitVectorRepository.findOrCreateSharedSubset(temp);
        temp.removeAll(sharedPart);
        if (!temp.isEmpty())
          privatePart = new MutableSparseIntSet(temp);
        else
          privatePart = null;
      } else {
        BitVectorIntSet temp = new BitVectorIntSet(sharedPart);
        // when we call findOrCreateSharedSubset, we will ask size() on temp.
        // so use addAll instead of addAllOblivious: which incrementally
        // updates the population count.
        temp.addAll(privatePart);
        sharedPart = BitVectorRepository.findOrCreateSharedSubset(temp);
        temp.removeAll(sharedPart);
        if (!temp.isEmpty())
          privatePart = new MutableSparseIntSet(temp);
        else
          privatePart = null;
      }
    }
    if (PARANOID) {
      checkIntegrity();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.intset.IntSet#contains(int)
   */
  public boolean contains(int i) {
    if (privatePart != null && privatePart.contains(i)) {
      return true;
    }
    if (sharedPart != null && sharedPart.contains(i)) {
      return true;
    }
    return false;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.intset.IntSet#intersection(com.ibm.wala.util.intset.IntSet)
   */
  public IntSet intersection(IntSet that) {
    if (that instanceof MutableSharedBitVectorIntSet) {
      return intersection((MutableSharedBitVectorIntSet) that);
    } else if (that instanceof BitVectorIntSet) {
      MutableSharedBitVectorIntSet m = new MutableSharedBitVectorIntSet((BitVectorIntSet) that);
      return intersection(m);
    } else if (that instanceof SparseIntSet) {
      BitVectorIntSet bv = new BitVectorIntSet(that);
      return intersection(bv);
    } else {
      // really slow. optimize as needed.
      BitVectorIntSet result = new BitVectorIntSet();
      for (IntIterator it = intIterator(); it.hasNext();) {
        int x = it.next();
        if (that.contains(x)) {
          result.add(x);
        }
      }
      return result;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.intset.IntSet#intersection(com.ibm.wala.util.intset.IntSet)
   */
  public IntSet intersection(MutableSharedBitVectorIntSet that) {
    MutableSparseIntSet t = makeSparseCopy();
    t.intersectWith(that);
    MutableSharedBitVectorIntSet result = new MutableSharedBitVectorIntSet(t);
    if (PARANOID) {
      checkIntegrity();
    }
    return result;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.intset.IntSet#isEmpty()
   */
  public boolean isEmpty() {
    return privatePart == null && sharedPart == null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.intset.IntSet#size()
   */
  public int size() {
    int result = 0;
    result += (privatePart == null) ? 0 : privatePart.size();
    result += (sharedPart == null) ? 0 : sharedPart.size();
    return result;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.intset.IntSet#iterator()
   */
  public IntIterator intIterator() {
    if (privatePart == null) {
      return (sharedPart == null) ? EmptyIntIterator.instance() : sharedPart.intIterator();
    } else {
      return (sharedPart == null) ? privatePart.intIterator() : new CompoundIntIterator(privatePart.intIterator(), sharedPart
          .intIterator());
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.intset.IntSet#foreach(com.ibm.wala.util.intset.IntSetAction)
   */
  public void foreach(IntSetAction action) {
    if (privatePart != null) {
      privatePart.foreach(action);
    }
    if (sharedPart != null) {
      sharedPart.foreach(action);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.intset.IntSet#foreachExcluding(com.ibm.wala.util.intset.IntSet,
   *      com.ibm.wala.util.intset.IntSetAction)
   */
  public void foreachExcluding(IntSet X, IntSetAction action) {
    if (X instanceof MutableSharedBitVectorIntSet) {
      foreachExcludingInternal((MutableSharedBitVectorIntSet) X, action);
    } else {
      foreachExcludingGeneral(X, action);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.intset.IntSet#foreachExcluding(com.ibm.wala.util.intset.IntSet,
   *      com.ibm.wala.util.intset.IntSetAction)
   */
  private void foreachExcludingInternal(MutableSharedBitVectorIntSet X, IntSetAction action) {

    if (sameSharedPart(this, X)) {
      if (privatePart != null) {
        if (X.privatePart != null) {
          privatePart.foreachExcluding(X.privatePart, action);
        } else {
          privatePart.foreach(action);
        }
      }
    } else {
      if (privatePart != null) {
        privatePart.foreachExcluding(X, action);
      }
      if (sharedPart != null) {
        sharedPart.foreachExcluding(X.makeDenseCopy(), action);
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.intset.IntSet#foreachExcluding(com.ibm.wala.util.intset.IntSet,
   *      com.ibm.wala.util.intset.IntSetAction)
   */
  private void foreachExcludingGeneral(IntSet X, IntSetAction action) {
    if (privatePart != null) {
      privatePart.foreachExcluding(X, action);
    }
    if (sharedPart != null) {
      sharedPart.foreachExcluding(X, action);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.intset.IntSet#max()
   */
  public int max() {
    int result = -1;
    if (privatePart != null && privatePart.size() > 0) {
      result = Math.max(result, privatePart.max());
    }
    if (sharedPart != null) {
      result = Math.max(result, sharedPart.max());
    }
    return result;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.intset.IntSet#sameValue(com.ibm.wala.util.intset.IntSet)
   */
  public boolean sameValue(IntSet that) {
    if (that instanceof MutableSharedBitVectorIntSet) {
      return sameValue((MutableSharedBitVectorIntSet) that);
    } else if (that instanceof SparseIntSet) {
      return sameValue((SparseIntSet) that);
    } else if (that instanceof BimodalMutableIntSet) {
      return that.sameValue(makeSparseCopy());
    } else if (that instanceof BitVectorIntSet) {
      return sameValue((BitVectorIntSet) that);
    } else if (that instanceof SemiSparseMutableIntSet) {
      return that.sameValue(this);
    } else {
      Assertions.UNREACHABLE("unexpected class " + that.getClass());
      return false;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.intset.IntSet#sameValue(com.ibm.wala.util.intset.IntSet)
   */
  private boolean sameValue(SparseIntSet that) {
    if (size() != that.size()) {
      return false;
    }
    if (sharedPart == null) {
      if (privatePart == null)
        /* both parts empty, and that has same (i.e. 0) size */
        return true;
      else
        return privatePart.sameValue(that);
    } else {
      /* sharedPart != null */
      return makeSparseCopy().sameValue(that);
    }
  }

  private boolean sameValue(BitVectorIntSet that) {
    if (size() != that.size()) {
      return false;
    }
    if (sharedPart == null) {
      if (privatePart == null)
        /* both parts empty, and that has same (i.e. 0) size */
        return true;
      else
        // shared part is null and size is same, so number of bits is low
        return that.makeSparseCopy().sameValue(privatePart);
    } else {
      if (privatePart == null)
        return sharedPart.sameValue(that);
      else
        /* sharedPart != null */
        return makeDenseCopy().sameValue(that);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.intset.IntSet#sameValue(com.ibm.wala.util.intset.IntSet)
   */
  private boolean sameValue(MutableSharedBitVectorIntSet that) {
    if (size() != that.size()) {
      return false;
    }
    if (sharedPart == null) {
      if (privatePart == null) {
        /* we must have size() == that.size() == 0 */
        return true;
      } else {
        /* sharedPart == null, privatePart != null */
        if (that.sharedPart == null) {
          if (that.privatePart == null) {
            return privatePart.isEmpty();
          } else {
            return privatePart.sameValue(that.privatePart);
          }
        } else {
          /* sharedPart = null, privatePart != null, that.sharedPart != null */
          if (that.privatePart == null) {
            return privatePart.sameValue(that.sharedPart);
          } else {
            BitVectorIntSet temp = new BitVectorIntSet(that.sharedPart);
            temp.addAllOblivious(that.privatePart);
            return privatePart.sameValue(temp);
          }
        }
      }
    } else {
      /* sharedPart != null */
      if (privatePart == null) {
        if (that.privatePart == null) {
          return sharedPart.sameValue(that.sharedPart);
        } else {
          /* privatePart == null, sharedPart != null, that.privatePart != null */
          if (that.sharedPart == null) {
            return sharedPart.sameValue(that.privatePart);
          } else {
            MutableSparseIntSet t = that.makeSparseCopy();
            return sharedPart.sameValue(t);
          }
        }
      } else {
        /* sharedPart != null , privatePart != null */
        if (that.sharedPart == null) {
          Assertions.UNREACHABLE();
          return false;
        } else {
          /* that.sharedPart != null */
          if (that.privatePart == null) {
            SparseIntSet s = makeSparseCopy();
            return s.sameValue(that.sharedPart);
          } else {
            /* that.sharedPart != null, that.privatePart != null */
            /* assume reference equality for canonical shared part */
            if (sharedPart == that.sharedPart) {
              return privatePart.sameValue(that.privatePart);
            } else {
              SparseIntSet s1 = makeSparseCopy();
              SparseIntSet s2 = that.makeSparseCopy();
              return s1.sameValue(s2);
            }
          }
        }
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.intset.IntSet#isSubset(com.ibm.wala.util.intset.IntSet)
   */
  public boolean isSubset(IntSet that) {
    if (that instanceof MutableSharedBitVectorIntSet) {
      return isSubset((MutableSharedBitVectorIntSet) that);
    } else {
      // really slow. optimize as needed.
      for (IntIterator it = intIterator(); it.hasNext();) {
        if (!that.contains(it.next())) {
          return false;
        }
      }
      return true;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.intset.IntSet#sameValue(com.ibm.wala.util.intset.IntSet)
   */
  private boolean isSubset(MutableSharedBitVectorIntSet that) {
    if (size() > that.size()) {
      return false;
    }
    if (sharedPart == null) {
      if (privatePart == null) {
        return true;
      } else {
        if (that.sharedPart == null) {
          return privatePart.isSubset(that.privatePart);
        } else {
          /* sharedPart == null, that.sharedPart != null */
          if (that.privatePart == null) {
            return privatePart.isSubset(that.sharedPart);
          } else {
            SparseIntSet s1 = that.makeSparseCopy();
            return privatePart.isSubset(s1);
          }
        }
      }
    } else {
      /* sharedPart != null */
      if (privatePart == null) {
        /* sharedPart != null, privatePart == null */
        if (that.privatePart == null) {
          if (that.sharedPart == null) {
            return false;
          } else {
            return sharedPart.isSubset(that.sharedPart);
          }
        } else {
          if (that.sharedPart == null) {
            return sharedPart.isSubset(that.privatePart);
          } else {
            SparseIntSet s1 = that.makeSparseCopy();
            return sharedPart.isSubset(s1);
          }
        }
      } else {
        /* sharedPart != null, privatePart != null */
        if (that.privatePart == null) {
          return privatePart.isSubset(that.sharedPart) && sharedPart.isSubset(that.sharedPart);
        } else {
          /* sharedPart != null, privatePart!= null, that.privatePart != null */
          if (that.sharedPart == null) {
            return privatePart.isSubset(that.privatePart) && sharedPart.isSubset(that.privatePart);
          } else {
            /*
             * sharedPart != null, privatePart!= null, that.privatePart != null,
             * that.sharedPart != null
             */
            if (sharedPart.isSubset(that.sharedPart)) {
              if (privatePart.isSubset(that.privatePart)) {
                return true;
              } else {
                SparseIntSet s1 = that.makeSparseCopy();
                return privatePart.isSubset(s1);
              }
            } else {
              /* !sharedPart.isSubset(that.sharedPart) */
              BitVectorIntSet temp = new BitVectorIntSet(sharedPart);
              temp.removeAll(that.sharedPart);
              if (temp.isSubset(that.privatePart)) {
                /* sharedPart.isSubset(that) */
                if (privatePart.isSubset(that.privatePart)) {
                  return true;
                } else {
                  MutableSparseIntSet t = new MutableSparseIntSet(privatePart);
                  t.removeAll(that.privatePart);
                  if (t.isSubset(that.sharedPart)) {
                    return true;
                  } else {
                    return false;
                  }
                }
              } else {
                /*
                 * !((sharedPart-that.sharedPart).isSubset(that.privatePart))
                 * i.e some bit in my shared part is in neither that's
                 * sharedPart nor that's privatePart, hence I am not a subset of
                 * that
                 */
                return false;
              }
            }
          }
        }
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.intset.MutableIntSet#copySet(com.ibm.wala.util.intset.IntSet)
   */
  public void copySet(IntSet set) {
    if (set instanceof MutableSharedBitVectorIntSet) {
      MutableSharedBitVectorIntSet other = (MutableSharedBitVectorIntSet) set;
      if (other.privatePart != null) {
        this.privatePart = new MutableSparseIntSet(other.privatePart);
      } else {
        this.privatePart = null;
      }
      this.sharedPart = other.sharedPart;
    } else {
      // really slow.  optimize as needed.
      clear();
      addAll(set);
    }

    if (PARANOID) {
      checkIntegrity();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.intset.MutableIntSet#addAll(com.ibm.wala.util.intset.IntSet)
   */
  public boolean addAll(IntSet set) {
    if (set instanceof MutableSharedBitVectorIntSet) {
      boolean result = addAll((MutableSharedBitVectorIntSet) set);
      if (PARANOID) {
        checkIntegrity();
      }
      return result;
    } else if (set instanceof SparseIntSet) {
      boolean result = addAllInternal((SparseIntSet) set);
      if (PARANOID) {
        checkIntegrity();
      }
      return result;
    } else if (set instanceof BitVectorIntSet) {
      boolean result = addAllInternal((BitVectorIntSet) set);
      if (PARANOID) {
        checkIntegrity();
      }
      return result;
    } else if (set instanceof DebuggingMutableIntSet) {
      SparseIntSet temp = new SparseIntSet(set);
      boolean result = addAllInternal(temp);
      if (PARANOID) {
        checkIntegrity();
      }
      return result;
    } else {
      // really slow.   optimize as needed.
      boolean result = false;
      for (IntIterator it = set.intIterator(); it.hasNext(); ) {
        int x = it.next();
        if (!contains(x)) {
          result = true;
          add(x);
        }
      }
      return result;
    }
  }

  /**
   * @param set
   */
  private boolean addAllInternal(BitVectorIntSet set) {
    if (Assertions.verifyAssertions) {
      // should have hijacked this case before getting here!
      Assertions._assert(sharedPart != set);
    }
    if (privatePart == null) {
      if (sharedPart == null) {
        copyValue(set);
        return !set.isEmpty();
      }
    }
    BitVectorIntSet temp = makeDenseCopy();
    boolean result = temp.addAll(set);
    copyValue(temp);
    return result;
  }

  /**
   * @param set
   */
  private boolean addAllInternal(SparseIntSet set) {
    if (privatePart == null) {
      if (sharedPart == null) {
        if (!set.isEmpty()) {
          privatePart = new MutableSparseIntSet(set);
          sharedPart = null;
          checkOverflow();
          return true;
        } else {
          return false;
        }
      } else {
        privatePart = new MutableSparseIntSet(set);
        privatePart.removeAll(sharedPart);
        if (privatePart.isEmpty()) {
          privatePart = null;
          return false;
        } else {
          checkOverflow();
          return true;
        }
      }
    } else { /* privatePart != null */
      if (sharedPart == null) {
        boolean result = privatePart.addAll(set);
        checkOverflow();
        return result;
      } else {
        int oldSize = privatePart.size();
        privatePart.addAll(set);
        privatePart.removeAll(sharedPart);
        boolean result = privatePart.size() > oldSize;
        checkOverflow();
        return result;
      }
    }
  }

  /**
   * @param set
   */
  private boolean addAll(MutableSharedBitVectorIntSet set) {
    if (set.isEmpty()) {
      return false;
    }
    if (isEmpty()) {
      if (set.privatePart != null) {
        privatePart = new MutableSparseIntSet(set.privatePart);
      }
      sharedPart = set.sharedPart;
      return true;
    }

    if (set.sharedPart == null) {
      return addAllInternal(set.privatePart);
    } else {
      // set.sharedPart != null
      if (sameSharedPart(this, set)) {
        if (set.privatePart == null) {
          return false;
        } else {
          return addAllInternal(set.privatePart);
        }
      } else {
        // !sameSharedPart
        if (set.privatePart == null) {
          if (sharedPart == null || sharedPart.isSubset(set.sharedPart)) {
            // a heuristic that should be profitable if this condition usually
            // holds.
            int oldSize = size();
            if (privatePart != null) {
              privatePart.removeAll(set.sharedPart);
              privatePart = privatePart.isEmpty() ? null : privatePart;
            }
            sharedPart = set.sharedPart;
            return size() > oldSize;
          } else {
            BitVectorIntSet temp = makeDenseCopy();
            boolean b = temp.addAll(set.sharedPart);
            if (b) {
              // a heuristic: many times these are the same value,
              // so avoid looking up the shared subset in the bv repository
              if (temp.sameValue(set.sharedPart)) {
                this.privatePart = null;
                this.sharedPart = set.sharedPart;
              } else {
                copyValue(temp);
              }
            }
            return b;
          }
        } else {
          // set.privatePart != null;
          BitVectorIntSet temp = makeDenseCopy();
          BitVectorIntSet other = set.makeDenseCopy();
          boolean b = temp.addAll(other);
          if (b) {
            // a heuristic: many times these are the same value,
            // so avoid looking up the shared subset in the bv repository
            if (temp.sameValue(other)) {
              this.privatePart = new MutableSparseIntSet(set.privatePart);
              this.sharedPart = set.sharedPart;
            } else {
              // System.err.println("COPY " + this + " " + set);
              copyValue(temp);
            }
          }
          return b;
        }
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.intset.MutableIntSet#add(int)
   */
  public boolean add(int i) {
    if (privatePart == null) {
      if (sharedPart == null) {
        privatePart = new MutableSparseIntSet();
        privatePart.add(i);
        return true;
      } else {
        if (sharedPart.contains(i)) {
          return false;
        } else {
          privatePart = new MutableSparseIntSet();
          privatePart.add(i);
          return true;
        }
      }
    } else {
      if (sharedPart == null) {
        boolean result = privatePart.add(i);
        checkOverflow();
        return result;
      } else {
        if (sharedPart.contains(i)) {
          return false;
        } else {
          boolean result = privatePart.add(i);
          checkOverflow();
          return result;
        }
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.intset.MutableIntSet#remove(int)
   */
  public boolean remove(int i) {
    if (privatePart != null) {
      if (privatePart.contains(i)) {
        privatePart.remove(i);
        if (privatePart.size() == 0) {
          privatePart = null;
        }
        return true;
      }
    }
    if (sharedPart != null) {
      if (sharedPart.contains(i)) {
        privatePart = makeSparseCopy();
        privatePart.remove(i);
        if (privatePart.size() == 0) {
          privatePart = null;
        }
        sharedPart = null;
        checkOverflow();
        return true;
      }
    }
    return false;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.intset.MutableIntSet#intersectWith(com.ibm.wala.util.intset.IntSet)
   */
  public void intersectWith(IntSet set) {
    if (set instanceof MutableSharedBitVectorIntSet) {
      intersectWithInternal((MutableSharedBitVectorIntSet) set);
    } else if (set instanceof BitVectorIntSet) {
      intersectWithInternal(new MutableSharedBitVectorIntSet((BitVectorIntSet) set));
    } else {
      // this is really slow. optimize as needed.
      for (IntIterator it = intIterator(); it.hasNext();) {
        int x = it.next();
        if (!set.contains(x)) {
          remove(x);
        }
      }
    }
    if (DEBUG) {
      if (privatePart != null && sharedPart != null)
        Assertions._assert(privatePart.intersection(sharedPart).isEmpty());
    }
  }

  /**
   * @param set
   */
  private void intersectWithInternal(MutableSharedBitVectorIntSet set) {

    if (sharedPart != null) {
      if (sameSharedPart(this, set)) {
        // no need to intersect shared part
        if (privatePart != null) {
          if (set.privatePart == null) {
            privatePart = null;
          } else {
            privatePart.intersectWith(set.privatePart);
            if (privatePart.isEmpty()) {
              privatePart = null;
            }
          }
        }
      } else {
        // not the same shared part
        if (set.sharedPart == null) {
          if (set.privatePart == null) {
            privatePart = null;
            sharedPart = null;
          } else {
            MutableSparseIntSet temp = new MutableSparseIntSet(set.privatePart);
            temp.intersectWith(this);
            sharedPart = null;
            if (temp.isEmpty()) {
              privatePart = null;
            } else {
              privatePart = temp;
              checkOverflow();
            }
          }
        } else {
          // set.sharedPart != null
          BitVectorIntSet b = makeDenseCopy();
          b.intersectWith(set.makeDenseCopy());
          copyValue(b);
        }
      }
    } else {
      if (privatePart != null) {
        privatePart.intersectWith(set);
        if (privatePart.isEmpty()) {
          privatePart = null;
        }
      }
    }
    if (PARANOID) {
      checkIntegrity();
    }

  }

  public static boolean sameSharedPart(MutableSharedBitVectorIntSet a, MutableSharedBitVectorIntSet b) {
    return a.sharedPart == b.sharedPart;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  public String toString() {
    return makeSparseCopy().toString();
  }

  /**
   * Warning: inefficient; this should not be called often.
   */
  MutableSparseIntSet makeSparseCopy() {
    if (privatePart == null) {
      if (sharedPart == null) {
        return new MutableSparseIntSet();
      } else {
        return (MutableSparseIntSet) new MutableSparseIntSetFactory().makeCopy(sharedPart);
      }
    } else {
      if (sharedPart == null) {
        return new MutableSparseIntSet(privatePart);
      } else {
        /* privatePart != null, sharedPart != null */
        MutableSparseIntSet result = new MutableSparseIntSet(privatePart);
        result.addAll(sharedPart);
        return result;
      }
    }
  }

  /**
   */
  BitVectorIntSet makeDenseCopy() {
    if (privatePart == null) {
      if (sharedPart == null) {
        return new BitVectorIntSet();
      } else {
        return new BitVectorIntSet(sharedPart);
      }
    } else {
      if (sharedPart == null) {
        return new BitVectorIntSet(privatePart);
      } else {
        BitVectorIntSet temp = new BitVectorIntSet(sharedPart);
        temp.addAllOblivious(privatePart);
        return temp;
      }
    }
  }

  public boolean hasSharedPart() {
    return sharedPart != null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.intset.IntSet#containsAny(com.ibm.wala.util.intset.IntSet)
   */
  public boolean containsAny(IntSet set) {
    if (set instanceof MutableSharedBitVectorIntSet) {
      MutableSharedBitVectorIntSet other = (MutableSharedBitVectorIntSet) set;
      if (sharedPart != null) {
        // an optimization to make life easier on the underlying
        // bitvectorintsets
        if (other.sharedPart != null && sharedPart.containsAny(other.sharedPart)) {
          return true;
        }
        if (other.privatePart != null && sharedPart.containsAny(other.privatePart)) {
          return true;
        }
      }
      if (privatePart != null && privatePart.containsAny(set)) {
        return true;
      }
      return false;
    } else {
      if (sharedPart != null && sharedPart.containsAny(set)) {
        return true;
      }
      if (privatePart != null && privatePart.containsAny(set)) {
        return true;
      }
      return false;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.intset.MutableIntSet#addAllExcluding(com.ibm.wala.util.intset.IntSet,
   *      com.ibm.wala.util.intset.IntSet)
   */
  public boolean addAllInIntersection(IntSet other, IntSet filter) {
    if (other instanceof MutableSharedBitVectorIntSet) {
      return addAllInIntersectionInternal((MutableSharedBitVectorIntSet) other, filter);
    }
    return addAllInIntersectionGeneral(other, filter);
  }

  /**
   */
  private boolean addAllInIntersectionGeneral(IntSet other, IntSet filter) {
    BitVectorIntSet o = new BitVectorIntSet(other);
    o.intersectWith(filter);
    return addAll(o);
  }

  /**
   */
  private boolean addAllInIntersectionInternal(MutableSharedBitVectorIntSet other, IntSet filter) {
    if (other.sharedPart == null) {
      if (other.privatePart == null) {
        return false;
      } else {
        // other.sharedPart == null, other.privatePart != null
        return addAllInIntersectionInternal(other.privatePart, filter);
      }
    } else {
      // other.sharedPart != null
      if (sharedPart == other.sharedPart) {
        // no need to add in other.sharedPart
        if (other.privatePart == null) {
          return false;
        } else {
          return addAllInIntersectionInternal(other.privatePart, filter);
        }
      } else {
        MutableSharedBitVectorIntSet o = new MutableSharedBitVectorIntSet(other);
        o.intersectWith(filter);
        return addAll(o);
      }
    }
  }

  /**
   * @param other
   * @param filter
   */
  private boolean addAllInIntersectionInternal(SparseIntSet other, IntSet filter) {
    if (sharedPart == null) {
      if (privatePart == null) {
        privatePart = new MutableSparseIntSet(other);
        privatePart.intersectWith(filter);
        if (privatePart.size() == 0) {
          privatePart = null;
        }
        checkOverflow();
        return size() > 0;
      } else {
        /** sharedPart == null, privatePart != null */
        boolean result = privatePart.addAllInIntersection(other, filter);
        checkOverflow();
        return result;
      }
    } else {
      /** sharedPart != null */
      if (privatePart == null) {
        privatePart = new MutableSparseIntSet(sharedPart);
        sharedPart = null;
        boolean result = privatePart.addAllInIntersection(other, filter);
        checkOverflow();
        return result;
      } else {
        /** sharedPart != null, privatePart != null */
        // note that "other" is likely small
        MutableSparseIntSet temp = new MutableSparseIntSet(other);
        temp.intersectWith(filter);
        return addAll(temp);
      }
    }
  }

  public void clear() {
    privatePart = null;
    sharedPart = null;
  }
}