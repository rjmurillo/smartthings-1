/**
 *  tvVS
 *
 *  Copyright 2014 Mike Maxwell
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
	metadata {
	definition (name: "tvVS", namespace: "MikeMaxwell", author: "Mike Maxwell") {
		capability "Switch"
	}
    
    preferences {
        input name: "position", type: "enum", title: "TV Position", description: "TV position to connct to (high, mid, low)", required: true, options:["high", "mid","low"]
	}

	simulator {
		// TODO: define status and reply messages here
	}

	tiles {
    standardTile("switch", "device.switch", width: 2, height: 2, canChangeIcon: true) {
        state "on", label: '${name}', action: "switch.off", icon: "st.switches.switch.on", backgroundColor: "#79b821"
        state "off", label: '${name}', action: "switch.on", icon: "st.switches.switch.off", backgroundColor: "#ffffff"
    }

    standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat") {
        state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
    }

    main "switch"
    details(["switch","refresh"])
}

}

// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"
	// TODO: handle 'switch' attribute

}

// handle commands
def on() {
	log.debug "On"
    sendEvent (name: "switch", value: "on")
}

def off() {
	log.debug "Off"
    sendEvent (name: "switch", value: "off")
}

