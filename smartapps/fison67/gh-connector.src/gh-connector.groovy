/**
 *  GH Connector (v.0.0.1)
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
 
import groovy.json.JsonSlurper
import groovy.json.JsonOutput

definition(
    name: "GH Connector",
    namespace: "fison67",
    author: "fison67",
    description: "A Connector between Google Home and ST",
    category: "My Apps",
    iconUrl: "https://cdn4.iconfinder.com/data/icons/new-google-logo-2015/400/new-google-favicon-512.png",
    iconX2Url: "https://cdn4.iconfinder.com/data/icons/new-google-logo-2015/400/new-google-favicon-512.png",
    iconX3Url: "https://cdn4.iconfinder.com/data/icons/new-google-logo-2015/400/new-google-favicon-512.png",
    oauth: true
)

preferences {
   page(name: "mainPage")
   page(name: "monitorPage")
   page(name: "langPage")
}


def mainPage() {
	def languageList = ["English", "Korean"]
    dynamicPage(name: "mainPage", title: "GH Connector", nextPage: null, uninstall: true, install: true) {
   		section("Request New Devices"){
        	input "address", "string", title: "Server address", required: true
            input(name: "selectedLang", title:"Select a language" , type: "enum", required: true, options: languageList, defaultValue: "English", description:"Language for DTH")
        	href url:"http://${settings.address}", style:"embedded", required:false, title:"Management", description:"This makes you easy to setup"
        }
        
       	section() {
            paragraph "View this SmartApp's configuration to use it in other places."
            href url:"${apiServerUrl("/api/smartapps/installations/${app.id}/config?access_token=${state.accessToken}")}", style:"embedded", required:false, title:"Config", description:"Tap, select, copy, then click \"Done\""
       	}
    }
}

def langPage(){
	dynamicPage(name: "langPage", title:"Select a Language") {
    	section ("Select") {
        	input "Korean",  title: "Korean", multiple: false, required: false
        }
    }
}

def installed() {
    log.debug "Installed with settings: ${settings}"
    
    if (!state.accessToken) {
        createAccessToken()
    }
    
    initialize()
}

def updated() {
    log.debug "Updated with settings: ${settings}"

    // Unsubscribe from all events
//    unsubscribe()
    // Subscribe to stuff
    initialize()
}

def addMonitorDevice(target, remoteDevice, attr, data){
	log.debug "IR Mapping >> " + target.deviceNetworkId + " >> Target(" + state.selectedDeviceNetworkID + ") Attr >> " + attr
	// Init
	if(state.monitorMap == null){
    	state.monitorMap = [:]
    }
    // Add
    def item = [:]
    item['id'] = target.deviceNetworkId
    item['data'] = data
    state.monitorMap[remoteDevice] = item
    
    log.debug state.monitorMap
    
    unsubscribe(target)
    subscribe(target, attr, stateChangeHandler)
}

def stateChangeHandler(event){
    def deviceNetworkID = event.getDevice().deviceNetworkId
    setStateRemoteDevice(event.name, event.value, getDeviceToNotifyList(deviceNetworkID) )
}

def setStateRemoteDevice(eventName, eventValue, list){
	log.debug "setStateRemoteDevice >> " + eventName + " [" + eventValue + "]"
	for(item in list){
        def targetRemoteDevice = getChildDevice(item.id)
        if(targetRemoteDevice){
            if(eventName == "contact"){
                targetRemoteDevice.setStatus( eventValue == "open" ? (item.data.default == "open" ? "on" : "off") : (item.data.default == "open" ? "off" : "on") )
            }else if(eventName == "power"){
            	targetRemoteDevice.setStatus( (item.data.min <= Float.parseFloat(eventValue) && Float.parseFloat(eventValue) <= item.data.max) ? "on" : "off" )
            }else if(eventName == "presence"){
            	targetRemoteDevice.setStatus( eventValue == "present" ? "on" : "off" )
            }
        }
    }
}

/**
* deviceNetworkID : Reference Device. Not Remote Device
*/
def getDeviceToNotifyList(deviceNetworkID){
	def list = []
	state.monitorMap.each{ targetNetworkID, _data -> 
        if(deviceNetworkID == _data.id){
        	def item = [:]
            item['id'] = 'gh-connector-' + targetNetworkID
            item['data'] = _data.data
            list.push(item)
        }
    }
    return list
}

def updateLanguage(){
    log.debug "Languge >> ${settings.selectedLang}"
    def list = getChildDevices()
    list.each { child ->
        try{
        	child.setLanguage(settings.selectedLang)
        }catch(e){
        	log.error "DTH is not supported to select language"
        }
    }
}

def updateExternalNetwork(){
	log.debug "External Network >> ${settings.externalAddress}"
    def list = getChildDevices()
    list.each { child ->
        try{
        	child.setExternalAddress(settings.externalAddress)
        }catch(e){
        	log.error "DTH is not supported to select external address"
        }
    }
}

