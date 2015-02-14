/**
 *  superState	0.1
 *
 *  Copyright 2015 Mike Maxwell
 *  
 *  Device (switch/dimmer/color) state capture and replay utility.
 *	- resulting scene is assigned to a selectable switch
 *  - scene devices are editable post capture (recapture/add/delete)
 *	- option to restore each devices previous state individually (as captured when scene is turned on) when the scene is turned off 
 *
 *	Known issues
 *	- unable to name/rename the app or enable mode selections
 * 	- previously configured device specific option settings seem to not refresh in the App  
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
definition(
    name: "superState",
    namespace: "mmaxwell",
    author: "Mike Maxwell",
    description: "Device state capture, replay and edit tool",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
    page(name: "page1", title: "superState Configuration", nextPage: "page2", uninstall: true) {
    	section("Select devices") {
        	input(
        	    name		: "trigger"
        	    ,title		: "Select switch for scene trigger..."
        	    ,multiple	: false
        	    ,required	: true
        	    ,type		: "capability.switch"
        	)
       		input(
        	    name		: "devices"
        	    ,title		: "Select devices for this scene......"
        	    ,multiple	: true
        	    ,required	: true
            	,type		: "capability.switch"
        	)
     	}
        /* the second section on this page no workie for some reason...
        section("App Options") {
        	label(
        	    name		: "sceneName"
        	    ,title		: "Name of this scene"
        	    ,required	: false
         	)
        	mode(
            	name		: "modesSelected"
                ,title		: "Set for specific mode(s)"
                ,required	: false
            )
        }
        */
     }
	//dynamic page calls
	page(name: "page2", title: "Set device options...",nextPage: "page3", install: false, uninstall: false)
    page(name: "page3", title: "Finalize", install: true, uninstall: false)
 }
def page3() {
	return dynamicPage(name: "page3") {
        def String pText = ""
        def Boolean reSnap  //= this."${stDevice.id}-resnap"
        def String sSettings = settings.toString()
        def String sMembers = state.members.toString()
        
        
        if (state.members == null) {
        	devices.each() { stDevice ->
        		pText = pText + "\n*" + stDevice.displayName
        	}
        } else {
        	log.debug settings.toString()
        	devices.each() { stDevice ->
        		//reSnap = this."${stDevice.id}-resnap"
                //log.debug this."${stDevice.id}-resnap"
                if (sSettings.contains("${stDevice.id}-resnap:true") || !sMembers.contains("${stDevice.id}") ) {
                	pText = pText + "\n*" + stDevice.displayName
                } else {
                	pText = pText + "\n" + stDevice.displayName
                }
            }
        }
        
        section("Final instructions") {
        	paragraph(
             	"Set the capture state of devices marked with '*', then 'Done' to finish!${pText}"
            	)
    	}

    }
}
def page2() {
    return dynamicPage(name: "page2") {
     	devices.each() { stDevice ->
        	def String deviceID = stDevice.id
            section ([hideable: false, hidden: false], "${stDevice.displayName} ") {
                input(
                	name			: "${deviceID}-reset"
                    ,type			: "bool"
                    ,title			: "State restore?"
                    ,description	: null
                    ,defaultValue	: false
                )
                input(
                	name					: "${deviceID}-resnap"
                    ,type					: "bool"
                    ,title					: "Re-snap settings?"
                    ,description			: null
                    ,defaultValue			: false
                    //,refreshAfterSelection	: true
                )
     		}
    	}
    }
}
//*/

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	initialize()
}

