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
package com.ibm.wala.util;

import java.util.HashMap;

import com.ibm.wala.shrikeBT.BytecodeConstants;
import com.ibm.wala.shrikeBT.Constants;
import com.ibm.wala.shrikeBT.IInvokeInstruction;
import com.ibm.wala.shrikeBT.InvokeInstruction;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.debug.Assertions;

/**
 * Utilties to interface with the Shrike CT library.
 * 
 * @author sfink
 */
public class ShrikeUtil implements BytecodeConstants {

  private static HashMap<String, TypeReference> primitiveMap;

  static {
    primitiveMap = HashMapFactory.make(10);
    primitiveMap.put("I", TypeReference.Int);
    primitiveMap.put("J", TypeReference.Long);
    primitiveMap.put("S", TypeReference.Short);
    primitiveMap.put("B", TypeReference.Byte);
    primitiveMap.put("C", TypeReference.Char);
    primitiveMap.put("D", TypeReference.Double);
    primitiveMap.put("F", TypeReference.Float);
    primitiveMap.put("Z", TypeReference.Boolean);
    primitiveMap.put("V", TypeReference.Void);
    primitiveMap.put(Constants.TYPE_null, TypeReference.Null);
  }

  /**
   * Method getInvocationCode.
   * 
   * @param instruction
   * @return byte
   * @throws IllegalArgumentException  if instruction is null
   */
  public static IInvokeInstruction.Dispatch getInvocationCode(InvokeInstruction instruction) {
    if (instruction == null) {
      throw new IllegalArgumentException("instruction is null");
    }
    switch (instruction.getInvocationMode()) {
    case Constants.OP_invokestatic:
      return IInvokeInstruction.Dispatch.STATIC;
    case Constants.OP_invokevirtual:
      return IInvokeInstruction.Dispatch.VIRTUAL;
    case Constants.OP_invokespecial:
      return IInvokeInstruction.Dispatch.SPECIAL;
    case Constants.OP_invokeinterface:
      return IInvokeInstruction.Dispatch.INTERFACE;
    default:
      Assertions.UNREACHABLE();
      return null;
    }
  }

  /**
   * @param type
   *          a type as a String returned by Shrike
   */
  public static TypeReference makeTypeReference(ClassLoaderReference loader, String type) throws IllegalArgumentException {

    if (Assertions.verifyAssertions) {
      Assertions._assert(type != null);
    }
    TypeReference p = primitiveMap.get(type);
    if (p != null) {
      return p;
    }
    ImmutableByteArray b = ImmutableByteArray.make(type);
    TypeName T = null;
    if (b.get(0) != '[') {
      T = TypeName.findOrCreate(b, 0, b.length() - 1);
    } else {
      if (b.get(b.length() - 1) == ';') {
        T = TypeName.findOrCreate(b, 0, b.length() - 1);
      } else {
        T = TypeName.findOrCreate(b);
      }
    }
    return TypeReference.findOrCreate(loader, T);
  }
}