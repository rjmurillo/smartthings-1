/* AEON specific micro driver 1.2
 *
 * Variation of the stock SmartThings "Dimmer-Switch" and twack's improved dimmer
 *	--auto re-configure after setting preferences
 *	--alarm indicator capability (using AEON hardware blink function)
 *	
 *
 * Includes:
 *	preferences tile for setting:
 * 		reporting functions (parameter 80)	[ 0:off, 1:hail, 2:report ] set to "Report" for fastest physical updates from the device
 *		control switch type (parameter 120)	[ 0:momentary, 1:toggle, 2:three way] (2 isn't tested, not sure how its suppposed to work)
 *		preconfigured blinker modes			[Blink, Flasher, Strobe]
 *		
 * Mike Maxwell
 * madmax98087@yahoo.com
 * 2014-12-06
 *
	change log
    1.1 2014-12-08
    	-added light state restore to prevent alarm smartapps from turning off the light if it was on when the stobe request was made.
    1.2 2014-12-10
    	-added flash command with parameters for smartapp integration
    
	AEON G2 
	0x20 Basic
	0x25 Switch Binary
	0x2C Scene Actuator Conf
	0x2B Scene Activation
	0x70 Configuration 
	0x72 Manufacturer Specific
	0x73 Powerlevel
	0x77 Node Naming
	0x85 Association
	0x86 Version
	0xEF MarkMark
	0x82 Hail
*/

metadata {
	definition (name: "aeonSwitch", author: "mmaxwell") {
		capability "Actuator"
		capability "Switch"
        capability "Sensor"
        capability "Alarm" 
		capability "Polling"
		capability "Refresh"
        command "flash", ["string"]  //blink,flasher,strobe
        //aeon S2 switch (DSC26103-ZWUS)
        fingerprint deviceId: "0x1001", inClusters: "0x25,0x27,0x2C,0x2B,0x70,0x85,0x72,0x86,0xEF,0x82"
	}
  	preferences {
       	input name: "param80", type: "enum", title: "State change notice:", description: "Type", required: true, options:["Off","Hail","Report"]
        input name: "param120", type: "enum", title: "Set trigger mode:", description: "Switch type", required: true, options:["Momentary","Toggle","Three Way"]
        input name: "blinker", type: "enum", title: "Set blinker mode:", description: "Blinker type", required: false, options:["Blink","Flasher","Strobe"]
    }
	simulator {
		status "on":  "command: 2003, payload: FF"
		status "off": "command: 2003, payload: 00"

		// reply messages
		reply "2001FF,delay 5000,2602": "command: 2603, payload: FF"
		reply "200100,delay 5000,2602": "command: 2603, payload: 00"
		reply "200119,delay 5000,2602": "command: 2603, payload: 19"
		reply "200132,delay 5000,2602": "command: 2603, payload: 32"
		reply "20014B,delay 5000,2602": "command: 2603, payload: 4B"
		reply "200163,delay 5000,2602": "command: 2603, payload: 63"
	}

	tiles {
		standardTile("switch", "device.switch", width: 2, height: 2, canChangeIcon: true, canChangeBackground: true) {
			state "on", label:'${name}', action:"switch.off", backgroundColor:"#79b821", nextState:"turningOff"
			state "off", label:'${name}', action:"switch.on", backgroundColor:"#ffffff", nextState:"turningOn"
			state "turningOn", label:'${name}', backgroundColor:"#79b821"
			state "turningOff", label:'${name}', backgroundColor:"#ffffff"
		}
		standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat") {
			state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
		}
		standardTile("blink", "device.alarm", inactiveLabel: false, decoration: "flat") {
			state "default", label:"", action:"alarm.strobe", icon:"st.secondary.strobe"
		}
		main(["switch"])
        details(["switch","blink","refresh"])
	}
}

def parse(String description) {
	def item1 = [
		canBeCurrentState: false,
		linkText: getLinkText(device),
		isStateChange: false,
		displayed: false,
		descriptionText: description,
		value:  description
	]
	def result
    def cmd = zwave.parse(description, [0x20: 1, 0x25: 1, 0x70: 1, 0x72: 1, 0x73: 1])
	if (cmd) {
		result = createEvent(cmd, item1)
	}
	else {
		item1.displayed = displayed(description, item1.isStateChange)
		result = [item1]
	}
    if (result?.name == 'hail' && hubFirmwareLessThan("000.011.00602")) {
		result = [result, response(zwave.basicV1.basicGet())]
		log.debug "Was hailed: requesting state update"
	}
	//log.debug "Parse returned ${result?.descriptionText}"
	return result
    
}

def zwaveEvent(physicalgraph.zwave.commands.hailv1.Hail cmd) {
	[name: "hail", value: "hail", descriptionText: "Switch button was pressed", displayed: false]
    //log.debug "hail called"
}

def createEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd, Map item1) {
	def result = doCreateEvent(cmd, item1)
	for (int i = 0; i < result.size(); i++) {
		result[i].type = "physical"
	}
	result
}
def zwaveEvent(physicalgraph.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd) {
	[name: "switch", value: cmd.value ? "on" : "off", type: "digital"]
}
def createEvent(physicalgraph.zwave.commands.switchmultilevelv1.SwitchMultilevelReport cmd, Map item1) {
	def result = doCreateEvent(cmd, item1)
	result[0].descriptionText = "${item1.linkText} is ${item1.value}"
	result[0].handlerName = cmd.value ? "statusOn" : "statusOff"
	for (int i = 0; i < result.size(); i++) {
		result[i].type = "digital"
	}
	result
}

def doCreateEvent(physicalgraph.zwave.Command cmd, Map item1) {
	def result = [item1]
	item1.name = "switch"
	item1.value = cmd.value ? "on" : "off"
	item1.handlerName = item1.value
	item1.descriptionText = "${item1.linkText} was turned ${item1.value}"
	item1.canBeCurrentState = true
	item1.isStateChange = isStateChange(device, item1.name, item1.value)
	item1.displayed = item1.isStateChange
	result
}

def zwaveEvent(physicalgraph.zwave.commands.configurationv1.ConfigurationReport cmd) {
	log.debug("Parameter number ${cmd.parameterNumber}, Value: ${cmd.configurationValue}")
}

def createEvent(physicalgraph.zwave.Command cmd,  Map map) {
	// Handles any Z-Wave commands we aren't interested in
	//log.debug "$cmd"
    [:]
}

def on() {
	//reset alarm trigger
    state.alarmTriggered = 0
	//Don't request a config report when advanced reporting is enabled
	if (settings.param80 in ["Hail","Report"]) zwave.basicV1.basicSet(value: 0xFF).format()
    else delayBetween([zwave.basicV1.basicSet(value: 0xFF).format(), zwave.switchMultilevelV1.switchMultilevelGet().format()], 5000)
}

def off() {
    //log.debug "at:${state.alarmTriggered} swf:${state.stateWhenFlashed}"
    
    //override alarm off command from smartApps
    if (state.alarmTriggered == 1 && state.stateWhenFlashed == 1) {
    	state.alarmTriggered = 0
    } else {
    	//Don't request a config report when advanced reporting is enabled
    	if (settings.param80 in ["Hail","Report"]) zwave.basicV1.basicSet(value: 0x00).format()
		else delayBetween ([zwave.basicV1.basicSet(value: 0x00).format(), zwave.switchMultilevelV1.switchMultilevelGet().format()], 5000)
    }
}


def poll() {
	zwave.switchMultilevelV1.switchMultilevelGet().format()
}

def refresh() {
	poll()
}

//built in flasher mode
def flash(type) {
	if (!type) type = settings.blinker
	//AEON blink parameters
	//1: blink duration in seconds 1-255
    //2: cycle time in .1 seconds (50% duty cycle) 1-255
    def pBlink = []
    def boolean isOn = device.currentValue("switch") == "on"
	
    if (isOn) state.stateWhenFlashed = 1
    else state.stateWhenFlashed = 0
	
    //switch (settings.blinker) {
    switch (type) {
		case "Flasher":
        	pBlink.add(10)
            pBlink.add(10)
            break
		case "Strobe":
            pBlink.add(3)
            pBlink.add(2)
            break
		default: //Blink
			if (isOn) {
            	pBlink.add(2)
            	pBlink.add(20)
            }
            else {
            	pBlink.add(1)
            	pBlink.add(20)
 			}
            break
	}
	zwave.configurationV1.configurationSet(configurationValue: pBlink, parameterNumber: 2, size: 2).format()
    
}
//alarm methods

def strobe() {
	state.alarmTriggered = 1
	flash(settings.blinker)
}

def siren() {
	state.alarmTriggered = 1
	flash(settings.blinker)
}

def both()	{
	state.alarmTriggered = 1
	flash(settings.blinker)
}

//capture preference changes
def updated() {
    //log.debug "before settings: ${settings.inspect()}, state: ${state.inspect()}" 
    
    //get requested reporting preferences
    Short p80
    switch (settings.param80) {
		case "Off":
			p80 = 0
            break
		case "Hail":
			p80 = 1
            break
		default:
			p80 = 2	//Report
            break
	}    
    
	//get requested switch function preferences
    Short p120
    switch (settings.param120) {
		case "Momentary":
			p120 = 0
            break
		case "Three Way":
			p120 = 2
            break
		default:
			p120 = 1	//Toggle
            break
	}    
  
	//update if the settings were changed
    if (p80 != state.param80)	{
    	//log.debug "update 80:${p80}"
        state.param80 = p80 
        return response(zwave.configurationV1.configurationSet(configurationValue: [p80], parameterNumber: 80, size: 1).format())
    }
	if (p120 != state.param120)	{
    	//log.debug "update 120:${p120}"
        state.param120 = p120
        return response(zwave.configurationV1.configurationSet(configurationValue: [p120], parameterNumber: 120, size: 1).format())
    }

	//log.debug "after settings: ${settings.inspect()}, state: ${state.inspect()}"
}