def initialize() {
	//turn off scene activation switch so we don't end up outa whack
    //trigger.off()
	subscribe(trigger, "switch.on", onHandler)
    subscribe(trigger, "switch.off", offHandler)
    def Boolean restoreSetting //= this."${stDevice.id}-reset"
    def Boolean reSnap  //= this."${stDevice.id}-resnap"
    def List members = []
    
    if (state.members == null) {
        //log.debug "init new..."
        devices.each { stDevice ->
    		restoreSetting = this."${stDevice.id}-reset"
    		//log.debug "${stDevice.displayName} ${stDevice.id} ${r}"
            members << getNewDeviceSettings(stDevice,restoreSetting)
            //log.debug "${it.displayName} can do:${it.capabilities.toListString()}"
            //log.debug "new:${stDevice.displayName}"
        }
        state.members = members
    } else {
    	//log.debug "update..."
        devices.each { stDevice ->
        	restoreSetting = this."${stDevice.id}-reset"
            reSnap = this."${stDevice.id}-resnap"
   			//check if existing member...
            if (state.members.toListString().contains(stDevice.id)) {
            	//log.debug "member edit: ${stDevice.displayName}"
                if (reSnap) {
                	//"${stDevice.id}-resnap" = false
                    //log.debug "resnap:${stDevice.displayName}"
                	members << getNewDeviceSettings(stDevice,restoreSetting)    
                } else {
                	//log.debug "member no change: ${stDevice.displayName}"
                	state.members.any { member ->
                    	if (member.id == stDevice.id) {
                        	member.r = restoreSetting
                        	members << member
                            //log.debug "save:${stDevice.displayName}"
                            return true //bootleg break
                        }
                    }
                }
            } else {
            	//log.debug "add:${stDevice.displayName}"
                members << getNewDeviceSettings(stDevice,restoreSetting)
            }
        }
        state.members = members
    }
    
    //state.members.each {it ->
    //  	log.debug "sM.p:${it.sM.p} sM.n:${it.sM.n} dM.p:${it.dM.p} dM.n:${it.dM.n}"
    //}
	//log.debug members.inspect()
    
    
}
def onHandler(event) {
	//log.debug "on"
	def stDevice
    state.members.each { member ->
    	//log.debug "id:${member.id}"
    	stDevice = devices.find{it.id == member.id}
        //log.debug stDevice.inspect()
        //save the devices curent settings
        member.sM.p = getSwitchSettings(stDevice)
        if (member.t in [2,3]) {
        	//log.debug "${member.n} is a dimmer"
            member.dM.p = getDimmerSettings(stDevice)
        }
        if (member.t == 3) {
        	//log.debug "${member.n} is a color light"
            member.cM.p = getColorSettings(stDevice)
        }
      //set device to the new settings
      if (member.sM.n.S == "off") {
      	stDevice.off()
      } else {
      	stDevice.on()
        if (member.t in [2,3]) stDevice.setLevel(member.dM.n.L)
        if (member.t == 3 ) {
        	stDevice.setHue(member.cM.n.H)
            stDevice.setSaturation(member.cM.n.S)
            stDevice.setColor(member.cM.n.C)
        }
      }
    }
    //log.debug "on:${state.members.inspect()}"
    //state.members.each {it ->
    //   	log.debug "sM.p:${it.sM.p} sM.n:${it.sM.n} dM.p:${it.dM.p} dM.n:${it.dM.n}"
    //}
}
def offHandler(event) {
	//log.debug "off"
	def stDevice
    state.members.each { member ->
    	stDevice = devices.find{it.id == member.id}
        //have a look at the restore setting
        if (member.r == true) {
        	//restore previous state
            if (member.sM.p.S == "off") stDevice.off()
            else {
            	stDevice.on()
                if (member.t in [2,3]) stDevice.setLevel(member.dM.p.L)
        		if (member.t == 3) {
        			stDevice.setHue(member.cM.p.H)
            		stDevice.setSaturation(member.cM.p.S)
            		stDevice.setColor(member.cM.p.C)
                }
            }
        } else stDevice.off()
	}    
    //log.debug "off:${state.members.inspect()}"
    //state.members.each {it ->
    //   	log.debug "sM.p:${it.sM.p} sM.n:${it.sM.n} dM.p:${it.dM.p} dM.n:${it.dM.n}"
    //}
}

//data methods
def getSwitchSettings(stDevice) {
	def Map thisSwitch = [
        S	: stDevice.currentValue("switch") ?: "off"
    ]
    return thisSwitch
}
def getDimmerSettings(stDevice) {
	def Map thisDimmer = [
        L	: stDevice.currentValue("level") ?: 0
    ]
    return thisDimmer
}
def getColorSettings(stDevice) {
	def Map thisColor = [
    	H	: stDevice.currentValue("hue") ?: 0
        ,S	: stDevice.currentValue("saturation") ?: 0
        ,C	: stDevice.currentValue("color") ?: 0
    ]
    return thisColor
}

def getNewDeviceSettings(stDevice, Boolean restoreWhenOff) {
    def Map sMap = [p:[:], n:[:]] //p, previous settings n, new settings
    def Map dMap = [p:[:], n:[:]]
    def Map cMap = [p:[:], n:[:]]
    def Integer dType
    def Map thisDevice = [
    	id	: stDevice.id
        ,n	: stDevice.displayName
        ,r	: restoreWhenOff	//true: restore to previous settings
        //sM : switch settings
        //dM : dimmer settings
        //cM : color setings
        //t	 : device type 1: switch, 2: dimmer, 3: color
    ]
	def String iCanDo = stDevice.capabilities.toListString()
    //everybody's a switch at least
    sMap.n = getSwitchSettings(stDevice)
    sMap.p = sMap.n
    dType = 1
    thisDevice.sM = sMap
    if (iCanDo.contains("Switch Level")) {
    	dMap.n =  getDimmerSettings(stDevice)
        dMap.p = dMap.n
        thisDevice.dM = dMap
        dType = 2
    }
    if (iCanDo.contains("Color Control")) {
        cMap.n = getColorSettings(stDevice)
        cMap.p = cMap.n
        thisDevice.cM = cMap
        dType = 3
    } 
    thisDevice.t = dType
    return thisDevice
}



