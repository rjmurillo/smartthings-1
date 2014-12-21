/**
 *  Auto Dimmer V1.0
 *
 *  Author: Mike Maxwell
 */
definition(
    name: "Auto Dimmer",
    namespace: "mmaxwell",
    author: "Mike Maxwell",
    description: "Adjust dimmer levels at switch state changes, based on LUX",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet-luminance.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet-luminance@2x.png"
)

preferences {
	section("Monitor the luminosity...") {
		input "lightSensor", "capability.illuminanceMeasurement"
	}
    section("Set the transition LUX level...") {
		input "lux", "enum", title: "LUX?", options:["10","50","100","200"], required: true
	}
    section("Manage these dimmers...") {
		input "dimmers", "capability.switchLevel", multiple: true
	}
    section("Dimmer low level...") {
		input "dLow", "enum", title: "Low%?", options:["20","30","40","50"], required: true
	}
    section("Dimmer high level...") {
		input "dHigh", "enum", title: "High%?", options:["50","60","70","80","90","100"], required: true
	}
}

def installed() {
	//subscribe(lightSensor, "illuminance", illuminanceHandler)
    subscribe(dimmers, "switch.on", dimHandler)
}

def updated() {
	unsubscribe()
	//subscribe(lightSensor, "illuminance", illuminanceHandler)
    subscribe(dimmers, "switch.on", dimHandler)
    
}

def dimHandler(evt) {
    def crntLux = lightSensor.currentValue("illuminance").toInteger()
    
	if (state.lastIlum != dLow && crntLux < lux.toInteger()) {
		state.lastIlum = dLow
        log.debug "set to:${dLow}"
	}
	else if (state.lastIlum != dHigh && crntLux > (lux.toInteger() * 1.2)) {
		state.lastIlum = dHigh
        log.debug "set to:${dHigh}"
	}
    
    def lastIlum = state.lastIlum.toInteger()
    if (lastIlum == 100) lastIlum = 99
 	def dimmer = dimmers.find{it.id == evt.deviceId}

    log.debug "dimmer:${dimmer.displayName} val:${dimmer.currentValue("level")} lastIllum:${state.lastIlum} lux:${crntLux}"
	if (dimmer.currentValue("level") != lastIlum) dimmer.setLevel(lastIlum)
	//dimmer.setLevel(lastIlum)
}

