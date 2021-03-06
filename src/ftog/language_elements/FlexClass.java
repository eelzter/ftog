/*
 * Copyright (C) 2008 Mattias �nstrand.
 * 
 * This file is part of Flex DTO Generator.
 *
 * Flex DTO Generator is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Flex DTO Generator is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Flex DTO Generator.  If not, see <http://www.gnu.org/licenses/>.
 */

package ftog.language_elements;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.log4j.Logger;

import ftog.main.ClassFileUtils;
import ftog.main.FlexCodeFormatting;
import ftog.main.JavaFilenameFilter;
import ftog.refactor.RefactorSingleton;


public class FlexClass implements GeneratedClass {
	private String packageName;
	private String className;
	private String superClassName;
	private String remoteClassPackageName;
	private ArrayList properties;
	private ArrayList constants;
	private HashSet imports;
	private boolean bindable;
	private ArrayList constructorParameters;
	
	
	private int indentionLevel;
	private FlexCodeFormatting format;
	
	private Logger log;
	
	public FlexClass() {
		log = Logger.getLogger(FlexClass.class);
		properties = new ArrayList();
		constants = new ArrayList();
		imports = new HashSet();
		bindable=true;
		constructorParameters=new ArrayList();
	}
	
	/* (non-Javadoc)
	 * @see ftog.language_elements.GeneratedClass#getPackage()
	 */
	public String getPackage() {
		return packageName;
	}
	
	/* (non-Javadoc)
	 * @see ftog.language_elements.GeneratedClass#setPackage(java.lang.String)
	 */
	public void setPackage(String p) {
		packageName = p;
	}
	
	/* (non-Javadoc)
	 * @see ftog.language_elements.GeneratedClass#getClassName()
	 */
	public String getClassName() {
		return className;
	}
	
	/* (non-Javadoc)
	 * @see ftog.language_elements.GeneratedClass#setClassName(java.lang.String)
	 */
	public void setClassName(String c) {
		className = c;
	}

	/* (non-Javadoc)
	 * @see ftog.language_elements.GeneratedClass#addProperty(ftog.language_elements.Property)
	 */
	public void addProperty(Property p) {
		properties.add(p);
	}
	
	/* (non-Javadoc)
	 * @see ftog.language_elements.GeneratedClass#addConstant(ftog.language_elements.Constant)
	 */
	public void addConstant(Constant c) {
		constants.add(c);
	}
	
	/* (non-Javadoc)
	 * @see ftog.language_elements.GeneratedClass#addImport(ftog.language_elements.Import)
	 */
	public void addImport(Import i) {
		imports.add(i);
	}
	
	/* (non-Javadoc)
	 * @see ftog.language_elements.GeneratedClass#addContructorParameter(ftog.language_elements.Property)
	 */
	public void addContructorParameter(Property p) {
		constructorParameters.add(p);
	}
	
	/* (non-Javadoc)
	 * @see ftog.language_elements.GeneratedClass#purgeUnrelatedImports()
	 */
	public void purgeUnrelatedImports() {
		HashSet referencedClasses = new HashSet();
		for(int i=0;i<properties.size();i++) {
			Property p = (Property) properties.get(i);
			referencedClasses.add(p.flexClass);
			referencedClasses.add(p.childType);
		}
		for(int i=0;i<constants.size();i++) {
			Constant c = (Constant) constants.get(i);
			referencedClasses.add(c.flexClass);
		}
		if(superClassName!=null)
			referencedClasses.add(superClassName);
		
		Iterator it = imports.iterator();
		while(it.hasNext()) {
			Import im = (Import) it.next();
			if(!referencedClasses.contains(im.className))
				it.remove();
		}
	}
	
	/* (non-Javadoc)
	 * @see ftog.language_elements.GeneratedClass#getBindable()
	 */
	public boolean getBindable() {
		return bindable;
	}
	
	/* (non-Javadoc)
	 * @see ftog.language_elements.GeneratedClass#setBindable(boolean)
	 */
	public void setBindable(boolean value) {
		bindable=value;
	}
	
	/* (non-Javadoc)
	 * @see ftog.language_elements.GeneratedClass#toCode()
	 */
	public String toCode() {
		Property.childNumber=0;
		purgeUnrelatedImports();
		FlexCodeFormatting format = new FlexCodeFormatting();
		return toCode(format);
	}
	
