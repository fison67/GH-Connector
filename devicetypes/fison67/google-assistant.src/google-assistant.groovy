/**
 *  Google Assistant (v.0.0.1)
 *
 * MIT License
 *
 * Copyright (c) 2019 fison67@nate.com
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
*/


import groovy.json.JsonSlurper

metadata {
	definition (name: "Google Assistant", namespace: "fison67", author: "fison67") {
        capability "Speech Synthesis"
        capability "Actuator"
	}

	preferences {
    
	}
    
	simulator {
	}

	tiles {
		multiAttributeTile(name:"status", type: "generic", width: 6, height: 4){
			tileAttribute ("device.status", key: "PRIMARY_CONTROL") {
				attributeState("status", label:'${currentValue}', backgroundColor: "#ffffff", icon: "https://cdn4.iconfinder.com/data/icons/small-n-flat/24/calendar-128.png")
			}
            tileAttribute("device.lastCheckin", key: "SECONDARY_CONTROL") {
    			attributeState("default", label:'Last Update: ${currentValue}',icon: "st.Health & Wellness.health9")
            }
		}
       
        main (["status"])
      	details(["status",])
	}
}

// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"
}

def setInfo(String appURL, String id, String targetAddress) {
	log.debug "${appURL}, ${id}, ${targetAddress}"
	state.appURL = appURL
    state.id = id
    state.targetAddress = targetAddress
}

def speak(text){
	log.debug "Speak ${text}"
	def options = [
     	"method": "POST",
        "path": "/assistant/api/message",
        "headers": [
        	"HOST": state.appURL,
            "Content-Type": "application/json"
        ],
        "body": makeMessage("google", ["text": text])
    ]
    sendCommand(options)
}

def makeMessage(type, data){
	def result
    switch(type){
    case "google":
    	result = [
        	data:[
                "message": data.text,
                "volume": 0,
                "type": "google",
                "language": "ko"
            ],
            deviceId: state.id.substring(10, state.id.length())
        ]
    	break
    }
	return result
}

def sendCommand(options){
	def myhubAction = new physicalgraph.device.HubAction(options, null, [callback: callback])
    sendHubCommand(myhubAction)
}

def callback(physicalgraph.device.HubResponse hubResponse){
	def msg, json, status
    try {
        msg = parseLanMessage(hubResponse.description)
        def jsonObj = new JsonSlurper().parseText(msg.body)
        log.debug jsonObj
    } catch (e) {
        log.error "Exception caught while parsing data: " + e 
    }
}
