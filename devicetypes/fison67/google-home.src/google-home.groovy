/**
 * Google Home (v.0.0.3)
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
	definition (name: "Google Home", namespace: "fison67", author: "fison67") {
        capability "Actuator"
        capability "Audio Notification"
        capability "Music Player"
        capability "Speech Synthesis"
               
        command "playText", ["string"]
        command "playText", ["string", "number"]
        command "playMp3", ["string"]
        command "playMp3", ["string", "number"]
        command "playTextTogether", ["string", "string"]
        command "playTextTogether", ["string", "string", "number"]
        command "playMp3Together", ["string", "string"]
        command "playMp3Together", ["string", "string", "number"]
	}

	simulator {
	}
    
    preferences {
        input name: "tts", title:"Type a text" , type: "string", required: false, defaultValue: "", description:"TTS"
        input name: "language", title:"Select a language" , type: "enum", required: true, options: ["en", "ko"], defaultValue: "ko", description:""
	}

	tiles {
		multiAttributeTile(name: "mediaMulti", type:"mediaPlayer", width:6, height:4, canChangeIcon: true) {
            tileAttribute("device.status", key: "PRIMARY_CONTROL") {
                attributeState("playing", label:"Playing",  backgroundColor:"#00a0dc")
                attributeState("Ready to cast", label:"Ready to cast")
            }
            tileAttribute("device.status", key: "MEDIA_STATUS") {
                attributeState("playing", label:"Playing", icon:"https://github.com/fison67/GH-Connector/blob/master/images/googleHome-off.png?raw=true", action:"music Player.stop", nextState: "stop", backgroundColor:"#00a0dc")
                attributeState("Ready to cast", label:"Ready to cast", action:"", nextState: "Ready to cast", icon:"https://github.com/fison67/GH-Connector/blob/master/images/googleHome-off.png?raw=true")
            }
            tileAttribute("device.status", key: "PREVIOUS_TRACK") {
                attributeState("status", action:"music Player.previousTrack", defaultState: true)
            }
            tileAttribute("device.status", key: "NEXT_TRACK") {
                attributeState("status", action:"music Player.nextTrack", defaultState: true)
            }
            tileAttribute ("device.level", key: "SLIDER_CONTROL", icon: "st.custom.sonos.unmuted") {
                attributeState("level", action:"music Player.setLevel", icon: "st.custom.sonos.unmuted")
            }
            tileAttribute ("device.mute", key: "MEDIA_MUTED") {
                attributeState("unmuted", action:"music Player.mute", nextState: "muted", icon: "st.custom.sonos.unmuted")
                attributeState("muted", action:"music Player.unmute", nextState: "unmuted", icon: "st.custom.sonos.muted")
            }
            tileAttribute("device.trackDescription", key: "MARQUEE") {
                attributeState("trackDescription", label:"${currentValue}", defaultState: true)
            }
        }
        
        valueTile("times", "device.times", width: 6, height: 2, decoration: "flat") {
            state "val", label:'${currentValue}', defaultState: true
        }
        
        main "mediaMulti"
        details(["mediaMulti", "times"])
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
    
    runIn(10000, initEvent)
}

def initEvent(){
    sendEvent(name:"status", value: "Ready to cast", displayed:false )
    sendEvent(name:"trackDescription", value: "Ready to cast", displayed:false )
}

def setExternalAddress(address){
	log.debug "External Address >> ${address}"
	state.externalAddress = address
}

def setStatus(params){
	log.debug "${params.key} : ${params.data}"
 	switch(params.key){
    case "volume":
    	def tmp = params.data.split("/")
        sendEvent(name:"level", value: tmp[0] as int )
        sendEvent(name:"mute", value: tmp[1] == "true" ? "muted" : "unmuted" )
    	break;
    case "totalStatus":
    	def tmp = params.data.split("/")
        def status = tmp[0]
        
    	def val = ""
        // Status
        if(status == "PLAYING" || status == "BUFFERING"){
        	val = "playing"
        }else if(status == "IDLE"){
        	val = "Ready to cast"
    		sendEvent(name:"times", value: "", displayed:false)
    	//	sendEvent(name:"title", value: "" )
    		sendEvent(name:"trackDescription", value: "Ready to cast", displayed:false )
        }
    	sendEvent(name:"status", value: val )
        
        
        // Time
        if(status == "PLAYING"){
            def time1 = formatSeconds(Double.valueOf(tmp[2] as Double).intValue())
            def time2 = formatSeconds(Double.valueOf(tmp[3] as Double).intValue())
            sendEvent(name:"times", value: time1 + " / " + time2, displayed:false )
            
            // Title
            sendEvent(name:"trackDescription", value: tmp[1] == "undefined" ? "" : tmp[1], displayed:false )
        }
    	break;
    }
//    updateLastTime()
}

def callback(physicalgraph.device.HubResponse hubResponse){
	def msg
    try {
        msg = parseLanMessage(hubResponse.description)
		def jsonObj = new JsonSlurper().parseText(msg.body)
        log.debug jsonObj
        
     //   sendEvent(name:"contact", value: (jsonObj.properties.contact == true ? "closed" : "open"))
     //   sendEvent(name:"battery", value: jsonObj.properties.batteryLevel)
    
     //   updateLastTime()
    } catch (e) {
        log.error "Exception caught while parsing data: "+e;
    }
}

def updated() {
	log.debug "TTS >> " + settings.tts
    
    setLanguage(settings.language)
    
    if(state.lastTTS != settings.tts){
    	makeCommand("tts", [getLanguage(), settings.tts, -1])
    }
    state.lastTTS = settings.tts
    
    
}

def setLanguage(language){
	state.language = language
}

def getLanguage(){
    def lang = state.language
    if(lang == null){
    	lang = "ko"
    }
    return lang
}

def stop(){
	log.debug "stop"
    makeCommand("stop", [])
}

def playText(text){
	log.debug "speak3 >> " + text
	makeCommand("tts", [getLanguage(), text, -1])
}

def playText(text, level){
	log.debug "speak2 >> " + text
	makeCommand("tts", [getLanguage(), text, level])
}

def speak(text) {
	log.debug "speak1 >> " + text
	makeCommand("tts", [getLanguage(), text, -1])
}

def playMp3(String name){
	log.debug "PlayTrack >> " + name.toString() + "(" + name.length() + ")"
	makeCommand("playByName", [name, -1])
}

def playMp3(name, level){
	log.debug "PlayTrack >> " + name + "(" + level + ")"
	makeCommand("playByName", [name, level])
}

def playTextTogether(addresses, text){
	log.debug "playTextTogether >> " + text
	makeCommand2(addresses, "tts", [getLanguage(), text, -1])
}

def playTextTogether(addresses, text, level){
	log.debug "playTextTogether >> " + text
	makeCommand2(addresses, "tts", [getLanguage(), text, level as int])
}

def playMp3Together(addresses, name){
	log.debug "playMp3Together >> " + name
	makeCommand2(addresses, "playByName", [name, -1])
}

def playMp3Together(addresses, name, level){
	log.debug "playMp3Together >> " + name
	makeCommand2(addresses, "playByName", [name, level as int])
}

def playTrack(url){
	log.debug "PlayTrack >> " + url + "(" + ")"
	makeCommand("playURL", [url, -1])
}

def playTrack(url, level){
	log.debug "PlayTrack >> " + url + "(" + level + ")"
	makeCommand("playURL", [url, level])
    
    sendEvent(name:"level", value: level )
}

def playTrackAndResume(url, level){
	log.debug "playTrackAndResume >> " + url
	makeCommand("playURL", [url, level])
    sendEvent(name:"level", value: level )
}

def mute() {
    log.debug "mute"
    makeCommand("mute", true)
    sendEvent(name:"mute", value: "muted" )
}

def unmute() {
    log.debug "unmute"
    makeCommand("mute", false)
    sendEvent(name:"mute", value: "unmuted" )
}

def updateLastTime(){
	def now = new Date().format("yyyy-MM-dd HH:mm:ss", location.timeZone)
    sendEvent(name: "lastCheckin", value: now)
}

def setLevel(level) {
	log.debug "setLevel >> " + level
    double lvl
    try { lvl = (double) level; } catch (e) {
        lvl = Double.parseDouble(level)
    }
    makeCommand("volume", [lvl])
    sendEvent(name:"level", value: level )
}

def makeCommand(type, value){
    def body = [
        "id": state.id,
        "target": state.targetAddress,
        "cmd": type,
        "data": value
    ]
    log.debug body
    def options = makeCommand(body)
    sendCommand(options, null)
}

def makeCommand2(addresses, type, value){
    def body = [
        "id": state.id,
        "target": addresses,
        "cmd": type,
        "data": value
    ]
    log.debug body
    def options = makeCommand(body)
    sendCommand(options, null)
}

def sendCommand(options, _callback){
	def myhubAction = new physicalgraph.device.HubAction(options, null, [callback: _callback])
    sendHubCommand(myhubAction)
}

def makeCommand(body){
	def options = [
     	"method": "POST",
        "path": "/googleHome/control",
        "headers": [
        	"HOST": state.appURL,
            "Content-Type": "application/json;charset=utf-8"
        ],
        "body":body
    ]
    log.debug options
    return options
}

public String formatSeconds(int timeInSeconds){
    int secondsLeft = timeInSeconds % 3600 % 60;
    int minutes = Math.floor(timeInSeconds % 3600 / 60);
    int hours = Math.floor(timeInSeconds / 3600);

    String HH = hours < 10 ? "0" + hours : hours;
    String MM = minutes < 10 ? "0" + minutes : minutes;
    String SS = secondsLeft < 10 ? "0" + secondsLeft : secondsLeft;

    return HH + ":" + MM + ":" + SS;
}

def refresh(){
	log.debug "Refresh"
    def options = [
     	"method": "GET",
        "path": "/devices/get/${state.id}",
        "headers": [
        	"HOST": state.app_url,
            "Content-Type": "application/json"
        ]
    ]
    sendCommand(options, callback)
}

/**
* Base64 Encode a String using Apache commons
*/
def encodeAC(arg){
    Base64 coder = new Base64()
    return new String(coder.encodeBase64(arg.getBytes("utf-8"), false), "utf-8")
}
