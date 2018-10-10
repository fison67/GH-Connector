# GH-Connector

Connector for Google Home Devices with SmartThings


# Install
#### Preparing
```
You need a Raspbery pi or Synology Nas to install GH Connector API Server
```
<br/><br/>

## Install API Server<br/>
#### Raspberry pi<br/>
> You must install docker first.
```
sudo mkdir /docker
sudo mkdir /docker/gh-connector
sudo chown -R pi:pi /docker
docker pull fison67/gh-connector-rasp:0.0.1
docker run -d --restart=always -v /docker/gh-connector:/config -v /yourMp3Folder:/music1 --name=gh-connector-rasp --net=host fison67/gh-connector-rasp:0.0.1
```

###### Synology nas<br/>
> You must install docker first.<br/>
```
make folder /docker/gh-connector
Run Docker
-> Registery 
-> Search fison67/gh-connector
-> Advanced Settings
-> Volume tab -> folder -> Select gh-connector & Mount path '/config'
-> Volume tab -> folder -> Select MP3 Folder & Mount path '/music1'
-> Network tab -> Check 'use same network as Docker Host'
-> Complete
```


## Install DTH<br/>

<br/><br/>

## Install Smartapps<br/>

## API<br/>
```
TTS
address : /googleHome/:googleHomeAddress/tts (GET, POST)
param
a. message
b. lang
c. volume
d. speed

ex) docker address(192.168.0.100), googleHome(192.168.0.200)
http://192.168.0.100/googleHome/192.168.0.200/tts?messahe=test&lang=ko&volume=20
```
```
Play MP3
address : /googleHome/:googleHomeAddress/play/:mp3Name (GET)
param
a. volume

ex) docker address(192.168.0.100), googleHome(192.168.0.200), mp3(test.mp3)
http://192.168.0.100/googleHome/192.168.0.200/play/test&volume=20

```
