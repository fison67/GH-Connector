/**
 *  GH Connector (v.0.0.7)
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
	 dynamicPage(name: "mainPage", title: "GH Connector", nextPage: null, uninstall: true, install: true) {
   		section("Request New Devices"){
        	input "address", "string", title: "Server address", required: true
        	input "address2", "string", title: "Port forwarding Server address", required: false
        	href url:"http://${settings.address}", style:"embedded", required:false, title:"Local Management", description:"This makes you easy to setup"
        	href url:"http://${settings.address2}", style:"embedded", required:false, title:"External Management", description:"This makes you easy to setup"
        }
        
       	section() {
            paragraph "View this SmartApp's configuration to use it in other places."
            href url:"${apiServerUrl("/api/smartapps/installations/${app.id}/config?access_token=${state.accessToken}")}", style:"embedded", required:false, title:"Config", description:"Tap, select, copy, then click \"Done\""
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
    initEvent()
    setAPIAddress()
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

def setAPIAddress(){
	def list = getChildDevices()
    list.each { child ->
        try{
            child.setAddress(settings.address)
        }catch(e){
        }
    }
}

def initEvent(){
    def list = getChildDevices()
    list.each { child ->
        try{
            child.initEvent()
        }catch(e){
        }
    }
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

def initialize() {
	log.debug "initialize"
    
    def options = [
     	"method": "POST",
        "path": "/settings/api/smartthings",
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
    
    def myhubAction = new physicalgraph.device.HubAction(options, null, [callback: null])
    sendHubCommand(myhubAction)
    
//    updateLanguage()
}

def dataCallback(physicalgraph.device.HubResponse hubResponse) {
    def msg, json, status
    try {
        msg = parseLanMessage(hubResponse.description)
        status = msg.status
        json = msg.json
        log.debug "${json}"
    //    state.latestHttpResponse = status
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

def addVirtualDevice(){
    def data = request.JSON
	def id = data.id
    def name = data.name
    def list = data.list
    
    log.debug("Try >> ADD GoogleHome PlayList id=${id} name=${name}")
	
    def dni = "gh-connector-playlist-" + new Date().getTime(); 
    def dth = "Google Home PlayList";

    def childDevice = addChildDevice("fison67", dth, dni, location.hubs[0].id, [
        "label": name
    ])    
    childDevice.setInfo(settings.address, id, list)
    log.debug "Success >> ADD PlayList DNI=${dni} ${name}"

    def resultString = new groovy.json.JsonOutput().toJson("result":"ok")
    render contentType: "application/javascript", data: resultString

}

def addDevice(){
    def data = request.JSON
    log.debug _data
	def id = data.id
    if(id.contains("calendar-")){
    	log.debug("Try >> ADD Calendar id=${id} name=${googleName}")
        def dni = "gh-connector-" + id.toLowerCase()
        def chlid = getChildDevice(dni)
        if(!child){
            def dth = "Google Calendar";
            def childDevice = addChildDevice("fison67", dth, dni, location.hubs[0].id, [
                "label": data.name_ko
            ])    
            log.debug "Success >> ADD Device DNI=${dni} ${data.name_ko}"

            def resultString = new groovy.json.JsonOutput().toJson("result":"ok")
            render contentType: "application/javascript", data: resultString
        }
    }else{
        def targetAddress = data.address
        def googleName = data.name
        log.debug("Try >> ADD GoogleHome Device id=${id} name=${googleName}")
        def dni = "gh-connector-" + id.toLowerCase()
        def chlid = getChildDevice(dni)
        if(!child){
            def dth = "Google Home";
            def name = id;

            def childDevice = addChildDevice("fison67", dth, dni, location.hubs[0].id, [
                "label": googleName
            ])    
            childDevice.setInfo(settings.address, id, targetAddress)
            log.debug "Success >> ADD Device DNI=${dni} ${googleName}"

            try{ childDevice.setLanguage(settings.selectedLang) }catch(e){}

            def resultString = new groovy.json.JsonOutput().toJson("result":"ok")
            render contentType: "application/javascript", data: resultString
        }
    }
    
    
}

def updateDevice(){
    def data = request.JSON
    def id = data.id
    def dni = "gh-connector-" + id.toLowerCase()
    def chlid = getChildDevice(dni)
    if(chlid){
        chlid.setStatus(data)
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

def auth(){
    def configString = new groovy.json.JsonOutput().toJson("list":{})
    render contentType: "application/javascript", data: configString
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
                access_token:  state.accessToken,
                calendar: [
                	authorized_javaScript_origins: apiServerUrl(""),
                    authorized_redirect_uris: apiServerUrl("/api/smartapps/installations/") + app.id + "/auth"
                ]
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
        path("/addVirtual")                     { action: [POST: "authError"]  }
        path("/delete")                         { action: [POST: "authError"]  }
        path("/auth")                         	{ action: [GET: "auth"]  }

    } else {
        path("/config")                         { action: [GET: "renderConfig"]  }
        path("/list")                         	{ action: [GET: "getDeviceList"]  }
        path("/update")                         { action: [POST: "updateDevice"]  }
        path("/add")                         	{ action: [POST: "addDevice"]  }
        path("/addVirtual")                     { action: [POST: "addVirtualDevice"]  }
        path("/delete")                         { action: [POST: "deleteDevice"]  }
        path("/auth")                         	{ action: [GET: "auth"]  }
    }
}
