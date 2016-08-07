/**
 * Sample React Native App
 * https://github.com/facebook/react-native
 * @flow
 */

import React, { Component } from 'react';
import {
  AppRegistry,
  StyleSheet,
  Text,
  TextInput,
  View,
  ToastAndroid,
  DeviceEventEmitter,
  ScrollView
} from 'react-native';

import BlueSerialNativeModule from './BlueSerialNativeModule';
import Button from './components/button.js';

class BlueSerial extends Component {
  constructor(props){
    super(props);
    this.state = {
      text: '',
      inputText: '',
      recvMsg: '',
      scanResults: [],
    };
  }

  componentWillMount(){
    that = this;

    DeviceEventEmitter.addListener('find', function(e) {      
      console.log(e);
      var list = that.state.scanResults;
      list.push(e);
      that.setState({scanResults: list});      
    });

    DeviceEventEmitter.addListener('findFinished', function(e) {      
      console.log(e);
      ToastAndroid.show(e.msg, ToastAndroid.SHORT);
    });

    DeviceEventEmitter.addListener('recv', function(e) {      
      console.log(e);
      that.setState({recvMsg: e.msg}); 
    });
  }

  async onPressTestButton(){
    try {
      var ret = await BlueSerialNativeModule.testMethod(false);
      ToastAndroid.show(ret, ToastAndroid.SHORT);
      this.setState({text: ret});
      console.log(ret);

    } catch (e){
      console.log(e);
      this.setState({text: e.message});
    }
  }

  async onPressStartDiscovery(){
    try {
      this.setState({scanResults:[]});
      var ret = await BlueSerialNativeModule.startDiscovery();
      ToastAndroid.show(ret, ToastAndroid.SHORT);
      console.log(ret);
    } catch(e){
      console.error(e);
    }    
  }

  onPressSendMsg(){
    BlueSerialNativeModule.send(this.state.inputText);
  }


  async _onListItemPress(e){
    console.log(e.devAddr);
    try {
      var ret = await BlueSerialNativeModule.connect(e.devAddr);
      ToastAndroid.show(ret, ToastAndroid.SHORT);
      console.log(ret);
    } catch(err) {
      ToastAndroid.show(err.message, ToastAndroid.SHORT);
      console.error(err);
    }
  }

  render() {
    return (
      <View style={styles.container}>
        <Text style={styles.welcome}>
          Welcome to React Native!
        </Text>
        <Text style={styles.instructions}>
          TestMethod: {this.state.text}
        </Text>
        <Button onPress={this.onPressTestButton.bind(this)}>Test</Button>
        <Button onPress={this.onPressStartDiscovery.bind(this)}>Scan</Button>

        <ScrollView>
          {this.state.scanResults.map((e, i)=><Button onPress={this._onListItemPress.bind(this, e)} key={i}>{e.devName + ": "+e.devAddr}</Button>)}
        </ScrollView>
        <TextInput style={styles.textInput}
          onChangeText={(text) => this.setState({inputText:text})}
          value={this.state.inputText}
        />
        <Text style={styles.instructions}>
          {this.state.recvMsg}
        </Text>
        <Button onPress={this.onPressSendMsg.bind(this)}>Send</Button>

      </View>
    );
  }
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#F5FCFF',
  },
  welcome: {
    fontSize: 20,
    textAlign: 'center',
    margin: 10,
  },
  instructions: {
    textAlign: 'center',
    color: '#333333',
    marginBottom: 5,
  },
  textInput: {
    height: 40, 
    borderColor: 'gray', 
    borderWidth: 1,
    marginTop: 20,
    marginBottom: 60,
  },
});

AppRegistry.registerComponent('BlueSerial', () => BlueSerial);
