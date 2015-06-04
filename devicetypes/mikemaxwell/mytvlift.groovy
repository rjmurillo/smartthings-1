metadata {
	// Automatically generated. Make future change here.
	definition (name: "myTVLift", namespace: "MikeMaxwell", author: "mmaxwell") {
		//capability "Actuator"
		capability "Switch"
		//capability "Sensor"
        capability "Polling"
        command "highon"
        command "highoff"
        command "midon"
        command "midoff"
        command "lowon"
        command "lowoff"
        attribute "position",  "string"
  	}

	// Simulator metadata
	simulator {
		status "on":  "catchall: 0104 0000 01 01 0040 00 0A21 00 00 0000 0A 00 0A6F6E"
		status "off": "catchall: 0104 0000 01 01 0040 00 0A21 00 00 0000 0A 00 0A6F6666"

		// reply messages
		reply "raw 0x0 { 00 00 0a 0a 6f 6e }": "catchall: 0104 0000 01 01 0040 00 0A21 00 00 0000 0A 00 0A6F6E"
		reply "raw 0x0 { 00 00 0a 0a 6f 66 66 }": "catchall: 0104 0000 01 01 0040 00 0A21 00 00 0000 0A 00 0A6F6666"
	}

	// UI tile definitions
	tiles {
		valueTile("position", "device.position", width: 2, height: 2, decoration: "flat") {
			state "lowon", label: "moving to\nlow"	//, backgroundColor: "#ff8d00"
            state "midon", label: "moving to\nmid"	//, backgroundColor: "#ff8d00"
            state "highon", label: "moving to\nhigh"	//, backgroundColor: "#ff8d00"
			state "lowoff", label: "low"	//, backgroundColor: "#ff8d00"
            state "midoff", label: "mid"	//, backgroundColor: "#ff8d00"
            state "highoff", label: "high"	//, backgroundColor: "#ff8d00"
		}
        standardTile("refresh", "device.main", inactiveLabel: false, decoration: "flat") {
			state "default", action:"poll", icon:"st.secondary.refresh"
		}
		main "position"
		details "position","refresh"
	}
}

// Parse incoming device messages to generate events
def parse(String description) {
	def value = zigbee.parse(description)?.text
 	def name = value in ["highon","highoff","lowon","lowoff","midon","midoff"] ? "position" : null
	def result = createEvent(name: name, value: value)
    if (value != "ping") log.debug "Parse returned: ${result.inspect()}"
	return result
}

// Commands sent to the device
def poll()
{
	zigbee.smartShield(text: "status").format()
}
def highon()
{
	zigbee.smartShield(text: "high").format()
}
def midon()
{
	zigbee.smartShield(text: "mid").format()
}
def lowon()
{
	zigbee.smartShield(text: "low").format()
}
def lowoff()
{
	log.debug "low off no op"
}
def midoff()
{
	log.debug "mid off no op"
}
def highoff()
{
	log.debug "high off no op"
}