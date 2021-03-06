/*
 * Copyright (c) 2000, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package java.lang;

import jdk.internal.loader.BuiltinClassLoader;
import jdk.internal.misc.SharedSecrets;
import jdk.internal.misc.VM;
import jdk.internal.module.ModuleHashes;

import java.lang.module.ModuleDescriptor.Version;
import java.lang.reflect.Layer;
import java.lang.reflect.Module;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * An element in a stack trace, as returned by {@link
 * Throwable#getStackTrace()}.  Each element represents a single stack frame.
 * All stack frames except for the one at the top of the stack represent
 * a method invocation.  The frame at the top of the stack represents the
 * execution point at which the stack trace was generated.  Typically,
 * this is the point at which the throwable corresponding to the stack trace
 * was created.
 *
 * @since  1.4
 * @author Josh Bloch
 */
public final class StackTraceElement implements java.io.Serializable {
    // This field is set to the compacted String representation used
    // by StackTraceElement::toString and stored in serial form.
    //
    // This field is of Object type. VM initially sets this field to
    // the Class object of the declaring class to build the compacted string.
    private Object classOrLoaderModuleClassName;

    // Normally initialized by VM
    private String classLoaderName;
    private String moduleName;
    private String moduleVersion;
    private String declaringClass;
    private String methodName;
    private String fileName;
    private int    lineNumber;

    /**
     * Creates a stack trace element representing the specified execution
     * point. The {@link #getModuleName module name} and {@link
     * #getModuleVersion module version} of the stack trace element will
     * be {@code null}.
     *
     * @param declaringClass the fully qualified name of the class containing
     *        the execution point represented by the stack trace element
     * @param methodName the name of the method containing the execution point
     *        represented by the stack trace element
     * @param fileName the name of the file containing the execution point
     *         represented by the stack trace element, or {@code null} if
     *         this information is unavailable
     * @param lineNumber the line number of the source line containing the
     *         execution point represented by this stack trace element, or
     *         a negative number if this information is unavailable. A value
     *         of -2 indicates that the method containing the execution point
     *         is a native method
     * @throws NullPointerException if {@code declaringClass} or
     *         {@code methodName} is null
     * @since 1.5
     */
    public StackTraceElement(String declaringClass, String methodName,
                             String fileName, int lineNumber) {
        this(null, null, null, declaringClass, methodName, fileName, lineNumber);
    }

    /**
     * Creates a stack trace element representing the specified execution
     * point.
     *
     * @param classLoaderName the class loader name if the class loader of
     *        the class containing the execution point represented by
     *        the stack trace is named; otherwise {@code null}
     * @param moduleName the module name if the class containing the
     *        execution point represented by the stack trace is in a named
     *        module; otherwise {@code null}
     * @param moduleVersion the module version if the class containing the
     *        execution point represented by the stack trace is in a named
     *        module that has a version; otherwise {@code null}
     * @param declaringClass the fully qualified name of the class containing
     *        the execution point represented by the stack trace element
     * @param methodName the name of the method containing the execution point
     *        represented by the stack trace element
     * @param fileName the name of the file containing the execution point
     *        represented by the stack trace element, or {@code null} if
     *        this information is unavailable
     * @param lineNumber the line number of the source line containing the
     *        execution point represented by this stack trace element, or
     *        a negative number if this information is unavailable. A value
     *        of -2 indicates that the method containing the execution point
     *        is a native method
     *
     * @throws NullPointerException if {@code declaringClass} is {@code null}
     *         or {@code methodName} is {@code null}
     *
     * @since 9
     */
    public StackTraceElement(String classLoaderName,
                             String moduleName, String moduleVersion,
                             String declaringClass, String methodName,
                             String fileName, int lineNumber) {
        this.classLoaderName = classLoaderName;
        this.moduleName      = moduleName;
        this.moduleVersion   = moduleVersion;
        this.declaringClass  = Objects.requireNonNull(declaringClass, "Declaring class is null");
        this.methodName      = Objects.requireNonNull(methodName, "Method name is null");
        this.fileName        = fileName;
        this.lineNumber      = lineNumber;
    }

    /*
     * Private constructor for the factory methods to create StackTraceElement
     * for Throwable and StackFrameInfo
     */
    private StackTraceElement() {}

