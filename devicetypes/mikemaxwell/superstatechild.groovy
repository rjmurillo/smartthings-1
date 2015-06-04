metadata {
	definition (name: "superStateChild", namespace: "MikeMaxwell", author: "Mike Maxwell") {
		capability "Switch"
	    command "snap"
        command "overrideScene"
    }
	preferences {
        input name: "stateRestore", type: "bool", title: "Restore device states when scene is turned off?"
    }    
	simulator {
	}
	tiles {
		standardTile("button", "device.switch", width: 2, height: 2, canChangeIcon: true, canChangeBackground: true) {
			state "off", label: 'Off', action: "switch.on", icon: "st.illuminance.illuminance.dark", backgroundColor: "#ffffff" //, nextState: "on"
            //state "overrideScene", label: 'Off', action: "switch.on", icon: "st.illuminance.illuminance.dark", backgroundColor: "#ffffff", nextState: "off"
			state "on", label: 'On', action: "switch.off", icon: "st.illuminance.illuminance.dark", backgroundColor: "#79b821" //, nextState: "off"
        }
        standardTile("snap", "device.switch", width: 1, height: 1, canChangeIcon: false,decoration: "flat") {
            state "default", label: 'Snap', action: "snap", icon: "st.camera.take-photo", backgroundColor: "#ffffff" 
		}
		main "button"
		details "button","snap"
 	}
}
def parse(String description) {
}
def snap(){
	//log.debug "snap request"
    on("snap")
}
//def on(action) {
def on() {
	parent.on(this)
    sendEvent(name: "switch", value: "on")
    /*
    if (!action) {
    	log.debug "superState ${device.displayName}:on"
    	return sendEvent(name: "switch", value: "on", data: "~on~")
    } else {
    	log.debug "superState ${device.displayName}:snap"
    	//return sendEvent(name: "switch", isStateChange: true, value: "on", data: "~${action}~")  //snaps when scene is on don't work
        return sendEvent(name: "switch", value: "on", data: "~${action}~")
    }
    */
}
//def off(action) {
def off() {
	parent.off(this)
    sendEvent(name: "switch", value: "off")
    /*
	if (!action) {
		if ((settings.stateRestore ?: "false") == "true") {
    		log.debug "superState ${device.displayName}:off-restore"
    	    return sendEvent(name: "switch", value: "off", data: "~restore~")   
    	} else {
    		log.debug "superState ${device.displayName}:off"
			return sendEvent(name: "switch", value: "off", data: "~off~")
    	}
    } else {
    	log.debug "superState ${device.displayName}:off-${action}~"
    	return sendEvent(name: "switch", value: "off", data: "~${action}~")
        
    }
    */
}
def overrideScene(){
    off("override")
}

/*
need to test these out on the other end
sendEvent
	isStateChange: true|false
	isPhysical:	true|false
	description: raw event text
	descriptionText: user display text
	name:			event display name
	source:	 APP, APP_COMMAND, COMMAND, DEVICE, HUB, or LOCATION (no idea how this works)
	value:	event value (string)
	displayed: true|false hide from activity panel or not
	data: arbitrary data string
*/