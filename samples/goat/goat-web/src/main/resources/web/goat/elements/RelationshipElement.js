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
dojo.provide("goat.elements.RelationshipElement");
dojo.require("dojox.gfx");
dojo.require("dojox.gfx.Moveable");
dojo.require("goat.Component");

//Relationship
// represents a line between two components.
//
//TODO: 
// - add methods for pulse & glow.
// - add methods for line offset adjust & reset
//   - this will allow lines to move to rows of a twistie, then snap back
dojo.declare("goat.elements.RelationshipElement", [], {	
	
	//relationship properties.
	fromComponent: null,
	toComponent: null,
	name: null,	
	type: null,
	
	//object properties	
	surface: null,
	visible: null,
	
	//gfx objects
	line: null,
	
	// helper elements
	decorators: null,
	
	//internals
	stroke: null,
    pulseColor: "#682DAE",
	
	//am I deleted?
	removed: false,
	
	//for the up and coming relationship aspect info.. 
	aspects: null,
	
	subs: null,
	
constructor: function(surface, name, type, fromComponent, toComponent, aspects) {
	this.surface=surface;
	
	//console.log("Building relationship elt for "+name+" "+type+" from "+fromComponent.id+" to "+toComponent.id+" ");
	
	this.name=name;
    this.type=type;
	this.fromComponent=fromComponent;
	this.toComponent=toComponent;
	this.aspects=aspects;
	
	//This need to be replaced with a call to a configuration/theme
	this.stroke = '#6D7B8D';

	this.updateVisibility();
		
	this.subs=new Array();
	this.subs.push(dojo.subscribe("goat.component.move."+fromComponent.id, this, this.onComponentMove));
	this.subs.push(dojo.subscribe("goat.component.move."+toComponent.id, this, this.onComponentMove));
	this.subs.push(dojo.subscribe("goat.component.hidden."+fromComponent.id, this, this.onComponentHidden));
	this.subs.push(dojo.subscribe("goat.component.hidden."+toComponent.id, this, this.onComponentHidden));
	this.subs.push(dojo.subscribe("goat.component.onclick."+toComponent.id, this, this.onComponentClick));
	this.subs.push(dojo.subscribe("goat.component.onclick."+fromComponent.id, this, this.onComponentClick));

    // When a component is resized the relationship line needs to be re-drawn.
    this.subs.push(dojo.subscribe("goat.component.resize."+fromComponent.id, this, this.onComponentResize));
    this.subs.push(dojo.subscribe("goat.component.resize."+toComponent.id, this, this.onComponentResize));
    
	dojo.publish("goat.relationship.create."+fromComponent.id,[this]);
	dojo.publish("goat.relationship.create."+toComponent.id,[this]);

	this.decorators = new Array();

},
addDecorator: function(decorator) {
	decorator.setStroke(this.stroke);
	this.decorators.push(decorator);
},
updateVisibility: function(){
	
	// Visible will be 'true' only if both componenets are not hidden (ie visible)
	this.visible = (!this.fromComponent.hidden) && (!this.toComponent.hidden);
	
	if(!this.visible){
		if(this.line!=null){
		//No need to hide a line that doesn't exist.
			this.line.setShape({x1: -1000, y1: -1000, x2: -1000, y2: -1000});

            //console.log("Hiding decorators..");
			dojo.forEach(this.decorators, function(decorator){
				decorator.makeInvisible();
			},this);

		}
	}else{
		this.updateLine();
	}
},
updateLine: function(){

	if(this.visible){
		var fromx = this.fromComponent.x + (this.fromComponent.width / 2);
		var fromy = this.fromComponent.y + (this.fromComponent.height / 2);
		var tox = this.toComponent.x + (this.toComponent.width / 2);
		var toy = this.toComponent.y + (this.toComponent.height / 2);
			
		if(this.line==null){
			this.line = this.surface.createLine({x1: fromx, y1: fromy, x2: tox, y2: toy})
		            .setStroke(this.stroke);
		}else{
			this.line.setShape({x1: fromx, y1: fromy, x2: tox, y2: toy});
		}

        if (this.decorators != null) {
            dojo.forEach(this.decorators, function(decorator){
                decorator.lineUpdated(this.line);
            },this);
        }

        // Our line should be underneath any decorations
        this.line.moveToBack();
	}
	
},
removeSelf: function(){
	if(!this.removed){
		this.removed = true;
		
        if(this.line!=null) {
            this.surface.remove(this.line);
        }

		dojo.forEach(this.decorators, function(decorator){
			decorator.removeSelf();
		});
        this.decorators = new Array();
		
		//console.log("Removing line subscriptions to components.");
		dojo.forEach(this.subs, function(sub){
			dojo.unsubscribe(sub);
		});
		
		this.subs = new Array();

		if(this.fromComponent.id != this.toComponent.id) {		
			dojo.publish("goat.relationship.remove."+this.fromComponent.id,[this]);
			dojo.publish("goat.relationship.remove."+this.toComponent.id,[this]);
		} else {
        	dojo.publish("goat.relationship.remove."+this.toComponent.id,[this]);
        }

	}else{
		//console.log("Line from "+this.fromComponent.id+" to "+this.toComponent.id+" already marked as deleted");
	}
},
getKey: function(){
	var key = ""+this.fromComponent.id+"!"+this.toComponent.id+"!"+this.type+"!"+this.name;
},
onComponentMove: function(component){
	this.updateLine();
},
onComponentResize: function(component){
	this.updateLine();
},
onComponentHidden: function(component){
	this.updateVisibility();
},
onComponentClick: function(component){

    if(this.line!=null) {
        dojox.gfx.fx.animateStroke({
            shape: this.line,
            duration: 500,
            //color: {start: "#FF3030", end: this.stroke},
            color: {start: this.pulseColor, end: this.stroke},
            width: {start: 3, end: 2},
            join:  {values: ["miter", "bevel", "round"]}
        }).play();	
    }
}
});
