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

import java.util.Iterator;
import java.util.TreeSet;

/**
 * @author sfink
 */
public class BitVectorIntSetFactory implements MutableIntSetFactory {

  /**
   * @param set
   * @throws IllegalArgumentException  if set is null
   */
  public MutableIntSet make(int[] set) {
    if (set == null) {
      throw new IllegalArgumentException("set is null");
    }
    if (set.length == 0) {
      return new BitVectorIntSet();
    } else {
      // XXX not very efficient.
      TreeSet<Integer> T = new TreeSet<Integer>();
      for (int i = 0; i < set.length; i++) {
        T.add(new Integer(set[i]));
      }
      BitVectorIntSet result = new BitVectorIntSet();
      for (Iterator<Integer> it = T.iterator(); it.hasNext();) {
        Integer I = it.next();
        result.add(I.intValue());
      }
      return result;
    }
  }

  /**
   * @param string
   */
  public MutableIntSet parse(String string) throws NumberFormatException {
    int[] data = SparseIntSet.parseIntArray(string);
    MutableIntSet result = new BitVectorIntSet();
    for (int i = 0; i < data.length; i++) {
      result.add(data[i]);
    }
    return result;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.intset.MutableIntSetFactory#make(com.ibm.wala.util.intset.IntSet)
   */
  public MutableIntSet makeCopy(IntSet x) {
    return new BitVectorIntSet(x);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.intset.MutableIntSetFactory#make()
   */
  public MutableIntSet make() {
    return new BitVectorIntSet();
  }

}