def initialize() {
	log.debug "initialize"
    
    def options = [
     	"method": "POST",
        "path": "/settings/smartthings",
        "headers": [
        	"HOST": settings.address,
            "Content-Type": "application/json"
        ],
        "body":[
            "app_url":"${apiServerUrl}/api/smartapps/installations/",
            "app_id":app.id,
            "access_token":state.accessToken
        ]
    ]
    log.debug options
    def myhubAction = new physicalgraph.device.HubAction(options, null, [callback: null])
    sendHubCommand(myhubAction)
    
    updateLanguage()
    updateExternalNetwork()
}

def dataCallback(physicalgraph.device.HubResponse hubResponse) {
    def msg, json, status
    try {
        msg = parseLanMessage(hubResponse.description)
        status = msg.status
        json = msg.json
        log.debug "${json}"
        state.latestHttpResponse = status
    } catch (e) {
        logger('warn', "Exception caught while parsing data: "+e);
    }
}

def getDataList(){
    def options = [
     	"method": "GET",
        "path": "/requestDevice",
        "headers": [
        	"HOST": settings.address,
            "Content-Type": "application/json"
        ]
    ]
    def myhubAction = new physicalgraph.device.HubAction(options, null, [callback: dataCallback])
    sendHubCommand(myhubAction)
}

def addDevice(){
	def id = params.id
    def targetAddress = params.address
    def googleHomeName = params.name
    
    log.debug("Try >> ADD GoogleHome Device id=${id} name=${googleHomeName}")
	
    def dni = "gh-connector-" + id.toLowerCase()
    log.debug("DNI >> " + dni)
    def chlid = getChildDevice(dni)
    if(!child){
        def dth = "Google Home";
        def name = id;
        
        def childDevice = addChildDevice("fison67", dth, dni, location.hubs[0].id, [
            "label": googleHomeName
        ])    
        childDevice.setInfo(settings.address, id, targetAddress)
        log.debug "Success >> ADD Device DNI=${dni} ${googleHomeName}"

        try{ childDevice.setLanguage(settings.selectedLang) }catch(e){}

        def resultString = new groovy.json.JsonOutput().toJson("result":"ok")
        render contentType: "application/javascript", data: resultString
    }
}

def updateDevice(){
    def id = params.id
    log.debug " ID >> " + id
    def dni = "gh-connector-" + id.toLowerCase()
    def chlid = getChildDevice(dni)
    if(chlid){
		chlid.setStatus(params)
    }
    def resultString = new groovy.json.JsonOutput().toJson("result":true)
    render contentType: "application/javascript", data: resultString
}

def deleteDevice(){
	def id = params.id
    def dni = "gh-connector-" + id.toLowerCase()
    
    log.debug "Try >> DELETE child device(${dni})"
    def result = false
    
    def chlid = getChildDevice(dni)
    if(!child){
    	try{
            deleteChildDevice(dni)
            result = true
    		log.debug "Success >> DELETE child device(${dni})"
        }catch(err){
			log.error("Failed >> DELETE child Device Error!!! ${dni} => " + err);
        }
    }
    
    def resultString = new groovy.json.JsonOutput().toJson("result":result)
    render contentType: "application/javascript", data: resultString
}

def getDeviceList(){
	log.debug "getDeviceList"
	def list = getChildDevices();
    def resultList = [];
    list.each { child ->
        def dni = child.deviceNetworkId
        log.debug dni
        resultList.push( dni.substring(13, dni.length()) );
    }
    
    def configString = new groovy.json.JsonOutput().toJson("list":resultList)
    render contentType: "application/javascript", data: configString
}

def authError() {
    [error: "Permission denied"]
}

def renderConfig() {
    def configJson = new groovy.json.JsonOutput().toJson([
        description: "GH Connector API",
        platforms: [
            [
                platform: "SmartThings GH Connector",
                name: "GH Connector",
                app_url: apiServerUrl("/api/smartapps/installations/"),
                app_id: app.id,
                access_token:  state.accessToken
            ]
        ],
    ])

    def configString = new groovy.json.JsonOutput().prettyPrint(configJson)
    render contentType: "text/plain", data: configString
}

mappings {
    if (!params.access_token || (params.access_token && params.access_token != state.accessToken)) {
        path("/config")                         { action: [GET: "authError"] }
        path("/list")                         	{ action: [GET: "authError"]  }
        path("/update")                         { action: [POST: "authError"]  }
        path("/add")                         	{ action: [POST: "authError"]  }
        path("/delete")                         { action: [POST: "authError"]  }

    } else {
        path("/config")                         { action: [GET: "renderConfig"]  }
        path("/list")                         	{ action: [GET: "getDeviceList"]  }
        path("/update")                         { action: [POST: "updateDevice"]  }
        path("/add")                         	{ action: [POST: "addDevice"]  }
        path("/delete")                         { action: [POST: "deleteDevice"]  }
    }
}
