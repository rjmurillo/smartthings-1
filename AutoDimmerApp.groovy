/**
 *  Auto Dimmer V1.3
 *
 *  Author: Mike Maxwell
 	1.1 2014-12-21
    	--updated logging for more clarity
    1.2 2014-12-27
    	--complete rewrite
    1.3 2015-01-08
    	--corrected logic errors (3 lux settings map to 4 lux ranges and dimmer settings)
 
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
    page(name: "page1", title: "AutoDim Configuration", nextPage: "page2", uninstall: true) {
        section {
            input(
            	name		: "luxOmatic",
                title		: "Use this Lumance Sensor...",
                multiple	: false,
                required	: true,
                type		: "capability.illuminanceMeasurement"
            )
            input(
            	name		: "luxDark",
                title		: "Dark transition Lux",
                multiple	: false,
                required	: true,
                type		: "enum",
                options		: ["10","25","50","75","100"]
            )
            input(
            	name		: "luxDusk",
                title		: "Dawn/Dusk transition Lux",
                multiple	: false,
                required	: true,
                type		: "enum",
                options		: ["100","125","150","175","200"]
            )
            input(
            	name		: "luxBright",
                title		: "Bright transition Lux",
                multiple	: false,
                required	: true,
                type		: "enum",
                options		: ["1000","2000","3000"]
            )
			input(
            	name		: "dimmers",
                title		: "Manage these Dimmers...",
                multiple	: true,
                required	: true,
                type		: "capability.switchLevel"
            )
            input(
                name		: "defDark",
                title		: "Default dimmer dark level",
                multiple	: false,
                required	: true,
                type		: "enum",
                options		: ["10","20","30","40","50","60"]
            )
            input(
                name		: "defDusk",
                title		: "Default dimmer dusk/dawn level",
                multiple	: false,
                required	: true,
                type		: "enum",
                options		: ["10","20","30","40","50","60"]
            )
            input(
                name		: "defDay", 
                title		: "Default dimmer day level",
                multiple	: false,
                required	: true,
                type		: "enum",
                options		: ["40","50","60","70","80","90","100"]
            )
			input(
                name		: "defBright", 
                title		: "Default dimmer bright level",
                multiple	: false,
                required	: true,
                type		: "enum",
                options		: ["40","50","60","70","80","90","100"]
            )
            mode(
            	name		: "modeMultiple",
                title		: "Set for specific mode(s)",
                required	: false
            )
        }
    }

    page(name: "page2", title: "Set individual dimmer levels to override defaults", install: true, uninstall: false)

}

def page2() {
    return dynamicPage(name: "page2") {
    	//loop through selected dimmers
        dimmers.each() { dimmer ->
        	def safeName = dimmer.displayName.replaceAll(/\W/,"")
            section ([hideable: true, hidden: true], "${dimmer.displayName} overrides...") {
                input(
                    name		: "${safeName}_dark",
                    title		: "Dark level",
                    multiple	: false,
                    required	: false,
                    type		: "enum",
                    options		: ["10","20","30","40","50","60"]
                )
                input(
                    name		: "${safeName}_dusk", 
                    title		: "Dusk/Dawn level",
                    multiple	: false,
                    required	: false,
                    type		: "enum",
                    options		: ["40","50","60","70","80"]
                )
                input(
                    name		: "${safeName}_day", 
                    title		: "Day level",
                    multiple	: false,
                    required	: false,
                    type		: "enum",
                    options		: ["40","50","60","70","80","90","100"]
                )
                input(
                    name		: "${safeName}_bright", 
                    title		: "Bright level",
                    multiple	: false,
                    required	: false,
                    type		: "enum",
                    options		: ["40","50","60","70","80","90","100"]
                )

			}
    	}
    }
}

def installed() {
   subscribe(dimmers, "switch.on", dimHandler)
}

def updated() {
	unsubscribe()
    subscribe(dimmers, "switch.on", dimHandler)
}

def dimHandler(evt) {
	def newLevel = 0
	//get the dimmer that turned on
	def dimmer = dimmers.find{it.id == evt.deviceId}
    //get his current dim level
    def crntDimmerLevel = dimmer.currentValue("level").toInteger()
    //get currentLux
    def crntLux = luxOmatic.currentValue("illuminance").toInteger()
    def prefVar = dimmer.displayName.replaceAll(/\W/,"")
    def defVar
    if (crntLux < luxDark.toInteger()) {
    	//log.debug "mode:dark"
        prefVar = prefVar + "_dark"
        defVar = defDark
    } else if (crntLux < luxDusk.toInteger()) {
    			//log.debug "mode:dusk"
                prefVar = prefVar + "_dusk"
                defVar = defDusk
  	} else if (crntLux < luxBright.toInteger()) {
    			//log.debug "mode:day"
                prefVar = prefVar + "_day"
                defVar = defDay
    } else {
    	//log.debug "mode:bright"
    	prefVar = prefVar + "_bright"
        defVar = defBright
    }
   
    //def elvisOutput = sampleText ?: 'Viva Las Vegas!'
    if (!this."${prefVar}") log.debug "Auto Dimmer is using defaults..."
    else log.debug "Auto Dimmer is using overrides..."
     
    def newDimmerLevel = (this."${prefVar}" ?: defVar).toInteger()
    
    log.debug "dimmer:${dimmer.displayName}, currentLevel:${crntDimmerLevel}%, requestedValue:${newDimmerLevel}%, currentLux:${crntLux}"
  
    if ( newDimmerLevel != crntDimmerLevel ) dimmer.setLevel(newDimmerLevel)
    
   
}