	/* (non-Javadoc)
	 * @see ftog.language_elements.GeneratedClass#toCode(ftog.main.FlexCodeFormatting)
	 */
	public String toCode(FlexCodeFormatting format) {
		this.format = format;
		StringBuffer code = new StringBuffer();
		addConstants(code);
		addProperties(code);
		addConstructor(code);
		addClassDeclaration(code);
		if(imports.size()>0) {
			insertEmtyRow(code);
			addImports(code);
		}
		insertEmtyRow(code);
		addPackageDeclaration(code);
		return code.toString();
	}
	
	/* (non-Javadoc)
	 * @see ftog.language_elements.GeneratedClass#seemsToBeTransferObject()
	 */
	public boolean seemsToBeTransferObject() {
		return (properties.size()>0 || constants.size()>0 || superClassName!=null);
	}
	
	/* (non-Javadoc)
	 * @see ftog.language_elements.GeneratedClass#sortProprties()
	 */
	public void sortProprties() {
		Collections.sort(properties);
	}
	
	/* (non-Javadoc)
	 * @see ftog.language_elements.GeneratedClass#expandImports(java.lang.String)
	 */
	public void expandImports(String fromDir) {
		Iterator it = imports.iterator();
		while(it.hasNext()) {
			Import im = (Import) it.next();
			log.debug("expandimport:"+im.getFullyQualifiedClassName());
			if("*".equals(im.className)) {
				it.remove();
				expandImport(fromDir, im);
			}
		}
	}
	
	private void expandImport(String fromDir, Import im) {
		String packageDir = im.getFullyQualifiedClassName().replace('.', File.separatorChar);
		//Remove .*	
		packageDir = packageDir.substring(0, packageDir.length()-1);
		File packageDirFile = new File(fromDir+File.separatorChar+packageDir);
		JavaFilenameFilter jff = new JavaFilenameFilter();
		log.debug("packageDirFile:"+packageDirFile);
		String[] files = packageDirFile.list(jff);
		for(int i=0;i<files.length;i++) {
			String f=files[i];
			log.debug("file:"+f);
			//Remove .java
			f = f.substring(0, f.length()-5);
			addImport(new Import(im.path+"."+f));
		}
	}
	
 	/* (non-Javadoc)
	 * @see ftog.language_elements.GeneratedClass#writeToDisk(ftog.main.ClassFileUtils)
	 */
 	public void writeToDisk(ClassFileUtils cfu) throws IOException {
		Writer out = cfu.createDirectoriesAndOpenStream(packageName, className);
		out.write(toCode());
		out.flush();
		out.close();
	}
 	
 	private void addConstructor(StringBuffer code) {
 		//if we don't have any constructor parameters
 		//default constructor is fine...
 		if(constructorParameters.size()==0)
 			return;
 		StringBuffer func = new StringBuffer();
 		func.append("public function ");
 		func.append(className);
 		func.append('(');
 		addConstructorParameters(func);
 		func.append(") {");
 		code.append(indent(func.toString()));
 		indentionLevel++;
 		addInitializerCode(code);
 		indentionLevel--;
 		code.append(indent("}"));
 	}

 	private void addConstructorParameters(StringBuffer code) {
 		Iterator it = constructorParameters.iterator();
 		while(it.hasNext()) {
 			StringBuffer initCode = new StringBuffer();
 		 	Property p = (Property) it.next();
 			initCode.append(p.name);
 			initCode.append(':');
 			initCode.append(p.flexClass);
 			initCode.append('=');
 	 		if("Number".equals(p.flexClass))
 	 			initCode.append("NaN");
 	 		else if ("int".equals(p.flexClass) || "uint".equals(p.flexClass))
 	 			initCode.append('0');
 	 		else
 	 			initCode.append("null");
 	 		
 	 		
 	 		if(it.hasNext())
 	 			initCode.append(", ");
 	 		
 	 		code.append(initCode.toString());
 		}
  	}

 	private void addInitializerCode(StringBuffer code) {
 		Iterator it = constructorParameters.iterator();
 		while(it.hasNext()) {
 			Property p = (Property) it.next();
 	 	 	//if(properties.contains(p)) {
 	 	 		StringBuffer sb = new StringBuffer();
 	 	 		sb.append("this.");
 	 	 		sb.append(p.name);
 	 	 		sb.append('=');
 	 	 		sb.append(p.name);
 	 	 		sb.append(';');
 	 	 		code.append(indent(sb.toString()));
 	 	 	//}
 		}
 	}
 	
