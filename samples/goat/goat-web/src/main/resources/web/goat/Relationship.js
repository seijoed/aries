/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
//dojo.provide allows pages use all types declared in this resource
dojo.provide("goat.Relationship");
dojo.require("goat.elements.RelationshipElement");
dojo.require("goat.elements.TriangleDecorator");

dojo.declare("goat.Relationship", [], {

	key: null,
	sscRelationship: null,
	relationshipElements: null,
	theme: null,

constructor : function(sscRelationship, theme) {
	//keep this lightweight.. these can be created ONLY to get the key via the next method.. 
	//all normal constructor logic lives in activate.
	this.sscRelationship = sscRelationship;
	this.relationshipElements=new Array();
	this.theme = theme;
},
getKey : function(){
	if(this.key==null){
		this.key="!"+this.sscRelationship.type+"!"+this.sscRelationship.name;
	}
	return this.key;
},
update : function(sscRelationship){
	console.log("Updating relationship "+this.key+" with new data");
	
	console.log("Removing old elements..");
    console.log(this.relationshipElements);
    //remove the old relationship elements .. 
	dojo.forEach(this.relationshipElements, function(relationshipElement){
        console.log("Removing.. ");
        console.log(relationshipElement);
		relationshipElement.removeSelf();
		//delete relationshipElement;
	},this);
	
	//new array...
	console.log("forgetting about the removed relationship elts");
	this.relationshipElements=new Array();
	
	console.log("switching to the new sscRelationship...");
	this.sscRelationship = sscRelationship;
	
	console.log("kicking self to rebuild relationship elts");
	this.activate();
},
activate : function(){
	//console.log(">activate");
	
	//Create a relationship element for each consuming component. Use the consuming component because it's
	//a 1:1 relationship whereas the providing component may provide the element to many different consuming
	//components.
	
	dojo.forEach(this.sscRelationship.consumedBy, function(component){
		//console.log("processing relationship prov by "+this.sscRelationship.providedBy.id+" to "+component.id);
		
		var r = new goat.elements.RelationshipElement(surface, this.sscRelationship.name, this.sscRelationship.type, components[this.sscRelationship.providedBy.id],components[component.id] );

		console.log("type is " + this.sscRelationship.type);
		//Add a service decorator if it is a service relationship
		if (this.sscRelationship.type == "serviceExport") {
			r.addDecorator(new goat.elements.TriangleDecorator(this.theme,surface));

		} else if (this.sscRelationship.type == "serviceImport") {
			r.addDecorator(new goat.elements.TriangleDecorator(this.theme,surface));

		} else if (this.sscRelationship.type == "Service") {
			r.addDecorator(new goat.elements.TriangleDecorator(this.theme,surface));
		}

		
		//console.log("create of relationship element complete");
		this.relationshipElements.push(r);
	},this);	
	//console.log(this.relationshipElements);
	//console.log("<activate");
}

});