    /**
     * Returns the name of the source file containing the execution point
     * represented by this stack trace element.  Generally, this corresponds
     * to the {@code SourceFile} attribute of the relevant {@code class}
     * file (as per <i>The Java Virtual Machine Specification</i>, Section
     * 4.7.7).  In some systems, the name may refer to some source code unit
     * other than a file, such as an entry in source repository.
     *
     * @return the name of the file containing the execution point
     *         represented by this stack trace element, or {@code null} if
     *         this information is unavailable.
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Returns the line number of the source line containing the execution
     * point represented by this stack trace element.  Generally, this is
     * derived from the {@code LineNumberTable} attribute of the relevant
     * {@code class} file (as per <i>The Java Virtual Machine
     * Specification</i>, Section 4.7.8).
     *
     * @return the line number of the source line containing the execution
     *         point represented by this stack trace element, or a negative
     *         number if this information is unavailable.
     */
    public int getLineNumber() {
        return lineNumber;
    }

    /**
     * Returns the module name of the module containing the execution point
     * represented by this stack trace element.
     *
     * @return the module name of the {@code Module} containing the execution
     *         point represented by this stack trace element; {@code null}
     *         if the module name is not available.
     * @since 9
     * @see java.lang.reflect.Module#getName()
     */
    public String getModuleName() {
        return moduleName;
    }

    /**
     * Returns the module version of the module containing the execution point
     * represented by this stack trace element.
     *
     * @return the module version of the {@code Module} containing the execution
     *         point represented by this stack trace element; {@code null}
     *         if the module version is not available.
     * @since 9
     * @see java.lang.module.ModuleDescriptor.Version
     */
    public String getModuleVersion() {
        return moduleVersion;
    }

    /**
     * Returns the name of the class loader of the class containing the
     * execution point represented by this stack trace element.
     *
     * @return the name of the class loader of the class containing the execution
     *         point represented by this stack trace element; {@code null}
     *         if the class loader is not named.
     *
     * @since 9
     * @see java.lang.ClassLoader#getName()
     */
    public String getClassLoaderName() {
        return classLoaderName;
    }

    /**
     * Returns the fully qualified name of the class containing the
     * execution point represented by this stack trace element.
     *
     * @return the fully qualified name of the {@code Class} containing
     *         the execution point represented by this stack trace element.
     */
    public String getClassName() {
        return declaringClass;
    }

    /**
     * Returns the name of the method containing the execution point
     * represented by this stack trace element.  If the execution point is
     * contained in an instance or class initializer, this method will return
     * the appropriate <i>special method name</i>, {@code <init>} or
     * {@code <clinit>}, as per Section 3.9 of <i>The Java Virtual
     * Machine Specification</i>.
     *
     * @return the name of the method containing the execution point
     *         represented by this stack trace element.
     */
    public String getMethodName() {
        return methodName;
    }

    /**
     * Returns true if the method containing the execution point
     * represented by this stack trace element is a native method.
     *
     * @return {@code true} if the method containing the execution point
     *         represented by this stack trace element is a native method.
     */
    public boolean isNativeMethod() {
        return lineNumber == -2;
    }

