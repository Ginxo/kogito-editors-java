/*
 * Copyright 2011 JBoss, by Red Hat, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.errai.codegen;

import org.jboss.errai.codegen.builder.BlockBuilder;
import org.jboss.errai.codegen.builder.ClassStructureBuilder;
import org.jboss.errai.codegen.builder.impl.ClassBuilder;
import org.jboss.errai.codegen.exception.UnproxyableClassException;
import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.codegen.meta.MetaClassFactory;
import org.jboss.errai.codegen.meta.MetaMethod;
import org.jboss.errai.codegen.meta.impl.build.BuildMetaClass;
import org.jboss.errai.codegen.util.GenUtil;
import org.jboss.errai.codegen.util.Stmt;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Mike Brock
 */
public class ProxyMaker {
  public static BuildMetaClass makeProxy(String proxyClassName, Class cls) {
    return makeProxy(proxyClassName, MetaClassFactory.get(cls));
  }

  public static final String PROXY_BIND_METHOD = "__$setProxiedInstance$";

  public static BuildMetaClass makeProxy(String proxyClassName, MetaClass toProxy) {
    ClassStructureBuilder builder;

    if (!toProxy.isInterface()) {
      if (toProxy.isFinal()) {
        throw new UnproxyableClassException(toProxy, toProxy.getFullyQualifiedName()
                + " is an unproxiable class because it is final");
      }
      if (!toProxy.isDefaultInstantiable()) {
        throw new UnproxyableClassException(toProxy, toProxy.getFullyQualifiedName() + " must have a default " +
                "no-arg constructor");
      }

      builder = ClassBuilder.define(proxyClassName, toProxy).publicScope().body();
    }
    else {
      builder = ClassBuilder.define(proxyClassName).publicScope().implementsInterface(toProxy).body();
    }

    final String proxyVar = "$$_proxy_$$";

    Set<String> renderedMethods = new HashSet<String>();

    builder.privateField(proxyVar, toProxy).finish();
    for (MetaMethod method : toProxy.getMethods()) {
      String methodString = GenUtil.getMethodString(method);
      if (renderedMethods.contains(methodString)) continue;
      renderedMethods.add(methodString);

      if (method.isSynthetic() || method.isFinal() ||
              method.getDeclaringClass().getFullyQualifiedName().equals("java.lang.Object")) continue;

      DefParameters defParameters = DefParameters.from(method);
      BlockBuilder methBody = builder.publicMethod(method.getReturnType(), method.getName()).parameters(defParameters)
              .body();

      List<Parameter> parms = defParameters.getParameters();

      Statement[] statementVars = new Statement[parms.size()];
      for (int i = 0; i < parms.size(); i++) {
        statementVars[i] = Stmt.loadVariable(parms.get(i).getName());
      }

      if (method.getReturnType().isVoid()) {
        methBody.append(Stmt.loadVariable(proxyVar).invoke(method, statementVars));
      }
      else {
        methBody.append(Stmt.loadVariable(proxyVar).invoke(method, statementVars).returnValue());
      }
      methBody.finish();
    }

    builder.publicMethod(void.class, PROXY_BIND_METHOD).parameters(DefParameters.of(Parameter.of(toProxy, "proxy")))
            .append(Stmt.loadVariable(proxyVar).assignValue(Stmt.loadVariable("proxy"))).finish();

    return builder.getClassDefinition();
  }
}
