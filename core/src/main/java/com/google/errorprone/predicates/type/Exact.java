/*
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.errorprone.predicates.type;

import com.google.errorprone.VisitorState;
import com.google.errorprone.predicates.TypePredicate;
import com.google.errorprone.suppliers.Supplier;

import com.sun.tools.javac.code.Type;

/** Matches types that exactly match the given type. */
public class Exact implements TypePredicate {

  public final Supplier<Type> supplier;

  public Exact(Supplier<Type> type) {
    this.supplier = type;
  }

  @Override
  public boolean apply(Type type, VisitorState state) {
    Type expected = supplier.get(state);
    if (expected == null || type == null) {
      return false;
    }
    return state.getTypes().isSameType(expected, type);
  }
}
