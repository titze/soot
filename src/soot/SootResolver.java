/* Soot - a J*va Optimization Framework
 * Copyright (C) 2000 Patrice Pominville
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 */

/*
 * Modified by the Sable Research Group and others 1997-1999.  
 * See the 'credits' file distributed with Soot for the complete list of
 * contributors.  (Soot is distributed at http://www.sable.mcgill.ca/soot)
 */


package soot;
import soot.*;
import soot.options.*;

import soot.coffi.*;
import java.util.*;
import java.io.*;
import soot.util.*;
import soot.jimple.*;
import soot.javaToJimple.*;

/** Loads symbols for SootClasses from either class files or jimple files. */
public class SootResolver 
{
    public SootResolver (Singletons.Global g) {
	for(int i=0;i<=maxresolvinglevel;i++) classesToResolve[i]=new LinkedList();
    }

    public static SootResolver v() { return G.v().soot_SootResolver();}
    
    private Set markedClasses = new HashSet();
    private int resolvinglevel=0;
    private final static int maxresolvinglevel=1;
    private LinkedList[] classesToResolve = new LinkedList[maxresolvinglevel+1];
    private boolean mIsResolving = false;
    private InitialResolver initSourceResolver;

    public InitialResolver getInitSourceResolver(){
        if (initSourceResolver == null) {
            initSourceResolver = InitialResolver.v();
        }
        return initSourceResolver;
    }

    /** Creates a new SootResolver. */
    //public SootResolver()
    //{
    //}

    /** Returns a SootClass object for the given className. 
     * Creates a new context class if needed. */
    public SootClass getResolvedClass(String className)
    {
        if(Scene.v().containsClass(className))
            return Scene.v().getSootClass(className);

        SootClass newClass;
        if(mIsResolving) {
            newClass = new SootClass(className);
            Scene.v().addClass(newClass);
        
            markedClasses.add(newClass);
            classesToResolve[resolvinglevel].addLast(newClass);
        } else {
            newClass = resolveClassAndSupportClasses(className);
        }
        
        return newClass;
    }


    /** Resolves the given className and all dependent classes. */
    public SootClass resolveClassAndSupportClasses(String className)
    {
        mIsResolving = true;
	if(resolvinglevel!=0) throw new RuntimeException("resolving level wasn't 0");
        SootClass resolvedClass = getResolvedClass(className);
       
	for(;resolvinglevel<=maxresolvinglevel;resolvinglevel++)
	    while(!classesToResolve[resolvinglevel].isEmpty()) {
            
		ClassSource is;

		SootClass sc = (SootClass) classesToResolve[resolvinglevel].removeFirst();
		className = sc.getName();
           
		is = SourceLocator.v().getClassSource(className);

		//		System.out.println("Resolving "+resolvinglevel+" "+className);

		if( is == null ) {
		    if(!Scene.v().allowsPhantomRefs()) {
			throw new RuntimeException("couldn't find type: " +
						   className + " (is your soot-class-path set properly?)");
		    } else {
			G.v().out.println("Warning: " + className +
					  " is a phantom class!");
			sc.setPhantomClass();
			continue;
		    }
		}
                
		is.resolve( sc );
	    }        
	resolvinglevel=0;
        mIsResolving = false;
        return resolvedClass;
    }

    /** Asserts that type is resolved. */
    public void assertResolvedClassForType(Type type,boolean samelevel)
    {
        if(type instanceof RefType)
            assertResolvedClass(((RefType) type).getClassName(),samelevel);
        else if(type instanceof ArrayType)
            assertResolvedClassForType(((ArrayType) type).baseType,samelevel);
    }

    /** Asserts that class is resolved. */
    public void assertResolvedClass(String className,boolean samelevel)
    {
        if(!Scene.v().containsClass(className))
	    if(samelevel || resolvinglevel<maxresolvinglevel) {
		SootClass newClass = new SootClass(className);
		Scene.v().addClass(newClass);
		
		markedClasses.add(newClass);
		classesToResolve[samelevel ? resolvinglevel : resolvinglevel+1]
		    .addLast(newClass);
		
	    } // else System.out.println("Not resolving "+className);

    }
}


