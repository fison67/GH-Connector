/**
 * Google Home (v.0.0.18)
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
	definition (name: "Google Home", namespace: "fison67", author: "fison67", vid: "0f759d7b-53d3-3f7a-9063-7bad30c0efc3", mnmn: "fison67") {
        capability "Media Playback"
        capability "Media Track Control"
        capability "Audio Mute"
        capability "Audio Volume"
        capability "Music Player"
        capability "Switch"
        capability "Audio Notification"
        capability "Speech Synthesis"
        
        command "resume"
        command "playTrack", ["string", "number"]
        command "playText", ["string", "number"]
        command "playYoutube", ["string", "number"]
        command "playMp3", ["string", "number"]
        command "playOddcastTTS", ["string", "string", "number"]
        command "playNaverTTS", ["string", "string", "number", "number"]
        command "playGoogleTTS0", ["string", "string", "number", "number"]
        command "playGoogleTTS", ["string", "string", "number"]
        command "playKakaoTTS", ["string", "string", "number"]
        command "playNaverNews", ["string", "number", "string", "number"]
	}

    preferences {
        input name: "tts", title:"Type a tts contents" , type: "string", required: false, defaultValue: "", description:"TTS"
        input name: "ttsType", title:"Select a TTS Type" , type: "enum", required: true, options: ["google", "oddcast", "naver", "kakao", "googleTTS"], defaultValue: "google", description:""
        input name: "ttsOPerson", title:"[ ODDCAST ] Select a Person" , type: "enum", required: true, options: ["dayoung", "hyeryun", "hyuna", "jihun", "jimin", "junwoo", "narae", "sena", "yumi", "yura"], defaultValue: "dayoung", description:""
        input name: "ttsLanguage", title:"[ GOOGLE ] Select a TTS language" , type: "enum", required: true, options: ["ko-KR", "en-US", "en-GB", "en-AU", "en-SG", "en-CA", "de-DE", "fr-FR", "fr-CA", "ja-JP", "es-ES", "pt-BR", "it-IT", "ru-RU", "hi-IN", "th-TH", "id-ID", "da-DK", "no-NO", "nl-NL", "sv-SE"], defaultValue: "ko-KR", description:""
		input name: "ttsNPerson", title:"[ NAVER ] Select a Person" , type: "enum", required: true, options: ["kyuri", "jinho", "mijin", "dsangjin", "djiyun", "clara", "matt", "shinji", "meimei", "liangliang", "jose", "carmen"], defaultValue: "kyuri", description:""
        input name: "googleTTSPerson", title:"[ GOOGLE Cloud ] Select a Person" , type: "enum", required: true, options: ["S-A", "S-B", "S-C", "S-D", "W-A", "W-B", "W-C", "W-D"], defaultValue: "S-A", description:""
		input name: "ttsKPerson", title:"[ KAKAO ] Select a Person" , type: "enum", required: true, options: ["Spring", "Ryan", "Naomi"], defaultValue: "Spring", description:""
		input name: "ttsKEngine", title:"[ KAKAO ] Select a Engine" , type: "enum", required: true, options: ["deep", "plain"], defaultValue: "deep", description:""
	}

}

def installed(){
	sendEvent(name:"supportedPlaybackCommands", value: ["pause", "play", "stop"] )
	sendEvent(name:"playbackStatus", value: "stop" )
	sendEvent(name:"volume", value: 0 )
}

// parse events into attributes
def parse(String description) {}

def setInfo(String appURL, String id, String targetAddress) {
	log.debug "${appURL}, ${id}, ${targetAddress}"
	state.appURL = appURL
    state.id = id
    state.targetAddress = targetAddress
}

def setAddress(String appURL){
	state.appURL = appURL
}

def setExternalAddress(address){
	state.externalAddress = address
}

def playTrackAndResume(url, volume) {
    playTrack(url, -1)
}

def previousTrack(){
	makeCommand("seek", 0)
}

def nextTrack(){
	makeCommand("seek", state.totalTime as Integer)
}

def setMute(mute){
	if(mute == "muted"){
    	mute()
    }else{
    	unmute()
    }
}

def mute() {
    makeCommand("mute", true)
}

def unmute() {
    makeCommand("mute", false)
}

def setVolume(volume){
	log.debug "setVolume"
    makeCommand("volume", [ "volume":volume ])
}

def volumeUp(){
	def volume = device.currentValue("volume") + 1
    if(volume > 100){ volume = 100 }
    setVolume(volume)
}

def volumeDown(){
	def volume = device.currentValue("volume") - 1
    if(volume < 0 ){ volume = 0 }
    setVolume(volume)
}

def setPlaybackStatus(status){
	if(status == "play"){
    	resume()
    }else if(status == "pause"){
    	pause()
    }else if(status == "stop"){
    	stop()
    }
}

def play(){
	resume()
}

def resume(){
	makeCommand("resume", [])
}

def pause(){
	makeCommand("pause", [])
}

def fastForward(){}
def rewind(){}

def on(){}

def off(){
	stop()
}

def setStatus(params){
	log.debug params
 	switch(params.key){
    case "volume":
    	def tmp = params.data.split("/")
        sendEvent(name:"volume", value: tmp[0] as int )
        sendEvent(name:"mute", value: tmp[1] == "true" ? "muted" : "unmuted" )
    	break;
    case "totalStatus":
    	def tmp = params.data.split("/")
        def status = tmp[0]
        
        // Status
        if(status == "PLAYING" || status == "BUFFERING"){
        	sendEvent(name:"switch", value: "on")
        	sendEvent(name:"playbackStatus", value: "play")
            
            if(device.currentValue("trackDescription") != tmp[1]){
            	sendEvent(name:"trackDescription", value: tmp[1])
            }
        }else if(status == "IDLE"){
    		sendEvent(name:"times", value: "", displayed:false)
        	sendEvent(name:"playbackStatus", value: "stop")
        	sendEvent(name:"switch", value: "off")
        	sendEvent(name:"trackDescription", value: "GH Connector")
        }else if(status == "PAUSED"){
        	sendEvent(name:"playbackStatus", value: "pause")
        }
        
        // Time
        if(status == "PLAYING"){
            def time1 = formatSeconds(Double.valueOf(tmp[2] as Double).intValue())
            def time2 = formatSeconds(Double.valueOf(tmp[3] as Double).intValue())
            state.totalTime = Double.valueOf(tmp[3] as Double).intValue()
            sendEvent(name:"times", value: time1 + " / " + time2, displayed:false )
        }
    	break;
    }
}

def callback(physicalgraph.device.HubResponse hubResponse){
	def msg
    try {
        msg = parseLanMessage(hubResponse.description)
		def jsonObj = new JsonSlurper().parseText(msg.body)
    } catch (e) {
        log.error "Exception caught while parsing data: "+e;
    }
}

def updated() {
    
    setTTSLanguage(settings.ttsLanguage)
    
    if(state.lastTTS != settings.tts){
    	if(ttsType == "google"){
    		playGoogleTTS0(settings.tts)
        }else if(ttsType == "oddcast"){
        	playOddcastTTS(settings.tts, settings.ttsOPerson)
        }else if(ttsType == "naver"){
        	playNaverTTS(settings.tts, ttsNPerson)
        }else if(ttsType == "googleTTS"){
        	playGoogleTTS(settings.tts, _getGoogleTTSPerson(googleTTSPerson))
        }else if(ttsType == "kakao"){
            playKakaoTTS(settings.tts, settings.ttsKEngine, settings.ttsKPerson)
        }
    }
    state.lastTTS = settings.tts
}

def setTTSLanguage(language){
	state.ttsLanguage = language
}

def setLanguage(language){
	state.language = language
}

def getTTSLanguage(){
    def lang = state.ttsLanguage
    if(lang == null){
    	lang = "ko-KR"
    }
    return lang
}

def stop(){
    makeCommand("stop", [])
}

def playText(text, volume=-1){
	playNaverTTS(text, volume)
}

def playGoogleTTS0(text, lang="ko", speed=1, volume=-1){
    makeCommand("tts", [ 
    	"type":"google", 
        "volume":volume , 
        "text":text, 
        "data":[
            "lang":lang,
            "speed":speed
         ] 
    ])
}

def playOddcastTTS(text, voice="sena", volume=-1){
    makeCommand("tts", [ 
    	"type":"oddcast", 
        "volume":volume , 
        "text":text, 
        "data":[
            "voice":voice
         ] 
    ])
}

def playNaverTTS(text, voice="kyuri", speed=0, volume=-1){
    makeCommand("tts", [ 
    	"type":"naver", 
        "volume":volume , 
        "text":text, 
        "data":[
            "voice":voice,
            "speed":speed
         ] 
    ])
}

def playGoogleTTS(text, voice, volume=-1){
	def personValue = _getGoogleTTSPerson(voice)
    makeCommand("tts", [ 
    	"type":"google2", 
        "volume":volume , 
        "text":text, 
        "data":[
            "voice":voice,
            "lang": settings.ttsLanguage
         ] 
    ])
}

def playNaverNews(type="속보", count=1, voice="dinna", volume=-1){
	log.debug "${type}, ${count}, ${voice}, ${volume}"
    makeCommand("naver-news", [
    	"data":[
        	"type":type, 
            "count":count,
            "voice":voice, 
            "volume": volume
        ]
    ])
}

def playKakaoTTS(text, engine="deep", voice="Spring", tone="default", volume=-1){
    makeCommand("tts", [
    	"type":"kakao", 
        "volume":volume , 
        "text":text, 
        "data":[
            "engine":engine, 
            "voice":voice, 
            "tone":tone
        ]
   ])
}

def _getGoogleTTSPerson(name){
	def tmp = name.split("-")
    def type = "Standard"
    if(tmp[0] == "W"){
   	 	type = "Wavenet"
    }
	return settings.ttsLanguage + "-${type}-${tmp[1]}"
}

def speak(text) {
	log.debug "Speak >> ${text}"
    switch(ttsType){
    case "google":
		makeCommand("tts", [getTTSLanguage(), text, -1])
    	break
    case "oddcast":
        playOddcastTTS(text, settings.ttsOPerson)
    	break
    case "naver":
        playNaverTTS(text, ttsNPerson)
    	break
    case "kakao":
    	playKakaoTTS(text, ttsKEngine)
    	break
    case "googleTTS":
        playGoogleTTS(text, _getGoogleTTSPerson(googleTTSPerson))
    	break
    }
}

def playYoutube(String ids, volume=-1){
    makeCommand("youtube", [
        "list":ids.split(","), 
        "volume": volume
    ])
}

def playMp3(name, volume=-1){
	makeCommand("playByName", [
        "name":name, 
        "volume": volume
    ])
}

def playTrack(url, volume=-1){
	makeCommand("playURL", [
        "url":url, 
        "volume": volume
    ])
}

def makeCommand(type, value){
    def body = [
        "id": state.id,
        "target": state.targetAddress,
        "cmd": type,
        "data": value
    ]
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
        "path": "/googleHome/api/control",
        "headers": [
        	"HOST": parent._getServerURL(),
            "Content-Type": "application/json;charset=utf-8"
        ],
        "body":body
    ]
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
    def options = [
     	"method": "GET",
        "path": "/devices/get/${state.id}",
        "headers": [
        	"HOST": parent._getServerURL(),
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
