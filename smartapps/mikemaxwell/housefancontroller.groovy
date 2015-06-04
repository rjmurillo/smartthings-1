/**
 *  houseFanController V1.0
 *
 *  Author: Mike Maxwell
 
 */
definition(
    name		: "houseFanController",
    namespace	: "MikeMaxwell",
    author		: "Mike Maxwell",
    description	: "Runs whole house fan.",
    category	: "Convenience",
    iconUrl		: "https://s3.amazonaws.com/smartapp-icons/Developers/whole-house-fan.png",
    iconX2Url	: "https://s3.amazonaws.com/smartapp-icons/Developers/whole-house-fan@2x.png"
)

preferences {
        section ("Fan related...") {
            input(
            	name		: "fan"
                ,title		: "aeon Fan switch"
                ,multiple	: false
                ,required	: true
                ,type		: "capability.switch"
            )
            
            input(
                name		: "loadSensor"
                ,title		: "ST multi"
                ,multiple	: false
                ,required	: true
                ,type		: "capability.threeAxis"
            )
		}
        section ("Control related...") {
        	input(
          		name		: "contacts"
            	,title		: "Select contact sensors for fan control..."
            	,multiple	: true
            	,required	: true
            	,type		: "capability.contactSensor"
        	)
            input(
            	name		: "thermostat"
                ,title		: "select house thermostat"
                ,multiple	: false
                ,required	: true
                ,type		: "capability.thermostat"
            )
            input(
          		name		: "externalTemp"
            	,title		: "Select external reference temperature sensor"
            	,multiple	: false
            	,required	: true
            	,type		: "capability.temperatureMeasurement"
        	)
            input(
          		name		: "internalTemp"
            	,title		: "Select internal temperature sensor"
            	,multiple	: true
            	,required	: true
            	,type		: "capability.temperatureMeasurement"
        	)
            
            
        }
       	section ("Fan set points...") {
        	input(
                name		: "fanLowTemp" 
                ,title		: "Fan low speed setpoint degrees."
                ,multiple	: false
                ,required	: true
                ,type		: "enum"
                ,options	: ["70","71","72","73","74","75","76","78","79"]
            )
        	input(
                name		: "fanHighTemp" 
                ,title		: "Fan high speed setpoint degrees"
                ,multiple	: false
                ,required	: true
                ,type		: "enum"
                ,options	: ["80","81","82","83","84","85","86","87"]
            )
            input(
                name		: "fanEnableOffset" 
                ,title		: "Internal/external enable offset degrees"
                ,multiple	: false
                ,required	: true
                ,type		: "enum"
                ,options	: ["3","4","5","6","7","8","9","10"]
            )
            
        }
}



def installed() {
	init()
}

def updated() {
	unsubscribe()
    init()
}

