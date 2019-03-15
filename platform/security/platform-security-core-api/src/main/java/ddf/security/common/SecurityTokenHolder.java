/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.security.common;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicReference;

public class SecurityTokenHolder implements Serializable {

  private static final long serialVersionUID = 1L;

  private final AtomicReference<Object> securityTokenAtomicReference = new AtomicReference<>();

  public Object getSecurityToken() {
    return securityTokenAtomicReference.get();
  }

  public void setSecurityToken(Object securityToken) {
    securityTokenAtomicReference.set(securityToken);
  }

  public void remove() {
    securityTokenAtomicReference.set(null);
  }
}
