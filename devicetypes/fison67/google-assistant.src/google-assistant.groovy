/**
 *  Google Assistant (v.0.0.4)
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
        input name: "voice", title:"Select a Voice", type: "enum", required: true, options: ["google_ko-KR", "googleTTS_ko-KR-Standard-A", "googleTTS_ko-KR-Standard-B", "googleTTS_ko-KR-Standard-C", "googleTTS_ko-KR-Standard-D", "googleTTS_ko-KR-Wavenet-A", "googleTTS_ko-KR-Wavenet-B", "googleTTS_ko-KR-Wavenet-C", "googleTTS_ko-KR-Wavenet-D",  "naver_dinna", "naver_nara", "naver_kyuri", "naver_jinho", "naver_mijin", "oddcast_dayoung", "oddcast_hyeryun", "oddcast_hyuna", "oddcast_jihun", "oddcast_jimin", "oddcast_junwoo", "oddcast_narae", "oddcast_sena", "oddcast_yumi", "oddcast_yura" ], defaultValue: "naver-kyuri"
	}
    
	simulator {
	}

	tiles {
		multiAttributeTile(name:"status", type: "generic", width: 6, height: 4){
			tileAttribute ("device.status", key: "PRIMARY_CONTROL") {
				attributeState("status", label:'${currentValue}', backgroundColor: "#ffffff", icon: "https://github.com/fison67/GH-Connector/blob/master/images/google-assistant.png?raw=true")
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
        "body": makeMessage(voice, ["text": text])
    ]
    sendCommand(options)
}

def makeMessage(_voice, data){
	def result
    def tmp = _voice.split("_")
    def type = tmp[0]
    def voice = tmp[1]
    switch(type){
    case "google":
    	result = [
        	data:[
                "message": data.text,
                "volume": 0,
                "type": type,
                "language": voice
            ],
            deviceId: state.id.substring(10, state.id.length())
        ]
    	break
     case "googleTTS":
        result = [
        	data:[
                "message": data.text,
                "volume": 0,
                "type": type,
                "person": voice,
                "language": voice.substring(0, 5)
            ],
            deviceId: state.id.substring(10, state.id.length())
        ]
        break
    case "naver":
    	result = [
        	data:[
                "message": data.text,
                "volume": 0,
                "type": type,
                "person": voice,
                "speed": 1
            ],
            deviceId: state.id.substring(10, state.id.length())
        ]
    	break
	case "oddcast":
    	result = [
        	data:[
                "message": data.text,
                "volume": 0,
                "type": type,
                "person": voice
            ],
            deviceId: state.id.substring(10, state.id.length())
        ]
    	break
    }
    log.debug result
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
