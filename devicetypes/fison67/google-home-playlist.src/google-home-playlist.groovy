/**
 * Google Home PlayList (v.0.0.1)
 *
 * MIT License
 *
 * Copyright (c) 2018 fison67@nate.com
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
 */
 
import org.apache.commons.codec.binary.Base64
import groovy.json.JsonSlurper
import java.util.concurrent.TimeUnit


metadata {
	definition (name: "Google Home PlayList", namespace: "fison67", author: "fison67") {
        capability "Switch"
	}

	simulator {
	}
    
    preferences {
  		input name: "repeatMode", title:"Select a repeat mode" , type: "enum", required: true, options: ["REPEAT_OFF", "REPEAT_ALL", "REPEAT_SINGLE", "REPEAT_ALL_AND_SHUFFLE"], defaultValue: "REPEAT_OFF", description:""
    }

	tiles {
		multiAttributeTile(name:"switch", type: "generic", width: 6, height: 4, canChangeIcon: true){
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
                attributeState "on", label:'${name}', action:"off", icon:"st.switches.light.on", backgroundColor:"#00a0dc", nextState:"turningOff"
                attributeState "off", label:'${name}', action:"on", icon:"st.switches.light.off", backgroundColor:"#ffffff", nextState:"turningOn"
                
                attributeState "turningOn", label:'${name}', action:"off", icon:"st.switches.light.on", backgroundColor:"#00a0dc", nextState:"turningOff"
                attributeState "turningOff", label:'${name}', action:"on", icon:"st.switches.light.off", backgroundColor:"#ffffff", nextState:"turningOn"
			}
            
            tileAttribute("device.lastCheckin", key: "SECONDARY_CONTROL") {
    			attributeState("default", label:'Updated: ${currentValue}',icon: "st.Health & Wellness.health9")
            }
		}
        
        main "switch"
        details(["switch"])
	}
}

// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"
}

def setInfo(String appURL, String id, String list) {
	log.debug "${appURL}, ${id}, ${list}"
	state.appURL = appURL
    state.id = id
    state.list = list
}

def setAddress(String appURL){
	state.appURL = appURL
}

def on(){
    makeCommand( getDataList() )
    runIn(10, setRepeatMode)
}

def off(){
    makeCommand( getDataList() )
    runIn(10, setRepeatMode)
}

def setRepeatMode(){
	def repeatMode = getRepeatMode()
	def body = [
        "mode": repeatMode
    ]
    def options = _makeCommand(body, "repeat")
    _sendCommand(options, null)
}

def updated() {
}

def getRepeatMode(){
	def repeatMode = settings.repeatMode
    if(repeatMode == null){
    	repeatMode = "REPEAT_OFF"
    }
    return repeatMode
}

def getDataList(){
    def _result = ""
    def _list = state.list.split(",")
    def resultList = []
    for (def i = 0; i <_list.length; i++) {
    	resultList.push(new String(_list[i].decodeBase64()))
    }
    return resultList
}

def makeCommand(list){
    def body = [
        "list": list,
        "repeatMode": getRepeatMode()
    ]
    def options = _makeCommand(body, "play")
    _sendCommand(options, null)
}

def _sendCommand(options, _callback){
	def myhubAction = new physicalgraph.device.HubAction(options, null, [callback: _callback])
    sendHubCommand(myhubAction)
}

def _makeCommand(body, method){
	def options = [
     	"method": "POST",
        "path": "/googleHome/${state.id}/${method}",
        "headers": [
        	"HOST": state.appURL,
            "Content-Type": "application/json;charset=utf-8"
        ],
        "body":body
    ]
    return options
}
