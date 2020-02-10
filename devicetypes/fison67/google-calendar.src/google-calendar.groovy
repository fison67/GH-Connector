/**
 *  Google Calendar (v.0.0.2)
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
 *
*/


import groovy.json.JsonSlurper

metadata {
	definition (name: "Google Calendar", namespace: "fison67", author: "fison67") {
        capability "Sensor"
		capability "Refresh"
        
        command "getSchedule", ["number"]
        command "getSchedule", ["number", "string"]
        
		attribute "todayCount", "number"
		attribute "tomorrowCount", "number"
        
		attribute "status", "number"
		attribute "today", "string"
		attribute "tomorrow", "string"
        
	}

	preferences {
		input name:"locationSet", type:"enum", title:"Do you want a including a location", options:["on", "off"], defaultValue: "off"
		input name:"descriptionSet", type:"enum", title:"Do you want a including a description", options:["on", "off"], defaultValue: "off"
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
        standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default", label:"", action:"refresh", icon:"st.secondary.refresh"
        }
       
        main (["status"])
      	details(["status",])
	}
}

// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"
}

def setStatus(_data){
    def data = _data.data
	log.debug data
    
    state._today = data.today.toString()
    state._tomorrow = data.tomorrow.toString()
    
    if(data.todayAll == null){
        state._todayAll = [].toString()
    }else{
        state._todayAll = data.todayAll.toString()
    }
    
    if(data.tomorrowAll == null){
        state._tomorrowAll = [].toString()
    }else{
        state._tomorrowAll = data.tomorrowAll.toString()
    }
    
   
    sendEvent(name:"status", value: getScheduleTodayCount(), displayed: false )
    sendEvent(name:"todayCount", value: getScheduleTodayCount() + getScheduleTodayAllCount(), displayed: false )
    sendEvent(name:"tomorrowCount", value: getScheduleTomorrowCount() + getScheduleTomorrowAllCount(), displayed: false )
    
    sendEvent(name:"today", value: getScheduleTodayText() )
    sendEvent(name:"tomorrow", value: getScheduleTomorrowText() )
}

def getScheduleTodayDataList(){
	return new JsonSlurper().parseText(state._today)
}

def getScheduleTodayAllDataList(){
	return new JsonSlurper().parseText(state._todayAll)
}

def getScheduleTomorrowyDataList(){
	return new JsonSlurper().parseText(state._tomorrow)
}

def getScheduleTomorrowyAllDataList(){
	return new JsonSlurper().parseText(state._tomorrowAll)
}

def getScheduleTodayCount(){
    return getScheduleTodayDataList().size()
}

def getScheduleTodayAllCount(){
    return getScheduleTodayAllDataList().size()
}

def getScheduleTomorrowCount(){
    return getScheduleTomorrowyDataList().size()
}

def getScheduleTomorrowAllCount(){
    return getScheduleTomorrowyAllDataList().size()
}

def getScheduleToday(index){
    def data = getScheduleTodayDataList()
    return data[index - 1]
}

def getScheduleAllToday(index){
    def data = getScheduleTodayAllDataList()
    return data[index - 1]
}

def getScheduleTomorrow(index){
    def data = getScheduleTomorrowyDataList()
    return data[index - 1]
}

def getScheduleTomorrowAll(index){
    def data = getScheduleTomorrowyAllDataList()
    return data[index - 1]
}

def getScheduleToday(index, type){
    def data = getScheduleTodayDataList()
    return data[index - 1][type]
}

def getScheduleAllToday(index, type){
    def data = getScheduleTodayAllDataList()
    return data[index - 1][type]
}

def getScheduleTomorrow(index, type){
    def data = getScheduleTomorrowyDataList()
    return data[index - 1][type]
}

def getScheduleTomorrowAll(index, type){
    def data = getScheduleTomorrowyAllDataList()
    return data[index - 1][type]
}

def getScheduleTodayText(){
	return processScheduleText("오늘", getScheduleTodayDataList(), getScheduleTodayAllDataList())
}

def getScheduleTomorrowText(){
	return processScheduleText("내일", getScheduleTomorrowyDataList(), getScheduleTomorrowyAllDataList())
}

def processScheduleText(type, dataList, allDataList){
	def count = dataList.size() + allDataList.size()
    def text = "${type}은 일정이 없습니다."
    if(count > 0){
    	def processCount = 0
    	text = "${type}은 일정이 ${count}개 있습니다. "
    	dataList.each { data ->
        	text = text + data.sDate.split(" ")[1].split(":")[0] + "시부터 " + data.eDate.split(" ")[1].split(":")[0] + "시까지 "
            if(locationSet == "on"){
            	if(data.location){
                	text = text + data.location + "에서 "
                }
            }
            text = text + data.name + " 일정이 있습니다. "
            
            if(descriptionSet == "on"){
            	if(data.description){
                	text = text + " 추가 정보는 " + data.description + " 입니다. "
                }
            }
            
            processCount++
            if(processCount < dataList.size()){
            	text = text + " 그리고 "
            }
        }
        
        if(processCount > 0 && allDataList.size() > 0){
        	text = text + " 그리고 "
        }
    	def processAllCount = 0
        allDataList.each { data ->
            if(locationSet == "on"){
            	if(data.location){
                	text = text + data.location + "에서 "
                }
            }
            text = text + data.name + " 종일 일정이 있습니다. "
            
            if(descriptionSet == "on"){
            	if(data.description){
                	text = text + " 추가 정보는 " + data.description + " 입니다. "
                }
            }
            
            processAllCount++
            if(processAllCount < allDataList.size()){
            	text = text + " 그리고 "
            }
        }
        
        
    }
    return text
}


def refresh(){
	log.debug "Refresh"
	def options = [
     	"method": "GET",
        "path": "/api/states/${state.entity_id}",
        "headers": [
        	"HOST": state.app_url,
            "x-ha-access": state.app_pwd,
            "Content-Type": "application/json"
        ]
    ]
    sendCommand(options)
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
        setStatus(jsonObj.state)
        setUnitOfMeasurement(jsonObj.attributes.unit_of_measurement)
    } catch (e) {
        log.error "Exception caught while parsing data: " + e 
    }
}