def init() {
    subscribe(loadSensor,"threeAxis",loadHandler)
    subscribe(contacts,"contact",contactHandler)
    subscribe(internalTemp,"temperature",internalTempHandler)
    subscribe (fan,"switch",fanHandler)
    //subscribe (thermostat,"",statHandler)
    subscribe(app, appTouch)
    state.tempEnable = false
    state.contactEnable = false
}
def appTouch(evt) {
	//def tempEnable = false
    //def contactEnable = false
	def stats = [:]
    def set = ["low":settings.fanLowTemp.toInteger(),"high":settings.fanHighTemp.toInteger(),"delta":settings.fanEnableOffset.toInteger()]
    def stat = [:]
    //loadSensor
    stat = ["loadSensor":loadSensor.currentValue("threeAxis")]
    stats << stat
    //externalTemp
    def eTemp = externalTemp.currentValue("temperature").toInteger()
    stat = ["externalTemp":eTemp]
    stats << stat
    
    //internalTemps
    stat = ["internalTemps":internalTemp.currentValue("temperature")]
    stats << stat
    
    //temp enable section
    def avgT = internalTemp.currentValue("temperature").sum() / internalTemp.currentValue("temperature").size()
    stat = ["avg":avgT.toInteger()]
    stats << stat
    if (eTemp + set.delta <= avgT) {
		if (avgT > set.high) {
    		//set fan to high
        	stat = ["tempAction":"Temp met (High)"]
            state.tempEnable = true
    	} else if (avgT >= set.low) {
    		//set fan to low
        	stat = ["tempAction":"Temp met (Low)"]
            state.tempEnable = true
    	} else {
    		//turn fan off
        	stat = ["tempAction":"None (set point is met)"]
            state.tempEnable = false
    	}
    } else {
    	stat = ["tempAction":"None (failed delta check)"]
        state.tempEnable = false
    }
    stats << stat
    stat = ["tempEnable":"${state.tempEnable}"]
    stats << stat
    
    //contact enable section
    if (contacts.currentValue("contact").contains("open")) {
        state.contactEnable = true
    } else {
    	state.contactEnable = false
    }
    log.info "contacts: ${contacts.currentValue("contact")}"
	stat = ["contactEnable":"${state.contactEnable}"]    
    stats << stat
    
    //house thermostat section
    /*
	thermostatMode 				String "auto" "emergency heat" "heat" "off" "cool" 
	thermostatFanMode 			String "auto" "on" "circulate" 
	thermostatOperatingState 	String "heating" "idle" "pending cool" "vent economizer" "cooling" "pending heat" "fan only" 
    */
    stat = ["statMode":"${thermostat.currentValue("thermostatMode")}"]
    stats << stat
	stat = ["fanMode":"${thermostat.currentValue("thermostatFanMode")}"]
    stats << stat
    stat = ["statState":"${thermostat.currentValue("thermostatOperatingState")}"]
    stats << stat

    
    //fan control
    if (state.contactEnable && state.tempEnable) {
    	stat = ["fanAction":"fanOn"]
    	fan.on()
    } else {
    	stat = ["fanAction":"fanOff"]
    	fan.off()
    }
    stats << stat
    
    
    
    
    log.info "set:${set}"
	log.info "stats:${stats}"
}
def fanHandler(evt){
	log.info "fantHandler- name:${evt.displayName} value:${evt.value}"
}

def loadHandler(evt){
	//log.info "loadHandler- name:${evt.displayName} value:${evt.value}"
    //def parts = description.split(',')
    def xyz = evt.value.split(',')
    def z = xyz[2].toInteger()
    if (z <= 50) {
    	//off
        log.info "loadHandler- state:OFF value:${evt.value}"
    } else if (z >=275) {
    	//high
        log.info "loadHandler- state:HIGH value:${evt.value}"
    } else {
    	//low
        log.info "loadHandler- state:LOW value:${evt.value}"
    }
}

def contactHandler(evt){
	log.info "contactHandler- name:${evt.displayName} value:${evt.value}"
    appTouch()
}

def internalTempHandler(evt){
	log.info "internalTempHandler- name:${evt.displayName} value:${evt.value}"
    appTouch()
    
    
}


/*
def dimHandler(evt) {
	def newLevel = 0
    
	//get the dimmer that's been turned on
	def dimmer = dimmers.find{it.id == evt.deviceId}
    
    //get its current dim level
    def crntDimmerLevel = dimmer.currentValue("level").toInteger()
    
    //get currentLux reading
    def crntLux = luxOmatic.currentValue("illuminance").toInteger()
    def prefVar = dimmer.displayName.replaceAll(/\W/,"")
    def dimVar
    if (crntLux < luxDark.toInteger()) {
    	//log.debug "mode:dark"
        prefVar = prefVar + "_dark"
        dimVar = dimDark
    } else if (crntLux < luxDusk.toInteger()) {
    			//log.debug "mode:dusk"
                prefVar = prefVar + "_dusk"
                dimVar = dimDusk
  	} else if (crntLux < luxBright.toInteger()) {
    			//log.debug "mode:day"
                prefVar = prefVar + "_day"
                dimVar = dimDay
    } else {
    	//log.debug "mode:bright"
    	prefVar = prefVar + "_bright"
        dimVar = dimBright
    }
   
    if (!this."${prefVar}") log.debug "Auto Dimmer is using defaults..."
    else log.debug "Auto Dimmer is using overrides..."
     
    def newDimmerLevel = (this."${prefVar}" ?: dimVar).toInteger()
	if (newDimmerLevel == 100) newDimmerLevel = 99
    
    log.debug "dimmer:${dimmer.displayName}, currentLevel:${crntDimmerLevel}%, requestedValue:${newDimmerLevel}%, currentLux:${crntLux}"
  
    if ( newDimmerLevel != crntDimmerLevel ) dimmer.setLevel(newDimmerLevel)
    
   
}
*/