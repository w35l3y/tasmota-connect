/**
 *  Tasmota - Virtual Fan
 *
 *  Copyright 2020 Wesley Menezes
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

metadata {
    definition(name: "Tasmota Virtual Fan", namespace: "br.com.wesley", author: "w35l3y", ocfDeviceType: "oic.d.fan") {
        capability "Switch"
        capability "Switch Level"
        capability "Fan Speed"
        capability "Actuator"
        capability "Configuration"
        capability "Health Check"
        capability "Signal Strength"

        attribute "lastSeen", "string"

        command "low"
        command "medium"
        command "high"
        command "max"
        command "raiseFanSpeed"
        command "lowerFanSpeed"
    }

    simulator {
    }

    preferences {
        section {
            input(title: "Device Settings",
                    description: "To view/update this settings, go to the Tasmota (Connect) SmartApp and select this device.",
                    displayDuringSetup: false,
                    type: "paragraph",
                    element: "paragraph")
        }
    }

    tiles(scale: 2) {
        multiAttributeTile(name: "fanSpeed", type: "generic", width: 6, height: 4, canChangeIcon: true) {
            tileAttribute("device.fanSpeed", key: "PRIMARY_CONTROL") {
                attributeState "0", label: "off", action: "raiseFanSpeed", icon: "st.thermostat.fan-off", backgroundColor: "#ffffff", nextState: "1"
                attributeState "1", label: "low", action: "raiseFanSpeed", icon: "st.thermostat.fan-on", backgroundColor: "#00a0dc", nextState: "2"
                attributeState "2", label: "medium", action: "raiseFanSpeed", icon: "st.thermostat.fan-on", backgroundColor: "#00a0dc", nextState: "3"
                attributeState "3", label: "high", action: "raiseFanSpeed", icon: "st.thermostat.fan-on", backgroundColor: "#00a0dc", nextState: "4"
                attributeState "4", label: "max", action: "raiseFanSpeed", icon: "st.thermostat.fan-on", backgroundColor: "#00a0dc", nextState: "0"
            }
            tileAttribute("device.fanSpeed", key: "VALUE_CONTROL") {
                attributeState "VALUE_UP", action: "raiseFanSpeed"
                attributeState "VALUE_DOWN", action: "lowerFanSpeed"
            }
        }

        standardTile("lqi", "device.lqi", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
            state "default", label: 'LQI: ${currentValue}'
        }

        standardTile("rssi", "device.rssi", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
            state "default", label: 'RSSI: ${currentValue}dBm'
        }

        main(["fanSpeed"])
        details(["fanSpeed"])
    }
}

def parse(String description) {
}

def parseEvents(status, json) {
    def events = []
    if (status as Integer == 200) {
        // rfData
        if (json?.rfData) {
            def data = parent.childSetting(device.id, ["payload_off", "payload_low", "payload_medium", "payload_high", "payload_max"])
            def found = data.find{ it?.value?.toUpperCase() == json?.rfData?.toUpperCase() }?.key
            if (found) {
                def rawLevel = 0
                if (found == "payload_low") {
                    rawLevel = 25
                    state.lastFanSpeed = 1
                } else if (found == "payload_medium") {
                    rawLevel = 50
                    state.lastFanSpeed = 2
                } else if (found == "payload_high") {
                    rawLevel = 75
                    state.lastFanSpeed = 3
                } else if (found == "payload_max") {
                    rawLevel = 100
                    state.lastFanSpeed = 4
                }
                def value = (rawLevel ? "on" : "off")
                def fanSpeed = rawLevel ? state.lastFanSpeed : 0
                events << sendEvent(name: "switch", value: value)
                events << sendEvent(name: "level", value: rawLevel)
                events << sendEvent(name: "fanSpeed", value: fanSpeed)
                log.debug "Fan switch: '" + value + "', level: '${rawLevel}', fanSpeed: '${fanSpeed}'"
            }
        }
        // irData
        if (json?.irData) {
            def data = parent.childSetting(device.id, ["payload_off", "payload_low", "payload_medium", "payload_high", "payload_max"])
            def found = data.find{ it?.value?.toUpperCase() == json?.irData?.toUpperCase() }?.key
            if (found) {
                def rawLevel = 0
                if (found == "payload_low") {
                    rawLevel = 25
                    state.lastFanSpeed = 1
                } else if (found == "payload_medium") {
                    rawLevel = 50
                    state.lastFanSpeed = 2
                } else if (found == "payload_high") {
                    rawLevel = 75
                    state.lastFanSpeed = 3
                } else if (found == "payload_max") {
                    rawLevel = 100
                    state.lastFanSpeed = 4
                }
                def value = (rawLevel ? "on" : "off")
                def fanSpeed = rawLevel ? state.lastFanSpeed : 0
                events << sendEvent(name: "switch", value: value)
                events << sendEvent(name: "level", value: rawLevel)
                events << sendEvent(name: "fanSpeed", value: fanSpeed)
                log.debug "Fan switch: '" + value + "', level: '${rawLevel}', fanSpeed: '${fanSpeed}'"
            }
        }
        // Bridge's Signal Strength
        if (json?.Wifi) {
            events << sendEvent(name: "lqi", value: json?.Wifi.RSSI, displayed: false)
            events << sendEvent(name: "rssi", value: json?.Wifi.Signal, displayed: false)
        }
        // Bridge's Last seen
        if (json?.lastSeen) {
            events << sendEvent(name: "lastSeen", value: json?.lastSeen, displayed: false)
        }
    }
    return events
}

def setLevel(value, rate = null) {
    def level = Math.max(Math.min(value as Integer, 100), 0)

    if (75 < level) {
        return max()
    }
    if (50 < level) {
        return high()
    }
    if (25 < level) {
        return medium()
    }
    if (0 < level) {
        return low()
    }
    return off()
}

def setFanSpeed(speed) {
	return setLevel(25 * speed)
}

def raiseFanSpeed() {
    setFanSpeed(((device.currentValue("fanSpeed") as Integer) + 1) % 5)
}

def lowerFanSpeed() {
    setFanSpeed(((device.currentValue("fanSpeed") as Integer) + 4) % 5)
}

def on() {
    if (state?.lastFanSpeed) {
        setFanSpeed(state.lastFanSpeed as Integer)
    } else {
        setFanSpeed(2)
    }
}

def execute (cmd, fanSpeed) {
    def bridge = parent.childSetting(device.id, "bridge") ?: null
    def command = parent.childSetting(device.id, cmd) ?: null
    if (bridge && command) {
        parent.callTasmota(bridge, command)
        sendEvent(name: "switch", value: fanSpeed?"on":"off")
        sendEvent(name: "level", value: 25 * fanSpeed)
        sendEvent(name: "fanSpeed", value: fanSpeed, isStateChange: true)
    }
}

def off() {
	execute("command_off", 0)
}

def low() {
	execute("command_low", 1)
}

def medium() {
	execute("command_medium", 2)
}

def high() {
	execute("command_high", 3)
}

def max() {
	execute("command_max", 4)
}

def ping() {
    // Intentionally left blank as parent should handle this
}

def installed() {
    state?.lastFanSpeed = 2
    sendEvent(name: "checkInterval", value: 30 * 60 + 2 * 60, displayed: false, data: [protocol: "lan", hubHardwareId: device.hub.hardwareID])
    sendEvent(name: "switch", value: "off")
    sendEvent(name: "level", value: 0)
    sendEvent(name: "fanSpeed", value: 0)
}

def updated() {
    initialize()
}

def initialize() {
    if (!state?.lastFanSpeed) { state?.lastFanSpeed = 2 }
}