	private void addProperties(StringBuffer code) {
		indentionLevel=2;
		Iterator i = properties.iterator();
		while(i.hasNext()) {
			Property p = (Property)i.next();
			code.append(indent(p.toFlexCode()));
		}
	}
	
	private void addConstants(StringBuffer code) {
		indentionLevel=2;
		Iterator i = constants.iterator();
		while(i.hasNext()) {
			Constant c = (Constant)i.next();
			code.append(indent(c.toFlexCode()));
			if(!i.hasNext())
				addEmtyRow(code);
		}
	}
	
	private void addImports(StringBuffer code) {
		Iterator i = imports.iterator();
		while(i.hasNext()) {
			Import im = (Import)i.next();
			code.insert(0, indent("import "+im.getFullyQualifiedClassName()+";"));
		}
	}

	private void addBindable(StringBuffer code) {
		indentionLevel=1;
		code.append(indent("[Bindable]"));
	}

	private void addRemoteClass(StringBuffer code) {
		indentionLevel=1;
		if(remoteClassPackageName!=null)
			code.append(indent("[RemoteClass(alias=\""+remoteClassPackageName+"."+className+"\")]"));
	}

	private void addAnnotations(StringBuffer code) {
		if(bindable)
			addBindable(code);
		
		addRemoteClass(code);
	}
	
	private void addClassDeclaration(StringBuffer code) {
		indentionLevel=1;
		code.insert(0, getClassDeclarationString());
		code.append(indent("}"));
	}
	
	/* (non-Javadoc)
	 * @see ftog.language_elements.GeneratedClass#getSuperClassName()
	 */
	public String getSuperClassName() {
		return superClassName;
	}

	/* (non-Javadoc)
	 * @see ftog.language_elements.GeneratedClass#setSuperClassName(java.lang.String)
	 */
	public void setSuperClassName(String superClassName) {
		this.superClassName = superClassName;
	}

	private String getClassDeclarationString() {
		StringBuffer sb=new StringBuffer();
		addAnnotations(sb);
		StringBuffer sb2 = new StringBuffer();
		sb2.append("public class "+className);
		if(superClassName!=null && !"Object".equals(superClassName))
			sb2.append(" extends "+superClassName);

		sb2.append(" {");
		sb.append(indent(sb2.toString()));
		
		return sb.toString();
	}

	
	private void addPackageDeclaration(StringBuffer code) {
		indentionLevel=0;
		code.insert(0, indent("package "+packageName+" {"));
		code.append(indent("}"));
	}

	private void addToString(StringBuffer code) {

	}
	
	private String indent(String in) {
		StringBuffer sb = new StringBuffer();
		for(int i=0;i<indentionLevel;i++)
			sb.append(format.indentionToken);
		
		sb.append(in);
		sb.append(format.lineTerminator);
		return sb.toString();
	}
	
	private void insertEmtyRow(StringBuffer sb) {
		sb.insert(0, format.lineTerminator);
	}
	
	private void addEmtyRow(StringBuffer sb) {
		sb.append(format.lineTerminator);
	}

	/* (non-Javadoc)
	 * @see ftog.language_elements.GeneratedClass#getRemoteClassPackageName()
	 */
	public String getRemoteClassPackageName() {
		return remoteClassPackageName;
	}

	/* (non-Javadoc)
	 * @see ftog.language_elements.GeneratedClass#setRemoteClassPackageName(java.lang.String)
	 */
	public void setRemoteClassPackageName(String remoteClassName) {
		this.remoteClassPackageName = remoteClassName;
	}
	
	/* (non-Javadoc)
	 * @see ftog.language_elements.GeneratedClass#clearConstructorParameters()
	 */
	public void clearConstructorParameters() {
		constructorParameters.clear();
	}
	
	/* (non-Javadoc)
	 * @see ftog.language_elements.GeneratedClass#applyRefactoring()
	 */
	public void applyRefactoring() {
		Iterator i = imports.iterator();
		while(i.hasNext()) {
			Import im = (Import)i.next();
			String s = RefactorSingleton.getInstance().applyRefactorings(im.getFullyQualifiedClassName());
			im.addImport(s);
		}

		packageName = RefactorSingleton.getInstance().applyRefactorings(packageName);
	}
}
 