    /**
     * Returns a string representation of this stack trace element.  The
     * format of this string depends on the implementation, but the following
     * examples may be regarded as typical:
     * <ul>
     * <li>
     *     "{@code com.foo.loader/foo@9.0/com.foo.Main.run(Main.java:101)}"
     * - See the description below.
     * </li>
     * <li>
     *     "{@code com.foo.loader/foo@9.0/com.foo.Main.run(Main.java)}"
     * - The line number is unavailable.
     * </li>
     * <li>
     *     "{@code com.foo.loader/foo@9.0/com.foo.Main.run(Unknown Source)}"
     * - Neither the file name nor the line number is available.
     * </li>
     * <li>
     *     "{@code com.foo.loader/foo@9.0/com.foo.Main.run(Native Method)}"
     * - The method containing the execution point is a native method.
     * </li>
     * <li>
     *     "{@code com.foo.loader//com.foo.bar.App.run(App.java:12)}"
     * - The class of the execution point is defined in the unnamed module of
     * the class loader named {@code com.foo.loader}.
     * </li>
     * <li>
     *     "{@code acme@2.1/org.acme.Lib.test(Lib.java:80)}"
     * - The class of the execution point is defined in {@code acme} module
     * loaded by by a built-in class loader such as the application class loader.
     * </li>
     * <li>
     *     "{@code MyClass.mash(MyClass.java:9)}"
     * - {@code MyClass} class is on the application class path.
     * </li>
     * </ul>
     *
     * <p> The first example shows a stack trace element consisting of
     * three elements, each separated by {@code "/"} followed with
     * the source file name and the line number of the source line
     * containing the execution point.
     *
     * The first element "{@code com.foo.loader}" is
     * the name of the class loader.  The second element "{@code foo@9.0}"
     * is the module name and version.  The third element is the method
     * containing the execution point; "{@code com.foo.Main"}" is the
     * fully-qualified class name and "{@code run}" is the name of the method.
     * "{@code Main.java}" is the source file name and "{@code 101}" is
     * the line number.
     *
     * <p> If a class is defined in an <em>unnamed module</em>
     * then the second element is omitted as shown in
     * "{@code com.foo.loader//com.foo.bar.App.run(App.java:12)}".
     *
     * If the class loader is a <a href="ClassLoader.html#builtinLoaders">
     * built-in class loader</a> or is not named then the first element
     * and its following {@code "/"} are omitted as shown in
     * "{@code acme@2.1/org.acme.Lib.test(Lib.java:80)}".
     * If the first element is omitted and the module is an unnamed module,
     * the second element and its following {@code "/"} are also omitted
     * as shown in "{@code MyClass.mash(MyClass.java:9)}".
     *
     * @see    Throwable#printStackTrace()
     */
    public String toString() {
        String s = buildLoaderModuleClassName();
        if (s == null) {
            // all elements will be included
            s = "";
            if (classLoaderName != null && !classLoaderName.isEmpty()) {
                s += classLoaderName + "/";
            }
            if (moduleName != null && !moduleName.isEmpty()) {
                s += moduleName;

                if (moduleVersion != null && !moduleVersion.isEmpty()) {
                    s += "@" + moduleVersion;
                }
            }
            s = s.isEmpty() ? declaringClass : s + "/" + declaringClass;
        }

        return s + "." + methodName + "(" +
             (isNativeMethod() ? "Native Method)" :
              (fileName != null && lineNumber >= 0 ?
               fileName + ":" + lineNumber + ")" :
                (fileName != null ?  ""+fileName+")" : "Unknown Source)")));
    }

    /**
     * Returns true if the specified object is another
     * {@code StackTraceElement} instance representing the same execution
     * point as this instance.  Two stack trace elements {@code a} and
     * {@code b} are equal if and only if:
     * <pre>{@code
     *     equals(a.getClassLoaderName(), b.getClassLoaderName()) &&
     *     equals(a.getModuleName(), b.getModuleName()) &&
     *     equals(a.getModuleVersion(), b.getModuleVersion()) &&
     *     equals(a.getClassName(), b.getClassName()) &&
     *     equals(a.getMethodName(), b.getMethodName())
     *     equals(a.getFileName(), b.getFileName()) &&
     *     a.getLineNumber() == b.getLineNumber()
     *
     * }</pre>
     * where {@code equals} has the semantics of {@link
     * java.util.Objects#equals(Object, Object) Objects.equals}.
     *
     * @param  obj the object to be compared with this stack trace element.
     * @return true if the specified object is another
     *         {@code StackTraceElement} instance representing the same
     *         execution point as this instance.
     */
    public boolean equals(Object obj) {
        if (obj==this)
            return true;
        if (!(obj instanceof StackTraceElement))
            return false;
        StackTraceElement e = (StackTraceElement)obj;
        return Objects.equals(classLoaderName, e.classLoaderName) &&
            Objects.equals(moduleName, e.moduleName) &&
            Objects.equals(moduleVersion, e.moduleVersion) &&
            e.declaringClass.equals(declaringClass) &&
            e.lineNumber == lineNumber &&
            Objects.equals(methodName, e.methodName) &&
            Objects.equals(fileName, e.fileName);
    }

    /**
     * Returns a hash code value for this stack trace element.
     */
    public int hashCode() {
        int result = 31*declaringClass.hashCode() + methodName.hashCode();
        result = 31*result + Objects.hashCode(classLoaderName);
        result = 31*result + Objects.hashCode(moduleName);
        result = 31*result + Objects.hashCode(moduleVersion);
        result = 31*result + Objects.hashCode(fileName);
        result = 31*result + lineNumber;
        return result;
    }


    /**
     * Build the compacted String representation to be returned by
     * toString method from the declaring Class object.
     */
    synchronized String buildLoaderModuleClassName() {
        if (classOrLoaderModuleClassName == null)
            return null;

        if (classOrLoaderModuleClassName instanceof Class) {
            Class<?> cls = (Class<?>)classOrLoaderModuleClassName;
            classOrLoaderModuleClassName = toLoaderModuleClassName(cls);
        }
        return (String)classOrLoaderModuleClassName;
    }

