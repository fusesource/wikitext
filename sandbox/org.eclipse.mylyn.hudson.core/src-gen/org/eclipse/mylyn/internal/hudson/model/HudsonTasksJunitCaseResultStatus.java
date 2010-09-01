/*******************************************************************************
 * Copyright (c) 2010 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/

// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 

package org.eclipse.mylyn.internal.hudson.model;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for hudson.tasks.junit.CaseResult-Status.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="hudson.tasks.junit.CaseResult-Status">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="PASSED"/>
 *     &lt;enumeration value="SKIPPED"/>
 *     &lt;enumeration value="FAILED"/>
 *     &lt;enumeration value="FIXED"/>
 *     &lt;enumeration value="REGRESSION"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "hudson.tasks.junit.CaseResult-Status")
@XmlEnum
@SuppressWarnings("all")
public enum HudsonTasksJunitCaseResultStatus {

    PASSED,
    SKIPPED,
    FAILED,
    FIXED,
    REGRESSION;

    public String value() {
        return name();
    }

    public static HudsonTasksJunitCaseResultStatus fromValue(String v) {
        return valueOf(v);
    }

}
