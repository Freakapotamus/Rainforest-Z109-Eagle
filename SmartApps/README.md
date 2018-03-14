# Rainforest Eagle Service Manager

Facillitates local LAN connection between the Rainforest Eagle and the SmartThings hub. 

## Notes
 * Both the SmartThings hub and the Eagle must be on the same LAN
 * Eagle will need a fixed IP address. Using a DHCP reservation seems to be the easiest way to go about this
 * The MAC address listed on the bottom your Eagle device is slightly different (more zeros) that what will show up in network logs, when the smart app reqeusts the MAC please use what is printed on the bottom of the device.
 * IP address must include the port. 80 is the default for the Eagle, ex. `192.168.0.17:80`
 * The smart app will query the device every 5 minutes
 * Added use of install code for authentication