    /**
     * Returns <loader>/<module>/<fully-qualified-classname> string
     * representation of the given class.
     * <p>
     * If the module is a non-upgradeable JDK module then omit
     * its version string.
     * <p>
     * If the loader has no name, or if the loader is one of the built-in
     * loaders (`boot`, `platform`, or `app`) then drop the first element
     * (`<loader>/`).
     * <p>
     * If the first element has been dropped and the module is unnamed
     * then drop the second element (`<module>/`).
     * <p>
     * If the first element is not dropped and the module is unnamed
     * then drop `<module>`.
     */
    private static String toLoaderModuleClassName(Class<?> cls) {
        ClassLoader loader = cls.getClassLoader0();
        Module m = cls.getModule();

        // First element - class loader name
        // Call package-private ClassLoader::name method
        String s = "";
        if (loader != null && loader.name() != null &&
                !(loader instanceof BuiltinClassLoader)) {
            s = loader.name() + "/";
        }

        // Second element - module name and version
        if (m != null && m.isNamed()) {
            s += m.getName();
            // Include version if it is a user module or upgradeable module
            //
            // If it is JDK non-upgradeable module which is recorded
            // in the hashes in java.base, omit the version.
            if (!isHashedInJavaBase(m)) {
                Optional<Version> ov = m.getDescriptor().version();
                if (ov.isPresent()) {
                    String version = "@" + ov.get().toString();
                    s += version;
                }
            }
        }

        // fully-qualified class name
        return s.isEmpty() ? cls.getName() : s + "/" + cls.getName();
    }

    /**
     * Returns true if the module is hashed with java.base.
     * <p>
     * This method returns false when running on the exploded image
     * since JDK modules are not hashed. They have no Version attribute
     * and so "@<version>" part will be omitted anyway.
     */
    private static boolean isHashedInJavaBase(Module m) {
        // return true if module system is not initialized as the code
        // must be in java.base
        if (!VM.isModuleSystemInited())
            return true;

        return Layer.boot() == m.getLayer() && HashedModules.contains(m);
    }

    /*
     * Finds JDK non-upgradeable modules, i.e. the modules that are
     * included in the hashes in java.base.
     */
    private static class HashedModules {
        static Set<String> HASHED_MODULES = hashedModules();

        static Set<String> hashedModules() {
            Module javaBase = Layer.boot().findModule("java.base").get();
            Optional<ModuleHashes> ohashes =
                SharedSecrets.getJavaLangModuleAccess()
                             .hashes(javaBase.getDescriptor());

            if (ohashes.isPresent()) {
                Set<String> names = new HashSet<>(ohashes.get().names());
                names.add("java.base");
                return names;
            }

            return Set.of();
        }

        static boolean contains(Module m) {
            return HASHED_MODULES.contains(m.getName());
        }
    }


    /*
     * Returns an array of StackTraceElements of the given depth
     * filled from the backtrace of a given Throwable.
     */
    static StackTraceElement[] of(Throwable x, int depth) {
        StackTraceElement[] stackTrace = new StackTraceElement[depth];
        for (int i = 0; i < depth; i++) {
            stackTrace[i] = new StackTraceElement();
        }

        // VM to fill in StackTraceElement
        initStackTraceElements(stackTrace, x);

        // ensure the proper StackTraceElement initialization
        for (StackTraceElement ste : stackTrace) {
            ste.buildLoaderModuleClassName();
        }
        return stackTrace;
    }

    /*
     * Returns a StackTraceElement from a given StackFrameInfo.
     */
    static StackTraceElement of(StackFrameInfo sfi) {
        StackTraceElement ste = new StackTraceElement();
        initStackTraceElement(ste, sfi);

        ste.buildLoaderModuleClassName();
        return ste;
    }

    /*
     * Sets the given stack trace elements with the backtrace
     * of the given Throwable.
     */
    private static native void initStackTraceElements(StackTraceElement[] elements,
                                                      Throwable x);
    /*
     * Sets the given stack trace element with the given StackFrameInfo
     */
    private static native void initStackTraceElement(StackTraceElement element,
                                                     StackFrameInfo sfi);

    private static final long serialVersionUID = 6992337162326171013L;
}
