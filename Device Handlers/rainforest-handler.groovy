/**
 *  Rainforest Eagle - Device handler, requires Rainforest Eagle Service Manager
 *
 *
 *  heavily adapted from wattvision device type
 *  https://github.com/SmartThingsCommunity/SmartThingsPublic/blob/master/devicetypes/smartthings/wattvision.src/wattvision.groovy
 *
 *  adapted from Rainforest Eagle - Device handler
 *  https://github.com/augoisms/smartthings/tree/master/devicetypes/augoisms/rainforest-handler.src
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */

metadata {

	definition(name: "RainforestEagle", namespace: "freakapotamus", author: "AshCorp") {
		capability "Power Meter"
		capability "Refresh"
		capability "Sensor"
        
        attribute "lastUpdated", "String"
        attribute "price", "string"
	}

	tiles(scale:2) {

        valueTile("mainPower", "device.power", width: 0, height: 0) {
			state "default", label: '${currentValue} kW'
		}

		valueTile("power", "device.power", width: 6, height: 2) {
			state "default", label: 'Current Demand\n\r${currentValue} kW'
		}

        valueTile("price", "device.price", width: 6, height: 2) {
			state "default", label: 'Est. Cost per hour\n\r${currentValue} c'
		}

		standardTile("refresh", "capability.refresh", width: 2, height: 2, inactiveLabel: true, decoration: "flat") {
			state "default", label: '', action: "refresh.refresh", icon: "st.secondary.refresh"
		}
        
        valueTile("lastUpdated", "device.lastUpdated", width: 4, height: 2, decoration: "flat") {
        	state "default", label:'Last Update\n\r${currentValue}'
	    }

		main "mainPower"
        
		details(["mainPower", "power", "price", "lastUpdated", "refresh"])

	}
}

def refresh() {
	log.debug "refresh()"
    
    instantaneousDemand()
}

def instantaneousDemandCallback(response) {
	log.debug "instantaneousDemand response"
    //log.debug response.headers
    
    log.debug "status: ${response?.status}"
    
    def successCodes = ["200","201","202"]
	boolean success = successCodes.findAll{response?.status?.toString().contains(it)}
    log.debug "success: $success"
    
    if(success) {
    	def json = parseJson(response.body)
		addInstDemandData(json)
    } else {
    	error()
    }
    
}

def instantaneousDemand(){

    def settings = parent.getSettings(this)
    //log.debug "settings"
    //log.debug settings
    
    def address = settings.theAddr
    def macId = settings.macId
    def instCode = settings.instCode
    
    def pre = "${settings.cloudId}:${settings.instCode}"
    def encoded = pre.bytes.encodeBase64()
    
    def xmlBody = """<Command>
    <Name>get_instantaneous_demand</Name>
    <MacId>0x${settings.macId}</MacId>
    <Format>JSON</Format>
    </Command>"""

    try {
    
        def hubAction = new physicalgraph.device.HubAction([
            method: "POST",
            path: "/cgi-bin/post_manager",
            headers: [
                HOST: address,
                "authorization": "Basic $encoded",
                "Content-Type": "application/xml"
            ],
            body: xmlBody],
            device.deviceNetworkId,
            [callback: instantaneousDemandCallback]
        )
        //log.debug "hubAction"
        //log.trace hubAction
        log.debug "sending get_instantaneous_demand request"
        sendHubCommand(hubAction)
    }
    catch (Exception e) {
        log.debug "Hit Exception $e on $hubAction"
    }
}

def getPriceCallback(response) {
	log.debug "getPrice response"
    //log.debug response.headers
    
    log.debug "status: ${response?.status}"
    
    def successCodes = ["200","201","202"]
	boolean success = successCodes.findAll{response?.status?.toString().contains(it)}
    log.debug "success: $success"
    
    if(success) {
    	def json = parseJson(response.body)
		addGetPriceData(json)
    } else {
    	error()
    }
    
}

def getPrice(){

    def settings = parent.getSettings(this)
    //log.debug "settings"
    //log.debug settings
    
    def address = settings.theAddr
    def macId = settings.macId
    def instCode = settings.instCode
    
    def pre = "${settings.cloudId}:${settings.instCode}"
    def encoded = pre.bytes.encodeBase64()
    
    def xmlBody = """<Command>
    <Name>get_price</Name>
    <MacId>0x${settings.macId}</MacId>
    <Format>JSON</Format>
    </Command>"""

    try {
    
        def hubAction = new physicalgraph.device.HubAction([
            method: "POST",
            path: "/cgi-bin/post_manager",
            headers: [
                HOST: address,
                "authorization": "Basic $encoded",
                "Content-Type": "application/xml"
            ],
            body: xmlBody],
            device.deviceNetworkId,
            [callback: getPriceCallback]
        )
        //log.debug "hubAction"
        //log.trace hubAction
        log.debug "sending get_price request"
        sendHubCommand(hubAction)
    }
    catch (Exception e) {
        log.debug "Hit Exception $e on $hubAction"
    }
}


// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"
}

public addInstDemandData(json) {

	log.trace "Adding data from Eagle for Instantaneous Demand"

    def data = json.InstantaneousDemand
    
    int demand = convertHexToInt(data.Demand)
    int multiplier = convertHexToInt(data.Multiplier)
    int divisor = convertHexToInt(data.Divisor)
    
    def value = (demand * multiplier) / divisor
    
    def valueString = String.valueOf(value)
    
    sendPowerEvent(new Date(), valueString, 'W', true)    
    
    // update time
	def timeData = [
    	date: new Date(),
        value: new Date().format("yyyy-MM-dd h:mm", location.timeZone),
        name: "lastUpdated"//,
        //isStateChange: true
    ]
    sendEvent(timeData)

    getPrice()

}

public addGetPriceData(json) {

	log.trace "Adding data from Eagle for Get Price"

    def data = json.PriceCluster
    
    int price = convertHexToInt(data.Price)
    int trailingDigits = convertHexToInt(data.TrailingDigits)
    def currentPower = device.currentValue("power")
    
    Double priceValue = ((price / (10**trailingDigits))*100)*currentPower
    
    
    sendEvent(name: "price", value: priceValue.round(1))

}

public error() {
	// there was an error retrieving data
    // clear out the value
    sendPowerEvent(new Date(), '---', 'W', true)    
}

private sendPowerEvent(time, value, units, isLatest = false) {

	def eventData = [
		date           : time,
		value          : value,
		name           : "power",
		displayed      : isLatest,
		//isStateChange  : isLatest,
		description    : "${value} ${units}",
		descriptionText: "${value} ${units}"
	]

	log.debug "sending event: ${eventData}"
	sendEvent(eventData)
}

def parseJson(String s) {
	new groovy.json.JsonSlurper().parseText(s)
}

private Integer convertHexToInt(hex) {
    return new BigInteger(hex[2..-1], 16)
}