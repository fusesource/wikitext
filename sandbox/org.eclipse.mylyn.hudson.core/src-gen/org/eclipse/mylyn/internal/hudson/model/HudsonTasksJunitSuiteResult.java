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

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for hudson.tasks.junit.SuiteResult complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="hudson.tasks.junit.SuiteResult">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="case" type="{}hudson.tasks.junit.CaseResult" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="duration" type="{http://www.w3.org/2001/XMLSchema}anyType"/>
 *         &lt;element name="name" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="stderr" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="stdout" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="timestamp" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "hudson.tasks.junit.SuiteResult", propOrder = {
    "_case",
    "duration",
    "name",
    "stderr",
    "stdout",
    "timestamp"
})
@SuppressWarnings("all")
public class HudsonTasksJunitSuiteResult {

    @XmlElement(name = "case")
    protected List<HudsonTasksJunitCaseResult> _case;
    @XmlElement(required = true)
    protected Object duration;
    protected String name;
    protected String stderr;
    protected String stdout;
    protected String timestamp;

    /**
     * Gets the value of the case property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the case property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getCase().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link HudsonTasksJunitCaseResult }
     * 
     * 
     */
    public List<HudsonTasksJunitCaseResult> getCase() {
        if (_case == null) {
            _case = new ArrayList<HudsonTasksJunitCaseResult>();
        }
        return this._case;
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

    /**
     * Gets the value of the timestamp property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the value of the timestamp property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTimestamp(String value) {
        this.timestamp = value;
    }

}
