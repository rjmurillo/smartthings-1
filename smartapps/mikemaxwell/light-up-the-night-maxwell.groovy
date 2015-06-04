/**
 *  Light Up The Night
 *
 *  Author: SmartThings
 */
definition(
    name: "Light Up the Night (maxwell)",
    namespace: "MikeMaxwell",
    author: "SmartThings",
    description: "Turn your lights on when it gets dark and off when it becomes light again.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet-luminance.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet-luminance@2x.png"
)

preferences {
	section("Monitor the luminosity...") {
		input "lightSensor", "capability.illuminanceMeasurement"
	}
    section("When it's below this LUX level...") {
		input "lux", "enum", title: "LUX?", options:["10","50","100","200"], required: true
	}
	section("Turn on these Lights...") {
		input "lights", "capability.switch", multiple: true
	}
}

def installed() {
	subscribe(lightSensor, "illuminance", illuminanceHandler)
}

def updated() {
	unsubscribe()
	subscribe(lightSensor, "illuminance", illuminanceHandler)
}

def illuminanceHandler(evt) {
	def lastStatus = state.lastStatus
	if (lastStatus != "on" && evt.integerValue < lux.toInteger()) {
		lights.on()
		state.lastStatus = "on"
	}
	else if (lastStatus != "off" && evt.integerValue > (lux.toInteger() * 1.2)) {
		lights.off()
		state.lastStatus = "off"
	}
}