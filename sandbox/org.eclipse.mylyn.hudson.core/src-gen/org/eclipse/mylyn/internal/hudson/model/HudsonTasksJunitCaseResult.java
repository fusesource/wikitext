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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for hudson.tasks.junit.CaseResult complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="hudson.tasks.junit.CaseResult">
 *   &lt;complexContent>
 *     &lt;extension base="{}hudson.tasks.test.TestResult">
 *       &lt;sequence>
 *         &lt;element name="age" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="className" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="duration" type="{http://www.w3.org/2001/XMLSchema}anyType"/>
 *         &lt;element name="errorDetails" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="errorStackTrace" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="failedSince" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="name" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="skipped" type="{http://www.w3.org/2001/XMLSchema}boolean"/>
 *         &lt;element name="status" type="{}hudson.tasks.junit.CaseResult-Status" minOccurs="0"/>
 *         &lt;element name="stderr" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="stdout" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "hudson.tasks.junit.CaseResult", propOrder = {
    "age",
    "className",
    "duration",
    "errorDetails",
    "errorStackTrace",
    "failedSince",
    "name",
    "skipped",
    "status",
    "stderr",
    "stdout"
})
@SuppressWarnings("all")
public class HudsonTasksJunitCaseResult
    extends HudsonTasksTestTestResult
{

    protected int age;
    protected String className;
    @XmlElement(required = true)
    protected Object duration;
    protected String errorDetails;
    protected String errorStackTrace;
    protected int failedSince;
    protected String name;
    protected boolean skipped;
    protected HudsonTasksJunitCaseResultStatus status;
    protected String stderr;
    protected String stdout;

    /**
     * Gets the value of the age property.
     * 
     */
    public int getAge() {
        return age;
    }

    /**
     * Sets the value of the age property.
     * 
     */
    public void setAge(int value) {
        this.age = value;
    }

    /**
     * Gets the value of the className property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getClassName() {
        return className;
    }

    /**
     * Sets the value of the className property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setClassName(String value) {
        this.className = value;
    }

    /**
     * Gets the value of the duration property.
     * 
     * @return
     *     possible object is
     *     {@link Object }
     *     
     */
    public Object getDuration() {
        return duration;
    }

    /**
     * Sets the value of the duration property.
     * 
     * @param value
     *     allowed object is
     *     {@link Object }
     *     
     */
    public void setDuration(Object value) {
        this.duration = value;
    }

    /**
     * Gets the value of the errorDetails property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getErrorDetails() {
        return errorDetails;
    }

    /**
     * Sets the value of the errorDetails property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setErrorDetails(String value) {
        this.errorDetails = value;
    }

    /**
     * Gets the value of the errorStackTrace property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getErrorStackTrace() {
        return errorStackTrace;
    }

    /**
     * Sets the value of the errorStackTrace property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setErrorStackTrace(String value) {
        this.errorStackTrace = value;
    }

    /**
     * Gets the value of the failedSince property.
     * 
     */
    public int getFailedSince() {
        return failedSince;
    }

    /**
     * Sets the value of the failedSince property.
     * 
     */
    public void setFailedSince(int value) {
        this.failedSince = value;
    }

    /**
     * Gets the value of the name property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the value of the name property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setName(String value) {
        this.name = value;
    }

    /**
     * Gets the value of the skipped property.
     * 
     */
    public boolean isSkipped() {
        return skipped;
    }

    /**
     * Sets the value of the skipped property.
     * 
     */
    public void setSkipped(boolean value) {
        this.skipped = value;
    }

    /**
     * Gets the value of the status property.
     * 
     * @return
     *     possible object is
     *     {@link HudsonTasksJunitCaseResultStatus }
     *     
     */
    public HudsonTasksJunitCaseResultStatus getStatus() {
        return status;
    }

    /**
     * Sets the value of the status property.
     * 
     * @param value
     *     allowed object is
     *     {@link HudsonTasksJunitCaseResultStatus }
     *     
     */
    public void setStatus(HudsonTasksJunitCaseResultStatus value) {
        this.status = value;
    }

    /**
     * Gets the value of the stderr property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getStderr() {
        return stderr;
    }

    /**
     * Sets the value of the stderr property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setStderr(String value) {
        this.stderr = value;
    }

    /**
     * Gets the value of the stdout property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getStdout() {
        return stdout;
    }

    /**
     * Sets the value of the stdout property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setStdout(String value) {
        this.stdout = value;
    }

}
